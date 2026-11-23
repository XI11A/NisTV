@file:OptIn(ExperimentalSharedTransitionApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.nis.nistv.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.animation.core.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import com.nis.nistv.core.designsystem.component.NisLinearProgressBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import com.nis.nistv.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.nis.nistv.core.database.entity.ProfileEntity
import com.nis.nistv.core.designsystem.component.NisProfileAvatar
import com.nis.nistv.core.database.entity.WatchLaterEntity
import com.nis.nistv.core.database.entity.toTmdbMediaItem
import com.nis.nistv.NisTvApplication
import com.nis.nistv.core.database.entity.ChannelEntity
import com.nis.nistv.core.designsystem.component.NisLogo
import com.nis.nistv.core.designsystem.component.ChannelLogoTile
import com.nis.nistv.core.designsystem.component.ChannelRailSkeleton
import com.nis.nistv.core.designsystem.component.HeroBannerSkeleton
import com.nis.nistv.core.designsystem.component.NisCategoryChip
import com.nis.nistv.core.designsystem.component.NisLiveBadge
import com.nis.nistv.core.designsystem.component.NisPrimaryButton
import com.nis.nistv.core.designsystem.component.NisRailChannelCard
import com.nis.nistv.core.designsystem.component.NisSecondaryButton
import com.nis.nistv.core.designsystem.component.NisSectionHeader
import com.nis.nistv.core.designsystem.icon.NisIcons
import com.nis.nistv.core.designsystem.component.NisBackdropMediaCard
import com.nis.nistv.core.designsystem.component.NisPosterMediaCard
import com.nis.nistv.core.designsystem.theme.*
import com.nis.nistv.core.designsystem.transition.SharedOrigins
import com.nis.nistv.data.tmdb.model.MediaPreview
import com.nis.nistv.data.tmdb.model.TmdbMediaItem
import com.nis.nistv.data.tmdb.model.TmdbMediaType
import com.nis.nistv.data.tmdb.model.toPreview
import com.nis.nistv.feature.discover.DiscoverMediaRail
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onChannelClick: (String) -> Unit,
    onMediaClick: (MediaPreview, String) -> Unit = { _, _ -> },
    onContinueWatchingClick: (String) -> Unit = {},
    onSearchClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onManageProfiles: () -> Unit = {},
    onFavoritesClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.provideFactory(
            (LocalContext.current.applicationContext as NisTvApplication).container.channelRepository,
            (LocalContext.current.applicationContext as NisTvApplication).container.epgManager,
            (LocalContext.current.applicationContext as NisTvApplication).container.userPreferences,
            (LocalContext.current.applicationContext as NisTvApplication).container.tmdbRepository,
            (LocalContext.current.applicationContext as NisTvApplication).container.recommendationEngine,
            (LocalContext.current.applicationContext as NisTvApplication).container.database.downloadDao(),
            (LocalContext.current.applicationContext as NisTvApplication).container.database.playbackProgressDao(),
            (LocalContext.current.applicationContext as NisTvApplication).container.profileRepository
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    val app = LocalContext.current.applicationContext as NisTvApplication
    val googleUser by app.container.userPreferences.googleUser.collectAsState(initial = null)
    val activeProfile by app.container.profileRepository.activeProfile.collectAsState()
    val profiles by app.container.profileRepository.profiles.collectAsState(initial = emptyList())
    var showProfileSwitcherSheet by remember { mutableStateOf(false) }
    val watchLaterList by app.container.watchLaterRepository.getWatchLaterList().collectAsState(initial = emptyList<WatchLaterEntity>())
    val watchLaterMediaItems = remember(watchLaterList) {
        watchLaterList.map { it.toTmdbMediaItem() }
    }
    val lazyListState = rememberLazyListState()
    val isScrolled by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 40
        }
    }

    var showTonightSheet by remember { mutableStateOf(false) }

    // Reset scroll when switching category chips so content starts cleanly at the top
    androidx.compose.runtime.LaunchedEffect(uiState.selectedFilter) {
        lazyListState.scrollToItem(0)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NisTheme.colors.background)
    ) {
        if (uiState.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(top = 100.dp)
            ) {
                HeroBannerSkeleton(modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(modifier = Modifier.height(20.dp))
                ChannelRailSkeleton()
                Spacer(modifier = Modifier.height(20.dp))
                ChannelRailSkeleton()
            }
        } else {
            val topUsageRecommendations = remember(
                uiState.dynamicRecommendedForYou,
                uiState.becauseYouWatchedMedia,
                uiState.trendingMovies,
                uiState.trendingTv,
                uiState.featuredMedia
            ) {
                val usageList = (uiState.dynamicRecommendedForYou + uiState.becauseYouWatchedMedia).distinctBy { "${it.mediaType}_${it.id}" }
                val fallbackList = listOfNotNull(uiState.featuredMedia) + uiState.trendingMovies + uiState.trendingTv
                (usageList + fallbackList).distinctBy { "${it.mediaType}_${it.id}" }.take(6)
            }

            val topMovieRecommendations = remember(
                uiState.dynamicRecommendedForYou,
                uiState.becauseYouWatchedMedia,
                uiState.trendingMovies
            ) {
                val usageList = (uiState.dynamicRecommendedForYou.filter { it.mediaType == TmdbMediaType.MOVIE } +
                    uiState.becauseYouWatchedMedia.filter { it.mediaType == TmdbMediaType.MOVIE }).distinctBy { it.id }
                (usageList + uiState.trendingMovies).distinctBy { it.id }.take(6)
            }

            val topSeriesRecommendations = remember(
                uiState.dynamicRecommendedForYou,
                uiState.becauseYouWatchedMedia,
                uiState.trendingTv
            ) {
                val usageList = (uiState.dynamicRecommendedForYou.filter { it.mediaType == TmdbMediaType.TV } +
                    uiState.becauseYouWatchedMedia.filter { it.mediaType == TmdbMediaType.TV }).distinctBy { it.id }
                (usageList + uiState.trendingTv).distinctBy { it.id }.take(6)
            }

            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = NisDimens.MaxContentWidth)
                    .align(Alignment.TopCenter),
                verticalArrangement = Arrangement.spacedBy(NisDimens.Scale24),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                val filter = uiState.selectedFilter

                // 1. Dynamic Hero Carousel per selected category chip (Usage-based recommendations)
                when (filter) {
                    "All" -> {
                        if (topUsageRecommendations.isNotEmpty()) {
                            item(key = "hero_carousel_all") {
                                RecommendedHeroCarousel(
                                    items = topUsageRecommendations,
                                    recommendationReason = uiState.recommendationReason,
                                    becauseYouWatchedTitle = uiState.becauseYouWatchedTitle,
                                    onMediaClick = onMediaClick
                                )
                            }
                        } else {
                            item(key = "all_top_spacer") {
                                Spacer(modifier = Modifier.statusBarsPadding().height(105.dp))
                            }
                        }
                    }
                    "Movies" -> {
                        if (topMovieRecommendations.isNotEmpty()) {
                            item(key = "hero_carousel_movies") {
                                RecommendedHeroCarousel(
                                    items = topMovieRecommendations,
                                    recommendationReason = "TOP MOVIE PICKS FOR YOU",
                                    becauseYouWatchedTitle = uiState.becauseYouWatchedTitle,
                                    onMediaClick = onMediaClick
                                )
                            }
                        } else {
                            item(key = "movies_top_spacer") {
                                Spacer(modifier = Modifier.statusBarsPadding().height(105.dp))
                            }
                        }
                    }
                    "Series" -> {
                        if (topSeriesRecommendations.isNotEmpty()) {
                            item(key = "hero_carousel_series") {
                                RecommendedHeroCarousel(
                                    items = topSeriesRecommendations,
                                    recommendationReason = "TOP SERIES PICKS FOR YOU",
                                    becauseYouWatchedTitle = uiState.becauseYouWatchedTitle,
                                    onMediaClick = onMediaClick
                                )
                            }
                        } else {
                            item(key = "series_top_spacer") {
                                Spacer(modifier = Modifier.statusBarsPadding().height(105.dp))
                            }
                        }
                    }
                    "Live TV", "News" -> {
                        item(key = "livetv_top_spacer") {
                            Spacer(modifier = Modifier.statusBarsPadding().height(105.dp))
                        }
                    }
                }

                // 1b. Continue Watching (Completed downloads with 5%-95% playback progress)
                if (uiState.continueWatching.isNotEmpty()) {
                    item(key = "continue_watching_rail") {
                        HomeContinueWatchingRail(
                            items = uiState.continueWatching,
                            onItemClick = onContinueWatchingClick,
                            onRemoveItem = { item -> viewModel.removeFromContinueWatching(item.download.id) }
                        )
                    }
                }

                // 2. Recent Channels (last 10 distinct channels from watch_history, newest first)
                if ((filter == "All" || filter == "Live TV") && uiState.recentChannels.isNotEmpty()) {
                    item(key = "recent_channels_rail") {
                        HomeChannelRail(
                            title = "Recent Channels",
                            subtitle = "Jump back into your channels",
                            channels = uiState.recentChannels,
                            favoriteIds = uiState.favoriteChannelIds,
                            onChannelClick = onChannelClick,
                            onFavoriteToggle = viewModel::toggleFavorite
                        )
                    }
                }

                // 2b. Watch Later (Personal watchlist of saved Movies and Series)
                if ((filter == "All" || filter == "Movies" || filter == "Series") && watchLaterMediaItems.isNotEmpty()) {
                    item(key = "home_watch_later_rail") {
                        HomeMediaRail(
                            rowId = "watch_later",
                            title = "Watch Later",
                            subtitle = "Titles saved to your personal watchlist",
                            items = watchLaterMediaItems,
                            onMediaClick = onMediaClick
                        )
                    }
                }

                // 3. Intelligent "Because You Watched {Title}" (Updates dynamically when user watches)
                if ((filter == "All" || filter == "Movies" || filter == "Series") &&
                    !uiState.becauseYouWatchedTitle.isNullOrBlank() &&
                    uiState.becauseYouWatchedMedia.isNotEmpty()
                ) {
                    item(key = "because_you_watched_rail") {
                        HomeMediaRail(
                            rowId = "because_watched",
                            title = "Because You Watched ${uiState.becauseYouWatchedTitle}",
                            subtitle = "Similar picks updating in real time as you watch",
                            items = uiState.becauseYouWatchedMedia,
                            onMediaClick = onMediaClick
                        )
                    }
                }

                // 4. Intelligent "Recommended For You" (Learned from user taste profile & watch decay)
                if ((filter == "All" || filter == "Movies" || filter == "Series") &&
                    uiState.dynamicRecommendedForYou.isNotEmpty()
                ) {
                    item(key = "dynamic_recommended_for_you_rail") {
                        HomeMediaRail(
                            rowId = "recommended",
                            title = "Recommended For You",
                            subtitle = uiState.recommendationReason ?: "Personalized from your watch patterns",
                            items = uiState.dynamicRecommendedForYou,
                            onMediaClick = onMediaClick
                        )
                    }
                }

                // 5. Intelligent "Live Channels For You" (Channels scored by category & language affinity)
                if ((filter == "All" || filter == "Live TV") &&
                    uiState.smartRecommendedChannels.isNotEmpty()
                ) {
                    item(key = "smart_channels_for_you_rail") {
                        HomeChannelRail(
                            title = "Live Channels For You",
                            subtitle = "Broadcasts tailored to your category & language taste",
                            channels = uiState.smartRecommendedChannels,
                            favoriteIds = uiState.favoriteChannelIds,
                            onChannelClick = onChannelClick,
                            onFavoriteToggle = viewModel::toggleFavorite
                        )
                    }
                }

                // 6. TMDB: Language-Based Recommendations ("All", "Movies", or "Series")
                if (filter == "All" || filter == "Movies" || filter == "Series") {
                    val filteredLangMedia = when (filter) {
                        "Movies" -> uiState.languageRecommendedMedia.filter { it.mediaType == TmdbMediaType.MOVIE }
                        "Series" -> uiState.languageRecommendedMedia.filter { it.mediaType == TmdbMediaType.TV }
                        else -> uiState.languageRecommendedMedia
                    }
                    if (filteredLangMedia.isNotEmpty()) {
                        item(key = "tmdb_recommended_lang_${filter}") {
                            HomeMediaRail(
                                rowId = "lang_${filter.lowercase()}",
                                title = "Recommended in ${uiState.preferredLanguage}",
                                subtitle = "Top titles tailored for your language",
                                items = filteredLangMedia,
                                onMediaClick = onMediaClick
                            )
                        }
                    }
                }

                // 4. Live TV: Trending Channels ("All" or "Live TV")
                if ((filter == "All" || filter == "Live TV") && uiState.top10Channels.isNotEmpty()) {
                    item(key = "trending_channels_rail") {
                        HomeChannelRail(
                            title = "Trending Live Channels",
                            subtitle = "Popular broadcasts across regions",
                            channels = uiState.top10Channels,
                            favoriteIds = uiState.favoriteChannelIds,
                            onChannelClick = onChannelClick,
                            onFavoriteToggle = viewModel::toggleFavorite
                        )
                    }
                }

                // 5. TMDB: Trending Movies ("All" or "Movies")
                if ((filter == "All" || filter == "Movies") && uiState.trendingMovies.isNotEmpty()) {
                    item(key = "tmdb_trending_movies_rail") {
                        HomeMediaRail(
                            rowId = "trending_movies",
                            title = "Trending Movies",
                            subtitle = "Blockbusters & cinema hits this week",
                            items = uiState.trendingMovies,
                            onMediaClick = onMediaClick
                        )
                    }
                }

                // 6. Live TV: Recommended Channels in Preferred Language ("All" or "Live TV")
                if ((filter == "All" || filter == "Live TV") && uiState.recommendedLanguageChannels.isNotEmpty()) {
                    item(key = "preferred_language_channels_rail") {
                        HomeChannelRail(
                            title = "Live Channels in ${uiState.preferredLanguage}",
                            subtitle = "Handpicked broadcasts in your language",
                            channels = uiState.recommendedLanguageChannels,
                            favoriteIds = uiState.favoriteChannelIds,
                            onChannelClick = onChannelClick,
                            onFavoriteToggle = viewModel::toggleFavorite
                        )
                    }
                }

                // 7. TMDB: Trending TV Series ("All" or "Series")
                if ((filter == "All" || filter == "Series") && uiState.trendingTv.isNotEmpty()) {
                    item(key = "tmdb_trending_tv_rail") {
                        HomeMediaRail(
                            rowId = "trending_tv",
                            title = "Trending TV Series",
                            subtitle = "Binge-worthy shows streaming now",
                            items = uiState.trendingTv,
                            onMediaClick = onMediaClick
                        )
                    }
                }

                // 8. Live TV: News Rail ("All", "Live TV", or "News")
                if ((filter == "All" || filter == "Live TV" || filter == "News") && uiState.newsChannels.isNotEmpty()) {
                    item(key = "news_channels_rail") {
                        HomeChannelRail(
                            title = "Live News & Headlines",
                            subtitle = "24/7 breaking news channels",
                            channels = uiState.newsChannels,
                            favoriteIds = uiState.favoriteChannelIds,
                            onChannelClick = onChannelClick,
                            onFavoriteToggle = viewModel::toggleFavorite
                        )
                    }
                }

                // 9. Live TV: Movies & Cinema Rail ("All", "Live TV", or "Movies")
                if ((filter == "All" || filter == "Live TV" || filter == "Movies") && uiState.moviesChannels.isNotEmpty()) {
                    item(key = "movies_channels_rail") {
                        HomeChannelRail(
                            title = "Movies & Blockbusters TV",
                            subtitle = "Non-stop cinema and entertainment",
                            channels = uiState.moviesChannels,
                            favoriteIds = uiState.favoriteChannelIds,
                            onChannelClick = onChannelClick,
                            onFavoriteToggle = viewModel::toggleFavorite
                        )
                    }
                }

                // 10. Live TV: Music & Hits Rail ("All" or "Live TV")
                if ((filter == "All" || filter == "Live TV") && uiState.musicChannels.isNotEmpty()) {
                    item(key = "music_channels_rail") {
                        HomeChannelRail(
                            title = "Music & Hits",
                            subtitle = "Live concerts and nonstop tracks",
                            channels = uiState.musicChannels,
                            favoriteIds = uiState.favoriteChannelIds,
                            onChannelClick = onChannelClick,
                            onFavoriteToggle = viewModel::toggleFavorite
                        )
                    }
                }

                // 11. Live TV: Kids Zone Rail ("All" or "Live TV")
                if ((filter == "All" || filter == "Live TV") && uiState.kidsChannels.isNotEmpty()) {
                    item(key = "kids_channels_rail") {
                        HomeChannelRail(
                            title = "Kids & Family",
                            subtitle = "Cartoons and family shows",
                            channels = uiState.kidsChannels,
                            favoriteIds = uiState.favoriteChannelIds,
                            onChannelClick = onChannelClick,
                            onFavoriteToggle = viewModel::toggleFavorite
                        )
                    }
                }
            }
        }

        // Top Floating App Bar with Logo, Filter Chips & Quick Actions
        CinematicTopBar(
            categories = uiState.categories,
            selectedCategory = uiState.selectedFilter,
            isScrolled = isScrolled,
            onCategorySelect = { viewModel.setFilter(it) },
            onTonightClick = { showTonightSheet = true },
            onSearchClick = onSearchClick,
            onProfileClick = onProfileClick,
            onFavoritesClick = onFavoritesClick,
            avatarUrl = googleUser?.avatarUrl,
            activeProfile = activeProfile,
            onAvatarClick = { showProfileSwitcherSheet = true },
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        )

        if (showProfileSwitcherSheet) {
            val switcherScope = rememberCoroutineScope()
            HomeProfileSwitcherSheet(
                profiles = profiles,
                activeProfile = activeProfile,
                onSwitchProfile = { prof ->
                    switcherScope.launch {
                        app.container.profileRepository.switchProfile(prof.id)
                        showProfileSwitcherSheet = false
                    }
                },
                onManageProfiles = {
                    showProfileSwitcherSheet = false
                    onManageProfiles()
                },
                onOpenSettings = {
                    showProfileSwitcherSheet = false
                    onProfileClick()
                },
                onDismiss = { showProfileSwitcherSheet = false }
            )
        }

        if (showTonightSheet) {
            TonightPicksSheet(
                picks = uiState.tonightPicks,
                onPickSelected = { pick ->
                    showTonightSheet = false
                    viewModel.recordPickViewed(pick)
                    if (pick.isDownloaded) {
                        onContinueWatchingClick(pick.id)
                    } else {
                        val numericId = pick.tmdbId ?: pick.id.substringAfterLast('_').toLongOrNull() ?: 0L
                        val isTv = pick.mediaType.equals("tv", ignoreCase = true)
                        val preview = MediaPreview(
                            tmdbId = numericId.toInt(),
                            mediaType = if (isTv) "tv" else "movie",
                            title = pick.title,
                            backdropPath = null,
                            posterPath = pick.posterUrl
                        )
                        onMediaClick(preview, SharedOrigins.homeRow("tonight"))
                    }
                },
                onDismiss = { showTonightSheet = false }
            )
        }
    }
}

