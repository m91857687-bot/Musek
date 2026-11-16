package com.musicplayer.promax.presentation.library

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.musicplayer.promax.data.local.FavoriteTrackEntity
import com.musicplayer.promax.data.local.MusicLibraryDatabase
import com.musicplayer.promax.data.local.MusicTrackEntity
import com.musicplayer.promax.data.local.PlaylistEntity
import com.musicplayer.promax.data.local.PlaylistWithCount
import com.musicplayer.promax.data.metadata.TrackMetadataRepository
import com.musicplayer.promax.data.playlists.M3uPlaylistCodec
import com.musicplayer.promax.data.preferences.DataStoreUserPreferencesRepository
import com.musicplayer.promax.data.work.MusicIndexScheduler
import com.musicplayer.promax.data.work.MusicIndexWorker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AlbumGroup(
    val name: String,
    val artist: String,
    val artworkPath: String?,
    val tracks: List<MusicTrackEntity>
)

data class ArtistGroup(
    val name: String,
    val artworkPath: String?,
    val tracks: List<MusicTrackEntity>
)

data class FolderGroup(
    val folderPath: String,
    val displayName: String,
    val artworkPath: String?,
    val tracks: List<MusicTrackEntity>
)

class MusicLibraryViewModel(context: Context) : ViewModel() {
    private val appContext = context.applicationContext
    private val database = MusicLibraryDatabase.getInstance(appContext)
    private val trackDao = database.musicTrackDao()
    private val playlistDao = database.playlistDao()
    private val statsDao = database.playbackStatsDao()
    private val preferences = DataStoreUserPreferencesRepository(appContext)
    private val metadataRepository = TrackMetadataRepository(appContext)
    private val workManager = WorkManager.getInstance(appContext)

    init {
        viewModelScope.launch { metadataRepository.prefetchMissingArtwork() }
    }

    private val searchQuery = MutableStateFlow("")
    private val sortMode = MutableStateFlow(SORT_TITLE)
    private val favoritesOnly = MutableStateFlow(false)

