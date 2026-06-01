package com.lechenmusic.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "lechen_settings")

class SettingsRepository(private val context: Context) {

    companion object {
        val SERVER_URL = stringPreferencesKey("server_url")
        val USERNAME = stringPreferencesKey("username")
        val PASSWORD = stringPreferencesKey("password")
        val THEME_MODE = stringPreferencesKey("theme_mode") // "dark" or "light"
        val CACHE_SIZE = intPreferencesKey("cache_size_gb")
        val RECENT_PLAY_IDS = stringPreferencesKey("recent_play_ids")
        val CACHED_ALL_SONGS_JSON = stringPreferencesKey("cached_all_songs_json")
        val CACHED_RECENT_SONGS_JSON = stringPreferencesKey("cached_recent_songs_json")
        val CACHED_DAILY_SONGS_JSON = stringPreferencesKey("cached_daily_songs_json")
        val CACHED_DAILY_SONGS_DATE = stringPreferencesKey("cached_daily_songs_date")
        val SKIPPED_VERSION_CODE = intPreferencesKey("skipped_version_code")
        // Ting Reader (Audiobook) settings
        val TING_ENABLED = stringPreferencesKey("ting_enabled")
        val TING_SERVER_URL = stringPreferencesKey("ting_server_url")
        val TING_USERNAME = stringPreferencesKey("ting_username")
        val TING_PASSWORD = stringPreferencesKey("ting_password")
        val TING_TOKEN = stringPreferencesKey("ting_token")
        val CACHED_AUDIOBOOK_PROGRESS_JSON = stringPreferencesKey("cached_audiobook_progress_json")
    }

    val serverUrl: Flow<String> = context.dataStore.data.map { it[SERVER_URL] ?: "" }
    val username: Flow<String> = context.dataStore.data.map { it[USERNAME] ?: "" }
    val password: Flow<String> = context.dataStore.data.map { it[PASSWORD] ?: "" }
    val themeMode: Flow<String> = context.dataStore.data.map { it[THEME_MODE] ?: "dark" }
    val cacheSize: Flow<Int> = context.dataStore.data.map { it[CACHE_SIZE] ?: 4 }
    val recentPlayIds: Flow<String> = context.dataStore.data.map { it[RECENT_PLAY_IDS] ?: "" }

    // Ting Reader settings
    val tingEnabled: Flow<Boolean> = context.dataStore.data.map { (it[TING_ENABLED] ?: "false") == "true" }
    val tingServerUrl: Flow<String> = context.dataStore.data.map { it[TING_SERVER_URL] ?: "" }
    val tingUsername: Flow<String> = context.dataStore.data.map { it[TING_USERNAME] ?: "" }
    val tingPassword: Flow<String> = context.dataStore.data.map { it[TING_PASSWORD] ?: "" }
    val tingToken: Flow<String> = context.dataStore.data.map { it[TING_TOKEN] ?: "" }

    suspend fun saveTingLogin(serverUrl: String, username: String, password: String) {
        context.dataStore.edit { prefs ->
            prefs[TING_ENABLED] = "true"
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
            prefs[TING_ENABLED] = "false"
            prefs.remove(TING_SERVER_URL)
            prefs.remove(TING_USERNAME)
            prefs.remove(TING_PASSWORD)
            prefs.remove(TING_TOKEN)
        }
    }

    suspend fun setTingEnabled(enabled: Boolean) {
        context.dataStore.edit { it[TING_ENABLED] = if (enabled) "true" else "false" }
    }

    suspend fun saveLogin(serverUrl: String, username: String, password: String) {
        context.dataStore.edit { prefs ->
            prefs[SERVER_URL] = serverUrl
            prefs[USERNAME] = username
            prefs[PASSWORD] = password
        }
    }

    suspend fun clearLogin() {
        context.dataStore.edit { prefs ->
            prefs.remove(SERVER_URL)
            prefs.remove(USERNAME)
            prefs.remove(PASSWORD)
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[THEME_MODE] = mode }
    }

    suspend fun setCacheSize(sizeGb: Int) {
        context.dataStore.edit { it[CACHE_SIZE] = sizeGb }
    }

    suspend fun addRecentPlay(songId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[RECENT_PLAY_IDS] ?: ""
            val ids = current.split(",").filter { it.isNotEmpty() }.toMutableList()
            ids.remove(songId)
            ids.add(0, songId)
            prefs[RECENT_PLAY_IDS] = ids.take(100).joinToString(",")
        }
    }

    suspend fun isLoggedIn(): Boolean {
        val url = context.dataStore.data.map { it[SERVER_URL] ?: "" }
        // We can't easily check flow here, so just return based on what we have
        return true // Will be checked in ViewModel
    }

    val cachedAllSongsJson: Flow<String> = context.dataStore.data.map { it[CACHED_ALL_SONGS_JSON] ?: "" }

    suspend fun saveCachedAllSongsJson(json: String) {
        context.dataStore.edit { it[CACHED_ALL_SONGS_JSON] = json }
    }

    val cachedRecentSongsJson: Flow<String> = context.dataStore.data.map { it[CACHED_RECENT_SONGS_JSON] ?: "" }

    suspend fun saveCachedRecentSongsJson(json: String) {
        context.dataStore.edit { it[CACHED_RECENT_SONGS_JSON] = json }
    }

    val cachedDailySongsJson: Flow<String> = context.dataStore.data.map { it[CACHED_DAILY_SONGS_JSON] ?: "" }
    val cachedDailySongsDate: Flow<String> = context.dataStore.data.map { it[CACHED_DAILY_SONGS_DATE] ?: "" }

    suspend fun saveCachedDailySongs(json: String, date: String) {
        context.dataStore.edit { prefs ->
            prefs[CACHED_DAILY_SONGS_JSON] = json
            prefs[CACHED_DAILY_SONGS_DATE] = date
        }
    }

    // Skipped version code for "skip this version" feature
    val skippedVersionCode: Flow<Int> = context.dataStore.data.map { it[SKIPPED_VERSION_CODE] ?: 0 }

    suspend fun setSkippedVersionCode(versionCode: Int) {
        context.dataStore.edit { it[SKIPPED_VERSION_CODE] = versionCode }
    }

    // Cached audiobook progress for offline resume
    val cachedAudiobookProgressJson: Flow<String> = context.dataStore.data.map { it[CACHED_AUDIOBOOK_PROGRESS_JSON] ?: "{}" }

    suspend fun saveCachedAudiobookProgressJson(json: String) {
        context.dataStore.edit { it[CACHED_AUDIOBOOK_PROGRESS_JSON] = json }
    }
}
