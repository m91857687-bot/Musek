package com.musicplayer.promax.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.musicplayer.promax.data.stats.PlaybackHistoryEntity
import com.musicplayer.promax.data.stats.PlaybackStatsDao

@Database(
    entities = [
        MusicTrackEntity::class,
        ScanCheckpointEntity::class,
        PlaylistEntity::class,
        PlaylistTrackEntity::class,
        FavoriteTrackEntity::class,
        PlaybackHistoryEntity::class
    ],
    version = 4,
    exportSchema = true
)
abstract class MusicLibraryDatabase : RoomDatabase() {
    abstract fun musicTrackDao(): MusicTrackDao
    abstract fun scanCheckpointDao(): ScanCheckpointDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun playbackStatsDao(): PlaybackStatsDao

    companion object {
        internal val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS playlists (playlistId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_playlists_name ON playlists(name)")
                db.execSQL("CREATE TABLE IF NOT EXISTS playlist_tracks (playlistId INTEGER NOT NULL, trackKey TEXT NOT NULL, position INTEGER NOT NULL, addedAt INTEGER NOT NULL, PRIMARY KEY(playlistId, trackKey), FOREIGN KEY(playlistId) REFERENCES playlists(playlistId) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_playlist_tracks_playlistId ON playlist_tracks(playlistId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_playlist_tracks_trackKey ON playlist_tracks(trackKey)")
                db.execSQL("CREATE TABLE IF NOT EXISTS favorite_tracks (trackKey TEXT NOT NULL PRIMARY KEY, createdAt INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_favorite_tracks_createdAt ON favorite_tracks(createdAt)")
            }
        }

        internal val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE music_tracks ADD COLUMN titleOverride TEXT")
                db.execSQL("ALTER TABLE music_tracks ADD COLUMN artistOverride TEXT")
                db.execSQL("ALTER TABLE music_tracks ADD COLUMN albumOverride TEXT")
                db.execSQL("ALTER TABLE music_tracks ADD COLUMN artworkFilePath TEXT")
                db.execSQL("ALTER TABLE music_tracks ADD COLUMN lyricsLrc TEXT")
            }
        }

        internal val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS playback_history (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, trackKey TEXT NOT NULL, playedAt INTEGER NOT NULL, listenedMs INTEGER NOT NULL, playCount INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_playback_history_trackKey ON playback_history(trackKey)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_playback_history_playedAt ON playback_history(playedAt)")
            }
        }

        @Volatile private var instance: MusicLibraryDatabase? = null

        fun getInstance(context: Context): MusicLibraryDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                MusicLibraryDatabase::class.java,
                "music_library.db"
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build().also { instance = it }
        }
    }
}
