package com.layzee.calibration

import com.layzee.gesture.HandFrame
import com.layzee.gesture.PinchMeasure

/**
 * One calibration run, driven frame by frame. Pure: no Android types, no allocation per frame.
 *
 * The user is asked, alternately, to hold an open hand and to hold a pinch, [ROUNDS] times each (open first).
 * Each ask is a window of camera time: the first [SETTLE_MS] let the user get into position, the next
 * [WINDOW_MS] are measured, and the median of the frames with a visible hand is that ask's value. An ask
 * with too few frames or an unsteady hand is repeated and [problem] says why. After the last ask the
 * loosest pinch and tightest open hand set the thresholds ([Calibration.from]); if the two are not clearly
 * apart the run [Status.FAILED] and has to be restarted with a new session.
 *
 * Times are camera frame timestamps ([HandFrame.timestampMs]); the window of an ask starts with its first frame,
 * or with the first frame after a pause in frames.
 */
internal class CalibrationSession {
    enum class Kind { OPEN, PINCH }
    enum class Status { RUNNING, DONE, FAILED }
    enum class Problem { NONE, NO_HAND, UNSTABLE, TOO_SIMILAR }

    var status = Status.RUNNING
        private set

    /** Why the last ask was repeated, or why the run failed; [Problem.NONE] after a clean ask. */
    var problem = Problem.NONE
        private set

    /** Set when [status] is [Status.DONE]. */
    var result: Calibration? = null
        private set

    private var asks = 0 // finished asks; even = open, odd = pinch
    val kind get() = if (asks % 2 == 0) Kind.OPEN else Kind.PINCH

    /** 1-based round of the current ask, for "3 of 5". */
    val round get() = (asks / 2 + 1).coerceAtMost(ROUNDS)

    private val measure = PinchMeasure()
    private val openTip = FloatArray(ROUNDS)
    private val openFallback = FloatArray(ROUNDS)
    private val pinchTip = FloatArray(ROUNDS)
    private val pinchFallback = FloatArray(ROUNDS)

    private var askStartMs = NOT_STARTED
    private var lastFrameMs = 0L
    private var count = 0
    private val windowTip = FloatArray(MAX_SAMPLES)
    private val windowFallback = FloatArray(MAX_SAMPLES)

    fun onFrame(frame: HandFrame) {
        if (status != Status.RUNNING) return
        // Frames stop coming while nothing moves (the pacer rests); a window that spans such a gap is not a measurement.
        if (askStartMs == NOT_STARTED || frame.timestampMs - lastFrameMs > MAX_GAP_MS) {
            askStartMs = frame.timestampMs
            count = 0
        }
        lastFrameMs = frame.timestampMs
        val elapsed = frame.timestampMs - askStartMs
        if (elapsed < SETTLE_MS) return
        if (elapsed < SETTLE_MS + WINDOW_MS) {
            if (frame.handPresent && frame.confidence >= MIN_CONFIDENCE && count < MAX_SAMPLES && measure.measure(frame)) {
                windowTip[count] = measure.tipRatio
                windowFallback[count] = measure.fallbackRatio
                count++
            }
            return
        }
        finishAsk()
    }

    private fun finishAsk() {
        val n = count
        count = 0
        askStartMs = NOT_STARTED
        if (n < MIN_SAMPLES) {
            problem = Problem.NO_HAND
            return
        }
        if (spread(windowTip, n) > MAX_SPREAD) {
            problem = Problem.UNSTABLE
            return
        }
        problem = Problem.NONE
        val slot = asks / 2
        if (kind == Kind.OPEN) {
            openTip[slot] = median(windowTip, n)
            openFallback[slot] = median(windowFallback, n)
        } else {
            pinchTip[slot] = median(windowTip, n)
            pinchFallback[slot] = median(windowFallback, n)
        }
        asks++
        if (asks == ROUNDS * 2) conclude()
    }

    private fun conclude() {
        val calibration = Calibration.from(
            pinchHigh = pinchTip.max(),
            openLow = openTip.min(),
            fallbackPinchHigh = pinchFallback.max(),
            fallbackOpenLow = openFallback.min(),
        )
        if (calibration == null) {
            status = Status.FAILED
            problem = Problem.TOO_SIMILAR
        } else {
            status = Status.DONE
            result = calibration
        }
    }

    private fun spread(values: FloatArray, n: Int): Float {
        var lo = values[0]
        var hi = values[0]
        for (i in 1 until n) {
            if (values[i] < lo) lo = values[i]
            if (values[i] > hi) hi = values[i]
        }
        return hi - lo
    }

    // Sorts the first n values in place (insertion sort: n is small).
    private fun median(values: FloatArray, n: Int): Float {
        for (i in 1 until n) {
            val v = values[i]
            var j = i - 1
            while (j >= 0 && values[j] > v) {
                values[j + 1] = values[j]
                j--
            }
            values[j + 1] = v
        }
        return if (n % 2 == 1) values[n / 2] else (values[n / 2 - 1] + values[n / 2]) / 2f
    }

    companion object {
        const val ROUNDS = 5
        const val SETTLE_MS = 700L
        private const val MAX_GAP_MS = 500L
        const val WINDOW_MS = 1000L
        private const val MIN_SAMPLES = 4
        private const val MAX_SAMPLES = 64
        /** Tip distance may wander this much (hand sizes) inside one window before the ask counts as unsteady. */
        private const val MAX_SPREAD = 0.3f
        private const val MIN_CONFIDENCE = 0.5f
        private const val NOT_STARTED = Long.MIN_VALUE
    }
}
