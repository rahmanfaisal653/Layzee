package com.layzee

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.layzee.gesture.HandFrame
import com.layzee.gesture.Landmark

/**
 * Camera test view: draws the camera frame the model is fed, and the 21 landmarks of the latest result on
 * top. Frames arrive at camera rate and results at inference rate (about 100 ms behind), so the points
 * trail the hand slightly while it moves. Both come from the same analysed frames, so there is no crop
 * mismatch to a separate preview. Frame and points are upright; they are mirrored, like a selfie, and
 * fitted centered into the view.
 * Keeps its own copies, so the camera threads never share buffers with the UI thread.
 */
class LandmarkView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    private val lock = Any()
    private val xs = FloatArray(Landmark.COUNT)
    private val ys = FloatArray(Landmark.COUNT)
    private var handVisible = false
    private var highlight = false

    private var image: Bitmap? = null
    private var imageCanvas: Canvas? = null
    private var hasImage = false

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.GREEN }
    private val pinchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.RED }
    private val imagePaint = Paint(Paint.FILTER_BITMAP_FLAG)

    /** Hides frame and landmarks (pipeline stopped). */
    fun clear() {
        synchronized(lock) {
            hasImage = false
            handVisible = false
        }
        postInvalidateOnAnimation()
    }

    /** Called from the analysis thread for every camera frame; [source] is upright. */
    internal fun setImage(source: Bitmap) {
        synchronized(lock) {
            var own = image
            if (own == null || own.width != source.width || own.height != source.height) {
                own = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
                image = own
                imageCanvas = Canvas(own)
            }
            imageCanvas!!.drawBitmap(source, 0f, 0f, null)
            hasImage = true
        }
        postInvalidateOnAnimation()
    }

    /** Called from the MediaPipe callback thread with the upright landmarks of the latest result. */
    internal fun update(frame: HandFrame, pinching: Boolean) {
        synchronized(lock) {
            handVisible = frame.handPresent
            if (handVisible) {
                System.arraycopy(frame.xs, 0, xs, 0, Landmark.COUNT)
                System.arraycopy(frame.ys, 0, ys, 0, Landmark.COUNT)
            }
            highlight = pinching
        }
        postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        synchronized(lock) {
            val frame = image
            if (!hasImage || frame == null) return
            val uprightW = frame.width.toFloat()
            val uprightH = frame.height.toFloat()
            val scale = minOf(width / uprightW, height / uprightH)
            val cx = width / 2f
            val cy = height / 2f

            // Mirror like a selfie, fit into the view.
            val saved = canvas.save()
            canvas.translate(cx, cy)
            canvas.scale(-scale, scale)
            canvas.translate(-uprightW / 2f, -uprightH / 2f)
            canvas.drawBitmap(frame, 0f, 0f, imagePaint)
            canvas.restoreToCount(saved)

            if (!handVisible) return
            val radius = resources.displayMetrics.density * 4f
            for (i in 0 until Landmark.COUNT) {
                val special = highlight && (i == Landmark.THUMB_TIP || i == Landmark.INDEX_TIP)
                canvas.drawCircle(
                    cx + (0.5f - xs[i]) * uprightW * scale,
                    cy + (ys[i] - 0.5f) * uprightH * scale,
                    radius,
                    if (special) pinchPaint else paint,
                )
            }
        }
    }
}
