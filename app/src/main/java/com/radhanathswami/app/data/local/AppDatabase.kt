package com.radhanathswami.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [DownloadEntity::class, HistoryEntity::class, PlaylistEntity::class, PlaylistEntryEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
    abstract fun historyDao(): HistoryDao
    abstract fun playlistDao(): PlaylistDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS history (
                        id TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        url TEXT NOT NULL,
                        category TEXT NOT NULL DEFAULT '',
                        date TEXT NOT NULL DEFAULT '',
                        localPath TEXT,
                        lastPositionMs INTEGER NOT NULL DEFAULT 0,
                        durationMs INTEGER NOT NULL DEFAULT 0,
                        lastPlayedAt INTEGER NOT NULL DEFAULT 0
                    )"""
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS playlists (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )"""
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS playlist_entries (
                        playlistId TEXT NOT NULL,
                        audioId TEXT NOT NULL,
                        position INTEGER NOT NULL DEFAULT 0,
                        title TEXT NOT NULL,
                        url TEXT NOT NULL,
                        category TEXT NOT NULL DEFAULT '',
                        date TEXT NOT NULL DEFAULT '',
                        localPath TEXT,
                        PRIMARY KEY (playlistId, audioId),
                        FOREIGN KEY (playlistId) REFERENCES playlists(id) ON DELETE CASCADE
                    )"""
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_playlist_entries_playlistId ON playlist_entries (playlistId)"
                )
            }
        }
    }
}
