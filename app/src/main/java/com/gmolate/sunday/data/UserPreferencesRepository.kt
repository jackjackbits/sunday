package com.gmolate.sunday.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val THEME = stringPreferencesKey("theme")
    }

    val theme: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.THEME] ?: "system"
        }

    suspend fun saveTheme(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME] = theme
        }
    }
}
