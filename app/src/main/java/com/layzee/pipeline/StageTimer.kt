package com.layzee.pipeline

import android.util.Log

/**
 * Debug-only per-stage timing. Averages over [WINDOW] frames and writes one logcat line per window.
 * Callers guard every use with `BuildConfig.DEBUG`, so R8 removes this class from release builds.
 * Called from the analysis thread and the MediaPipe callback thread, hence the lock; it is not on the
 * release path, so its cost does not matter.
 */
internal object StageTimer {
    const val CAMERA = 0 // sensor timestamp -> analyzer entry
    const val CONVERT = 1 // RGBA buffer -> reused bitmap
    const val INFERENCE = 2 // detectAsync -> result callback
    const val LOGIC = 3 // gesture engine

    private const val TAG = "LayzeeTiming"
    private const val WINDOW = 60
    private val NAMES = arrayOf("camera", "convert", "inference", "logic")

    private val sumNs = LongArray(NAMES.size)
    private val count = IntArray(NAMES.size)
    private var windowStartNs = 0L
    private var frames = 0

    @Synchronized
    fun record(stage: Int, ns: Long) {
        sumNs[stage] += ns
        count[stage]++
        if (stage != LOGIC) return

        // LOGIC is the last stage of a processed frame: close the window here.
        if (windowStartNs == 0L) windowStartNs = System.nanoTime()
        if (++frames < WINDOW) return
        val now = System.nanoTime()
        val sb = StringBuilder()
        sb.append("fps=").append(String.format("%.1f", frames * 1e9 / (now - windowStartNs)))
        for (i in NAMES.indices) {
            val avgMs = if (count[i] == 0) 0.0 else sumNs[i] / count[i] / 1e6
            sb.append(' ').append(NAMES[i]).append('=').append(String.format("%.1f", avgMs)).append("ms")
            sumNs[i] = 0
            count[i] = 0
        }
        Log.d(TAG, sb.toString())
        frames = 0
        windowStartNs = now
    }
}
