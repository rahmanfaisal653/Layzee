package com.layzee

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.graphics.Bitmap
import android.widget.AbsListView
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.layzee.gesture.GestureState
import com.layzee.gesture.HandFrame
import com.layzee.pipeline.HandLandmarkerSource
import com.layzee.swipe.LayzeeAccessibilityService

/**
 * Test screen for now: front camera frame with hand landmarks, the live gesture state and a playground list
 * the virtual finger can scroll. Start and Stop control [LayzeeService], which owns the pipeline; this screen
 * only shows its frames while it is open, and never runs the camera itself.
 */
class DashboardActivity : ComponentActivity(), HandLandmarkerSource.Listener {
    private lateinit var landmarks: LandmarkView
    private lateinit var statusView: TextView
    private lateinit var toggle: Button
    private lateinit var accessibilityHint: View

    // Written on the camera thread, compared there; the UI label is only touched on a change.
    private var shownState: GestureState? = null

    private companion object {
        const val IDLE_COLOR = 0xCC555555.toInt()
    }

    private val requestCamera =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) requestNotificationsThenStart() else statusView.setText(R.string.camera_permission_needed)
        }

    // The foreground service runs either way; without this permission its notification is only in the task manager.
    private val requestNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { LayzeeService.start(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        landmarks = findViewById(R.id.landmarks)
        statusView = findViewById(R.id.status)
        toggle = findViewById(R.id.toggle)
        accessibilityHint = findViewById(R.id.accessibility_hint)

        findViewById<ListView>(R.id.playground).adapter = PlaygroundAdapter()
        findViewById<Button>(R.id.open_accessibility).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        findViewById<Button>(R.id.open_overlay_permission).setOnClickListener {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        }
        toggle.setOnClickListener {
            when {
                LayzeeService.running -> LayzeeService.stop(this)
                hasPermission(Manifest.permission.CAMERA) -> requestNotificationsThenStart()
                else -> requestCamera.launch(Manifest.permission.CAMERA)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        LayzeeService.preview = this
        LayzeeService.onRunningChanged = ::refresh
        refresh()
    }

    override fun onResume() {
        super.onResume()
        accessibilityHint.visibility =
            if (LayzeeAccessibilityService.dispatcher == null) View.VISIBLE else View.GONE
    }

    // The service keeps running; only the test screen stops listening.
    override fun onStop() {
        LayzeeService.preview = null
        LayzeeService.onRunningChanged = null
        super.onStop()
    }

    // Matches the Off / running state of the service; the live state then comes from onFrame.
    private fun refresh() {
        val running = LayzeeService.running
        toggle.setText(if (running) R.string.stop else R.string.start)
        if (!running) {
            shownState = null
            statusView.setText(R.string.status_off)
            statusView.setBackgroundColor(IDLE_COLOR)
            landmarks.clear()
        } else if (shownState == null) {
            statusView.setText(R.string.status_idle)
        }
    }

    private fun hasPermission(permission: String) =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private fun requestNotificationsThenStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !hasPermission(Manifest.permission.POST_NOTIFICATIONS)
        ) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            LayzeeService.start(this)
        }
    }

    override fun onImage(image: Bitmap) = landmarks.setImage(image)

    override fun onFrame(frame: HandFrame, state: GestureState) {
        landmarks.update(frame, state == GestureState.PINCH)
        if (state != shownState) {
            shownState = state
            statusView.post {
                statusView.setBackgroundColor(
                    when (state) {
                        GestureState.IDLE -> IDLE_COLOR
                        GestureState.TRACKING -> 0xCCE0A000.toInt()
                        GestureState.PINCH -> 0xCC1B9E3E.toInt()
                    },
                )
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

/** A finite list of distinct colored bands, only there to make scrolling visible. */
private class PlaygroundAdapter : BaseAdapter() {
    override fun getCount() = ROWS
    override fun getItem(position: Int) = position
    override fun getItemId(position: Int) = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val row = convertView ?: View(parent.context).apply {
            layoutParams = AbsListView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (ROW_HEIGHT_DP * resources.displayMetrics.density).toInt(),
            )
        }
        row.setBackgroundColor(Color.HSVToColor(floatArrayOf(position * 360f / ROWS, 0.55f, 0.6f)))
        return row
    }

    private companion object {
        const val ROWS = 40
        const val ROW_HEIGHT_DP = 80
    }
}
