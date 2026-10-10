package com.layzee.gesture

import kotlin.math.sqrt

/**
 * Pure frame-by-frame pinch logic: Idle / Tracking / Pinch. No Android types and no allocation per frame.
 *
 * - Pinch distance = thumb tip to index tip over hand size, measured in real pixels.
 * - Fallback distance = thumb tip to index PIP over hand size, for a partly hidden index tip.
 * - Hysteresis: different enter and exit thresholds; the band in between keeps the current state.
 * - Debounce: a change must hold for N consecutive frames before the state follows.
 * - Hand lost or low confidence leaves Pinch at once, with no grace period.
 */
class GestureEngine {
    var state = GestureState.IDLE
        private set

    private var pendingFrames = 0
    private var tipRatio = 0f
    private var fallbackRatio = 0f

    private fun reset() {
        state = GestureState.IDLE
        pendingFrames = 0
    }

    fun process(frame: HandFrame, config: GestureConfig, out: GestureOutput) {
        if (!frame.handPresent || frame.confidence < config.minConfidence || !measure(frame)) {
            reset()
            out.state = state
            return
        }

        if (state == GestureState.IDLE) state = GestureState.TRACKING

        val inPinch = state == GestureState.PINCH
        val wantsPinch = if (inPinch) {
            // Stay pinched until both measures say the fingers are apart.
            !(tipRatio > config.exitRatio && fallbackRatio > config.fallbackExitRatio)
        } else {
            tipRatio < config.enterRatio || fallbackRatio < config.fallbackEnterRatio
        }

        if (wantsPinch == inPinch) {
            pendingFrames = 0
        } else {
            pendingFrames++
            val needed = if (inPinch) config.releaseFrames else config.enterFrames
            if (pendingFrames >= needed) {
                state = if (inPinch) GestureState.TRACKING else GestureState.PINCH
                pendingFrames = 0
            }
        }
        out.state = state
    }

    /** Fills [tipRatio] and [fallbackRatio]; false when the hand is degenerate (zero hand size). */
    private fun measure(frame: HandFrame): Boolean {
        val w = frame.frameWidth.toFloat()
        val h = frame.frameHeight.toFloat()
        val xs = frame.xs
        val ys = frame.ys

        val handSize = distance(xs, ys, Landmark.WRIST, Landmark.MIDDLE_MCP, w, h)
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
    }
}
