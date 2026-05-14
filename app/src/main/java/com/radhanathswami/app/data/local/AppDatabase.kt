package com.radhanathswami.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [DownloadEntity::class, HistoryEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
    abstract fun historyDao(): HistoryDao

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
    }
}
