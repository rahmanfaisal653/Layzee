package com.layzee.gesture

/**
 * Pinch tuning. Distances are in hand sizes (wrist to middle MCP), so they do not depend on camera distance.
 * The defaults are only starting points; calibration replaces them.
 *
 * Enter/exit differ on purpose (hysteresis). The fallback pair applies to thumb tip -> index PIP,
 * which still reads as a pinch when the index tip is partly hidden.
 */
class GestureConfig(
    val enterRatio: Float = 0.25f,
    val exitRatio: Float = 0.40f,
    val fallbackEnterRatio: Float = 0.30f,
    val fallbackExitRatio: Float = 0.50f,
    val minConfidence: Float = 0.5f,
    val enterFrames: Int = 2,
    val releaseFrames: Int = 2,
) {
    init {
        require(enterRatio <= exitRatio) { "enterRatio must not exceed exitRatio" }
        require(fallbackEnterRatio <= fallbackExitRatio) { "fallbackEnterRatio must not exceed fallbackExitRatio" }
        require(enterFrames >= 1 && releaseFrames >= 1) { "debounce frames must be >= 1" }
    }
}
