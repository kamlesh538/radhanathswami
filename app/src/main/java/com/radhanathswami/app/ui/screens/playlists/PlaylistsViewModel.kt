package com.radhanathswami.app.ui.screens.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.radhanathswami.app.data.local.PlaylistDao
import com.radhanathswami.app.data.local.PlaylistEntity
import com.radhanathswami.app.data.local.PlaylistEntryEntity
import com.radhanathswami.app.data.model.AudioItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    private val playlistDao: PlaylistDao
) : ViewModel() {

    val playlists: StateFlow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            playlistDao.insertPlaylist(
                PlaylistEntity(id = UUID.randomUUID().toString(), name = name.trim())
            )
        }
    }

    fun deletePlaylist(entity: PlaylistEntity) {
        viewModelScope.launch {
            playlistDao.deletePlaylist(entity)
        }
    }

    fun addAudioToPlaylist(playlistId: String, audio: AudioItem) {
        viewModelScope.launch {
            val maxPos = playlistDao.maxPosition(playlistId) ?: -1
            playlistDao.insertEntry(
                PlaylistEntryEntity(
                    playlistId = playlistId,
                    audioId = audio.id,
                    position = maxPos + 1,
                    title = audio.title,
                    url = audio.url,
                    category = audio.category,
                    date = audio.date,
                    localPath = audio.localPath
                )
            )
        }
    }
}
