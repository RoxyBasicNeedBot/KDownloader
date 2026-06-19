package com.roxybasicneedbot.kdownloader.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.roxybasicneedbot.kdownloader.core.model.DownloadProgress
import com.roxybasicneedbot.kdownloader.core.model.DownloadState

@Composable
fun DownloadProgressBar(
    progress: DownloadProgress,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${progress.percent}%",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "${progress.speedFormatted} | ETA: ${progress.etaFormatted}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        LinearProgressIndicator(
            progress = { progress.percent / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = ProgressIndicatorDefaults.LinearStrokeCap
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        val downloadedMb = String.format("%.2f", progress.downloadedBytes / (1024f * 1024f))
        val totalMb = if (progress.totalBytes > 0) {
            String.format("%.2f", progress.totalBytes / (1024f * 1024f)) + " MB"
        } else {
            "Unknown"
        }
        
        Text(
            text = "$downloadedMb MB / $totalMb",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun DownloadButton(
    state: DownloadState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = when (state) {
                is DownloadState.Done -> Color(0xFF2E7D32) // Green
                is DownloadState.Failed -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.primary
            }
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            when (state) {
                is DownloadState.Idle -> {
                    Text("Download")
                }
                is DownloadState.Queued -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Queued")
                }
                is DownloadState.Scheduled -> {
                    Text("Scheduled")
                }
                is DownloadState.Connecting -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Connecting...")
                }
                is DownloadState.Downloading -> {
                    Text("Pause (${state.progress.percent}%)")
                }
                is DownloadState.Paused -> {
                    Text("Resume")
                }
                is DownloadState.Merging -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Merging...")
                }
                is DownloadState.Verifying -> {
                    Text("Verifying...")
                }
                is DownloadState.PostProcessing -> {
                    Text("Extracting...")
                }
                is DownloadState.Done -> {
                    Text("Done ✓")
                }
                is DownloadState.Failed -> {
                    Text("Retry ↻")
                }
                is DownloadState.Cancelled -> {
                    Text("Cancelled")
                }
                is DownloadState.WaitingForNetwork -> {
                    Text("Waiting for Wifi...")
                }
            }
        }
    }
}

@Composable
fun DownloadItemCard(
    fileName: String,
    state: DownloadState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Row {
                    when (state) {
                        is DownloadState.Downloading, is DownloadState.Connecting, is DownloadState.WaitingForNetwork -> {
                            IconButton(onClick = onPause) {
                                Icon(
                                    painter = painterResource(id = android.R.drawable.ic_media_pause),
                                    contentDescription = "Pause",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        is DownloadState.Paused, is DownloadState.Failed -> {
                            IconButton(onClick = onResume) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Resume",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        else -> Unit
                    }
                    
                    if (state !is DownloadState.Done && state !is DownloadState.Cancelled) {
                        IconButton(onClick = onCancel) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Cancel",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            AnimatedVisibility(visible = state is DownloadState.Downloading) {
                if (state is DownloadState.Downloading) {
                    DownloadProgressBar(progress = state.progress)
                }
            }
            
            AnimatedVisibility(visible = state !is DownloadState.Downloading) {
                val statusText = when (state) {
                    is DownloadState.Idle -> "Idle"
                    is DownloadState.Queued -> "Queued..."
                    is DownloadState.Scheduled -> "Scheduled"
                    is DownloadState.Connecting -> "Connecting to server..."
                    is DownloadState.Downloading -> "Downloading..."
                    is DownloadState.Paused -> "Paused"
                    is DownloadState.Merging -> "Merging chunk files..."
                    is DownloadState.Verifying -> "Verifying file checksum..."
                    is DownloadState.PostProcessing -> "Post processing hooks..."
                    is DownloadState.Done -> "Completed successfully"
                    is DownloadState.Failed -> "Failed: ${state.error.message}"
                    is DownloadState.Cancelled -> "Cancelled"
                    is DownloadState.WaitingForNetwork -> "Waiting for connection..."
                }
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = when (state) {
                        is DownloadState.Failed -> MaterialTheme.colorScheme.error
                        is DownloadState.Done -> Color(0xFF2E7D32)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

@Composable
fun DownloadQueueList(
    downloads: List<Triple<String, String, DownloadState>>, // Triple of <Id, FileName, State>
    onPause: (String) -> Unit,
    onResume: (String) -> Unit,
    onCancel: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp)
    ) {
        items(downloads, key = { it.first }) { (id, fileName, state) ->
            DownloadItemCard(
                fileName = fileName,
                state = state,
                onPause = { onPause(id) },
                onResume = { onResume(id) },
                onCancel = { onCancel(id) }
            )
        }
    }
}
