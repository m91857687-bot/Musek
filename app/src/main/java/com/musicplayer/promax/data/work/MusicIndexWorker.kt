package com.musicplayer.promax.data.work

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.musicplayer.promax.data.local.MusicLibraryDatabase
import com.musicplayer.promax.data.media.MediaStoreMusicScanner
import com.musicplayer.promax.data.metadata.TrackMetadataRepository
import com.musicplayer.promax.data.preferences.DataStoreUserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CancellationException

class MusicIndexWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val preferences = DataStoreUserPreferencesRepository(applicationContext)
        val internalEnabled = preferences.internalMusicEnabled.first()
        val externalEnabled = preferences.externalMusicEnabled.first()
        val externalTree = preferences.externalTreeUri.first()
        val database = MusicLibraryDatabase.getInstance(applicationContext)
        if (!internalEnabled && !externalEnabled) {
            database.musicTrackDao().deleteAllTracks()
            return Result.success()
        }

        val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE
        val hasMediaPermission = ContextCompat.checkSelfPermission(applicationContext, permission) == PackageManager.PERMISSION_GRANTED
        val scanner = MediaStoreMusicScanner(applicationContext, database)
        try {
            var total = 0
            val volumes = MediaStoreMusicScanner.availableVolumeNames(applicationContext)
            val primary = MediaStoreMusicScanner.primaryVolumeName()
            if (!internalEnabled) database.musicTrackDao().deleteVolume(primary)
            val indexedVolumes = database.musicTrackDao().allVolumeNames()
            if (!externalEnabled) {
                indexedVolumes.filter { it != primary }.forEach { database.musicTrackDao().deleteVolume(it) }
                database.musicTrackDao().deleteSafTracks()
            } else {
                // Remove cached entries for removable volumes that are no longer mounted.
                indexedVolumes.filter { it != primary && !it.startsWith("saf:") && it !in volumes }
                    .forEach { database.musicTrackDao().deleteVolume(it) }
            }
            if (hasMediaPermission) {
                if (internalEnabled && primary in volumes) {
                    val legacyPrefix = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                        MediaStoreMusicScanner.legacyPrimaryPathPrefix()
                    } else null
                    total += scanner.scanVolume(primary, fullMetadataScan = false, legacyPathPrefix = legacyPrefix)
                }
                if (externalEnabled) {
                    val secondaryVolumes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) volumes - primary else emptySet()
                    secondaryVolumes.forEach { volume ->
                        total += scanner.scanVolume(volume, fullMetadataScan = false)
                    }
                    // On Android 10+, SAF is a fallback if the removable volume is not exposed.
                    // On Android 8/9, SAF is the explicit SD-card route because MediaStore uses one aggregate volume.
                    if ((secondaryVolumes.isEmpty() || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) && !externalTree.isNullOrBlank()) {
                        total += scanner.scanSafTree(Uri.parse(externalTree))
                    }
                }
            } else if (externalEnabled && !externalTree.isNullOrBlank()) {
                // A user-selected SAF tree has its own persisted permission and does not require READ_MEDIA_AUDIO.
                total += scanner.scanSafTree(Uri.parse(externalTree))
            } else {
                return Result.failure(androidx.work.workDataOf(KEY_ERROR to "audio_permission_required"))
            }
            setProgress(androidx.work.workDataOf(KEY_TRACK_COUNT to total))
            TrackMetadataRepository(applicationContext).prefetchMissingArtwork()
            return Result.success(androidx.work.workDataOf(KEY_TRACK_COUNT to total))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (security: SecurityException) {
            return Result.failure(androidx.work.workDataOf(KEY_ERROR to "permission_lost"))
        } catch (error: Exception) {
            return if (runAttemptCount < 3) Result.retry() else Result.failure(
                androidx.work.workDataOf(KEY_ERROR to (error.message ?: "indexing_failed"))
            )
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "music_library_index"
        const val KEY_TRACK_COUNT = "indexed_track_count"
        const val KEY_ERROR = "index_error"
    }
}
