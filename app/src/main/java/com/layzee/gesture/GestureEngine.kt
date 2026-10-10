package com.layzee.gesture

/**
 * Pure frame-by-frame gesture logic: Idle / Tracking / Pinch, plus the swipe command. No Android types and
 * no allocation per frame.
 *
 * - Pinch distance = thumb tip to index tip over hand size, measured in real pixels.
 * - Fallback distance = thumb tip to index PIP over hand size, for a partly hidden index tip.
 * - Hysteresis: different enter and exit thresholds; the band in between keeps the current state.
 * - Debounce: a change must hold for N consecutive frames before the state follows.
 * - Hand lost or low confidence leaves Pinch at once, with no grace period.
 *
 * Pinch is the swipe: entering Pinch emits one FLICK (up, from the fixed start point) and nothing else; the
 * hand's movement is not read. One pinch gives one flick. The next one needs the fingers to have been apart
 * for [GestureConfig.releaseFrames] frames, so a held pinch, or a hand that drops out and returns still
 * pinched, never flicks twice.
 */
class GestureEngine {
    var state = GestureState.IDLE
        private set

    private var pendingFrames = 0
    private var apartFrames = 0
    private val measure = PinchMeasure()

    // True from a flick until the fingers have been apart long enough; losing the hand does not clear it.
    private var spent = false

    /** Back to Idle. [spent] survives: losing the hand does not count as releasing the pinch. */
    private fun reset() {
        state = GestureState.IDLE
        pendingFrames = 0
        apartFrames = 0
    }

    fun process(frame: HandFrame, config: GestureConfig, out: GestureOutput) {
        out.command = SwipeCommand.NONE

        if (!frame.handPresent || frame.confidence < config.minConfidence || !measure.measure(frame)) {
            reset()
            out.state = state
            return
        }

        if (state == GestureState.IDLE) state = GestureState.TRACKING

        val inPinch = state == GestureState.PINCH
        val tipRatio = measure.tipRatio
        val fallbackRatio = measure.fallbackRatio
        val apart = tipRatio > config.exitRatio && fallbackRatio > config.fallbackExitRatio

        apartFrames = if (apart) apartFrames + 1 else 0
        if (apartFrames >= config.releaseFrames) spent = false

        val wantsPinch = if (inPinch) {
            // Stay pinched until both measures say the fingers are apart.
            !apart
        } else {
            !spent && (tipRatio < config.enterRatio || fallbackRatio < config.fallbackEnterRatio)
        }

        if (wantsPinch == inPinch) {
            pendingFrames = 0
        } else {
            pendingFrames++
            val needed = if (inPinch) config.releaseFrames else config.enterFrames
            if (pendingFrames >= needed) {
                pendingFrames = 0
                if (inPinch) {
                    state = GestureState.TRACKING
                } else {
                    state = GestureState.PINCH
                    flick(config, out)
                }
            }
        }
        out.state = state
    }

    private fun flick(config: GestureConfig, out: GestureOutput) {
        spent = true
        val height = config.screenHeight.toFloat()
        out.command = SwipeCommand.FLICK
        out.x = config.screenWidth / 2f
        out.y = height * config.startYFraction
        out.toY = (out.y - height * config.flickDistanceFraction).coerceAtLeast(0f)
    }
}
