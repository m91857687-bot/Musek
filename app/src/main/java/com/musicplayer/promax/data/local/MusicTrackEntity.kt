package com.musicplayer.promax.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "music_tracks",
    indices = [
        Index(value = ["volumeName", "mediaStoreId"]),
        Index(value = ["title"]),
        Index(value = ["artist"]),
        Index(value = ["album"])
    ]
)
data class MusicTrackEntity(
    @PrimaryKey val trackKey: String,
    val mediaStoreId: Long,
    val volumeName: String,
    val contentUri: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val dateModifiedSeconds: Long,
    val mimeType: String?,
    val relativePath: String?,
    val titleOverride: String? = null,
    val artistOverride: String? = null,
    val albumOverride: String? = null,
    val artworkFilePath: String? = null,
    val lyricsLrc: String? = null
) {
    val displayTitle: String get() = titleOverride?.takeIf(String::isNotBlank) ?: title
    val displayArtist: String get() = artistOverride?.takeIf(String::isNotBlank) ?: artist
    val displayAlbum: String get() = albumOverride?.takeIf(String::isNotBlank) ?: album
}
