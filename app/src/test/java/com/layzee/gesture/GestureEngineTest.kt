package com.layzee.gesture

import org.junit.Assert.assertEquals
import org.junit.Test

/** Drives the engine with synthetic frames and checks only the state that comes out. */
class GestureEngineTest {
    private val config = GestureConfig(
        enterRatio = 0.25f,
        exitRatio = 0.40f,
        fallbackEnterRatio = 0.30f,
        fallbackExitRatio = 0.50f,
        minConfidence = 0.5f,
        enterFrames = 2,
        releaseFrames = 2,
    )

    private var engine = GestureEngine()
    private val frame = HandFrame()
    private val out = GestureOutput()
    private var clockMs = 0L

    /**
     * A hand of [handSize] pixels (wrist to middle MCP, pointing up) centred at ([cx], [cy]).
     * [tipGap] is thumb tip to index tip and [pipGap] is thumb tip to index PIP, both in hand sizes.
     * Defaults describe an open hand.
     */
    private fun feed(
        tipGap: Float = 1.0f,
        pipGap: Float = 1.0f,
        handSize: Float = 100f,
        cx: Float = 320f,
        cy: Float = 240f,
        width: Int = 640,
        height: Int = 480,
        confidence: Float = 0.9f,
    ): GestureState {
        fun put(i: Int, px: Float, py: Float) = frame.setLandmark(i, px / width, py / height)
        for (i in 0 until Landmark.COUNT) put(i, cx, cy)
        put(Landmark.WRIST, cx, cy + handSize / 2)
        put(Landmark.MIDDLE_MCP, cx, cy - handSize / 2)
        put(Landmark.THUMB_TIP, cx, cy)
        put(Landmark.INDEX_TIP, cx + tipGap * handSize, cy)
        put(Landmark.INDEX_PIP, cx, cy + pipGap * handSize)
        frame.handPresent = true
        frame.confidence = confidence
        frame.frameWidth = width
        frame.frameHeight = height
        frame.timestampMs = clockMs
        clockMs += 33
        engine.process(frame, config, out)
        return out.state
    }

    private fun feedNoHand(): GestureState {
        frame.handPresent = false
        frame.timestampMs = clockMs
        clockMs += 33
        engine.process(frame, config, out)
        return out.state
    }

    private fun pinch() = feed(tipGap = 0.1f, pipGap = 0.6f)
    private fun open() = feed()

    private fun enterPinch() {
        open()
        pinch()
        assertEquals(GestureState.PINCH, pinch())
    }

    @Test
    fun noHandIsIdle() {
        assertEquals(GestureState.IDLE, feedNoHand())
    }

    @Test
    fun visibleOpenHandIsTracking() {
        assertEquals(GestureState.TRACKING, open())
    }

    @Test
    fun heldPinchBecomesPinch() {
        open()
        pinch()
        assertEquals(GestureState.PINCH, pinch())
    }

    @Test
    fun releasedPinchReturnsToTracking() {
        enterPinch()
        open()
        assertEquals(GestureState.TRACKING, open())
    }

    @Test
    fun handLostDuringPinchGoesIdle() {
        enterPinch()
        assertEquals(GestureState.IDLE, feedNoHand())
    }

    @Test
    fun handLostWhileTrackingGoesIdle() {
        open()
        assertEquals(GestureState.IDLE, feedNoHand())
    }

    @Test
    fun sameHandAtDifferentCameraDistancesGivesSameDecisions() {
        for ((handSize, cx, cy) in listOf(Triple(200f, 320f, 240f), Triple(40f, 100f, 80f))) {
            engine = GestureEngine()
            assertEquals(GestureState.TRACKING, feed(handSize = handSize, cx = cx, cy = cy))
            feed(tipGap = 0.1f, pipGap = 0.6f, handSize = handSize, cx = cx, cy = cy)
            assertEquals(
                GestureState.PINCH,
                feed(tipGap = 0.1f, pipGap = 0.6f, handSize = handSize, cx = cx, cy = cy),
            )
            feed(handSize = handSize, cx = cx, cy = cy)
            assertEquals(GestureState.TRACKING, feed(handSize = handSize, cx = cx, cy = cy))
        }
    }

    @Test
    fun distancesAreMeasuredInPixelsNotNormalizedUnits() {
        // Gap is 0.30 hand sizes horizontally: not a pinch (enter 0.25). Using raw normalized
        // coordinates on a 640x480 frame would shrink it to 0.225 and wrongly pinch.
        open()
        repeat(5) { assertEquals(GestureState.TRACKING, feed(tipGap = 0.30f, pipGap = 0.6f)) }
    }

