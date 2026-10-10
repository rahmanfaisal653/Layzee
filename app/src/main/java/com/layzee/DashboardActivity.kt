package com.layzee

import android.Manifest
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.ListView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.layzee.setup.Permissions
import com.layzee.setup.SetupStore
import com.layzee.swipe.LayzeeAccessibilityService

/**
 * The permission guide: one checked step per permission, then Start/Stop and the playground. Accessibility,
 * overlay and camera are re-read every time the screen returns to the front; the battery step is ticked by
 * the user, since Android cannot report it. It never runs the camera itself (the camera preview only exists
 * for Calibration); [LayzeeService] owns the pipeline.
 */
class DashboardActivity : ComponentActivity() {
    private lateinit var store: SetupStore
    private lateinit var stepAccessibility: TextView
    private lateinit var stepOverlay: TextView
    private lateinit var stepCamera: TextView
    private lateinit var stepBattery: TextView
    private lateinit var batteryDone: CheckBox
    private lateinit var diedHint: View
    private lateinit var toggle: Button
    private lateinit var playground: ListView

    private val closePlayground = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = showPlayground(false)
    }

    private val requestCamera =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            // Denied with "don't ask again" makes the dialog silent; the app's settings page is the way out.
            if (!granted && !shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) openAppInfo()
            refresh()
        }

    // The foreground service runs either way; without this permission its notification is only in the task manager.
    private val requestNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { LayzeeService.start(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        store = SetupStore(this)
        stepAccessibility = findViewById(R.id.step_accessibility)
        stepOverlay = findViewById(R.id.step_overlay)
        stepCamera = findViewById(R.id.step_camera)
        stepBattery = findViewById(R.id.step_battery)
        batteryDone = findViewById(R.id.battery_done)
        diedHint = findViewById(R.id.died_hint)
        toggle = findViewById(R.id.toggle)
        playground = findViewById(R.id.playground)
        playground.adapter = PlaygroundAdapter()
        onBackPressedDispatcher.addCallback(this, closePlayground)

        findViewById<Button>(R.id.open_accessibility).setOnClickListener { openAccessibility() }
        findViewById<Button>(R.id.open_overlay_permission).setOnClickListener {
            openSettings(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        }
        findViewById<Button>(R.id.request_camera).setOnClickListener {
            requestCamera.launch(Manifest.permission.CAMERA)
        }
        findViewById<Button>(R.id.open_battery).setOnClickListener {
            openSettings(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
        findViewById<Button>(R.id.open_app_info).setOnClickListener { openAppInfo() }
        batteryDone.setOnClickListener {
            store.state = store.state.withBatteryDone(batteryDone.isChecked)
            refresh()
        }
        findViewById<Button>(R.id.open_playground).setOnClickListener { showPlayground(true) }
        toggle.setOnClickListener {
            if (LayzeeService.running) LayzeeService.stop(this) else requestNotificationsThenStart()
        }
    }

    override fun onStart() {
        super.onStart()
        // A kill by the system leaves no trace except that Layzee should be running and is not.
        store.state = store.state.reconcile(LayzeeService.running)
        LayzeeService.onRunningChanged = ::refresh
        refresh()
    }

    // Returning from a settings page lands here, so the checks follow what the user just changed.
    override fun onResume() {
        super.onResume()
        refresh()
    }

    override fun onStop() {
        LayzeeService.onRunningChanged = null
        super.onStop()
    }

    private fun permissions() = Permissions(
        accessibility = LayzeeAccessibilityService.dispatcher != null,
        overlay = Settings.canDrawOverlays(this),
        camera = hasPermission(Manifest.permission.CAMERA),
    )

    private fun refresh() {
        val permissions = permissions()
        val state = store.state
        bindStep(stepAccessibility, R.string.step_accessibility, permissions.accessibility && state.consentGiven)
        bindStep(stepOverlay, R.string.step_overlay, permissions.overlay)
        bindStep(stepCamera, R.string.step_camera, permissions.camera)
        bindStep(stepBattery, R.string.step_battery, state.batteryDone)
        batteryDone.isChecked = state.batteryDone
        diedHint.visibility = if (state.diedUnexpectedly) View.VISIBLE else View.GONE
        val running = LayzeeService.running
        toggle.setText(if (running) R.string.stop else R.string.start)
        // Stop is always available; Start only once the guide is complete.
        toggle.isEnabled = running || state.canStart(permissions)
    }

    private fun bindStep(view: TextView, @StringRes title: Int, done: Boolean) {
        view.text = getString(if (done) R.string.step_done else R.string.step_todo, getString(title))
    }

    // The explicit consent (an affirmative tap, remembered) comes before the user is sent to Settings.
    private fun openAccessibility() {
        if (store.state.consentGiven) {
            openSettings(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            return
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.consent_title)
            .setMessage(R.string.consent_message)
            .setPositiveButton(R.string.consent_agree) { _, _ ->
                store.state = store.state.copy(consentGiven = true)
                openSettings(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            .setNegativeButton(R.string.consent_cancel, null)
            .show()
    }

    private fun openAppInfo() =
        openSettings(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))

    // Some vendor ROMs lack individual settings pages; the app info page always exists.
    private fun openSettings(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            try {
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
            } catch (e: ActivityNotFoundException) {
                // Nowhere left to send the user.
            }
        }
    }

    private fun showPlayground(show: Boolean) {
        playground.visibility = if (show) View.VISIBLE else View.GONE
        closePlayground.isEnabled = show
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
