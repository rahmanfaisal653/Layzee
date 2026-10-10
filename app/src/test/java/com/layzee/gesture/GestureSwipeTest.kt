package com.layzee.gesture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/** Drives the engine with synthetic frames and checks the swipe commands that come out. */
class GestureSwipeTest {
    private val screenW = 1000
    private val screenH = 2000
    private val ratio = 0.5f
    private val startX = screenW / 2f
    private val startY = screenH * 0.65f

    private fun config(swipeRatio: Float = ratio) = GestureConfig(
        screenWidth = screenW,
        screenHeight = screenH,
        startYFraction = 0.65f,
        swipeRatio = swipeRatio,
    )

    private var cfg = config()
    private var engine = GestureEngine()
    private val frame = HandFrame()
    private val out = GestureOutput()
    private var clockMs = 0L

    /** Pixels the finger moves per hand size of hand movement. */
    private val pxPerHandSize get() = cfg.swipeRatio * screenH

    /**
     * A hand of [handSize] pixels (wrist to middle MCP) centred at ([cx], [cy]) in a 640x480 frame.
     * [tipGap] is thumb tip to index tip in hand sizes; the pinch point is their midpoint.
     */
    private fun feed(
        cy: Float = 240f,
        cx: Float = 320f,
        handSize: Float = 100f,
        tipGap: Float = 0.1f,
        confidence: Float = 0.9f,
        touched: Boolean = false,
        timestampMs: Long = clockMs,
    ): SwipeCommand {
        val width = 640
        val height = 480
        fun put(i: Int, px: Float, py: Float) = frame.setLandmark(i, px / width, py / height)
        for (i in 0 until Landmark.COUNT) put(i, cx, cy)
        put(Landmark.WRIST, cx, cy + handSize / 2)
        put(Landmark.MIDDLE_MCP, cx, cy - handSize / 2)
        put(Landmark.THUMB_TIP, cx, cy)
        put(Landmark.INDEX_TIP, cx + tipGap * handSize, cy)
        put(Landmark.INDEX_PIP, cx, cy + 0.6f * handSize)
        frame.handPresent = true
        frame.confidence = confidence
        frame.frameWidth = width
        frame.frameHeight = height
        frame.screenTouched = touched
        frame.timestampMs = timestampMs
        clockMs = timestampMs + 33
        engine.process(frame, cfg, out)
        return out.command
    }

    private fun feedNoHand(): SwipeCommand {
        frame.handPresent = false
        frame.screenTouched = false
        frame.timestampMs = clockMs
        clockMs += 33
        engine.process(frame, cfg, out)
        return out.command
    }

    private fun open(cy: Float = 240f) = feed(cy = cy, tipGap = 1.0f)

    /** Opens, then pinches until START is emitted. */
    private fun startSwipe(cy: Float = 240f, handSize: Float = 100f) {
        open(cy)
        assertEquals(SwipeCommand.NONE, feed(cy = cy, handSize = handSize))
        assertEquals(SwipeCommand.START, feed(cy = cy, handSize = handSize))
    }

    /** Releases the pinch (two frames) and returns the second frame's command. */
    private fun release(cy: Float = 240f): SwipeCommand {
        feed(cy = cy, tipGap = 1.0f)
        return feed(cy = cy, tipGap = 1.0f)
    }

    /** Holds the hand still long enough for the One Euro filter to settle, then returns the finger y. */
    private fun settledY(cy: Float, handSize: Float = 100f, frames: Int = 40): Float {
        repeat(frames) { feed(cy = cy, handSize = handSize) }
        assertEquals(SwipeCommand.MOVE, out.command)
        return out.y
    }

    @Test
    fun noCommandsWhileNotPinching() {
        assertEquals(SwipeCommand.NONE, feedNoHand())
        repeat(5) { assertEquals(SwipeCommand.NONE, open()) }
    }

    @Test
    fun pinchStartsAtFixedPointAfterDebounce() {
        open()
        assertEquals(SwipeCommand.NONE, feed(cy = 100f))
        assertEquals(SwipeCommand.START, feed(cy = 100f))
        assertEquals(startX, out.x, 0.01f)
        assertEquals(startY, out.y, 0.01f)
    }

    @Test
    fun startPointDoesNotDependOnWhereTheHandIs() {
        startSwipe(cy = 400f)
        assertEquals(startX, out.x, 0.01f)
        assertEquals(startY, out.y, 0.01f)
    }

    @Test
    fun heldPinchEmitsMoveEveryNewFrame() {
        startSwipe()
        repeat(5) { assertEquals(SwipeCommand.MOVE, feed()) }
    }

