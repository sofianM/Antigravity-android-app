package com.sofianm.antigravity

import com.sofianm.antigravity.foldable.DevicePosture
import com.sofianm.antigravity.foldable.FoldMetricsCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FoldMetricsCalculatorTest {

    @Test
    fun pixel9ProFoldCoverScreen_isCompactPhone() {
        val state = FoldMetricsCalculator.computeState(
            windowWidthDp = 412,
            windowHeightDp = 915,
            windowWidthPx = 1080,
            hasFoldingFeature = false,
            isHalfOpened = false,
            isVerticalHinge = false,
            hingeCenterX = 540,
            hingeWidthPx = 0
        )
        assertEquals(DevicePosture.COMPACT_PHONE, state.posture)
        assertFalse(state.isExpandedScreen)
        assertFalse(state.canShowDualPane)
    }

    @Test
    fun pixel9ProFoldUnfoldedInnerScreen_isExpandedFlat() {
        // Pixel 9 Pro Fold inner display is nearly square (~730dp x 756dp)
        val state = FoldMetricsCalculator.computeState(
            windowWidthDp = 730,
            windowHeightDp = 756,
            windowWidthPx = 2076,
            hasFoldingFeature = true,
            isHalfOpened = false,
            isVerticalHinge = true,
            hingeCenterX = 1038,
            hingeWidthPx = 0
        )
        assertEquals(DevicePosture.UNFOLDED_FLAT, state.posture)
        assertTrue(state.isExpandedScreen)
        assertTrue(state.canShowDualPane)
        assertEquals(0.5f, state.hingeBoundsLeftRatio, 0.01f)
    }

    @Test
    fun bookMode_detectsVerticalHalfOpenedHinge() {
        val state = FoldMetricsCalculator.computeState(
            windowWidthDp = 730,
            windowHeightDp = 756,
            windowWidthPx = 2000,
            hasFoldingFeature = true,
            isHalfOpened = true,
            isVerticalHinge = true,
            hingeCenterX = 1000,
            hingeWidthPx = 24
        )
        assertEquals(DevicePosture.BOOK_MODE, state.posture)
        assertTrue(state.isExpandedScreen)
        assertTrue(state.hasVerticalHinge)
        assertEquals(24, state.hingeWidthPx)
    }

    @Test
    fun tabletopMode_detectsHorizontalHalfOpenedHinge() {
        val state = FoldMetricsCalculator.computeState(
            windowWidthDp = 756,
            windowHeightDp = 730,
            windowWidthPx = 2152,
            hasFoldingFeature = true,
            isHalfOpened = true,
            isVerticalHinge = false,
            hingeCenterX = 1076,
            hingeWidthPx = 24
        )
        assertEquals(DevicePosture.TABLETOP_MODE, state.posture)
        assertTrue(state.hasHorizontalHinge)
    }
}
