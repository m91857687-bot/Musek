package com.musicplayer.promax.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_checkpoints")
data class ScanCheckpointEntity(
    @PrimaryKey val volumeName: String,
    val lastSuccessfulScanSeconds: Long,
    val lastFullReconciliationSeconds: Long
)