    @Test
    fun frameWithoutNewDataEmitsNothing() {
        startSwipe()
        assertEquals(SwipeCommand.MOVE, feed())
        val sameTimestamp = frame.timestampMs
        assertEquals(SwipeCommand.NONE, feed(timestampMs = sameTimestamp))
    }

    @Test
    fun handMovingUpMovesFingerUp() {
        startSwipe(cy = 240f)
        val y = settledY(cy = 140f) // one hand size up
        assertTrue("finger should be above the start point, was $y", y < startY)
        assertEquals(startX, out.x, 0.01f)
    }

    @Test
    fun handMovingDownMovesFingerDown() {
        startSwipe(cy = 240f)
        val y = settledY(cy = 340f)
        assertTrue("finger should be below the start point, was $y", y > startY)
    }

    @Test
    fun upThenDownInOnePinchAreBothFollowed() {
        startSwipe(cy = 240f)
        val up = settledY(cy = 140f)
        val back = settledY(cy = 240f)
        assertTrue(up < startY - 100f)
        assertEquals(startY, back, 5f)
        val down = settledY(cy = 340f)
        assertTrue(down > startY + 100f)
    }

    @Test
    fun fingerMovesRatioTimesScreenHeightPerHandSize() {
        startSwipe(cy = 240f)
        val y = settledY(cy = 140f) // exactly one hand size up
        assertEquals(startY - pxPerHandSize, y, pxPerHandSize * 0.02f)
    }

    @Test
    fun sameMotionAtDifferentCameraDistancesGivesSameSwipe() {
        val results = listOf(200f, 50f).map { handSize ->
            engine = GestureEngine()
            startSwipe(cy = 240f, handSize = handSize)
            settledY(cy = 240f - handSize, handSize = handSize) // one hand size up
        }
        assertEquals(results[0], results[1], pxPerHandSize * 0.02f)
    }

    @Test
    fun doublingSwipeRatioDoublesSwipeDistance() {
        val distances = listOf(0.25f, 0.5f).map { swipeRatio ->
            cfg = config(swipeRatio)
            engine = GestureEngine()
            startSwipe(cy = 240f)
            startY - settledY(cy = 190f) // half a hand size up
        }
        assertEquals(distances[0] * 2f, distances[1], distances[1] * 0.02f)
    }

    @Test
    fun onlyVerticalMovementCounts() {
        startSwipe()
        repeat(40) { i -> feed(cx = 320f + i * 5f) } // drifts 200 px sideways
        assertEquals(SwipeCommand.MOVE, out.command)
        assertEquals(startX, out.x, 0.01f)
        assertEquals(startY, out.y, 1f)
    }

    @Test
    fun fingerStaysOnScreen() {
        startSwipe(cy = 240f)
        settledY(cy = 240f - 400f)
        assertTrue(out.y >= 0f)
        settledY(cy = 240f + 400f)
        assertTrue(out.y <= screenH.toFloat())
    }

    @Test
    fun jitterOnAStillHandIsSmoothedAwayFromTheFinger() {
        startSwipe(cy = 240f)
        val noise = 2f // px, 0.02 hand size
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        repeat(60) { i ->
            feed(cy = 240f + if (i % 2 == 0) noise else -noise)
            if (i >= 20) {
                minY = minOf(minY, out.y)
                maxY = maxOf(maxY, out.y)
            }
        }
        val rawRange = 2f * (noise / 100f) * pxPerHandSize
        assertTrue("filtered range ${maxY - minY} vs raw $rawRange", maxY - minY < rawRange * 0.4f)
    }

    @Test
    fun steadyMotionIsFollowedWithoutMeaningfulLag() {
        startSwipe(cy = 240f)
        val stepPx = 1.5f // 0.015 hand size per 33 ms frame = ~0.45 hand sizes per second
        var lagPx = 0f
        for (i in 1..50) {
            feed(cy = 240f - i * stepPx)
            lagPx = (startY - i * stepPx / 100f * pxPerHandSize) - out.y
        }
        val speedPxPerSec = stepPx / 100f * pxPerHandSize / 0.033f
        // Finger is at most ~150 ms behind the raw hand position.
        assertTrue("lag $lagPx px at $speedPxPerSec px/s", abs(lagPx) < speedPxPerSec * 0.15f)
    }

