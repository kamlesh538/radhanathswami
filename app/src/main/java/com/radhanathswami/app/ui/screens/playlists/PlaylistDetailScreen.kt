package com.radhanathswami.app.ui.screens.playlists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.radhanathswami.app.data.local.PlaylistEntryEntity
import com.radhanathswami.app.data.local.toAudioItem
import com.radhanathswami.app.ui.player.PlayerController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    playlistName: String,
    onNavigateBack: () -> Unit,
    onAddLecture: () -> Unit,
    playerController: PlayerController,
    viewModel: PlaylistDetailViewModel = hiltViewModel()
) {
    val entries by viewModel.entries.collectAsState()
    val historyMap by viewModel.historyMap.collectAsState()
    val playerState by playerController.playerState.collectAsState()

    LaunchedEffect(playlistId) {
        viewModel.init(playlistId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = playlistName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onAddLecture) {
                        Icon(Icons.Default.Add, contentDescription = "Add lecture")
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
            if (entries.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.QueueMusic,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Text(
                        "No lectures yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Tap + to browse and add lectures",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                    items(entries, key = { it.audioId }) { entry ->
                        PlaylistEntryItem(
                            entry = entry,
                            isPlaying = playerState.currentAudio?.id == entry.audioId && playerState.isPlaying,
                            historyEntity = historyMap[entry.audioId],
                            onPlay = {
                                val audioItems = entries.map { it.toAudioItem() }
                                playerController.setQueue(audioItems, playlistId, playlistName)
                                playerController.play(entry.toAudioItem())
                            },
                            onRemove = { viewModel.removeEntry(entry.audioId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistEntryItem(
    entry: PlaylistEntryEntity,
    isPlaying: Boolean,
    historyEntity: HistoryEntity? = null,
    onPlay: () -> Unit,
    onRemove: () -> Unit
) {
    val isHeard = !isPlaying && historyEntity != null && historyEntity.lastPositionMs > 0

    ListItem(
        modifier = Modifier
            .clickable(onClick = onPlay)
            .alpha(if (isHeard) 0.5f else 1f),
        headlineContent = {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isPlaying) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = if (entry.date.isNotBlank() || isHeard) {
            {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (entry.date.isNotBlank()) {
                        Text(entry.date, style = MaterialTheme.typography.bodySmall)
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
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.RemoveCircleOutline,
                    contentDescription = "Remove from playlist",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background)
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
