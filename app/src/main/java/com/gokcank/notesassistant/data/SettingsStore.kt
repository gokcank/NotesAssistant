package com.gokcank.notesassistant.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {

    private val themeKey = stringPreferencesKey("theme_mode")
    private val gridViewKey = booleanPreferencesKey("grid_view")

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        runCatching { ThemeMode.valueOf(prefs[themeKey].orEmpty()) }.getOrDefault(ThemeMode.SYSTEM)
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[themeKey] = mode.name }
    }

    val gridView: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[gridViewKey] ?: false
    }

    suspend fun setGridView(value: Boolean) {
        context.dataStore.edit { it[gridViewKey] = value }
    }

    // --- Google Drive eşitleme ---

    private val driveSyncKey = booleanPreferencesKey("drive_sync_enabled")
    private val lastSyncKey = longPreferencesKey("drive_last_sync")

    val driveSyncEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[driveSyncKey] ?: false
    }

    suspend fun setDriveSyncEnabled(value: Boolean) {
        context.dataStore.edit { it[driveSyncKey] = value }
    }

    val lastSyncAt: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[lastSyncKey] ?: 0L
    }

    suspend fun setLastSyncAt(value: Long) {
        context.dataStore.edit { it[lastSyncKey] = value }
    }
}
