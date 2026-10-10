package com.layzee.overlay

import org.junit.Assert.assertEquals
import org.junit.Test

class OverlayPlacementTest {
    private val screenW = 1080
    private val screenH = 2400
    private val icon = 144

    @Test fun snapsToLeftEdgeWhenCenterIsInLeftHalf() {
        assertEquals(0, OverlayPlacement.snapX(centerX = 300f, screenWidth = screenW, size = icon))
    }

    @Test fun snapsToRightEdgeWhenCenterIsInRightHalf() {
        assertEquals(screenW - icon, OverlayPlacement.snapX(centerX = 800f, screenWidth = screenW, size = icon))
    }

    @Test fun clampsYInsideTheScreen() {
        assertEquals(0, OverlayPlacement.clampY(-50, screenH, icon))
        assertEquals(screenH - icon, OverlayPlacement.clampY(screenH, screenH, icon))
        assertEquals(700, OverlayPlacement.clampY(700, screenH, icon))
    }

    @Test fun menuOpensToTheRightOfAnIconOnTheLeftEdge() {
        assertEquals(icon + 16, OverlayPlacement.menuX(iconX = 0, screenWidth = screenW, iconSize = icon, menuWidth = 400, gap = 16))
    }

    @Test fun menuOpensToTheLeftOfAnIconOnTheRightEdge() {
        val iconX = screenW - icon
        assertEquals(iconX - 400 - 16, OverlayPlacement.menuX(iconX, screenW, icon, menuWidth = 400, gap = 16))
    }

    @Test fun menuIsClampedToTheScreen() {
        // Icon at x=100 (center in the left half) would put the menu at 260..660 on a 600 px wide screen.
        assertEquals(200, OverlayPlacement.menuX(iconX = 100, screenWidth = 600, iconSize = icon, menuWidth = 400, gap = 16))
    }
}
