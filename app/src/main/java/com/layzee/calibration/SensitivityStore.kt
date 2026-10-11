package com.layzee.calibration

import android.content.Context

/** The Pinch sensitivity level in SharedPreferences, on the device only. Neutral until the user moves the slider. */
class SensitivityStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("layzee_settings", Context.MODE_PRIVATE)

    var level: Int
        get() = prefs.getInt(LEVEL, Calibration.SENSITIVITY_NEUTRAL)
            .coerceIn(Calibration.SENSITIVITY_MIN, Calibration.SENSITIVITY_MAX)
        set(value) = prefs.edit().putInt(LEVEL, value).apply()

    private companion object {
        const val LEVEL = "pinch_sensitivity"
    }
}
