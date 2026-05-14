package com.radhanathswami.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey val id: String,
    val title: String,
    val url: String,
    val category: String = "",
    val date: String = "",
    val localPath: String? = null,
    val lastPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val lastPlayedAt: Long = System.currentTimeMillis()
)
