package com.musicplayer.promax.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.RemoteViews
import androidx.media3.session.MediaController
import androidx.core.content.ContextCompat
import com.musicplayer.promax.MainActivity
import com.musicplayer.promax.R
import com.musicplayer.promax.data.metadata.ArtworkSampling
import com.musicplayer.promax.playback.PlaybackController
import java.io.File

class MusicPlayerWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateOne(context, manager, it, context.getString(R.string.app_name), context.getString(R.string.widget_open_hint)) }
    }

    companion object {
        const val ACTION_PLAY_PAUSE = "com.musicplayer.promax.widget.PLAY_PAUSE"
        const val ACTION_NEXT = "com.musicplayer.promax.widget.NEXT"

        fun refresh(context: Context, title: String, artist: String, isPlaying: Boolean, artworkPath: String? = null) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, MusicPlayerWidgetProvider::class.java)
            manager.getAppWidgetIds(component).forEach { updateOne(context, manager, it, title, artist, isPlaying, artworkPath) }
        }

        private fun updateOne(
            context: Context,
            manager: AppWidgetManager,
            id: Int,
            title: String,
            artist: String,
            isPlaying: Boolean = false,
            artworkPath: String? = null
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_music_player)
            views.setTextViewText(R.id.widget_track_title, title)
            views.setTextViewText(R.id.widget_track_artist, artist)
            views.setTextViewText(R.id.widget_play_pause, if (isPlaying) "⏸" else "▶")
            views.setContentDescription(R.id.widget_play_pause, context.getString(R.string.accessibility_play_pause))
            views.setContentDescription(R.id.widget_next, context.getString(R.string.accessibility_next_track))
            val widgetBitmap = decodeWidgetArtwork(artworkPath)
            if (widgetBitmap != null) {
                views.setImageViewBitmap(R.id.widget_artwork, widgetBitmap)
            } else {
                views.setImageViewResource(R.id.widget_artwork, R.mipmap.ic_launcher)
            }
            val launchActivityIntent = Intent(context, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_OPEN_NOW_PLAYING, true)
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openIntent = PendingIntent.getActivity(
                context, 1, launchActivityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val playIntent = PendingIntent.getBroadcast(
                context, 2, Intent(context, MusicWidgetActionReceiver::class.java).setAction(ACTION_PLAY_PAUSE),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val nextIntent = PendingIntent.getBroadcast(
                context, 3, Intent(context, MusicWidgetActionReceiver::class.java).setAction(ACTION_NEXT),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, openIntent)
            views.setOnClickPendingIntent(R.id.widget_artwork, openIntent)
            views.setOnClickPendingIntent(R.id.widget_play_pause, playIntent)
            views.setOnClickPendingIntent(R.id.widget_next, nextIntent)
            manager.updateAppWidget(id, views)
        }

        private fun decodeWidgetArtwork(artworkPath: String?): Bitmap? = runCatching {
            val file = artworkPath?.takeIf(String::isNotBlank)?.let(::File)?.takeIf(File::isFile) ?: return@runCatching null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
            val opts = BitmapFactory.Options().apply {
                inSampleSize = ArtworkSampling.calculateSampleSize(bounds.outWidth, bounds.outHeight, 192)
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            BitmapFactory.decodeFile(file.absolutePath, opts)
        }.getOrNull()
    }
}

class MusicWidgetActionReceiver : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != MusicPlayerWidgetProvider.ACTION_PLAY_PAUSE && intent.action != MusicPlayerWidgetProvider.ACTION_NEXT) return
        val pendingResult = goAsync()
        val future = PlaybackController.connect(context)
        future.addListener({
            try {
                val controller: MediaController = future.get()
                when (intent.action) {
                    MusicPlayerWidgetProvider.ACTION_PLAY_PAUSE -> if (controller.isPlaying) controller.pause() else controller.play()
                    MusicPlayerWidgetProvider.ACTION_NEXT -> controller.seekToNext()
                }
            } catch (_: Exception) {
                // The playback service may not be available; the widget remains usable to reopen the app.
            } finally {
                MediaController.releaseFuture(future)
                pendingResult.finish()
            }
        }, ContextCompat.getMainExecutor(context))
    }
}
