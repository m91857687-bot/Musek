package com.musicplayer.promax.data.stats

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** One continuous audible segment. playCount is 1 only for the first segment of a track visit. */
@Entity(
    tableName = "playback_history",
    indices = [Index("trackKey"), Index("playedAt")]
)
data class PlaybackHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackKey: String,
    val playedAt: Long,
    val listenedMs: Long,
    val playCount: Int
)
