package com.antos.gaming.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "antos_settings")

class PreferencesRepository(private val context: Context) {

    fun getGameProfile(packageName: String): Flow<GameProfile> {
        return context.dataStore.data.map { preferences ->
            GameProfile(
                packageName = packageName,
                performanceMode = preferences[booleanPreferencesKey("${packageName}_perf")] ?: true,
                brightness = preferences[floatPreferencesKey("${packageName}_bright")] ?: -1f,
                isLandscapeLocked = preferences[booleanPreferencesKey("${packageName}_land")] ?: false,
                blockNotifications = preferences[booleanPreferencesKey("${packageName}_notif")] ?: true,
                showFps = preferences[booleanPreferencesKey("${packageName}_fps")] ?: true,
                touchProtection = preferences[booleanPreferencesKey("${packageName}_touch")] ?: false
            )
        }
    }

    suspend fun saveGameProfile(profile: GameProfile) {
        context.dataStore.edit { preferences ->
            preferences[booleanPreferencesKey("${profile.packageName}_perf")] = profile.performanceMode
            preferences[floatPreferencesKey("${profile.packageName}_bright")] = profile.brightness
            preferences[booleanPreferencesKey("${profile.packageName}_land")] = profile.isLandscapeLocked
            preferences[booleanPreferencesKey("${profile.packageName}_notif")] = profile.blockNotifications
            preferences[booleanPreferencesKey("${profile.packageName}_fps")] = profile.showFps
            preferences[booleanPreferencesKey("${profile.packageName}_touch")] = profile.touchProtection
        }
    }

    val manualGamesFlow: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[stringSetPreferencesKey("manual_games")] ?: emptySet()
    }

    suspend fun addManualGame(packageName: String) {
        context.dataStore.edit { preferences ->
            val key = stringSetPreferencesKey("manual_games")
            val current = preferences[key] ?: emptySet()
            preferences[key] = current + packageName
        }
    }

    suspend fun removeManualGame(packageName: String) {
        context.dataStore.edit { preferences ->
            val key = stringSetPreferencesKey("manual_games")
            val current = preferences[key] ?: emptySet()
            preferences[key] = current - packageName
        }
    }

    companion object {
        val DEFAULT_POPUP_APPS = listOf(
            "com.discord",
            "com.brave.browser",
            "com.instagram.android"
        )
    }

    val popupAppsFlow: Flow<List<String>> = context.dataStore.data.map { preferences ->
        val raw = preferences[stringPreferencesKey("popup_apps_ordered")]
        if (raw != null) {
            if (raw.isBlank()) emptyList() else raw.split(",").filter { it.isNotBlank() }
        } else {
            DEFAULT_POPUP_APPS
        }
    }

    suspend fun addPopupApp(packageName: String) {
        context.dataStore.edit { preferences ->
            val key = stringPreferencesKey("popup_apps_ordered")
            val raw = preferences[key]
            val currentList = if (raw != null) {
                if (raw.isBlank()) emptyList() else raw.split(",").filter { it.isNotBlank() }
            } else {
                DEFAULT_POPUP_APPS
            }
            if (!currentList.contains(packageName)) {
                preferences[key] = (currentList + packageName).joinToString(",")
            }
        }
    }

    suspend fun removePopupApp(packageName: String) {
        context.dataStore.edit { preferences ->
            val key = stringPreferencesKey("popup_apps_ordered")
            val raw = preferences[key]
            val currentList = if (raw != null) {
                if (raw.isBlank()) emptyList() else raw.split(",").filter { it.isNotBlank() }
            } else {
                DEFAULT_POPUP_APPS
            }
            preferences[key] = currentList.filter { it != packageName }.joinToString(",")
        }
    }
}

