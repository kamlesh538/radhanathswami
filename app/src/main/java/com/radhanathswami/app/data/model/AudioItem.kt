package com.radhanathswami.app.data.model

data class AudioItem(
    val id: String,
    val title: String,
    val url: String,
    val duration: String = "",
    val date: String = "",
    val category: String = "",
    val fileSizeMb: Double = 0.0,
    val isDownloaded: Boolean = false,
    val localPath: String? = null
)
