package com.musicplayer.promax.data.stats

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackStatsDao {
    @Insert
    suspend fun insertSegment(segment: PlaybackHistoryEntity)

    @Query("DELETE FROM playback_history WHERE playedAt < :cutoffMillis")
    suspend fun pruneBefore(cutoffMillis: Long)

    @Query("SELECT COALESCE(SUM(playCount), 0) FROM playback_history WHERE playedAt >= (CAST(strftime('%s','now') AS INTEGER) * 1000 - 31536000000)")
    fun observeTotalPlays(): Flow<Long>

    @Query("SELECT COALESCE(SUM(listenedMs), 0) FROM playback_history WHERE playedAt >= (CAST(strftime('%s','now') AS INTEGER) * 1000 - 31536000000)")
    fun observeListeningTime(): Flow<Long>

    @Query("SELECT COUNT(DISTINCT trackKey) FROM playback_history WHERE playCount > 0 AND playedAt >= (CAST(strftime('%s','now') AS INTEGER) * 1000 - 31536000000)")
    fun observeUniqueTracks(): Flow<Int>

    @Query("""
        SELECT h.trackKey AS trackKey, COALESCE(NULLIF(t.titleOverride, ''), t.title, '') AS title,
               COALESCE(NULLIF(t.artistOverride, ''), t.artist, '') AS artist,
               SUM(h.playCount) AS plays, SUM(h.listenedMs) AS listenedMs
        FROM playback_history h LEFT JOIN music_tracks t ON t.trackKey = h.trackKey
        WHERE h.playedAt >= (CAST(strftime('%s','now') AS INTEGER) * 1000 - 31536000000)
        GROUP BY h.trackKey
        HAVING SUM(h.playCount) > 0
        ORDER BY plays DESC, listenedMs DESC
        LIMIT 10
    """)
    fun observeTopTracks(): Flow<List<TopTrackStat>>

    @Query("SELECT COUNT(*) FROM playback_history WHERE playedAt >= :sinceMillis AND playedAt >= (CAST(strftime('%s','now') AS INTEGER) * 1000 - 31536000000)")
    fun observeSessionsSince(sinceMillis: Long): Flow<Int>

    @Query("""
        SELECT t.* FROM music_tracks t
        INNER JOIN (
            SELECT trackKey, MAX(playedAt) AS lastPlayedAt
            FROM playback_history
            WHERE playCount > 0 AND playedAt >= (CAST(strftime('%s','now') AS INTEGER) * 1000 - 31536000000)
            GROUP BY trackKey
        ) h ON h.trackKey = t.trackKey
        ORDER BY h.lastPlayedAt DESC
        LIMIT :limit
    """)
    fun observeRecentTracks(limit: Int = 5): Flow<List<com.musicplayer.promax.data.local.MusicTrackEntity>>

    @Query("""
        SELECT t.* FROM music_tracks t
        INNER JOIN (
            SELECT trackKey, SUM(playCount) AS totalPlays, SUM(listenedMs) AS totalListened
            FROM playback_history
            WHERE playCount > 0 AND playedAt >= (CAST(strftime('%s','now') AS INTEGER) * 1000 - 31536000000)
            GROUP BY trackKey
        ) h ON h.trackKey = t.trackKey
        ORDER BY h.totalPlays DESC, h.totalListened DESC
        LIMIT :limit
    """)
    fun observeMostPlayedTracks(limit: Int = 30): Flow<List<com.musicplayer.promax.data.local.MusicTrackEntity>>
}

data class TopTrackStat(
    val trackKey: String,
    val title: String,
    val artist: String,
    val plays: Long,
    val listenedMs: Long
)
