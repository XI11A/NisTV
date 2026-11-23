@file:OptIn(ExperimentalSharedTransitionApi::class)
package com.nis.nistv.feature.discover

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
import com.nis.nistv.core.designsystem.transition.SharedKeys
import com.nis.nistv.core.designsystem.transition.SharedRole
import com.nis.nistv.core.designsystem.transition.debouncedClickable
import com.nis.nistv.core.designsystem.transition.nisSharedBounds
import com.nis.nistv.data.tmdb.model.MediaPreview
import com.nis.nistv.data.tmdb.model.TmdbMediaItem
import com.nis.nistv.data.tmdb.model.TmdbMediaType
import com.nis.nistv.data.tmdb.model.toPreview

@Composable
fun DiscoverScreen(
    onMediaClick: (MediaPreview, String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DiscoverViewModel = viewModel(
        factory = run {
            val app = LocalContext.current.applicationContext as NisTvApplication
            DiscoverViewModel.provideFactory(
                repository = app.container.tmdbRepository,
                recommendationEngine = app.container.recommendationEngine,
                userPreferences = app.container.userPreferences
            )
        }
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NisTheme.colors.background)
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header with Media Type Toggle & Search Bar
        DiscoverTopHeader(
            selectedType = uiState.selectedMediaType,
            onTypeSelect = { viewModel.setMediaType(it) },
            searchQuery = uiState.searchQuery,
            onSearchQueryChange = { viewModel.searchMedia(it) },
            onClearSearch = {
                viewModel.clearSearch()
                focusManager.clearFocus()
            },
            modifier = Modifier.widthIn(max = NisDimens.MaxContentWidth)
        )

        // Genre Filter Chips Rail
        if (!uiState.isSearching && uiState.searchQuery.isBlank() && uiState.genres.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = NisDimens.MaxContentWidth)
            ) {
                item {
                    NisCategoryChip(
                        text = "All",
                        isSelected = uiState.selectedGenreId == null,
                        onClick = { viewModel.selectGenre(null) }
                    )
                }
                items(uiState.genres, key = { it.id }) { genre ->
                    NisCategoryChip(
                        text = genre.name,
                        isSelected = uiState.selectedGenreId == genre.id,
                        onClick = { viewModel.selectGenre(genre.id) }
                    )
                }
            }
        }

        // Main Content Area
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.isLoading -> {
                    DiscoverSkeletonContent()
                }

                uiState.errorMessage != null && uiState.trendingRail.isEmpty() -> {
                    NisErrorState(
                        title = "Unable to Load Discovery",
                        message = uiState.errorMessage ?: "Network error or invalid TMDB API Key.",
                        onRetry = { viewModel.refresh() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                uiState.searchQuery.isNotBlank() -> {
                    // Search Results Grid
                    if (uiState.searchResults.isEmpty() && !uiState.isSearching) {
                        NisEmptyState(
                            title = "No Titles Found",
                            message = "Try searching with a different title or keyword",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 110.dp),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = NisDimens.MaxContentWidth)
                        ) {
                            items(uiState.searchResults, key = { "${it.mediaType}_${it.id}" }) { item ->
                                DiscoverMediaPosterCard(
                                    item = item,
                                    origin = "discover_search",
                                    onClick = { onMediaClick(item.toPreview(), "discover_search") },
                                    fixedWidth = false
                                )
                            }
                        }
                    }
                }

                uiState.selectedGenreId != null -> {
                    // Filtered Grid by Genre with Infinite Scroll
                    val gridState = rememberLazyGridState()

                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Adaptive(minSize = 110.dp),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 90.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = NisDimens.MaxContentWidth)
                    ) {
                        items(uiState.gridItems, key = { "discover_grid_${it.mediaType}_${it.id}" }) { item ->
                            DiscoverMediaPosterCard(
                                item = item,
                                origin = "discover_grid",
                                onClick = { onMediaClick(item.toPreview(), "discover_grid") },
                                fixedWidth = false
                            )
                        }

                        if (uiState.gridItems.isNotEmpty()) {
                            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (uiState.isLoadingMore) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(28.dp),
                                            color = NisTheme.colors.primary,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        NisSecondaryButton(
                                            text = "Load More",
                                            onClick = { viewModel.loadMore() }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                else -> {
                    // Default Discover Hub: Hero Spotlight + Horizontal Rails
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = NisDimens.MaxContentWidth),
                        verticalArrangement = Arrangement.spacedBy(NisDimens.Scale24),
                        contentPadding = PaddingValues(bottom = 90.dp)
                    ) {
                        // Hero Spotlight
                        uiState.heroItem?.let { hero ->
                            item(key = "discover_hero") {
                                DiscoverHeroBanner(
                                    item = hero,
                                    onClick = { onMediaClick(hero.toPreview(), "discover_hero") }
                                )
                            }
                        }

                        // Rail 1: Intelligent Recommendations for You
                        if (uiState.recommendedForYou.isNotEmpty()) {
                            item(key = "recommended_rail") {
                                DiscoverMediaRail(
                                    title = "Recommended For You",
                                    subtitle = uiState.recommendationReason ?: "Personalized based on your watch history",
                                    items = uiState.recommendedForYou,
                                    onItemClick = onMediaClick
                                )
                            }
                        }

                        // Rail 2: User's Preferred Language Content
                        if (uiState.trendingInPreferredLanguage.isNotEmpty()) {
                            item(key = "preferred_lang_rail") {
                                DiscoverMediaRail(
                                    title = if (uiState.selectedMediaType == TmdbMediaType.MOVIE)
                                        "Top ${uiState.preferredLanguage} Movies"
                                    else
                                        "Top ${uiState.preferredLanguage} Series",
                                    subtitle = "Popular in your preferred language (${uiState.preferredLanguage})",
                                    items = uiState.trendingInPreferredLanguage,
                                    onItemClick = onMediaClick
                                )
                            }
                        }

                        // Rail 3: Because You Watched {Title}
                        if (!uiState.becauseYouWatchedTitle.isNullOrBlank() && uiState.becauseYouWatchedMedia.isNotEmpty()) {
                            item(key = "because_watched_rail") {
                                DiscoverMediaRail(
                                    title = "Because You Watched ${uiState.becauseYouWatchedTitle}",
                                    subtitle = "Tailored suggestions based on your recent viewing",
                                    items = uiState.becauseYouWatchedMedia,
                                    onItemClick = onMediaClick
                                )
                            }
                        }

                        // Rail 4: Trending
                        if (uiState.trendingRail.isNotEmpty()) {
                            item(key = "trending_rail") {
                                DiscoverMediaRail(
                                    title = if (uiState.selectedMediaType == TmdbMediaType.MOVIE) "Trending Movies" else "Trending Series",
                                    subtitle = "Top global picks this week",
                                    items = uiState.trendingRail,
                                    onItemClick = onMediaClick
                                )
                            }
                        }

                        // Rail 2: Popular
                        if (uiState.popularRail.isNotEmpty()) {
                            item(key = "popular_rail") {
                                DiscoverMediaRail(
                                    title = "Popular Now",
                                    subtitle = "Audience favorites",
                                    items = uiState.popularRail,
                                    onItemClick = onMediaClick
                                )
                            }
                        }

                        // Rail 3: Now Playing / Airing Today
                        if (uiState.nowPlayingOrAiringRail.isNotEmpty()) {
                            item(key = "now_playing_rail") {
                                DiscoverMediaRail(
                                    title = if (uiState.selectedMediaType == TmdbMediaType.MOVIE) "In Theaters Now" else "Airing Today",
                                    subtitle = "Fresh releases and new episodes",
                                    items = uiState.nowPlayingOrAiringRail,
                                    onItemClick = onMediaClick
                                )
                            }
                        }

                        // Rail 4: Top Rated
                        if (uiState.topRatedRail.isNotEmpty()) {
                            item(key = "top_rated_rail") {
                                DiscoverMediaRail(
                                    title = "Top Rated All-Time",
                                    subtitle = "Critically acclaimed blockbusters",
                                    items = uiState.topRatedRail,
                                    onItemClick = onMediaClick
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DiscoverTopHeader(
    selectedType: TmdbMediaType,
    onTypeSelect: (TmdbMediaType) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Discover",
                color = NisTheme.colors.textPrimary,
                style = MaterialTheme.typography.titleLarge
            )

            // Movies vs TV Series Pill Toggle
            Row(
                modifier = Modifier
                    .clip(NisPillShape)
                    .background(NisTheme.colors.surface)
                    .border(1.dp, NisTheme.colors.border, NisPillShape)
                    .padding(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TabTogglePill(
                    text = "Movies",
                    isSelected = selectedType == TmdbMediaType.MOVIE,
                    onClick = { onTypeSelect(TmdbMediaType.MOVIE) }
                )
                TabTogglePill(
                    text = "Series",
                    isSelected = selectedType == TmdbMediaType.TV,
                    onClick = { onTypeSelect(TmdbMediaType.TV) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search Input Field using standard NisSearchBar for clean typography & vertical alignment
        NisSearchBar(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            placeholderText = if (selectedType == TmdbMediaType.MOVIE) "Search movies by title..." else "Search series by title...",
            onClearClick = onClearSearch,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun TabTogglePill(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(NisPillShape)
            .background(if (isSelected) NisTheme.colors.primary else Color.Transparent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else NisTheme.colors.textSecondary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun DiscoverHeroBanner(
    item: TmdbMediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(340.dp)
            .clickable(onClick = onClick)
    ) {
        val backdropUrl = item.fullBackdropUrl ?: item.fullPosterUrl
        if (!backdropUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(backdropUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = item.title,
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

        // Cinematic Vignette Overlays
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            if (NisTheme.colors.isDark) NisScrimSoft else NisTheme.colors.background.copy(alpha = 0.20f),
                            Color.Transparent,
                            if (NisTheme.colors.isDark) NisScrimStrong else NisTheme.colors.background.copy(alpha = 0.70f),
                            NisTheme.colors.background
                        )
                    )
                )
        )

        // Hero Info
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(NisBadgeShape)
                        .background(NisTheme.colors.warning)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = item.formattedRating,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                item.releaseYear?.let { year ->
                    Text(
                        text = year,
                        color = NisTheme.colors.textSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = item.title,
                color = NisTheme.colors.textPrimary,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (item.overview.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.overview,
                    color = NisTheme.colors.textSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NisPrimaryButton(
                    text = "View Details",
                    icon = NisIcons.Info,
                    onClick = onClick
                )

                NisBookmarkButton(
                    media = item,
                    size = 40.dp,
                    iconSize = 20.dp
                )
            }
        }
    }
}

@Composable
fun DiscoverMediaRail(
    title: String,
    subtitle: String?,
    items: List<TmdbMediaItem>,
    onItemClick: (MediaPreview, String) -> Unit,
    modifier: Modifier = Modifier,
    rowId: String = title.lowercase().replace(" ", "_")
) {
    val origin = "discover_$rowId"
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        NisSectionHeader(
            title = title,
            subtitle = subtitle
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items, key = { "${origin}_${it.mediaType}_${it.id}" }) { item ->
                DiscoverMediaPosterCard(
                    item = item,
                    origin = origin,
                    onClick = { onItemClick(item.toPreview(), origin) }
                )
            }
        }
    }
}

@Composable
fun DiscoverMediaPosterCard(
    item: TmdbMediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fixedWidth: Boolean = true,
    origin: String? = null
) {
    val shape = NisMediaCardShape
    Column(
        modifier = modifier
            .then(if (fixedWidth) Modifier.width(115.dp) else Modifier.fillMaxWidth())
            .then(
                if (!origin.isNullOrBlank()) {
                    Modifier.nisSharedBounds(
                        key = SharedKeys.of(item.mediaType.value, item.id.toInt(), SharedRole.CONTAINER, origin),
                        clipShape = shape,
                        zIndex = 1f
                    )
                } else Modifier
            )
            .debouncedClickable(
                role = Role.Button,
                onClickLabel = item.title,
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (fixedWidth) Modifier.height(165.dp) else Modifier.aspectRatio(2f / 3f))
                .clip(shape)
                .background(NisTheme.colors.surface)
                .border(1.dp, NisTheme.colors.border, shape)
        ) {
            if (!item.fullPosterUrl.isNullOrBlank()) {
                val preview = item.toPreview()
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.fullPosterUrl)
                        .memoryCacheKey(preview.posterCacheKey)
                        .crossfade(true)
                        .build(),
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (!origin.isNullOrBlank()) {
                                Modifier.nisSharedBounds(
                                    key = SharedKeys.of(item.mediaType.value, item.id.toInt(), SharedRole.IMAGE, origin),
                                    clipShape = shape,
                                    zIndex = 2f
                                )
                            } else Modifier
                        )
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(NisTheme.colors.surfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.title.take(1),
                        color = NisTheme.colors.textMuted,
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            }

            // Rating pill badge top-end
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .clip(NisBadgeShape)
                    .background(NisScrimMedium)
                    .border(0.5.dp, NisTheme.colors.borderSubtle, NisBadgeShape)
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = item.formattedRating,
                    color = NisTheme.colors.warning,
                    style = NisTheme.textExtras.micro,
                    fontWeight = FontWeight.Bold
                )
            }

            // Watch later bookmark top-start
            NisBookmarkButton(
                media = item,
                size = 28.dp,
                iconSize = 15.dp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(5.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = item.title,
            color = NisTheme.colors.textPrimary,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        item.releaseYear?.let { year ->
            Text(
                text = year,
                color = NisTheme.colors.textMuted,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1
            )
        }
    }
}

@Composable
fun DiscoverSkeletonContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        HeroBannerSkeleton()
        Spacer(modifier = Modifier.height(16.dp))
        ChannelRailSkeleton()
        ChannelRailSkeleton()
    }
}
