package com.layzee.pipeline

internal data class FpsRange(val lower: Int, val upper: Int)

internal object FpsRanges {
    private const val MAX_FPS = 30 // the Pinch rate; more would only cost power
    private const val MIN_FLOOR = 15 // below this the Tracking rate would not hold in dim light

    /**
     * Picks the camera range to lock `CONTROL_AE_TARGET_FPS_RANGE` to: the highest top not above 30 (the
     * smallest top above it if there is none), and on that top the lowest floor of at least 15, so exposure
     * cannot drop the frame rate in dim light. Without such a floor, the highest one. Null for no ranges.
     */
    fun choose(ranges: List<FpsRange>): FpsRange? {
        if (ranges.isEmpty()) return null
        val top = ranges.map { it.upper }.filter { it <= MAX_FPS }.maxOrNull() ?: ranges.minOf { it.upper }
        val sameTop = ranges.filter { it.upper == top }
        return sameTop.filter { it.lower >= MIN_FLOOR }.minByOrNull { it.lower } ?: sameTop.maxByOrNull { it.lower }
    }
}
