package com.layzee.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import com.layzee.R
import com.layzee.gesture.GestureState

/**
 * The floating status icon, one small View in a `WindowManager` overlay window. Its color follows the gesture
 * state, it can be dragged and snaps to the left or right edge, and a click opens a small menu with "Stop"
 * and "Buka dashboard". No camera preview and no Start button. Windows are not focusable and the icon is
 * edge-sized, so the screen center (where the swipe starts) stays free.
 *
 * All methods except [setState] must be called on the main thread; [setState] may be called from the
 * MediaPipe callback thread and only touches the View when the state actually changed.
 */
internal class Overlay(
    private val context: Context,
    private val screenWidth: Int,
    private val screenHeight: Int,
    private val onStop: () -> Unit,
    private val onOpenDashboard: () -> Unit,
) {
    private val windowManager = context.getSystemService(WindowManager::class.java)
    private val main = Handler(Looper.getMainLooper())
    private val density = context.resources.displayMetrics.density
    private val size = (ICON_DP * density).toInt()
    private val gap = (GAP_DP * density).toInt()

    private val icon = IconView(context)
    private val iconParams = windowParams(size, size)
    private var menu: View? = null
    private var shown = false
    private var snapAnimator: ValueAnimator? = null
    @Volatile private var lastState: GestureState? = null

    fun show() {
        if (shown) return
        iconParams.x = screenWidth - size
        iconParams.y = (screenHeight * START_Y_FRACTION).toInt()
        windowManager.addView(icon, iconParams)
        shown = true
    }

    fun hide() {
        main.removeCallbacksAndMessages(null)
        snapAnimator?.cancel()
        closeMenu()
        if (shown) windowManager.removeView(icon)
        shown = false
    }

    fun setState(state: GestureState) {
        if (state == lastState) return
        lastState = state
        main.post { icon.setColorFor(state) }
    }

    private fun openMenu() {
        val panel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = 12 * density
            }
            addView(menuButton(R.string.stop) { closeMenu(); onStop() })
            addView(menuButton(R.string.open_dashboard) { closeMenu(); onOpenDashboard() })
            // A touch outside the menu (including the flick Layzee sends itself) closes it.
            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_OUTSIDE) closeMenu()
                false
            }
        }
        panel.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )
        val params = windowParams(panel.measuredWidth, panel.measuredHeight, watchOutside = true)
        params.x = OverlayPlacement.menuX(iconParams.x, screenWidth, size, panel.measuredWidth, gap)
        params.y = OverlayPlacement.clampY(iconParams.y, screenHeight, panel.measuredHeight)
        windowManager.addView(panel, params)
        menu = panel
    }

    private fun closeMenu() {
        menu?.let { windowManager.removeView(it) }
        menu = null
    }

    private fun menuButton(label: Int, onClick: () -> Unit) = Button(context).apply {
        setText(label)
        isAllCaps = false
        setOnClickListener { onClick() }
    }

    private fun windowParams(width: Int, height: Int, watchOutside: Boolean = false): WindowManager.LayoutParams {
        var flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        if (watchOutside) flags = flags or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        return WindowManager.LayoutParams(
            width, height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            flags, PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.TOP or Gravity.START }
    }

    /** A colored dot; drag moves the window, a release without drag is a click. */
    private inner class IconView(context: Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = IDLE_COLOR }
        private val slop = ViewConfiguration.get(context).scaledTouchSlop
        private var downX = 0f
        private var downY = 0f
        private var startX = 0
        private var startY = 0
        private var dragging = false
        private var menuWasOpen = false

        init {
            contentDescription = context.getString(R.string.app_name)
        }

        fun setColorFor(state: GestureState) {
            paint.color = when (state) {
                GestureState.IDLE -> IDLE_COLOR
                GestureState.TRACKING -> TRACKING_COLOR
                GestureState.PINCH -> PINCH_COLOR
            }
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            val r = width / 2f
            canvas.drawCircle(r, r, r - 2 * density, paint)
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    startX = iconParams.x
                    startY = iconParams.y
                    dragging = false
                    snapAnimator?.cancel()
                    // The menu window also sees this touch as "outside" and closes itself; remember it was open
                    // so the release does not reopen it.
                    menuWasOpen = menu != null
                    closeMenu()
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    if (!dragging && dx * dx + dy * dy > slop * slop) {
                        dragging = true
                        closeMenu()
                    }
                    if (dragging) {
                        iconParams.x = (startX + dx).toInt().coerceIn(0, screenWidth - size)
                        iconParams.y = OverlayPlacement.clampY((startY + dy).toInt(), screenHeight, size)
                        windowManager.updateViewLayout(this, iconParams)
                    }
                }
                MotionEvent.ACTION_UP -> if (dragging) snapToEdge() else if (!menuWasOpen) openMenu()
                MotionEvent.ACTION_CANCEL -> if (dragging) snapToEdge()
            }
            return true
        }

        private fun snapToEdge() {
            val target = OverlayPlacement.snapX(iconParams.x + size / 2f, screenWidth, size)
            snapAnimator = ValueAnimator.ofInt(iconParams.x, target).apply {
                duration = SNAP_MS
                addUpdateListener {
                    if (!shown) return@addUpdateListener
                    iconParams.x = it.animatedValue as Int
                    windowManager.updateViewLayout(this@IconView, iconParams)
                }
                start()
            }
        }
    }

    private companion object {
        const val ICON_DP = 48
        const val GAP_DP = 8
        const val START_Y_FRACTION = 0.3f
        const val SNAP_MS = 150L

        // Same palette as the Dashboard's status label.
        const val IDLE_COLOR = 0xCC555555.toInt()
        const val TRACKING_COLOR = 0xCCE0A000.toInt()
        const val PINCH_COLOR = 0xCC1B9E3E.toInt()
    }
}
