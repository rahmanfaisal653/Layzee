package com.layzee

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ListView
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
import com.layzee.swipe.LayzeeAccessibilityService

/**
 * Test screen for now: front camera preview, hand landmarks, the live gesture state and a playground list
 * the virtual finger can scroll. A temporary debug button starts and stops the pipeline (ticket 07
 * replaces it with the foreground service). The pipeline is stopped whenever the screen leaves the
 * foreground, which lifts a pressed virtual finger.
 */
class DashboardActivity : ComponentActivity(), HandLandmarkerSource.Listener {
    private lateinit var landmarks: LandmarkView
    private lateinit var statusView: TextView
    private lateinit var toggle: Button
    private lateinit var accessibilityHint: View
    private var pipeline: CameraPipeline? = null

    // Written on the camera thread, compared there; the UI label is only touched on a change.
    private var shownState: GestureState? = null

    private val requestCamera =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startPipeline() else statusView.setText(R.string.camera_permission_needed)
            updateToggle()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        landmarks = findViewById(R.id.landmarks)
        statusView = findViewById(R.id.status)
        statusView.setText(R.string.status_idle)
        toggle = findViewById(R.id.toggle)
        accessibilityHint = findViewById(R.id.accessibility_hint)

        findViewById<ListView>(R.id.playground).adapter = PlaygroundAdapter()
        findViewById<Button>(R.id.open_accessibility).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        toggle.setOnClickListener {
            when {
                pipeline != null -> stopPipeline()
                hasCameraPermission() -> startPipeline()
                else -> requestCamera.launch(Manifest.permission.CAMERA)
            }
            updateToggle()
        }
        updateToggle()
    }

    override fun onResume() {
        super.onResume()
        accessibilityHint.visibility =
            if (LayzeeAccessibilityService.dispatcher == null) View.VISIBLE else View.GONE
    }

    // Leaving the foreground ends the debug run (and lifts a pressed virtual finger); Start again to resume.
    override fun onStop() {
        stopPipeline()
        updateToggle()
        super.onStop()
    }

    private fun updateToggle() {
        toggle.setText(if (pipeline != null) R.string.debug_stop else R.string.debug_start)
    }

    private fun stopPipeline() {
        pipeline?.release()
        pipeline = null
        shownState = null
        statusView.setText(R.string.status_idle)
        landmarks.clear()
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
            config = gestureConfig(),
            listener = this,
            dispatcherProvider = { LayzeeAccessibilityService.dispatcher },
        ).also { it.start() }
    }

    // The virtual finger works in real display pixels (including system bars), not app-window pixels.
    private fun gestureConfig(): GestureConfig {
        val width: Int
        val height: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.maximumWindowMetrics.bounds
            width = bounds.width()
            height = bounds.height()
        } else {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(metrics)
            width = metrics.widthPixels
            height = metrics.heightPixels
        }
        return GestureConfig(screenWidth = width, screenHeight = height)
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

/** Colored rows with words, only there to make scrolling visible. */
private class PlaygroundAdapter : BaseAdapter() {
    override fun getCount() = ROWS
    override fun getItem(position: Int) = WORDS[position % WORDS.size]
    override fun getItemId(position: Int) = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val row = (convertView as? TextView) ?: TextView(parent.context).apply {
            val dp = resources.displayMetrics.density
            setPadding((24 * dp).toInt(), (28 * dp).toInt(), (24 * dp).toInt(), (28 * dp).toInt())
            textSize = 22f
            setTextColor(Color.WHITE)
        }
        row.text = getItem(position)
        row.setBackgroundColor(Color.HSVToColor(floatArrayOf((position * 37 % 360).toFloat(), 0.55f, 0.55f)))
        return row
    }

    private companion object {
        const val ROWS = 500
        val WORDS = arrayOf("Apel", "Jeruk", "Mangga", "Anggur", "Melon", "Pisang", "Nanas", "Semangka")
    }
}
