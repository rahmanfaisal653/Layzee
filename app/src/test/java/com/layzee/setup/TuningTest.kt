package com.layzee.setup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TuningTest {
    @Test
    fun neutralLevelsKeepTheShippedLook() {
        assertEquals(1f, Tuning.dotScale(Tuning.DOT_NEUTRAL), 0.0001f)
        assertEquals(120L, Tuning.flickMs(Tuning.SPEED_NEUTRAL))
    }

    @Test
    fun dotScaleGrowsWithLevelAndStaysInRange() {
        assertEquals(0.5f, Tuning.dotScale(Tuning.LEVEL_MIN), 0.0001f)
        assertEquals(2f, Tuning.dotScale(Tuning.LEVEL_MAX), 0.0001f)
        for (l in Tuning.LEVEL_MIN until Tuning.LEVEL_MAX) assertTrue(Tuning.dotScale(l) < Tuning.dotScale(l + 1))
    }

    @Test
    fun fasterSpeedMeansShorterFlick() {
        assertEquals(180L, Tuning.flickMs(Tuning.LEVEL_MIN))
        assertEquals(60L, Tuning.flickMs(Tuning.LEVEL_MAX))
        for (l in Tuning.LEVEL_MIN until Tuning.LEVEL_MAX) assertTrue(Tuning.flickMs(l) > Tuning.flickMs(l + 1))
    }

    @Test
    fun outOfRangeLevelsAreClamped() {
        assertEquals(Tuning.flickMs(Tuning.LEVEL_MAX), Tuning.flickMs(99))
        assertEquals(Tuning.dotScale(Tuning.LEVEL_MIN), Tuning.dotScale(-4), 0.0001f)
    }
}