/**
 * Clean cinematic Hero Banner.
 */
@Composable
fun CinematicHeroBanner(
    channel: ChannelEntity,
    epgTitle: String?,
    isFavorite: Boolean,
    onPlayClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 410.dp)
    ) {
        // Background Gradient & Ambient Glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            NisTheme.colors.surfaceElevated,
                            NisTheme.colors.surface,
                            NisTheme.colors.background
                        )
                    )
                )
        ) {
            // Channel Logo / Visual in background
            if (channel.logoUrl.isNotBlank()) {
                AsyncImage(
                    model = channel.logoUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(190.dp)
                        .align(Alignment.Center)
                        .padding(bottom = 60.dp)
                )
            }
        }

        // Vignette Gradients
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

        // Hero Info Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Live Badge + Category Ribbon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NisLiveBadge()
                Text(
                    text = "${channel.category} • ${channel.language}",
                    color = NisTheme.colors.textSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Channel Title
            Text(
                text = channel.cleanName.ifEmpty { channel.name },
                color = NisTheme.colors.textPrimary,
                style = MaterialTheme.typography.headlineLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Live Programme Info
            if (!epgTitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Now Playing: $epgTitle",
                    color = NisTheme.colors.primary,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NisPrimaryButton(
                    text = "Watch Live",
                    icon = NisIcons.Play,
                    onClick = onPlayClick
                )

                NisSecondaryButton(
                    text = if (isFavorite) "Saved" else "My List",
                    icon = if (isFavorite) NisIcons.Favorites else NisIcons.FavoritesBorder,
                    onClick = onFavoriteToggle
                )
            }
        }
    }
}

