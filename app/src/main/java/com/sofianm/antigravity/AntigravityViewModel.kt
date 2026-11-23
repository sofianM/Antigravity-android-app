package com.sofianm.antigravity

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sofianm.antigravity.data.AppSettings
import com.sofianm.antigravity.data.RenderEngine
import com.sofianm.antigravity.data.SettingsRepository
import com.sofianm.antigravity.data.ThemeMode
import com.sofianm.antigravity.data.UrlValidator
import com.sofianm.antigravity.data.WorkspaceProfile
import com.sofianm.antigravity.foldable.FoldUiState
import com.sofianm.antigravity.web.ActivityMonitorService
import com.sofianm.antigravity.web.CustomTabsManager
import com.sofianm.antigravity.web.PaneSlot
import com.sofianm.antigravity.web.WebNotificationBridge
import com.sofianm.antigravity.web.WebSessionHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen {
    HUB,
    WEB_WORKSPACE,
    QR_SCANNER
}

data class ProfileDialogState(
    val editingProfile: WorkspaceProfile? = null,
    val initialUrl: String = "",
    val initialName: String = "",
    val fromQrScan: Boolean = false
)

class AntigravityViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application)
    val customTabsManager = CustomTabsManager(application)

    @Volatile
    private var appInForeground = true

    val settings: StateFlow<AppSettings> = repository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppSettings()
    )

    val notificationBridge = WebNotificationBridge(
        context = application,
        isNotificationsEnabled = { settings.value.notificationsEnabled },
        isAppInForeground = { appInForeground }
    )

    val webSessionHolder = WebSessionHolder(
        appContext = application,
        notificationBridge = notificationBridge
    )

    private val _foldState = MutableStateFlow(FoldUiState())
    val foldState: StateFlow<FoldUiState> = _foldState.asStateFlow()

    private val _currentScreen = MutableStateFlow(AppScreen.HUB)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _compactActiveSlot = MutableStateFlow(PaneSlot.PRIMARY)
    val compactActiveSlot: StateFlow<PaneSlot> = _compactActiveSlot.asStateFlow()

    private val _dualPaneSplitRatio = MutableStateFlow(0.5f)
    val dualPaneSplitRatio: StateFlow<Float> = _dualPaneSplitRatio.asStateFlow()

    private val _railCollapsed = MutableStateFlow(false)
    val railCollapsed: StateFlow<Boolean> = _railCollapsed.asStateFlow()

    private val _showSettingsSheet = MutableStateFlow(false)
    val showSettingsSheet: StateFlow<Boolean> = _showSettingsSheet.asStateFlow()

    private val _profileDialog = MutableStateFlow<ProfileDialogState?>(null)
    val profileDialog: StateFlow<ProfileDialogState?> = _profileDialog.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private var hasAutoLaunched = false

    init {
        customTabsManager.bind()
        viewModelScope.launch {
            settings.collectLatest { current ->
                current.defaultProfile?.let { profile ->
                    customTabsManager.prefetchUrl(profile.url)
                }
            }
        }
    }

    fun setAppInForeground(inForeground: Boolean) {
        appInForeground = inForeground
    }

    fun updateFoldState(state: FoldUiState) {
        _foldState.value = state
        if (state.hasVerticalHinge) {
            _dualPaneSplitRatio.value = state.hingeBoundsLeftRatio
        }
    }

    fun maybeAutoLaunchDefault(activity: Activity, isDarkTheme: Boolean) {
        if (hasAutoLaunched) return
        val current = settings.value
        val defaultProfile = current.defaultProfile ?: return
        if (!current.autoLaunchDefault) return
        hasAutoLaunched = true
        openProfile(activity, defaultProfile, isDarkTheme, forceEngine = null)
    }

    fun openHub() {
        _currentScreen.value = AppScreen.HUB
    }

    fun openQrScanner() {
        _profileDialog.value = null
        _currentScreen.value = AppScreen.QR_SCANNER
    }

    fun closeQrScanner() {
        _currentScreen.value = AppScreen.HUB
    }

    fun onQrUrlScanned(url: String) {
        _currentScreen.value = AppScreen.HUB
        _profileDialog.value = ProfileDialogState(
            initialUrl = url,
            initialName = UrlValidator.suggestProfileName(url),
            fromQrScan = true
        )
    }

    fun onExternalUrlShared(rawText: String) {
        val url = UrlValidator.extractUrlFromQrPayload(rawText) ?: return
        _currentScreen.value = AppScreen.HUB
        _profileDialog.value = ProfileDialogState(
            initialUrl = url,
            initialName = UrlValidator.suggestProfileName(url),
            fromQrScan = false
        )
    }

    fun openAddProfileDialog(prefillUrl: String = "") {
        _profileDialog.value = ProfileDialogState(
            initialUrl = prefillUrl,
            initialName = if (prefillUrl.isNotBlank()) UrlValidator.suggestProfileName(prefillUrl) else ""
        )
    }

    fun openEditProfileDialog(profile: WorkspaceProfile) {
        _profileDialog.value = ProfileDialogState(
            editingProfile = profile,
            initialUrl = profile.url,
            initialName = profile.name
        )
    }

    fun dismissProfileDialog() {
        _profileDialog.value = null
    }

    fun saveProfileAndOptionallyOpen(
        activity: Activity,
        existingId: String?,
        name: String,
        normalizedUrl: String,
        engine: RenderEngine,
        setAsDefault: Boolean,
        openImmediately: Boolean,
        isDarkTheme: Boolean
    ) {
        val profile = WorkspaceProfile(
            id = existingId ?: java.util.UUID.randomUUID().toString(),
            name = name.trim().ifBlank { UrlValidator.suggestProfileName(normalizedUrl) },
            url = normalizedUrl,
            preferredEngine = engine,
            isDefault = setAsDefault
        )
        _profileDialog.value = null
        viewModelScope.launch {
            repository.saveProfile(profile, setAsActivePane1 = true)
            if (openImmediately) {
                openProfile(activity, profile, isDarkTheme, forceEngine = engine)
            } else {
                showMessage("Profiel '${profile.name}' opgeslagen")
            }
        }
    }

    fun deleteProfile(profileId: String) {
        viewModelScope.launch {
            repository.deleteProfile(profileId)
            showMessage("Profiel verwijderd")
        }
    }

    fun setDefaultProfile(profileId: String) {
        viewModelScope.launch {
            repository.setDefaultProfile(profileId)
            showMessage("Standaard workspace bijgewerkt")
        }
    }

    fun openProfile(
        activity: Activity,
        profile: WorkspaceProfile,
        isDarkTheme: Boolean,
        forceEngine: RenderEngine? = null,
        targetSlot: PaneSlot = PaneSlot.PRIMARY,
        preferSideSheet: Boolean? = null
    ) {
        val engine = forceEngine ?: profile.preferredEngine
        when (engine) {
            RenderEngine.SAFE_CUSTOM_TAB -> {
                val useSideSheet = preferSideSheet
                    ?: (foldState.value.isExpandedScreen && settings.value.useSideSheetOnFold)
                if (useSideSheet) {
                    customTabsManager.launchFoldSideSheet(activity, profile.url, isDarkTheme)
                } else {
                    customTabsManager.launchFullScreen(activity, profile.url, isDarkTheme)
                }
            }

            RenderEngine.NATIVE_WEBVIEW -> {
                viewModelScope.launch {
                    if (targetSlot == PaneSlot.PRIMARY) {
                        repository.setActivePane1Profile(profile.id)
                    } else {
                        repository.setActivePane2Profile(profile.id)
                    }
                }
                webSessionHolder.loadUrl(targetSlot, profile.url, profile.id)
                _compactActiveSlot.value = targetSlot
                _currentScreen.value = AppScreen.WEB_WORKSPACE
            }
        }
    }

    fun launchAdjacentSplitScreen(activity: Activity, url: String, isDarkTheme: Boolean) {
        customTabsManager.launchAdjacentSplitScreen(activity, url, isDarkTheme)
    }

    fun openUrlInSafeCustomTab(activity: Activity, url: String, isDarkTheme: Boolean) {
        val useSideSheet = foldState.value.isExpandedScreen && settings.value.useSideSheetOnFold
        if (useSideSheet) {
            customTabsManager.launchFoldSideSheet(activity, url, isDarkTheme)
        } else {
            customTabsManager.launchFullScreen(activity, url, isDarkTheme)
        }
    }

    fun switchCompactSlot(slot: PaneSlot) {
        _compactActiveSlot.value = slot
    }

    fun updateDualPaneSplitRatio(ratio: Float) {
        _dualPaneSplitRatio.value = ratio.coerceIn(0.3f, 0.7f)
    }

    fun toggleRailCollapsed() {
        _railCollapsed.value = !_railCollapsed.value
    }

    fun openSettings() {
        _showSettingsSheet.value = true
    }

    fun closeSettings() {
        _showSettingsSheet.value = false
    }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch {
        repository.setThemeMode(mode)
    }

    fun setDualPaneEnabledOnFold(enabled: Boolean) = viewModelScope.launch {
        repository.setDualPaneEnabledOnFold(enabled)
    }

    fun setUseSideSheetOnFold(enabled: Boolean) = viewModelScope.launch {
        repository.setUseSideSheetOnFold(enabled)
    }

    fun setCoverZoomPercent(percent: Int) = viewModelScope.launch {
        repository.setCoverZoomPercent(percent)
    }

    fun setExpandedZoomPercent(percent: Int) = viewModelScope.launch {
        repository.setExpandedZoomPercent(percent)
    }

    fun setExpandedWideLayout(enabled: Boolean) = viewModelScope.launch {
        repository.setExpandedWideLayout(enabled)
    }

    fun setPullToRefreshEnabled(enabled: Boolean) = viewModelScope.launch {
        repository.setPullToRefreshEnabled(enabled)
    }

    fun setNotificationsEnabled(enabled: Boolean) = viewModelScope.launch {
        repository.setNotificationsEnabled(enabled)
    }

    fun setBackgroundWatcherEnabled(enabled: Boolean) = viewModelScope.launch {
        repository.setBackgroundWatcherEnabled(enabled)
        if (enabled) {
            ActivityMonitorService.start(getApplication())
        } else {
            ActivityMonitorService.stop(getApplication())
        }
    }

    fun setAutoLaunchDefault(enabled: Boolean) = viewModelScope.launch {
        repository.setAutoLaunchDefault(enabled)
    }

    fun sendTestNotification() {
        notificationBridge.postNotification(
            title = "Antigravity testmelding",
            body = "Meldingen werken op dit apparaat!"
        )
        showMessage("Testmelding verzonden")
    }

    fun clearWebViewData() {
        webSessionHolder.clearCacheAndCookies()
        showMessage("WebView-cache en cookies gewist")
    }

    fun showMessage(message: String) {
        _toastMessage.value = message
    }

    fun consumeMessage() {
        _toastMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        customTabsManager.unbind()
        webSessionHolder.destroyAll()
    }
}
