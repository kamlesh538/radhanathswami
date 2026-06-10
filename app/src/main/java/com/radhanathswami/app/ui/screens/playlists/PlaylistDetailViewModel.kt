package com.radhanathswami.app.ui.screens.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.radhanathswami.app.data.local.HistoryDao
import com.radhanathswami.app.data.local.HistoryEntity
import com.radhanathswami.app.data.local.PlaylistDao
import com.radhanathswami.app.data.local.PlaylistEntryEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val historyDao: HistoryDao
) : ViewModel() {

    private val _playlistId = MutableStateFlow("")

    val historyMap: StateFlow<Map<String, HistoryEntity>> = historyDao.getAll()
        .map { list -> list.associateBy { it.id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val entries: StateFlow<List<PlaylistEntryEntity>> = _playlistId
        .flatMapLatest { id ->
            if (id.isBlank()) flowOf(emptyList())
            else playlistDao.getEntriesForPlaylist(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun init(playlistId: String) {
        if (_playlistId.value.isBlank()) _playlistId.value = playlistId
    }

    fun removeEntry(audioId: String) {
        viewModelScope.launch {
            playlistDao.removeEntry(_playlistId.value, audioId)
        }
    }
}