    @Test
    fun releaseWhileMovingLiftsWithLastVelocity() {
        startSwipe(cy = 240f)
        var cy = 240f
        repeat(15) { cy -= 3f; feed(cy = cy) } // moving up
        val lastMoveY = out.y
        feed(cy = cy, tipGap = 1.0f)
        assertEquals(SwipeCommand.LIFT, feed(cy = cy, tipGap = 1.0f))
        assertEquals(lastMoveY, out.y, 0.01f)
        assertEquals(startX, out.x, 0.01f)
        // 3 px per 33 ms frame = 0.03 hand sizes -> ~909 px/s at the finger; smoothing lags a little.
        assertTrue("lift velocity should be about -900 px/s, was ${out.velocityY}", out.velocityY in -1000f..-600f)
    }

    @Test
    fun releaseOfStillHandLiftsWithNearZeroVelocity() {
        startSwipe()
        repeat(30) { feed() }
        assertEquals(SwipeCommand.LIFT, release())
        assertEquals(0f, out.velocityY, 20f)
    }

    @Test
    fun releaseDebounceFramesDoNotMoveTheFinger() {
        startSwipe()
        repeat(5) { feed() }
        // First release frame: fingers opening, nothing is sent yet.
        assertEquals(SwipeCommand.NONE, feed(tipGap = 1.0f))
    }

    @Test
    fun singleFrameReleaseBlipKeepsTheSwipeGoing() {
        startSwipe()
        repeat(3) { feed() }
        assertEquals(SwipeCommand.NONE, feed(tipGap = 1.0f))
        assertEquals(SwipeCommand.MOVE, feed())
    }

    @Test
    fun handLostDuringPinchLiftsAtOnce() {
        startSwipe()
        var cy = 240f
        repeat(10) { cy -= 3f; feed(cy = cy) }
        val lastMoveY = out.y
        assertEquals(SwipeCommand.LIFT, feedNoHand())
        assertEquals(GestureState.IDLE, out.state)
        assertEquals(lastMoveY, out.y, 0.01f)
        assertTrue(out.velocityY < -100f)
    }

    @Test
    fun lowConfidenceDuringPinchLiftsAtOnce() {
        startSwipe()
        repeat(3) { feed() }
        assertEquals(SwipeCommand.LIFT, feed(confidence = 0.2f))
    }

    @Test
    fun noLiftWhenHandIsLostWhileOnlyTracking() {
        open()
        assertEquals(SwipeCommand.NONE, feedNoHand())
    }

    @Test
    fun screenTouchDuringPinchCancelsAndReturnsToTracking() {
        startSwipe()
        repeat(3) { feed() }
        assertEquals(SwipeCommand.CANCEL, feed(touched = true))
        assertEquals(GestureState.TRACKING, out.state)
    }

    @Test
    fun afterCancelHeldPinchDoesNotStartANewSwipe() {
        startSwipe()
        feed(touched = true)
        repeat(10) {
            assertEquals(SwipeCommand.NONE, feed())
            assertEquals(GestureState.TRACKING, out.state)
        }
    }

    @Test
    fun afterCancelNewSwipeStartsOnlyAfterReleaseAndPinchAgain() {
        startSwipe()
        feed(touched = true)
        repeat(3) { feed() }
        assertEquals(SwipeCommand.NONE, release())
        assertEquals(SwipeCommand.NONE, feed())
        assertEquals(SwipeCommand.START, feed())
        assertEquals(GestureState.PINCH, out.state)
    }

    @Test
    fun afterCancelLosingTheHandDoesNotCountAsReleasingThePinch() {
        startSwipe()
        feed(touched = true)
        feedNoHand()
        repeat(5) { assertEquals(SwipeCommand.NONE, feed()) }
        assertEquals(GestureState.TRACKING, out.state)
        release()
        feed()
        assertEquals(SwipeCommand.START, feed())
    }

    @Test
    fun screenTouchWithoutPinchIsIgnored() {
        open()
        assertEquals(SwipeCommand.NONE, feed(tipGap = 1.0f, touched = true))
        assertEquals(SwipeCommand.NONE, feed())
        assertEquals(SwipeCommand.START, feed())
    }

    @Test
    fun screenTouchAndHandLostInSameFrameCancelsWithoutLift() {
        startSwipe()
        frame.handPresent = false
        frame.screenTouched = true
        frame.timestampMs = clockMs
        engine.process(frame, cfg, out)
        assertEquals(SwipeCommand.CANCEL, out.command)
        assertEquals(GestureState.IDLE, out.state)
    }

    @Test
    fun secondSwipeStartsFromTheFixedPointAgain() {
        startSwipe()
        settledY(cy = 140f)
        release(cy = 140f)
        open(cy = 140f)
        feed(cy = 140f)
        assertEquals(SwipeCommand.START, feed(cy = 140f))
        assertEquals(startY, out.y, 0.01f)
    }
}
