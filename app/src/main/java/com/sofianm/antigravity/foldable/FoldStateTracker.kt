package com.sofianm.antigravity.foldable

import android.app.Activity
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowLayoutInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class DevicePosture {
    /** Regular phone or Fold closed (cover display < 600dp). */
    COMPACT_PHONE,

    /** Unfolded Fold (e.g. Pixel 9 Pro Fold inner 8" display >= 600dp) or tablet, flat open. */
    UNFOLDED_FLAT,

    /** Fold half-opened with a vertical hinge (book posture). */
    BOOK_MODE,

    /** Fold half-opened with a horizontal hinge (tabletop posture). */
    TABLETOP_MODE
}

data class FoldUiState(
    val posture: DevicePosture = DevicePosture.COMPACT_PHONE,
    val windowWidthDp: Int = 412,
    val windowHeightDp: Int = 915,
    val hasVerticalHinge: Boolean = false,
    val hasHorizontalHinge: Boolean = false,
    val hingeBoundsLeftRatio: Float = 0.5f,
    val hingeWidthPx: Int = 0
) {
    /** True on Pixel 9 Pro Fold inner display (~700-800dp) and tablets (>= 600dp). */
    val isExpandedScreen: Boolean
        get() = windowWidthDp >= FoldMetricsCalculator.FOLD_EXPANDED_BREAKPOINT_DP ||
            posture == DevicePosture.BOOK_MODE

    val canShowDualPane: Boolean
        get() = isExpandedScreen

    val postureLabelNl: String
        get() = when (posture) {
            DevicePosture.COMPACT_PHONE -> "Telefoon / Coverscherm"
            DevicePosture.UNFOLDED_FLAT -> "Fold opengeklapt"
            DevicePosture.BOOK_MODE -> "Fold boekmodus"
            DevicePosture.TABLETOP_MODE -> "Fold tabletop-modus"
        }
}

/**
 * Pure calculation helper for foldable posture & Pixel 9 Pro Fold breakpoints so it can be unit-tested.
 */
object FoldMetricsCalculator {

    /**
     * 600dp cleanly separates regular phones / Pixel 9 Pro Fold cover display (~412dp)
     * from the Pixel 9 Pro Fold nearly-square inner display (~700-800dp) and tablets.
     */
    const val FOLD_EXPANDED_BREAKPOINT_DP = 600

    fun computeState(
        windowWidthDp: Int,
        windowHeightDp: Int,
        windowWidthPx: Int,
        hasFoldingFeature: Boolean,
        isHalfOpened: Boolean,
        isVerticalHinge: Boolean,
        hingeCenterX: Int,
        hingeWidthPx: Int
    ): FoldUiState {
        val isWide = windowWidthDp >= FOLD_EXPANDED_BREAKPOINT_DP
        val posture = when {
            hasFoldingFeature && isHalfOpened && isVerticalHinge -> DevicePosture.BOOK_MODE
            hasFoldingFeature && isHalfOpened && !isVerticalHinge -> DevicePosture.TABLETOP_MODE
            isWide -> DevicePosture.UNFOLDED_FLAT
            else -> DevicePosture.COMPACT_PHONE
        }

        val normalizedRatio = if (hasFoldingFeature && isVerticalHinge && windowWidthPx > 0) {
            (hingeCenterX.toFloat() / windowWidthPx.toFloat()).coerceIn(0.3f, 0.7f)
        } else {
            0.5f
        }

        return FoldUiState(
            posture = posture,
            windowWidthDp = windowWidthDp,
            windowHeightDp = windowHeightDp,
            hasVerticalHinge = hasFoldingFeature && isVerticalHinge,
            hasHorizontalHinge = hasFoldingFeature && !isVerticalHinge,
            hingeBoundsLeftRatio = normalizedRatio,
            hingeWidthPx = if (hasFoldingFeature) hingeWidthPx else 0
        )
    }
}

class FoldStateTracker(private val activity: Activity) {

    fun observeFoldState(): Flow<FoldUiState> {
        return WindowInfoTracker.getOrCreate(activity)
            .windowLayoutInfo(activity)
            .map { layoutInfo -> mapLayoutInfo(layoutInfo) }
    }

    fun currentSnapshot(layoutInfo: WindowLayoutInfo? = null): FoldUiState {
        return mapLayoutInfo(layoutInfo)
    }

    private fun mapLayoutInfo(layoutInfo: WindowLayoutInfo?): FoldUiState {
        val config = activity.resources.configuration
        val metrics = activity.resources.displayMetrics
        val widthDp = config.screenWidthDp
        val heightDp = config.screenHeightDp
        val widthPx = metrics.widthPixels

        val foldingFeature = layoutInfo
            ?.displayFeatures
            ?.filterIsInstance<FoldingFeature>()
            ?.firstOrNull()

        return if (foldingFeature != null) {
            val isVertical = foldingFeature.orientation == FoldingFeature.Orientation.VERTICAL
            val isHalfOpened = foldingFeature.state == FoldingFeature.State.HALF_OPENED
            FoldMetricsCalculator.computeState(
                windowWidthDp = widthDp,
                windowHeightDp = heightDp,
                windowWidthPx = widthPx,
                hasFoldingFeature = true,
                isHalfOpened = isHalfOpened,
                isVerticalHinge = isVertical,
                hingeCenterX = foldingFeature.bounds.centerX(),
                hingeWidthPx = if (isVertical) foldingFeature.bounds.width() else foldingFeature.bounds.height()
            )
        } else {
            FoldMetricsCalculator.computeState(
                windowWidthDp = widthDp,
                windowHeightDp = heightDp,
                windowWidthPx = widthPx,
                hasFoldingFeature = false,
                isHalfOpened = false,
                isVerticalHinge = false,
                hingeCenterX = widthPx / 2,
                hingeWidthPx = 0
            )
        }
    }
}
