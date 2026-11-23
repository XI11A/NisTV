@file:OptIn(ExperimentalSharedTransitionApi::class)
package com.nis.nistv.feature.details

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.*
import kotlinx.coroutines.delay
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.nis.nistv.core.designsystem.component.*
import com.nis.nistv.core.designsystem.icon.NisIcons
import com.nis.nistv.core.designsystem.theme.*
import com.nis.nistv.core.designsystem.transition.SharedOrigins
import com.nis.nistv.core.designsystem.transition.nisContentEnterExit
import com.nis.nistv.core.designsystem.transition.nisRenderInOverlay
import com.nis.nistv.core.database.entity.DownloadEntity
import com.nis.nistv.data.download.provider.DownloadItemCandidate
import com.nis.nistv.data.tmdb.model.*
import com.nis.nistv.feature.discover.DiscoverMediaPosterCard
import com.nis.nistv.feature.details.components.InAppTrailerDialog

@Composable
fun MediaDetailsScreen(
    mediaType: TmdbMediaType,
    mediaId: Long,
    origin: String = "",
    onBack: () -> Unit,
    onSimilarClick: (TmdbMediaType, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as NisTvApplication
    val viewModel: MediaDetailsViewModel = viewModel(
        factory = MediaDetailsViewModel.provideFactory(
            repository = app.container.tmdbRepository,
            downloadManager = app.container.downloadManager,
            afdsProvider = app.container.afdsDownloadProvider,
            userPreferences = app.container.userPreferences,
            afdsAuthService = app.container.afdsAuthService,
            recommendationEngine = app.container.recommendationEngine,
            watchLaterRepository = app.container.watchLaterRepository,
            mediaPreviewStore = app.container.mediaPreviewStore,
            trackedSeriesDao = app.container.database.trackedSeriesDao()
        )
    )

    val uiState by viewModel.uiState.collectAsState()
    val headerPreview = uiState.detailsPreview ?: uiState.preview
    var showConfirmCloseSearchDialog by remember { mutableStateOf(false) }
    var afdsEmailInput by remember { mutableStateOf("") }
    var afdsOtpInput by remember { mutableStateOf("") }
    var afdsManualTokenInput by remember { mutableStateOf("") }
    var showManualSection by remember { mutableStateOf(false) }
    var activeTrailerVideo by remember { mutableStateOf<TmdbVideoDto?>(null) }

    LaunchedEffect(mediaType, mediaId) {
        viewModel.loadDetails(mediaType, mediaId)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NisTheme.colors.background)
    ) {
        if (headerPreview == null && uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = NisTheme.colors.primary)
            }
        } else if (headerPreview == null && uiState.errorMessage != null) {
            NisErrorState(
                title = "Failed to Load Details",
                message = uiState.errorMessage ?: "Network error",
                onRetry = { viewModel.loadDetails(mediaType, mediaId) },
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = NisDimens.MaxContentWidth)
                    .align(Alignment.TopCenter),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // 1. Shared Poster Header (Instant from MediaPreviewStore)
                item(key = "header") {
                    if (headerPreview != null) {
                        MediaDetailsHeader(
                            preview = headerPreview,
                            origin = origin
                        )
                    } else {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(400.dp)
                                .background(NisTheme.colors.surface)
                        )
                    }
                }

                // 2. Body Content (Soft fade & slide enter)
                item(key = "body") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .nisContentEnterExit()
                    ) {
                        if (uiState.isLoading && uiState.movieDetails == null && uiState.tvDetails == null) {
                            MediaDetailsBodySkeleton()
                        } else {
                            // Title, Meta, and Action Buttons
                            DetailsHeaderSection(
                                uiState = uiState,
                                showTitle = headerPreview == null,
                                onTrailerClick = {
                                    val trailer = uiState.trailerVideo
                                    if (trailer != null && trailer.key.isNotBlank()) {
                                        activeTrailerVideo = trailer
                                    } else {
                                        val fallbackKey = uiState.trailerUrl?.let { url ->
                                            val vParam = Regex("[?&]v=([^&]+)").find(url)?.groupValues?.get(1)
                                            vParam ?: if (url.contains("youtu.be/")) url.substringAfter("youtu.be/").substringBefore("?") else null
                                        }
                                        if (!fallbackKey.isNullOrBlank()) {
                                            activeTrailerVideo = TmdbVideoDto(
                                                id = "fallback",
                                                key = fallbackKey,
                                                name = "Official Trailer",
                                                site = "YouTube",
                                                type = "Trailer"
                                            )
                                        } else {
                                            Toast.makeText(context, "No official trailer available", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onDownloadClick = {
                                    viewModel.onDownloadClick()
                                },
                                onWatchLaterClick = {
                                    viewModel.toggleWatchLater()
                                },
                                onEpisodeRadarToggle = { enabled ->
                                    viewModel.toggleEpisodeRadarNotification(enabled)
                                }
                            )

                            // 3. Where to Watch (Streaming Providers for region)
                            uiState.regionWatchProviders?.let { providers ->
                                if (providers.flatrate.isNotEmpty() || providers.rent.isNotEmpty() || providers.buy.isNotEmpty()) {
                                    WhereToWatchSection(
                                        region = uiState.userRegion,
                                        providers = providers
                                    )
                                }
                            }

                            // 4. TV Series Seasons & Episodes Selector
                            if (mediaType == TmdbMediaType.TV && uiState.tvDetails != null) {
                                val tv = uiState.tvDetails!!
                                if (tv.seasons.isNotEmpty()) {
                                    TvSeasonsSection(
                                        seasons = tv.seasons,
                                        selectedSeasonNumber = uiState.selectedSeasonNumber,
                                        onSelectSeason = { sNum ->
                                            viewModel.loadSeason(sNum)
                                        }
                                    )

                                    uiState.currentSeasonDetails?.let { season ->
                                        season.episodes.forEach { episode ->
                                            EpisodeRowItem(
                                                episode = episode,
                                                onDownloadEpisode = {
                                                    viewModel.onDownloadEpisode(
                                                        seasonNumber = episode.seasonNumber,
                                                        episodeNumber = episode.episodeNumber,
                                                        episodeName = episode.name
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // 5. Cast Section
                            if (uiState.cast.isNotEmpty()) {
                                CastRailSection(cast = uiState.cast)
                            }

                            // 6. Recommendations Section
                            if (uiState.recommendations.isNotEmpty()) {
                                Column(modifier = Modifier.padding(vertical = 12.dp)) {
                                    NisSectionHeader(
                                        title = "Recommendations",
                                        subtitle = "Titles recommended based on this ${if (uiState.mediaType == TmdbMediaType.MOVIE) "movie" else "show"}"
                                    )
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(uiState.recommendations, key = { "rec_${it.mediaType}_${it.id}" }) { item ->
                                            DiscoverMediaPosterCard(
                                                item = item,
                                                origin = SharedOrigins.SIMILAR,
                                                onClick = { onSimilarClick(item.mediaType, item.id) }
                                            )
                                        }
                                    }
                                }
                            }

                            // 7. Similar Content Section
                            if (uiState.similar.isNotEmpty()) {
                                Column(modifier = Modifier.padding(vertical = 12.dp)) {
                                    NisSectionHeader(
                                        title = "Similar Content",
                                        subtitle = "Titles with similar genre and style"
                                    )
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(uiState.similar, key = { "sim_${it.mediaType}_${it.id}" }) { item ->
                                            DiscoverMediaPosterCard(
                                                item = item,
                                                origin = SharedOrigins.SIMILAR,
                                                onClick = { onSimilarClick(item.mediaType, item.id) }
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

        // Floating Back Button top-start with accessible 48dp touch target
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 16.dp, top = 8.dp)
                .defaultMinSize(minWidth = NisDimens.TouchTarget, minHeight = NisDimens.TouchTarget)
                .nisRenderInOverlay(zIndex = 4f),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (NisTheme.colors.isDark) NisScrimMedium else NisTheme.colors.surfaceElevated.copy(alpha = 0.85f))
                    .border(1.dp, NisTheme.colors.borderSubtle, CircleShape)
                    .nisPressable(role = Role.Button, onClickLabel = "Back", onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = NisIcons.Back,
                    contentDescription = "Back",
                    tint = NisTheme.colors.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Animated Download Started Notification Banner
        AnimatedVisibility(
            visible = uiState.downloadStartedNotification != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn() + scaleIn(initialScale = 0.9f),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut() + scaleOut(targetScale = 0.9f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        ) {
            uiState.downloadStartedNotification?.let { notif ->
                DownloadStartedBanner(
                    notification = notif,
                    onDismiss = { viewModel.dismissDownloadStartedNotification() }
                )
            }
        }
    }

    // In-App Trailer Player
    activeTrailerVideo?.let { trailer ->
        InAppTrailerDialog(
            videoKey = trailer.key,
            title = uiState.title,
            trailerName = trailer.name,
            onDismiss = { activeTrailerVideo = null }
        )
    }

    // Download Candidate Selection Dialog
    if (uiState.showDownloadDialog) {
        DownloadSourceSelectionDialog(
            title = uiState.downloadDialogTitle ?: uiState.title,
            isLoading = uiState.isSearchingDownload,
            message = uiState.downloadDialogMessage,
            candidates = uiState.downloadCandidates,
            onSelectCandidate = { viewModel.selectCandidateToDownload(it) },
            onDismiss = {
                // Confirm before closing search dialog
                showConfirmCloseSearchDialog = true
            }
        )
    }

    // Confirmation before closing search dialog
    if (showConfirmCloseSearchDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmCloseSearchDialog = false },
            containerColor = NisTheme.colors.surface,
            title = {
                Text(
                    text = "Close Download Search?",
                    color = NisTheme.colors.textPrimary,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to close the download search?",
                    color = NisTheme.colors.textSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmCloseSearchDialog = false
                        viewModel.dismissDownloadDialog()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NisTheme.colors.primary)
                ) {
                    Text("Yes, Close", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmCloseSearchDialog = false }) {
                    Text("Keep Searching", color = NisTheme.colors.textSecondary)
                }
            }
        )
    }

    // Daily Download Limit Exceeded Dialog (from AFDS Server Quota)
    if (uiState.showDownloadLimitDialog && uiState.downloadLimitExceededInfo != null) {
        val info = uiState.downloadLimitExceededInfo!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissDownloadLimitDialog() },
            containerColor = NisTheme.colors.surface,
            title = {
                Text(
                    text = "Daily Download Limit Reached",
                    color = NisTheme.colors.textPrimary,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "You have reached your AFDS server daily download limit (${info.used} / ${info.limit} links used today).\n\n" +
                               "The AFDS server resets daily link quotas automatically at midnight.",
                        color = NisTheme.colors.textSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Quota is synced directly from your AFDS account.",
                        color = NisTheme.colors.primary,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissDownloadLimitDialog() },
                    colors = ButtonDefaults.buttonColors(containerColor = NisTheme.colors.primary)
                ) {
                    Text("OK", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {}
        )
    }

    // Unified AFDS Sign-In Dialog for Downloads
    if (uiState.showAfdsLoginDialog) {
        val app = LocalContext.current.applicationContext as NisTvApplication
        AfdsAuthGateDialog(
            afdsAuthService = app.container.afdsAuthService,
            googleAuthManager = app.container.googleAuthManager,
            onSuccess = { viewModel.onAfdsAuthSuccess() },
            onDismiss = { viewModel.dismissAfdsLoginDialog() }
        )
    }
}

@Composable
fun DetailsHeroBackdrop(
    backdropPath: String?,
    title: String,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
    ) {
        val fullUrl = backdropPath?.let { "https://image.tmdb.org/t/p/w780$it" }
        if (!fullUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(fullUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NisTheme.colors.surfaceElevated)
            )
        }

        // Ambient Vignette
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
    }
}

@Composable
fun MediaDetailsBodySkeleton(modifier: Modifier = Modifier) {
    val brush = rememberShimmerBrush()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(width = 54.dp, height = 24.dp).clip(NisBadgeShape).background(brush))
            Box(Modifier.size(width = 46.dp, height = 24.dp).clip(NisBadgeShape).background(brush))
            Box(Modifier.size(width = 70.dp, height = 24.dp).clip(NisBadgeShape).background(brush))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.weight(1f).height(44.dp).clip(NisButtonShape).background(brush))
            Box(Modifier.weight(1f).height(44.dp).clip(NisButtonShape).background(brush))
            Box(Modifier.size(44.dp).clip(NisButtonShape).background(brush))
        }
        Box(Modifier.size(width = 110.dp, height = 20.dp).clip(NisBadgeShape).background(brush))
        Box(Modifier.fillMaxWidth().height(14.dp).clip(NisBadgeShape).background(brush))
        Box(Modifier.fillMaxWidth(0.9f).height(14.dp).clip(NisBadgeShape).background(brush))
        Box(Modifier.fillMaxWidth(0.6f).height(14.dp).clip(NisBadgeShape).background(brush))
    }
}

@Composable
fun DetailsHeaderSection(
    uiState: MediaDetailsUiState,
    showTitle: Boolean = false,
    onTrailerClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onWatchLaterClick: () -> Unit,
    onEpisodeRadarToggle: (Boolean) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        val runtimeStr = when {
            uiState.movieDetails?.runtime != null && uiState.movieDetails.runtime > 0 -> {
                val hrs = uiState.movieDetails.runtime / 60
                val mins = uiState.movieDetails.runtime % 60
                if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"
            }
            uiState.tvDetails?.numberOfSeasons != null && uiState.tvDetails.numberOfSeasons > 0 -> {
                val sCount = uiState.tvDetails.numberOfSeasons
                if (sCount == 1) "1 Season" else "$sCount Seasons"
            }
            else -> null
        }

        if (showTitle) {
            // Badges: Rating, Year, Runtime / Seasons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(NisBadgeShape)
                        .background(NisAccentAmber)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (uiState.voteAverage > 0) String.format(java.util.Locale.US, "★ %.1f", uiState.voteAverage) else "New",
                        color = Color.Black,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                val yearText = uiState.releaseYear ?: uiState.preview?.releaseYear
                yearText?.let { year ->
                    Text(
                        text = year,
                        color = NisTheme.colors.textSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                runtimeStr?.let {
                    Text(
                        text = "•  $it",
                        color = NisTheme.colors.textSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (uiState.title.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.title,
                    color = NisTheme.colors.textPrimary,
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        } else {
            runtimeStr?.let {
                Text(
                    text = "Runtime: $it",
                    color = NisTheme.colors.textSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Genres
        if (uiState.genres.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                uiState.genres.take(4).forEach { genre ->
                    Box(
                        modifier = Modifier
                            .clip(NisBadgeShape)
                            .background(NisTheme.colors.surfaceElevated)
                            .border(1.dp, NisTheme.colors.borderSubtle, NisBadgeShape)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = genre.name,
                            color = NisTheme.colors.textSecondary,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons: Trailer, Download, Watch Later
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NisPrimaryButton(
                text = "Watch Trailer",
                icon = NisIcons.Play,
                onClick = onTrailerClick,
                modifier = Modifier.weight(1f)
            )

            NisSecondaryButton(
                text = "Download",
                icon = NisIcons.Download,
                onClick = onDownloadClick,
                modifier = Modifier.weight(1f)
            )

            NisBookmarkButton(
                mediaType = uiState.mediaType.value,
                mediaId = uiState.mediaId,
                title = uiState.title,
                posterPath = uiState.posterPath,
                backdropPath = uiState.backdropPath,
                overview = uiState.overview,
                releaseYear = uiState.releaseYear,
                voteAverage = uiState.voteAverage,
                genres = (uiState.movieDetails?.genres ?: uiState.tvDetails?.genres ?: emptyList()).joinToString(", ") { it.name },
                size = 44.dp,
                iconSize = 22.dp
            )

            if (uiState.mediaType == TmdbMediaType.TV) {
                IconButton(
                    onClick = {
                        onEpisodeRadarToggle(!uiState.isEpisodeRadarNotifyEnabled)
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(NisTheme.colors.surfaceElevated, CircleShape)
                ) {
                    Icon(
                        imageVector = if (uiState.isEpisodeRadarNotifyEnabled) NisIcons.NotificationsActive else NisIcons.Notifications,
                        contentDescription = "New episode alerts",
                        tint = if (uiState.isEpisodeRadarNotifyEnabled) NisTheme.colors.primary else NisTheme.colors.textMuted,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Synopsis / Overview
        if (uiState.overview.isNotBlank()) {
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Synopsis",
                color = NisTheme.colors.textPrimary,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = uiState.overview,
                color = NisTheme.colors.textSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun WhereToWatchSection(
    region: String,
    providers: TmdbCountryWatchProviders
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            text = "Where to Watch ($region)",
            color = NisTheme.colors.textPrimary,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(10.dp))

        val allProviders = (providers.flatrate + providers.rent + providers.buy).distinctBy { it.providerId }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(allProviders, key = { it.providerId }) { provider ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(60.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(NisThumbnailShape)
                            .background(NisTheme.colors.surface)
                            .border(1.dp, NisTheme.colors.borderSubtle, NisThumbnailShape)
                    ) {
                        if (!provider.fullLogoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(provider.fullLogoUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = provider.providerName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = provider.providerName,
                        color = NisTheme.colors.textMuted,
                        style = NisTheme.textExtras.micro,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun TvSeasonsSection(
    seasons: List<TmdbSeasonDto>,
    selectedSeasonNumber: Int,
    onSelectSeason: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        NisSectionHeader(
            title = "Episodes",
            subtitle = "Select season to browse episodes"
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(seasons.filter { it.seasonNumber > 0 }, key = { it.id }) { season ->
                NisCategoryChip(
                    text = "Season ${season.seasonNumber}",
                    isSelected = season.seasonNumber == selectedSeasonNumber,
                    onClick = { onSelectSeason(season.seasonNumber) }
                )
            }
        }
    }
}

@Composable
fun EpisodeRowItem(
    episode: TmdbEpisodeDto,
    onDownloadEpisode: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(NisMediaCardShape)
            .background(NisTheme.colors.surface)
            .border(1.dp, NisTheme.colors.borderSubtle, NisMediaCardShape)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(100.dp)
                .height(60.dp)
                .clip(NisThumbnailShape)
                .background(NisTheme.colors.surfaceElevated)
        ) {
            if (!episode.fullStillUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(episode.fullStillUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = episode.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${episode.episodeNumber}. ${episode.name}",
                color = NisTheme.colors.textPrimary,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (episode.overview.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = episode.overview,
                    color = NisTheme.colors.textSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        IconButton(onClick = onDownloadEpisode) {
            Icon(
                imageVector = NisIcons.Download,
                contentDescription = "Download Episode",
                tint = NisTheme.colors.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun CastRailSection(cast: List<TmdbCastMember>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        NisSectionHeader(
            title = "Top Cast",
            subtitle = "Actors and characters"
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(cast.take(15), key = { it.id }) { actor ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(70.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(NisTheme.colors.surface)
                            .border(1.dp, NisTheme.colors.borderSubtle, CircleShape)
                    ) {
                        if (!actor.fullProfileUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(actor.fullProfileUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = actor.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = actor.name.take(1),
                                    color = NisTheme.colors.textMuted,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = actor.name,
                        color = NisTheme.colors.textPrimary,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    actor.character?.let { char ->
                        Text(
                            text = char,
                            color = NisTheme.colors.textMuted,
                            style = NisTheme.textExtras.micro,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadSourceSelectionDialog(
    title: String,
    isLoading: Boolean,
    message: String?,
    candidates: List<DownloadItemCandidate>,
    onSelectCandidate: (DownloadItemCandidate) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NisTheme.colors.surface,
        title = {
            Text(
                text = "Download: $title",
                color = NisTheme.colors.textPrimary,
                style = MaterialTheme.typography.titleMedium,
                softWrap = true
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (candidates.isEmpty()) {
                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = NisTheme.colors.primary, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = message ?: "Searching download sources...",
                                    color = NisTheme.colors.textSecondary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    } else if (!message.isNullOrBlank()) {
                        Text(
                            text = message,
                            color = NisTheme.colors.textSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Available Sources (${candidates.size}):",
                            color = NisTheme.colors.textSecondary,
                            style = MaterialTheme.typography.labelSmall
                        )
                        if (isLoading) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = NisTheme.colors.primary,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Searching...",
                                    color = NisTheme.colors.primary,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    candidates.forEach { candidate ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(NisShapes.small)
                                .background(NisTheme.colors.surfaceElevated)
                                .border(1.dp, NisTheme.colors.borderSubtle, NisShapes.small)
                                .clickable { onSelectCandidate(candidate) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                                Text(
                                    text = candidate.title,
                                    color = NisTheme.colors.textPrimary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    softWrap = true
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${candidate.quality} • ${DownloadEntity.formatBytes(candidate.sizeBytes)}",
                                    color = NisTheme.colors.primary,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Icon(
                                imageVector = NisIcons.Download,
                                contentDescription = "Download",
                                tint = NisTheme.colors.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Searching loader at the bottom of the download list when searches are still running
                    if (isLoading) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(NisShapes.small)
                                .background(NisTheme.colors.surfaceElevated.copy(alpha = 0.6f))
                                .border(1.dp, NisTheme.colors.borderSubtle.copy(alpha = 0.5f), NisShapes.small)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = NisTheme.colors.primary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Searching for more sources...",
                                color = NisTheme.colors.textSecondary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = NisTheme.colors.textSecondary)
            }
        }
    )
}

@Composable
fun DownloadStartedBanner(
    notification: DownloadStartedNotification,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(notification) {
        delay(4000)
        onDismiss()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = NisBottomSheetShape,
        color = NisTheme.colors.surfaceElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, NisTheme.colors.success.copy(alpha = 0.6f)),
        shadowElevation = 14.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(NisThumbnailShape)
                    .background(NisTheme.colors.success.copy(alpha = 0.15f * pulseAlpha))
                    .border(1.dp, NisTheme.colors.success.copy(alpha = 0.5f * pulseAlpha), NisThumbnailShape),
                contentAlignment = Alignment.Center
            ) {
                if (!notification.posterUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(notification.posterUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(NisThumbnailShape)
                    )
                } else {
                    Icon(
                        imageVector = NisIcons.Download,
                        contentDescription = "Downloading",
                        tint = NisTheme.colors.success,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(NisTheme.colors.success.copy(alpha = pulseAlpha))
                    )
                    Text(
                        text = "Download Started",
                        color = NisTheme.colors.success,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                Text(
                    text = notification.title,
                    color = NisTheme.colors.textPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = notification.fileName,
                    color = NisTheme.colors.textSecondary,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = NisIcons.Close,
                    contentDescription = "Dismiss",
                    tint = NisTheme.colors.textSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
