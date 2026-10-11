package com.layzee.calibration

/**
 * The user's own pinch thresholds, in hand sizes (see [com.layzee.gesture.GestureConfig]). Results of one
 * [CalibrationSession]; stored by [CalibrationStore] and used by the gesture engine from the next Start.
 *
 * A fallback pair of 0 / 0 means the fallback measure did not separate pinch from open hand for this user and
 * is switched off: it can then neither start nor hold a pinch.
 */
data class Calibration(
    val enterRatio: Float,
    val exitRatio: Float,
    val fallbackEnterRatio: Float,
    val fallbackExitRatio: Float,
) {
    init {
        require(enterRatio <= exitRatio) { "enterRatio must not exceed exitRatio" }
        require(fallbackEnterRatio <= fallbackExitRatio) { "fallbackEnterRatio must not exceed fallbackExitRatio" }
    }

    /**
     * These thresholds nudged by Pinch sensitivity [level] ([SENSITIVITY_MIN]..[SENSITIVITY_MAX], out of range is
     * clamped): higher is looser (a pinch is read from further apart), lower is tighter. Every threshold is
     * scaled by the same small factor, so enter stays below exit and an off fallback stays off. The stored
     * calibration is never changed.
     */
    fun withSensitivity(level: Int): Calibration {
        val steps = level.coerceIn(SENSITIVITY_MIN, SENSITIVITY_MAX) - SENSITIVITY_NEUTRAL
        val scale = 1f + steps * SCALE_PER_STEP
        return Calibration(enterRatio * scale, exitRatio * scale, fallbackEnterRatio * scale, fallbackExitRatio * scale)
    }

    companion object {
        const val SENSITIVITY_MIN = 0
        const val SENSITIVITY_NEUTRAL = 5
        const val SENSITIVITY_MAX = 10

        // At either end the thresholds move 15%: enough to feel, small enough to stay inside the calibrated gap.
        private const val SCALE_PER_STEP = 0.03f

        /** Smallest gap, in hand sizes, between the loosest pinch and the tightest open hand that still separates them. */
        private const val MIN_GAP = 0.15f

        // Where in the gap the thresholds sit: starting points, to be tuned by real use.
        private const val ENTER_AT = 0.3f
        private const val EXIT_AT = 0.6f

        /**
         * Thresholds from the loosest pinch ([pinchHigh]) and the tightest open hand ([openLow]) the user
         * showed, for the tip measure and the fallback measure. Null when the tip measure does not separate
         * the two conditions.
         */
        fun from(pinchHigh: Float, openLow: Float, fallbackPinchHigh: Float, fallbackOpenLow: Float): Calibration? {
            val gap = openLow - pinchHigh
            if (gap < MIN_GAP) return null
            val fallbackGap = fallbackOpenLow - fallbackPinchHigh
            val fallbackOn = fallbackGap >= MIN_GAP
            return Calibration(
                enterRatio = pinchHigh + ENTER_AT * gap,
                exitRatio = pinchHigh + EXIT_AT * gap,
                fallbackEnterRatio = if (fallbackOn) fallbackPinchHigh + ENTER_AT * fallbackGap else 0f,
                fallbackExitRatio = if (fallbackOn) fallbackPinchHigh + EXIT_AT * fallbackGap else 0f,
            )
        }
    }
}
