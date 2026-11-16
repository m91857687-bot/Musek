package com.musicplayer.promax.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.musicplayer.promax.domain.model.AppAccent
import com.musicplayer.promax.domain.model.AppLanguage
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
        val DYNAMIC_ARTWORK_THEME = booleanPreferencesKey("dynamic_artwork_theme_enabled")
        val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
        val CROSSFADE_SECONDS = intPreferencesKey("crossfade_seconds")
        val AUDIO_EFFECTS_ENABLED = booleanPreferencesKey("audio_effects_enabled")
        val EQ_PRESET_NAME = stringPreferencesKey("eq_preset_name")
        val BASS_BOOST_STRENGTH = intPreferencesKey("bass_boost_strength")
        val VIRTUALIZER_STRENGTH = intPreferencesKey("virtualizer_strength")
        val LOUDNESS_GAIN_MB = intPreferencesKey("loudness_gain_mb")
        val SLEEP_TIMER_END = longPreferencesKey("sleep_timer_end_millis")
        val INTERNAL_MUSIC_ENABLED = booleanPreferencesKey("internal_music_enabled")
        val EXTERNAL_MUSIC_ENABLED = booleanPreferencesKey("external_music_enabled")
        val EXTERNAL_TREE_URI = stringPreferencesKey("external_tree_uri")
        val EQ_BAND_LEVELS_MB = stringPreferencesKey("eq_band_levels_mb")
        val CUSTOM_EQ_PROFILES_JSON = stringPreferencesKey("custom_eq_profiles_json")
        val STEREO_BALANCE = floatPreferencesKey("stereo_balance")
        val SKIP_SILENCE_ENABLED = booleanPreferencesKey("skip_silence_enabled")
        val MIN_TRACK_DURATION_SECONDS = intPreferencesKey("min_track_duration_seconds")
        val SHAKE_TO_SKIP_ENABLED = booleanPreferencesKey("shake_to_skip_enabled")
    }

    override val language: Flow<AppLanguage> = context.userPreferencesDataStore.data.map { AppLanguage.fromStoredValue(it[Keys.LANGUAGE]) }
    override val theme: Flow<AppTheme> = context.userPreferencesDataStore.data.map { AppTheme.fromStoredValue(it[Keys.THEME]) }
    override val accent: Flow<AppAccent> = context.userPreferencesDataStore.data.map { AppAccent.fromStoredValue(it[Keys.ACCENT]) }
    override val dynamicArtworkThemeEnabled: Flow<Boolean> = context.userPreferencesDataStore.data.map { it[Keys.DYNAMIC_ARTWORK_THEME] ?: true }
    override val playbackSpeed: Flow<Float> = context.userPreferencesDataStore.data.map { (it[Keys.PLAYBACK_SPEED] ?: 1.0f).coerceIn(0.5f, 2.0f) }
    override val crossfadeSeconds: Flow<Int> = context.userPreferencesDataStore.data.map { (it[Keys.CROSSFADE_SECONDS] ?: 2).coerceIn(0, 10) }
    override val audioEffectsEnabled: Flow<Boolean> = context.userPreferencesDataStore.data.map { it[Keys.AUDIO_EFFECTS_ENABLED] ?: true }
    override val eqPresetName: Flow<String?> = context.userPreferencesDataStore.data.map { it[Keys.EQ_PRESET_NAME] }
    override val bassBoostStrength: Flow<Int> = context.userPreferencesDataStore.data.map { (it[Keys.BASS_BOOST_STRENGTH] ?: 0).coerceIn(0, 1000) }
    override val virtualizerStrength: Flow<Int> = context.userPreferencesDataStore.data.map { (it[Keys.VIRTUALIZER_STRENGTH] ?: 0).coerceIn(0, 1000) }
    override val loudnessGainMb: Flow<Int> = context.userPreferencesDataStore.data.map { (it[Keys.LOUDNESS_GAIN_MB] ?: 0).coerceIn(0, 1500) }
    override val sleepTimerEndMillis: Flow<Long?> = context.userPreferencesDataStore.data.map { it[Keys.SLEEP_TIMER_END] }
    override val internalMusicEnabled: Flow<Boolean> = context.userPreferencesDataStore.data.map { it[Keys.INTERNAL_MUSIC_ENABLED] ?: true }
    override val externalMusicEnabled: Flow<Boolean> = context.userPreferencesDataStore.data.map { it[Keys.EXTERNAL_MUSIC_ENABLED] ?: true }
    override val externalTreeUri: Flow<String?> = context.userPreferencesDataStore.data.map { it[Keys.EXTERNAL_TREE_URI] }
    override val eqBandLevelsMb: Flow<String?> = context.userPreferencesDataStore.data.map { it[Keys.EQ_BAND_LEVELS_MB] }
    override val customEqProfilesJson: Flow<String?> = context.userPreferencesDataStore.data.map { it[Keys.CUSTOM_EQ_PROFILES_JSON] }
    override val stereoBalance: Flow<Float> = context.userPreferencesDataStore.data.map { (it[Keys.STEREO_BALANCE] ?: 0f).coerceIn(-1f, 1f) }
    override val skipSilenceEnabled: Flow<Boolean> = context.userPreferencesDataStore.data.map { it[Keys.SKIP_SILENCE_ENABLED] ?: false }
    override val minTrackDurationSeconds: Flow<Int> = context.userPreferencesDataStore.data.map { (it[Keys.MIN_TRACK_DURATION_SECONDS] ?: 0).coerceIn(0, 600) }
    override val shakeToSkipEnabled: Flow<Boolean> = context.userPreferencesDataStore.data.map { it[Keys.SHAKE_TO_SKIP_ENABLED] ?: false }

    override suspend fun setLanguage(language: AppLanguage) {
        context.userPreferencesDataStore.edit { it[Keys.LANGUAGE] = language.name }
    }

    override suspend fun setTheme(theme: AppTheme) {
        context.userPreferencesDataStore.edit { it[Keys.THEME] = theme.name }
    }

    override suspend fun setAccent(accent: AppAccent) {
        context.userPreferencesDataStore.edit { it[Keys.ACCENT] = accent.name }
    }

    override suspend fun setDynamicArtworkThemeEnabled(enabled: Boolean) {
        context.userPreferencesDataStore.edit { it[Keys.DYNAMIC_ARTWORK_THEME] = enabled }
    }

    override suspend fun setPlaybackSpeed(speed: Float) {
        context.userPreferencesDataStore.edit { it[Keys.PLAYBACK_SPEED] = speed.coerceIn(0.5f, 2.0f) }
    }

    override suspend fun setCrossfadeSeconds(seconds: Int) {
        context.userPreferencesDataStore.edit { it[Keys.CROSSFADE_SECONDS] = seconds.coerceIn(0, 10) }
    }

    override suspend fun setAudioEffectsEnabled(enabled: Boolean) {
        context.userPreferencesDataStore.edit { it[Keys.AUDIO_EFFECTS_ENABLED] = enabled }
    }

    override suspend fun setEqPresetName(name: String?) {
        context.userPreferencesDataStore.edit {
            if (name == null) it.remove(Keys.EQ_PRESET_NAME) else it[Keys.EQ_PRESET_NAME] = name
        }
    }

    override suspend fun setBassBoostStrength(strength: Int) {
        context.userPreferencesDataStore.edit { it[Keys.BASS_BOOST_STRENGTH] = strength.coerceIn(0, 1000) }
    }

    override suspend fun setVirtualizerStrength(strength: Int) {
        context.userPreferencesDataStore.edit { it[Keys.VIRTUALIZER_STRENGTH] = strength.coerceIn(0, 1000) }
    }

    override suspend fun setLoudnessGainMb(gainMb: Int) {
        context.userPreferencesDataStore.edit { it[Keys.LOUDNESS_GAIN_MB] = gainMb.coerceIn(0, 1500) }
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

    override suspend fun setEqBandLevelsMb(levelsCsv: String?) {
        context.userPreferencesDataStore.edit {
            if (levelsCsv == null) it.remove(Keys.EQ_BAND_LEVELS_MB) else it[Keys.EQ_BAND_LEVELS_MB] = levelsCsv
        }
    }

    override suspend fun setCustomEqProfilesJson(json: String?) {
        context.userPreferencesDataStore.edit {
            if (json == null) it.remove(Keys.CUSTOM_EQ_PROFILES_JSON) else it[Keys.CUSTOM_EQ_PROFILES_JSON] = json
        }
    }

    override suspend fun setStereoBalance(balance: Float) {
        context.userPreferencesDataStore.edit { it[Keys.STEREO_BALANCE] = balance.coerceIn(-1f, 1f) }
    }

    override suspend fun setSkipSilenceEnabled(enabled: Boolean) {
        context.userPreferencesDataStore.edit { it[Keys.SKIP_SILENCE_ENABLED] = enabled }
    }

    override suspend fun setMinTrackDurationSeconds(seconds: Int) {
        context.userPreferencesDataStore.edit { it[Keys.MIN_TRACK_DURATION_SECONDS] = seconds.coerceIn(0, 600) }
    }

    override suspend fun setShakeToSkipEnabled(enabled: Boolean) {
        context.userPreferencesDataStore.edit { it[Keys.SHAKE_TO_SKIP_ENABLED] = enabled }
    }
}
