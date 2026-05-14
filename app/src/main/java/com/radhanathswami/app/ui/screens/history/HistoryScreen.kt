package com.radhanathswami.app.ui.screens.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.radhanathswami.app.data.local.HistoryEntity
import com.radhanathswami.app.data.model.AudioItem
import com.radhanathswami.app.ui.player.PlayerController
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(
    playerController: PlayerController,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val history by viewModel.history.collectAsState()

    if (history.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
                Text(
                    "No history yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
        items(history, key = { it.id }) { item ->
            HistoryListItem(
                item = item,
                onClick = {
                    val audio = AudioItem(
                        id = item.id,
                        title = item.title,
                        url = item.url,
                        category = item.category,
                        date = item.date,
                        localPath = item.localPath
                    )
                    val resumeThreshold = if (item.durationMs > 0) (item.durationMs * 0.95).toLong() else Long.MAX_VALUE
                    if (item.lastPositionMs > 0 && item.lastPositionMs < resumeThreshold) {
                        playerController.playFromPosition(audio, item.lastPositionMs)
                    } else {
                        playerController.play(audio)
                    }
                },
                onDelete = { viewModel.delete(item) }
            )
        }
    }
}

@Composable
private fun HistoryListItem(
    item: HistoryEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val progress = if (item.durationMs > 0)
        (item.lastPositionMs.toFloat() / item.durationMs).coerceIn(0f, 1f)
    else 0f
    val isPartial = progress > 0f && progress < 0.95f

    val dateStr = remember(item.lastPlayedAt) {
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(item.lastPlayedAt))
    }

    Column {
        ListItem(
            modifier = Modifier.clickable(onClick = onClick),
            headlineContent = {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            },
            supportingContent = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (item.category.isNotBlank()) {
                            Text(
                                item.category.replace("_", " "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            dateStr,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (item.durationMs > 0) {
                        Text(
                            text = if (isPartial)
                                "Resume from ${formatTime(item.lastPositionMs)} / ${formatTime(item.durationMs)}"
                            else
                                formatTime(item.durationMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isPartial) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            leadingContent = {
                Icon(
                    imageVector = if (isPartial) Icons.Default.PlayCircle else Icons.Default.CheckCircleOutline,
                    contentDescription = null,
                    tint = if (isPartial) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(36.dp)
                )
            },
            trailingContent = {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove from history",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background)
        )
        if (item.durationMs > 0) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(2.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(minutes, seconds)
}
