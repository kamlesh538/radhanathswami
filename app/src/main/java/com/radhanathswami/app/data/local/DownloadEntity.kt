package com.radhanathswami.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val id: String,
    val title: String,
    val url: String,
    val localPath: String,
    val category: String = "",
    val date: String = "",
    val fileSizeMb: Double = 0.0,
    val downloadedAt: Long = System.currentTimeMillis(),
    val status: DownloadStatus = DownloadStatus.COMPLETED
)

enum class DownloadStatus {
    IN_PROGRESS, COMPLETED, FAILED
}
