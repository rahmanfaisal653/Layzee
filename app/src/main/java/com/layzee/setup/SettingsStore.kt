package com.layzee.setup

import android.content.Context
import kotlin.math.pow

/** Slider levels (0..10) and what they mean. Neutral levels give the look and feel Layzee shipped with. */
object Tuning {
    const val LEVEL_MIN = 0
    const val LEVEL_MAX = 10
    const val DOT_NEUTRAL = 5
    const val SPEED_NEUTRAL = 5

    /** Landmark dot size factor, 0.5x (smallest) to 2x (largest), 1x at the neutral level. */
    fun dotScale(level: Int): Float = 2f.pow((level.coerceIn(LEVEL_MIN, LEVEL_MAX) - DOT_NEUTRAL) / 5f)

    /** Flick duration in ms: the higher the speed level, the shorter (faster) the flick. 180 ms down to 60 ms. */
    fun flickMs(level: Int): Long = 180L - 12L * level.coerceIn(LEVEL_MIN, LEVEL_MAX)
}

/** Dot size and swipe speed in SharedPreferences, on the device only. Both apply immediately. */
class SettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("layzee_settings", Context.MODE_PRIVATE)

    var dotLevel: Int
        get() = prefs.getInt(DOT, Tuning.DOT_NEUTRAL).coerceIn(Tuning.LEVEL_MIN, Tuning.LEVEL_MAX)
        set(value) = prefs.edit().putInt(DOT, value).apply()

    var speedLevel: Int
        get() = prefs.getInt(SPEED, Tuning.SPEED_NEUTRAL).coerceIn(Tuning.LEVEL_MIN, Tuning.LEVEL_MAX)
        set(value) = prefs.edit().putInt(SPEED, value).apply()

    private companion object {
        const val DOT = "dot_size"
        const val SPEED = "swipe_speed"
    }
}
