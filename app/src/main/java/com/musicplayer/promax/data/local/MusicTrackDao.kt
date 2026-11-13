package com.musicplayer.promax.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface MusicTrackDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTracksIfMissing(tracks: List<MusicTrackEntity>)

    @Query("""
        UPDATE music_tracks SET
            mediaStoreId = :mediaStoreId,
            volumeName = :volumeName,
            contentUri = :contentUri,
            title = CASE WHEN :volumeName LIKE 'saf:%' AND title != '' THEN title ELSE :title END,
            artist = CASE WHEN :artist != '' THEN :artist ELSE artist END,
            album = CASE WHEN :album != '' THEN :album ELSE album END,
            durationMs = CASE WHEN :durationMs > 0 THEN :durationMs ELSE durationMs END,
            artworkFilePath = CASE WHEN sizeBytes != :sizeBytes OR dateModifiedSeconds != :dateModifiedSeconds THEN NULL ELSE artworkFilePath END,
            lyricsLrc = CASE WHEN sizeBytes != :sizeBytes OR dateModifiedSeconds != :dateModifiedSeconds THEN NULL ELSE lyricsLrc END,
            sizeBytes = :sizeBytes,
            dateModifiedSeconds = :dateModifiedSeconds,
            mimeType = :mimeType,
            relativePath = :relativePath
        WHERE trackKey = :trackKey
    """)
    suspend fun updateIndexedFields(
        trackKey: String, mediaStoreId: Long, volumeName: String, contentUri: String,
        title: String, artist: String, album: String, durationMs: Long, sizeBytes: Long,
        dateModifiedSeconds: Long, mimeType: String?, relativePath: String?
    )

    @Transaction
    suspend fun upsertTracks(tracks: List<MusicTrackEntity>) {
        insertTracksIfMissing(tracks)
        tracks.forEach { track ->
            updateIndexedFields(
                track.trackKey, track.mediaStoreId, track.volumeName, track.contentUri,
                track.title, track.artist, track.album, track.durationMs, track.sizeBytes,
                track.dateModifiedSeconds, track.mimeType, track.relativePath
            )
        }
    }

    @Query("SELECT * FROM music_tracks WHERE trackKey = :trackKey LIMIT 1")
    fun observeTrack(trackKey: String): Flow<MusicTrackEntity?>

    @Query("SELECT * FROM music_tracks WHERE trackKey = :trackKey LIMIT 1")
    suspend fun getTrack(trackKey: String): MusicTrackEntity?

    @Query("UPDATE music_tracks SET titleOverride = :title, artistOverride = :artist, albumOverride = :album WHERE trackKey = :trackKey")
    suspend fun updateDisplayMetadata(trackKey: String, title: String?, artist: String?, album: String?)

    @Query("""
        UPDATE music_tracks SET
            title = CASE WHEN :title IS NOT NULL AND :title != '' THEN :title ELSE title END,
            artist = CASE WHEN :artist IS NOT NULL AND :artist != '' THEN :artist ELSE artist END,
            album = CASE WHEN :album IS NOT NULL AND :album != '' THEN :album ELSE album END,
            durationMs = CASE WHEN :durationMs > 0 THEN :durationMs ELSE durationMs END
        WHERE trackKey = :trackKey
    """)
    suspend fun updateEmbeddedMetadata(trackKey: String, title: String?, artist: String?, album: String?, durationMs: Long)

    @Query("UPDATE music_tracks SET artworkFilePath = :path WHERE trackKey = :trackKey")
    suspend fun updateArtworkPath(trackKey: String, path: String?)

    @Query("UPDATE music_tracks SET lyricsLrc = :lyrics WHERE trackKey = :trackKey")
    suspend fun updateLyrics(trackKey: String, lyrics: String?)

    @Query("SELECT COUNT(*) FROM music_tracks")
    fun observeTrackCount(): Flow<Int>

    @Query("""
        SELECT t.* FROM music_tracks t
        WHERE (:query = '' OR COALESCE(t.titleOverride, t.title) LIKE '%' || :query || '%' COLLATE NOCASE
            OR COALESCE(t.artistOverride, t.artist) LIKE '%' || :query || '%' COLLATE NOCASE
            OR COALESCE(t.albumOverride, t.album) LIKE '%' || :query || '%' COLLATE NOCASE)
          AND (:favoritesOnly = 0 OR EXISTS (
              SELECT 1 FROM favorite_tracks f WHERE f.trackKey = t.trackKey
          ))
        ORDER BY
            CASE WHEN :sortMode = 'ARTIST' THEN COALESCE(t.artistOverride, t.artist) END COLLATE NOCASE ASC,
            CASE WHEN :sortMode = 'ALBUM' THEN COALESCE(t.albumOverride, t.album) END COLLATE NOCASE ASC,
            CASE WHEN :sortMode = 'DURATION' THEN t.durationMs END ASC,
            CASE WHEN :sortMode = 'NEWEST' THEN t.dateModifiedSeconds END DESC,
            COALESCE(t.titleOverride, t.title) COLLATE NOCASE ASC
    """)
    fun observeLibraryTracks(query: String, sortMode: String, favoritesOnly: Boolean): Flow<List<MusicTrackEntity>>

    @Query("SELECT trackKey FROM music_tracks WHERE volumeName = :volumeName")
    suspend fun trackKeysForVolume(volumeName: String): List<String>

    @Query("SELECT DISTINCT volumeName FROM music_tracks")
    suspend fun allVolumeNames(): List<String>

    @Query("SELECT contentUri, dateModifiedSeconds, sizeBytes FROM music_tracks WHERE volumeName = :volumeName")
    suspend fun fingerprintsForVolume(volumeName: String): List<TrackFingerprint>

    @Query("DELETE FROM music_tracks WHERE trackKey IN (:trackKeys)")
    suspend fun deleteTracksChunk(trackKeys: List<String>)

    @Transaction
    suspend fun deleteTracks(trackKeys: List<String>) {
        if (trackKeys.isEmpty()) return
        trackKeys.chunked(500).forEach { chunk ->
            deleteTracksChunk(chunk)
        }
    }

    @Query("DELETE FROM music_tracks WHERE volumeName = :volumeName")
    suspend fun deleteVolume(volumeName: String)

    @Query("DELETE FROM music_tracks")
    suspend fun deleteAllTracks()

    @Query("DELETE FROM music_tracks WHERE volumeName LIKE 'saf:%'")
    suspend fun deleteSafTracks()

    @Query("DELETE FROM music_tracks WHERE volumeName LIKE 'saf:%' AND volumeName != :keepVolumeName")
    suspend fun deleteOtherSafTrees(keepVolumeName: String)
}


data class TrackFingerprint(
    val contentUri: String,
    val dateModifiedSeconds: Long,
    val sizeBytes: Long
)
