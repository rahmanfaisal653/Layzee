package com.layzee.gesture

import kotlin.math.sqrt

/**
 * Pure frame-by-frame gesture logic: Idle / Tracking / Pinch, plus the swipe commands for the virtual
 * finger. No Android types and no allocation per frame.
 *
 * - Pinch distance = thumb tip to index tip over hand size, measured in real pixels.
 * - Fallback distance = thumb tip to index PIP over hand size, for a partly hidden index tip.
 * - Hysteresis: different enter and exit thresholds; the band in between keeps the current state.
 * - Debounce: a change must hold for N consecutive frames before the state follows.
 * - Hand lost or low confidence leaves Pinch at once, with no grace period.
 *
 * Swipe: entering Pinch emits START at the fixed start point; the first segment already moves the finger a
 * small jump up ([GestureOutput.toY]), so it never reads as a tap or a press-and-hold. Each later frame with new data emits MOVE;
 * vertical pinch-point movement is accumulated in hand sizes, smoothed by a One Euro filter, then scaled
 * by the swipe ratio. Leaving Pinch emits LIFT at the last position with the last speed. A real screen
 * touch during Pinch emits CANCEL and returns to Tracking; Pinch cannot start again until the fingers
 * have been apart.
 */
class GestureEngine {
    var state = GestureState.IDLE
        private set

    private var pendingFrames = 0
    private var tipRatio = 0f
    private var fallbackRatio = 0f
    private var handSize = 0f

    // True after a cancel: stay in Tracking until the pinch has been released.
    private var awaitingRelease = false

    // Swipe tracking, valid while in Pinch.
    private val filter = OneEuroFilter()
    private var prevPinchY = 0f
    private var rawDisplacement = 0f
    private var lastTimestampMs = 0L
    private var lastX = 0f
    private var lastY = 0f
    private var velocityY = 0f
    private var touchJump = 0f

    /** Back to Idle. [awaitingRelease] survives: losing the hand does not count as releasing the pinch. */
    private fun reset() {
        state = GestureState.IDLE
        pendingFrames = 0
    }

    fun process(frame: HandFrame, config: GestureConfig, out: GestureOutput) {
        out.command = SwipeCommand.NONE

        if (state == GestureState.PINCH && frame.screenTouched) {
            // The real touch already cancelled the virtual stroke; nothing to lift.
            cancel(out)
        }

        if (!frame.handPresent || frame.confidence < config.minConfidence || !measure(frame)) {
            if (state == GestureState.PINCH) lift(out)
            reset()
            out.state = state
            return
        }

        if (state == GestureState.IDLE) state = GestureState.TRACKING

        val inPinch = state == GestureState.PINCH
        val apart = tipRatio > config.exitRatio && fallbackRatio > config.fallbackExitRatio

        if (awaitingRelease) {
            pendingFrames = if (apart) pendingFrames + 1 else 0
            if (pendingFrames >= config.releaseFrames) {
                awaitingRelease = false
                pendingFrames = 0
            }
            out.state = state
            return
        }

        val wantsPinch = if (inPinch) {
            // Stay pinched until both measures say the fingers are apart.
            !apart
        } else {
            tipRatio < config.enterRatio || fallbackRatio < config.fallbackEnterRatio
        }

        if (wantsPinch == inPinch) {
            pendingFrames = 0
            if (inPinch) move(frame, config, out)
        } else {
            pendingFrames++
            val needed = if (inPinch) config.releaseFrames else config.enterFrames
            if (pendingFrames >= needed) {
                pendingFrames = 0
                if (inPinch) {
                    state = GestureState.TRACKING
                    lift(out)
                } else {
                    state = GestureState.PINCH
                    start(frame, config, out)
                }
            }
            // Frames still waiting out a release are not followed: the fingers are opening, so the
            // pinch point is not a reliable hand position.
        }
        out.state = state
    }

    private fun start(frame: HandFrame, config: GestureConfig, out: GestureOutput) {
        prevPinchY = pinchY(frame)
        rawDisplacement = 0f
        filter.reset(0f)
        lastTimestampMs = frame.timestampMs
        lastX = config.screenWidth / 2f
        touchJump = config.screenHeight * config.touchJumpFraction
        lastY = config.screenHeight * config.startYFraction
        velocityY = 0f
        emit(out, SwipeCommand.START)
        // The first segment already carries the jump, so the touch is a drag from its first moment.
        lastY -= touchJump
        out.toY = lastY
    }

    private fun move(frame: HandFrame, config: GestureConfig, out: GestureOutput) {
        val dtMs = frame.timestampMs - lastTimestampMs
        if (dtMs <= 0) return // no new data
        val dt = dtMs / 1000f
        lastTimestampMs = frame.timestampMs

        val y = pinchY(frame)
        rawDisplacement += (y - prevPinchY) / handSize
        prevPinchY = y

        val displacement = filter.filter(
            rawDisplacement, dt, config.filterMinCutoff, config.filterBeta, config.filterDerivativeCutoff,
        )
        val screenH = config.screenHeight.toFloat()
        val startY = screenH * config.startYFraction
        // Hand up (image y decreasing) moves the finger up; keep it on screen.
        val fingerY = (startY - touchJump + displacement * config.swipeRatio * screenH).coerceIn(0f, screenH - 1f)

        val instantVelocity = (fingerY - lastY) / dt
        velocityY += VELOCITY_SMOOTHING * (instantVelocity - velocityY)
        lastY = fingerY
        emit(out, SwipeCommand.MOVE)
    }

    private fun lift(out: GestureOutput) {
        emit(out, SwipeCommand.LIFT)
    }

    private fun cancel(out: GestureOutput) {
        state = GestureState.TRACKING
        pendingFrames = 0
        awaitingRelease = true
        emit(out, SwipeCommand.CANCEL)
    }

    private fun emit(out: GestureOutput, command: SwipeCommand) {
        out.command = command
        out.x = lastX
        out.y = lastY
        out.velocityY = velocityY
    }

    /** Vertical position of the pinch point (midpoint of thumb tip and index tip) in pixels. */
    private fun pinchY(frame: HandFrame): Float =
        (frame.ys[Landmark.THUMB_TIP] + frame.ys[Landmark.INDEX_TIP]) * 0.5f * frame.frameHeight

    /** Fills [tipRatio], [fallbackRatio] and [handSize]; false when the hand is degenerate (zero hand size). */
    private fun measure(frame: HandFrame): Boolean {
        val w = frame.frameWidth.toFloat()
        val h = frame.frameHeight.toFloat()
        val xs = frame.xs
        val ys = frame.ys

        handSize = distance(xs, ys, Landmark.WRIST, Landmark.MIDDLE_MCP, w, h)
        if (handSize < MIN_HAND_SIZE_PX) return false

        tipRatio = distance(xs, ys, Landmark.THUMB_TIP, Landmark.INDEX_TIP, w, h) / handSize
        fallbackRatio = distance(xs, ys, Landmark.THUMB_TIP, Landmark.INDEX_PIP, w, h) / handSize
        return true
    }

    private fun distance(xs: FloatArray, ys: FloatArray, a: Int, b: Int, w: Float, h: Float): Float {
        val dx = (xs[a] - xs[b]) * w
        val dy = (ys[a] - ys[b]) * h
        return sqrt(dx * dx + dy * dy)
    }

    private companion object {
        const val MIN_HAND_SIZE_PX = 1e-3f

        // Weight of the newest per-frame speed in the lift velocity (0..1, higher = less smoothing).
        const val VELOCITY_SMOOTHING = 0.5f
    }
}
