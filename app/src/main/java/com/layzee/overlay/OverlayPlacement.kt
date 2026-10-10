package com.layzee.overlay

/** Pure placement rules for the Overlay icon and its menu, in screen pixels. */
internal object OverlayPlacement {
    /** The edge the icon sticks to after a drag: whichever side its center is closer to. */
    fun snapX(centerX: Float, screenWidth: Int, size: Int): Int =
        if (centerX < screenWidth / 2f) 0 else screenWidth - size

    fun clampY(y: Int, screenHeight: Int, size: Int): Int = y.coerceIn(0, screenHeight - size)

    /** The menu opens on the side of the icon that faces the screen center, and never leaves the screen. */
    fun menuX(iconX: Int, screenWidth: Int, iconSize: Int, menuWidth: Int, gap: Int): Int {
        val onLeft = iconX + iconSize / 2f < screenWidth / 2f
        val x = if (onLeft) iconX + iconSize + gap else iconX - menuWidth - gap
        return x.coerceIn(0, maxOf(0, screenWidth - menuWidth))
    }
}
