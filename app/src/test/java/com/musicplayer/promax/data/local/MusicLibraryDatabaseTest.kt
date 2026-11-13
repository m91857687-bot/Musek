package com.musicplayer.promax.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.musicplayer.promax.data.stats.PlaybackHistoryEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MusicLibraryDatabaseTest {
    private lateinit var context: Context
    private lateinit var db: MusicLibraryDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, MusicLibraryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun deleteTracksHandlesMoreThan999KeysWithoutSqliteVariableError() = runTest {
        val dao = db.musicTrackDao()
        val tracks = (1..1200).map { i ->
            MusicTrackEntity(
                trackKey = "primary:$i",
                mediaStoreId = i.toLong(),
                volumeName = "primary",
                contentUri = "content://media/external/audio/media/$i",
                title = "Track $i",
                artist = "Artist",
                album = "Album",
                durationMs = 180_000L,
                sizeBytes = 4_000_000L,
                dateModifiedSeconds = 1_700_000_000L,
                mimeType = "audio/mpeg",
                relativePath = "Music/"
            )
        }
        dao.upsertTracks(tracks)
        assertEquals(1200, dao.observeTrackCount().first())

        dao.deleteTracks(tracks.map { it.trackKey })
        assertEquals(0, dao.observeTrackCount().first())
    }

    @Test
    fun playlistTrackCountExcludesDeletedTracksAndRejectsCaseInsensitiveDuplicates() = runTest {
        val trackDao = db.musicTrackDao()
        val playlistDao = db.playlistDao()

        val track1 = MusicTrackEntity("v:1", 1L, "v", "content://1", "Song 1", "Artist A", "Album A", 120_000L, 1000L, 100L, "audio/mpeg", null)
        val track2 = MusicTrackEntity("v:2", 2L, "v", "content://2", "Song 2", "Artist B", "Album B", 150_000L, 1000L, 100L, "audio/mpeg", null)
        trackDao.upsertTracks(listOf(track1, track2))

        assertTrue(playlistDao.createPlaylistIfNameAvailable(PlaylistEntity(name = "Workout", createdAt = 10L, updatedAt = 10L)))
        assertFalse(playlistDao.createPlaylistIfNameAvailable(PlaylistEntity(name = "workout", createdAt = 20L, updatedAt = 20L)))

        val playlistId = playlistDao.observePlaylists().first().first().playlist.playlistId
        playlistDao.addTrack(playlistId, "v:1", 100L)
        playlistDao.addTrack(playlistId, "v:2", 101L)
        assertEquals(2, playlistDao.observePlaylists().first().first().trackCount)

        // Removing track2 from catalog should not show inflated count on playlist card
        trackDao.deleteTracks(listOf("v:2"))
        assertEquals(1, playlistDao.observePlaylistTracks(playlistId).first().size)
        assertEquals(1, playlistDao.observePlaylists().first().first().trackCount)
    }

    @Test
    fun statsAndRecentTracksRespect365DayWindow() = runTest {
        val trackDao = db.musicTrackDao()
        val statsDao = db.playbackStatsDao()
        val now = System.currentTimeMillis()
        val overYearAgo = now - 400L * 86_400_000L

        trackDao.upsertTracks(
            listOf(
                MusicTrackEntity("v:1", 1L, "v", "content://1", "Recent Song", "Artist", "Album", 100_000L, 1000L, 100L, "audio/mpeg", null),
                MusicTrackEntity("v:2", 2L, "v", "content://2", "Old Song", "Artist", "Album", 100_000L, 1000L, 100L, "audio/mpeg", null)
            )
        )
        statsDao.insertSegment(PlaybackHistoryEntity(trackKey = "v:2", playedAt = overYearAgo, listenedMs = 60_000L, playCount = 1))
        statsDao.insertSegment(PlaybackHistoryEntity(trackKey = "v:1", playedAt = now - 10_000L, listenedMs = 30_000L, playCount = 1))
        statsDao.insertSegment(PlaybackHistoryEntity(trackKey = "v:1", playedAt = now - 5_000L, listenedMs = 15_000L, playCount = 0))

        assertEquals(1L, statsDao.observeTotalPlays().first())
        assertEquals(45_000L, statsDao.observeListeningTime().first())
        assertEquals(1, statsDao.observeUniqueTracks().first())
        val recent = statsDao.observeRecentTracks().first()
        assertEquals(listOf("v:1"), recent.map { it.trackKey })
    }

    @Test
    fun migrationsFromVersion1To4PreserveTracksAndOpenCleanly() = runTest {
        val dbFile = File(context.cacheDir, "migration_test_v1_v4.db")
        if (dbFile.exists()) dbFile.delete()

        // Create version 1 schema manually matching initial MusicTrackEntity & ScanCheckpointEntity
        SQLiteDatabase.openOrCreateDatabase(dbFile, null).use { v1 ->
            v1.execSQL(
                "CREATE TABLE IF NOT EXISTS `music_tracks` (`trackKey` TEXT NOT NULL, `mediaStoreId` INTEGER NOT NULL, `volumeName` TEXT NOT NULL, `contentUri` TEXT NOT NULL, `title` TEXT NOT NULL, `artist` TEXT NOT NULL, `album` TEXT NOT NULL, `durationMs` INTEGER NOT NULL, `sizeBytes` INTEGER NOT NULL, `dateModifiedSeconds` INTEGER NOT NULL, `mimeType` TEXT, `relativePath` TEXT, PRIMARY KEY(`trackKey`))"
            )
            v1.execSQL("CREATE INDEX IF NOT EXISTS `index_music_tracks_volumeName_mediaStoreId` ON `music_tracks` (`volumeName`, `mediaStoreId`)")
            v1.execSQL("CREATE INDEX IF NOT EXISTS `index_music_tracks_title` ON `music_tracks` (`title`)")
            v1.execSQL("CREATE INDEX IF NOT EXISTS `index_music_tracks_artist` ON `music_tracks` (`artist`)")
            v1.execSQL("CREATE INDEX IF NOT EXISTS `index_music_tracks_album` ON `music_tracks` (`album`)")
            v1.execSQL(
                "CREATE TABLE IF NOT EXISTS `scan_checkpoints` (`volumeName` TEXT NOT NULL, `lastSuccessfulScanSeconds` INTEGER NOT NULL, `lastFullReconciliationSeconds` INTEGER NOT NULL, PRIMARY KEY(`volumeName`))"
            )
            v1.execSQL(
                "INSERT INTO `music_tracks` VALUES ('v:42', 42, 'v', 'content://42', 'Preserved Song', 'Preserved Artist', 'Preserved Album', 210000, 5000, 1000, 'audio/mpeg', 'Music/')"
            )
            v1.version = 1
        }

        val migrated = Room.databaseBuilder(context, MusicLibraryDatabase::class.java, dbFile.absolutePath)
            .addMigrations(
                MusicLibraryDatabase.MIGRATION_1_2,
                MusicLibraryDatabase.MIGRATION_2_3,
                MusicLibraryDatabase.MIGRATION_3_4
            )
            .allowMainThreadQueries()
            .build()
        try {
            val track = migrated.musicTrackDao().getTrack("v:42")
            assertEquals("Preserved Song", track?.title)
            assertEquals(null, track?.titleOverride)
        } finally {
            migrated.close()
            dbFile.delete()
        }
    }

    @Test
    fun observeLibraryTracksSearchMatchesOverridesAndIgnoresEmptyOverrides() = runTest {
        val dao = db.musicTrackDao()
        dao.upsertTracks(
            listOf(
                MusicTrackEntity(
                    trackKey = "v:1",
                    mediaStoreId = 1L,
                    volumeName = "v",
                    contentUri = "content://1",
                    title = "Midnight Echo",
                    artist = "Aria",
                    album = "Skyline",
                    durationMs = 180_000L,
                    sizeBytes = 3_000L,
                    dateModifiedSeconds = 100L,
                    mimeType = "audio/mpeg",
                    relativePath = null,
                    titleOverride = ""
                ),
                MusicTrackEntity(
                    trackKey = "v:2",
                    mediaStoreId = 2L,
                    volumeName = "v",
                    contentUri = "content://2",
                    title = "Track 02",
                    artist = "Unknown",
                    album = "Demos",
                    durationMs = 200_000L,
                    sizeBytes = 4_000L,
                    dateModifiedSeconds = 200L,
                    mimeType = "audio/mpeg",
                    relativePath = null,
                    titleOverride = "Desert Breeze"
                )
            )
        )

        val matchByOriginalWithEmptyOverride = dao.observeLibraryTracks("midnight", "TITLE", false).first()
        assertEquals(listOf("v:1"), matchByOriginalWithEmptyOverride.map { it.trackKey })

        val matchByOverride = dao.observeLibraryTracks("breeze", "TITLE", false).first()
        assertEquals(listOf("v:2"), matchByOverride.map { it.trackKey })

        val matchByArtist = dao.observeLibraryTracks("aria", "TITLE", false).first()
        assertEquals(listOf("v:1"), matchByArtist.map { it.trackKey })
    }

    @Test
    fun tracksMissingArtworkOnlyReturnsNullArtworkTracks() = runTest {
        val dao = db.musicTrackDao()
        dao.upsertTracks(
            listOf(
                MusicTrackEntity("v:1", 1L, "v", "content://1", "Song 1", "A", "B", 100_000L, 1000L, 100L, "audio/mpeg", null, artworkFilePath = null),
                MusicTrackEntity("v:2", 2L, "v", "content://2", "Song 2", "A", "B", 100_000L, 1000L, 100L, "audio/mpeg", null, artworkFilePath = ""),
                MusicTrackEntity("v:3", 3L, "v", "content://3", "Song 3", "A", "B", 100_000L, 1000L, 100L, "audio/mpeg", null, artworkFilePath = "/tmp/cover.jpg")
            )
        )

        val missing = dao.tracksMissingArtwork(50)
        assertEquals(listOf("v:1"), missing.map { it.trackKey })
    }

    @Test
    fun observePlaylistIdsForTrackReturnsAllContainingPlaylists() = runTest {
        val trackDao = db.musicTrackDao()
        val playlistDao = db.playlistDao()
        trackDao.upsertTracks(
            listOf(MusicTrackEntity("v:1", 1L, "v", "content://1", "Song 1", "A", "B", 100_000L, 1000L, 100L, "audio/mpeg", null))
        )
        playlistDao.createPlaylistIfNameAvailable(PlaylistEntity(name = "Chill", createdAt = 1L, updatedAt = 1L))
        playlistDao.createPlaylistIfNameAvailable(PlaylistEntity(name = "Drive", createdAt = 2L, updatedAt = 2L))
        playlistDao.createPlaylistIfNameAvailable(PlaylistEntity(name = "Focus", createdAt = 3L, updatedAt = 3L))

        val playlists = playlistDao.observePlaylists().first().associateBy({ it.playlist.name }, { it.playlist.playlistId })
        val chillId = playlists.getValue("Chill")
        val driveId = playlists.getValue("Drive")

        playlistDao.addTrack(chillId, "v:1", 10L)
        playlistDao.addTrack(driveId, "v:1", 11L)

        val containing = playlistDao.observePlaylistIdsForTrack("v:1").first().toSet()
        assertEquals(setOf(chillId, driveId), containing)

        playlistDao.removeTrack(chillId, "v:1", 12L)
        assertEquals(setOf(driveId), playlistDao.observePlaylistIdsForTrack("v:1").first().toSet())
    }
}
