package com.musicplayer.promax.playback

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.musicplayer.promax.MainActivity
import com.musicplayer.promax.data.local.MusicLibraryDatabase
import com.musicplayer.promax.data.preferences.DataStoreUserPreferencesRepository
import com.musicplayer.promax.domain.model.CustomEqProfileCodec
import com.musicplayer.promax.widget.MusicPlayerWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Owns the player independently of the Activity so audio can continue in the background. */
@OptIn(UnstableApi::class)
class MusicPlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private var audioEffectsManager: AudioEffectsManager? = null
    private var player: ExoPlayer? = null
    private var historyRecorder: PlaybackHistoryRecorder? = null
    private val stereoBalanceProcessor = StereoBalanceAudioProcessor()
    private var currentAudioSessionId: Int = C.AUDIO_SESSION_ID_UNSET
    private var crossfadeJob: Job? = null
    @Volatile private var configuredCrossfadeMs: Long = 2_000L
    @Volatile private var fadeInStartedAtMs: Long = 0L
    private lateinit var preferences: DataStoreUserPreferencesRepository
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val audioSessionListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) attachAudioEffectsIfNeeded()
            refreshWidget()
        }

        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            attachAudioEffectsIfNeeded(audioSessionId)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (configuredCrossfadeMs > 0L) {
                fadeInStartedAtMs = System.currentTimeMillis()
                player?.volume = 0.08f
            } else {
                player?.volume = 1.0f
            }
            refreshWidget()
        }

        override fun onEvents(player: Player, events: Player.Events) = refreshWidget()

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying && configuredCrossfadeMs > 0L) {
                startCrossfadeMonitor()
            } else if (!isPlaying) {
                player?.volume = 1.0f
            }
            refreshWidget()
        }
    }

    override fun onCreate() {
        super.onCreate()
        preferences = DataStoreUserPreferencesRepository(applicationContext)
        val renderersFactory = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink {
                return DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .setAudioProcessors(arrayOf(stereoBalanceProcessor))
                    .build()
            }
        }
        val activePlayer = ExoPlayer.Builder(this, renderersFactory).build().apply {
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

        serviceScope.launch {
            preferences.playbackSpeed.collectLatest { speed ->
                activePlayer.setPlaybackSpeed(speed)
            }
        }
        serviceScope.launch {
            preferences.skipSilenceEnabled.collectLatest { skipSilence ->
                activePlayer.skipSilenceEnabled = skipSilence
            }
        }
        serviceScope.launch {
            preferences.stereoBalance.collectLatest { balance ->
                stereoBalanceProcessor.setBalance(balance)
            }
        }
        serviceScope.launch {
            preferences.crossfadeSeconds.collectLatest { seconds ->
                configuredCrossfadeMs = (seconds.coerceIn(0, 10) * 1000L)
                if (configuredCrossfadeMs <= 0L) {
                    activePlayer.volume = 1.0f
                } else if (activePlayer.isPlaying) {
                    startCrossfadeMonitor()
                }
            }
        }
        serviceScope.launch {
            preferences.sleepTimerEndMillis.collectLatest { endMillis ->
                if (endMillis != null) {
                    while (true) {
                        val waitMillis = endMillis - System.currentTimeMillis()
                        if (waitMillis <= 0L) break
                        delay(waitMillis)
                    }
                    val active = player
                    if (active?.isPlaying == true) {
                        // Gentle fade-out before pausing on Sleep Timer completion
                        val steps = 12
                        for (step in steps downTo 1) {
                            if (player?.isPlaying != true) break
                            active.volume = (step.toFloat() / steps.toFloat()).coerceIn(0.05f, 1.0f)
                            delay(140L)
                        }
                        active.pause()
                        active.volume = 1.0f
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

    private fun startCrossfadeMonitor() {
        if (crossfadeJob?.isActive == true) return
        crossfadeJob = serviceScope.launch {
            while (true) {
                val activePlayer = player ?: break
                val crossfadeMs = configuredCrossfadeMs
                if (!activePlayer.isPlaying || crossfadeMs <= 0L) {
                    activePlayer.volume = 1.0f
                    break
                }
                val now = System.currentTimeMillis()
                val fadeInDuration = minOf(crossfadeMs, 2_500L)
                val fadeInElapsed = (now - fadeInStartedAtMs).coerceAtLeast(0L)
                val fadeInFactor = if (fadeInStartedAtMs > 0L && fadeInElapsed < fadeInDuration) {
                    (0.08f + 0.92f * (fadeInElapsed.toFloat() / fadeInDuration.toFloat())).coerceIn(0.08f, 1.0f)
                } else {
                    1.0f
                }

                val duration = activePlayer.duration
                val position = activePlayer.currentPosition
                val fadeOutFactor = if (duration != C.TIME_UNSET && duration > crossfadeMs * 2 && position >= 0L) {
                    val remaining = duration - position
                    if (remaining in 1L..crossfadeMs) {
                        (0.10f + 0.90f * (remaining.toFloat() / crossfadeMs.toFloat())).coerceIn(0.10f, 1.0f)
                    } else 1.0f
                } else 1.0f

                activePlayer.volume = minOf(fadeInFactor, fadeOutFactor)
                delay(60L)
            }
            crossfadeJob = null
        }
    }

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
        val created = runCatching { AudioEffectsManager(sessionId) }.getOrNull()
        audioEffectsManager = created
        if (created != null) {
            serviceScope.launch {
                val enabled = preferences.audioEffectsEnabled.first()
                val presetName = preferences.eqPresetName.first()
                val savedBandLevels = CustomEqProfileCodec.decodeBandLevels(preferences.eqBandLevelsMb.first())
                val bass = preferences.bassBoostStrength.first()
                val virtualizer = preferences.virtualizerStrength.first()
                val loudness = preferences.loudnessGainMb.first()
                if (presetName != null) {
                    runCatching { AudioEffectsManager.Preset.valueOf(presetName) }.getOrNull()?.let(created::applyPreset)
                } else if (savedBandLevels.isNotEmpty()) {
                    created.applyBandLevels(savedBandLevels)
                }
                created.setBassStrength(bass)
                created.setVirtualizerStrength(virtualizer)
                created.setLoudnessGain(loudness)
                created.setEnabled(enabled)
                AudioEffectsBridge.attach(created)
            }
        }
    }

    override fun onDestroy() {
        crossfadeJob?.cancel()
        crossfadeJob = null
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