/**
 * Usage-Based Recommended Hero Carousel for Home Screen.
 * Automatically rotates through top recommended items learned from user watch history.
 */
@Composable
fun RecommendedHeroCarousel(
    items: List<TmdbMediaItem>,
    recommendationReason: String?,
    becauseYouWatchedTitle: String?,
    onMediaClick: (MediaPreview, String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { items.size })

    // Keep pager clamped if recommendation items shrink
    LaunchedEffect(items.size) {
        if (items.isNotEmpty() && pagerState.currentPage >= items.size) {
            pagerState.scrollToPage((items.size - 1).coerceAtLeast(0))
        }
    }

    // Auto-advance carousel every 5.5 seconds; resets cleanly when user swipes or changes page
    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress, items.size) {
        if (!pagerState.isScrollInProgress && items.size > 1) {
            delay(5500)
            val nextPage = (pagerState.currentPage + 1) % items.size
            pagerState.animateScrollToPage(nextPage)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 430.dp)
            .height(500.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            key = { page -> items.getOrNull(page)?.let { "${it.mediaType}_${it.id}" } ?: page },
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val media = items.getOrNull(page) ?: return@HorizontalPager
            val imageUrl = media.fullBackdropUrl ?: media.fullPosterUrl

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onMediaClick(media.toPreview(), SharedOrigins.homeRow("hero")) }
            ) {
                // Background Backdrop Image
                if (!imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUrl)
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
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        NisTheme.colors.surfaceElevated,
                                        NisTheme.colors.surface,
                                        NisTheme.colors.background
                                    )
                                )
                            )
                    )
                }

                // Cinematic Gradients: Top scrim for header contrast, clear window for artwork, bottom scrim for text
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.0f to (if (NisTheme.colors.isDark) NisScrimMedium else NisTheme.colors.background.copy(alpha = 0.85f)),
                                0.22f to (if (NisTheme.colors.isDark) Color.Transparent else NisTheme.colors.background.copy(alpha = 0.15f)),
                                0.55f to Color.Transparent,
                                0.80f to (if (NisTheme.colors.isDark) NisScrimStrong else NisTheme.colors.background.copy(alpha = 0.70f)),
                                1.0f to NisTheme.colors.background
                            )
                        )
                )

                // Hero Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Recommendation Badge: Personalized hook on page 0, contextual badges on other pages
                    val badgeLabel = when {
                        page == 0 && !becauseYouWatchedTitle.isNullOrBlank() -> "BECAUSE YOU WATCHED ${becauseYouWatchedTitle.uppercase()}"
                        page == 0 && !recommendationReason.isNullOrBlank() -> recommendationReason.uppercase()
                        media.voteAverage >= 8.0 -> "CRITICS' CHOICE"
                        media.mediaType == TmdbMediaType.MOVIE -> "TOP MOVIE PICK"
                        media.mediaType == TmdbMediaType.TV -> "TOP SERIES PICK"
                        else -> "RECOMMENDED FOR YOU"
                    }

                    Surface(
                        shape = NisChipShape,
                        color = NisTheme.colors.surfaceElevated.copy(alpha = 0.60f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.40f),
                                    NisTheme.colors.primary.copy(alpha = 0.70f)
                                )
                            )
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(NisTheme.colors.primary)
                            )
                            Text(
                                text = badgeLabel,
                                color = NisTheme.colors.primary,
                                style = NisTheme.textExtras.micro,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Title
                    Text(
                        text = media.title,
                        color = NisTheme.colors.textPrimary,
                        style = MaterialTheme.typography.headlineMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Meta Row: Type + Year + Rating
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = NisBadgeShape,
                            color = (if (media.mediaType == TmdbMediaType.MOVIE) NisTheme.colors.warning else NisTheme.colors.accentCyan).copy(alpha = 0.20f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                (if (media.mediaType == TmdbMediaType.MOVIE) NisTheme.colors.warning else NisTheme.colors.accentCyan).copy(alpha = 0.50f)
                            )
                        ) {
                            Text(
                                text = if (media.mediaType == TmdbMediaType.MOVIE) "MOVIE" else "SERIES",
                                color = if (media.mediaType == TmdbMediaType.MOVIE) NisTheme.colors.warning else NisTheme.colors.accentCyan,
                                style = NisTheme.textExtras.micro,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        media.releaseYear?.let { year ->
                            Text(
                                text = year,
                                color = NisTheme.colors.textSecondary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Text(
                            text = "★ ${media.formattedRating}",
                            color = NisTheme.colors.warning,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    if (media.overview.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = media.overview,
                            color = NisTheme.colors.textSecondary,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action Buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NisPrimaryButton(
                            text = "Watch Now",
                            icon = NisIcons.Play,
                            onClick = { onMediaClick(media.toPreview(), SharedOrigins.homeRow("hero")) }
                        )

                        NisSecondaryButton(
                            text = "Details",
                            icon = NisIcons.Info,
                            onClick = { onMediaClick(media.toPreview(), SharedOrigins.homeRow("hero")) }
                        )
                    }
                }
            }
        }

        // Pager Indicators: Modern Segmented Progress Story Bars at the bottom
        if (items.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp, start = 24.dp, end = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(items.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(NisPillShape)
                            .background(
                                if (isSelected) NisTheme.colors.primary else (if (NisTheme.colors.isDark) Color.White.copy(alpha = 0.20f) else NisTheme.colors.textMuted.copy(alpha = 0.25f))
                            )
                    )
                }
            }
        }
    }
}


