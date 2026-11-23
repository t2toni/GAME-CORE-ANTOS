package com.antos.gaming.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import androidx.datastore.preferences.core.stringPreferencesKey

enum class PerformanceMode(val label: String, val shortDesc: String) {
    BATTERY_SAVER("BATTERY SAVER", "Downclock CPU & save battery"),
    BALANCED("BALANCED", "Adaptive performance & thermals"),
    MONSTER("MONSTER MODE", "Max CPU/GPU boost & ultra response")
}

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {

    companion object {
        val START_ON_BOOT = booleanPreferencesKey("start_on_boot")
        val AUTO_GAMING_MODE = booleanPreferencesKey("auto_gaming_mode")
        val ENABLE_SIDEBAR = booleanPreferencesKey("enable_sidebar")
        val AUTO_HIDE = booleanPreferencesKey("auto_hide")
        val SHOW_FPS_COUNTER = booleanPreferencesKey("show_fps_counter")
        val SHOW_NORMAL_FPS = booleanPreferencesKey("show_normal_fps")
        val SHOW_ACCURATE_FPS = booleanPreferencesKey("show_accurate_fps")
        val PERFORMANCE_MODE = stringPreferencesKey("performance_mode")
    }

    val startOnBootFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[START_ON_BOOT] ?: false
        }

    val autoGamingModeFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[AUTO_GAMING_MODE] ?: false
        }

    val enableSidebarFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[ENABLE_SIDEBAR] ?: true
        }

    val autoHideFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[AUTO_HIDE] ?: false
        }

    val showFpsCounterFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[SHOW_FPS_COUNTER] ?: false
        }

    val showNormalFpsFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[SHOW_NORMAL_FPS] ?: true
        }

    val showAccurateFpsFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[SHOW_ACCURATE_FPS] ?: false
        }

    val performanceModeFlow: Flow<PerformanceMode> = context.dataStore.data
        .map { preferences ->
            val raw = preferences[PERFORMANCE_MODE] ?: PerformanceMode.BALANCED.name
            try {
                PerformanceMode.valueOf(raw)
            } catch (e: Exception) {
                PerformanceMode.BALANCED
            }
        }

    suspend fun setPerformanceMode(mode: PerformanceMode) {
        context.dataStore.edit { preferences ->
            preferences[PERFORMANCE_MODE] = mode.name
        }
    }

    suspend fun saveSetting(key: Preferences.Key<Boolean>, value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[key] = value
        }
    }
}
