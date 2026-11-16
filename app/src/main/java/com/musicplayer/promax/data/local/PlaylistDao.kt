package com.musicplayer.promax.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT p.*, COUNT(t.trackKey) AS trackCount FROM playlists p LEFT JOIN playlist_tracks pt ON p.playlistId = pt.playlistId LEFT JOIN music_tracks t ON pt.trackKey = t.trackKey GROUP BY p.playlistId ORDER BY p.updatedAt DESC, p.name COLLATE NOCASE ASC")
    fun observePlaylists(): Flow<List<PlaylistWithCount>>

    @Query("SELECT t.* FROM music_tracks t INNER JOIN playlist_tracks pt ON t.trackKey = pt.trackKey WHERE pt.playlistId = :playlistId ORDER BY pt.position ASC, pt.addedAt ASC")
    fun observePlaylistTracks(playlistId: Long): Flow<List<MusicTrackEntity>>

    @Query("SELECT playlistId FROM playlist_tracks WHERE trackKey = :trackKey")
    fun observePlaylistIdsForTrack(trackKey: String): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :name, updatedAt = :updatedAt WHERE playlistId = :playlistId")
    suspend fun renamePlaylist(playlistId: Long, name: String, updatedAt: Long): Int

    @Query("SELECT EXISTS(SELECT 1 FROM playlists WHERE name = :name COLLATE NOCASE AND playlistId != :playlistId)")
    suspend fun playlistNameExistsExcept(name: String, playlistId: Long): Boolean

    @Transaction
    suspend fun createPlaylistIfNameAvailable(playlist: PlaylistEntity): Boolean {
        if (playlist.name.isBlank() || playlistNameExists(playlist.name)) return false
        insertPlaylist(playlist)
        return true
    }

    @Transaction
    suspend fun createPlaylistReturningIdIfAvailable(playlist: PlaylistEntity): Long? {
        if (playlist.name.isBlank() || playlistNameExists(playlist.name)) return null
        return insertPlaylist(playlist)
    }

    @Transaction
    suspend fun renamePlaylistIfNameAvailable(playlistId: Long, name: String, updatedAt: Long): Boolean {
        if (name.isBlank() || playlistNameExistsExcept(name, playlistId)) return false
        return renamePlaylist(playlistId, name, updatedAt) > 0
    }

    @Query("DELETE FROM playlists WHERE playlistId = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun nextPosition(playlistId: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylistTrack(item: PlaylistTrackEntity): Long

    @Query("UPDATE playlists SET updatedAt = :updatedAt WHERE playlistId = :playlistId")
    suspend fun touchPlaylist(playlistId: Long, updatedAt: Long)

    @Transaction
    suspend fun addTrack(playlistId: Long, trackKey: String, now: Long) {
        val inserted = insertPlaylistTrack(PlaylistTrackEntity(playlistId, trackKey, nextPosition(playlistId), now))
        if (inserted != -1L) touchPlaylist(playlistId, now)
    }

    @Query("UPDATE playlist_tracks SET position = :position WHERE playlistId = :playlistId AND trackKey = :trackKey")
    suspend fun updateTrackPosition(playlistId: Long, trackKey: String, position: Int)

    @Transaction
    suspend fun reorderPlaylistTracks(playlistId: Long, orderedTrackKeys: List<String>, now: Long) {
        orderedTrackKeys.forEachIndexed { index, trackKey ->
            updateTrackPosition(playlistId, trackKey, index)
        }
        touchPlaylist(playlistId, now)
    }

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackKey = :trackKey")
    suspend fun deletePlaylistTrack(playlistId: Long, trackKey: String): Int

    @Transaction
    suspend fun removeTrack(playlistId: Long, trackKey: String, now: Long) {
        if (deletePlaylistTrack(playlistId, trackKey) > 0) touchPlaylist(playlistId, now)
    }

    @Query("SELECT trackKey FROM favorite_tracks")
    fun observeFavoriteKeys(): Flow<List<String>>

    @Query("SELECT t.* FROM music_tracks t INNER JOIN favorite_tracks f ON t.trackKey = f.trackKey ORDER BY f.createdAt DESC LIMIT :limit")
    fun observeFavoriteTracks(limit: Int = 25): Flow<List<MusicTrackEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addFavorite(favorite: FavoriteTrackEntity)

    @Query("DELETE FROM favorite_tracks WHERE trackKey = :trackKey")
    suspend fun removeFavorite(trackKey: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_tracks WHERE trackKey = :trackKey)")
    suspend fun isFavorite(trackKey: String): Boolean

    @Transaction
    suspend fun toggleFavorite(trackKey: String, now: Long): Boolean {
        return if (isFavorite(trackKey)) {
            removeFavorite(trackKey)
            false
        } else {
            addFavorite(FavoriteTrackEntity(trackKey, now))
            true
        }
    }

    @Query("SELECT EXISTS(SELECT 1 FROM playlists WHERE name = :name COLLATE NOCASE)")
    suspend fun playlistNameExists(name: String): Boolean
}
