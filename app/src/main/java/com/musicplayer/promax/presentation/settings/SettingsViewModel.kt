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
    val sleepTimerEndMillis: Long? = null,
    val internalMusicEnabled: Boolean = true,
    val externalMusicEnabled: Boolean = true,
    val externalTreeUri: String? = null,
    val isLoaded: Boolean = false
)

class SettingsViewModel(
    private val preferences: UserPreferencesRepository
) : ViewModel() {

    private val appearanceState = combine(preferences.language, preferences.theme, preferences.accent) { language, theme, accent ->
        Triple(language, theme, accent)
    }
    private val sleepTimerState = preferences.sleepTimerEndMillis

    private val sourceState = combine(
        preferences.internalMusicEnabled,
        preferences.externalMusicEnabled,
        preferences.externalTreeUri
    ) { internalEnabled, externalEnabled, treeUri ->
        Triple(internalEnabled, externalEnabled, treeUri)
    }

    val uiState = combine(appearanceState, sourceState, sleepTimerState) { appearance, sources, timerEnd ->
        SettingsUiState(
            language = appearance.first,
            theme = appearance.second,
            accent = appearance.third,
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
