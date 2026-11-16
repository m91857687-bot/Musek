package com.musicplayer.promax

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.media3.session.MediaController
import com.google.common.util.concurrent.ListenableFuture
import com.musicplayer.promax.data.preferences.DataStoreUserPreferencesRepository
import com.musicplayer.promax.data.work.MusicIndexScheduler
import com.musicplayer.promax.playback.PlaybackController
import com.musicplayer.promax.presentation.library.MusicLibraryViewModel
import com.musicplayer.promax.presentation.settings.SettingsViewModel
import com.musicplayer.promax.presentation.statistics.StatisticsViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var mediaObserver: ContentObserver? = null
    private var pendingIndexRunnable: Runnable? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    @Volatile private var activityDestroyed = false
    @Volatile private var shakeToSkipEnabled = false
    @Volatile private var isResumedState = false
    private val playbackController = mutableStateOf<MediaController?>(null)
    private val openNowPlayingSignal = mutableIntStateOf(0)

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var lastShakeTriggerMs: Long = 0L

    private val shakeListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (!shakeToSkipEnabled) return
            val controller = playbackController.value ?: return
            if (!controller.isPlaying) return
            val x = event.values.getOrElse(0) { 0f }
            val y = event.values.getOrElse(1) { 0f }
            val z = event.values.getOrElse(2) { 0f }
            val netAcceleration = sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH
            val now = System.currentTimeMillis()
            if (netAcceleration > 14.5f && now - lastShakeTriggerMs > 1_200L) {
                lastShakeTriggerMs = now
                controller.seekToNextMediaItem()
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handleLaunchIntent(intent)

        val audioPermission = audioPermissionName()
        if (ContextCompat.checkSelfPermission(this, audioPermission) == PackageManager.PERMISSION_GRANTED) {
            MusicIndexScheduler.enqueue(applicationContext)
            MusicIndexScheduler.ensurePeriodicUpdates(applicationContext)
        }

        sensorManager = getSystemService(SENSOR_SERVICE) as? SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val preferences = DataStoreUserPreferencesRepository(applicationContext)
        lifecycleScope.launch {
            preferences.shakeToSkipEnabled.collectLatest { enabled ->
                shakeToSkipEnabled = enabled
                updateShakeSensorRegistration()
            }
        }

        val settingsViewModel = ViewModelProvider(
            this,
            SettingsViewModel.Factory(preferences)
        )[SettingsViewModel::class.java]
        val statisticsViewModel = ViewModelProvider(this, StatisticsViewModel.Factory(applicationContext))[StatisticsViewModel::class.java]
        val libraryViewModel = ViewModelProvider(
            this,
            MusicLibraryViewModel.Factory(applicationContext)
        )[MusicLibraryViewModel::class.java]

        controllerFuture = PlaybackController.connect(this).also { future ->
            future.addListener({
                if (activityDestroyed || isFinishing || isDestroyed) return@addListener
                runCatching { future.get() }.onSuccess { controller ->
                    if (!activityDestroyed && !isFinishing && !isDestroyed) {
                        playbackController.value = controller
                    } else {
                        controller.release()
                    }
                }
            }, mainExecutor)
        }

        setContent {
            MusicPlayerProMaxApp(
                settingsViewModel = settingsViewModel,
                musicLibraryViewModel = libraryViewModel,
                statisticsViewModel = statisticsViewModel,
                playbackController = playbackController.value,
                openNowPlayingSignal = openNowPlayingSignal.intValue
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLaunchIntent(intent)
    }

    private fun handleLaunchIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_NOW_PLAYING, false) == true) {
            intent.removeExtra(EXTRA_OPEN_NOW_PLAYING)
            openNowPlayingSignal.intValue += 1
        }
    }

    override fun onResume() {
        super.onResume()
        isResumedState = true
        updateShakeSensorRegistration()
        if (ContextCompat.checkSelfPermission(this, audioPermissionName()) == PackageManager.PERMISSION_GRANTED) {
            registerMusicObserver()
        }
    }

    override fun onPause() {
        isResumedState = false
        updateShakeSensorRegistration()
        unregisterMusicObserver()
        super.onPause()
    }

    override fun onDestroy() {
        activityDestroyed = true
        sensorManager?.unregisterListener(shakeListener)
        playbackController.value = null
        controllerFuture?.let(MediaController::releaseFuture)
        controllerFuture = null
        super.onDestroy()
    }

    private fun updateShakeSensorRegistration() {
        val mgr = sensorManager ?: return
        val sensor = accelerometer ?: return
        mgr.unregisterListener(shakeListener)
        if (isResumedState && shakeToSkipEnabled && !activityDestroyed) {
            mgr.registerListener(shakeListener, sensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    private fun audioPermissionName(): String = if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    private fun registerMusicObserver() {
        if (mediaObserver != null) return
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        mediaObserver = object : ContentObserver(mainHandler) {
            override fun onChange(selfChange: Boolean) {
                scheduleIncrementalIndex()
            }

            override fun onChange(selfChange: Boolean, uri: android.net.Uri?) {
                scheduleIncrementalIndex()
            }
        }
        contentResolver.registerContentObserver(collection, true, mediaObserver!!)
    }

    private fun scheduleIncrementalIndex() {
        pendingIndexRunnable?.let(mainHandler::removeCallbacks)
        val runnable = Runnable { MusicIndexScheduler.enqueue(applicationContext) }
        pendingIndexRunnable = runnable
        mainHandler.postDelayed(runnable, 1_500L)
    }

    private fun unregisterMusicObserver() {
        mediaObserver?.let(contentResolver::unregisterContentObserver)
        mediaObserver = null
        pendingIndexRunnable?.let(mainHandler::removeCallbacks)
        pendingIndexRunnable = null
    }

    companion object {
        const val EXTRA_OPEN_NOW_PLAYING = "com.musicplayer.promax.extra.OPEN_NOW_PLAYING"
    }
}
