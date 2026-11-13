package com.musicplayer.promax.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.musicplayer.promax.MainActivity
import com.musicplayer.promax.data.local.MusicLibraryDatabase
import com.musicplayer.promax.widget.MusicPlayerWidgetProvider
import com.musicplayer.promax.data.preferences.DataStoreUserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** Owns the player independently of the Activity so audio can continue in the background. */
class MusicPlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private var audioEffectsManager: AudioEffectsManager? = null
    private var player: ExoPlayer? = null
    private var historyRecorder: PlaybackHistoryRecorder? = null
    private var currentAudioSessionId: Int = C.AUDIO_SESSION_ID_UNSET
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val audioSessionListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) attachAudioEffectsIfNeeded()
            refreshWidget()
        }
        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            attachAudioEffectsIfNeeded(audioSessionId)
        }
        override fun onEvents(player: Player, events: Player.Events) = refreshWidget()
        override fun onIsPlayingChanged(isPlaying: Boolean) = refreshWidget()
    }

    override fun onCreate() {
        super.onCreate()
        val activePlayer = ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true // ExoPlayer manages audio focus and transient interruptions.
            )
            setHandleAudioBecomingNoisy(true)
            repeatMode = Player.REPEAT_MODE_OFF
        }
        player = activePlayer
        val preferences = DataStoreUserPreferencesRepository(applicationContext)
        serviceScope.launch {
            preferences.sleepTimerEndMillis.collectLatest { endMillis ->
                if (endMillis != null) {
                    while (true) {
                        val waitMillis = endMillis - System.currentTimeMillis()
                        if (waitMillis <= 0L) break
                        delay(waitMillis)
                    }
                    if (player?.isPlaying == true) {
                        player?.pause()
                    }
                    preferences.setSleepTimerEndMillis(null)
                }
            }
        }
        historyRecorder = PlaybackHistoryRecorder(MusicLibraryDatabase.getInstance(applicationContext)).also { recorder ->
            activePlayer.addListener(recorder)
            recorder.onInitialPlaybackState(activePlayer)
        }
        activePlayer.addListener(audioSessionListener)
        val sessionIntent = Intent(this, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_OPEN_NOW_PLAYING, true)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val sessionPendingIntent = PendingIntent.getActivity(
            this,
            1001,
            sessionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        mediaSession = MediaSession.Builder(this, activePlayer)
            .setSessionActivity(sessionPendingIntent)
            .build()
        attachAudioEffectsIfNeeded()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    private fun refreshWidget() {
        val activePlayer = player ?: return
        val metadata = activePlayer.currentMediaItem?.mediaMetadata
        MusicPlayerWidgetProvider.refresh(
            applicationContext,
            metadata?.title?.toString()?.takeIf(String::isNotBlank) ?: getString(com.musicplayer.promax.R.string.widget_no_track),
            metadata?.artist?.toString().orEmpty(),
            activePlayer.isPlaying,
            metadata?.artworkUri?.path
        )
    }

    private fun attachAudioEffectsIfNeeded(explicitSessionId: Int? = null) {
        val sessionId = explicitSessionId ?: player?.audioSessionId ?: return
        if (sessionId <= 0 || sessionId == C.AUDIO_SESSION_ID_UNSET) return
        if (audioEffectsManager != null && currentAudioSessionId == sessionId) return
        audioEffectsManager?.let { previous ->
            AudioEffectsBridge.detach(previous)
            previous.release()
        }
        currentAudioSessionId = sessionId
        audioEffectsManager = runCatching { AudioEffectsManager(sessionId) }.getOrNull()
        audioEffectsManager?.let(AudioEffectsBridge::attach)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        audioEffectsManager?.let { effects ->
            AudioEffectsBridge.detach(effects)
            effects.release()
        }
        audioEffectsManager = null
        historyRecorder?.let { recorder ->
            player?.removeListener(recorder)
            recorder.release()
        }
        historyRecorder = null
        player?.removeListener(audioSessionListener)
        mediaSession?.release()
        player?.release()
        player = null
        mediaSession = null
        super.onDestroy()
    }
}
