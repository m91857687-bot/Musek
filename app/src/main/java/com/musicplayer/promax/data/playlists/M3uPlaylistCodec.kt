package com.musicplayer.promax.data.playlists

import android.net.Uri
import com.musicplayer.promax.data.local.MusicTrackEntity

object M3uPlaylistCodec {
    fun export(playlistName: String, tracks: List<MusicTrackEntity>): String {
        val sb = StringBuilder()
        sb.append("#EXTM3U\n")
        if (playlistName.isNotBlank()) {
            sb.append("#PLAYLIST:").append(playlistName.replace('\n', ' ').trim()).append('\n')
        }
        tracks.forEach { track ->
            val durationSec = (track.durationMs / 1000L).coerceAtLeast(-1L)
            val artist = track.displayArtist.ifBlank { "Unknown Artist" }.replace('\n', ' ').trim()
            val title = track.displayTitle.replace('\n', ' ').trim()
            sb.append("#EXTINF:").append(durationSec).append(',').append(artist).append(" - ").append(title).append('\n')
            sb.append(track.contentUri).append('\n')
        }
        return sb.toString()
    }

    fun extractPlaylistTitle(m3uText: String): String? {
        m3uText.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.startsWith("#PLAYLIST:", ignoreCase = true)) {
                return line.substringAfter(':').trim().takeIf { it.isNotEmpty() }
            }
        }
        return null
    }

    fun matchImportedTracks(
        m3uText: String,
        libraryTracks: List<MusicTrackEntity>
    ): List<MusicTrackEntity> {
        if (libraryTracks.isEmpty() || m3uText.isBlank()) return emptyList()

        val byUri = libraryTracks.associateBy { it.contentUri.trim() }
        val byNormalizedTitleArtist = libraryTracks.associateBy {
            normalize("${it.displayArtist} - ${it.displayTitle}")
        }
        val byNormalizedTitle = libraryTracks.associateBy { normalize(it.displayTitle) }

        val matched = LinkedHashMap<String, MusicTrackEntity>()
        var pendingExtInfTitle: String? = null

        m3uText.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) return@forEach
            if (line.startsWith("#EXTINF:", ignoreCase = true)) {
                pendingExtInfTitle = line.substringAfter(',', "").trim().takeIf { it.isNotEmpty() }
                return@forEach
            }
            if (line.startsWith("#")) return@forEach

            var found: MusicTrackEntity? = byUri[line]

            if (found == null) {
                val fileName = extractFileName(line)
                if (fileName.isNotEmpty()) {
                    val stem = fileName.substringBeforeLast('.')
                    found = libraryTracks.firstOrNull { track ->
                        track.contentUri.endsWith(fileName, ignoreCase = true) ||
                            track.displayTitle.equals(stem, ignoreCase = true) ||
                            normalize(track.displayTitle) == normalize(stem)
                    }
                }
            }

            if (found == null && pendingExtInfTitle != null) {
                val normExt = normalize(pendingExtInfTitle!!)
                found = byNormalizedTitleArtist[normExt]
                    ?: byNormalizedTitle[normExt]
                    ?: pendingExtInfTitle!!.substringAfter(" - ", "").takeIf { it.isNotBlank() }?.let { part ->
                        byNormalizedTitle[normalize(part)]
                    }
            }

            pendingExtInfTitle = null
            if (found != null) {
                matched[found.trackKey] = found
            }
        }
        return matched.values.toList()
    }

    private fun extractFileName(pathOrUri: String): String {
        val parsed = runCatching { Uri.parse(pathOrUri).lastPathSegment }.getOrNull()
        val candidate = parsed ?: pathOrUri
        return candidate.substringAfterLast('/').substringAfterLast('\\').trim()
    }

    private fun normalize(value: String): String =
        value.lowercase().replace(Regex("[^\\p{L}\\p{Nd}]+"), " ").trim()
}
