package com.layzee.swipe

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.graphics.Path
import android.util.Log
import com.layzee.BuildConfig
import com.layzee.setup.SettingsStore
import com.layzee.setup.Tuning
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Sends the engine's flick as one Accessibility gesture: a fast straight stroke that ends with the finger
 * still moving, so paging apps (Reels, TikTok) treat it as a fling and change page. The whole stroke is one
 * `dispatchGesture` call, so there is nothing to lift or cancel later. A flick asked for while the previous
 * one is still running is dropped (a new gesture would cancel the running one).
 *
 * Called from the MediaPipe callback thread, callbacks arrive on the main thread. The Accessibility API
 * allocates per gesture (stroke, description); that cannot be reused, but a flick is rare. The flick duration
 * is the user's swipe speed setting, read per flick so a change applies at once.
 */
class SwipeDispatcher(private val service: AccessibilityService) {
    private val path = Path() // StrokeDescription copies the path, so one instance is reused
    private val settings = SettingsStore(service)
    private val inFlight = AtomicBoolean(false)

    private val callback = object : AccessibilityService.GestureResultCallback() {
        override fun onCompleted(description: GestureDescription) {
            inFlight.set(false)
        }

        override fun onCancelled(description: GestureDescription) {
            if (BuildConfig.DEBUG) Log.d("LayzeeSwipe", "flick cancelled")
            inFlight.set(false)
        }
    }

    /** One fast swipe from ([x], [fromY]) to ([x], [toY]). */
    fun flick(x: Float, fromY: Float, toY: Float) {
        if (!inFlight.compareAndSet(false, true)) return
        path.rewind()
        path.moveTo(x, fromY)
        path.lineTo(x, toY)
        val gesture = GestureDescription.Builder().addStroke(StrokeDescription(path, 0, Tuning.flickMs(settings.speedLevel))).build()
        if (!service.dispatchGesture(gesture, callback, null)) {
            if (BuildConfig.DEBUG) Log.d("LayzeeSwipe", "dispatchGesture rejected")
            inFlight.set(false)
        }
    }
}
