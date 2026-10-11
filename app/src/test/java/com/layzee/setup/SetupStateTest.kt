package com.layzee.setup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SetupStateTest {
    private val granted = Permissions(accessibility = true, overlay = true, camera = true)

    @Test
    fun startNeedsEveryPermissionTheBatteryStepConsent() {
        val ready = SetupState(consentGiven = true, batteryDone = true)
        assertTrue(ready.canStart(granted))
        assertFalse(ready.canStart(granted.copy(accessibility = false)))
        assertFalse(ready.canStart(granted.copy(overlay = false)))
        assertFalse(ready.canStart(granted.copy(camera = false)))
        assertFalse(ready.copy(batteryDone = false).canStart(granted))
        // Enabled straight from system Settings: the in-app consent was never given.
        assertFalse(ready.copy(consentGiven = false).canStart(granted))
    }

    @Test
    fun deadWithoutStopSendsTheUserBackToTheBatteryStep() {
        val afterKill = SetupState(batteryDone = true, shouldRun = true).reconcile(running = false)
        assertFalse(afterKill.batteryDone)
        assertFalse(afterKill.shouldRun)
        assertTrue(afterKill.diedUnexpectedly)
    }

    @Test
    fun runningOrStoppedByUserIsNotAKill() {
        val running = SetupState(batteryDone = true, shouldRun = true)
        assertEquals(running, running.reconcile(running = true))
        val stopped = SetupState(batteryDone = true, shouldRun = false)
        assertEquals(stopped, stopped.reconcile(running = false))
    }

    @Test
    fun tickingTheBatteryStepAgainClearsTheHint() {
        val died = SetupState(shouldRun = true).reconcile(running = false)
        val fixed = died.withBatteryDone(true)
        assertTrue(fixed.batteryDone)
        assertFalse(fixed.diedUnexpectedly)
        assertTrue(died.withBatteryDone(false).diedUnexpectedly)
    }
}
