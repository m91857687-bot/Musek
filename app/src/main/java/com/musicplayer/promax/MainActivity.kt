package com.musicplayer.promax

import android.Manifest
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.media3.session.MediaController
import com.musicplayer.promax.playback.PlaybackController
import androidx.compose.runtime.mutableStateOf
import com.google.common.util.concurrent.ListenableFuture
import com.musicplayer.promax.data.preferences.DataStoreUserPreferencesRepository
import com.musicplayer.promax.data.work.MusicIndexScheduler
import com.musicplayer.promax.presentation.library.MusicLibraryViewModel
import com.musicplayer.promax.presentation.settings.SettingsViewModel
import com.musicplayer.promax.presentation.statistics.StatisticsViewModel

class MainActivity : AppCompatActivity() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var mediaObserver: ContentObserver? = null
    private var pendingIndexRunnable: Runnable? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    @Volatile private var activityDestroyed = false
    private val playbackController = mutableStateOf<MediaController?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val audioPermission = audioPermissionName()
        if (ContextCompat.checkSelfPermission(this, audioPermission) == PackageManager.PERMISSION_GRANTED) {
            MusicIndexScheduler.enqueue(applicationContext)
            MusicIndexScheduler.ensurePeriodicUpdates(applicationContext)
        }

        val preferences = DataStoreUserPreferencesRepository(applicationContext)
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
                playbackController = playbackController.value
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this, audioPermissionName()) == PackageManager.PERMISSION_GRANTED) {
            registerMusicObserver()
        }
    }

    override fun onPause() {
        unregisterMusicObserver()
        super.onPause()
    }

    override fun onDestroy() {
        activityDestroyed = true
        playbackController.value = null
        controllerFuture?.let(MediaController::releaseFuture)
        controllerFuture = null
        super.onDestroy()
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
}
