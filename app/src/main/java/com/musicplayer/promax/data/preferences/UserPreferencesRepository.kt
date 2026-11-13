package com.musicplayer.promax.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.musicplayer.promax.domain.model.AppLanguage
import com.musicplayer.promax.domain.model.AppAccent
import com.musicplayer.promax.domain.model.AppTheme
import com.musicplayer.promax.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userPreferencesDataStore by preferencesDataStore(name = "user_preferences")

class DataStoreUserPreferencesRepository(private val context: Context) : UserPreferencesRepository {
    private object Keys {
        val LANGUAGE = stringPreferencesKey("app_language")
        val THEME = stringPreferencesKey("app_theme")
        val ACCENT = stringPreferencesKey("app_accent")
        val SLEEP_TIMER_END = longPreferencesKey("sleep_timer_end_millis")
        val INTERNAL_MUSIC_ENABLED = booleanPreferencesKey("internal_music_enabled")
        val EXTERNAL_MUSIC_ENABLED = booleanPreferencesKey("external_music_enabled")
        val EXTERNAL_TREE_URI = stringPreferencesKey("external_tree_uri")
    }

    override val language: Flow<AppLanguage> = context.userPreferencesDataStore.data.map { AppLanguage.fromStoredValue(it[Keys.LANGUAGE]) }
    override val theme: Flow<AppTheme> = context.userPreferencesDataStore.data.map { AppTheme.fromStoredValue(it[Keys.THEME]) }
    override val accent: Flow<AppAccent> = context.userPreferencesDataStore.data.map { AppAccent.fromStoredValue(it[Keys.ACCENT]) }
    override val sleepTimerEndMillis: Flow<Long?> = context.userPreferencesDataStore.data.map { it[Keys.SLEEP_TIMER_END] }
    override val internalMusicEnabled: Flow<Boolean> = context.userPreferencesDataStore.data.map { it[Keys.INTERNAL_MUSIC_ENABLED] ?: true }
    override val externalMusicEnabled: Flow<Boolean> = context.userPreferencesDataStore.data.map { it[Keys.EXTERNAL_MUSIC_ENABLED] ?: true }
    override val externalTreeUri: Flow<String?> = context.userPreferencesDataStore.data.map { it[Keys.EXTERNAL_TREE_URI] }

    override suspend fun setLanguage(language: AppLanguage) {
        context.userPreferencesDataStore.edit { it[Keys.LANGUAGE] = language.name }
    }

    override suspend fun setTheme(theme: AppTheme) {
        context.userPreferencesDataStore.edit { it[Keys.THEME] = theme.name }
    }

    override suspend fun setAccent(accent: AppAccent) {
        context.userPreferencesDataStore.edit { it[Keys.ACCENT] = accent.name }
    }

    override suspend fun setSleepTimerEndMillis(endMillis: Long?) {
        context.userPreferencesDataStore.edit { prefs ->
            if (endMillis == null) prefs.remove(Keys.SLEEP_TIMER_END) else prefs[Keys.SLEEP_TIMER_END] = endMillis
        }
    }

    override suspend fun setInternalMusicEnabled(enabled: Boolean) {
        context.userPreferencesDataStore.edit { it[Keys.INTERNAL_MUSIC_ENABLED] = enabled }
    }

    override suspend fun setExternalMusicEnabled(enabled: Boolean) {
        context.userPreferencesDataStore.edit { it[Keys.EXTERNAL_MUSIC_ENABLED] = enabled }
    }

    override suspend fun setExternalTreeUri(uri: String?) {
        context.userPreferencesDataStore.edit {
            if (uri == null) it.remove(Keys.EXTERNAL_TREE_URI) else it[Keys.EXTERNAL_TREE_URI] = uri
        }
    }
}
