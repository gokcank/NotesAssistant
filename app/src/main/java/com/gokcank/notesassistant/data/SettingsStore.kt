package com.gokcank.notesassistant.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {

    private val themeKey = stringPreferencesKey("theme_mode")
    private val onboardingKey = booleanPreferencesKey("onboarding_done")
    private val gridViewKey = booleanPreferencesKey("grid_view")

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        runCatching { ThemeMode.valueOf(prefs[themeKey].orEmpty()) }.getOrDefault(ThemeMode.SYSTEM)
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[themeKey] = mode.name }
    }

    val onboardingDone: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[onboardingKey] ?: false
    }

    suspend fun setOnboardingDone() {
        context.dataStore.edit { it[onboardingKey] = true }
    }

    val gridView: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[gridViewKey] ?: false
    }

    suspend fun setGridView(value: Boolean) {
        context.dataStore.edit { it[gridViewKey] = value }
    }
}
