package com.layzee.gesture

/**
 * Pinch tuning. Distances are in hand sizes (wrist to middle MCP), so they do not depend on camera distance.
 * The defaults are only starting points; calibration replaces them.
 *
 * Enter/exit differ on purpose (hysteresis). The fallback pair applies to thumb tip -> index PIP,
 * which still reads as a pinch when the index tip is partly hidden.
 *
 * A hand smaller than [minHandSizePx] (wrist to middle MCP, in analysis-frame pixels) is too far away to read
 * reliably: it stays Tracking and never pinches. The default is a starting point to tune on the device.
 *
 * The pinch ratios are smoothed with a One Euro filter ([smoothingMinCutoffHz], [smoothingBeta]) before the
 * thresholds are applied. Entering Pinch needs [enterFrames] frames in a row: a false pinch (an accidental
 * swipe in another app) costs more than a missed one.
 *
 * Swipe: a pinch is one flick up from (screenWidth / 2, screenHeight * startYFraction), [flickDistanceFraction]
 * of the screen height long.
 */
class GestureConfig(
    val enterRatio: Float = 0.25f,
    val exitRatio: Float = 0.40f,
    val fallbackEnterRatio: Float = 0.30f,
    val fallbackExitRatio: Float = 0.50f,
    val minConfidence: Float = 0.5f,
    val enterFrames: Int = 3,
    val releaseFrames: Int = 2,
    val minHandSizePx: Float = DEFAULT_MIN_HAND_SIZE_PX,
    val smoothingMinCutoffHz: Float = 2f,
    val smoothingBeta: Float = 1f,
    val screenWidth: Int = 1080,
    val screenHeight: Int = 2400,
    val startYFraction: Float = 0.65f,
    val flickDistanceFraction: Float = 0.45f,
) {
    init {
        require(enterRatio <= exitRatio) { "enterRatio must not exceed exitRatio" }
        require(fallbackEnterRatio <= fallbackExitRatio) { "fallbackEnterRatio must not exceed fallbackExitRatio" }
        require(enterFrames >= 1 && releaseFrames >= 1) { "debounce frames must be >= 1" }
        require(minHandSizePx > 0f) { "minHandSizePx must be positive" }
        require(smoothingMinCutoffHz > 0f && smoothingBeta >= 0f) { "invalid smoothing parameters" }
        require(screenWidth > 0 && screenHeight > 0) { "screen size must be positive" }
        require(startYFraction in 0f..1f) { "startYFraction must be within 0..1" }
        require(flickDistanceFraction in 0f..startYFraction) { "flick must stay on screen" }
    }

    companion object {
        /** About 70-80 cm from the camera at the 320x240 analysis size. */
        const val DEFAULT_MIN_HAND_SIZE_PX = 30f
    }
}
