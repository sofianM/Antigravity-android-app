package com.sofianm.antigravity.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "antigravity_settings"
)

class SettingsRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private object Keys {
        val PROFILES_JSON = stringPreferencesKey("profiles_json")
        val ACTIVE_PANE1_ID = stringPreferencesKey("active_pane1_profile_id")
        val ACTIVE_PANE2_ID = stringPreferencesKey("active_pane2_profile_id")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DUAL_PANE_FOLD = booleanPreferencesKey("dual_pane_enabled_on_fold")
        val USE_SIDE_SHEET_FOLD = booleanPreferencesKey("use_side_sheet_on_fold")
        val COVER_ZOOM = intPreferencesKey("cover_zoom_percent")
        val EXPANDED_ZOOM = intPreferencesKey("expanded_zoom_percent")
        val EXPANDED_WIDE_LAYOUT = booleanPreferencesKey("expanded_wide_layout")
        val PULL_TO_REFRESH = booleanPreferencesKey("pull_to_refresh_enabled")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val BACKGROUND_WATCHER = booleanPreferencesKey("background_watcher_enabled")
        val AUTO_LAUNCH_DEFAULT = booleanPreferencesKey("auto_launch_default")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        val profiles = prefs[Keys.PROFILES_JSON]?.let { raw ->
            runCatching { json.decodeFromString<List<WorkspaceProfile>>(raw) }.getOrNull()
        }.orEmpty()

        val themeMode = prefs[Keys.THEME_MODE]?.let { raw ->
            runCatching { ThemeMode.valueOf(raw) }.getOrNull()
        } ?: ThemeMode.SYSTEM

        AppSettings(
            profiles = profiles,
            activePane1ProfileId = prefs[Keys.ACTIVE_PANE1_ID],
            activePane2ProfileId = prefs[Keys.ACTIVE_PANE2_ID],
            themeMode = themeMode,
            dualPaneEnabledOnFold = prefs[Keys.DUAL_PANE_FOLD] ?: true,
            useSideSheetOnFold = prefs[Keys.USE_SIDE_SHEET_FOLD] ?: true,
            coverZoomPercent = (prefs[Keys.COVER_ZOOM] ?: 100).coerceIn(75, 150),
            expandedZoomPercent = (prefs[Keys.EXPANDED_ZOOM] ?: 100).coerceIn(75, 150),
            expandedWideLayout = prefs[Keys.EXPANDED_WIDE_LAYOUT] ?: true,
            pullToRefreshEnabled = prefs[Keys.PULL_TO_REFRESH] ?: true,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            backgroundWatcherEnabled = prefs[Keys.BACKGROUND_WATCHER] ?: false,
            autoLaunchDefault = prefs[Keys.AUTO_LAUNCH_DEFAULT] ?: false
        )
    }

    suspend fun saveProfile(profile: WorkspaceProfile, setAsActivePane1: Boolean = true) {
        context.dataStore.edit { prefs ->
            val current = decodeProfiles(prefs[Keys.PROFILES_JSON])
            val shouldBeDefault = profile.isDefault || current.isEmpty()
            val updatedList = current
                .filterNot { it.id == profile.id }
                .map { if (shouldBeDefault) it.copy(isDefault = false) else it } +
                profile.copy(isDefault = shouldBeDefault)

            prefs[Keys.PROFILES_JSON] = json.encodeToString(updatedList)
            if (setAsActivePane1 || prefs[Keys.ACTIVE_PANE1_ID] == null) {
                prefs[Keys.ACTIVE_PANE1_ID] = profile.id
            }
            if (prefs[Keys.ACTIVE_PANE2_ID] == null && updatedList.size >= 2) {
                prefs[Keys.ACTIVE_PANE2_ID] = updatedList.last().id
            }
        }
    }

    suspend fun deleteProfile(profileId: String) {
        context.dataStore.edit { prefs ->
            val current = decodeProfiles(prefs[Keys.PROFILES_JSON])
            val remaining = current.filterNot { it.id == profileId }
            val normalized = if (remaining.isNotEmpty() && remaining.none { it.isDefault }) {
                remaining.mapIndexed { idx, p -> if (idx == 0) p.copy(isDefault = true) else p }
            } else {
                remaining
            }
            prefs[Keys.PROFILES_JSON] = json.encodeToString(normalized)
            if (prefs[Keys.ACTIVE_PANE1_ID] == profileId) {
                if (normalized.isNotEmpty()) {
                    prefs[Keys.ACTIVE_PANE1_ID] = normalized.first().id
                } else {
                    prefs.remove(Keys.ACTIVE_PANE1_ID)
                }
            }
            if (prefs[Keys.ACTIVE_PANE2_ID] == profileId) {
                val nextPane2 = normalized.getOrNull(1)?.id ?: normalized.firstOrNull()?.id
                if (nextPane2 != null) {
                    prefs[Keys.ACTIVE_PANE2_ID] = nextPane2
                } else {
                    prefs.remove(Keys.ACTIVE_PANE2_ID)
                }
            }
        }
    }

    suspend fun setDefaultProfile(profileId: String) {
        context.dataStore.edit { prefs ->
            val updated = decodeProfiles(prefs[Keys.PROFILES_JSON]).map {
                it.copy(isDefault = it.id == profileId)
            }
            prefs[Keys.PROFILES_JSON] = json.encodeToString(updated)
        }
    }

    suspend fun setActivePane1Profile(profileId: String) {
        context.dataStore.edit { it[Keys.ACTIVE_PANE1_ID] = profileId }
    }

    suspend fun setActivePane2Profile(profileId: String) {
        context.dataStore.edit { it[Keys.ACTIVE_PANE2_ID] = profileId }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setDualPaneEnabledOnFold(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DUAL_PANE_FOLD] = enabled }
    }

    suspend fun setUseSideSheetOnFold(enabled: Boolean) {
        context.dataStore.edit { it[Keys.USE_SIDE_SHEET_FOLD] = enabled }
    }

    suspend fun setCoverZoomPercent(percent: Int) {
        context.dataStore.edit { it[Keys.COVER_ZOOM] = percent.coerceIn(75, 150) }
    }

    suspend fun setExpandedZoomPercent(percent: Int) {
        context.dataStore.edit { it[Keys.EXPANDED_ZOOM] = percent.coerceIn(75, 150) }
    }

    suspend fun setExpandedWideLayout(enabled: Boolean) {
        context.dataStore.edit { it[Keys.EXPANDED_WIDE_LAYOUT] = enabled }
    }

    suspend fun setPullToRefreshEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.PULL_TO_REFRESH] = enabled }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setBackgroundWatcherEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BACKGROUND_WATCHER] = enabled }
    }

    suspend fun setAutoLaunchDefault(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_LAUNCH_DEFAULT] = enabled }
    }

    private fun decodeProfiles(raw: String?): List<WorkspaceProfile> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<WorkspaceProfile>>(raw) }.getOrDefault(emptyList())
    }
}
