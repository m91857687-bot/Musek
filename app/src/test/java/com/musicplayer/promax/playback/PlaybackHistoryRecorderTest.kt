package com.musicplayer.promax.playback

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.musicplayer.promax.data.local.MusicLibraryDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PlaybackHistoryRecorderTest {
    private lateinit var db: MusicLibraryDatabase
    private val testDispatcher = StandardTestDispatcher()
    private var fakeElapsedMs = 1_000L
    private var fakeWallMs = 1_700_000_000_000L

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, MusicLibraryDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor(Runnable::run)
            .setTransactionExecutor(Runnable::run)
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun pausedTransitionDoesNotRecordFakePlaysOrListeningTime() = runTest(testDispatcher) {
        val recorder = PlaybackHistoryRecorder(
            database = db,
            ioDispatcher = testDispatcher,
            elapsedMs = { fakeElapsedMs },
            wallMs = { fakeWallMs }
        )

        // Transition to a track while paused
        recorder.onMediaItemTransition(MediaItem.Builder().setMediaId("track:1").build(), Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED)
        fakeElapsedMs += 30_000L
        fakeWallMs += 30_000L

        // Transition to another track while still paused
        recorder.onMediaItemTransition(MediaItem.Builder().setMediaId("track:2").build(), Player.MEDIA_ITEM_TRANSITION_REASON_SEEK)
        fakeElapsedMs += 20_000L
        fakeWallMs += 20_000L

        recorder.release()
        advanceUntilIdle()

        assertEquals(0L, db.playbackStatsDao().observeTotalPlays().first())
        assertEquals(0L, db.playbackStatsDao().observeListeningTime().first())
    }

    @Test
    fun playingPauseResumeAndAutoTransitionRecordSinglePlayPerVisitAndAccurateDuration() = runTest(testDispatcher) {
        val recorder = PlaybackHistoryRecorder(
            database = db,
            ioDispatcher = testDispatcher,
            elapsedMs = { fakeElapsedMs },
            wallMs = { fakeWallMs }
        )
        fakeWallMs = System.currentTimeMillis() - 100_000L

        recorder.onMediaItemTransition(MediaItem.Builder().setMediaId("track:1").build(), Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED)
        recorder.onIsPlayingChanged(true)
        fakeElapsedMs += 15_000L
        fakeWallMs += 15_000L

        // Pause for 10 seconds (must not count paused time)
        recorder.onIsPlayingChanged(false)
        fakeElapsedMs += 10_000L
        fakeWallMs += 10_000L

        // Resume for 5 seconds
        recorder.onIsPlayingChanged(true)
        fakeElapsedMs += 5_000L
        fakeWallMs += 5_000L

        // Auto-advance to track:2 while still playing
        recorder.onMediaItemTransition(MediaItem.Builder().setMediaId("track:2").build(), Player.MEDIA_ITEM_TRANSITION_REASON_AUTO)
        fakeElapsedMs += 10_000L
        fakeWallMs += 10_000L

        recorder.release()
        advanceUntilIdle()

        assertEquals(2L, db.playbackStatsDao().observeTotalPlays().first())
        assertEquals(30_000L, db.playbackStatsDao().observeListeningTime().first())
        assertEquals(2, db.playbackStatsDao().observeUniqueTracks().first())
    }
}
