package com.musicplayer.promax.domain.repository

import com.musicplayer.promax.domain.model.AppLanguage
import com.musicplayer.promax.domain.model.AppAccent
import com.musicplayer.promax.domain.model.AppTheme
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val language: Flow<AppLanguage>
    val theme: Flow<AppTheme>
    val accent: Flow<AppAccent>
    val sleepTimerEndMillis: Flow<Long?>
    val internalMusicEnabled: Flow<Boolean>
    val externalMusicEnabled: Flow<Boolean>
    val externalTreeUri: Flow<String?>
    suspend fun setLanguage(language: AppLanguage)
    suspend fun setTheme(theme: AppTheme)
    suspend fun setAccent(accent: AppAccent)
    suspend fun setSleepTimerEndMillis(endMillis: Long?)
    suspend fun setInternalMusicEnabled(enabled: Boolean)
    suspend fun setExternalMusicEnabled(enabled: Boolean)
    suspend fun setExternalTreeUri(uri: String?)
}
