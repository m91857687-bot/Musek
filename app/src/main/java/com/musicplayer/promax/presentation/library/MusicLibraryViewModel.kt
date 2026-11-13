package com.musicplayer.promax.presentation.library

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.asFlow
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.musicplayer.promax.data.local.MusicLibraryDatabase
import com.musicplayer.promax.data.local.MusicTrackEntity
import com.musicplayer.promax.data.local.PlaylistWithCount
import com.musicplayer.promax.data.metadata.TrackMetadataRepository
import com.musicplayer.promax.data.work.MusicIndexScheduler
import com.musicplayer.promax.data.work.MusicIndexWorker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MusicLibraryViewModel(context: Context) : ViewModel() {
    private val appContext = context.applicationContext
    private val database = MusicLibraryDatabase.getInstance(appContext)
    private val trackDao = database.musicTrackDao()
    private val playlistDao = database.playlistDao()
    private val statsDao = database.playbackStatsDao()
    private val metadataRepository = TrackMetadataRepository(appContext)
    private val workManager = WorkManager.getInstance(appContext)

    private val searchQuery = MutableStateFlow("")
    private val sortMode = MutableStateFlow(SORT_TITLE)
    private val favoritesOnly = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private val filteredTracks = combine(searchQuery.debounce(250), sortMode, favoritesOnly) { query, sort, onlyFavorites ->
        Triple(query, sort, onlyFavorites)
    }.flatMapLatest { (query, sort, onlyFavorites) ->
        trackDao.observeLibraryTracks(query.trim(), sort, onlyFavorites)
    }

    private val homeHighlights = combine(
        statsDao.observeRecentTracks(),
        playlistDao.observeFavoriteTracks()
    ) { recent, favorites -> recent to favorites }

    val uiState = combine(
        trackDao.observeTrackCount(),
        filteredTracks,
        workManager.getWorkInfosForUniqueWorkLiveData(MusicIndexWorker.UNIQUE_WORK_NAME).asFlow(),
        playlistDao.observePlaylists(),
        playlistDao.observeFavoriteKeys(),
        homeHighlights
    ) { values ->
        val count = values[0] as Int
        @Suppress("UNCHECKED_CAST")
        val tracks = values[1] as List<MusicTrackEntity>
        @Suppress("UNCHECKED_CAST")
        val workInfos = values[2] as List<WorkInfo>
        @Suppress("UNCHECKED_CAST")
        val playlists = values[3] as List<PlaylistWithCount>
        @Suppress("UNCHECKED_CAST")
        val favoriteKeys = values[4] as List<String>
        @Suppress("UNCHECKED_CAST")
        val highlights = values[5] as Pair<List<MusicTrackEntity>, List<MusicTrackEntity>>
        val active = workInfos.any { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
        val failed = workInfos.lastOrNull()?.state == WorkInfo.State.FAILED
        LibraryUiState(
            trackCount = count,
            tracks = tracks,
            isScanning = active,
            scanFailed = failed,
            searchQuery = searchQuery.value,
            sortMode = sortMode.value,
            favoritesOnly = favoritesOnly.value,
            favoriteKeys = favoriteKeys.toSet(),
            playlists = playlists,
            recentTracks = highlights.first,
            favoriteTracks = highlights.second
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun scan() = MusicIndexScheduler.enqueue(appContext)
    fun observeTrack(trackKey: String) = trackDao.observeTrack(trackKey)

    fun updateTrackMetadata(trackKey: String, title: String, artist: String, album: String) {
        viewModelScope.launch { metadataRepository.updateDisplayMetadata(trackKey, title, artist, album) }
    }

    fun importLyrics(trackKey: String, uri: Uri, onResult: (Result<Int>) -> Unit = {}) {
        viewModelScope.launch { onResult(metadataRepository.saveLyricsFromUri(trackKey, uri)) }
    }
    fun setSearchQuery(value: String) { searchQuery.value = value }
    fun setSortMode(value: String) { sortMode.value = value }
    fun setFavoritesOnly(value: Boolean) { favoritesOnly.value = value }

    fun toggleFavorite(trackKey: String) {
        viewModelScope.launch { playlistDao.toggleFavorite(trackKey, System.currentTimeMillis()) }
    }

    fun createPlaylist(name: String, onResult: (Boolean) -> Unit = {}) {
        val cleanName = name.trim()
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val created = runCatching {
                playlistDao.createPlaylistIfNameAvailable(
                    com.musicplayer.promax.data.local.PlaylistEntity(name = cleanName, createdAt = now, updatedAt = now)
                )
            }.getOrDefault(false)
            onResult(created)
        }
    }

    fun renamePlaylist(playlistId: Long, name: String, onResult: (Boolean) -> Unit = {}) {
        val cleanName = name.trim()
        viewModelScope.launch {
            val renamed = runCatching {
                playlistDao.renamePlaylistIfNameAvailable(playlistId, cleanName, System.currentTimeMillis())
            }.getOrDefault(false)
            onResult(renamed)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch { playlistDao.deletePlaylist(playlistId) }
    }

    fun addTrackToPlaylist(playlistId: Long, trackKey: String) {
        viewModelScope.launch { playlistDao.addTrack(playlistId, trackKey, System.currentTimeMillis()) }
    }

    fun removeTrackFromPlaylist(playlistId: Long, trackKey: String) {
        viewModelScope.launch { playlistDao.removeTrack(playlistId, trackKey, System.currentTimeMillis()) }
    }

    fun playlistTracks(playlistId: Long) = playlistDao.observePlaylistTracks(playlistId)

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MusicLibraryViewModel::class.java)) {
                return MusicLibraryViewModel(context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }

    companion object {
        const val SORT_TITLE = "TITLE"
        const val SORT_ARTIST = "ARTIST"
        const val SORT_ALBUM = "ALBUM"
        const val SORT_DURATION = "DURATION"
        const val SORT_NEWEST = "NEWEST"
    }
}

data class LibraryUiState(
    val trackCount: Int = 0,
    val tracks: List<MusicTrackEntity> = emptyList(),
    val isScanning: Boolean = false,
    val scanFailed: Boolean = false,
    val searchQuery: String = "",
    val sortMode: String = MusicLibraryViewModel.SORT_TITLE,
    val favoritesOnly: Boolean = false,
    val favoriteKeys: Set<String> = emptySet(),
    val playlists: List<PlaylistWithCount> = emptyList(),
    val recentTracks: List<MusicTrackEntity> = emptyList(),
    val favoriteTracks: List<MusicTrackEntity> = emptyList()
)
