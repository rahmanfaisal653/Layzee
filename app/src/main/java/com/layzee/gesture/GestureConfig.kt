package com.layzee.gesture

/**
 * Pinch tuning. Distances are in hand sizes (wrist to middle MCP), so they do not depend on camera distance.
 * The defaults are only starting points; calibration replaces them.
 *
 * Enter/exit differ on purpose (hysteresis). The fallback pair applies to thumb tip -> index PIP,
 * which still reads as a pinch when the index tip is partly hidden.
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
    val enterFrames: Int = 2,
    val releaseFrames: Int = 2,
    val screenWidth: Int = 1080,
    val screenHeight: Int = 2400,
    val startYFraction: Float = 0.65f,
    val flickDistanceFraction: Float = 0.45f,
) {
    init {
        require(enterRatio <= exitRatio) { "enterRatio must not exceed exitRatio" }
        require(fallbackEnterRatio <= fallbackExitRatio) { "fallbackEnterRatio must not exceed fallbackExitRatio" }
        require(enterFrames >= 1 && releaseFrames >= 1) { "debounce frames must be >= 1" }
        require(screenWidth > 0 && screenHeight > 0) { "screen size must be positive" }
        require(startYFraction in 0f..1f) { "startYFraction must be within 0..1" }
        require(flickDistanceFraction in 0f..startYFraction) { "flick must stay on screen" }
    }
}
