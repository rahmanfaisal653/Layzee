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
 * The floating status icon, one small View in a `WindowManager` overlay window. A round badge whose ring color
 * and glyph (dot / open V / closed V) follow the gesture state, so the state never relies on color alone. It can
 * be dragged and snaps to the left or right edge, and a click opens a small menu with "Stop" and "Buka dashboard".
 * No camera preview and no Start button. Windows are not focusable and the icon is
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

    /** A round badge (dark disc, state-colored ring, state glyph); drag moves the window, a release without drag is a click. */
    private inner class IconView(context: Context) : View(context) {
        private val idleColor = context.getColor(R.color.status_idle)
        private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = FILL_COLOR }
        private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = RING_DP * density
            color = idleColor
        }
        private val trackingColor = context.getColor(R.color.status_tracking)
        private val pinchColor = context.getColor(R.color.status_pinch)
        private val glyphInset = (GLYPH_INSET_DP * density).toInt()
        private val glyphs = arrayOf(R.drawable.ic_overlay_idle, R.drawable.ic_overlay_tracking, R.drawable.ic_overlay_pinch)
            .map { requireNotNull(context.getDrawable(it)) }
        private var glyph = glyphs[GestureState.IDLE.ordinal]
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
            ring.color = when (state) {
                GestureState.IDLE -> idleColor
                GestureState.TRACKING -> trackingColor
                GestureState.PINCH -> pinchColor
            }
            glyph = glyphs[state.ordinal]
            invalidate()
        }

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            for (g in glyphs) g.setBounds(glyphInset, glyphInset, w - glyphInset, h - glyphInset)
        }

        override fun onDraw(canvas: Canvas) {
            val r = width / 2f
            val edge = r - ring.strokeWidth / 2 - density
            canvas.drawCircle(r, r, edge, fill)
            canvas.drawCircle(r, r, edge, ring)
            glyph.draw(canvas)
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

        const val RING_DP = 3
        const val GLYPH_INSET_DP = 11

        // Ring colors are the status_* resources, shared with the Dashboard status label; the glyph carries the
        // state without color.
        const val FILL_COLOR = 0xCC202020.toInt()
    }
}
