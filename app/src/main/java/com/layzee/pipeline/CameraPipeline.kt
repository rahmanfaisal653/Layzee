package com.layzee.pipeline

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.os.SystemClock
import android.util.Range
import android.util.Size
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.layzee.BuildConfig
import com.layzee.gesture.GestureConfig
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Front camera -> hand landmarker -> gesture engine. Analysis is bound to [lifecycleOwner],
 * so the camera runs only while the owner is started.
 *
 * Analysis: low resolution, RGBA_8888 straight from CameraX (no manual YUV conversion), only the latest
 * frame kept. The analyzer runs on one dedicated thread; inference itself runs asynchronously inside
 * MediaPipe, so neither blocks the UI thread.
 *
 * Load: the camera's fps range is locked (see [FpsRanges]); [FramePacer] then drops frames before conversion
 * and inference, by gesture state, and holds the model while Idle and the scene is still.
 *
 * [preview] is for the dashboard test screen: every frame goes to the model, so the picture
 * stays smooth when no hand is in view. A frame that arrives while the model is still busy is ignored by
 * MediaPipe's LIVE_STREAM mode, so a slow phone drops frames instead of queueing them. The service never sets it.
 */
internal class CameraPipeline(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    config: GestureConfig,
    listener: HandLandmarkerSource.Listener,
    private val preview: Boolean = false,
) {
    private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor { r ->
        Thread(r, "layzee-analysis")
    }

    private val pacer = FramePacer()

    // Created and closed on the analysis thread: loading the 7.5 MB model must not block the UI thread,
    // and running there guarantees analyze() never sees a closed landmarker.
    @Volatile private var source: HandLandmarkerSource? = null
    @Volatile private var released = false

    private var provider: ProcessCameraProvider? = null

    init {
        analysisExecutor.execute { source = HandLandmarkerSource(context, config, listener, pacer) }
    }

    @OptIn(ExperimentalCamera2Interop::class)
    fun start() {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            if (released) return@addListener
            val cameraProvider = future.get()
            provider = cameraProvider

            val builder = ImageAnalysis.Builder()
                .setResolutionSelector(selector())
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            runCatching { frontFpsRange() }.getOrNull()?.let {
                Camera2Interop.Extender(builder)
                    .setCaptureRequestOption(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range(it.lower, it.upper))
            }
            val analysis = builder.build()
                .also { it.setAnalyzer(analysisExecutor, ::analyze) }

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner, CameraSelector.DEFAULT_FRONT_CAMERA, analysis,
            )
        }, ContextCompat.getMainExecutor(context))
    }

    fun release() {
        released = true
        provider?.unbindAll()
        // Queued behind any in-flight analyze(), so the landmarker is closed only after it.
        analysisExecutor.execute {
            source?.close()
            source = null
        }
        analysisExecutor.shutdown()
    }

    private fun frontFpsRange(): FpsRange? {
        val manager = context.getSystemService(CameraManager::class.java)
        for (id in manager.cameraIdList) {
            val characteristics = manager.getCameraCharacteristics(id)
            if (characteristics.get(CameraCharacteristics.LENS_FACING) != CameraCharacteristics.LENS_FACING_FRONT) continue
            val ranges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES) ?: return null
            return FpsRanges.choose(ranges.map { FpsRange(it.lower, it.upper) })
        }
        return null
    }

    private fun selector() = ResolutionSelector.Builder()
        .setAspectRatioStrategy(AspectRatioStrategy(AspectRatio.RATIO_4_3, AspectRatioStrategy.FALLBACK_RULE_AUTO))
        .setResolutionStrategy(
            ResolutionStrategy(ANALYSIS_SIZE, ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER),
        )
        .build()

    private fun analyze(image: ImageProxy) {
        try {
            val source = source ?: return
            val t0 = SystemClock.elapsedRealtimeNanos()
            val timestampNs = image.imageInfo.timestamp
            val timestampMs = timestampNs / 1_000_000L
            val plane = image.planes[0]
            val width = image.width
            val height = image.height

            val verdict = if (preview) {
                FramePacer.ADMIT
            } else {
                pacer.admit(timestampMs, plane.buffer, plane.rowStride, width, height)
            }
            if (verdict != FramePacer.ADMIT) {
                if (BuildConfig.DEBUG) StageTimer.skipped(verdict)
                return
            }
            if (BuildConfig.DEBUG) StageTimer.record(StageTimer.CAMERA, t0 - timestampNs)

            source.submit(plane.buffer, plane.rowStride, width, height, image.imageInfo.rotationDegrees, timestampMs)
        } finally {
            image.close()
        }
    }

    private companion object {
        // Start small; raise to 480x360 only if pinch detection misses.
        val ANALYSIS_SIZE = Size(320, 240)
    }
}
