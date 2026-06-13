package com.yueyin.data.repository

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

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "yueyin_settings")

class SettingsRepository(private val context: Context) {
    companion object {
        val TING_SERVER_URL = stringPreferencesKey("ting_server_url")
        val TING_USERNAME = stringPreferencesKey("ting_username")
        val TING_PASSWORD = stringPreferencesKey("ting_password")
        val TING_TOKEN = stringPreferencesKey("ting_token")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val CACHE_SIZE_GB = intPreferencesKey("cache_size_gb")
    }

    val tingServerUrl: Flow<String> = context.dataStore.data.map { it[TING_SERVER_URL] ?: "" }
    val tingUsername: Flow<String> = context.dataStore.data.map { it[TING_USERNAME] ?: "" }
    val tingPassword: Flow<String> = context.dataStore.data.map { it[TING_PASSWORD] ?: "" }
    val tingToken: Flow<String> = context.dataStore.data.map { it[TING_TOKEN] ?: "" }
    val darkMode: Flow<Boolean> = context.dataStore.data.map { it[DARK_MODE] ?: false }
    val cacheSizeGB: Flow<Int> = context.dataStore.data.map { it[CACHE_SIZE_GB] ?: 2 }

    suspend fun saveTingLogin(serverUrl: String, username: String, password: String) {
        context.dataStore.edit { prefs ->
            prefs[TING_SERVER_URL] = serverUrl
            prefs[TING_USERNAME] = username
            prefs[TING_PASSWORD] = password
        }
    }

    suspend fun saveTingToken(token: String) {
        context.dataStore.edit { it[TING_TOKEN] = token }
    }

    suspend fun clearTingLogin() {
        context.dataStore.edit { prefs ->
            prefs.remove(TING_SERVER_URL)
            prefs.remove(TING_USERNAME)
            prefs.remove(TING_PASSWORD)
            prefs.remove(TING_TOKEN)
        }
    }

    suspend fun saveDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[DARK_MODE] = enabled }
    }

    suspend fun saveCacheSize(gb: Int) {
        context.dataStore.edit { it[CACHE_SIZE_GB] = gb }
    }
}
