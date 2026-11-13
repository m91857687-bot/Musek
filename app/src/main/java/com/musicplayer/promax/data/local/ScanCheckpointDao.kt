package com.musicplayer.promax.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ScanCheckpointDao {
    @Query("SELECT * FROM scan_checkpoints WHERE volumeName = :volumeName LIMIT 1")
    suspend fun get(volumeName: String): ScanCheckpointEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(checkpoint: ScanCheckpointEntity)
}
