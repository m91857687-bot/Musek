package com.musicplayer.promax.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.musicplayer.promax.data.local.MusicTrackEntity
import java.io.File

data class QueueItem(
    val index: Int,
    val mediaId: String,
    val title: String,
    val artist: String,
    val artworkPath: String?,
    val isCurrent: Boolean
)

/** Small factory/helper for connecting the UI to the service-owned MediaSession. */
object PlaybackController {
    fun connect(context: Context): ListenableFuture<MediaController> {
        val token = SessionToken(context, ComponentName(context, MusicPlaybackService::class.java))
        return MediaController.Builder(context, token).buildAsync()
    }

    fun playQueue(controller: Player, tracks: List<MusicTrackEntity>, selected: MusicTrackEntity) {
        val selectedIndex = tracks.indexOfFirst { it.trackKey == selected.trackKey }
        if (selectedIndex < 0) return
        val items = tracks.map(::toMediaItem)
        controller.setMediaItems(items, selectedIndex, 0L)
        controller.prepare()
        controller.play()
    }

    fun playNext(controller: Player, track: MusicTrackEntity) {
        val item = toMediaItem(track)
        if (controller.mediaItemCount == 0) {
            controller.setMediaItems(listOf(item), 0, 0L)
            controller.prepare()
            controller.play()
            return
        }
        val currentIdx = controller.currentMediaItemIndex.coerceAtLeast(0)
        // If already present later in the queue, move it right after current; if before current, avoid shifting current.
        var existingAfterIdx = -1
        for (i in 0 until controller.mediaItemCount) {
            if (i != currentIdx && controller.getMediaItemAt(i).mediaId == track.trackKey) {
                existingAfterIdx = i
                break
            }
        }
        val targetIndex = (currentIdx + 1).coerceAtMost(controller.mediaItemCount)
        if (existingAfterIdx >= 0) {
            val adjustedTarget = if (existingAfterIdx < currentIdx) currentIdx else targetIndex
            controller.moveMediaItem(existingAfterIdx, adjustedTarget)
        } else {
            controller.addMediaItem(targetIndex, item)
        }
    }

    fun readQueue(controller: Player): List<QueueItem> {
        val count = controller.mediaItemCount
        if (count <= 0) return emptyList()
        val currentIndex = controller.currentMediaItemIndex
        return (0 until count).map { idx ->
            val item = controller.getMediaItemAt(idx)
            val md = item.mediaMetadata
            QueueItem(
                index = idx,
                mediaId = item.mediaId,
                title = md.title?.toString().orEmpty(),
                artist = md.artist?.toString().orEmpty(),
                artworkPath = md.artworkUri?.path,
                isCurrent = idx == currentIndex
            )
        }
    }

    fun moveQueueItem(controller: Player, fromIndex: Int, toIndex: Int) {
        val count = controller.mediaItemCount
        if (fromIndex !in 0 until count || toIndex !in 0 until count || fromIndex == toIndex) return
        controller.moveMediaItem(fromIndex, toIndex)
    }

    fun removeQueueItem(controller: Player, index: Int) {
        if (index !in 0 until controller.mediaItemCount) return
        controller.removeMediaItem(index)
    }

    fun jumpToQueueIndex(controller: Player, index: Int) {
        if (index !in 0 until controller.mediaItemCount) return
        controller.seekToDefaultPosition(index)
        controller.play()
    }

    fun refreshTrackMetadata(controller: Player, track: MusicTrackEntity) {
        val count = controller.mediaItemCount
        for (index in 0 until count) {
            val item = controller.getMediaItemAt(index)
            if (item.mediaId == track.trackKey) {
                val updated = item.buildUpon()
                    .setMediaMetadata(buildMetadata(track))
                    .build()
                controller.replaceMediaItem(index, updated)
            }
        }
    }

    private fun toMediaItem(track: MusicTrackEntity): MediaItem = MediaItem.Builder()
        .setMediaId(track.trackKey)
        .setUri(Uri.parse(track.contentUri))
        .setMediaMetadata(buildMetadata(track))
        .build()

    private fun buildMetadata(track: MusicTrackEntity): MediaMetadata = MediaMetadata.Builder()
        .setTitle(track.displayTitle)
        .setArtist(track.displayArtist)
        .setAlbumTitle(track.displayAlbum)
        .apply {
            track.artworkFilePath?.takeIf { File(it).isFile }?.let { setArtworkUri(Uri.fromFile(File(it))) }
        }
        .build()
}
