package com.layzee.swipe

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.graphics.Path
import android.os.SystemClock
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Turns the engine's virtual finger commands into one continued Accessibility stroke.
 *
 * Android has no "finger up" or "cancel" call: a stroke is a chain of `dispatchGesture` segments, each
 * `continueStroke` of the previous one with `willContinue = true`, and the last one with `false`. Each
 * segment must start exactly where the previous ended, so [endX]/[endY] are kept as the single source of
 * truth. Only one segment is in flight at a time: [move] only records the newest target, and the next
 * segment (from the last end point to that target, over the time since the previous segment) goes out
 * when the previous one completes. That smooths the movement linearly without extrapolating.
 *
 * A real touch cancels the stroke; the callback reports that through [consumeTouched]. Dispatching a new
 * gesture that is not a continuation would cancel the running one, so [start] waits for a final segment
 * still in flight.
 *
 * Calls come from the MediaPipe callback thread, callbacks from the main thread: everything is under [lock].
 * The Accessibility API allocates per segment (stroke, description, callback result); that cannot be reused.
 */
class SwipeDispatcher(private val service: AccessibilityService) {
    private val lock = Any()
    private val path = Path() // StrokeDescription copies the path, so one instance is reused
    private val touched = AtomicBoolean(false)

    private var stroke: StrokeDescription? = null // last dispatched segment of the live stroke
    private var endX = 0f
    private var endY = 0f
    private var inFlight = false
    private var lastDispatchMs = 0L

    private var hasTarget = false
    private var targetX = 0f
    private var targetY = 0f

    private var liftPending = false
    private var liftY = 0f
    private var liftVelocityY = 0f
    private var liftMaxY = 0f

    private var startPending = false
    private var startX = 0f
    private var startY = 0f

    private val callback = object : AccessibilityService.GestureResultCallback() {
        override fun onCompleted(description: GestureDescription) {
            synchronized(lock) {
                inFlight = false
                flush()
            }
        }

        override fun onCancelled(description: GestureDescription) {
            synchronized(lock) {
                inFlight = false
                if (stroke != null) {
                    // Cancelled while we still meant to continue: a real finger touched the screen.
                    touched.set(true)
                    dropStroke()
                }
                flush()
            }
        }
    }

    /** True once if the real screen was touched since the last call; the engine turns it into CANCEL. */
    fun consumeTouched(): Boolean = touched.getAndSet(false)

    /** Virtual finger touches down at ([x], [y]). */
    fun start(x: Float, y: Float) {
        synchronized(lock) {
            if (stroke != null) return // the engine lifts or cancels before starting again
            startX = x
            startY = y
            startPending = true
            flush()
        }
    }

    /** Virtual finger moves to ([x], [y]); sent when the previous segment is done. */
    fun move(x: Float, y: Float) {
        synchronized(lock) {
            if (stroke == null || liftPending) return
            targetX = x
            targetY = y
            hasTarget = true
            flush()
        }
    }

    /**
     * Virtual finger lifts at [y] moving at [velocityY] px/s (positive = down); [maxY] is the bottom
     * of the screen, for the short tail after the lift point.
     */
    fun lift(y: Float, velocityY: Float, maxY: Float) {
        synchronized(lock) {
            startPending = false // released before the finger ever went down
            if (stroke == null) return
            liftY = y
            liftVelocityY = velocityY
            liftMaxY = maxY
            liftPending = true
            hasTarget = false
            flush()
        }
    }

    /** The engine saw the real touch: the stroke is already dead, forget it. */
    fun cancel() {
        synchronized(lock) {
            startPending = false
            dropStroke()
        }
    }

    /** Pipeline stopped: lift a live stroke where it is so no finger stays pressed. */
    fun release() {
        synchronized(lock) {
            startPending = false
            if (stroke == null) return
            if (!liftPending) {
                liftY = endY
                liftVelocityY = 0f
                liftMaxY = endY
                liftPending = true
            }
            hasTarget = false
            flush()
        }
    }

    private fun dropStroke() {
        stroke = null
        hasTarget = false
        liftPending = false
    }

    // Sends the next segment if nothing is in flight. Caller holds [lock].
    private fun flush() {
        if (inFlight) return
        val now = SystemClock.uptimeMillis()
        val current = stroke
        when {
            // Catch up to the lift point as a normal move first, so the exit speed is not the lag.
            current != null && liftPending && liftY != endY -> sendMove(current, now, endX, liftY)
            current != null && liftPending -> sendLift(current, now)
            current != null && hasTarget -> sendMove(current, now, targetX, targetY)
            current == null && startPending -> sendStart(now)
        }
    }

    private fun sendStart(now: Long) {
        startPending = false
        path.rewind()
        path.moveTo(startX, startY)
        val first = StrokeDescription(path, 0, START_MS, true)
        if (send(first, now)) {
            stroke = first
            endX = startX
            endY = startY
        }
    }

    private fun sendMove(current: StrokeDescription, now: Long, toX: Float, toY: Float) {
        hasTarget = false
        if (toX == endX && toY == endY) return
        val duration = (now - lastDispatchMs).coerceIn(MIN_SEGMENT_MS, MAX_SEGMENT_MS)
        path.rewind()
        path.moveTo(endX, endY)
        path.lineTo(toX, toY)
        val next = current.continueStroke(path, 0, duration, true)
        if (send(next, now)) {
            stroke = next
            endX = toX
            endY = toY
        }
    }

    // Last segment, from the lift point (the finger is already there): the finger keeps moving at the last
    // speed for a moment, so the app's velocity tracker sees it still moving when it leaves.
    private fun sendLift(current: StrokeDescription, now: Long) {
        liftPending = false
        val tailEndY = (endY + liftVelocityY * LIFT_TAIL_MS / 1000f).coerceIn(0f, liftMaxY)
        path.rewind()
        path.moveTo(endX, endY)
        if (tailEndY != endY) path.lineTo(endX, tailEndY)
        val last = current.continueStroke(path, 0, LIFT_TAIL_MS, false)
        // Whether it dispatches or not, no finger remains afterwards.
        stroke = null
        send(last, now)
    }

    private fun send(segment: StrokeDescription, now: Long): Boolean {
        val gesture = GestureDescription.Builder().addStroke(segment).build()
        val accepted = service.dispatchGesture(gesture, callback, null)
        if (accepted) {
            inFlight = true
            lastDispatchMs = now
        } else {
            dropStroke()
        }
        return accepted
    }

    private companion object {
        const val START_MS = 10L
        const val MIN_SEGMENT_MS = 8L
        const val MAX_SEGMENT_MS = 100L
        const val LIFT_TAIL_MS = 16L
    }
}
