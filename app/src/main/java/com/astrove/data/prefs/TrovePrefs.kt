package com.astrove.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("trove_prefs")

/** How the app picks light vs dark. SYSTEM follows the device setting. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

// A tiny synchronous mirror of the theme choice. DataStore is async, but the
// launch/splash window background and the very first frame need the value before
// any coroutine can run, so we also keep it in plain SharedPreferences.
private const val THEME_SYNC_FILE = "trove_theme"
private const val THEME_SYNC_KEY = "mode"

fun readThemeModeSync(context: Context): ThemeMode {
    val sp = context.getSharedPreferences(THEME_SYNC_FILE, Context.MODE_PRIVATE)
    return runCatching { ThemeMode.valueOf(sp.getString(THEME_SYNC_KEY, "") ?: "") }
        .getOrDefault(ThemeMode.SYSTEM)
}

private fun writeThemeModeSync(context: Context, mode: ThemeMode) {
    context.getSharedPreferences(THEME_SYNC_FILE, Context.MODE_PRIVATE)
        .edit().putString(THEME_SYNC_KEY, mode.name).apply()
}

/** Small key-value state: the scan watermark and recent searches. */
class TrovePrefs(context: Context) {

    private val appCtx = context.applicationContext
    private val ds = appCtx.dataStore

    val watermark: Flow<Long> = ds.data.map { it[WATERMARK] ?: 0L }

    suspend fun setWatermark(seconds: Long) {
        ds.edit { it[WATERMARK] = seconds }
    }

    val recentSearches: Flow<List<String>> = ds.data.map { prefs ->
        prefs[RECENT]?.split('\n')?.filter { it.isNotBlank() } ?: emptyList()
    }

    val chargingOnly: Flow<Boolean> = ds.data.map { it[CHARGING_ONLY] ?: false }

    suspend fun setChargingOnly(enabled: Boolean) {
        ds.edit { it[CHARGING_ONLY] = enabled }
    }

    val ocrPipelineVersion: Flow<Int> = ds.data.map { it[OCR_VERSION] ?: 0 }

    suspend fun setOcrPipelineVersion(version: Int) {
        ds.edit { it[OCR_VERSION] = version }
    }

    val themeMode: Flow<ThemeMode> = ds.data.map { prefs ->
        runCatching { ThemeMode.valueOf(prefs[THEME_MODE] ?: "") }.getOrDefault(ThemeMode.SYSTEM)
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        writeThemeModeSync(appCtx, mode)
        ds.edit { it[THEME_MODE] = mode.name }
    }

    suspend fun addRecentSearch(query: String) {
        val q = query.trim()
        if (q.isEmpty()) return
        ds.edit { prefs ->
            val current = prefs[RECENT]?.split('\n')?.filter { it.isNotBlank() } ?: emptyList()
            val updated = (listOf(q) + current.filterNot { it.equals(q, ignoreCase = true) })
                .take(MAX_RECENT)
            prefs[RECENT] = updated.joinToString("\n")
        }
    }

    private companion object {
        val WATERMARK = longPreferencesKey("scan_watermark")
        val RECENT = stringPreferencesKey("recent_searches")
        val CHARGING_ONLY = booleanPreferencesKey("charging_only")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val OCR_VERSION = intPreferencesKey("ocr_pipeline_version")
        const val MAX_RECENT = 8
    }
}
