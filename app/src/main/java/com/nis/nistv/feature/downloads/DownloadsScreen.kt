package com.nis.nistv.feature.downloads

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.nis.nistv.NisTvApplication
import com.nis.nistv.core.database.entity.DownloadEntity
import com.nis.nistv.core.database.entity.DownloadStatus
import com.nis.nistv.core.designsystem.component.*
import com.nis.nistv.core.designsystem.icon.NisIcons
import com.nis.nistv.core.designsystem.theme.*
import com.nis.nistv.data.download.engine.MultiThreadedDownloader
import com.nis.nistv.feature.downloads.components.OfflineMediaPlayerDialog
import java.io.File

@Composable
fun DownloadsScreen(
    onPlayDownload: (String) -> Unit = {},
    onOpenOfflineDetails: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
    viewModel: DownloadsViewModel = viewModel(
        factory = DownloadsViewModel.provideFactory(
            downloadManager = (LocalContext.current.applicationContext as NisTvApplication).container.downloadManager,
            userPreferences = (LocalContext.current.applicationContext as NisTvApplication).container.userPreferences,
            offlineMediaDao = (LocalContext.current.applicationContext as NisTvApplication).container.database.offlineMediaDao(),
            offlineMediaRepository = (LocalContext.current.applicationContext as NisTvApplication).container.offlineMediaRepository,
            downloadsRepairManager = (LocalContext.current.applicationContext as NisTvApplication).container.downloadsRepairManager
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val app = context.applicationContext as NisTvApplication
    val isOnline by app.container.networkMonitor.isOnline.collectAsState(initial = true)
    var playingDownload by remember { mutableStateOf<DownloadEntity?>(null) }
    var downloadToDelete by remember { mutableStateOf<DownloadEntity?>(null) }
    var seriesToDelete by remember { mutableStateOf<SeriesDownloadGroup?>(null) }

    // Storage & Notification Permissions Launcher
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val storageGranted = results[Manifest.permission.READ_MEDIA_VIDEO] == true ||
                results[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        if (storageGranted) {
            viewModel.scanAndImportDownloads()
        }
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO)
            permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val missing = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            permissionsLauncher.launch(missing.toTypedArray())
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NisTheme.colors.background)
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Screen Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = NisDimens.MaxContentWidth)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Downloads",
                    color = NisTheme.colors.textPrimary,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )

                if (!isOnline) {
                    Surface(
                        shape = NisPillShape,
                        color = NisTheme.colors.success.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NisTheme.colors.success.copy(alpha = 0.5f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(NisTheme.colors.success)
                            )
                            Text(
                                text = "AIRPLANE / OFFLINE",
                                color = NisTheme.colors.success,
                                style = NisTheme.textExtras.micro,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Scan & import local downloaded files
                IconButton(
                    onClick = {
                        viewModel.scanAndImportDownloads()
                        Toast.makeText(context, "Scanning folder for media...", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(NisTheme.colors.surfaceElevated)
                ) {
                    Icon(
                        imageVector = NisIcons.Refresh,
                        contentDescription = "Scan & Import Local Files",
                        tint = NisTheme.colors.textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Add direct download URL button
                IconButton(
                    onClick = { viewModel.openAddUrlDialog() },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(NisTheme.colors.surfaceElevated)
                ) {
                    Icon(
                        imageVector = NisIcons.Download,
                        contentDescription = "Add Download URL",
                        tint = NisTheme.colors.textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = NisDimens.MaxContentWidth),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Subtle "Saving details for offline..." banner
            if (uiState.isSavingOfflineDetails) {
                item(key = "saving_offline_details_banner") {
                    Surface(
                        shape = NisThumbnailShape,
                        color = NisTheme.colors.primary.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NisTheme.colors.primary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = NisTheme.colors.primary,
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "Saving details for offline...",
                                color = NisTheme.colors.primary,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
            // 1. Storage Stats & AFDS Quota Card
            item(key = "storage_stats") {
                val stats = uiState.storageStats
                DownloadsStorageCard(
                    usedNisBytes = stats?.formattedNistvUsed ?: "0 B",
                    freeDeviceBytes = stats?.formattedAvailable ?: "--",
                    fraction = stats?.usedFraction ?: 0f,
                    isWifiOnly = uiState.isWifiOnly,
                    todayBytes = uiState.todayDownloadedBytes,
                    afdsLinksUsed = uiState.afdsDailyLinksUsed,
                    afdsLinksLimit = uiState.afdsDailyLinksLimit,
                    onToggleWifiOnly = { viewModel.toggleWifiOnly(it) }
                )
            }

            // 2. Active Download Queue
            if (uiState.activeDownloads.isNotEmpty()) {
                item(key = "active_header") {
                    Text(
                        text = "ACTIVE & QUEUED (${uiState.activeDownloads.size})",
                        color = NisAccentPrimary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                items(uiState.activeDownloads, key = { it.id }) { download ->
                    ActiveDownloadCard(
                        download = download,
                        threadProgress = uiState.activeThreadProgress[download.id] ?: emptyList(),
                        onPause = { viewModel.pauseDownload(download.id) },
                        onResume = { viewModel.resumeDownload(download.id) },
                        onCancel = { viewModel.cancelDownload(download.id) },
                        onRetry = { viewModel.retryDownload(download.id) }
                    )
                }
            }

            // 3. Movies Section (One card per movie, tap opens offline details)
            if (uiState.movies.isNotEmpty()) {
                item(key = "movies_header") {
                    Text(
                        text = "MOVIES (${uiState.movies.size})",
                        color = NisTheme.colors.textSecondary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                items(uiState.movies, key = { it.id }) { movie ->
                    MovieDownloadCard(
                        download = movie,
                        onClick = {
                            val idStr = movie.seriesId?.ifBlank { null } ?: movie.mediaId.substringBefore('_').ifBlank { movie.id }
                            onOpenOfflineDetails("movie", idStr)
                        },
                        onPlay = { onPlayDownload(movie.id) },
                        onDelete = { downloadToDelete = movie }
                    )
                }
            }

            // 4. TV Series Section (Grouped Series > Season > Episodes)
            if (uiState.tvSeries.isNotEmpty()) {
                item(key = "series_header") {
                    Text(
                        text = "TV SERIES (${uiState.tvSeries.size})",
                        color = NisTheme.colors.textSecondary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                items(uiState.tvSeries, key = { "series_${it.seriesId}" }) { series ->
                    SeriesDownloadCard(
                        series = series,
                        onClick = {
                            onOpenOfflineDetails("tv", series.seriesId)
                        },
                        onPlay = {
                            val allEpisodes = series.seasons.flatMap { it.episodes }
                            val inProgress = allEpisodes.filter { it.lastPlaybackPositionMs > 5_000L }
                                .maxByOrNull { it.updatedAt }
                            val target = inProgress ?: allEpisodes.firstOrNull()
                            if (target != null) {
                                onPlayDownload(target.id)
                            } else {
                                onOpenOfflineDetails("tv", series.seriesId)
                            }
                        },
                        onDeleteAll = { seriesToDelete = series }
                    )
                }
            }

            // 5. Empty State
            if (uiState.completedDownloads.isEmpty() && uiState.activeDownloads.isEmpty()) {
                item(key = "empty_downloads") {
                    NisEmptyState(
                        title = "No Downloads Yet",
                        message = "Find movies and series in Discover to download and watch offline"
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(90.dp))
            }
        }
    }

    // Delete Single Download Confirmation Dialog
    downloadToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { downloadToDelete = null },
            containerColor = NisTheme.colors.surface,
            title = {
                Text(
                    text = "Delete Download?",
                    color = NisTheme.colors.textPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete \"${target.title}\"?\n\nThis will permanently remove the video file from your device storage.",
                    color = NisTheme.colors.textSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDownload(target.id)
                        downloadToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NisError)
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { downloadToDelete = null }) {
                    Text("Cancel", color = NisTheme.colors.textSecondary)
                }
            }
        )
    }

    // Delete Entire Series Confirmation Dialog
    seriesToDelete?.let { targetSeries ->
        AlertDialog(
            onDismissRequest = { seriesToDelete = null },
            containerColor = NisTheme.colors.surface,
            title = {
                Text(
                    text = "Delete All Episodes?",
                    color = NisTheme.colors.textPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete all ${targetSeries.episodesCount} episodes of \"${targetSeries.seriesName}\"?\n\nThis will permanently remove all files for this series from your device.",
                    color = NisTheme.colors.textSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSeries(targetSeries.seriesId)
                        seriesToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NisError)
                ) {
                    Text("Delete All", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { seriesToDelete = null }) {
                    Text("Cancel", color = NisTheme.colors.textSecondary)
                }
            }
        )
    }

    // In-App Offline Media Player Dialog Fallback
    playingDownload?.let { downloadToPlay ->
        OfflineMediaPlayerDialog(
            download = downloadToPlay,
            onDismiss = { playingDownload = null }
        )
    }

    // Add Direct URL Download Dialog
    if (uiState.showAddUrlDialog) {
        AddUrlDownloadDialog(
            onAdd = { title, url ->
                viewModel.addManualDownload(title, url)
            },
            onDismiss = { viewModel.dismissAddUrlDialog() }
        )
    }
}

@Composable
fun MovieDownloadCard(
    download: DownloadEntity,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(NisMediaCardShape)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = NisTheme.colors.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, NisTheme.colors.border),
        shape = NisMediaCardShape
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Poster thumbnail (check local file or posterUrl)
            Box(
                modifier = Modifier
                    .width(76.dp)
                    .height(112.dp)
                    .clip(NisShapes.small)
                    .background(NisTheme.colors.surfaceElevated)
            ) {
                if (!download.posterUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(download.posterUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = download.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(imageVector = NisIcons.LiveTv, contentDescription = null, tint = NisTheme.colors.textMuted, modifier = Modifier.size(28.dp))
                    }
                }

                // Overlay Play Circle
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(NisScrimStrong)
                        .clickable(onClick = onPlay),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = NisIcons.Play, contentDescription = "Play", tint = NisTheme.colors.primary, modifier = Modifier.size(14.dp))
                }
            }

            // Movie Info Column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = download.title,
                        color = NisTheme.colors.textPrimary,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(imageVector = NisIcons.Delete, contentDescription = "Delete", tint = NisTheme.colors.textMuted, modifier = Modifier.size(18.dp))
                    }
                }

                // Metadata Row (Rating, Year, Runtime, Size)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (download.rating != null && download.rating > 0.0) {
                        Surface(shape = NisBadgeShape, color = NisTheme.colors.warning.copy(alpha = 0.2f)) {
                            Text(
                                text = "★ ${String.format(java.util.Locale.US, "%.1f", download.rating)}",
                                color = NisTheme.colors.warning,
                                style = NisTheme.textExtras.micro,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (!download.releaseDate.isNullOrBlank()) {
                        val year = download.releaseDate.take(4)
                        Text(text = year, color = NisTheme.colors.textSecondary, style = MaterialTheme.typography.labelSmall)
                        Text(text = "•", color = NisTheme.colors.textMuted, style = NisTheme.textExtras.micro)
                    }

                    if (download.runtimeMinutes != null && download.runtimeMinutes > 0) {
                        Text(text = "${download.runtimeMinutes}m", color = NisTheme.colors.textSecondary, style = MaterialTheme.typography.labelSmall)
                        Text(text = "•", color = NisTheme.colors.textMuted, style = NisTheme.textExtras.micro)
                    }

                    Text(
                        text = download.formattedTotal,
                        color = NisTheme.colors.success,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!download.genres.isNullOrBlank()) {
                    Text(
                        text = download.genres,
                        color = NisTheme.colors.textMuted,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = "Tap to view offline details",
                    color = NisTheme.colors.primary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SeriesDownloadCard(
    series: SeriesDownloadGroup,
    onClick: () -> Unit,
    onPlay: () -> Unit = {},
    onDeleteAll: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(NisMediaCardShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onDeleteAll
            ),
        colors = CardDefaults.cardColors(containerColor = NisTheme.colors.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, NisTheme.colors.border),
        shape = NisMediaCardShape
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Poster thumbnail (check local file first)
            Box(
                modifier = Modifier
                    .width(76.dp)
                    .height(112.dp)
                    .clip(NisShapes.small)
                    .background(NisTheme.colors.surfaceElevated)
            ) {
                val localFile = series.localPosterPath?.let { File(it) }
                if (localFile != null && localFile.exists()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(localFile)
                            .crossfade(true)
                            .build(),
                        contentDescription = series.seriesName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (!series.posterUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(series.posterUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = series.seriesName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(imageVector = NisIcons.LiveTv, contentDescription = null, tint = NisTheme.colors.textMuted, modifier = Modifier.size(28.dp))
                    }
                }

                // Overlay Series Badge at top
                Surface(
                    shape = NisBadgeShape,
                    color = NisScrimStrong,
                    modifier = Modifier.align(Alignment.TopStart).padding(4.dp)
                ) {
                    Text(
                        text = "SERIES",
                        color = NisTheme.colors.warning,
                        style = NisTheme.textExtras.micro,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                // Overlay Play Circle at bottom
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(NisScrimStrong)
                        .clickable(onClick = onPlay),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = NisIcons.Play, contentDescription = "Play Series", tint = NisTheme.colors.primary, modifier = Modifier.size(14.dp))
                }
            }

            // Series Info Column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = series.seriesName,
                        color = NisTheme.colors.textPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = onDeleteAll,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(imageVector = NisIcons.Delete, contentDescription = "Delete All Episodes", tint = NisTheme.colors.textMuted, modifier = Modifier.size(18.dp))
                    }
                }

                Text(
                    text = series.episodesLabel,
                    color = NisTheme.colors.primary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "Total Size: ${series.formattedTotalSize}",
                    color = NisTheme.colors.success,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = "Tap to explore seasons & episodes",
                    color = NisTheme.colors.textMuted,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

private fun resolveTmdbId(download: DownloadEntity): Long {
    val effectiveSeriesId = download.effectiveSeriesId
    if (!effectiveSeriesId.isNullOrBlank()) {
        return effectiveSeriesId.toLongOrNull() ?: 0L
    }
    val mediaId = download.mediaId
    return if (mediaId.contains("_")) {
        mediaId.substringBefore('_').toLongOrNull() ?: 0L
    } else {
        mediaId.toLongOrNull() ?: 0L
    }
}

@Composable
fun DownloadsStorageCard(
    usedNisBytes: String,
    freeDeviceBytes: String,
    fraction: Float,
    isWifiOnly: Boolean,
    todayBytes: Long = 0L,
    afdsLinksUsed: Int = 0,
    afdsLinksLimit: Int = 100,
    onToggleWifiOnly: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(NisMediaCardShape)
            .background(NisTheme.colors.surface)
            .border(1.dp, NisTheme.colors.border, NisMediaCardShape)
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Storage Used",
                        color = NisTheme.colors.textPrimary,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$usedNisBytes by NisTV • $freeDeviceBytes free on device",
                        color = NisTheme.colors.textSecondary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(NisBadgeShape)
                        .background(NisTheme.colors.surfaceElevated)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = usedNisBytes,
                        color = NisTheme.colors.primary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Storage Meter Bar
            NisLinearProgressBar(
                progress = fraction,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(NisBadgeShape),
                color = NisTheme.colors.primary,
                trackColor = NisTheme.colors.surfaceElevated
            )

            // Today's Usage & AFDS Server Quota Row
            val usedTodayBytes = DownloadEntity.formatBytes(todayBytes)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AFDS Quota: $afdsLinksUsed / $afdsLinksLimit links used ($usedTodayBytes)",
                    color = NisTheme.colors.textSecondary,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            // Wi-Fi Only Download Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Download on Wi-Fi only",
                        color = NisTheme.colors.textPrimary,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Saves mobile data by only running downloads on Wi-Fi",
                        color = NisTheme.colors.textMuted,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                Switch(
                    checked = isWifiOnly,
                    onCheckedChange = onToggleWifiOnly,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NisTheme.colors.primary,
                        checkedTrackColor = NisTheme.colors.primary.copy(alpha = 0.3f),
                        uncheckedThumbColor = NisTheme.colors.textMuted,
                        uncheckedTrackColor = NisTheme.colors.surfaceElevated
                    )
                )
            }
        }
    }
}

@Composable
fun ActiveDownloadCard(
    download: DownloadEntity,
    threadProgress: List<MultiThreadedDownloader.ThreadProgress> = emptyList(),
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(NisMediaCardShape),
        colors = CardDefaults.cardColors(containerColor = NisTheme.colors.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, NisTheme.colors.borderSubtle),
        shape = NisMediaCardShape
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = download.title,
                        color = NisTheme.colors.textPrimary,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${download.formattedDownloaded} / ${download.formattedTotal} • ${download.formattedSpeed}",
                        color = NisTheme.colors.textSecondary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                // Action Buttons: Pause/Resume/Retry + Cancel
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    when (download.status) {
                        DownloadStatus.DOWNLOADING.name -> {
                            IconButton(onClick = onPause) {
                                Icon(NisIcons.Pause, contentDescription = "Pause", tint = NisTheme.colors.primary)
                            }
                        }
                        DownloadStatus.PAUSED.name -> {
                            IconButton(onClick = onResume) {
                                Icon(NisIcons.Play, contentDescription = "Resume", tint = NisTheme.colors.primary)
                            }
                        }
                        DownloadStatus.FAILED.name -> {
                            IconButton(onClick = onRetry) {
                                Icon(NisIcons.Refresh, contentDescription = "Retry", tint = NisTheme.colors.warning)
                            }
                        }
                    }

                    IconButton(onClick = onCancel) {
                        Icon(NisIcons.Close, contentDescription = "Cancel", tint = NisTheme.colors.error)
                    }
                }
            }

            // Overall Progress Bar
            NisLinearProgressBar(
                progress = download.progressFloat,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(NisBadgeShape),
                color = when (download.status) {
                    DownloadStatus.FAILED.name -> NisTheme.colors.error
                    DownloadStatus.PAUSED.name -> NisTheme.colors.warning
                    else -> NisTheme.colors.primary
                },
                trackColor = NisTheme.colors.surfaceElevated
            )

            // Multi-thread breakdown: Exact status (progress) of all threads
            if (threadProgress.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(NisShapes.small)
                        .background(NisTheme.colors.surfaceElevated)
                        .border(1.dp, NisTheme.colors.borderSubtle, NisShapes.small)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (threadProgress.size > 1) "THREADS (${threadProgress.size} Chunks Active)" else "SINGLE STREAM",
                            color = NisTheme.colors.textSecondary,
                            style = NisTheme.textExtras.micro,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "${download.progressPercent}% Total",
                            color = NisTheme.colors.primary,
                            style = NisTheme.textExtras.micro,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    threadProgress.forEach { thread ->
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Thread ${thread.index + 1}",
                                        color = NisTheme.colors.textPrimary,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (thread.isCompleted) {
                                        Box(
                                            modifier = Modifier
                                                .clip(NisBadgeShape)
                                                .background(NisTheme.colors.success.copy(alpha = 0.15f))
                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = "Done",
                                                color = NisTheme.colors.success,
                                                style = NisTheme.textExtras.micro,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = "${thread.formattedDownloaded} / ${thread.formattedTotal} (${thread.progressPercent}%)",
                                    color = if (thread.isCompleted) NisTheme.colors.success else NisTheme.colors.textSecondary,
                                    style = NisTheme.textExtras.micro
                                )
                            }

                            NisLinearProgressBar(
                                progress = thread.progressFloat,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .clip(NisBadgeShape),
                                color = if (thread.isCompleted) NisTheme.colors.success else NisTheme.colors.primary,
                                trackColor = NisTheme.colors.surface
                            )
                        }
                    }
                }
            }

            // Status message
            if (download.status == DownloadStatus.FAILED.name && !download.errorMessage.isNullOrBlank()) {
                Text(
                    text = download.errorMessage,
                    color = NisTheme.colors.error,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
fun AddUrlDownloadDialog(
    onAdd: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NisTheme.colors.surface,
        title = {
            Text(
                text = "Download Media via URL",
                color = NisTheme.colors.textPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Enter a direct video URL (.mp4, .mkv, etc.) to download to your device storage for offline playback.",
                    color = NisTheme.colors.textSecondary,
                    style = MaterialTheme.typography.labelSmall
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title / Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = NisTheme.colors.textPrimary,
                        unfocusedTextColor = NisTheme.colors.textPrimary,
                        focusedBorderColor = NisTheme.colors.primary,
                        unfocusedBorderColor = NisTheme.colors.border
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Direct Video URL") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = NisTheme.colors.textPrimary,
                        unfocusedTextColor = NisTheme.colors.textPrimary,
                        focusedBorderColor = NisTheme.colors.primary,
                        unfocusedBorderColor = NisTheme.colors.border
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(title, url) },
                enabled = title.isNotBlank() && url.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = NisTheme.colors.primary)
            ) {
                Text("Start Download", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = NisTheme.colors.textSecondary)
            }
        }
    )
}
