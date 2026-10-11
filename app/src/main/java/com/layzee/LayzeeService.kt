package com.layzee

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.layzee.gesture.GestureConfig
import com.layzee.gesture.GestureState
import com.layzee.gesture.HandFrame
import com.layzee.overlay.Overlay
import com.layzee.pipeline.CameraPipeline
import com.layzee.pipeline.HandLandmarkerSource
import com.layzee.setup.SetupStore
import com.layzee.swipe.LayzeeAccessibilityService

/**
 * The only place that runs the pipeline. A running service is "not Off": the camera stays on, even with
 * another app in front, until [stop] (there is no auto-Off). It is a camera-type foreground service, so
 * Android shows its notification.
 *
 * The camera is bound to this service's own lifecycle, which is STARTED for as long as the service lives.
 * While the screen is off the pipeline is released (camera, model and conversion stop); the service,
 * notification and overlay stay, and a new pipeline starts at Idle when the screen turns on. This is not Off
 * and not a new state.
 * A flick is one short gesture, so stopping the service never leaves a stroke hanging. Not sticky: a restart
 * without the Dashboard in front could not reopen the camera anyway.
 */
class LayzeeService : Service(), LifecycleOwner {
    private val registry = LifecycleRegistry(this)
    private var pipeline: CameraPipeline? = null
    private var overlay: Overlay? = null

    override val lifecycle: Lifecycle get() = registry

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> releasePipeline()
                Intent.ACTION_SCREEN_ON -> startPipeline()
            }
        }
    }

    // The camera image is only shown on the test screen, never here.
    private val forward = object : HandLandmarkerSource.Listener {
        override fun onFrame(frame: HandFrame, state: GestureState) {
            overlay?.setState(state)
        }

        override fun onImage(image: Bitmap) = Unit
    }

    override fun onCreate() {
        super.onCreate()
        registry.currentState = Lifecycle.State.STARTED
        running = true
        // From here until the user's Stop; a start that never gets this far is not a kill.
        SetupStore(this).let { it.state = it.state.copy(shouldRun = true) }
        onRunningChanged?.invoke()
        registerReceiver(
            screenReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
            },
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground()
        if (overlay == null) showOverlay(gestureConfig())
        startPipeline()
        return START_NOT_STICKY
    }

    private fun startPipeline() {
        if (pipeline != null) return
        overlay?.setState(GestureState.IDLE)
        pipeline = CameraPipeline(
            context = this,
            lifecycleOwner = this,
            config = gestureConfig(),
            listener = forward,
            dispatcherProvider = { LayzeeAccessibilityService.dispatcher },
        ).also { it.start() }
    }

    private fun releasePipeline() {
        pipeline?.release()
        pipeline = null
    }

    override fun onDestroy() {
        unregisterReceiver(screenReceiver)
        overlay?.hide()
        overlay = null
        releasePipeline()
        registry.currentState = Lifecycle.State.DESTROYED
        running = false
        onRunningChanged?.invoke()
        super.onDestroy()
    }

    // Without the "display over other apps" permission Layzee still runs; Stop is then only on the Dashboard.
    private fun showOverlay(config: GestureConfig) {
        if (!Settings.canDrawOverlays(this)) return
        val fresh = Overlay(
            context = this,
            screenWidth = config.screenWidth,
            screenHeight = config.screenHeight,
            onStop = { stop(this) },
            onOpenDashboard = {
                startActivity(Intent(this, DashboardActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            },
        )
        // The permission can be revoked between the check and the window being added.
        try {
            fresh.show()
            overlay = fresh
        } catch (e: WindowManager.BadTokenException) {
            fresh.hide()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForeground() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.notification_channel), NotificationManager.IMPORTANCE_LOW),
        )
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, DashboardActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notification_text))
            .setContentIntent(open)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    // The virtual finger works in real display pixels (including system bars), not app-window pixels.
    private fun gestureConfig(): GestureConfig {
        val windowManager = getSystemService(WindowManager::class.java)
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

    companion object {
        private const val CHANNEL_ID = "layzee_running"
        private const val NOTIFICATION_ID = 1

        /** True while the service exists (Idle/Tracking/Pinch); false is Off. Only [onCreate]/[onDestroy] write it. */
        @Volatile
        var running = false
            private set

        /** Called on the main thread whenever [running] changes. Set by the Dashboard while it is open. */
        internal var onRunningChanged: (() -> Unit)? = null

        /** Start from the Dashboard, while it is in front (a camera service cannot start from the background). */
        fun start(context: Context) {
            context.startForegroundService(Intent(context, LayzeeService::class.java))
        }

        fun stop(context: Context) {
            SetupStore(context).let { it.state = it.state.copy(shouldRun = false) }
            context.stopService(Intent(context, LayzeeService::class.java))
        }
    }
}
