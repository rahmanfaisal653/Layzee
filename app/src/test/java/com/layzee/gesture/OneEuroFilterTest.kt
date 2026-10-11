package com.layzee.gesture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OneEuroFilterTest {
    private val filter = OneEuroFilter()
    private fun f(v: Float, t: Long) = filter.filter(v, t, 2f, 1f)

    @Test
    fun firstSamplePassesThrough() {
        assertEquals(0.7f, f(0.7f, 1000), 0f)
    }

    @Test
    fun constantInputStaysConstant() {
        f(0.5f, 0)
        repeat(10) { i -> assertEquals(0.5f, f(0.5f, (i + 1) * 33L), 1e-5f) }
    }

    @Test
    fun smallJitterIsSmoothed() {
        f(0.5f, 0)
        var maxDeviation = 0f
        for (i in 1..30) {
            val v = if (i % 2 == 0) 0.55f else 0.45f
            maxDeviation = maxOf(maxDeviation, kotlin.math.abs(f(v, i * 33L) - 0.5f))
        }
        assertTrue("deviation $maxDeviation", maxDeviation < 0.03f)
    }

    @Test
    fun aBigFastChangeIsFollowed() {
        f(1.0f, 0)
        f(0.1f, 33)
        val settled = (2..6).map { f(0.1f, it * 33L) }.last()
        assertTrue("settled at $settled", settled < 0.15f)
    }

    @Test
    fun aLongGapBarelySmooths() {
        f(1.0f, 0)
        assertEquals(0.1f, f(0.1f, 2000), 0.05f)
    }

    @Test
    fun repeatedOrBackwardsTimestampChangesNothing() {
        f(0.5f, 100)
        assertEquals(0.5f, f(0.9f, 100), 0f)
    }

    @Test
    fun clockGoingBackwardsRestartsTheFilter() {
        f(0.5f, 1000)
        assertEquals(0.9f, f(0.9f, 50), 0f)
    }

    @Test
    fun resetForgetsHistory() {
        f(0.5f, 0)
        f(0.5f, 33)
        filter.reset()
        assertEquals(0.9f, f(0.9f, 66), 0f)
    }
}
