package com.layzee

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.layzee.gesture.GestureConfig
import com.layzee.gesture.GestureState
import com.layzee.gesture.HandFrame
import com.layzee.pipeline.CameraPipeline
import com.layzee.pipeline.HandLandmarkerSource

/**
 * Test screen for now: front camera preview, hand landmarks and the live gesture state. The camera runs
 * only while this screen is visible.
 */
class DashboardActivity : ComponentActivity(), HandLandmarkerSource.Listener {
    private lateinit var landmarks: LandmarkView
    private lateinit var statusView: TextView
    private var pipeline: CameraPipeline? = null

    // Written on the camera thread, compared there; the UI label is only touched on a change.
    private var shownState: GestureState? = null

    private val requestCamera =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startPipeline() else statusView.setText(R.string.camera_permission_needed)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        landmarks = findViewById(R.id.landmarks)
        statusView = findViewById(R.id.status)
        statusView.setText(R.string.status_idle)

        if (hasCameraPermission()) startPipeline() else requestCamera.launch(Manifest.permission.CAMERA)
    }

    override fun onDestroy() {
        pipeline?.release()
        pipeline = null
        super.onDestroy()
    }

    private fun hasCameraPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun startPipeline() {
        if (pipeline != null) return
        // LandmarkView assumes the frame is fitted, not cropped.
        val preview = findViewById<PreviewView>(R.id.preview)
        preview.scaleType = PreviewView.ScaleType.FIT_CENTER
        pipeline = CameraPipeline(
            context = this,
            lifecycleOwner = this,
            previewView = preview,
            config = GestureConfig(),
            listener = this,
        ).also { it.start() }
    }

    override fun onFrame(frame: HandFrame, state: GestureState) {
        landmarks.update(frame, state == GestureState.PINCH)
        if (state != shownState) {
            shownState = state
            statusView.post {
                statusView.setText(
                    when (state) {
                        GestureState.IDLE -> R.string.status_idle
                        GestureState.TRACKING -> R.string.status_tracking
                        GestureState.PINCH -> R.string.status_pinch
                    },
                )
            }
        }
    }
}