    private val filterParams = combine(searchQuery, sortMode, favoritesOnly, preferences.minTrackDurationSeconds) { query, sort, onlyFavorites, minSec ->
        FilterParams(query, sort, onlyFavorites, minSec)
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private val filteredTracks = combine(
        searchQuery.debounce { if (it.isEmpty()) 0L else 150L },
        sortMode,
        favoritesOnly
    ) { query, sort, onlyFavorites ->
        Triple(query, sort, onlyFavorites)
    }.flatMapLatest { (query, sort, onlyFavorites) ->
        trackDao.observeLibraryTracks(query.trim(), sort, onlyFavorites)
    }.combine(preferences.minTrackDurationSeconds) { tracks, minSec ->
        if (minSec <= 0) tracks else tracks.filter { it.durationMs >= minSec * 1000L }
    }

    private val homeHighlights = combine(
        trackDao.observeLibraryTracks("", SORT_NEWEST, false),
        statsDao.observeRecentTracks(limit = 30),
        playlistDao.observeFavoriteTracks(limit = 100),
        statsDao.observeMostPlayedTracks(limit = 30),
        preferences.minTrackDurationSeconds
    ) { allNewest, recent, favorites, mostPlayed, minSec ->
        val minMs = minSec * 1000L
        fun List<MusicTrackEntity>.applyDurationFilter(): List<MusicTrackEntity> =
            if (minMs <= 0L) this else filter { it.durationMs >= minMs }
        SmartCollections(
            allNewest = allNewest.applyDurationFilter(),
            recent = recent.applyDurationFilter(),
            favorites = favorites.applyDurationFilter(),
            mostPlayed = mostPlayed.applyDurationFilter()
        )
    }

    val uiState = combine(
        trackDao.observeTrackCount(),
        filteredTracks,
        filterParams,
        workManager.getWorkInfosForUniqueWorkLiveData(MusicIndexWorker.UNIQUE_WORK_NAME).asFlow(),
        playlistDao.observePlaylists(),
        playlistDao.observeFavoriteKeys(),
        homeHighlights
    ) { values ->
        val count = values[0] as Int
        @Suppress("UNCHECKED_CAST")
        val tracks = values[1] as List<MusicTrackEntity>
        val filters = values[2] as FilterParams
        @Suppress("UNCHECKED_CAST")
        val workInfos = values[3] as List<WorkInfo>
        @Suppress("UNCHECKED_CAST")
        val playlists = values[4] as List<PlaylistWithCount>
        @Suppress("UNCHECKED_CAST")
        val favoriteKeys = values[5] as List<String>
        val highlights = values[6] as SmartCollections
        val active = workInfos.any { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
        val failed = workInfos.lastOrNull()?.state == WorkInfo.State.FAILED

        val baseForGroups = highlights.allNewest
        val albums = buildAlbumGroups(baseForGroups)
        val artists = buildArtistGroups(baseForGroups)
        val folders = buildFolderGroups(baseForGroups)

        LibraryUiState(
            trackCount = count,
            tracks = tracks,
            homeTracks = highlights.allNewest,
            albums = albums,
            artists = artists,
            folders = folders,
            isScanning = active,
            scanFailed = failed,
            searchQuery = filters.searchQuery,
            sortMode = filters.sortMode,
            favoritesOnly = filters.favoritesOnly,
            minDurationSeconds = filters.minDurationSeconds,
            favoriteKeys = favoriteKeys.toSet(),
            playlists = playlists,
            recentTracks = highlights.recent.take(8),
            smartRecentTracks = highlights.recent,
            smartMostPlayedTracks = highlights.mostPlayed,
            favoriteTracks = highlights.favorites
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun scan() = MusicIndexScheduler.enqueue(appContext)
    fun observeTrack(trackKey: String) = trackDao.observeTrack(trackKey)

    fun updateTrackMetadata(trackKey: String, title: String, artist: String, album: String) {
        viewModelScope.launch { metadataRepository.updateDisplayMetadata(trackKey, title, artist, album) }
    }

    fun updateTrackArtwork(trackKey: String, imageUri: Uri, onResult: (Result<String>) -> Unit = {}) {
        viewModelScope.launch { onResult(metadataRepository.updateCustomArtwork(trackKey, imageUri)) }
    }

    fun resetTrackArtwork(trackKey: String) {
        viewModelScope.launch { metadataRepository.resetArtwork(trackKey) }
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

    fun addFavoritesBatch(trackKeys: Collection<String>) {
        if (trackKeys.isEmpty()) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            trackKeys.forEach { key ->
                playlistDao.addFavorite(FavoriteTrackEntity(key, now))
            }
        }
    }

    fun addTracksToPlaylistBatch(playlistId: Long, trackKeys: Collection<String>) {
        if (trackKeys.isEmpty()) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            trackKeys.forEach { key ->
                playlistDao.addTrack(playlistId, key, now)
            }
        }
    }

    fun createPlaylist(name: String, onResult: (Boolean) -> Unit = {}) {
        val cleanName = name.trim()
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val created = runCatching {
                playlistDao.createPlaylistIfNameAvailable(
                    PlaylistEntity(name = cleanName, createdAt = now, updatedAt = now)
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

    fun movePlaylistTrack(playlistId: Long, currentTracks: List<MusicTrackEntity>, fromIndex: Int, toIndex: Int) {
        if (fromIndex !in currentTracks.indices || toIndex !in currentTracks.indices || fromIndex == toIndex) return
        val reordered = currentTracks.toMutableList()
        val moved = reordered.removeAt(fromIndex)
        reordered.add(toIndex, moved)
        viewModelScope.launch {
            playlistDao.reorderPlaylistTracks(
                playlistId = playlistId,
                orderedTrackKeys = reordered.map { it.trackKey },
                now = System.currentTimeMillis()
            )
        }
    }

    fun importM3uPlaylist(fallbackName: String, uri: Uri, onResult: (Result<Int>) -> Unit) {
        viewModelScope.launch {
            val result = runCatching {
                val text = appContext.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("Cannot read playlist file")
                val libraryTracks = trackDao.observeLibraryTracks("", SORT_TITLE, false).first()
                val matched = M3uPlaylistCodec.matchImportedTracks(text, libraryTracks)
                if (matched.isEmpty()) return@runCatching 0

                val desiredBase = M3uPlaylistCodec.extractPlaylistTitle(text) ?: fallbackName.ifBlank { "Imported Playlist" }
                var candidate = desiredBase.take(50)
                var suffix = 2
                while (playlistDao.playlistNameExists(candidate)) {
                    candidate = "${desiredBase.take(44)} ($suffix)"
                    suffix++
                }
                val now = System.currentTimeMillis()
                val playlistId = playlistDao.createPlaylistReturningIdIfAvailable(
                    PlaylistEntity(name = candidate, createdAt = now, updatedAt = now)
                ) ?: error("Could not create imported playlist")
                matched.forEach { track ->
                    playlistDao.addTrack(playlistId, track.trackKey, now)
                }
                matched.size
            }
            onResult(result)
        }
    }

    fun exportM3uPlaylist(playlistName: String, tracks: List<MusicTrackEntity>, uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = runCatching {
                val content = M3uPlaylistCodec.export(playlistName, tracks)
                appContext.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(content) }
                    ?: error("Cannot write playlist file")
            }.isSuccess
            onResult(success)
        }
    }

    fun setTrackInPlaylist(playlistId: Long, trackKey: String, included: Boolean) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            if (included) playlistDao.addTrack(playlistId, trackKey, now)
            else playlistDao.removeTrack(playlistId, trackKey, now)
        }
    }

    fun playlistIdsForTrack(trackKey: String) = playlistDao.observePlaylistIdsForTrack(trackKey)

    fun playlistTracks(playlistId: Long) = playlistDao.observePlaylistTracks(playlistId)

    private fun buildAlbumGroups(tracks: List<MusicTrackEntity>): List<AlbumGroup> =
        tracks.groupBy { it.displayAlbum.trim().ifBlank { "Unknown Album" } }
            .entries
            .map { (albumName, groupTracks) ->
                val sorted = groupTracks.sortedBy { it.displayTitle.lowercase() }
                AlbumGroup(
                    name = albumName,
                    artist = sorted.firstOrNull()?.displayArtist.orEmpty(),
                    artworkPath = sorted.firstOrNull { !it.artworkFilePath.isNullOrEmpty() }?.artworkFilePath,
                    tracks = sorted
                )
            }
            .sortedBy { it.name.lowercase() }

    private fun buildArtistGroups(tracks: List<MusicTrackEntity>): List<ArtistGroup> =
        tracks.groupBy { it.displayArtist.trim().ifBlank { "Unknown Artist" } }
            .entries
            .map { (artistName, groupTracks) ->
                val sorted = groupTracks.sortedBy { it.displayTitle.lowercase() }
                ArtistGroup(
                    name = artistName,
                    artworkPath = sorted.firstOrNull { !it.artworkFilePath.isNullOrEmpty() }?.artworkFilePath,
                    tracks = sorted
                )
            }
            .sortedBy { it.name.lowercase() }

    private fun buildFolderGroups(tracks: List<MusicTrackEntity>): List<FolderGroup> =
        tracks.groupBy { (it.relativePath ?: "").trim('/').ifBlank { "/" } }
            .entries
            .map { (folderPath, groupTracks) ->
                val sorted = groupTracks.sortedBy { it.displayTitle.lowercase() }
                val shortName = if (folderPath == "/") "" else folderPath.substringAfterLast('/')
                FolderGroup(
                    folderPath = folderPath,
                    displayName = shortName,
                    artworkPath = sorted.firstOrNull { !it.artworkFilePath.isNullOrEmpty() }?.artworkFilePath,
                    tracks = sorted
                )
            }
            .sortedBy { it.folderPath.lowercase() }

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

private data class FilterParams(
    val searchQuery: String,
    val sortMode: String,
    val favoritesOnly: Boolean,
    val minDurationSeconds: Int
)

private data class SmartCollections(
    val allNewest: List<MusicTrackEntity>,
    val recent: List<MusicTrackEntity>,
    val favorites: List<MusicTrackEntity>,
    val mostPlayed: List<MusicTrackEntity>
)

data class LibraryUiState(
    val trackCount: Int = 0,
    val tracks: List<MusicTrackEntity> = emptyList(),
    val homeTracks: List<MusicTrackEntity> = emptyList(),
    val albums: List<AlbumGroup> = emptyList(),
    val artists: List<ArtistGroup> = emptyList(),
    val folders: List<FolderGroup> = emptyList(),
    val isScanning: Boolean = false,
    val scanFailed: Boolean = false,
    val searchQuery: String = "",
    val sortMode: String = MusicLibraryViewModel.SORT_TITLE,
    val favoritesOnly: Boolean = false,
    val minDurationSeconds: Int = 0,
    val favoriteKeys: Set<String> = emptySet(),
    val playlists: List<PlaylistWithCount> = emptyList(),
    val recentTracks: List<MusicTrackEntity> = emptyList(),
    val smartRecentTracks: List<MusicTrackEntity> = emptyList(),
    val smartMostPlayedTracks: List<MusicTrackEntity> = emptyList(),
    val favoriteTracks: List<MusicTrackEntity> = emptyList()
)
