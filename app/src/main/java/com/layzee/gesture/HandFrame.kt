package com.layzee.gesture

/** MediaPipe hand landmark indices used by the gesture engine. */
object Landmark {
    const val COUNT = 21
    const val WRIST = 0
    const val THUMB_TIP = 4
    const val INDEX_PIP = 6
    const val INDEX_TIP = 8
    const val MIDDLE_MCP = 9
}

/**
 * One camera frame's worth of hand data. Allocate once and refill per frame.
 * Landmark coordinates are normalized to 0..1 of the frame, as MediaPipe reports them.
 */
class HandFrame {
    /** False when the model found no hand; the landmark arrays are then ignored. */
    var handPresent = false
    var confidence = 0f
    var timestampMs = 0L
    var frameWidth = 0
    var frameHeight = 0

    internal val xs = FloatArray(Landmark.COUNT)
    internal val ys = FloatArray(Landmark.COUNT)

    fun setLandmark(index: Int, x: Float, y: Float) {
        xs[index] = x
        ys[index] = y
    }
}
