package com.layzee.calibration

import android.content.Context

/** The last finished [Calibration] in SharedPreferences, on the device only. Null until one has finished. */
class CalibrationStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("layzee_calibration", Context.MODE_PRIVATE)

    var calibration: Calibration?
        get() = if (!prefs.contains(ENTER)) null else Calibration(
            enterRatio = prefs.getFloat(ENTER, 0f),
            exitRatio = prefs.getFloat(EXIT, 0f),
            fallbackEnterRatio = prefs.getFloat(FALLBACK_ENTER, 0f),
            fallbackExitRatio = prefs.getFloat(FALLBACK_EXIT, 0f),
        )
        set(value) {
            val edit = prefs.edit()
            if (value == null) {
                edit.clear()
            } else {
                edit.putFloat(ENTER, value.enterRatio)
                    .putFloat(EXIT, value.exitRatio)
                    .putFloat(FALLBACK_ENTER, value.fallbackEnterRatio)
                    .putFloat(FALLBACK_EXIT, value.fallbackExitRatio)
            }
            edit.apply()
        }

    private companion object {
        const val ENTER = "enter"
        const val EXIT = "exit"
        const val FALLBACK_ENTER = "fallback_enter"
        const val FALLBACK_EXIT = "fallback_exit"
    }
}
