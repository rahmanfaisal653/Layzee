package com.layzee.pipeline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FpsRangesTest {
    private fun choose(vararg r: Pair<Int, Int>) = FpsRanges.choose(r.map { FpsRange(it.first, it.second) })

    @Test
    fun prefersTopOf30WithLowestFloorAtOrAbove15() {
        assertEquals(FpsRange(15, 30), choose(7 to 30, 15 to 30, 24 to 30, 30 to 30, 15 to 15))
    }

    @Test
    fun usesHighestFloorWhenNoneReaches15() {
        assertEquals(FpsRange(10, 30), choose(5 to 30, 10 to 30))
    }

    @Test
    fun neverExceedsThirtyWhenAlternativesExist() {
        assertEquals(FpsRange(15, 30), choose(15 to 60, 15 to 30))
    }

    @Test
    fun fallsBackToSmallestTopWhenAllAbove30() {
        assertEquals(FpsRange(30, 60), choose(30 to 120, 30 to 60))
    }

    @Test
    fun emptyListGivesNull() {
        assertNull(FpsRanges.choose(emptyList()))
    }
}
