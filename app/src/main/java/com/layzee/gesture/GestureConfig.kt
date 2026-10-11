package com.layzee.gesture

/**
 * Pinch tuning. Distances are in hand sizes (wrist to middle MCP), so they do not depend on camera distance.
 * The defaults come from measured pinches on the test phone (pinch: tip 0.1-0.27, PIP 0.36-0.57; open hand: tip 0.7+, PIP 0.54+).
 *
 * Enter/exit differ on purpose (hysteresis). While pinching, the exit is also [exitMargin] (hand sizes, ~1 cm)
 * above the closest the tip distance got in that pinch, so a loose "touch" reading still releases promptly.
 * The fallback applies to thumb tip -> index PIP and only gates
 * entering (a second opinion, loose on purpose): it varies too much with hand angle to decide the release.
 *
 * A hand smaller than [minHandSizePx] (wrist to middle MCP, in analysis-frame pixels) is too far away to read
 * reliably: it stays Tracking and never pinches. The default is a starting point to tune on the device.
 *
 * The pinch ratios are smoothed with a One Euro filter ([smoothingMinCutoffHz], [smoothingBeta]) before the
 * thresholds are applied. Entering Pinch needs [enterFrames] frames in a row: a false pinch (an accidental
 * swipe in another app) costs more than a missed one. Leaving Pinch takes only [exitFrames] frames so the
 * state follows the fingers opening; [releaseFrames] is the longer wait that re-arms the next flick.
 *
 * Swipe: a pinch is one flick up from (screenWidth / 2, screenHeight * startYFraction), [flickDistanceFraction]
 * of the screen height long.
 */
class GestureConfig(
    val enterRatio: Float = 0.25f,
    val exitRatio: Float = 0.30f,
    val exitMargin: Float = 0.07f,
    val fallbackEnterRatio: Float = 0.70f,
    val minConfidence: Float = 0.5f,
    val enterFrames: Int = 3,
    val releaseFrames: Int = 2,
    val exitFrames: Int = 1,
    val minHandSizePx: Float = DEFAULT_MIN_HAND_SIZE_PX,
    val smoothingMinCutoffHz: Float = 3f,
    val smoothingBeta: Float = 1f,
    val screenWidth: Int = 1080,
    val screenHeight: Int = 2400,
    val startYFraction: Float = 0.65f,
    val flickDistanceFraction: Float = 0.45f,
) {
    init {
        require(enterRatio <= exitRatio) { "enterRatio must not exceed exitRatio" }
        require(exitMargin > 0f) { "exitMargin must be positive" }
        require(enterFrames >= 1 && releaseFrames >= 1 && exitFrames >= 1) { "debounce frames must be >= 1" }
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
