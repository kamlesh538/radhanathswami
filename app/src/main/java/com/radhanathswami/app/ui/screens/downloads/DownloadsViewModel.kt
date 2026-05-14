package com.radhanathswami.app.ui.screens.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.radhanathswami.app.data.model.AudioItem
import com.radhanathswami.app.data.remote.AudioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val repository: AudioRepository
) : ViewModel() {

    val downloads: StateFlow<List<AudioItem>> = repository.getDownloadedAudios()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteDownload(audio: AudioItem) {
        viewModelScope.launch {
            repository.deleteDownload(audio)
        }
    }
}
