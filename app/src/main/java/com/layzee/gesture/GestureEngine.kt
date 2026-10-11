package com.layzee.gesture

import kotlin.math.sqrt

/** Reused result container: the engine writes into it instead of returning new objects per frame. */
class GestureOutput {
    var state = GestureState.IDLE
        internal set

    /** True for the one frame where a pinch begins: send one flick up from ([x], [y]) to [toY]. */
    var flick = false
        internal set

    /** Where the flick starts and ends, in screen pixels; valid when [flick]. */
    var x = 0f
        internal set
    var y = 0f
        internal set
    var toY = 0f
        internal set
}

/**
 * Pure frame-by-frame gesture logic: Idle / Tracking / Pinch, plus the flick. No Android types and
 * no allocation per frame.
 *
 * - Pinch distance = thumb tip to index tip over hand size, measured in real pixels.
 * - Fallback distance = thumb tip to index PIP over hand size, a second opinion for entering only: a pinch
 *   starts only when both distances are under their enter thresholds (AND), unless the fallback threshold is 0
 *   (switched off). Leaving Pinch looks at the tip distance alone.
 * - Both distances are smoothed with a One Euro filter before any threshold is applied.
 * - A hand under [GestureConfig.minHandSizePx] is too far away: it stays Tracking and never pinches.
 * - Hysteresis: different enter and exit thresholds; the band in between keeps the current state. In Pinch the exit
 *   threshold is also capped at this pinch's closest tip distance plus [GestureConfig.exitMargin].
 * - Debounce: a change must hold for N consecutive frames before the state follows (entering [GestureConfig.enterFrames],
 *   leaving [GestureConfig.exitFrames]).
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
    // Closest the tip distance got during this pinch: the user's own "touching" reading, which varies with hand angle.
    private var pinchBaseline = 0f
    private var apartFrames = 0
    // The two pinch distances of the current frame, in hand sizes (see [measure]).
    private var rawTipRatio = 0f
    private var rawFallbackRatio = 0f
    private val tipFilter = OneEuroFilter()
    private val fallbackFilter = OneEuroFilter()

    // True from a flick until the fingers have been apart long enough; losing the hand does not clear it.
    private var spent = false

    /** Back to Idle, filters forgotten. [spent] survives: losing the hand does not count as releasing the pinch. */
    private fun reset() {
        state = GestureState.IDLE
        pendingFrames = 0
        apartFrames = 0
        tipFilter.reset()
        fallbackFilter.reset()
    }

    fun process(frame: HandFrame, config: GestureConfig, out: GestureOutput) {
        out.flick = false

        if (!frame.handPresent || frame.confidence < config.minConfidence) {
            reset()
            out.state = state
            return
        }

        if (!measure(frame, config.minHandSizePx)) {
            // Too far away to read: seen, but never a pinch. A pinch in progress ends at once.
            reset()
            state = GestureState.TRACKING
            out.state = state
            return
        }

        if (state == GestureState.IDLE) state = GestureState.TRACKING

        val inPinch = state == GestureState.PINCH
        val tipRatio = tipFilter.filter(rawTipRatio, frame.timestampMs, config.smoothingMinCutoffHz, config.smoothingBeta)
        // A fallback threshold of 0 means it is switched off: skip its filter too.
        val fallbackOn = config.fallbackEnterRatio > 0f
        val fallbackRatio = if (fallbackOn) {
            fallbackFilter.filter(rawFallbackRatio, frame.timestampMs, config.smoothingMinCutoffHz, config.smoothingBeta)
        } else {
            rawFallbackRatio
        }
        // Only the tip measure decides that the fingers are apart: the fallback measure reads 0.5-0.6 even on an
        // open hand at some angles and distances, which used to keep the pinch stuck. In Pinch the fingers count as
        // apart as soon as the tip distance rises [GestureConfig.exitMargin] above this pinch's own closest reading
        // (about 1 cm), because "touching" reads anywhere from 0.15 to 0.27 and a fixed threshold lags behind.
        val releaseAt = if (inPinch) minOf(config.exitRatio, pinchBaseline + config.exitMargin) else config.exitRatio
        val apart = tipRatio > releaseAt
        if (inPinch && !apart) {
            pinchBaseline = if (tipRatio < pinchBaseline) tipRatio else pinchBaseline + BASELINE_RISE * (tipRatio - pinchBaseline)
        }

        apartFrames = if (apart) apartFrames + 1 else 0
        if (apartFrames >= config.releaseFrames) spent = false

        val wantsPinch = if (inPinch) {
            // Stay pinched until the tip measure says the fingers are apart.
            !apart
        } else {
            !spent && tipRatio < config.enterRatio && (!fallbackOn || fallbackRatio < config.fallbackEnterRatio)
        }

        if (wantsPinch == inPinch) {
            pendingFrames = 0
        } else {
            pendingFrames++
            val needed = if (inPinch) config.exitFrames else config.enterFrames
            if (pendingFrames >= needed) {
                pendingFrames = 0
                if (inPinch) {
                    state = GestureState.TRACKING
                } else {
                    state = GestureState.PINCH
                    pinchBaseline = tipRatio
                    flick(config, out)
                }
            }
        }
        out.state = state
    }

    private companion object {
        /** How fast the baseline follows a tip distance that stays above it (per frame): slow, a drift and not a release. */
        const val BASELINE_RISE = 0.02f
    }

    private fun flick(config: GestureConfig, out: GestureOutput) {
        spent = true
        val height = config.screenHeight.toFloat()
        out.flick = true
        out.x = config.screenWidth / 2f
        out.y = height * config.startYFraction
        out.toY = (out.y - height * config.flickDistanceFraction).coerceAtLeast(0f)
    }

    /**
     * Fills [rawTipRatio] (thumb tip to index tip over hand size) and [rawFallbackRatio] (thumb tip to index PIP,
     * for a partly hidden index tip), both in real pixels. False when the hand is smaller than [minHandSizePx]
     * (too far away, or degenerate).
     */
    private fun measure(frame: HandFrame, minHandSizePx: Float): Boolean {
        val w = frame.frameWidth.toFloat()
        val h = frame.frameHeight.toFloat()
        val handSize = distance(frame, Landmark.WRIST, Landmark.MIDDLE_MCP, w, h)
        if (handSize < minHandSizePx) return false
        rawTipRatio = distance(frame, Landmark.THUMB_TIP, Landmark.INDEX_TIP, w, h) / handSize
        rawFallbackRatio = distance(frame, Landmark.THUMB_TIP, Landmark.INDEX_PIP, w, h) / handSize
        return true
    }

    private fun distance(frame: HandFrame, a: Int, b: Int, w: Float, h: Float): Float {
        val dx = (frame.xs[a] - frame.xs[b]) * w
        val dy = (frame.ys[a] - frame.ys[b]) * h
        return sqrt(dx * dx + dy * dy)
    }
}
