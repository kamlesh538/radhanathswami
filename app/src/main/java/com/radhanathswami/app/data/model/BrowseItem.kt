package com.radhanathswami.app.data.model

sealed class BrowseItem {
    data class Folder(
        val name: String,
        val path: String,
        val itemCount: Int = 0
    ) : BrowseItem()

    data class Audio(val audioItem: AudioItem) : BrowseItem()
}
