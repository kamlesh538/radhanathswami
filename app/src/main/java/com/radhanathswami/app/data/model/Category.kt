package com.radhanathswami.app.data.model

data class Category(
    val name: String,
    val path: String,
    val itemCount: Int = 0,
    val isFolder: Boolean = true
)
