package com.gmolate.sunday

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// Extensión de propiedad para facilitar el acceso a DataStore
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sunday_app_prefs")

class PreferencesManager(private val context: Context) {

    private object PreferencesKeys {
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val NOTIFICATION_HOUR = intPreferencesKey("notification_hour")
        val NOTIFICATION_MINUTE = intPreferencesKey("notification_minute")
        val DARK_MODE_PREFERENCE = intPreferencesKey("dark_mode_preference") // -1: system, 0: light, 1: dark
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit {\ preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun areNotificationsEnabled(): Boolean {
        return context.dataStore.data.map { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true // Por defecto, activadas
        }.first()
    }

    suspend fun setNotificationTime(hour: Int, minute: Int) {
        context.dataStore.edit {\ preferences ->
            preferences[PreferencesKeys.NOTIFICATION_HOUR] = hour
            preferences[PreferencesKeys.NOTIFICATION_MINUTE] = minute
        }
    }

    suspend fun getNotificationHour(): Int {
        return context.dataStore.data.map { preferences ->
            preferences[PreferencesKeys.NOTIFICATION_HOUR] ?: 9 // Por defecto, 9 AM
        }.first()
    }

    suspend fun getNotificationMinute(): Int {
        return context.dataStore.data.map { preferences ->
            preferences[PreferencesKeys.NOTIFICATION_MINUTE] ?: 0 // Por defecto, 0 minutos
        }.first()
    }

    // isDark: true = dark, false = light, null = system
    suspend fun setDarkModePreference(isDark: Boolean?) {
        context.dataStore.edit {\ preferences ->
            preferences[PreferencesKeys.DARK_MODE_PREFERENCE] = when (isDark) {
                true -> 1
                false -> 0
                null -> -1
            }
        }
    }

    // Retorna true = dark, false = light, null = system
    suspend fun getDarkModePreference(): Boolean? {
        return context.dataStore.data.map { preferences ->
            when (preferences[PreferencesKeys.DARK_MODE_PREFERENCE] ?: -1) {
                1 -> true
                0 -> false
                else -> null
            }
        }.first()
    }
}
