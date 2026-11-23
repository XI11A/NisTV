package com.nis.nistv.feature.livetv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nis.nistv.NisTvApplication
import com.nis.nistv.core.designsystem.component.ChannelGridSkeleton
import com.nis.nistv.core.designsystem.component.NisCategoryChip
import com.nis.nistv.core.designsystem.component.NisChannelCard
import com.nis.nistv.core.designsystem.component.NisEmptyState
import com.nis.nistv.core.designsystem.component.NisSearchBar
import com.nis.nistv.core.designsystem.icon.NisIcons
import com.nis.nistv.core.designsystem.theme.NisDimens
import com.nis.nistv.core.designsystem.theme.NisTheme
import com.nis.nistv.core.designsystem.theme.NisTextMuted
import com.nis.nistv.core.designsystem.theme.NisTextPrimary

/**
 * Browse Screen (Live TV Catalog).
 * Specifications per iptv-ui-redesign:
 * - Sticky search bar + horizontally scrolling filter chips (category, language)
 * - Adaptive grid (2 columns on mobile, 3+ on tablets)
 * - Skeleton loader for initial data loading
 * - Empty state with filter reset action
 */
@Composable
fun LiveTvScreen(
    onChannelClick: (String) -> Unit,
    onOpenGuide: () -> Unit = {},
    onOpenPlaylists: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: LiveTvViewModel = viewModel(
        factory = LiveTvViewModel.provideFactory(
            repository = (LocalContext.current.applicationContext as NisTvApplication).container.channelRepository,
            playlistDao = (LocalContext.current.applicationContext as NisTvApplication).container.database.playlistDao(),
            userPreferences = (LocalContext.current.applicationContext as NisTvApplication).container.userPreferences,
            parentalControlManager = (LocalContext.current.applicationContext as NisTvApplication).container.parentalControlManager
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val filteredChannels = remember(uiState.channels, searchQuery) {
        if (searchQuery.isBlank()) {
            uiState.channels
        } else {
            val q = searchQuery.trim().lowercase()
            uiState.channels.filter { channel ->
                channel.name.lowercase().contains(q) ||
                channel.cleanName.lowercase().contains(q) ||
                channel.category.lowercase().contains(q) ||
                channel.language.lowercase().contains(q)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NisTheme.colors.background)
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Sticky Header: Title + Channel Count + Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = NisDimens.MaxContentWidth)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Browse Live TV",
                    color = NisTheme.colors.textPrimary,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "${filteredChannels.size} channels",
                    color = NisTheme.colors.textMuted,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.IconButton(
                    onClick = onOpenGuide,
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(NisTheme.colors.surfaceElevated)
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = NisIcons.LiveTv,
                        contentDescription = "TV Guide",
                        tint = NisTheme.colors.accentCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                androidx.compose.material3.IconButton(
                    onClick = onOpenPlaylists,
                    modifier = Modifier
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(NisTheme.colors.surfaceElevated)
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = NisIcons.Add,
                        contentDescription = "My Playlists",
                        tint = NisTheme.colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Sticky Search Bar
        NisSearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            placeholderText = "Filter by name, genre or language...",
            onClearClick = { searchQuery = "" },
            modifier = Modifier
                .widthIn(max = NisDimens.MaxContentWidth)
                .padding(horizontal = 16.dp, vertical = 6.dp)
        )

        // Filter Chips Rails: Categories & My Playlists
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .widthIn(max = NisDimens.MaxContentWidth)
                .padding(top = 4.dp, bottom = 4.dp)
        ) {
            item {
                NisCategoryChip(
                    text = "All",
                    isSelected = !uiState.isMyPlaylistsSelected && uiState.selectedCategory == null && uiState.selectedLanguage == null && uiState.selectedCountry == null,
                    onClick = { viewModel.selectCategory(null) }
                )
            }
            item {
                NisCategoryChip(
                    text = "My Playlists",
                    isSelected = uiState.isMyPlaylistsSelected,
                    onClick = { viewModel.selectMyPlaylists(null) }
                )
            }
            items(uiState.categories, key = { it }) { cat ->
                NisCategoryChip(
                    text = cat,
                    isSelected = !uiState.isMyPlaylistsSelected && uiState.selectedCategory == cat,
                    onClick = { viewModel.selectCategory(cat) }
                )
            }
        }

        // Sub-filter Chips: Individual Playlists (if My Playlists is selected and playlists exist)
        if (uiState.isMyPlaylistsSelected && uiState.playlists.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                item {
                    NisCategoryChip(
                        text = "All Custom",
                        isSelected = uiState.selectedPlaylistId == null,
                        onClick = { viewModel.selectMyPlaylists(null) }
                    )
                }
                items(uiState.playlists, key = { it.id }) { pl ->
                    NisCategoryChip(
                        text = pl.name,
                        isSelected = uiState.selectedPlaylistId == pl.id,
                        onClick = { viewModel.selectMyPlaylists(pl.id) }
                    )
                }
            }
        }

        // Filter Chips Rails: Regions / Countries
        if (uiState.countries.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                items(uiState.countries, key = { it }) { country ->
                    NisCategoryChip(
                        text = "Region: $country",
                        isSelected = uiState.selectedCountry == country,
                        onClick = { viewModel.selectCountry(country) }
                    )
                }
            }
        }

        // Filter Chips Rails: Languages
        if (uiState.languages.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                items(uiState.languages, key = { it }) { lang ->
                    NisCategoryChip(
                        text = lang,
                        isSelected = uiState.selectedLanguage == lang,
                        onClick = { viewModel.selectLanguage(lang) }
                    )
                }
            }
        }

        // Content Area: Skeleton Loader / Empty State / Virtualized Grid
        if (uiState.isLoading) {
            ChannelGridSkeleton(modifier = Modifier.weight(1f))
        } else if (filteredChannels.isEmpty()) {
            NisEmptyState(
                title = "No channels found",
                message = if (searchQuery.isNotEmpty()) {
                    "No channels match \"$searchQuery\". Try a different search term or clear the filter."
                } else {
                    "No channels found in this category or language."
                },
                icon = NisIcons.FilterList,
                actionText = "Reset Filters",
                onActionClick = {
                    searchQuery = ""
                    viewModel.selectCategory(null)
                },
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = NisDimens.MaxContentWidth)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = filteredChannels,
                    key = { it.id }
                ) { channel ->
                    NisChannelCard(
                        channel = channel,
                        isFavorite = uiState.favoriteChannelIds.contains(channel.id),
                        onClick = { onChannelClick(channel.id) },
                        onFavoriteToggle = { viewModel.toggleFavorite(channel.id) }
                    )
                }
            }
        }
    }
}
