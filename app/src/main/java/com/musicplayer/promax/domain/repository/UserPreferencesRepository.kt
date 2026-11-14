package com.musicplayer.promax.domain.repository

import com.musicplayer.promax.domain.model.AppLanguage
import com.musicplayer.promax.domain.model.AppAccent
import com.musicplayer.promax.domain.model.AppTheme
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val language: Flow<AppLanguage>
    val theme: Flow<AppTheme>
    val accent: Flow<AppAccent>
    val dynamicArtworkThemeEnabled: Flow<Boolean>
    val playbackSpeed: Flow<Float>
    val crossfadeSeconds: Flow<Int>
    val audioEffectsEnabled: Flow<Boolean>
    val eqPresetName: Flow<String?>
    val bassBoostStrength: Flow<Int>
    val virtualizerStrength: Flow<Int>
    val loudnessGainMb: Flow<Int>
    val sleepTimerEndMillis: Flow<Long?>
    val internalMusicEnabled: Flow<Boolean>
    val externalMusicEnabled: Flow<Boolean>
    val externalTreeUri: Flow<String?>
    suspend fun setLanguage(language: AppLanguage)
    suspend fun setTheme(theme: AppTheme)
    suspend fun setAccent(accent: AppAccent)
    suspend fun setDynamicArtworkThemeEnabled(enabled: Boolean)
    suspend fun setPlaybackSpeed(speed: Float)
    suspend fun setCrossfadeSeconds(seconds: Int)
    suspend fun setAudioEffectsEnabled(enabled: Boolean)
    suspend fun setEqPresetName(name: String?)
    suspend fun setBassBoostStrength(strength: Int)
    suspend fun setVirtualizerStrength(strength: Int)
    suspend fun setLoudnessGainMb(gainMb: Int)
    suspend fun setSleepTimerEndMillis(endMillis: Long?)
    suspend fun setInternalMusicEnabled(enabled: Boolean)
    suspend fun setExternalMusicEnabled(enabled: Boolean)
    suspend fun setExternalTreeUri(uri: String?)
}
