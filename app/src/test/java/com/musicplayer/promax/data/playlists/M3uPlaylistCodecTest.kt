package com.musicplayer.promax.data.playlists

import com.musicplayer.promax.data.local.MusicTrackEntity
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class M3uPlaylistCodecTest {
    private val sampleLibrary = listOf(
        MusicTrackEntity(
            trackKey = "k1",
            mediaStoreId = 1L,
            volumeName = "external_primary",
            contentUri = "content://media/external/audio/media/1",
            title = "Desert Dawn",
            artist = "Fairuz",
            album = "Classics",
            durationMs = 210_000L,
            sizeBytes = 5_000_000L,
            dateModifiedSeconds = 100L,
            mimeType = "audio/mpeg",
            relativePath = "Music/"
        ),
        MusicTrackEntity(
            trackKey = "k2",
            mediaStoreId = 2L,
            volumeName = "external_primary",
            contentUri = "content://media/external/audio/media/2",
            title = "Ocean Breeze",
            artist = "Nadia",
            album = "Horizon",
            durationMs = 185_000L,
            sizeBytes = 4_200_000L,
            dateModifiedSeconds = 200L,
            mimeType = "audio/flac",
            relativePath = "Download/"
        )
    )

    @Test
    fun exportAndImport_matchesByUriAndExtInfFallback_withoutDuplicates() {
        val m3u = M3uPlaylistCodec.export("Evening Drive", sampleLibrary)
        assertTrue(m3u.startsWith("#EXTM3U"))
        assertEquals("Evening Drive", M3uPlaylistCodec.extractPlaylistTitle(m3u))

        val imported = M3uPlaylistCodec.matchImportedTracks(m3u, sampleLibrary)
        assertEquals(listOf("k1", "k2"), imported.map { it.trackKey })

        val externalM3u = """
            #EXTM3U
            #PLAYLIST:Custom Import
            #EXTINF:185,Nadia - Ocean Breeze
            /storage/emulated/0/Music/Ocean Breeze.flac
            #EXTINF:210,Fairuz - Desert Dawn
            /storage/emulated/0/Music/Desert Dawn.mp3
            #EXTINF:210,Fairuz - Desert Dawn
            /storage/emulated/0/Music/Desert Dawn.mp3
        """.trimIndent()
        val matchedExternal = M3uPlaylistCodec.matchImportedTracks(externalM3u, sampleLibrary)
        assertEquals(listOf("k2", "k1"), matchedExternal.map { it.trackKey })
    }
}
