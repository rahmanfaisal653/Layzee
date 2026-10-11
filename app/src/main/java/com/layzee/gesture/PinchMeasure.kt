package com.layzee.gesture

import kotlin.math.sqrt

/**
 * The two pinch distances of one frame, in hand sizes, measured in real pixels:
 * - [tipRatio]: thumb tip to index tip over hand size.
 * - [fallbackRatio]: thumb tip to index PIP over hand size, for a partly hidden index tip.
 *
 * Reuse one instance; no allocation.
 */
internal class PinchMeasure {
    var tipRatio = 0f
        private set
    var fallbackRatio = 0f
        private set

    /** Fills the ratios; false when the hand is smaller than [minHandSizePx] (too far away, or degenerate). */
    fun measure(frame: HandFrame, minHandSizePx: Float): Boolean {
        val w = frame.frameWidth.toFloat()
        val h = frame.frameHeight.toFloat()
        val xs = frame.xs
        val ys = frame.ys

        val handSize = distance(xs, ys, Landmark.WRIST, Landmark.MIDDLE_MCP, w, h)
        if (handSize < minHandSizePx) return false

        tipRatio = distance(xs, ys, Landmark.THUMB_TIP, Landmark.INDEX_TIP, w, h) / handSize
        fallbackRatio = distance(xs, ys, Landmark.THUMB_TIP, Landmark.INDEX_PIP, w, h) / handSize
        return true
    }

    private fun distance(xs: FloatArray, ys: FloatArray, a: Int, b: Int, w: Float, h: Float): Float {
        val dx = (xs[a] - xs[b]) * w
        val dy = (ys[a] - ys[b]) * h
        return sqrt(dx * dx + dy * dy)
    }
}
