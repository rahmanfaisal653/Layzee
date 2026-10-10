package com.layzee.pipeline

import android.util.Log

/**
 * Debug-only per-stage timing. Averages over [WINDOW] processed frames, or [MAX_WINDOW_NS] when frames come
 * slower (Idle), and writes one logcat line per window, with how many frames the pacer dropped.
 * Callers guard every use with `BuildConfig.DEBUG`, so R8 removes this class from release builds.
 * Called from the analysis thread and the MediaPipe callback thread, hence the lock; it is not on the
 * release path, so its cost does not matter.
 */
internal object StageTimer {
    const val CAMERA = 0 // sensor timestamp -> analyzer entry
    const val CONVERT = 1 // RGBA buffer -> reused bitmap
    const val INFERENCE = 2 // detectAsync -> result callback
    const val LOGIC = 3 // gesture engine
    const val DISPATCH = 4 // dispatcher call for a swipe command (only frames that send one)

    private const val TAG = "LayzeeTiming"
    private const val WINDOW = 60
    private const val MAX_WINDOW_NS = 5_000_000_000L
    private val NAMES = arrayOf("camera", "convert", "inference", "logic", "dispatch")

    private val sumNs = LongArray(NAMES.size)
    private val count = IntArray(NAMES.size)
    private var windowStartNs = 0L
    private var frames = 0
    private var rateSkips = 0
    private var motionSkips = 0

    @Synchronized
    fun record(stage: Int, ns: Long) {
        sumNs[stage] += ns
        count[stage]++
        // LOGIC is the last stage of a processed frame: count it and maybe close the window.
        if (stage == LOGIC) {
            frames++
            flushIfDue()
        }
    }

    /** A frame the pacer dropped before conversion: [FramePacer.SKIP_RATE] or [FramePacer.SKIP_MOTION]. */
    @Synchronized
    fun skipped(verdict: Int) {
        if (verdict == FramePacer.SKIP_MOTION) motionSkips++ else rateSkips++
        flushIfDue()
    }

    private fun flushIfDue() {
        val now = System.nanoTime()
        if (windowStartNs == 0L) windowStartNs = now
        if (frames < WINDOW && now - windowStartNs < MAX_WINDOW_NS) return
        val sb = StringBuilder()
        sb.append("modelFps=").append(String.format("%.1f", frames * 1e9 / (now - windowStartNs)))
        for (i in NAMES.indices) {
            val avgMs = if (count[i] == 0) 0.0 else sumNs[i] / count[i] / 1e6
            sb.append(' ').append(NAMES[i]).append('=').append(String.format("%.1f", avgMs)).append("ms")
            sumNs[i] = 0
            count[i] = 0
        }
        sb.append(" skipped(rate=").append(rateSkips).append(" motion=").append(motionSkips).append(')')
        Log.d(TAG, sb.toString())
        frames = 0
        rateSkips = 0
        motionSkips = 0
        windowStartNs = now
    }
}
