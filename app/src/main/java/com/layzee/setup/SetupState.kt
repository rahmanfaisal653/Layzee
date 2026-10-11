package com.layzee.setup

/** What the system reports right now; checked on every return to the Dashboard. */
data class Permissions(val accessibility: Boolean, val overlay: Boolean, val camera: Boolean) {
    val all get() = accessibility && overlay && camera
}

/**
 * What the Dashboard remembers between visits. Pure, so the guide's rules are testable without a phone.
 * [shouldRun] is true from Start until the user's own Stop, which is how a kill by the system is told apart.
 */
data class SetupState(
    val consentGiven: Boolean = false,
    val batteryDone: Boolean = false,
    val shouldRun: Boolean = false,
    val diedUnexpectedly: Boolean = false,
) {
    fun canStart(permissions: Permissions) = permissions.all && batteryDone && consentGiven

    /** The main screen (Start/Stop) replaces the setup list; a running Layzee always keeps its Stop. */
    fun showsMain(permissions: Permissions, running: Boolean) = running || canStart(permissions)

    /** Layzee should be running but is not: the system killed it, so the battery step must be checked again. */
    fun reconcile(running: Boolean) =
        if (shouldRun && !running) copy(batteryDone = false, shouldRun = false, diedUnexpectedly = true) else this

    fun withBatteryDone(done: Boolean) = copy(batteryDone = done, diedUnexpectedly = diedUnexpectedly && !done)
}
