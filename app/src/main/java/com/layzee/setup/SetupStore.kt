package com.layzee.setup

import android.content.Context

/** [SetupState] in SharedPreferences, on the device only. */
class SetupStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("layzee_setup", Context.MODE_PRIVATE)

    var state: SetupState
        get() = SetupState(
            consentGiven = prefs.getBoolean(CONSENT, false),
            batteryDone = prefs.getBoolean(BATTERY, false),
            shouldRun = prefs.getBoolean(SHOULD_RUN, false),
            diedUnexpectedly = prefs.getBoolean(DIED, false),
        )
        set(value) = prefs.edit()
            .putBoolean(CONSENT, value.consentGiven)
            .putBoolean(BATTERY, value.batteryDone)
            .putBoolean(SHOULD_RUN, value.shouldRun)
            .putBoolean(DIED, value.diedUnexpectedly)
            .apply()

    private companion object {
        const val CONSENT = "consent_given"
        const val BATTERY = "battery_done"
        const val SHOULD_RUN = "should_run"
        const val DIED = "died_unexpectedly"
    }
}
