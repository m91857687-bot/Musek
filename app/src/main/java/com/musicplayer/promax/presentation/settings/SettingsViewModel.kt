package com.musicplayer.promax.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.musicplayer.promax.domain.model.AppAccent
import com.musicplayer.promax.domain.model.AppLanguage
import com.musicplayer.promax.domain.model.AppTheme
import com.musicplayer.promax.domain.model.CustomEqProfile
import com.musicplayer.promax.domain.model.CustomEqProfileCodec
import com.musicplayer.promax.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class SettingsUiState(
    val language: AppLanguage = AppLanguage.SYSTEM,
    val theme: AppTheme = AppTheme.CLASSIC,
    val accent: AppAccent = AppAccent.ELECTRIC_BLUE,
    val dynamicArtworkThemeEnabled: Boolean = true,
    val playbackSpeed: Float = 1.0f,
    val crossfadeSeconds: Int = 2,
    val audioEffectsEnabled: Boolean = true,
    val eqPresetName: String? = null,
    val eqBandLevelsMb: List<Int> = emptyList(),
    val customEqProfiles: List<CustomEqProfile> = emptyList(),
    val bassBoostStrength: Int = 0,
    val virtualizerStrength: Int = 0,
    val loudnessGainMb: Int = 0,
    val stereoBalance: Float = 0f,
    val skipSilenceEnabled: Boolean = false,
    val sleepTimerEndMillis: Long? = null,
    val internalMusicEnabled: Boolean = true,
    val externalMusicEnabled: Boolean = true,
    val externalTreeUri: String? = null,
    val minTrackDurationSeconds: Int = 0,
    val shakeToSkipEnabled: Boolean = false,
    val isLoaded: Boolean = false
)

