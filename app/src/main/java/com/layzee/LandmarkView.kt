package com.layzee

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.layzee.gesture.HandFrame
import com.layzee.gesture.Landmark

/**
 * Draws the 21 hand landmarks over a camera preview. The preview is a front camera, so it is mirrored:
 * x is flipped here. The frame is fitted centered into the view, matching PreviewView's FIT_CENTER.
 * Holds its own copy of the points, so the camera thread never shares the engine's buffers.
 */
class LandmarkView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    private val lock = Any()
    private val xs = FloatArray(Landmark.COUNT)
    private val ys = FloatArray(Landmark.COUNT)
    private var visible = false
    private var aspect = 0.75f // width / height of the upright frame

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.GREEN }
    private val pinchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.RED }
    private var thumbIndexHighlight = false

    /** Hides the landmarks (pipeline stopped). */
    fun clear() {
        synchronized(lock) { visible = false }
        postInvalidateOnAnimation()
    }

    /** Called from the camera thread. */
    internal fun update(frame: HandFrame, highlight: Boolean) {
        synchronized(lock) {
            visible = frame.handPresent
            if (visible) {
                System.arraycopy(frame.xs, 0, xs, 0, Landmark.COUNT)
                System.arraycopy(frame.ys, 0, ys, 0, Landmark.COUNT)
                aspect = frame.frameWidth.toFloat() / frame.frameHeight
            }
            thumbIndexHighlight = highlight
        }
        postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        synchronized(lock) {
            if (!visible) return
            val viewW = width.toFloat()
            val viewH = height.toFloat()
            var w = viewW
            var h = w / aspect
            if (h > viewH) {
                h = viewH
                w = h * aspect
            }
            val left = (viewW - w) / 2f
            val top = (viewH - h) / 2f
            val radius = resources.displayMetrics.density * 4f
            for (i in 0 until Landmark.COUNT) {
                val special = thumbIndexHighlight && (i == Landmark.THUMB_TIP || i == Landmark.INDEX_TIP)
                canvas.drawCircle(left + (1f - xs[i]) * w, top + ys[i] * h, radius, if (special) pinchPaint else paint)
            }
        }
    }
}
