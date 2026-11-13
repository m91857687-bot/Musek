package com.musicplayer.promax.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import java.io.File
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.musicplayer.promax.data.local.MusicTrackEntity

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

    private fun toMediaItem(track: MusicTrackEntity): MediaItem = MediaItem.Builder()
        .setMediaId(track.trackKey)
        .setUri(Uri.parse(track.contentUri))
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(track.displayTitle)
                .setArtist(track.displayArtist)
                .setAlbumTitle(track.displayAlbum)
                .apply {
                    track.artworkFilePath?.takeIf { File(it).isFile }?.let { setArtworkUri(Uri.fromFile(File(it))) }
                }
                .build()
        )
        .build()
}
