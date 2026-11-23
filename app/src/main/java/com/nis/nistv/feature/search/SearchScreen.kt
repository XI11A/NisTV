@file:OptIn(ExperimentalSharedTransitionApi::class)
package com.nis.nistv.feature.search

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nis.nistv.NisTvApplication
import com.nis.nistv.core.designsystem.component.NisCategoryChip
import com.nis.nistv.core.designsystem.component.NisChannelCard
import com.nis.nistv.core.designsystem.component.NisEmptyState
import com.nis.nistv.core.designsystem.component.NisSearchBar
import com.nis.nistv.core.designsystem.component.NisSectionHeader
import com.nis.nistv.core.designsystem.icon.NisIcons
import com.nis.nistv.core.designsystem.theme.NisAccentPrimary
import com.nis.nistv.core.designsystem.theme.NisTheme
import com.nis.nistv.core.designsystem.theme.NisBorder
import com.nis.nistv.core.designsystem.theme.NisChipShape
import com.nis.nistv.core.designsystem.theme.NisDimens
import com.nis.nistv.core.designsystem.theme.NisSurfaceElevated
import com.nis.nistv.core.designsystem.theme.NisTextMuted
import com.nis.nistv.core.designsystem.theme.NisTextPrimary
import com.nis.nistv.core.designsystem.theme.NisTextSecondary
import com.nis.nistv.core.designsystem.transition.SharedOrigins
import com.nis.nistv.data.tmdb.model.MediaPreview
import com.nis.nistv.data.tmdb.model.TmdbMediaType
import com.nis.nistv.data.tmdb.model.toPreview
import com.nis.nistv.feature.discover.DiscoverMediaPosterCard

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    onChannelClick: (String) -> Unit,
    onMediaClick: (MediaPreview, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = viewModel(
        factory = SearchViewModel.provideFactory(
            repository = (LocalContext.current.applicationContext as NisTvApplication).container.channelRepository,
            tmdbRepository = (LocalContext.current.applicationContext as NisTvApplication).container.tmdbRepository,
            userPreferences = (LocalContext.current.applicationContext as NisTvApplication).container.userPreferences,
            parentalControlManager = (LocalContext.current.applicationContext as NisTvApplication).container.parentalControlManager
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    var searchQuery by rememberSaveable { mutableStateOf(uiState.query) }

    fun updateSearchQuery(newQuery: String) {
        searchQuery = newQuery
        viewModel.onQueryChange(newQuery)
    }

    val isSearching = searchQuery.isNotBlank()

    val popularSuggestions = remember {
        listOf("News", "Movies", "Sports", "Music", "Kids", "Tamil", "Hindi", "Telugu", "Malayalam", "Kannada", "English")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NisTheme.colors.background)
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Search Screen Header Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = NisDimens.MaxContentWidth)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Search",
                color = NisTheme.colors.textPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        // Search Input Bar (unified across IPTV and TMDB)
        NisSearchBar(
            query = searchQuery,
            onQueryChange = ::updateSearchQuery,
            placeholderText = "Search channels, movies, series, genres...",
            onSearchSubmit = {
                viewModel.submitSearch(it)
                keyboardController?.hide()
            },
            onClearClick = { updateSearchQuery("") },
            modifier = Modifier
                .widthIn(max = NisDimens.MaxContentWidth)
                .padding(horizontal = 16.dp, vertical = 6.dp)
        )

        if (!isSearching) {
            // Screen state: Not searching yet (show Recents and Suggestions)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = NisDimens.MaxContentWidth)
                    .padding(top = 10.dp)
            ) {
                // 1. Recent Searches
                if (uiState.recentSearches.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Recent Searches",
                            color = NisTheme.colors.textPrimary,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Clear All",
                            color = NisTheme.colors.primary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clickable(
                                    role = Role.Button,
                                    onClick = viewModel::clearRecentSearches,
                                    onClickLabel = "Clear recent searches"
                                )
                                .padding(4.dp)
                        )
                    }

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        items(uiState.recentSearches) { term ->
                            Row(
                                modifier = Modifier
                                    .clip(NisChipShape)
                                    .background(NisTheme.colors.surfaceElevated)
                                    .border(1.dp, NisTheme.colors.border, NisChipShape)
                                    .clickable { updateSearchQuery(term) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = NisIcons.Refresh,
                                    contentDescription = null,
                                    tint = NisTheme.colors.textSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = term,
                                    color = NisTheme.colors.textSecondary,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // 2. Discover / Suggested Topics
                NisSectionHeader(
                    title = "Explore Categories & Languages",
                    subtitle = "Tap any topic to discover channels and entertainment"
                )

                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    popularSuggestions.forEach { tag ->
                        NisCategoryChip(
                            text = tag,
                            isSelected = false,
                            onClick = {
                                updateSearchQuery(tag)
                            }
                        )
                    }
                }
            }
        } else {
            // Screen state: Searching (Tabs & Unified Results)
            val tabs = listOf("All", "Channels", "Movies", "Series")

            // Filter Tabs Row
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tabs) { tab ->
                    val count = when (tab) {
                        "All" -> uiState.totalCount
                        "Channels" -> uiState.channelResults.size
                        "Movies" -> uiState.movieResults.size
                        "Series" -> uiState.tvResults.size
                        else -> 0
                    }
                    NisCategoryChip(
                        text = "$tab ($count)",
                        isSelected = uiState.selectedFilterTab == tab,
                        onClick = { viewModel.setFilterTab(tab) }
                    )
                }
            }

            if (uiState.totalCount == 0 && !uiState.isLoadingTmdb) {
                NisEmptyState(
                    title = "No results found",
                    message = "We couldn't find any channels, movies, or series matching \"$searchQuery\". Try a broader search term.",
                    icon = NisIcons.Search,
                    actionText = "Clear Search",
                    onActionClick = { updateSearchQuery("") }
                )
            } else {
                when (uiState.selectedFilterTab) {
                    "All" -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 96.dp)
                        ) {
                            // Movies Result Rail
                            if (uiState.movieResults.isNotEmpty()) {
                                item(key = "search_movies_rail") {
                                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                        NisSectionHeader(
                                            title = "Movies (${uiState.movieResults.size})",
                                            subtitle = "Cinema releases and blockbusters"
                                        )
                                        LazyRow(
                                            contentPadding = PaddingValues(horizontal = 16.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            items(uiState.movieResults, key = { "m_${it.id}" }) { movie ->
                                                DiscoverMediaPosterCard(
                                                    item = movie,
                                                    origin = SharedOrigins.SEARCH,
                                                    onClick = {
                                                        viewModel.submitSearch(searchQuery)
                                                        onMediaClick(movie.toPreview(), SharedOrigins.SEARCH)
                                                    },
                                                    fixedWidth = true
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // TV Series Result Rail
                            if (uiState.tvResults.isNotEmpty()) {
                                item(key = "search_tv_rail") {
                                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                        NisSectionHeader(
                                            title = "TV Series (${uiState.tvResults.size})",
                                            subtitle = "Shows and binge-worthy series"
                                        )
                                        LazyRow(
                                            contentPadding = PaddingValues(horizontal = 16.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            items(uiState.tvResults, key = { "tv_${it.id}" }) { tv ->
                                                DiscoverMediaPosterCard(
                                                    item = tv,
                                                    origin = SharedOrigins.SEARCH,
                                                    onClick = {
                                                        viewModel.submitSearch(searchQuery)
                                                        onMediaClick(tv.toPreview(), SharedOrigins.SEARCH)
                                                    },
                                                    fixedWidth = true
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Live TV Channels Section
                            if (uiState.channelResults.isNotEmpty()) {
                                item(key = "search_channels_header") {
                                    NisSectionHeader(
                                        title = "Live TV Channels (${uiState.channelResults.size})",
                                        subtitle = "Broadcast streams available now"
                                    )
                                }
                                items(uiState.channelResults, key = { "ch_${it.id}" }) { channel ->
                                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                                        NisChannelCard(
                                            channel = channel,
                                            isFavorite = uiState.favoriteChannelIds.contains(channel.id),
                                            onClick = {
                                                viewModel.submitSearch(searchQuery)
                                                onChannelClick(channel.id)
                                            },
                                            onFavoriteToggle = { viewModel.toggleFavorite(channel.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    "Channels" -> {
                        if (uiState.channelResults.isEmpty()) {
                            NisEmptyState(
                                title = "No Channels Found",
                                message = "No live channels matched \"$searchQuery\""
                            )
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 160.dp),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(uiState.channelResults, key = { it.id }) { channel ->
                                    NisChannelCard(
                                        channel = channel,
                                        isFavorite = uiState.favoriteChannelIds.contains(channel.id),
                                        onClick = {
                                            viewModel.submitSearch(searchQuery)
                                            onChannelClick(channel.id)
                                        },
                                        onFavoriteToggle = { viewModel.toggleFavorite(channel.id) }
                                    )
                                }
                            }
                        }
                    }

                    "Movies" -> {
                        if (uiState.movieResults.isEmpty()) {
                            NisEmptyState(
                                title = "No Movies Found",
                                message = "No movies matched \"$searchQuery\""
                            )
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(uiState.movieResults, key = { it.id }) { movie ->
                                    DiscoverMediaPosterCard(
                                        item = movie,
                                        origin = SharedOrigins.SEARCH,
                                        onClick = {
                                            viewModel.submitSearch(searchQuery)
                                            onMediaClick(movie.toPreview(), SharedOrigins.SEARCH)
                                        },
                                        fixedWidth = false
                                    )
                                }
                            }
                        }
                    }

                    "Series" -> {
                        if (uiState.tvResults.isEmpty()) {
                            NisEmptyState(
                                title = "No Series Found",
                                message = "No TV series matched \"$searchQuery\""
                            )
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(uiState.tvResults, key = { it.id }) { tv ->
                                    DiscoverMediaPosterCard(
                                        item = tv,
                                        origin = SharedOrigins.SEARCH,
                                        onClick = {
                                            viewModel.submitSearch(searchQuery)
                                            onMediaClick(tv.toPreview(), SharedOrigins.SEARCH)
                                        },
                                        fixedWidth = false
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
