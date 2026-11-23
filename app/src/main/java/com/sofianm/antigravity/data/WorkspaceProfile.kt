package com.sofianm.antigravity.data

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class RenderEngine {
    /** Chrome Custom Tabs / Side Sheet — shares Chrome session so Google Sign-In & Passkeys always work. */
    SAFE_CUSTOM_TAB,

    /** Embedded Android WebView — borderless full-screen, in-app Dual-Pane, custom zoom & JS notification bridge. */
    NATIVE_WEBVIEW
}

@Serializable
enum class ThemeMode {
    SYSTEM,
    DARK,
    LIGHT
}

@Serializable
data class WorkspaceProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val url: String,
    val preferredEngine: RenderEngine = RenderEngine.SAFE_CUSTOM_TAB,
    val isDefault: Boolean = false,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

data class AppSettings(
    val profiles: List<WorkspaceProfile> = emptyList(),
    val activePane1ProfileId: String? = null,
    val activePane2ProfileId: String? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dualPaneEnabledOnFold: Boolean = true,
    val useSideSheetOnFold: Boolean = true,
    val coverZoomPercent: Int = 100,
    val expandedZoomPercent: Int = 100,
    val expandedWideLayout: Boolean = true,
    val pullToRefreshEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val backgroundWatcherEnabled: Boolean = false,
    val autoLaunchDefault: Boolean = false
) {
    val defaultProfile: WorkspaceProfile?
        get() = profiles.firstOrNull { it.isDefault } ?: profiles.firstOrNull()

    val activePane1Profile: WorkspaceProfile?
        get() = profiles.firstOrNull { it.id == activePane1ProfileId } ?: defaultProfile

    val activePane2Profile: WorkspaceProfile?
        get() = profiles.firstOrNull { it.id == activePane2ProfileId }
            ?: profiles.firstOrNull { it.id != activePane1Profile?.id }
            ?: activePane1Profile
}