/**
 * Cinematic Hero Banner for Movies & Series when selected in the top category chips.
 */
@Composable
fun CinematicMediaHeroBanner(
    media: TmdbMediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 410.dp)
            .clickable(onClick = onClick)
    ) {
        val imageUrl = media.fullBackdropUrl ?: media.fullPosterUrl
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
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
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                NisTheme.colors.surfaceElevated,
                                NisTheme.colors.surface,
                                NisTheme.colors.background
                            )
                        )
                    )
            )
        }

        // Vignette Gradients
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            if (NisTheme.colors.isDark) NisScrimMedium else NisTheme.colors.background.copy(alpha = 0.85f),
                            if (NisTheme.colors.isDark) NisScrimSoft else NisTheme.colors.background.copy(alpha = 0.20f),
                            if (NisTheme.colors.isDark) NisScrimStrong else NisTheme.colors.background.copy(alpha = 0.70f),
                            NisTheme.colors.background
                        )
                    )
                )
        )

        // Hero Info Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Media Type Badge + Year + Rating
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(NisBadgeShape)
                        .background(if (media.mediaType == TmdbMediaType.MOVIE) NisTheme.colors.warning else NisTheme.colors.accentCyan)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (media.mediaType == TmdbMediaType.MOVIE) "MOVIE" else "SERIES",
                        color = Color.White,
                        style = NisTheme.textExtras.micro
                    )
                }

                media.releaseYear?.let { year ->
                    Text(
                        text = year,
                        color = NisTheme.colors.textSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Text(
                    text = "• ${media.formattedRating}",
                    color = NisTheme.colors.warning,
                    style = MaterialTheme.typography.labelMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = media.title,
                color = NisTheme.colors.textPrimary,
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (media.overview.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = media.overview,
                    color = NisTheme.colors.textSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Button
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NisPrimaryButton(
                    text = "View Details",
                    icon = NisIcons.Info,
                    onClick = onClick
                )
            }
        }
    }
}

