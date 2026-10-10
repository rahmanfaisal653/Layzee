package com.layzee.pipeline

import com.layzee.gesture.GestureState
import java.nio.ByteBuffer

/**
 * Everything that sets how much work the pipeline does while Layzee is on. All tuning lives here.
 *
 * Rates are model runs per second; the camera itself keeps its locked range (see [FpsRanges]) and the extra
 * frames are dropped before conversion and inference. Frames follow a schedule, so a 30 fps camera averages
 * 20 fps for a 20 fps target.
 */
internal class PacingConfig(
    val idleFps: Int = 5,
    val trackingFps: Int = 20,
    val pinchFps: Int = 30,
    /** After the hand is lost the rate stays at the Tracking rate this long, then falls to Idle. */
    val holdMs: Long = 3_000,
    /** While Idle the model runs at least this often even if the scene never moves. */
    val forcedRunMs: Long = 2_000,
    /** Motion gate: a grid pixel counts as changed when its gray value moves by more than this. */
    val motionPixelDelta: Int = 24,
    /** Motion gate: the scene counts as moving when this fraction of the grid has changed. */
    val motionChangedFraction: Float = 0.02f,
)

/**
 * Decides per camera frame whether it goes to the model.
 *
 * - Rate: the target fps follows the last gesture state (Idle low, Tracking, Pinch highest). After a hand is
 *   lost the Tracking rate holds for [PacingConfig.holdMs].
 * - Motion gate: only at the Idle rate. The frame is sampled down to a 64x48 gray grid and compared with the
 *   grid of the last frame that went to the model; the model is skipped when too little changed. Comparing
 *   against that frame (not the previous one) lets a slow movement add up. A forced run every
 *   [PacingConfig.forcedRunMs] covers a hand that appeared without motion the gate noticed.
 *
 * [admit] runs on the analysis thread, [onResult] on the MediaPipe callback thread. No allocation per frame:
 * both grids are allocated once. All times are camera frame timestamps in ms.
 */
internal class FramePacer(private val config: PacingConfig = PacingConfig()) {
    @Volatile private var state = GestureState.IDLE
    @Volatile private var lastHandMs = NEVER
    // Start of the current burst of motion that woke the model; NONE when there is none.
    @Volatile private var wakeMs = NONE

    // Analysis thread only.
    private var nextPassMs = NEVER
    private var lastModelMs = NEVER
    private var referenceValid = false
    private var current = ByteArray(GRID_SIZE)
    private var reference = ByteArray(GRID_SIZE)
    private val minChanged = (GRID_SIZE * config.motionChangedFraction).toInt().coerceAtLeast(1)

    /** Returns [ADMIT], [SKIP_RATE] or [SKIP_MOTION]. [pixels] is RGBA_8888 with the given row stride in bytes. */
    fun admit(timestampMs: Long, pixels: ByteBuffer, rowStride: Int, width: Int, height: Int): Int {
        val now = state
        val idle = now == GestureState.IDLE && timestampMs - lastHandMs >= config.holdMs
        val fps = when {
            idle -> config.idleFps
            now == GestureState.PINCH -> config.pinchFps
            else -> config.trackingFps
        }
        // A schedule, not "time since the last pass": otherwise a 30 fps camera could only give 30, 15, 10 ... fps.
        val interval = 1000L / fps
        val due = minOf(nextPassMs, timestampMs + interval) // a slower earlier rate must not delay a faster one
        if (timestampMs + JITTER_MS < due) return SKIP_RATE
        nextPassMs = if (timestampMs - due > interval) timestampMs + interval else due + interval

        if (!idle) {
            // The reference would be stale by the time Idle returns.
            referenceValid = false
            lastModelMs = timestampMs
            return ADMIT
        }

        sample(pixels, rowStride, width, height)
        val moved = !referenceValid || changedCount() >= minChanged
        val forced = timestampMs - lastModelMs >= config.forcedRunMs
        if (!moved && !forced) {
            wakeMs = NONE
            return SKIP_MOTION
        }
        if (moved && referenceValid && (wakeMs == NONE || timestampMs - wakeMs > WAKE_STALE_MS)) wakeMs = timestampMs
        val swap = reference
        reference = current
        current = swap
        referenceValid = true
        lastModelMs = timestampMs
        return ADMIT
    }

    /**
     * Feeds the gesture state of a result back into the rate. Returns how long (ms of camera time) it took
     * from the first motion that woke the model to this first non-Idle result, once per appearance; -1
     * otherwise. For the debug log.
     */
    fun onResult(newState: GestureState, timestampMs: Long): Long {
        state = newState
        if (newState == GestureState.IDLE) return -1L
        lastHandMs = timestampMs
        val wake = wakeMs
        if (wake == NONE) return -1L
        wakeMs = NONE
        return timestampMs - wake
    }

    // Nearest-neighbour sample of the centre of each grid cell into `current`: (r + 2g + b) / 4.
    private fun sample(pixels: ByteBuffer, rowStride: Int, width: Int, height: Int) {
        var out = 0
        for (gy in 0 until GRID_H) {
            val row = ((2 * gy + 1) * height / (2 * GRID_H)) * rowStride
            for (gx in 0 until GRID_W) {
                val i = row + ((2 * gx + 1) * width / (2 * GRID_W)) * 4
                val r = pixels.get(i).toInt() and 0xFF
                val g = pixels.get(i + 1).toInt() and 0xFF
                val b = pixels.get(i + 2).toInt() and 0xFF
                current[out++] = ((r + 2 * g + b) shr 2).toByte()
            }
        }
    }

    private fun changedCount(): Int {
        var changed = 0
        for (i in 0 until GRID_SIZE) {
            val d = (current[i].toInt() and 0xFF) - (reference[i].toInt() and 0xFF)
            if (d > config.motionPixelDelta || -d > config.motionPixelDelta) changed++
        }
        return changed
    }

    companion object {
        const val ADMIT = 0
        const val SKIP_RATE = 1
        const val SKIP_MOTION = 2

        const val GRID_W = 64
        const val GRID_H = 48
        private const val GRID_SIZE = GRID_W * GRID_H
        private const val JITTER_MS = 8L
        private const val WAKE_STALE_MS = 3_000L // longer motion than this is not one hand appearing
        private const val NEVER = Long.MIN_VALUE / 2
        private const val NONE = -1L
    }
}
