package com.layzee.pipeline

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import com.layzee.BuildConfig
import com.layzee.gesture.GestureConfig
import com.layzee.gesture.GestureEngine
import com.layzee.gesture.GestureOutput
import com.layzee.gesture.GestureState
import com.layzee.gesture.HandFrame
import com.layzee.gesture.Landmark
import com.layzee.gesture.SwipeCommand
import com.layzee.swipe.SwipeDispatcher

/**
 * MediaPipe Hand Landmarker in LIVE_STREAM mode (async, one hand) feeding the gesture engine.
 *
 * [submit] is called from the camera analysis thread and returns at once; MediaPipe runs inference on
 * its own threads and delivers results to [onResult], which fills the reused [HandFrame], runs the
 * engine and publishes to [listener]. The input bitmap and MPImage are reused for every frame.
 */
internal class HandLandmarkerSource(
    context: Context,
    private val config: GestureConfig,
    private val listener: Listener,
    private val dispatcherProvider: () -> SwipeDispatcher?,
) {
    interface Listener {
        /** Called on the MediaPipe callback thread for every result. [frame] is only valid during the call. */
        fun onFrame(frame: HandFrame, state: GestureState)

        /**
         * Called on the analysis thread for every camera frame, before it goes to the model, so the
         * screen can show video at camera rate while results arrive at inference rate. [image] is only
         * valid during the call; upright, the same image the landmarks refer to.
         */
        fun onImage(image: Bitmap)
    }

    private val engine = GestureEngine()
    private val frame = HandFrame()
    private val output = GestureOutput()

    private val landmarker: HandLandmarker
    private val matrix = Matrix()

    @Volatile private var closed = false
    private var bitmap: Bitmap? = null // as the camera delivers it (sideways)
    private var upright: Bitmap? = null
    private var uprightCanvas: Canvas? = null
    private var mpImage: MPImage? = null

    // Rotation and size of the frames in flight: written by submit(), read by onResult().
    @Volatile private var uprightWidth = 0
    @Volatile private var uprightHeight = 0

    // detectAsync start time per frame, indexed by timestamp, for the debug inference timing.
    private val startNs = LongArray(START_SLOTS)

    init {
        val options = HandLandmarker.HandLandmarkerOptions.builder()
            .setBaseOptions(BaseOptions.builder().setModelAssetPath(MODEL_ASSET).build())
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setNumHands(1)
            .setMinHandDetectionConfidence(MIN_CONFIDENCE)
            .setMinHandPresenceConfidence(MIN_CONFIDENCE)
            .setMinTrackingConfidence(MIN_CONFIDENCE)
            .setResultListener { result, _ -> onResult(result) }
            .build()
        landmarker = HandLandmarker.createFromOptions(context, options)
    }

    /** The bitmap the caller fills before [submit]; recreated only when the frame size changes. */
    fun bitmapFor(width: Int, height: Int): Bitmap {
        val current = bitmap
        if (current != null && current.width == width && current.height == height) return current
        val fresh = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap = fresh
        return fresh
    }

    /**
     * Sends the bitmap from [bitmapFor] (already filled) to the model. [rotationDegrees] is CameraX's
     * clockwise rotation to upright (0, 90, 180 or 270). The frame is turned upright here, into a reused
     * bitmap, instead of through MediaPipe's rotation option, so the landmarks are by construction in the
     * coordinates of the upright image that [Listener.onImage] shows.
     */
    fun submit(rotationDegrees: Int, timestampMs: Long) {
        val raw = bitmap ?: return
        val sideways = rotationDegrees == 90 || rotationDegrees == 270
        val width = if (sideways) raw.height else raw.width
        val height = if (sideways) raw.width else raw.height

        var target = upright
        if (target == null || target.width != width || target.height != height) {
            mpImage?.close()
            target = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            upright = target
            uprightCanvas = Canvas(target)
            mpImage = BitmapImageBuilder(target).build()
        }
        uprightWidth = width
        uprightHeight = height

        // Rotate about the centre, then move the rotated frame's centre onto the target's centre.
        matrix.setRotate(rotationDegrees.toFloat(), raw.width / 2f, raw.height / 2f)
        matrix.postTranslate((width - raw.width) / 2f, (height - raw.height) / 2f)
        uprightCanvas!!.drawBitmap(raw, matrix, null)

        listener.onImage(target)

        if (BuildConfig.DEBUG) startNs[(timestampMs and START_MASK).toInt()] = SystemClock.elapsedRealtimeNanos()
        landmarker.detectAsync(mpImage ?: return, timestampMs)
    }

    fun close() {
        closed = true
        landmarker.close()
        mpImage?.close()
    }

    private fun onResult(result: HandLandmarkerResult) {
        if (closed) return
        val ts = result.timestampMs()
        if (BuildConfig.DEBUG) {
            val start = startNs[(ts and START_MASK).toInt()]
            StageTimer.record(StageTimer.INFERENCE, SystemClock.elapsedRealtimeNanos() - start)
        }
        val t0 = if (BuildConfig.DEBUG) SystemClock.elapsedRealtimeNanos() else 0L

        frame.timestampMs = ts
        frame.frameWidth = uprightWidth
        frame.frameHeight = uprightHeight
        val dispatcher = dispatcherProvider()

        val hands = result.landmarks()
        val handedness = result.handedness()
        if (hands.isEmpty() || handedness.isEmpty() || handedness[0].isEmpty()) {
            frame.handPresent = false
            frame.confidence = 0f
        } else {
            val points = hands[0]
            for (i in 0 until Landmark.COUNT) {
                val p = points[i]
                frame.setLandmark(i, p.x(), p.y())
            }
            frame.handPresent = true
            frame.confidence = handedness[0][0].score()
        }

        engine.process(frame, config, output)

        if (BuildConfig.DEBUG) StageTimer.record(StageTimer.LOGIC, SystemClock.elapsedRealtimeNanos() - t0)

        if (!closed && dispatcher != null && output.command != SwipeCommand.NONE) {
            val d0 = if (BuildConfig.DEBUG) SystemClock.elapsedRealtimeNanos() else 0L
            dispatch(dispatcher)
            if (BuildConfig.DEBUG) StageTimer.record(StageTimer.DISPATCH, SystemClock.elapsedRealtimeNanos() - d0)
        }

        listener.onFrame(frame, output.state)
    }

    private fun dispatch(dispatcher: SwipeDispatcher) {
        if (BuildConfig.DEBUG) Log.d("LayzeeSwipe", "${output.command} y=${output.y.toInt()}->${output.toY.toInt()}")
        if (output.command == SwipeCommand.FLICK) dispatcher.flick(output.x, output.y, output.toY)
    }

    private companion object {
        const val MODEL_ASSET = "hand_landmarker.task"
        const val MIN_CONFIDENCE = 0.5f
        const val START_SLOTS = 8
        const val START_MASK = (START_SLOTS - 1).toLong()
    }
}
