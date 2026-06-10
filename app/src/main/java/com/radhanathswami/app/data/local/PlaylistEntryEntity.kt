package com.radhanathswami.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.radhanathswami.app.data.model.AudioItem

@Entity(
    tableName = "playlist_entries",
    primaryKeys = ["playlistId", "audioId"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("playlistId")]
)
data class PlaylistEntryEntity(
    val playlistId: String,
    val audioId: String,
    val position: Int,
    val title: String,
    val url: String,
    val category: String = "",
    val date: String = "",
    val localPath: String? = null
)

fun PlaylistEntryEntity.toAudioItem() = AudioItem(
    id = audioId,
    title = title,
    url = url,
    category = category,
    date = date,
    localPath = localPath
)
