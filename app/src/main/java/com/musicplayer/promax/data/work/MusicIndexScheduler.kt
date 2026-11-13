package com.musicplayer.promax.data.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object MusicIndexScheduler {
    fun enqueue(context: Context) {
        val request = OneTimeWorkRequestBuilder<MusicIndexWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            MusicIndexWorker.UNIQUE_WORK_NAME,
            // A scan request can arrive while another scan is running (for example,
            // after a MediaStore change or a source-setting update). Queue a
            // follow-up instead of silently dropping that request.
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request
        )
    }

    fun ensurePeriodicUpdates(context: Context) {
        val request = PeriodicWorkRequestBuilder<MusicIndexWorker>(12, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "music_library_periodic_update",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
