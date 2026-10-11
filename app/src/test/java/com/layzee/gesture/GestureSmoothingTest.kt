package com.layzee.gesture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The pinch ratios are smoothed (One Euro) with the default tuning; jitter must not pinch, a real pinch must. */
class GestureSmoothingTest {
    private val config = GestureConfig(enterRatio = 0.25f, exitRatio = 0.40f, fallbackEnterRatio = 0.30f, fallbackExitRatio = 0.50f)
    private val rawConfig = GestureConfig(
        enterRatio = 0.25f,
        exitRatio = 0.40f,
        fallbackEnterRatio = 0.30f,
        fallbackExitRatio = 0.50f,
        smoothingMinCutoffHz = 1000f,
        smoothingBeta = 0f,
    )

    private val frame = HandFrame()
    private val out = GestureOutput()
    private var clockMs = 0L

    private fun feed(
        engine: GestureEngine,
        cfg: GestureConfig,
        tipGap: Float,
        pipGap: Float,
        handSize: Float = 100f,
        stepMs: Long = 33,
    ): GestureState {
        val width = 640
        val height = 480
        val cx = 320f
        val cy = 240f
        fun put(i: Int, px: Float, py: Float) = frame.setLandmark(i, px / width, py / height)
        for (i in 0 until Landmark.COUNT) put(i, cx, cy)
        put(Landmark.WRIST, cx, cy + handSize / 2)
        put(Landmark.MIDDLE_MCP, cx, cy - handSize / 2)
        put(Landmark.INDEX_TIP, cx + tipGap * handSize, cy)
        put(Landmark.INDEX_PIP, cx, cy + pipGap * handSize)
        frame.handPresent = true
        frame.confidence = 0.9f
        frame.frameWidth = width
        frame.frameHeight = height
        frame.timestampMs = clockMs
        clockMs += stepMs
        engine.process(frame, cfg, out)
        return out.state
    }

    /** Open hand, then brief dips of [dip] frames into pinch range: what landmark jitter on a far hand looks like. */
    private fun runDips(cfg: GestureConfig, dipFrames: Int, handSize: Float): Boolean {
        val engine = GestureEngine()
        repeat(3) { feed(engine, cfg, 0.5f, 1.0f, handSize) }
        repeat(20) {
            repeat(dipFrames) { if (feed(engine, cfg, 0.15f, 0.2f, handSize) == GestureState.PINCH) return true }
            repeat(3) { if (feed(engine, cfg, 0.5f, 1.0f, handSize) == GestureState.PINCH) return true }
        }
        return false
    }

    @Test
    fun threeFrameJitterOnASmallHandPinchesWithoutSmoothing() {
        // Sanity check of the next test: the same input does pinch when the filter is off.
        assertTrue(runDips(rawConfig, dipFrames = 3, handSize = 32f))
    }

    @Test
    fun threeFrameJitterOnASmallHandDoesNotPinch() {
        assertTrue(!runDips(config, dipFrames = 3, handSize = 32f))
    }

    @Test
    fun aRealPinchIsStillDetectedAndQuickly() {
        val engine = GestureEngine()
        repeat(5) { feed(engine, config, 1.0f, 1.0f) }
        // Fingers close over about 200 ms (6 frames), then hold.
        var framesToPinch = -1
        for (i in 1..20) {
            val t = (i / 6f).coerceAtMost(1f)
            val state = feed(engine, config, 1.0f - 0.9f * t, 1.0f - 0.8f * t)
            if (state == GestureState.PINCH && framesToPinch < 0) framesToPinch = i
        }
        assertTrue("pinch not detected", framesToPinch > 0)
        // Closing takes 6 frames; the filter and the 3-frame debounce may add a few more, not many.
        assertTrue("detected after $framesToPinch frames", framesToPinch <= 10)
    }

    @Test
    fun aStepPinchIsDetectedWithinSixFrames() {
        val engine = GestureEngine()
        repeat(5) { feed(engine, config, 1.0f, 1.0f) }
        var framesToPinch = -1
        for (i in 1..10) {
            if (feed(engine, config, 0.1f, 0.2f) == GestureState.PINCH && framesToPinch < 0) framesToPinch = i
        }
        assertTrue("detected after $framesToPinch frames", framesToPinch in 1..6)
    }

    @Test
    fun aPinchIsDetectedAtTheIdleFrameRateToo() {
        // Frames 200 ms apart (5 fps): the time step comes from the timestamps, not a fixed rate.
        val engine = GestureEngine()
        repeat(3) { feed(engine, config, 1.0f, 1.0f, stepMs = 200) }
        var state = GestureState.TRACKING
        // The filter trails the first frame at this rate, so one frame more than the debounce is allowed.
        repeat(4) { state = feed(engine, config, 0.1f, 0.2f, stepMs = 200) }
        assertEquals(GestureState.PINCH, state)
    }

    @Test
    fun aPinchReleasesAfterOpening() {
        val engine = GestureEngine()
        repeat(3) { feed(engine, config, 1.0f, 1.0f) }
        repeat(8) { feed(engine, config, 0.1f, 0.2f) }
        assertEquals(GestureState.PINCH, out.state)
        repeat(8) { feed(engine, config, 1.0f, 1.0f) }
        assertEquals(GestureState.TRACKING, out.state)
    }
}