/**
 * Horizontal VOD Media Rail featuring 16:9 NisBackdropMediaCard components with
 * stable row identifiers and origin keys for Netflix-style shared element transitions.
 */
@Composable
fun HomeMediaRail(
    rowId: String,
    title: String,
    subtitle: String?,
    items: List<TmdbMediaItem>,
    onMediaClick: (MediaPreview, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val origin = SharedOrigins.homeRow(rowId)
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        NisSectionHeader(
            title = title,
            subtitle = subtitle
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items, key = { "${origin}_${it.mediaType.value}_${it.id}" }) { item ->
                NisPosterMediaCard(
                    preview = item.toPreview(),
                    origin = origin,
                    onClick = onMediaClick
                )
            }
        }
    }
}

/**
 * Horizontal Channel Rail with Section Header, snap peek, and NisRailChannelCards.
 */
@Composable
fun HomeChannelRail(
    title: String,
    channels: List<ChannelEntity>,
    favoriteIds: Set<String>,
    onChannelClick: (String) -> Unit,
    onFavoriteToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        NisSectionHeader(
            title = title,
            subtitle = subtitle
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(channels, key = { it.id }) { channel ->
                NisRailChannelCard(
                    channel = channel,
                    isFavorite = favoriteIds.contains(channel.id),
                    onClick = { onChannelClick(channel.id) },
                    onFavoriteToggle = { onFavoriteToggle(channel.id) }
                )
            }
        }
    }
}

