package com.musicplayer.promax.presentation.statistics

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.musicplayer.promax.data.local.MusicLibraryDatabase
import com.musicplayer.promax.data.stats.TopTrackStat
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class StatisticsViewModel(context: Context) : ViewModel() {
    private val dao = MusicLibraryDatabase.getInstance(context.applicationContext).playbackStatsDao()
    val state = combine(
        dao.observeTotalPlays(),
        dao.observeListeningTime(),
        dao.observeUniqueTracks(),
        dao.observeTopTracks()
    ) { plays, listeningMs, uniqueTracks, topTracks ->
        StatisticsUiState(plays, listeningMs, uniqueTracks, topTracks)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatisticsUiState())

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(StatisticsViewModel::class.java)) return StatisticsViewModel(context) as T
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

data class StatisticsUiState(
    val totalPlays: Long = 0,
    val listeningMs: Long = 0,
    val uniqueTracks: Int = 0,
    val topTracks: List<TopTrackStat> = emptyList()
)
