package com.layzee.gesture

import kotlin.math.abs

/**
 * One Euro filter (Casiez et al.) on a single number: steady values are smoothed hard, fast changes pass
 * almost untouched. The time step comes from the real timestamps, because the frame rate is adaptive.
 * No allocation; reuse one instance per signal.
 */
internal class OneEuroFilter {
    private var hasPrevious = false
    private var previousMs = 0L
    private var previousValue = 0f
    private var previousDerivative = 0f

    /** Forget the history; the next sample passes through unchanged. */
    fun reset() {
        hasPrevious = false
    }

    /** Filtered [value] at [timestampMs]. [minCutoffHz] sets the smoothing at rest, [beta] how fast it opens up with speed. */
    fun filter(value: Float, timestampMs: Long, minCutoffHz: Float, beta: Float): Float {
        if (!hasPrevious) {
            hasPrevious = true
            previousMs = timestampMs
            previousValue = value
            previousDerivative = 0f
            return value
        }
        val dt = (timestampMs - previousMs) / 1000f
        // Clock went backwards (restarted source): start over instead of freezing until it catches up.
        if (dt < 0f) {
            previousMs = timestampMs
            previousValue = value
            previousDerivative = 0f
            return value
        }
        // Same timestamp: nothing to learn from this sample.
        if (dt == 0f) return previousValue
        previousMs = timestampMs

        val derivative = (value - previousValue) / dt
        previousDerivative += alpha(dt, DERIVATIVE_CUTOFF_HZ) * (derivative - previousDerivative)
        val cutoff = minCutoffHz + beta * abs(previousDerivative)
        previousValue += alpha(dt, cutoff) * (value - previousValue)
        return previousValue
    }

    private fun alpha(dt: Float, cutoffHz: Float): Float {
        val tau = 1f / (TWO_PI * cutoffHz)
        return 1f / (1f + tau / dt)
    }

    private companion object {
        const val TWO_PI = 6.2831855f
        const val DERIVATIVE_CUTOFF_HZ = 1f
    }
}
