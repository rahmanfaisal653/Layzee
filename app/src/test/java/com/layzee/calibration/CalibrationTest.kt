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
}
