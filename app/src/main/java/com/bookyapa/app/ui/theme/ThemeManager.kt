package com.bookyapa.app.ui.theme

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

enum class ThemeMode { DARK, LIGHT, SEPIA }

val Context.dataStore by preferencesDataStore(name = "settings")

object ThemeManager {
    private val THEME_KEY = stringPreferencesKey("theme_mode")

    fun getThemeMode(context: Context): Flow<ThemeMode> =
        context.dataStore.data.map { prefs ->
            val name = prefs[THEME_KEY] ?: "DARK"
            try { ThemeMode.valueOf(name) } catch (_: Exception) { ThemeMode.DARK }
        }

    suspend fun setThemeMode(context: Context, mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[THEME_KEY] = mode.name
        }
    }

    fun nextMode(current: ThemeMode): ThemeMode = when (current) {
        ThemeMode.DARK -> ThemeMode.LIGHT
        ThemeMode.LIGHT -> ThemeMode.SEPIA
        ThemeMode.SEPIA -> ThemeMode.DARK
    }

    private val FONT_SIZE_KEY = intPreferencesKey("reader_font_size")

    fun getFontSize(context: Context): Flow<Int> =
        context.dataStore.data.map { prefs ->
            prefs[FONT_SIZE_KEY] ?: 16
        }

    suspend fun setFontSize(context: Context, size: Int) {
        context.dataStore.edit { prefs ->
            prefs[FONT_SIZE_KEY] = size
        }
    }
}

object RepoUrlManager {
    private val REPO_URLS_KEY = stringPreferencesKey("source_repo_urls")
    private val json = Json { ignoreUnknownKeys = true }

    private var cachedContext: Context? = null

    fun init(context: Context) {
        cachedContext = context.applicationContext
    }

    fun getUrlsFlow(): Flow<List<String>> {
        val ctx = cachedContext ?: return flowOf(emptyList())
        return ctx.dataStore.data.map { prefs ->
            val raw = prefs[REPO_URLS_KEY] ?: "[]"
            try {
                json.decodeFromString<List<String>>(raw)
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    suspend fun getUrls(): List<String> {
        return getUrlsFlow().first()
    }

    suspend fun addUrl(url: String) {
        val ctx = cachedContext ?: return
        val current = getUrls().toMutableList()
        if (url !in current) {
            current.add(url)
            ctx.dataStore.edit { prefs ->
                prefs[REPO_URLS_KEY] = json.encodeToString(current)
            }
        }
    }

    suspend fun removeUrl(url: String) {
        val ctx = cachedContext ?: return
        val current = getUrls().toMutableList()
        current.remove(url)
        ctx.dataStore.edit { prefs ->
            prefs[REPO_URLS_KEY] = json.encodeToString(current)
        }
    }
}
