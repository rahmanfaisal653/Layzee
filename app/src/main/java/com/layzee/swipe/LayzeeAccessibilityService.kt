package com.layzee.swipe

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * Only exists to be allowed to dispatch gestures (`canPerformGestures`); it reads no screen content and
 * listens to no events. While it is connected, [dispatcher] is the way to send the virtual finger.
 */
class LayzeeAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        super.onServiceConnected()
        dispatcher = SwipeDispatcher(this)
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        dispatcher = null
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    companion object {
        /** Non-null only while the service is enabled and connected. */
        @Volatile
        var dispatcher: SwipeDispatcher? = null
            private set
    }
}
