package com.radhanathswami.app.ui.screens.categories

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.radhanathswami.app.data.local.HistoryEntity
import com.radhanathswami.app.data.model.AudioItem
import com.radhanathswami.app.data.model.BrowseItem
import com.radhanathswami.app.ui.player.PlayerController
import com.radhanathswami.app.ui.player.PlayerState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    folderPath: String,
    folderName: String,
    onNavigateBack: () -> Unit,
    onFolderClick: (BrowseItem.Folder) -> Unit,
    playerController: PlayerController,
    viewModel: CategoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val downloadingIds by viewModel.downloadingIds.collectAsState()
    val downloadedIds by viewModel.downloadedIds.collectAsState()
    val historyMap by viewModel.historyMap.collectAsState()
    val playerState by playerController.playerState.collectAsState()

    LaunchedEffect(folderPath) {
        viewModel.loadDirectory(folderPath)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = folderName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is CategoryUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                is CategoryUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(onClick = { viewModel.retry() }) {
                            Text("Retry")
                        }
                    }
                }

                is CategoryUiState.Success -> {
                    if (state.items.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "No content found",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        val folderAudioItems = remember(state.items) {
                            state.items.filterIsInstance<BrowseItem.Audio>().map { it.audioItem }
                        }
                        LazyColumn(
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(state.items) { item ->
                                when (item) {
                                    is BrowseItem.Folder -> FolderListItem(
                                        folder = item,
                                        onClick = { onFolderClick(item) }
                                    )
                                    is BrowseItem.Audio -> AudioListItem(
                                        audio = item.audioItem,
                                        isPlaying = playerState.currentAudio?.id == item.audioItem.id && playerState.isPlaying,
                                        isDownloading = item.audioItem.id in downloadingIds,
                                        isDownloaded = item.audioItem.id in downloadedIds,
                                        historyEntity = historyMap[item.audioItem.id],
                                        onPlay = { audio ->
                                            val history = historyMap[audio.id]
                                            playerController.setQueue(folderAudioItems)
                                            val resumeThreshold = if (history != null && history.durationMs > 0) (history.durationMs * 0.95).toLong() else Long.MAX_VALUE
                                            if (history != null && history.lastPositionMs > 0 && history.lastPositionMs < resumeThreshold) {
                                                val audioWithPath = if (history.localPath != null) audio.copy(localPath = history.localPath) else audio
                                                playerController.playFromPosition(audioWithPath, history.lastPositionMs)
                                            } else {
                                                playerController.play(audio)
                                            }
                                        },
                                        onDownload = { audio ->
                                            viewModel.downloadAudio(audio)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FolderListItem(folder: BrowseItem.Folder, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = {
            Text(
                text = folder.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = if (folder.itemCount > 0) {
            { Text("${folder.itemCount} items", style = MaterialTheme.typography.bodySmall) }
        } else null,
        leadingContent = {
            Icon(
                Icons.Default.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
        },
        trailingContent = {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    )
}

@Composable
fun AudioListItem(
    audio: AudioItem,
    isPlaying: Boolean,
    isDownloading: Boolean,
    isDownloaded: Boolean,
    historyEntity: HistoryEntity? = null,
    onPlay: (AudioItem) -> Unit,
    onDownload: (AudioItem) -> Unit
) {
    val isHeard = !isPlaying && historyEntity != null && historyEntity.lastPositionMs > 0

    ListItem(
        modifier = Modifier
            .clickable { onPlay(audio) }
            .alpha(if (isHeard) 0.5f else 1f),
        headlineContent = {
            Text(
                text = audio.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isPlaying) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = if (audio.date.isNotBlank() || isHeard) {
            {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (audio.date.isNotBlank()) {
                        Text(audio.date, style = MaterialTheme.typography.bodySmall)
                    }
                    if (isHeard && historyEntity != null) {
                        val posText = formatDuration(historyEntity.lastPositionMs)
                        val durText = if (historyEntity.durationMs > 0) " / ${formatDuration(historyEntity.durationMs)}" else ""
                        Text(
                            "Heard: $posText$durText",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        } else null,
        leadingContent = {
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isPlaying) {
                    Icon(
                        Icons.Default.VolumeUp,
                        contentDescription = "Now playing",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                } else {
                    Icon(
                        Icons.Default.PlayCircleOutline,
                        contentDescription = "Play",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        },
        trailingContent = {
            when {
                isDownloading -> CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
                isDownloaded -> Icon(
                    Icons.Default.DownloadDone,
                    contentDescription = "Downloaded",
                    tint = MaterialTheme.colorScheme.primary
                )
                else -> IconButton(onClick = { onDownload(audio) }) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = "Download",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    )
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(minutes, seconds)
}
