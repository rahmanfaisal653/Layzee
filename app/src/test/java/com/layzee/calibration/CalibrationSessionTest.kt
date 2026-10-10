package com.layzee.calibration

import com.layzee.calibration.CalibrationSession.Kind
import com.layzee.calibration.CalibrationSession.Problem
import com.layzee.calibration.CalibrationSession.Status
import com.layzee.gesture.HandFrame
import com.layzee.gesture.Landmark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Plays synthetic hands into a session and checks the thresholds that come out. */
class CalibrationSessionTest {
    private val session = CalibrationSession()
    private val frame = HandFrame()
    private var clockMs = 0L

    private fun feed(tipGap: Float, pipGap: Float, present: Boolean = true) {
        val size = 100f
        fun put(i: Int, x: Float, y: Float) = frame.setLandmark(i, x / 640f, y / 480f)
        for (i in 0 until Landmark.COUNT) put(i, 320f, 240f)
        put(Landmark.WRIST, 320f, 240f + size / 2)
        put(Landmark.MIDDLE_MCP, 320f, 240f - size / 2)
        put(Landmark.INDEX_TIP, 320f + tipGap * size, 240f)
        put(Landmark.INDEX_PIP, 320f, 240f + pipGap * size)
        frame.handPresent = present
        frame.confidence = 0.9f
        frame.frameWidth = 640
        frame.frameHeight = 480
        frame.timestampMs = clockMs
        clockMs += 50
        session.onFrame(frame)
    }

    /** One ask: frames at 20 fps through settle, window and the first frame past it. */
    private fun ask(tipGap: Float, pipGap: Float, present: Boolean = true) {
        val frames = ((CalibrationSession.SETTLE_MS + CalibrationSession.WINDOW_MS) / 50).toInt() + 1
        repeat(frames) { feed(tipGap, pipGap, present) }
    }

    private fun open() = ask(tipGap = 0.9f, pipGap = 0.9f)
    private fun pinch() = ask(tipGap = 0.1f, pipGap = 0.4f)

    private fun runAllRounds(open: () -> Unit = ::open, pinch: () -> Unit = ::pinch) =
        repeat(CalibrationSession.ROUNDS) { open(); pinch() }

    @Test
    fun asksAlternateOpenThenPinchFiveTimesEach() {
        assertEquals(Kind.OPEN, session.kind)
        assertEquals(1, session.round)
        open()
        assertEquals(Kind.PINCH, session.kind)
        assertEquals(1, session.round)
        pinch()
        assertEquals(Kind.OPEN, session.kind)
        assertEquals(2, session.round)
    }

    @Test
    fun cleanRunSetsThresholdsBetweenPinchAndOpenHand() {
        runAllRounds()
        assertEquals(Status.DONE, session.status)
        val result = session.result!!
        // Pinch 0.1, open 0.9: enter and exit sit inside the gap, enter below exit.
        assertTrue(result.enterRatio > 0.1f && result.exitRatio < 0.9f)
        assertTrue(result.enterRatio < result.exitRatio)
        assertTrue(result.fallbackEnterRatio > 0.4f && result.fallbackExitRatio < 0.9f)
    }

    @Test
    fun loosestPinchAndTightestOpenHandSetTheThresholds() {
        repeat(CalibrationSession.ROUNDS) { round ->
            ask(tipGap = 0.9f - round * 0.05f, pipGap = 0.9f) // tightest open: 0.7
            ask(tipGap = 0.1f + round * 0.02f, pipGap = 0.4f) // loosest pinch: 0.18
        }
        val expected = Calibration.from(pinchHigh = 0.18f, openLow = 0.7f, fallbackPinchHigh = 0.4f, fallbackOpenLow = 0.9f)!!
        assertEquals(expected.enterRatio, session.result!!.enterRatio, 0.01f)
        assertEquals(expected.exitRatio, session.result!!.exitRatio, 0.01f)
    }

    @Test
    fun noHandRepeatsTheAskWithAMessage() {
        ask(0.9f, 0.9f, present = false)
        assertEquals(Problem.NO_HAND, session.problem)
        assertEquals(Kind.OPEN, session.kind)
        assertEquals(1, session.round)
        open()
        assertEquals(Problem.NONE, session.problem)
        assertEquals(Kind.PINCH, session.kind)
    }

    @Test
    fun tooFewFramesAreNotEnough() {
        // A slow camera (a frame every 400 ms): only about two frames fall inside the window.
        repeat(6) {
            feed(0.9f, 0.9f)
            clockMs += 350
        }
        assertEquals(Problem.NO_HAND, session.problem)
        assertEquals(Kind.OPEN, session.kind)
    }

    @Test
    fun aPauseInFramesRestartsTheWindowInsteadOfClosingIt() {
        feed(0.9f, 0.9f)
        clockMs += 5_000 // the pacer rested; the next frame comes long after
        feed(0.9f, 0.9f)
        assertEquals(Problem.NONE, session.problem)
        assertEquals(Kind.OPEN, session.kind)
        open()
        assertEquals(Kind.PINCH, session.kind)
    }

    @Test
    fun anUnsteadyHandRepeatsTheAsk() {
        val frames = ((CalibrationSession.SETTLE_MS + CalibrationSession.WINDOW_MS) / 50).toInt() + 1
        repeat(frames) { i -> feed(tipGap = if (i % 2 == 0) 0.4f else 1.0f, pipGap = 0.9f) }
        assertEquals(Problem.UNSTABLE, session.problem)
        assertEquals(Kind.OPEN, session.kind)
    }

    @Test
    fun framesDuringSettleTimeAreNotMeasured() {
        // The hand is still moving into position during settle; only the window counts.
        val settleFrames = (CalibrationSession.SETTLE_MS / 50).toInt()
        repeat(settleFrames) { feed(tipGap = 0.1f, pipGap = 0.4f) }
        val windowFrames = (CalibrationSession.WINDOW_MS / 50).toInt() + 1
        repeat(windowFrames) { feed(tipGap = 0.9f, pipGap = 0.9f) }
        assertEquals(Problem.NONE, session.problem)
        assertEquals(Kind.PINCH, session.kind)
    }

    @Test
    fun pinchAndOpenHandTooAlikeFailTheRun() {
        runAllRounds(open = { ask(0.3f, 0.5f) }, pinch = { ask(0.2f, 0.4f) })
        assertEquals(Status.FAILED, session.status)
        assertEquals(Problem.TOO_SIMILAR, session.problem)
        assertNull(session.result)
    }

    @Test
    fun fallbackThatDoesNotSeparateIsSwitchedOff() {
        runAllRounds(pinch = { ask(tipGap = 0.1f, pipGap = 0.9f) })
        val result = session.result!!
        assertEquals(0f, result.fallbackEnterRatio, 0f)
        assertEquals(0f, result.fallbackExitRatio, 0f)
        assertNotNull(result)
    }

    @Test
    fun aFinishedSessionIgnoresFurtherFrames() {
        runAllRounds()
        val result = session.result
        ask(0.1f, 0.1f)
        assertEquals(result, session.result)
        assertEquals(Status.DONE, session.status)
    }
}
