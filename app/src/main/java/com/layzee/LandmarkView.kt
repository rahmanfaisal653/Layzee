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
 * Camera test view: draws the camera frame the landmarks were found in, and the 21 landmarks on top.
 * Frame and points come from the same result, so they line up exactly (no separate preview whose crop or
 * delay could differ). The frame arrives sideways by its rotation; it is turned upright here and mirrored,
 * like a selfie. Upright frame and landmarks are fitted centered into the view.
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
    private var rotation = 0

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

    /** Called from the MediaPipe callback thread. [frame] is the upright landmark data, [source] the sideways camera frame. */
    internal fun update(frame: HandFrame, pinching: Boolean, source: Bitmap?, rotationDegrees: Int) {
        synchronized(lock) {
            handVisible = frame.handPresent
            if (handVisible) {
                System.arraycopy(frame.xs, 0, xs, 0, Landmark.COUNT)
                System.arraycopy(frame.ys, 0, ys, 0, Landmark.COUNT)
            }
            highlight = pinching
            if (source != null) {
                var own = image
                if (own == null || own.width != source.width || own.height != source.height) {
                    own = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
                    image = own
                    imageCanvas = Canvas(own)
                }
                imageCanvas!!.drawBitmap(source, 0f, 0f, null)
                rotation = rotationDegrees
                hasImage = true
            }
        }
        postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        synchronized(lock) {
            val frame = image
            if (!hasImage || frame == null) return
            val sideways = rotation == 90 || rotation == 270
            val uprightW = (if (sideways) frame.height else frame.width).toFloat()
            val uprightH = (if (sideways) frame.width else frame.height).toFloat()
            val scale = minOf(width / uprightW, height / uprightH)
            val cx = width / 2f
            val cy = height / 2f

            // Raw frame -> rotate clockwise to upright -> mirror -> fit into the view.
            val saved = canvas.save()
            canvas.translate(cx, cy)
            canvas.scale(-scale, scale)
            canvas.rotate(rotation.toFloat())
            canvas.translate(-frame.width / 2f, -frame.height / 2f)
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
