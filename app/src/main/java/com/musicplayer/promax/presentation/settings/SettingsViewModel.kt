package com.musicplayer.promax.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.musicplayer.promax.domain.repository.UserPreferencesRepository
import com.musicplayer.promax.domain.model.AppLanguage
import com.musicplayer.promax.domain.model.AppAccent
import com.musicplayer.promax.domain.model.AppTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val language: AppLanguage = AppLanguage.SYSTEM,
    val theme: AppTheme = AppTheme.CLASSIC,
    val accent: AppAccent = AppAccent.ELECTRIC_BLUE,
    val dynamicArtworkThemeEnabled: Boolean = true,
    val playbackSpeed: Float = 1.0f,
    val crossfadeSeconds: Int = 2,
    val audioEffectsEnabled: Boolean = true,
    val eqPresetName: String? = null,
    val bassBoostStrength: Int = 0,
    val virtualizerStrength: Int = 0,
    val loudnessGainMb: Int = 0,
    val sleepTimerEndMillis: Long? = null,
    val internalMusicEnabled: Boolean = true,
    val externalMusicEnabled: Boolean = true,
    val externalTreeUri: String? = null,
    val isLoaded: Boolean = false
)

class SettingsViewModel(
    private val preferences: UserPreferencesRepository
) : ViewModel() {

    private data class AudioEnhancementPrefs(
        val speed: Float,
        val crossfadeSeconds: Int,
        val effectsEnabled: Boolean,
        val eqPresetName: String?,
        val bassStrength: Int,
        val virtualizerStrength: Int,
        val loudnessGainMb: Int
    )

    private val appearanceState = combine(
        preferences.language,
        preferences.theme,
        preferences.accent,
        preferences.dynamicArtworkThemeEnabled
    ) { language, theme, accent, dynamicArtwork ->
        Quad(language, theme, accent, dynamicArtwork)
    }

    private val audioState = combine(
        preferences.playbackSpeed,
        preferences.crossfadeSeconds,
        preferences.audioEffectsEnabled,
        preferences.eqPresetName,
        preferences.bassBoostStrength
    ) { speed, crossfade, enabled, preset, bass ->
        AudioEnhancementPrefs(speed, crossfade, enabled, preset, bass, 0, 0)
    }.combine(
        combine(preferences.virtualizerStrength, preferences.loudnessGainMb) { virt, loud -> virt to loud }
    ) { base, (virt, loud) ->
        base.copy(virtualizerStrength = virt, loudnessGainMb = loud)
    }

    private val sourceState = combine(
        preferences.internalMusicEnabled,
        preferences.externalMusicEnabled,
        preferences.externalTreeUri
    ) { internalEnabled, externalEnabled, treeUri ->
        Triple(internalEnabled, externalEnabled, treeUri)
    }

    val uiState = combine(appearanceState, audioState, sourceState, preferences.sleepTimerEndMillis) { appearance, audio, sources, timerEnd ->
        SettingsUiState(
            language = appearance.first,
            theme = appearance.second,
            accent = appearance.third,
            dynamicArtworkThemeEnabled = appearance.fourth,
            playbackSpeed = audio.speed,
            crossfadeSeconds = audio.crossfadeSeconds,
            audioEffectsEnabled = audio.effectsEnabled,
            eqPresetName = audio.eqPresetName,
            bassBoostStrength = audio.bassStrength,
            virtualizerStrength = audio.virtualizerStrength,
            loudnessGainMb = audio.loudnessGainMb,
            sleepTimerEndMillis = timerEnd,
            internalMusicEnabled = sources.first,
            externalMusicEnabled = sources.second,
            externalTreeUri = sources.third,
            isLoaded = true
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    fun selectLanguage(language: AppLanguage) {
        viewModelScope.launch { preferences.setLanguage(language) }
    }

    fun selectTheme(theme: AppTheme) {
        viewModelScope.launch { preferences.setTheme(theme) }
    }

    fun selectAccent(accent: AppAccent) {
        viewModelScope.launch { preferences.setAccent(accent) }
    }

    fun setDynamicArtworkThemeEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setDynamicArtworkThemeEnabled(enabled) }
    }

    fun setPlaybackSpeed(speed: Float) {
        viewModelScope.launch { preferences.setPlaybackSpeed(speed) }
    }

    fun setCrossfadeSeconds(seconds: Int) {
        viewModelScope.launch { preferences.setCrossfadeSeconds(seconds) }
    }

    fun setAudioEffectsEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setAudioEffectsEnabled(enabled) }
    }

    fun setEqPresetName(name: String?) {
        viewModelScope.launch { preferences.setEqPresetName(name) }
    }

    fun setBassBoostStrength(strength: Int) {
        viewModelScope.launch { preferences.setBassBoostStrength(strength) }
    }

    fun setVirtualizerStrength(strength: Int) {
        viewModelScope.launch { preferences.setVirtualizerStrength(strength) }
    }

    fun setLoudnessGainMb(gainMb: Int) {
        viewModelScope.launch { preferences.setLoudnessGainMb(gainMb) }
    }

    fun setSleepTimer(minutes: Int?) {
        viewModelScope.launch {
            val end = minutes?.let { System.currentTimeMillis() + it * 60_000L }
            preferences.setSleepTimerEndMillis(end)
        }
    }

    fun setInternalMusicEnabled(enabled: Boolean, afterSave: () -> Unit = {}) {
        viewModelScope.launch {
            preferences.setInternalMusicEnabled(enabled)
            afterSave()
        }
    }

    fun setExternalMusicEnabled(enabled: Boolean, afterSave: () -> Unit = {}) {
        viewModelScope.launch {
            preferences.setExternalMusicEnabled(enabled)
            afterSave()
        }
    }

    fun setExternalTreeUri(uri: String?, afterSave: () -> Unit = {}) {
        viewModelScope.launch {
            preferences.setExternalTreeUri(uri)
            afterSave()
        }
    }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    class Factory(
        private val preferences: UserPreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                return SettingsViewModel(preferences) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
