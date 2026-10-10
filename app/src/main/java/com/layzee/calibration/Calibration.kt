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

    companion object {
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