    @Test
    fun singleFramePinchDoesNotChangeState() {
        open()
        assertEquals(GestureState.TRACKING, pinch())
        assertEquals(GestureState.TRACKING, open())
        assertEquals(GestureState.TRACKING, pinch())
        assertEquals(GestureState.TRACKING, open())
    }

    @Test
    fun singleFrameReleaseDoesNotLeavePinch() {
        enterPinch()
        assertEquals(GestureState.PINCH, open())
        assertEquals(GestureState.PINCH, pinch())
    }

    @Test
    fun gapFlickeringBetweenThresholdsDoesNotFlickerPinch() {
        enterPinch()
        // 0.20 is inside enter, 0.35 is between enter and exit: both keep Pinch.
        repeat(10) {
            assertEquals(GestureState.PINCH, feed(tipGap = 0.35f, pipGap = 0.6f))
            assertEquals(GestureState.PINCH, feed(tipGap = 0.20f, pipGap = 0.6f))
        }
    }

    @Test
    fun gapInsideHysteresisBandDoesNotStartPinch() {
        open()
        repeat(10) { assertEquals(GestureState.TRACKING, feed(tipGap = 0.35f, pipGap = 0.6f)) }
    }

    @Test
    fun gapPastExitReleasesPinch() {
        enterPinch()
        feed(tipGap = 0.45f, pipGap = 0.6f)
        assertEquals(GestureState.TRACKING, feed(tipGap = 0.45f, pipGap = 0.6f))
    }

    @Test
    fun lowConfidenceDuringPinchLeavesPinchImmediately() {
        enterPinch()
        assertEquals(GestureState.IDLE, feed(tipGap = 0.1f, pipGap = 0.6f, confidence = 0.2f))
    }

    @Test
    fun pinchAfterLossNeedsDebounceAgain() {
        enterPinch()
        feedNoHand()
        assertEquals(GestureState.TRACKING, pinch())
        assertEquals(GestureState.PINCH, pinch())
    }

    @Test
    fun hiddenIndexTipStillReadsAsPinchThroughFallback() {
        // Index tip estimate is far from the thumb, but the thumb touches the index PIP.
        open()
        feed(tipGap = 0.8f, pipGap = 0.2f)
        assertEquals(GestureState.PINCH, feed(tipGap = 0.8f, pipGap = 0.2f))
    }

    @Test
    fun fallbackAloneStaysPinchedWhileTipEstimateWobbles() {
        enterPinch()
        repeat(5) { assertEquals(GestureState.PINCH, feed(tipGap = 0.8f, pipGap = 0.2f)) }
    }

    @Test
    fun farTipAndFarPipIsNotPinch() {
        open()
        repeat(5) { assertEquals(GestureState.TRACKING, feed(tipGap = 0.8f, pipGap = 0.6f)) }
    }

    @Test
    fun fallbackReleasesOnlyWhenBothMeasuresAreApart() {
        enterPinch()
        // Tip is far but the thumb still touches the PIP (0.4 is inside the fallback band): stays pinched.
        repeat(5) { assertEquals(GestureState.PINCH, feed(tipGap = 0.8f, pipGap = 0.4f)) }
        feed(tipGap = 0.8f, pipGap = 0.6f)
        assertEquals(GestureState.TRACKING, feed(tipGap = 0.8f, pipGap = 0.6f))
    }

    @Test
    fun verticalGapIsMeasuredInPixelsToo() {
        // Index tip directly above the thumb by 0.30 hand sizes on a wide frame: not a pinch.
        // Normalized y-units would inflate it relative to x and the result would differ.
        val width = 640
        val height = 480
        val handSize = 100f
        fun put(i: Int, px: Float, py: Float) = frame.setLandmark(i, px / width, py / height)
        repeat(5) {
            for (i in 0 until Landmark.COUNT) put(i, 320f, 240f)
            put(Landmark.WRIST, 320f, 240f + handSize / 2)
            put(Landmark.MIDDLE_MCP, 320f, 240f - handSize / 2)
            put(Landmark.INDEX_TIP, 320f, 240f - 0.30f * handSize)
            put(Landmark.INDEX_PIP, 320f + 0.6f * handSize, 240f)
            frame.handPresent = true
            frame.confidence = 0.9f
            frame.frameWidth = width
            frame.frameHeight = height
            engine.process(frame, config, out)
            assertEquals(GestureState.TRACKING, out.state)
        }
    }

    @Test
    fun lowConfidenceWhileTrackingGoesIdle() {
        open()
        assertEquals(GestureState.IDLE, feed(confidence = 0.2f))
    }
}
