package com.layzee.pipeline

import com.layzee.gesture.GestureState
import java.nio.ByteBuffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Drives the pacer with synthetic RGBA frames at a 30 fps camera and checks which frames reach the model. */
class FramePacerTest {
    private val w = 320
    private val h = 240
    private val cfg = PacingConfig()
    private var pacer = FramePacer(cfg)
    private var clockMs = 1_000_000L

    private fun frame(gray: Int, block: Boolean = false): ByteBuffer {
        val buf = ByteBuffer.allocateDirect(w * h * 4)
        for (y in 0 until h) for (x in 0 until w) {
            val v = if (block && x in 100..219 && y in 60..179) 255 else gray
            val i = (y * w + x) * 4
            buf.put(i, v.toByte()); buf.put(i + 1, v.toByte()); buf.put(i + 2, v.toByte()); buf.put(i + 3, 255.toByte())
        }
        return buf
    }

    private val still = frame(80)
    private val hand = frame(80, block = true)

    /** One camera frame (33 ms apart); returns the verdict. */
    private fun tick(buf: ByteBuffer = still): Int {
        val verdict = pacer.admit(clockMs, buf, w * 4, w, h)
        clockMs += 33
        return verdict
    }

    /** Runs [ms] of camera time and returns how many frames were admitted. */
    private fun run(ms: Long, buf: ByteBuffer = still): Int {
        var admitted = 0
        val end = clockMs + ms
        while (clockMs < end) if (tick(buf) == FramePacer.ADMIT) admitted++
        return admitted
    }

    private fun result(state: GestureState) = pacer.onResult(state, clockMs)

    @Test
    fun idleStillSceneSkipsTheModel() {
        tick() // first frame always runs
        val admitted = run(10_000)
        // Only the forced refresh every forcedRunMs gets through, never a per-frame run.
        assertTrue("admitted=$admitted", admitted <= 10_000 / cfg.forcedRunMs + 1)
    }

    @Test
    fun idleMotionWakesTheModel() {
        tick()
        run(1_000)
        // A hand enters: within one idle interval the frame goes to the model.
        var waited = 0
        while (tick(hand) != FramePacer.ADMIT) waited += 33
        assertTrue("waited=$waited", waited <= 1000 / cfg.idleFps)
    }

    @Test
    fun trackingRunsAtTrackingRateWithoutMotionGate() {
        tick()
        result(GestureState.TRACKING)
        val admitted = run(2_000) // still scene, but a hand is tracked
        // 30 fps camera, 20 fps target: the schedule averages 20 fps (about 40 in 2 s).
        assertTrue("admitted=$admitted", admitted in 36..44)
    }

    @Test
    fun pinchRunsFasterThanTracking() {
        tick()
        result(GestureState.PINCH)
        val pinch = run(2_000)
        result(GestureState.TRACKING)
        val tracking = run(2_000)
        assertTrue("pinch=$pinch tracking=$tracking", pinch > tracking)
    }

    @Test
    fun rateStaysHighForHoldTimeAfterHandIsLostThenDrops() {
        tick()
        result(GestureState.TRACKING)
        run(500)
        result(GestureState.IDLE) // hand lost
        val during = run(cfg.holdMs - 500)
        val after = run(3_000)
        assertTrue("during=$during", during >= (cfg.holdMs - 500) / 1000 * 12)
        assertTrue("after=$after", after <= 3)
    }

    @Test
    fun slowDriftEventuallyWakesTheModel() {
        pacer = FramePacer(PacingConfig(forcedRunMs = Long.MAX_VALUE / 4)) // only motion may wake it
        tick(frame(80))
        run(500, frame(80))
        // Brightening by 5 levels per step never exceeds the per-pixel delta against the previous frame,
        // but the reference is the last frame sent to the model, so the change accumulates.
        var level = 80
        var wokeAt = -1
        for (i in 0 until 20) {
            level += 5
            if (run(200, frame(level)) > 0) { wokeAt = level; break }
        }
        assertTrue("wokeAt=$wokeAt", wokeAt > 80 + cfg.motionPixelDelta)
    }

    @Test
    fun appearDelayIsReportedWhenMotionLeadsToTracking() {
        tick()
        run(1_000)
        while (tick(hand) != FramePacer.ADMIT) Unit
        val wakeAt = clockMs - 33
        clockMs += 100
        val delay = pacer.onResult(GestureState.TRACKING, clockMs)
        assertEquals(clockMs - wakeAt, delay)
        // Reported once only.
        assertEquals(-1L, pacer.onResult(GestureState.TRACKING, clockMs + 50))
    }

    @Test
    fun noDelayReportedWhenMotionHadNoHand() {
        tick()
        run(1_000)
        while (tick(hand) != FramePacer.ADMIT) Unit
        result(GestureState.IDLE)
        run(2_000, hand) // scene settles with the object left in place: gate holds again
        assertEquals(-1L, pacer.onResult(GestureState.TRACKING, clockMs))
    }

    @Test
    fun skipsAreReportedByReason() {
        tick()
        var rate = 0
        var motion = 0
        val end = clockMs + 3_000
        while (clockMs < end) when (tick()) {
            FramePacer.SKIP_RATE -> rate++
            FramePacer.SKIP_MOTION -> motion++
        }
        assertTrue(rate > 0)
        assertTrue(motion > 0)
    }

    @Test
    fun paddedRowsAreReadByStride() {
        val stride = w * 4 + 16
        val padded = ByteBuffer.allocateDirect(stride * h)
        tick() // reference from a flat scene
        run(500)
        // Same flat scene with padding must not look like motion.
        for (i in 0 until stride * h) padded.put(i, 80.toByte())
        var admitted = 0
        val end = clockMs + 3_000
        while (clockMs < end) if (pacer.admit(clockMs, padded, stride, w, h).also { clockMs += 33 } == FramePacer.ADMIT) admitted++
        assertTrue("admitted=$admitted", admitted <= 3)
    }
}
