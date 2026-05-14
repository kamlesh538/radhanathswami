package com.radhanathswami.app.ui.screens.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.radhanathswami.app.data.model.AudioItem
import com.radhanathswami.app.data.model.BrowseItem
import com.radhanathswami.app.data.remote.AudioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class CategoryUiState {
    object Loading : CategoryUiState()
    data class Success(val items: List<BrowseItem>) : CategoryUiState()
    data class Error(val message: String) : CategoryUiState()
}

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val repository: AudioRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CategoryUiState>(CategoryUiState.Loading)
    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()

    private val _downloadingIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadingIds: StateFlow<Set<String>> = _downloadingIds.asStateFlow()

    private val _downloadedIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadedIds: StateFlow<Set<String>> = _downloadedIds.asStateFlow()

    private var currentPath: String = ""

    fun loadDirectory(path: String) {
        currentPath = path
        viewModelScope.launch {
            _uiState.value = CategoryUiState.Loading
            try {
                val items = repository.browseDirectory(path)
                _uiState.value = CategoryUiState.Success(items)
                checkDownloadedStatus(items)
            } catch (e: Exception) {
                _uiState.value = CategoryUiState.Error(e.message ?: "Failed to load content")
            }
        }
    }

    private suspend fun checkDownloadedStatus(items: List<BrowseItem>) {
        val audioIds = items.filterIsInstance<BrowseItem.Audio>().map { it.audioItem.id }
        val downloaded = audioIds.filter { repository.isDownloaded(it) }.toSet()
        _downloadedIds.value = downloaded
    }

    fun downloadAudio(audio: AudioItem) {
        viewModelScope.launch {
            _downloadingIds.value = _downloadingIds.value + audio.id
            val result = repository.downloadAudio(audio)
            _downloadingIds.value = _downloadingIds.value - audio.id
            if (result.isSuccess) {
                _downloadedIds.value = _downloadedIds.value + audio.id
            }
        }
    }

    fun retry() = loadDirectory(currentPath)
}
