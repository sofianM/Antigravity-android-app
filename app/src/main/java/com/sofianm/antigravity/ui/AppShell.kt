package com.sofianm.antigravity.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewSidebar
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Splitscreen
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WebAsset
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sofianm.antigravity.AntigravityViewModel
import com.sofianm.antigravity.AppScreen
import com.sofianm.antigravity.data.RenderEngine
import com.sofianm.antigravity.foldable.DevicePosture
import com.sofianm.antigravity.qr.QrScannerScreen
import com.sofianm.antigravity.ui.theme.resolveDarkTheme
import com.sofianm.antigravity.web.PaneSlot

@Composable
fun AppShell(
    activity: Activity,
    viewModel: AntigravityViewModel,
    onRequestNotificationPermission: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val foldState by viewModel.foldState.collectAsStateWithLifecycle()
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val compactSlot by viewModel.compactActiveSlot.collectAsStateWithLifecycle()
    val splitRatio by viewModel.dualPaneSplitRatio.collectAsStateWithLifecycle()
    val railCollapsed by viewModel.railCollapsed.collectAsStateWithLifecycle()
    val showSettings by viewModel.showSettingsSheet.collectAsStateWithLifecycle()
    val profileDialog by viewModel.profileDialog.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    val primaryPaneState by viewModel.webSessionHolder.primaryState.collectAsStateWithLifecycle()
    val secondaryPaneState by viewModel.webSessionHolder.secondaryState.collectAsStateWithLifecycle()

    val isDarkTheme = resolveDarkTheme(settings.themeMode)
    val snackbarHostState = remember { SnackbarHostState() }

    // Apply adaptive per-screen zoom & layout settings immediately on fold/unfold
    LaunchedEffect(
        foldState.isExpandedScreen,
        settings.coverZoomPercent,
        settings.expandedZoomPercent,
        settings.expandedWideLayout,
        isDarkTheme
    ) {
        val zoom = if (foldState.isExpandedScreen) {
            settings.expandedZoomPercent
        } else {
            settings.coverZoomPercent
        }
        val wideLayout = if (foldState.isExpandedScreen) settings.expandedWideLayout else false
        viewModel.webSessionHolder.applyDisplaySettings(
            textZoomPercent = zoom,
            wideLayout = wideLayout,
            isDarkTheme = isDarkTheme
        )
    }

    // Optional auto-launch of default profile on startup
    LaunchedEffect(settings.profiles, settings.autoLaunchDefault) {
        if (settings.profiles.isNotEmpty() && settings.autoLaunchDefault) {
            viewModel.maybeAutoLaunchDefault(activity, isDarkTheme)
        }
    }

    LaunchedEffect(toastMessage) {
        val msg = toastMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.consumeMessage()
    }

    // System back button navigation
    BackHandler(enabled = currentScreen != AppScreen.HUB) {
        when (currentScreen) {
            AppScreen.QR_SCANNER -> viewModel.closeQrScanner()
            AppScreen.WEB_WORKSPACE -> {
                val activeSlot = if (foldState.isExpandedScreen) PaneSlot.PRIMARY else compactSlot
                val wentBack = viewModel.webSessionHolder.goBack(activeSlot)
                if (!wentBack) {
                    viewModel.openHub()
                }
            }
            AppScreen.HUB -> Unit
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            when (currentScreen) {
                AppScreen.QR_SCANNER -> {
                    QrScannerScreen(
                        onUrlScanned = { scannedUrl -> viewModel.onQrUrlScanned(scannedUrl) },
                        onClose = { viewModel.closeQrScanner() }
                    )
                }

                AppScreen.HUB -> {
                    WorkspaceHubScreen(
                        settings = settings,
                        foldState = foldState,
                        onAddUrlClick = { viewModel.openAddProfileDialog() },
                        onScanQrClick = { viewModel.openQrScanner() },
                        onOpenSettingsClick = { viewModel.openSettings() },
                        onOpenProfile = { profile, forceEngine, slot, preferSideSheet ->
                            viewModel.openProfile(
                                activity = activity,
                                profile = profile,
                                isDarkTheme = isDarkTheme,
                                forceEngine = forceEngine,
                                targetSlot = slot,
                                preferSideSheet = preferSideSheet
                            )
                        },
                        onLaunchAdjacentSplit = { profile ->
                            viewModel.launchAdjacentSplitScreen(activity, profile.url, isDarkTheme)
                        },
                        onEditProfile = { viewModel.openEditProfileDialog(it) },
                        onDeleteProfile = { viewModel.deleteProfile(it.id) },
                        onSetDefaultProfile = { viewModel.setDefaultProfile(it.id) }
                    )
                }

                AppScreen.WEB_WORKSPACE -> {
                    if (!foldState.isExpandedScreen) {
                        // Compact phone & Pixel 9 Pro Fold closed cover screen
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .imePadding()
                        ) {
                            val activeState = if (compactSlot == PaneSlot.PRIMARY) {
                                primaryPaneState
                            } else {
                                secondaryPaneState
                            }
                            val activeProfile = if (compactSlot == PaneSlot.PRIMARY) {
                                settings.activePane1Profile
                            } else {
                                settings.activePane2Profile
                            }

                            WebPaneView(
                                slot = compactSlot,
                                state = activeState,
                                profiles = settings.profiles,
                                activeProfile = activeProfile,
                                webSessionHolder = viewModel.webSessionHolder,
                                pullToRefreshEnabled = settings.pullToRefreshEnabled,
                                showHomeButton = true,
                                showSettingsButton = true,
                                paneLabel = if (secondaryPaneState.currentUrl.isNotBlank()) {
                                    if (compactSlot == PaneSlot.PRIMARY) "Pane 1" else "Pane 2"
                                } else {
                                    null
                                },
                                onSelectProfile = { profile ->
                                    viewModel.openProfile(
                                        activity = activity,
                                        profile = profile,
                                        isDarkTheme = isDarkTheme,
                                        forceEngine = RenderEngine.NATIVE_WEBVIEW,
                                        targetSlot = compactSlot
                                    )
                                },
                                onOpenInSafeMode = { url ->
                                    viewModel.openUrlInSafeCustomTab(activity, url, isDarkTheme)
                                },
                                onHomeClick = { viewModel.openHub() },
                                onSettingsClick = { viewModel.openSettings() },
                                modifier = Modifier.weight(1f)
                            )

                            // If user previously opened Dual-Pane on unfolded Fold, keep quick switcher on cover screen
                            if (secondaryPaneState.currentUrl.isNotBlank()) {
                                Surface(tonalElevation = 3.dp) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .navigationBarsPadding()
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                                    ) {
                                        FilterChip(
                                            selected = compactSlot == PaneSlot.PRIMARY,
                                            onClick = { viewModel.switchCompactSlot(PaneSlot.PRIMARY) },
                                            label = { Text("Pane 1") }
                                        )
                                        FilterChip(
                                            selected = compactSlot == PaneSlot.SECONDARY,
                                            onClick = { viewModel.switchCompactSlot(PaneSlot.SECONDARY) },
                                            label = { Text("Pane 2") }
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Expanded Pixel 9 Pro Fold inner display & tablets (>= 600dp)
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .imePadding()
                        ) {
                            if (!railCollapsed) {
                                NavigationRail(
                                    header = {
                                        IconButton(onClick = { viewModel.toggleRailCollapsed() }) {
                                            Icon(
                                                imageVector = Icons.Default.ChevronLeft,
                                                contentDescription = "Zijbalk inklappen"
                                            )
                                        }
                                    }
                                ) {
                                    NavigationRailItem(
                                        selected = false,
                                        onClick = { viewModel.openHub() },
                                        icon = { Icon(Icons.Default.Home, contentDescription = "Hub") },
                                        label = { Text("Hub") }
                                    )
                                    NavigationRailItem(
                                        selected = settings.dualPaneEnabledOnFold,
                                        onClick = {
                                            viewModel.setDualPaneEnabledOnFold(!settings.dualPaneEnabledOnFold)
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = if (settings.dualPaneEnabledOnFold) {
                                                    Icons.Default.Splitscreen
                                                } else {
                                                    Icons.Default.WebAsset
                                                },
                                                contentDescription = "Dual-Pane"
                                            )
                                        },
                                        label = { Text(if (settings.dualPaneEnabledOnFold) "Dual" else "Enkel") }
                                    )
                                    NavigationRailItem(
                                        selected = false,
                                        onClick = {
                                            val url = primaryPaneState.currentUrl.ifBlank {
                                                settings.activePane1Profile?.url.orEmpty()
                                            }
                                            if (url.isNotBlank()) {
                                                viewModel.customTabsManager.launchFoldSideSheet(
                                                    activity,
                                                    url,
                                                    isDarkTheme
                                                )
                                            }
                                        },
                                        icon = {
                                            Icon(
                                                Icons.AutoMirrored.Filled.ViewSidebar,
                                                contentDescription = "Side-Sheet"
                                            )
                                        },
                                        label = { Text("Veilig") }
                                    )
                                    NavigationRailItem(
                                        selected = false,
                                        onClick = {
                                            val url = settings.activePane2Profile?.url
                                                ?: settings.activePane1Profile?.url
                                                ?: primaryPaneState.currentUrl
                                            if (url.isNotBlank()) {
                                                viewModel.launchAdjacentSplitScreen(activity, url, isDarkTheme)
                                            }
                                        },
                                        icon = {
                                            Icon(
                                                Icons.Default.VerifiedUser,
                                                contentDescription = "Split Chrome"
                                            )
                                        },
                                        label = { Text("Split") }
                                    )
                                    NavigationRailItem(
                                        selected = false,
                                        onClick = { viewModel.openQrScanner() },
                                        icon = {
                                            Icon(Icons.Default.QrCodeScanner, contentDescription = "QR")
                                        },
                                        label = { Text("QR") }
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    NavigationRailItem(
                                        selected = showSettings,
                                        onClick = { viewModel.openSettings() },
                                        icon = {
                                            Icon(Icons.Default.Settings, contentDescription = "Instellingen")
                                        },
                                        label = { Text("Opties") }
                                    )
                                }
                            } else {
                                // Ultra-slim expand tab when rail is collapsed for full-width view
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.fillMaxHeight()
                                ) {
                                    IconButton(
                                        onClick = { viewModel.toggleRailCollapsed() },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = "Zijbalk uitklappen",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            // Main unfolded workspace canvas
                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                when {
                                    foldState.posture == DevicePosture.TABLETOP_MODE -> {
                                        // Pixel 9 Pro Fold half-opened horizontally: Web on top, quick deck below
                                        Column(modifier = Modifier.fillMaxSize()) {
                                            WebPaneView(
                                                slot = PaneSlot.PRIMARY,
                                                state = primaryPaneState,
                                                profiles = settings.profiles,
                                                activeProfile = settings.activePane1Profile,
                                                webSessionHolder = viewModel.webSessionHolder,
                                                pullToRefreshEnabled = settings.pullToRefreshEnabled,
                                                showHomeButton = railCollapsed,
                                                showSettingsButton = railCollapsed,
                                                onSelectProfile = { profile ->
                                                    viewModel.openProfile(
                                                        activity,
                                                        profile,
                                                        isDarkTheme,
                                                        RenderEngine.NATIVE_WEBVIEW,
                                                        PaneSlot.PRIMARY
                                                    )
                                                },
                                                onOpenInSafeMode = { url ->
                                                    viewModel.openUrlInSafeCustomTab(activity, url, isDarkTheme)
                                                },
                                                onHomeClick = { viewModel.openHub() },
                                                onSettingsClick = { viewModel.openSettings() },
                                                modifier = Modifier.weight(1f)
                                            )
                                            HorizontalDivider()
                                            TabletopControlDeck(
                                                profiles = settings.profiles,
                                                onSelectProfile = { profile ->
                                                    viewModel.openProfile(
                                                        activity,
                                                        profile,
                                                        isDarkTheme,
                                                        RenderEngine.NATIVE_WEBVIEW,
                                                        PaneSlot.PRIMARY
                                                    )
                                                },
                                                onOpenSafeMode = {
                                                    val url = primaryPaneState.currentUrl.ifBlank {
                                                        settings.activePane1Profile?.url.orEmpty()
                                                    }
                                                    if (url.isNotBlank()) {
                                                        viewModel.openUrlInSafeCustomTab(activity, url, isDarkTheme)
                                                    }
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .weight(0.55f)
                                            )
                                        }
                                    }

                                    settings.dualPaneEnabledOnFold -> {
                                        // Hinge-aware resizable Dual-Pane for Pixel 9 Pro Fold inner display
                                        val density = LocalDensity.current
                                        val dividerWidthDp = if (foldState.hasVerticalHinge && foldState.hingeWidthPx > 0) {
                                            with(density) { foldState.hingeWidthPx.toDp() }.coerceAtLeast(10.dp)
                                        } else {
                                            10.dp
                                        }

                                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                            val totalWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
                                            val leftWeight = splitRatio.coerceIn(0.3f, 0.7f)
                                            val rightWeight = (1f - leftWeight).coerceIn(0.3f, 0.7f)

                                            Row(modifier = Modifier.fillMaxSize()) {
                                                WebPaneView(
                                                    slot = PaneSlot.PRIMARY,
                                                    state = primaryPaneState,
                                                    profiles = settings.profiles,
                                                    activeProfile = settings.activePane1Profile,
                                                    webSessionHolder = viewModel.webSessionHolder,
                                                    pullToRefreshEnabled = settings.pullToRefreshEnabled,
                                                    showHomeButton = railCollapsed,
                                                    showSettingsButton = false,
                                                    paneLabel = "Pane 1",
                                                    onSelectProfile = { profile ->
                                                        viewModel.openProfile(
                                                            activity,
                                                            profile,
                                                            isDarkTheme,
                                                            RenderEngine.NATIVE_WEBVIEW,
                                                            PaneSlot.PRIMARY
                                                        )
                                                    },
                                                    onOpenInSafeMode = { url ->
                                                        viewModel.openUrlInSafeCustomTab(activity, url, isDarkTheme)
                                                    },
                                                    onHomeClick = { viewModel.openHub() },
                                                    onSettingsClick = { viewModel.openSettings() },
                                                    modifier = Modifier
                                                        .weight(leftWeight)
                                                        .fillMaxHeight()
                                                )

                                                // Draggable hinge divider handle
                                                Box(
                                                    modifier = Modifier
                                                        .width(dividerWidthDp)
                                                        .fillMaxHeight()
                                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                                        .pointerInput(totalWidthPx) {
                                                            detectHorizontalDragGestures { change, dragAmount ->
                                                                change.consume()
                                                                val delta = dragAmount / totalWidthPx
                                                                viewModel.updateDualPaneSplitRatio(splitRatio + delta)
                                                            }
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .width(4.dp)
                                                            .height(44.dp)
                                                            .background(
                                                                color = MaterialTheme.colorScheme.outline,
                                                                shape = RoundedCornerShape(4.dp)
                                                            )
                                                    )
                                                }

                                                WebPaneView(
                                                    slot = PaneSlot.SECONDARY,
                                                    state = secondaryPaneState,
                                                    profiles = settings.profiles,
                                                    activeProfile = settings.activePane2Profile,
                                                    webSessionHolder = viewModel.webSessionHolder,
                                                    pullToRefreshEnabled = settings.pullToRefreshEnabled,
                                                    showHomeButton = false,
                                                    showSettingsButton = railCollapsed,
                                                    paneLabel = "Pane 2",
                                                    onSelectProfile = { profile ->
                                                        viewModel.openProfile(
                                                            activity,
                                                            profile,
                                                            isDarkTheme,
                                                            RenderEngine.NATIVE_WEBVIEW,
                                                            PaneSlot.SECONDARY
                                                        )
                                                    },
                                                    onOpenInSafeMode = { url ->
                                                        viewModel.openUrlInSafeCustomTab(activity, url, isDarkTheme)
                                                    },
                                                    onHomeClick = { viewModel.openHub() },
                                                    onSettingsClick = { viewModel.openSettings() },
                                                    modifier = Modifier
                                                        .weight(rightWeight)
                                                        .fillMaxHeight()
                                                )
                                            }
                                        }
                                    }

                                    else -> {
                                        // Full-width single expanded pane
                                        WebPaneView(
                                            slot = PaneSlot.PRIMARY,
                                            state = primaryPaneState,
                                            profiles = settings.profiles,
                                            activeProfile = settings.activePane1Profile,
                                            webSessionHolder = viewModel.webSessionHolder,
                                            pullToRefreshEnabled = settings.pullToRefreshEnabled,
                                            showHomeButton = railCollapsed,
                                            showSettingsButton = railCollapsed,
                                            onSelectProfile = { profile ->
                                                viewModel.openProfile(
                                                    activity,
                                                    profile,
                                                    isDarkTheme,
                                                    RenderEngine.NATIVE_WEBVIEW,
                                                    PaneSlot.PRIMARY
                                                )
                                            },
                                            onOpenInSafeMode = { url ->
                                                viewModel.openUrlInSafeCustomTab(activity, url, isDarkTheme)
                                            },
                                            onHomeClick = { viewModel.openHub() },
                                            onSettingsClick = { viewModel.openSettings() },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Profile Dialog
    profileDialog?.let { dialogState ->
        ProfileEditorDialog(
            state = dialogState,
            onScanQrClick = { viewModel.openQrScanner() },
            onDismiss = { viewModel.dismissProfileDialog() },
            onConfirm = { existingId, name, normalizedUrl, engine, setAsDefault, openNow ->
                viewModel.saveProfileAndOptionallyOpen(
                    activity = activity,
                    existingId = existingId,
                    name = name,
                    normalizedUrl = normalizedUrl,
                    engine = engine,
                    setAsDefault = setAsDefault,
                    openImmediately = openNow,
                    isDarkTheme = isDarkTheme
                )
            }
        )
    }

    // Settings Bottom Sheet
    if (showSettings) {
        SettingsSheet(
            settings = settings,
            onDismiss = { viewModel.closeSettings() },
            onThemeModeChange = { viewModel.setThemeMode(it) },
            onDualPaneFoldChange = { viewModel.setDualPaneEnabledOnFold(it) },
            onUseSideSheetFoldChange = { viewModel.setUseSideSheetOnFold(it) },
            onCoverZoomChange = { viewModel.setCoverZoomPercent(it) },
            onExpandedZoomChange = { viewModel.setExpandedZoomPercent(it) },
            onExpandedWideLayoutChange = { viewModel.setExpandedWideLayout(it) },
            onPullToRefreshChange = { viewModel.setPullToRefreshEnabled(it) },
            onAutoLaunchChange = { viewModel.setAutoLaunchDefault(it) },
            onNotificationsChange = { enabled ->
                if (enabled) onRequestNotificationPermission()
                viewModel.setNotificationsEnabled(enabled)
            },
            onBackgroundWatcherChange = { enabled ->
                if (enabled) onRequestNotificationPermission()
                viewModel.setBackgroundWatcherEnabled(enabled)
            },
            onSendTestNotification = {
                onRequestNotificationPermission()
                viewModel.sendTestNotification()
            },
            onClearCache = { viewModel.clearWebViewData() }
        )
    }
}

@Composable
private fun TabletopControlDeck(
    profiles: List<com.sofianm.antigravity.data.WorkspaceProfile>,
    onSelectProfile: (com.sofianm.antigravity.data.WorkspaceProfile) -> Unit,
    onOpenSafeMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Tabletop bedieningspaneel",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                profiles.take(4).forEach { profile ->
                    FilterChip(
                        selected = false,
                        onClick = { onSelectProfile(profile) },
                        label = { Text(profile.name) }
                    )
                }
                FilterChip(
                    selected = true,
                    onClick = onOpenSafeMode,
                    label = { Text("Veilige Modus") }
                )
            }
        }
    }
}
