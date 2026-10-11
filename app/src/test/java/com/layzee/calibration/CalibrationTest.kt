package com.layzee.calibration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalibrationTest {
    @Test
    fun thresholdsSitInsideTheGapWithEnterBelowExit() {
        val c = Calibration.from(pinchHigh = 0.2f, openLow = 0.8f, fallbackPinchHigh = 0.4f, fallbackOpenLow = 1.0f)!!
        assertTrue(c.enterRatio > 0.2f && c.enterRatio < c.exitRatio && c.exitRatio < 0.8f)
        assertTrue(c.fallbackEnterRatio > 0.4f && c.fallbackEnterRatio < c.fallbackExitRatio && c.fallbackExitRatio < 1.0f)
    }

    @Test
    fun noGapBetweenPinchAndOpenHandGivesNoCalibration() {
        assertNull(Calibration.from(pinchHigh = 0.5f, openLow = 0.5f, fallbackPinchHigh = 0.4f, fallbackOpenLow = 1.0f))
        assertNull(Calibration.from(pinchHigh = 0.6f, openLow = 0.4f, fallbackPinchHigh = 0.4f, fallbackOpenLow = 1.0f))
    }

    @Test
    fun fallbackWithoutAGapIsOff() {
        val c = Calibration.from(pinchHigh = 0.2f, openLow = 0.8f, fallbackPinchHigh = 0.9f, fallbackOpenLow = 0.9f)!!
        assertEquals(0f, c.fallbackEnterRatio, 0f)
        assertEquals(0f, c.fallbackExitRatio, 0f)
    }

    private val base = Calibration(enterRatio = 0.3f, exitRatio = 0.5f, fallbackEnterRatio = 0.4f, fallbackExitRatio = 0.7f)

    @Test
    fun neutralSensitivityKeepsTheCalibrationAsIs() {
        assertEquals(base, base.withSensitivity(Calibration.SENSITIVITY_NEUTRAL))
    }

    @Test
    fun looserSensitivityRaisesAndTighterLowersEveryThreshold() {
        val loose = base.withSensitivity(Calibration.SENSITIVITY_MAX)
        val tight = base.withSensitivity(Calibration.SENSITIVITY_MIN)
        assertTrue(loose.enterRatio > base.enterRatio && loose.exitRatio > base.exitRatio)
        assertTrue(loose.fallbackEnterRatio > base.fallbackEnterRatio && loose.fallbackExitRatio > base.fallbackExitRatio)
        assertTrue(tight.enterRatio < base.enterRatio && tight.exitRatio < base.exitRatio)
        assertTrue(tight.fallbackEnterRatio < base.fallbackEnterRatio && tight.fallbackExitRatio < base.fallbackExitRatio)
    }

    @Test
    fun sensitivityOnlyNudgesTheCalibration() {
        val loose = base.withSensitivity(Calibration.SENSITIVITY_MAX)
        val tight = base.withSensitivity(Calibration.SENSITIVITY_MIN)
        assertTrue(loose.exitRatio < base.exitRatio * 1.16f)
        assertTrue(tight.enterRatio > base.enterRatio * 0.84f)
    }

    @Test
    fun sensitivityKeepsEnterBelowExitAndAnOffFallbackOff() {
        for (level in Calibration.SENSITIVITY_MIN..Calibration.SENSITIVITY_MAX) {
            val c = base.withSensitivity(level)
            assertTrue(c.enterRatio <= c.exitRatio)
            val off = base.copy(fallbackEnterRatio = 0f, fallbackExitRatio = 0f).withSensitivity(level)
            assertEquals(0f, off.fallbackEnterRatio, 0f)
            assertEquals(0f, off.fallbackExitRatio, 0f)
        }
    }

    @Test
    fun sensitivityOutsideTheRangeIsClamped() {
        assertEquals(base.withSensitivity(Calibration.SENSITIVITY_MAX), base.withSensitivity(Calibration.SENSITIVITY_MAX + 5))
        assertEquals(base.withSensitivity(Calibration.SENSITIVITY_MIN), base.withSensitivity(Calibration.SENSITIVITY_MIN - 5))
    }
}
