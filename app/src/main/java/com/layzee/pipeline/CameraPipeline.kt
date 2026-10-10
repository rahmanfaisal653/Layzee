package com.layzee.pipeline

import android.content.Context
import android.os.SystemClock
import android.util.Size
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
import com.layzee.swipe.SwipeDispatcher
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Front camera -> hand landmarker -> gesture engine. Analysis is bound to [lifecycleOwner],
 * so the camera runs only while the owner is started.
 *
 * Analysis: low resolution, RGBA_8888 straight from CameraX (no manual YUV conversion), only the latest
 * frame kept. The analyzer runs on one dedicated thread; inference itself runs asynchronously inside
 * MediaPipe, so neither blocks the UI thread.
 */
internal class CameraPipeline(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    config: GestureConfig,
    listener: HandLandmarkerSource.Listener,
    dispatcherProvider: () -> SwipeDispatcher?,
) {
    private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor { r ->
        Thread(r, "layzee-analysis")
    }

    // Created and closed on the analysis thread: loading the 7.5 MB model must not block the UI thread,
    // and running there guarantees analyze() never sees a closed landmarker.
    @Volatile private var source: HandLandmarkerSource? = null
    @Volatile private var released = false

    // Only used when the plane has row padding; reused.
    private var rowScratch: ByteBuffer? = null
    private var provider: ProcessCameraProvider? = null

    init {
        analysisExecutor.execute { source = HandLandmarkerSource(context, config, listener, dispatcherProvider) }
    }

    fun start() {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            if (released) return@addListener
            val cameraProvider = future.get()
            provider = cameraProvider

            val analysis = ImageAnalysis.Builder()
                .setResolutionSelector(selector())
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
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
            if (BuildConfig.DEBUG) StageTimer.record(StageTimer.CAMERA, t0 - timestampNs)

            val plane = image.planes[0]
            val width = image.width
            val height = image.height
            val bitmap = source.bitmapFor(width, height)
            copyPlane(plane.buffer, plane.rowStride, width, height, bitmap)

            if (BuildConfig.DEBUG) StageTimer.record(StageTimer.CONVERT, SystemClock.elapsedRealtimeNanos() - t0)
            source.submit(image.imageInfo.rotationDegrees, timestampNs / 1_000_000L)
        } finally {
            image.close()
        }
    }

    private fun copyPlane(src: ByteBuffer, rowStride: Int, width: Int, height: Int, bitmap: android.graphics.Bitmap) {
        val tightRow = width * 4
        src.rewind()
        if (rowStride == tightRow) {
            bitmap.copyPixelsFromBuffer(src)
            return
        }
        // Padded rows: repack into a tight buffer first.
        val needed = tightRow * height
        var scratch = rowScratch
        if (scratch == null || scratch.capacity() != needed) {
            scratch = ByteBuffer.allocateDirect(needed)
            rowScratch = scratch
        }
        scratch.clear()
        val limit = src.limit()
        for (row in 0 until height) {
            val start = row * rowStride
            src.limit(minOf(start + tightRow, limit)).position(start)
            scratch.put(src)
        }
        src.limit(limit)
        scratch.rewind()
        bitmap.copyPixelsFromBuffer(scratch)
    }

    private companion object {
        // Start small; raise to 480x360 only if pinch detection misses.
        val ANALYSIS_SIZE = Size(320, 240)
    }
}
