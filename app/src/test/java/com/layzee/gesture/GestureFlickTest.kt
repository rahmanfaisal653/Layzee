package com.layzee.gesture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Drives the engine with synthetic frames and checks whether a flick comes out: pinch = one flick. */
class GestureFlickTest {
    private val screenW = 1000
    private val screenH = 2000
    // Smoothing off: these tests check the flick, not the filter. See GestureSmoothingTest.
    private val cfg = GestureConfig(
        screenWidth = screenW,
        screenHeight = screenH,
        startYFraction = 0.65f,
        smoothingMinCutoffHz = 1000f,
        smoothingBeta = 0f,
    )

    private val engine = GestureEngine()
    private val frame = HandFrame()
    private val out = GestureOutput()
    private var clockMs = 0L

    /** A hand of 100 px (wrist to middle MCP) in a 640x480 frame; [tipGap] is thumb tip to index tip in hand sizes. */
    private fun feed(cy: Float = 240f, tipGap: Float = 0.1f, pipGap: Float = 0.2f, confidence: Float = 0.9f): Boolean {
        val width = 640
        val height = 480
        val cx = 320f
        val handSize = 100f
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
        engine.process(frame, cfg, out)
        return out.flick
    }

    private fun feedNoHand(): Boolean {
        frame.handPresent = false
        frame.timestampMs = clockMs
        clockMs += 33
        engine.process(frame, cfg, out)
        return out.flick
    }

    private fun open() = feed(tipGap = 1.0f, pipGap = 1.0f)

    /** Pinches until the flick fires. */
    private fun pinch() {
        repeat(cfg.enterFrames - 1) { assertFalse(feed()) }
        assertTrue(feed())
    }

    private fun release() {
        open()
        open()
    }

    @Test
    fun threeFramesInARowAreNeededByDefault() {
        assertEquals(3, GestureConfig().enterFrames)
    }

    @Test
    fun noCommandsWhileNotPinching() {
        assertFalse(feedNoHand())
        repeat(5) { assertFalse(open()) }
    }

    @Test
    fun pinchFlicksUpFromTheFixedPointAfterDebounce() {
        open()
        pinch()
        assertEquals(GestureState.PINCH, out.state)
        assertEquals(screenW / 2f, out.x, 0.01f)
        assertEquals(screenH * 0.65f, out.y, 0.01f)
        assertEquals(screenH * (0.65f - 0.45f), out.toY, 0.01f)
    }

    @Test
    fun flickStartPointDoesNotDependOnWhereTheHandIs() {
        open()
        repeat(cfg.enterFrames - 1) { feed(cy = 100f) }
        assertTrue(feed(cy = 100f))
        assertEquals(screenH * 0.65f, out.y, 0.01f)
    }

    @Test
    fun heldPinchFlicksOnlyOnce() {
        open()
        pinch()
        repeat(60) { assertFalse(feed()) }
        assertEquals(GestureState.PINCH, out.state)
    }

    @Test
    fun handMovementDuringPinchIsNotRead() {
        open()
        pinch()
        repeat(20) { i -> assertFalse(feed(cy = 240f - i * 10f)) }
    }

    @Test
    fun releaseAndPinchAgainFlicksAgain() {
        open()
        pinch()
        release()
        assertEquals(GestureState.TRACKING, out.state)
        pinch()
    }

    @Test
    fun briefReleaseBlipDoesNotAllowASecondFlick() {
        open()
        pinch()
        assertFalse(open())
        repeat(5) { assertFalse(feed()) }
    }

    @Test
    fun handLostWhilePinchedAndBackStillPinchedDoesNotFlickAgain() {
        open()
        pinch()
        assertFalse(feedNoHand())
        assertEquals(GestureState.IDLE, out.state)
        repeat(10) { assertFalse(feed()) }
        assertEquals(GestureState.TRACKING, out.state)
    }

    @Test
    fun handLostThenBackOpenAllowsTheNextPinch() {
        open()
        pinch()
        feedNoHand()
        release()
        pinch()
    }

    @Test
    fun lowConfidencePinchDoesNotFlick() {
        open()
        repeat(5) { assertFalse(feed(confidence = 0.2f)) }
    }
}
