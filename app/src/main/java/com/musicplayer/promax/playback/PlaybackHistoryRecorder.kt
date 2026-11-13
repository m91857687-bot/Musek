package com.musicplayer.promax.playback

import android.os.SystemClock
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import com.musicplayer.promax.data.local.MusicLibraryDatabase
import com.musicplayer.promax.data.stats.PlaybackHistoryEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Records audible segments without blocking the playback thread. */
class PlaybackHistoryRecorder(
    private val database: MusicLibraryDatabase,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val elapsedMs: () -> Long = SystemClock::elapsedRealtime,
    private val wallMs: () -> Long = System::currentTimeMillis
) : Player.Listener {
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private var currentTrackKey: String? = null
    private var segmentStartedAtElapsed: Long? = null
    private var segmentStartedAtWall: Long = 0L
    private var countedThisVisit = false
    private var pendingPlayCount = false
    private var isCurrentlyPlaying = false

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        finishSegment()
        currentTrackKey = mediaItem?.mediaId?.takeIf(String::isNotBlank)
        countedThisVisit = false
        pendingPlayCount = false
        if (mediaItem != null && isCurrentlyPlaying) {
            beginSegmentIfPlaying()
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        isCurrentlyPlaying = isPlaying
        if (isPlaying) beginSegmentIfPlaying() else finishSegment()
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == Player.STATE_ENDED || playbackState == Player.STATE_IDLE) {
            isCurrentlyPlaying = false
            finishSegment()
        }
    }

    private fun beginSegmentIfPlaying() {
        if (segmentStartedAtElapsed != null) return
        val key = currentTrackKey ?: return
        segmentStartedAtElapsed = elapsedMs()
        segmentStartedAtWall = wallMs()
        if (!countedThisVisit) {
            countedThisVisit = true
            pendingPlayCount = true
        }
    }

    private fun finishSegment() {
        val started = segmentStartedAtElapsed ?: return
        val key = currentTrackKey
        segmentStartedAtElapsed = null
        val duration = (elapsedMs() - started).coerceAtLeast(0L)
        if (key.isNullOrBlank() || duration <= 0L) return
        // The visit is counted on its first audible segment only.
        val shouldCount = pendingPlayCount
        pendingPlayCount = false
        val playedAt = segmentStartedAtWall
        val nowWall = wallMs()
        scope.launch {
            val dao = database.playbackStatsDao()
            dao.insertSegment(
                PlaybackHistoryEntity(
                    trackKey = key,
                    playedAt = playedAt,
                    listenedMs = duration,
                    playCount = if (shouldCount) 1 else 0
                )
            )
            dao.pruneBefore(nowWall - RETENTION_DAYS * DAY_MS)
        }
    }

    fun onInitialPlaybackState(player: Player) {
        currentTrackKey = player.currentMediaItem?.mediaId?.takeIf(String::isNotBlank)
        isCurrentlyPlaying = player.isPlaying
        if (isCurrentlyPlaying) beginSegmentIfPlaying()
    }

    override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) = Unit

    fun release() {
        isCurrentlyPlaying = false
        finishSegment()
    }

    companion object {
        private const val RETENTION_DAYS = 365L
        private const val DAY_MS = 86_400_000L
    }
}
