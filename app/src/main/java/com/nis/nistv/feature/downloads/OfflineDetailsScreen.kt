package com.nis.nistv.feature.downloads

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.nis.nistv.NisTvApplication
import com.nis.nistv.core.database.entity.AutopilotRuleEntity
import com.nis.nistv.core.database.entity.DownloadEntity
import com.nis.nistv.core.database.entity.DownloadStatus
import com.nis.nistv.core.database.entity.OfflineCastMember
import com.nis.nistv.core.database.entity.OfflineEpisodeEntity
import com.nis.nistv.core.designsystem.component.*
import com.nis.nistv.core.designsystem.icon.NisIcons
import com.nis.nistv.core.designsystem.theme.*
import java.io.File
import java.util.Locale

@Composable
fun OfflineDetailsScreen(
    mediaType: String,
    mediaId: String,
    onBack: () -> Unit,
    onPlayDownload: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OfflineDetailsViewModel = viewModel(
        key = "offline_${mediaType}_$mediaId",
        factory = run {
            val app = LocalContext.current.applicationContext as NisTvApplication
            OfflineDetailsViewModel.provideFactory(
                mediaType = mediaType,
                mediaId = mediaId,
                offlineMediaDao = app.container.database.offlineMediaDao(),
                downloadDao = app.container.database.downloadDao(),
                offlineMediaRepository = app.container.offlineMediaRepository,
                watchLaterRepository = app.container.watchLaterRepository,
                recommendationEngine = app.container.recommendationEngine,
                trackedSeriesDao = app.container.database.trackedSeriesDao(),
                autopilotRuleDao = app.container.database.autopilotRuleDao(),
                autopilotManager = app.container.autopilotManager
            )
        }
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDeleteSeasonDialog by remember { mutableStateOf(false) }
    var isOverviewExpanded by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }

    val media = uiState.media
    val isMovie = mediaType.equals("movie", ignoreCase = true)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NisTheme.colors.background)
    ) {
        if (uiState.isLoading && media == null && uiState.movieDownload == null && uiState.allSeriesDownloads.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NisTheme.colors.primary)
            }
            return
        }

        // Main Scrollable Netflix-style details content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = NisDimens.MaxContentWidth)
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // 1. Hero Backdrop Banner with gradient fade
            item(key = "hero_banner") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 340.dp)
                ) {
                    val backdropFile = media?.localBackdropPath?.let { File(it) }
                    if (backdropFile != null && backdropFile.exists()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(backdropFile)
                                .crossfade(true)
                                .build(),
                            contentDescription = media.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(NisTheme.colors.surface)
                        )
                    }

                    // Multi-layer Gradient Fades for cinematic contrast
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        if (NisTheme.colors.isDark) NisScrimMedium else NisTheme.colors.background.copy(alpha = 0.85f),
                                        Color.Transparent,
                                        if (NisTheme.colors.isDark) NisScrimStrong else NisTheme.colors.background.copy(alpha = 0.70f),
                                        NisTheme.colors.background
                                    )
                                )
                            )
                    )

                    // Hero Content Row (Poster thumbnail + Title + Badges)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Poster Thumbnail (Strictly Local)
                        Box(
                            modifier = Modifier
                                .width(96.dp)
                                .height(144.dp)
                                .clip(NisShapes.small)
                                .background(NisTheme.colors.surfaceElevated)
                                .border(1.dp, NisTheme.colors.border, NisShapes.small)
                        ) {
                            val posterFile = media?.localPosterPath?.let { File(it) }
                            if (posterFile != null && posterFile.exists()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(posterFile)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = media.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = NisIcons.LiveTv,
                                        contentDescription = null,
                                        tint = NisTheme.colors.textMuted,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }

                        // Title, Year, Rating, Badges
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val titleText = media?.title ?: if (isMovie) {
                                uiState.movieDownload?.title ?: "Movie"
                            } else {
                                uiState.allSeriesDownloads.firstOrNull()?.effectiveSeriesName ?: "TV Series"
                            }

                            Text(
                                text = titleText,
                                color = NisTheme.colors.textPrimary,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            // Tagline
                            if (!media?.tagline.isNullOrBlank()) {
                                Text(
                                    text = media.tagline,
                                    color = NisTheme.colors.textMuted,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Badges Row
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val rating = media?.rating ?: uiState.movieDownload?.rating ?: 0.0
                                if (rating > 0.0) {
                                    Surface(
                                        shape = NisBadgeShape,
                                        color = NisTheme.colors.warning.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "★ ${String.format(Locale.US, "%.1f", rating)}",
                                            color = NisTheme.colors.warning,
                                            style = NisTheme.textExtras.micro,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                val releaseYear = media?.releaseDate?.take(4)
                                    ?: uiState.movieDownload?.releaseDate?.take(4)
                                if (!releaseYear.isNullOrBlank()) {
                                    Surface(
                                        shape = NisBadgeShape,
                                        color = NisTheme.colors.surfaceElevated
                                    ) {
                                        Text(
                                            text = releaseYear,
                                            color = NisTheme.colors.textSecondary,
                                            style = NisTheme.textExtras.micro,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                val runtime = media?.runtimeMinutes ?: uiState.movieDownload?.runtimeMinutes
                                if (runtime != null && runtime > 0) {
                                    Surface(
                                        shape = NisBadgeShape,
                                        color = NisTheme.colors.surfaceElevated
                                    ) {
                                        Text(
                                            text = "${runtime}m",
                                            color = NisTheme.colors.textSecondary,
                                            style = NisTheme.textExtras.micro,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Surface(
                                    shape = NisBadgeShape,
                                    color = NisTheme.colors.success.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "OFFLINE",
                                        color = NisTheme.colors.success,
                                        style = NisTheme.textExtras.micro,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Total Downloaded Size
                            val totalSize = if (isMovie) {
                                uiState.movieDownload?.fileSizeBytes ?: 0L
                            } else {
                                uiState.allSeriesDownloads.sumOf { it.fileSizeBytes.coerceAtLeast(it.downloadedBytes) }
                            }
                            if (totalSize > 0) {
                                Text(
                                    text = "Total Downloaded: ${DownloadEntity.formatBytes(totalSize)}",
                                    color = NisAccentCyan,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // 2. Big Action Buttons: Play / Resume & Watch Later & Delete
            item(key = "action_buttons") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val targetDownload = if (isMovie) uiState.movieDownload else uiState.nextEpisodeToPlay
                    val isResume = uiState.lastPlaybackPositionMs > 10_000L

                    val playButtonLabel = if (isMovie) {
                        if (isResume) "Resume Movie" else "Play Movie"
                    } else {
                        val nextEp = uiState.nextEpisodeToPlay
                        if (nextEp != null) {
                            val epTag = "S${nextEp.seasonNumber ?: 1} E${nextEp.episodeNumber ?: 1}"
                            if (isResume) "Resume $epTag" else "Play $epTag"
                        } else {
                            "Play Download"
                        }
                    }

                    // Big Primary Play Button
                    Button(
                        onClick = {
                            if (targetDownload != null) {
                                onPlayDownload(targetDownload.id)
                            } else {
                                Toast.makeText(context, "No completed episode ready to play", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = NisThumbnailShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NisAccentPrimary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = NisIcons.Play,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = playButtonLabel,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }

                    // Secondary Action Row: Watch Later Bookmark & Delete Download
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Watch Later Button
                        val titleForSave = media?.title ?: if (isMovie) uiState.movieDownload?.title ?: "Movie" else "TV Series"
                        NisBookmarkButton(
                            mediaType = mediaType,
                            mediaId = uiState.tmdbId,
                            title = titleForSave,
                            posterPath = media?.localPosterPath,
                            backdropPath = media?.localBackdropPath,
                            overview = media?.overview,
                            releaseYear = media?.releaseDate?.take(4),
                            voteAverage = media?.rating,
                            genres = media?.genres,
                            size = 42.dp,
                            iconSize = 20.dp,
                            showBackground = true,
                            modifier = Modifier
                        )

                        // Delete Download Button
                        OutlinedButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            shape = NisThumbnailShape,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = NisError
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NisError.copy(alpha = 0.6f))
                        ) {
                            Icon(
                                imageVector = NisIcons.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = NisError
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isMovie) "Delete Download" else "Delete All Episodes",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // 3. Overview & Genre Pills
            item(key = "overview_genres") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val genresStr = media?.genres ?: uiState.movieDownload?.genres
                    if (!genresStr.isNullOrBlank()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            genresStr.split(",").take(4).forEach { genre ->
                                val clean = genre.trim()
                                if (clean.isNotBlank()) {
                                    Surface(
                                        shape = NisBadgeShape,
                                        color = NisSurfaceElevated,
                                        border = androidx.compose.foundation.BorderStroke(0.5.dp, NisBorder)
                                    ) {
                                        Text(
                                            text = clean,
                                            color = NisTextSecondary,
                                            style = NisTheme.textExtras.micro,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    val overviewText = media?.overview?.ifBlank { null }
                        ?: uiState.movieDownload?.overview
                        ?: uiState.allSeriesDownloads.firstOrNull()?.overview
                    if (!overviewText.isNullOrBlank()) {
                        Text(
                            text = overviewText,
                            color = NisTextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = if (isOverviewExpanded) Int.MAX_VALUE else 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable { isOverviewExpanded = !isOverviewExpanded }
                        )
                        if (overviewText.length > 120) {
                            Text(
                                text = if (isOverviewExpanded) "Show less" else "Read more",
                                color = NisAccentPrimary,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { isOverviewExpanded = !isOverviewExpanded }
                            )
                        }
                    }

                    // Directors / Creators
                    if (uiState.directorsOrCreators.isNotBlank()) {
                        Text(
                            text = if (isMovie) "Director: ${uiState.directorsOrCreators}" else "Creators: ${uiState.directorsOrCreators}",
                            color = NisTextMuted,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            // 4. Cast Row (Strictly Local Profile Images)
            if (uiState.cast.isNotEmpty()) {
                item(key = "cast_header") {
                    Text(
                        text = "CAST",
                        color = NisTextSecondary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp)
                    )
                }

                item(key = "cast_rail") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.cast, key = { it.id }) { actor ->
                            OfflineCastCard(cast = actor)
                        }
                    }
                }
            }

            // 5. TV Series Season Selector & Episodes List
            if (!isMovie) {
                // Upcoming Episode Radar indicator
                val tracked = uiState.trackedSeries
                if (!tracked?.nextAirDate.isNullOrBlank()) {
                    item(key = "upcoming_episode_banner") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .background(NisSurfaceElevated, NisShapes.small)
                                .border(0.5.dp, NisBorder, NisShapes.small)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = NisIcons.Timer,
                                contentDescription = null,
                                tint = NisAccentPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            val prefix = tracked?.lastKnownEpisodeKey?.let { "Upcoming: $it - " } ?: "Upcoming: "
                            Text(
                                text = "$prefix${tracked?.nextAirDate}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = NisTextPrimary
                            )
                        }
                    }
                }

                // Notify me about new episodes toggle
                item(key = "episode_radar_toggle") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clip(NisThumbnailShape)
                            .background(NisSurface)
                            .border(0.5.dp, NisBorder, NisThumbnailShape)
                            .clickable {
                                val current = tracked?.notifyEnabled ?: true
                                viewModel.setNotifyEnabled(!current)
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = if (tracked?.notifyEnabled != false) NisIcons.NotificationsActive else NisIcons.Notifications,
                                contentDescription = null,
                                tint = if (tracked?.notifyEnabled != false) NisAccentPrimary else NisTextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Notify me about new episodes",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = NisTextPrimary
                                )
                                Text(
                                    text = "Alerts when next episode airs",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NisTextMuted
                                )
                            }
                        }
                        Switch(
                            checked = tracked?.notifyEnabled ?: true,
                            onCheckedChange = { viewModel.setNotifyEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NisAccentPrimary,
                                checkedTrackColor = NisAccentPrimary.copy(alpha = 0.3f),
                                uncheckedThumbColor = NisTextMuted,
                                uncheckedTrackColor = NisSurfaceElevated
                            )
                        )
                    }
                }

                // Smart Downloads Autopilot Settings Card
                item(key = "autopilot_settings") {
                    AutopilotSettingsCard(
                        rule = uiState.autopilotRule,
                        seriesKey = viewModel.seriesKey,
                        onSaveRule = { viewModel.updateAutopilotRule(it) }
                    )
                }

                // Season Selector Tabs / Chips
                if (uiState.seasons.isNotEmpty()) {
                    item(key = "season_chips") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(uiState.seasons, key = { it.seasonNumber }) { season ->
                                    val isSelected = season.seasonNumber == uiState.selectedSeasonNumber
                                    NisCategoryChip(
                                        text = season.name,
                                        isSelected = isSelected,
                                        onClick = { viewModel.selectSeason(season.seasonNumber) }
                                    )
                                }
                            }

                            // Delete season action
                            TextButton(
                                onClick = { showDeleteSeasonDialog = true },
                                colors = ButtonDefaults.textButtonColors(contentColor = NisError)
                            ) {
                                Text(
                                    text = "Delete Season",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Episode List
                val seasonEpisodes = uiState.episodes
                if (seasonEpisodes.isEmpty()) {
                    item(key = "no_episodes_season") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No downloaded episodes found for Season ${uiState.selectedSeasonNumber}.",
                                color = NisTextMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                } else {
                    items(seasonEpisodes, key = { "ep_${it.seasonNumber}_${it.episodeNumber}" }) { episode ->
                        val downloadKey = "${episode.seasonNumber}_${episode.episodeNumber}"
                        val dl = uiState.downloadedEpisodesMap[downloadKey]
                        val isDownloaded = dl != null && (dl.status.equals(DownloadStatus.COMPLETED.name, ignoreCase = true) || (dl.fileSizeBytes > 0 && dl.downloadedBytes >= dl.fileSizeBytes))

                        OfflineEpisodeCard(
                            episode = episode,
                            download = dl,
                            isDownloaded = isDownloaded,
                            onPlay = {
                                if (dl != null) {
                                    onPlayDownload(dl.id)
                                }
                            }
                        )
                    }
                }
            }
        }

        // Floating Top Bar with Back and Options
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = NisDimens.MaxContentWidth)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Floating Back Button
            Box(
                modifier = Modifier
                    .size(NisDimens.TouchTarget)
                    .nisPressable(role = Role.Button, onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(NisScrimStrong)
                        .border(1.dp, NisBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = NisIcons.Back,
                        contentDescription = "Back",
                        tint = NisTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Floating Options Menu
            Box(
                modifier = Modifier
                    .size(NisDimens.TouchTarget)
                    .nisPressable(role = Role.Button, onClick = { showOptionsMenu = true }),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(NisScrimStrong)
                        .border(1.dp, NisTheme.colors.border, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = NisIcons.MoreVert,
                        contentDescription = "More options",
                        tint = NisTheme.colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = showOptionsMenu,
                    onDismissRequest = { showOptionsMenu = false },
                    modifier = Modifier.background(NisTheme.colors.surface)
                ) {
                DropdownMenuItem(
                    text = { Text("Refresh offline details", color = NisTheme.colors.textPrimary, style = MaterialTheme.typography.bodyMedium) },
                    onClick = {
                        showOptionsMenu = false
                        viewModel.refreshOfflineDetails()
                        Toast.makeText(context, "Refreshing offline details...", Toast.LENGTH_SHORT).show()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = NisIcons.Refresh,
                            contentDescription = null,
                            tint = NisTheme.colors.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }
    }
}

    // Delete Confirmation Dialog for Entire Movie or All Episodes
    if (showDeleteDialog) {
        val titleText = if (isMovie) "Delete Movie?" else "Delete All Episodes?"
        val descText = if (isMovie) {
            "Are you sure you want to delete this downloaded movie from your device?"
        } else {
            "Are you sure you want to delete all downloaded episodes of this series? This will free up storage space."
        }

        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = NisTheme.colors.surface,
            title = {
                Text(text = titleText, color = NisTheme.colors.textPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            },
            text = {
                Text(text = descText, color = NisTheme.colors.textSecondary, style = MaterialTheme.typography.bodySmall)
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        if (isMovie) {
                            viewModel.deleteMovieDownload(onComplete = onBack)
                        } else {
                            viewModel.deleteAllSeriesEpisodes(onComplete = onBack)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NisTheme.colors.error)
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = NisTheme.colors.textSecondary)
                }
            }
        )
    }

    // Delete Season Dialog
    if (showDeleteSeasonDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteSeasonDialog = false },
            containerColor = NisTheme.colors.surface,
            title = {
                Text(
                    text = "Delete Season ${uiState.selectedSeasonNumber}?",
                    color = NisTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text(
                    text = "This will remove all downloaded episodes in Season ${uiState.selectedSeasonNumber} from your device storage.",
                    color = NisTheme.colors.textSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteSeasonDialog = false
                        viewModel.deleteSeason(uiState.selectedSeasonNumber)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NisTheme.colors.error)
                ) {
                    Text("Delete Season", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSeasonDialog = false }) {
                    Text("Cancel", color = NisTheme.colors.textSecondary)
                }
            }
        )
    }
}

@Composable
private fun OfflineCastCard(cast: OfflineCastMember) {
    Column(
        modifier = Modifier.width(82.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(NisSurfaceElevated)
                .border(1.dp, NisBorder, CircleShape)
        ) {
            val file = cast.localProfilePath?.let { File(it) }
            if (file != null && file.exists()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(file)
                        .crossfade(true)
                        .build(),
                    contentDescription = cast.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = NisIcons.Profile,
                        contentDescription = null,
                        tint = NisTextMuted,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = cast.name,
            color = NisTextPrimary,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (!cast.character.isNullOrBlank()) {
            Text(
                text = cast.character,
                color = NisTextMuted,
                style = NisTheme.textExtras.micro,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun OfflineEpisodeCard(
    episode: OfflineEpisodeEntity,
    download: DownloadEntity?,
    isDownloaded: Boolean,
    onPlay: () -> Unit
) {
    val alpha = if (isDownloaded) 1f else 0.45f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(NisMediaCardShape)
            .clickable(enabled = isDownloaded, onClick = onPlay),
        colors = CardDefaults.cardColors(containerColor = NisSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, NisBorder),
        shape = NisMediaCardShape
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Still Image Thumbnail (Strictly Local)
            Box(
                modifier = Modifier
                    .width(112.dp)
                    .height(64.dp)
                    .clip(NisShapes.small)
                    .background(NisSurfaceElevated)
            ) {
                val stillFile = episode.localStillPath?.let { File(it) }
                if (stillFile != null && stillFile.exists()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(stillFile)
                            .crossfade(true)
                            .build(),
                        contentDescription = episode.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(NisIcons.Play, contentDescription = null, tint = NisTextMuted, modifier = Modifier.size(24.dp))
                    }
                }

                if (isDownloaded) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(NisScrimStrong),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = NisIcons.Play,
                            contentDescription = "Play",
                            tint = NisAccentPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Episode Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "S${episode.seasonNumber} E${episode.episodeNumber} • ${episode.name}",
                    color = NisTextPrimary.copy(alpha = alpha),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (download != null) {
                    Text(
                        text = if (isDownloaded) download.formattedTotal else "Downloading...",
                        color = if (isDownloaded) NisSuccess else NisAccentAmber,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Text(
                        text = "Not downloaded",
                        color = NisTextMuted,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                if (episode.overview.isNotBlank()) {
                    Text(
                        text = episode.overview,
                        color = NisTextSecondary.copy(alpha = alpha),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun AutopilotSettingsCard(
    rule: AutopilotRuleEntity?,
    seriesKey: String,
    onSaveRule: (AutopilotRuleEntity) -> Unit
) {
    val currentRule = rule ?: AutopilotRuleEntity(
        seriesKey = seriesKey,
        enabled = false,
        keepAhead = 2,
        wifiOnly = true,
        chargingOnly = false,
        minFreeBytes = 2L * 1024L * 1024L * 1024L,
        autoDeleteWatched = false,
        deleteAfterDays = 3
    )
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = NisSurface),
        shape = NisMediaCardShape
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = NisIcons.Download,
                        contentDescription = null,
                        tint = if (currentRule.enabled) NisAccentPrimary else NisTextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Smart Downloads Autopilot",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = NisTextPrimary
                        )
                        Text(
                            text = if (currentRule.enabled) "Keeps next ${currentRule.keepAhead} unwatched episodes downloaded" else "Automatically downloads next episodes",
                            style = MaterialTheme.typography.labelSmall,
                            color = NisTextMuted
                        )
                    }
                }
                Switch(
                    checked = currentRule.enabled,
                    onCheckedChange = { enabled ->
                        onSaveRule(currentRule.copy(enabled = enabled))
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NisAccentPrimary,
                        checkedTrackColor = NisAccentPrimary.copy(alpha = 0.3f),
                        uncheckedThumbColor = NisTextMuted,
                        uncheckedTrackColor = NisSurfaceElevated
                    )
                )
            }

            if (currentRule.enabled || isExpanded) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = NisBorder, thickness = 0.5.dp)
                Spacer(Modifier.height(12.dp))

                // Keep ahead count (1, 2, 3, 5)
                Text(
                    text = "Keep next unwatched episodes:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = NisTextSecondary
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 2, 3, 5).forEach { count ->
                        val isSelected = currentRule.keepAhead == count
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSaveRule(currentRule.copy(keepAhead = count)) },
                            label = { Text("$count ep") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NisAccentPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Wi-Fi only toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Wi-Fi only", style = MaterialTheme.typography.labelSmall, color = NisTextPrimary)
                    Switch(
                        checked = currentRule.wifiOnly,
                        onCheckedChange = { onSaveRule(currentRule.copy(wifiOnly = it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NisAccentPrimary,
                            checkedTrackColor = NisAccentPrimary.copy(alpha = 0.3f)
                        )
                    )
                }

                // Charging only toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Charging only", style = MaterialTheme.typography.labelSmall, color = NisTextPrimary)
                    Switch(
                        checked = currentRule.chargingOnly,
                        onCheckedChange = { onSaveRule(currentRule.copy(chargingOnly = it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NisAccentPrimary,
                            checkedTrackColor = NisAccentPrimary.copy(alpha = 0.3f)
                        )
                    )
                }

                // Min free storage floor (1 GB, 2 GB, 5 GB, 10 GB)
                Spacer(Modifier.height(8.dp))
                val gbFloor = (currentRule.minFreeBytes / (1024L * 1024L * 1024L)).toInt().coerceAtLeast(1)
                Text(
                    text = "Storage safety floor ($gbFloor GB free required):",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = NisTextSecondary
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 2, 5, 10).forEach { gb ->
                        val isSelected = gbFloor == gb
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                onSaveRule(currentRule.copy(minFreeBytes = gb.toLong() * 1024L * 1024L * 1024L))
                            },
                            label = { Text("$gb GB") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NisAccentPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                // Auto-delete watched
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-delete watched episodes", style = MaterialTheme.typography.labelSmall, color = NisTextPrimary)
                        Text("Removes watched episodes after grace period", style = NisTheme.textExtras.micro, color = NisTextMuted)
                    }
                    Switch(
                        checked = currentRule.autoDeleteWatched,
                        onCheckedChange = { onSaveRule(currentRule.copy(autoDeleteWatched = it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NisAccentPrimary,
                            checkedTrackColor = NisAccentPrimary.copy(alpha = 0.3f)
                        )
                    )
                }

                if (currentRule.autoDeleteWatched) {
                    Spacer(Modifier.height(6.dp))
                    Text("Delete after:", style = MaterialTheme.typography.labelSmall, color = NisTextSecondary)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1, 3, 7, 14).forEach { days ->
                            val isSelected = currentRule.deleteAfterDays == days
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSaveRule(currentRule.copy(deleteAfterDays = days)) },
                                label = { Text("$days d") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NisAccentPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OfflineDetailsScreen(
    mediaType: String,
    mediaId: Long,
    onBack: () -> Unit,
    onPlayDownload: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OfflineDetailsScreen(
        mediaType = mediaType,
        mediaId = mediaId.toString(),
        onBack = onBack,
        onPlayDownload = onPlayDownload,
        modifier = modifier
    )
}
