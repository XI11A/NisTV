package com.nis.nistv.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import com.nis.nistv.core.designsystem.component.NisLinearProgressBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.nis.nistv.core.designsystem.component.NisPrimaryButton
import com.nis.nistv.core.designsystem.icon.NisIcons
import com.nis.nistv.core.designsystem.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryAndPrivacyScreen(
    viewModel: HistoryAndPrivacyViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToChannel: (String) -> Unit,
    onNavigateToDownload: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showResetTasteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.message) {
        uiState.message?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = NisTheme.colors.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("History & Privacy", color = NisTheme.colors.textPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NisTheme.colors.textPrimary)
                    }
                },
                actions = {
                    val hasHistory = uiState.todayItems.isNotEmpty() ||
                            uiState.yesterdayItems.isNotEmpty() ||
                            uiState.earlierItems.isNotEmpty()
                    if (hasHistory) {
                        TextButton(onClick = { showClearConfirm = true }) {
                            Text("Clear All", color = NisTheme.colors.primary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NisTheme.colors.background)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = NisDimens.MaxContentWidth),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // 1. On-Device Privacy Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = NisSurface),
                    shape = NisShapes.medium
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            NisIcons.Lock,
                            contentDescription = null,
                            tint = NisAccentCyan,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "100% On-Device Privacy",
                                color = NisTextPrimary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "Your watch history, downloads, and AI recommendation taste profiles stay strictly on this device and are never sent to remote servers.",
                                color = NisTextMuted,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            // 2. Privacy Controls Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = NisCardBackground),
                    shape = NisShapes.medium
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Pause Watch History",
                                    color = NisTextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    "When paused, new channels and downloads won't be recorded in history or update recommendation affinities.",
                                    color = NisTextMuted,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = uiState.isHistoryPaused,
                                onCheckedChange = { viewModel.togglePauseHistory(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = NisAccentPrimary
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Reset Recommendation Taste",
                                    color = NisTextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    "Clear all stored affinities and reset the on-device AI recommendation model to fresh defaults.",
                                    color = NisTextMuted,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            TextButton(onClick = { showResetTasteConfirm = true }) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = NisAccentPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reset", color = NisAccentPrimary)
                            }
                        }
                    }
                }
            }

            // 3. Recently Watched Downloads (if any)
            if (uiState.recentDownloads.isNotEmpty()) {
                item {
                    Text(
                        "Recently Watched Downloads",
                        color = NisTextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(uiState.recentDownloads, key = { it.downloadId }) { item ->
                            DownloadHistoryCard(
                                item = item,
                                onClick = { onNavigateToDownload(item.downloadId) }
                            )
                        }
                    }
                }
            }

            // 4. Channel History Sections
            if (uiState.todayItems.isNotEmpty()) {
                item {
                    Text(
                        "Today",
                        color = NisTextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(uiState.todayItems, key = { "today_${it.channelId}" }) { item ->
                    HistoryItemRow(
                        item = item,
                        onClick = { onNavigateToChannel(item.channelId) },
                        onDelete = { viewModel.deleteChannelHistory(item.channelId) }
                    )
                }
            }

            if (uiState.yesterdayItems.isNotEmpty()) {
                item {
                    Text(
                        "Yesterday",
                        color = NisTextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(uiState.yesterdayItems, key = { "yesterday_${it.channelId}" }) { item ->
                    HistoryItemRow(
                        item = item,
                        onClick = { onNavigateToChannel(item.channelId) },
                        onDelete = { viewModel.deleteChannelHistory(item.channelId) }
                    )
                }
            }

            if (uiState.earlierItems.isNotEmpty()) {
                item {
                    Text(
                        "Earlier",
                        color = NisTextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(uiState.earlierItems, key = { "earlier_${it.channelId}" }) { item ->
                    HistoryItemRow(
                        item = item,
                        onClick = { onNavigateToChannel(item.channelId) },
                        onDelete = { viewModel.deleteChannelHistory(item.channelId) }
                    )
                }
            }

            if (uiState.todayItems.isEmpty() && uiState.yesterdayItems.isEmpty() && uiState.earlierItems.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No channel watch history yet",
                            color = NisTextMuted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            shape = NisDialogShape,
            title = { Text("Clear All Watch History?", color = NisTextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently remove all channel watch history records from your device.", color = NisTextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showClearConfirm = false
                    viewModel.clearAllHistory()
                }) {
                    Text("Clear All", color = NisAccentPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel", color = NisTextMuted)
                }
            },
            containerColor = NisSurface
        )
    }

    if (showResetTasteConfirm) {
        AlertDialog(
            onDismissRequest = { showResetTasteConfirm = false },
            shape = NisDialogShape,
            title = { Text("Reset Recommendation Profile?", color = NisTextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("This will reset your calculated taste affinities and recommendation history back to factory state.", color = NisTextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showResetTasteConfirm = false
                    viewModel.resetRecommendations()
                }) {
                    Text("Reset", color = NisAccentPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetTasteConfirm = false }) {
                    Text("Cancel", color = NisTextMuted)
                }
            },
            containerColor = NisSurface
        )
    }
}

@Composable
private fun HistoryItemRow(
    item: HistoryItemUi,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = NisCardBackground),
        shape = NisThumbnailShape
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(NisShapes.small)
                    .background(NisSurface),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = item.logoUrl,
                    contentDescription = item.title,
                    modifier = Modifier.size(36.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    color = NisTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${item.category} • ${item.durationFormatted}",
                    color = NisTextMuted,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = NisTextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun DownloadHistoryCard(
    item: DownloadHistoryItemUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(110.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = NisCardBackground),
        shape = NisShapes.small
    ) {
        Column {
            Box(
                modifier = Modifier
                    .width(110.dp)
                    .height(150.dp)
                    .background(NisSurface)
            ) {
                AsyncImage(
                    model = item.posterUrl,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                if (item.progressPercent > 0) {
                    NisLinearProgressBar(
                        progress = item.progressPercent / 100f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .align(Alignment.BottomCenter),
                        color = NisAccentPrimary,
                        trackColor = Color.Black.copy(alpha = 0.5f)
                    )
                }
            }
            Text(
                text = item.title,
                color = NisTextPrimary,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(6.dp)
            )
        }
    }
}