class SettingsViewModel(
    private val preferences: UserPreferencesRepository
) : ViewModel() {

    private data class AudioCorePrefs(
        val speed: Float,
        val crossfadeSeconds: Int,
        val effectsEnabled: Boolean,
        val eqPresetName: String?,
        val bassStrength: Int
    )

    private data class ProEqPrefs(
        val virtualizerStrength: Int,
        val loudnessGainMb: Int,
        val eqBandLevelsMb: List<Int>,
        val customEqProfiles: List<CustomEqProfile>,
        val stereoBalance: Float,
        val skipSilenceEnabled: Boolean
    )

    private data class SourceAndUtilityPrefs(
        val internalMusicEnabled: Boolean,
        val externalMusicEnabled: Boolean,
        val externalTreeUri: String?,
        val minTrackDurationSeconds: Int,
        val shakeToSkipEnabled: Boolean
    )

    private val appearanceState = combine(
        preferences.language,
        preferences.theme,
        preferences.accent,
        preferences.dynamicArtworkThemeEnabled
    ) { language, theme, accent, dynamicArtwork ->
        Quad(language, theme, accent, dynamicArtwork)
    }

    private val audioCoreState = combine(
        preferences.playbackSpeed,
        preferences.crossfadeSeconds,
        preferences.audioEffectsEnabled,
        preferences.eqPresetName,
        preferences.bassBoostStrength
    ) { speed, crossfade, enabled, preset, bass ->
        AudioCorePrefs(speed, crossfade, enabled, preset, bass)
    }

    private val proEqState = combine(
        preferences.virtualizerStrength,
        preferences.loudnessGainMb,
        preferences.eqBandLevelsMb,
        preferences.customEqProfilesJson,
        preferences.stereoBalance
    ) { virt, loud, levelsCsv, profilesJson, balance ->
        ProEqPrefs(
            virtualizerStrength = virt,
            loudnessGainMb = loud,
            eqBandLevelsMb = CustomEqProfileCodec.decodeBandLevels(levelsCsv),
            customEqProfiles = CustomEqProfileCodec.decodeProfiles(profilesJson),
            stereoBalance = balance,
            skipSilenceEnabled = false
        )
    }.combine(preferences.skipSilenceEnabled) { pro, skipSilence ->
        pro.copy(skipSilenceEnabled = skipSilence)
    }

    private val sourceState = combine(
        preferences.internalMusicEnabled,
        preferences.externalMusicEnabled,
        preferences.externalTreeUri,
        preferences.minTrackDurationSeconds,
        preferences.shakeToSkipEnabled
    ) { internalEnabled, externalEnabled, treeUri, minDuration, shakeToSkip ->
        SourceAndUtilityPrefs(internalEnabled, externalEnabled, treeUri, minDuration, shakeToSkip)
    }

    val uiState = combine(
        appearanceState,
        audioCoreState,
        proEqState,
        sourceState,
        preferences.sleepTimerEndMillis
    ) { appearance, audioCore, proEq, sources, timerEnd ->
        SettingsUiState(
            language = appearance.first,
            theme = appearance.second,
            accent = appearance.third,
            dynamicArtworkThemeEnabled = appearance.fourth,
            playbackSpeed = audioCore.speed,
            crossfadeSeconds = audioCore.crossfadeSeconds,
            audioEffectsEnabled = audioCore.effectsEnabled,
            eqPresetName = audioCore.eqPresetName,
            eqBandLevelsMb = proEq.eqBandLevelsMb,
            customEqProfiles = proEq.customEqProfiles,
            bassBoostStrength = audioCore.bassStrength,
            virtualizerStrength = proEq.virtualizerStrength,
            loudnessGainMb = proEq.loudnessGainMb,
            stereoBalance = proEq.stereoBalance,
            skipSilenceEnabled = proEq.skipSilenceEnabled,
            sleepTimerEndMillis = timerEnd,
            internalMusicEnabled = sources.internalMusicEnabled,
            externalMusicEnabled = sources.externalMusicEnabled,
            externalTreeUri = sources.externalTreeUri,
            minTrackDurationSeconds = sources.minTrackDurationSeconds,
            shakeToSkipEnabled = sources.shakeToSkipEnabled,
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

    fun setEqBandLevelsMb(levelsMb: List<Int>) {
        viewModelScope.launch {
            preferences.setEqBandLevelsMb(
                if (levelsMb.isEmpty()) null else CustomEqProfileCodec.encodeBandLevels(levelsMb)
            )
        }
    }

    fun saveCustomEqProfile(
        name: String,
        bandsMb: List<Int>,
        bassStrength: Int,
        virtualizerStrength: Int,
        loudnessMb: Int,
        stereoBalance: Float
    ) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            val current = uiState.value.customEqProfiles.toMutableList()
            val existingIndex = current.indexOfFirst { it.name.equals(clean, ignoreCase = true) }
            val profile = CustomEqProfile(
                id = if (existingIndex >= 0) current[existingIndex].id else UUID.randomUUID().toString(),
                name = clean,
                bandsMb = bandsMb,
                bassStrength = bassStrength,
                virtualizerStrength = virtualizerStrength,
                loudnessMb = loudnessMb,
                stereoBalance = stereoBalance
            )
            if (existingIndex >= 0) current[existingIndex] = profile else current.add(0, profile)
            preferences.setCustomEqProfilesJson(CustomEqProfileCodec.encodeProfiles(current))
        }
    }

    fun deleteCustomEqProfile(profileId: String) {
        viewModelScope.launch {
            val updated = uiState.value.customEqProfiles.filterNot { it.id == profileId }
            preferences.setCustomEqProfilesJson(
                if (updated.isEmpty()) null else CustomEqProfileCodec.encodeProfiles(updated)
            )
        }
    }

    fun applyCustomEqProfile(profile: CustomEqProfile) {
        viewModelScope.launch {
            preferences.setEqPresetName(null)
            preferences.setEqBandLevelsMb(
                if (profile.bandsMb.isEmpty()) null else CustomEqProfileCodec.encodeBandLevels(profile.bandsMb)
            )
            preferences.setBassBoostStrength(profile.bassStrength)
            preferences.setVirtualizerStrength(profile.virtualizerStrength)
            preferences.setLoudnessGainMb(profile.loudnessMb)
            preferences.setStereoBalance(profile.stereoBalance)
        }
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

    fun setStereoBalance(balance: Float) {
        viewModelScope.launch { preferences.setStereoBalance(balance) }
    }

    fun setSkipSilenceEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setSkipSilenceEnabled(enabled) }
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

    fun setMinTrackDurationSeconds(seconds: Int) {
        viewModelScope.launch { preferences.setMinTrackDurationSeconds(seconds) }
    }

    fun setShakeToSkipEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setShakeToSkipEnabled(enabled) }
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
