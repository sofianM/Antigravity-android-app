package com.sofianm.antigravity.web

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession

/**
 * Manages Chrome Custom Tabs warmup, prefetching, and adaptive launch modes:
 * - Full-screen themed Custom Tab (regular phone & Fold cover screen)
 * - Partial Custom Tab Side Sheet (Pixel 9 Pro Fold unfolded inner screen >= 600dp)
 * - Adjacent Multi-Window Split-Screen launch (2 Google-logged-in sessions side-by-side on Fold)
 */
class CustomTabsManager(private val appContext: Context) {

    private var client: CustomTabsClient? = null
    private var session: CustomTabsSession? = null
    private var boundPackageName: String? = null
    private var isBound = false

    private val connection = object : CustomTabsServiceConnection() {
        override fun onCustomTabsServiceConnected(name: ComponentName, customTabsClient: CustomTabsClient) {
            client = customTabsClient
            runCatching { customTabsClient.warmup(0L) }
            session = customTabsClient.newSession(null)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            client = null
            session = null
            isBound = false
        }
    }

    fun bind() {
        if (isBound) return
        val preferredPackages = listOf(
            "com.android.chrome",
            "com.chrome.beta",
            "com.chrome.dev",
            "com.google.android.apps.chrome"
        )
        val packageName = CustomTabsClient.getPackageName(appContext, preferredPackages, false)
            ?: CustomTabsClient.getPackageName(appContext, emptyList())
            ?: return

        boundPackageName = packageName
        isBound = runCatching {
            CustomTabsClient.bindCustomTabsService(appContext, packageName, connection)
        }.getOrDefault(false)
    }

    fun unbind() {
        if (!isBound) return
        runCatching { appContext.unbindService(connection) }
        isBound = false
        client = null
        session = null
    }

    fun prefetchUrl(url: String) {
        val parsed = runCatching { Uri.parse(url) }.getOrNull() ?: return
        runCatching { session?.mayLaunchUrl(parsed, null, null) }
    }

    fun launchFullScreen(activity: Activity, url: String, isDarkTheme: Boolean) {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return
        val customTabsIntent = buildBaseBuilder(isDarkTheme).build()
        launchSafe(activity, customTabsIntent, uri)
    }

    /**
     * Opens a Partial Custom Tab as a Side Sheet on unfolded Fold screens (breakpoint 600dp
     * specifically tuned for Pixel 9 Pro Fold's nearly-square ~700-800dp inner display).
     * Falls back seamlessly to full-screen on compact phones or browsers without partial tab support.
     */
    fun launchFoldSideSheet(activity: Activity, url: String, isDarkTheme: Boolean) {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return
        val displayMetrics = activity.resources.displayMetrics
        val sideSheetWidthPx = (displayMetrics.widthPixels * 0.68f).toInt().coerceAtLeast(1)
        val initialHeightPx = (displayMetrics.heightPixels * 0.88f).toInt().coerceAtLeast(1)

        val builder = buildBaseBuilder(isDarkTheme)
            .setInitialActivityWidthPx(sideSheetWidthPx)
            .setInitialActivityHeightPx(initialHeightPx)
            .setActivitySideSheetBreakpointDp(600)
            .setActivitySideSheetPosition(CustomTabsIntent.ACTIVITY_SIDE_SHEET_POSITION_END)
            .setActivitySideSheetDecorationType(
                CustomTabsIntent.ACTIVITY_SIDE_SHEET_DECORATION_TYPE_SHADOW
            )
            .setBackgroundInteractionEnabled(true)

        val customTabsIntent = builder.build()
        customTabsIntent.intent.putExtra(
            CustomTabsIntent.EXTRA_ACTIVITY_SIDE_SHEET_ROUNDED_CORNERS_POSITION,
            CustomTabsIntent.ACTIVITY_SIDE_SHEET_ROUNDED_CORNERS_POSITION_TOP
        )
        launchSafe(activity, customTabsIntent, uri)
    }

    /**
     * Launches a second Custom Tab window adjacent in Android Multi-Window Split-Screen
     * so Pixel 9 Pro Fold users can view two Google-authenticated sessions side-by-side.
     */
    fun launchAdjacentSplitScreen(activity: Activity, url: String, isDarkTheme: Boolean) {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return
        val customTabsIntent = buildBaseBuilder(isDarkTheme).build()
        customTabsIntent.intent.addFlags(
            Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT or
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK
        )
        launchSafe(activity, customTabsIntent, uri)
    }

    private fun buildBaseBuilder(isDarkTheme: Boolean): CustomTabsIntent.Builder {
        val darkColors = CustomTabColorSchemeParams.Builder()
            .setToolbarColor(Color.parseColor("#0B1026"))
            .setNavigationBarColor(Color.parseColor("#0B1026"))
            .setSecondaryToolbarColor(Color.parseColor("#141B3A"))
            .build()

        val lightColors = CustomTabColorSchemeParams.Builder()
            .setToolbarColor(Color.parseColor("#F5F7FF"))
            .setNavigationBarColor(Color.parseColor("#F5F7FF"))
            .setSecondaryToolbarColor(Color.parseColor("#E6ECFF"))
            .build()

        val colorScheme = if (isDarkTheme) {
            CustomTabsIntent.COLOR_SCHEME_DARK
        } else {
            CustomTabsIntent.COLOR_SCHEME_LIGHT
        }

        return CustomTabsIntent.Builder(session)
            .setColorScheme(colorScheme)
            .setDefaultColorSchemeParams(if (isDarkTheme) darkColors else lightColors)
            .setColorSchemeParams(CustomTabsIntent.COLOR_SCHEME_DARK, darkColors)
            .setColorSchemeParams(CustomTabsIntent.COLOR_SCHEME_LIGHT, lightColors)
            .setUrlBarHidingEnabled(true)
            .setShowTitle(true)
            .setShareState(CustomTabsIntent.SHARE_STATE_ON)
            .setBookmarksButtonEnabled(false)
            .setDownloadButtonEnabled(true)
    }

    private fun launchSafe(activity: Activity, customTabsIntent: CustomTabsIntent, uri: Uri) {
        boundPackageName?.let { pkg ->
            customTabsIntent.intent.setPackage(pkg)
        }
        runCatching {
            customTabsIntent.launchUrl(activity, uri)
        }.onFailure {
            // Fallback to default browser if the bound package is unavailable
            val fallback = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { activity.startActivity(fallback) }
        }
    }
}