/**
 * Top floating bar with blur / dark background and quick search + profile controls.
 */
@Composable
fun CinematicTopBar(
    categories: List<String>,
    selectedCategory: String,
    isScrolled: Boolean,
    onCategorySelect: (String) -> Unit,
    onTonightClick: () -> Unit = {},
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit,
    onFavoritesClick: () -> Unit = {},
    avatarUrl: String? = null,
    activeProfile: ProfileEntity? = null,
    onAvatarClick: () -> Unit = onProfileClick,
    modifier: Modifier = Modifier
) {
    val dividerColor = NisTheme.colors.borderSubtle
    Box(
        modifier = modifier
            .background(
                if (isScrolled) {
                    Brush.verticalGradient(
                        colors = listOf(
                            NisTheme.colors.background.copy(alpha = 0.98f),
                            NisTheme.colors.surface.copy(alpha = 0.95f)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(
                            NisTheme.colors.background.copy(alpha = 0.90f),
                            NisTheme.colors.background.copy(alpha = 0.40f),
                            Color.Transparent
                        )
                    )
                }
            )
            .then(
                if (isScrolled) {
                    Modifier.drawBehind {
                        val stroke = 1.dp.toPx()
                        drawLine(
                            color = dividerColor,
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = stroke
                        )
                    }
                } else Modifier
            )
            .statusBarsPadding()
            .padding(bottom = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = NisDimens.MaxContentWidth)
                .align(Alignment.Center)
        ) {
            // Header Row: Brand Title + Search, Favorites & Profile Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // App Logo Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NisLogo(size = 32.dp)
                    Text(
                        text = "NisTV",
                        color = NisTheme.colors.textPrimary,
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                // Actions: Favorites, Search & Profile
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val actionLensBg = Brush.verticalGradient(
                        listOf(
                            NisTheme.colors.surfaceElevated.copy(alpha = 0.65f),
                            NisTheme.colors.surface.copy(alpha = 0.45f)
                        )
                    )
                    val actionLensBorder = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.35f),
                            NisTheme.colors.borderSubtle.copy(alpha = 0.25f)
                        )
                    )

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(actionLensBg, CircleShape)
                            .border(1.dp, actionLensBorder, CircleShape)
                            .clickable(
                                role = Role.Button,
                                onClick = onFavoritesClick,
                                onClickLabel = "My List"
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = NisIcons.FavoritesBorder,
                            contentDescription = "My List",
                            tint = NisTheme.colors.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(actionLensBg, CircleShape)
                            .border(1.dp, actionLensBorder, CircleShape)
                            .clickable(
                                role = Role.Button,
                                onClick = onSearchClick,
                                onClickLabel = "Search channels"
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = NisIcons.Search,
                            contentDescription = "Search",
                            tint = NisTheme.colors.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(actionLensBg, CircleShape)
                            .border(1.dp, actionLensBorder, CircleShape)
                            .clickable(
                                role = Role.Button,
                                onClick = onAvatarClick,
                                onClickLabel = "Profile & Settings"
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (activeProfile != null) {
                            NisProfileAvatar(
                                avatarId = activeProfile.avatarId,
                                name = activeProfile.name,
                                size = 32.dp,
                                isKids = activeProfile.isKids,
                                isLocked = activeProfile.isLocked
                            )
                        } else if (!avatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(avatarUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Profile",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        } else {
                            Icon(
                                imageVector = NisIcons.Profile,
                                contentDescription = "Profile",
                                tint = NisTheme.colors.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Category Filter Pills
            if (categories.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    item(key = "tonight_chip") {
                        NisCategoryChip(
                            text = "✨ Tonight",
                            isSelected = false,
                            onClick = onTonightClick
                        )
                    }
                    items(categories) { cat ->
                        NisCategoryChip(
                            text = cat,
                            isSelected = cat.equals(selectedCategory, ignoreCase = true),
                            onClick = { onCategorySelect(cat) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Portrait card for downloaded in-progress content in Continue Watching row.
 */
@Composable
fun HomeContinueWatchingCard(
    item: ContinueWatchingItem,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = NisMediaCardShape
    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .width(115.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true }
            )
    ) {
        Box(
            modifier = Modifier
                .width(115.dp)
                .height(165.dp)
                .clip(shape)
                .background(NisTheme.colors.surface)
                .border(1.dp, NisTheme.colors.border, shape)
        ) {
            val posterUrl = item.download.posterUrl ?: item.download.backdropUrl
            if (!posterUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                    .data(posterUrl)
                    .crossfade(true)
                    .build(),
                    contentDescription = item.download.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(NisTheme.colors.surfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = NisIcons.Play,
                        contentDescription = null,
                        tint = NisTheme.colors.textMuted,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Top-right overflow menu button
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(NisScrimMedium)
                        .clickable { showMenu = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = NisIcons.MoreVert,
                        contentDescription = "Options",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(NisTheme.colors.surfaceElevated)
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Remove from Continue Watching",
                                color = NisTheme.colors.live,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        onClick = {
                            showMenu = false
                            onRemove()
                        }
                    )
                }
            }

            // Bottom gradient + progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, NisScrimStrong)
                        )
                    )
                    .padding(horizontal = 6.dp, vertical = 6.dp)
            ) {
                NisLinearProgressBar(
                    progress = item.progressFraction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(NisBadgeShape),
                    color = NisTheme.colors.primary,
                    trackColor = Color.White.copy(alpha = 0.25f)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = item.download.title,
            color = NisTheme.colors.textPrimary,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!item.subtitleText.isNullOrBlank()) {
                Text(
                    text = item.subtitleText,
                    color = NisTheme.colors.primary,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = item.remainingTimeText,
                color = NisTheme.colors.textMuted,
                style = NisTheme.textExtras.micro,
                maxLines = 1
            )
        }
    }
}

/**
 * Continue Watching row for in-progress downloads.
 */
@Composable
fun HomeContinueWatchingRail(
    items: List<ContinueWatchingItem>,
    onItemClick: (String) -> Unit,
    onRemoveItem: (ContinueWatchingItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        NisSectionHeader(
            title = "Continue Watching",
            subtitle = "Pick up right where you left off"
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items, key = { "continue_watching_${it.download.id}" }) { item ->
                HomeContinueWatchingCard(
                    item = item,
                    onClick = { onItemClick(item.download.id) },
                    onRemove = { onRemoveItem(item) }
                )
            }
        }
    }
}
