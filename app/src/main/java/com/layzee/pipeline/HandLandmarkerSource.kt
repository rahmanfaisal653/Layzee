package com.layzee.pipeline

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.SystemClock
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
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
        /**
         * Called on the MediaPipe callback thread for every result. [frame] and [image] are only valid
         * during the call. [image] is the very camera frame the landmarks were found in (null if it is
         * no longer available), sideways by [rotationDegrees] clockwise: the frame is not rotated upright.
         */
        fun onFrame(frame: HandFrame, state: GestureState, image: Bitmap?, rotationDegrees: Int)
    }

    private val engine = GestureEngine()
    private val frame = HandFrame()
    private val output = GestureOutput()

    private val landmarker: HandLandmarker
    private val optionsByRotation = arrayOfNulls<ImageProcessingOptions>(4)

    @Volatile private var closed = false
    private var bitmap: Bitmap? = null
    private var mpImage: MPImage? = null

    // Rotation and size of the frames in flight: written by submit(), read by onResult().
    @Volatile private var uprightWidth = 0
    @Volatile private var uprightHeight = 0

    // The last few submitted frames, so a (late) result is shown over the frame it belongs to. Only the
    // dashboard test screen reads them; ticket 07 decides when to skip the copy.
    private val shown = arrayOfNulls<Bitmap>(SHOWN_SLOTS)
    private val shownCanvas = arrayOfNulls<Canvas>(SHOWN_SLOTS)
    private val shownTimestamp = LongArray(SHOWN_SLOTS) { -1L }
    private val shownRotation = IntArray(SHOWN_SLOTS)
    private var shownNext = 0

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
        mpImage?.close()
        val fresh = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap = fresh
        mpImage = BitmapImageBuilder(fresh).build()
        return fresh
    }

    /** Sends the bitmap from [bitmapFor] (already filled) to the model. [rotationDegrees] is CameraX's clockwise rotation to upright (0, 90, 180 or 270). */
    fun submit(rotationDegrees: Int, timestampMs: Long) {
        val image = mpImage ?: return
        val bmp = bitmap ?: return
        val sideways = rotationDegrees == 90 || rotationDegrees == 270
        uprightWidth = if (sideways) bmp.height else bmp.width
        uprightHeight = if (sideways) bmp.width else bmp.height

        // Same direction as CameraX's rotationDegrees (clockwise to upright): MediaPipe negates it into its
        // NormalizedRect rotation. Reversing it reads the hand upside down.
        val slot = rotationDegrees / 90
        val options = optionsByRotation[slot]
            ?: ImageProcessingOptions.builder().setRotationDegrees(rotationDegrees).build()
                .also { optionsByRotation[slot] = it }

        rememberFrame(bmp, rotationDegrees, timestampMs)

        if (BuildConfig.DEBUG) startNs[(timestampMs and START_MASK).toInt()] = SystemClock.elapsedRealtimeNanos()
        landmarker.detectAsync(image, options, timestampMs)
    }

    private fun rememberFrame(source: Bitmap, rotationDegrees: Int, timestampMs: Long) {
        val slot = shownNext
        shownNext = (shownNext + 1) % SHOWN_SLOTS
        var copy = shown[slot]
        if (copy == null || copy.width != source.width || copy.height != source.height) {
            copy = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
            shown[slot] = copy
            shownCanvas[slot] = Canvas(copy)
        }
        shownCanvas[slot]!!.drawBitmap(source, 0f, 0f, null)
        shownRotation[slot] = rotationDegrees
        shownTimestamp[slot] = timestampMs // last: marks the slot as holding this frame
    }

    fun close() {
        closed = true
        // Stopped mid-Pinch: lift the virtual finger so no stroke is left pressed.
        dispatcherProvider()?.release()
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
        frame.screenTouched = dispatcher?.consumeTouched() ?: false

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

        var slot = -1
        for (i in 0 until SHOWN_SLOTS) if (shownTimestamp[i] == ts) slot = i
        listener.onFrame(frame, output.state, if (slot >= 0) shown[slot] else null, if (slot >= 0) shownRotation[slot] else 0)
    }

    private fun dispatch(dispatcher: SwipeDispatcher) {
        when (output.command) {
            SwipeCommand.START -> dispatcher.start(output.x, output.y)
            SwipeCommand.MOVE -> dispatcher.move(output.x, output.y)
            SwipeCommand.LIFT -> dispatcher.lift(output.y, output.velocityY, config.screenHeight - 1f)
            SwipeCommand.CANCEL -> dispatcher.cancel()
            SwipeCommand.NONE -> Unit
        }
    }

    private companion object {
        const val MODEL_ASSET = "hand_landmarker.task"
        const val MIN_CONFIDENCE = 0.5f
        const val SHOWN_SLOTS = 4
        const val START_SLOTS = 8
        const val START_MASK = (START_SLOTS - 1).toLong()
    }
}
