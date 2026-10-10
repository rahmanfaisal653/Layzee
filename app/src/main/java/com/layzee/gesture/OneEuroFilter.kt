package com.layzee.gesture

import kotlin.math.PI
import kotlin.math.abs

/**
 * One Euro filter (Casiez et al.): low cutoff at rest to kill jitter, higher cutoff when moving fast
 * to keep lag small. Holds a single float of state; no allocation per call.
 */
internal class OneEuroFilter {
    private var started = false
    private var xHat = 0f
    private var dxHat = 0f

    fun reset(initial: Float) {
        started = true
        xHat = initial
        dxHat = 0f
    }

    /** [dtSeconds] must be > 0. */
    fun filter(x: Float, dtSeconds: Float, minCutoff: Float, beta: Float, dCutoff: Float): Float {
        if (!started) {
            reset(x)
            return x
        }
        val dx = (x - xHat) / dtSeconds
        dxHat += alpha(dCutoff, dtSeconds) * (dx - dxHat)
        val cutoff = minCutoff + beta * abs(dxHat)
        xHat += alpha(cutoff, dtSeconds) * (x - xHat)
        return xHat
    }

    private fun alpha(cutoff: Float, dtSeconds: Float): Float {
        val tau = 1f / (2f * PI.toFloat() * cutoff)
        return 1f / (1f + tau / dtSeconds)
    }
}
