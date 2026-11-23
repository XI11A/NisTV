@file:OptIn(ExperimentalSharedTransitionApi::class)
package com.nis.nistv.feature.favorites

import android.widget.Toast
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.nis.nistv.feature.parental.ParentalPinDialog
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.nis.nistv.NisTvApplication
import com.nis.nistv.core.database.entity.ChannelEntity
import com.nis.nistv.core.database.entity.WatchLaterEntity
import com.nis.nistv.core.designsystem.component.NisCategoryChip
import com.nis.nistv.core.designsystem.component.NisChannelCard
import com.nis.nistv.core.designsystem.component.NisEmptyState
import com.nis.nistv.core.designsystem.icon.NisIcons
import com.nis.nistv.core.designsystem.theme.*
import com.nis.nistv.core.designsystem.transition.SharedKeys
import com.nis.nistv.core.designsystem.transition.SharedOrigins
import com.nis.nistv.core.designsystem.transition.SharedRole
import com.nis.nistv.core.designsystem.transition.debouncedClickable
import com.nis.nistv.core.designsystem.transition.nisSharedBounds
import com.nis.nistv.data.tmdb.model.MediaPreview
import com.nis.nistv.data.tmdb.model.toPreview

/**
 * My List / Favorites Screen:
 * - Unified interface for saved Movies & TV Shows (Watch Later) and Live TV Channels
 * - Category tabs for switching between Movies & Shows vs Channels
 * - Visual poster cards with ratings, media type badges, and 1-tap delete
 * - Dedicated empty states with actions to Discover or Live TV
 */
@Composable
fun FavoritesScreen(
    onChannelClick: (String) -> Unit,
    onExploreClick: () -> Unit,
    modifier: Modifier = Modifier,
    onMediaClick: (MediaPreview, String) -> Unit = { _, _ -> },
    onExploreMediaClick: () -> Unit = onExploreClick,
    viewModel: FavoritesViewModel = viewModel(
        factory = FavoritesViewModel.provideFactory(
            channelRepository = (LocalContext.current.applicationContext as NisTvApplication).container.channelRepository,
            watchLaterRepository = (LocalContext.current.applicationContext as NisTvApplication).container.watchLaterRepository,
            userPreferences = (LocalContext.current.applicationContext as NisTvApplication).container.userPreferences,
            parentalControlManager = (LocalContext.current.applicationContext as NisTvApplication).container.parentalControlManager
        )
    )
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val watchLaterList by viewModel.watchLaterList.collectAsState()
    val lockedChannelIds by viewModel.lockedChannelIds.collectAsState()
    val context = LocalContext.current
    val app = context.applicationContext as NisTvApplication
    val isOnline by app.container.networkMonitor.isOnline.collectAsState(initial = true)
    val scope = rememberCoroutineScope()

    var channelToRemove by remember { mutableStateOf<ChannelEntity?>(null) }
    var mediaToRemove by remember { mutableStateOf<WatchLaterEntity?>(null) }
    var channelToUnlock by remember { mutableStateOf<ChannelEntity?>(null) }
    var pinErrorMessage by remember { mutableStateOf<String?>(null) }

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
            Text(
                text = "My List",
                color = NisTheme.colors.textPrimary,
                style = MaterialTheme.typography.titleLarge
            )
            val currentCount = when (selectedTab) {
                FavoritesTab.MOVIES_AND_SHOWS -> watchLaterList.size
                FavoritesTab.LIVE_CHANNELS -> favorites.size
            }
            if (currentCount > 0) {
                Text(
                    text = "$currentCount items",
                    color = NisTheme.colors.textMuted,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        // Tab Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = NisDimens.MaxContentWidth)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NisCategoryChip(
                text = "Movies & Shows (${watchLaterList.size})",
                isSelected = selectedTab == FavoritesTab.MOVIES_AND_SHOWS,
                onClick = { viewModel.selectTab(FavoritesTab.MOVIES_AND_SHOWS) }
            )
            NisCategoryChip(
                text = "Live Channels (${favorites.size})",
                isSelected = selectedTab == FavoritesTab.LIVE_CHANNELS,
                onClick = { viewModel.selectTab(FavoritesTab.LIVE_CHANNELS) }
            )
        }

        when (selectedTab) {
            FavoritesTab.MOVIES_AND_SHOWS -> {
                if (!isOnline) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = NisDimens.MaxContentWidth)
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clip(NisShapes.small)
                            .background(NisTheme.colors.surfaceElevated)
                            .border(1.dp, NisTheme.colors.border, NisShapes.small)
                            .padding(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(NisIcons.Info, contentDescription = null, tint = NisTheme.colors.warning, modifier = Modifier.size(18.dp))
                            Text(
                                text = "You're offline. Full online details require an internet connection. Use Downloads to play saved offline media.",
                                color = NisTheme.colors.textSecondary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                if (watchLaterList.isEmpty()) {
                    NisEmptyState(
                        title = "Your Watchlist is empty",
                        message = "Save movies and series while exploring to easily watch or download them later.",
                        icon = NisIcons.FavoritesBorder,
                        actionText = "Browse Movies & Shows",
                        onActionClick = onExploreMediaClick
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 115.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = NisDimens.MaxContentWidth)
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(
                            items = watchLaterList,
                            key = { it.id }
                        ) { item ->
                            val preview = item.toPreview()
                            WatchLaterMediaCard(
                                item = item,
                                origin = SharedOrigins.WATCH_LATER,
                                onClick = {
                                    if (!isOnline) {
                                        Toast.makeText(context, "You are offline. Details require internet. Check Downloads tab.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        onMediaClick(preview, SharedOrigins.WATCH_LATER)
                                    }
                                },
                                onRemoveClick = { mediaToRemove = item }
                            )
                        }
                    }
                }
            }

            FavoritesTab.LIVE_CHANNELS -> {
                if (favorites.isEmpty()) {
                    NisEmptyState(
                        title = "No favorite channels yet",
                        message = "Tap the heart icon on any channel while browsing or playing to add it to your personal watchlist for instant access.",
                        icon = NisIcons.FavoritesBorder,
                        actionText = "Browse Live Channels",
                        onActionClick = onExploreClick
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
                            items = favorites,
                            key = { it.id }
                        ) { channel ->
                            val isLocked = channel.id in lockedChannelIds
                            NisChannelCard(
                                channel = channel,
                                isFavorite = true,
                                isLocked = isLocked,
                                onClick = {
                                    if (isLocked) {
                                        channelToUnlock = channel
                                        pinErrorMessage = null
                                    } else {
                                        onChannelClick(channel.id)
                                    }
                                },
                                onFavoriteToggle = {
                                    viewModel.toggleFavorite(channel.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Confirmation dialog for Channel Removal
    channelToRemove?.let { channel ->
        AlertDialog(
            onDismissRequest = { channelToRemove = null },
            containerColor = NisTheme.colors.surface,
            title = {
                Text(
                    text = "Remove Favorite?",
                    color = NisTheme.colors.textPrimary,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text(
                    text = "Do you want to remove ${channel.cleanName.ifEmpty { channel.name }} from your favorites?",
                    color = NisTheme.colors.textSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.toggleFavorite(channel.id)
                        channelToRemove = null
                    }
                ) {
                    Text(text = "Remove", color = NisTheme.colors.live)
                }
            },
            dismissButton = {
                TextButton(onClick = { channelToRemove = null }) {
                    Text(text = "Cancel", color = NisTheme.colors.textSecondary)
                }
            }
        )
    }

    // Confirmation dialog for Media Removal
    mediaToRemove?.let { media ->
        AlertDialog(
            onDismissRequest = { mediaToRemove = null },
            containerColor = NisTheme.colors.surface,
            title = {
                Text(
                    text = "Remove from Watch Later?",
                    color = NisTheme.colors.textPrimary,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text(
                    text = "Do you want to remove \"${media.title}\" from your Watch Later list?",
                    color = NisTheme.colors.textSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeFromWatchLater(media.mediaType, media.mediaId)
                        mediaToRemove = null
                    }
                ) {
                    Text(text = "Remove", color = NisTheme.colors.live)
                }
            },
            dismissButton = {
                TextButton(onClick = { mediaToRemove = null }) {
                    Text(text = "Cancel", color = NisTheme.colors.textSecondary)
                }
            }
        )
    }

    // Parental PIN Dialog for unlocking protected favorite channel
    channelToUnlock?.let { channel ->
        ParentalPinDialog(
            title = "Locked Channel",
            subtitle = "Enter your Parental PIN to watch ${channel.cleanName.ifEmpty { channel.name }}.",
            errorMessage = pinErrorMessage,
            onDismissRequest = {
                channelToUnlock = null
                pinErrorMessage = null
            },
            onConfirm = { pin ->
                scope.launch {
                    val success = viewModel.verifyParentalPin(pin)
                    if (success) {
                        val targetId = channel.id
                        channelToUnlock = null
                        pinErrorMessage = null
                        onChannelClick(targetId)
                    } else {
                        pinErrorMessage = "Incorrect PIN"
                    }
                }
            },
            onForgotPinClick = {
                Toast.makeText(context, "Go to Profile > Parental Controls to reset your PIN", Toast.LENGTH_LONG).show()
            }
        )
    }
}

@Composable
fun WatchLaterMediaCard(
    item: WatchLaterEntity,
    onClick: () -> Unit,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier,
    origin: String = SharedOrigins.WATCH_LATER
) {
    val shape = NisMediaCardShape
    val preview = item.toPreview()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .nisSharedBounds(
                key = SharedKeys.of(item.mediaType, item.mediaId.toInt(), SharedRole.CONTAINER, origin),
                clipShape = shape,
                zIndex = 1f
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
                .aspectRatio(2f / 3f)
                .clip(shape)
                .background(NisTheme.colors.surface)
                .border(1.dp, NisTheme.colors.border, shape)
        ) {
            val fullPosterUrl = item.posterPath?.let { path ->
                if (path.startsWith("http")) path else "https://image.tmdb.org/t/p/w500$path"
            }

            if (!fullPosterUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(fullPosterUrl)
                        .memoryCacheKey(preview.posterCacheKey)
                        .crossfade(true)
                        .build(),
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .nisSharedBounds(
                            key = SharedKeys.of(item.mediaType, item.mediaId.toInt(), SharedRole.IMAGE, origin),
                            clipShape = shape,
                            zIndex = 2f
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

            // Media Type badge (top-start)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .clip(NisBadgeShape)
                    .background(NisScrimMedium)
                    .border(0.5.dp, NisTheme.colors.borderSubtle, NisBadgeShape)
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (item.mediaType.equals("tv", ignoreCase = true)) "SERIES" else "MOVIE",
                    color = NisTheme.colors.primary,
                    style = NisTheme.textExtras.micro,
                    fontWeight = FontWeight.Bold
                )
            }

            // Rating pill badge (top-end)
            val rating = item.voteAverage ?: 0.0
            if (rating > 0) {
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
                        text = String.format(java.util.Locale.US, "★ %.1f", rating),
                        color = NisTheme.colors.warning,
                        style = NisTheme.textExtras.micro,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Quick Remove Button (bottom-end)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(NisScrimStrong)
                    .border(0.5.dp, NisTheme.colors.borderSubtle, CircleShape)
                    .clickable(
                        role = Role.Button,
                        onClick = onRemoveClick,
                        onClickLabel = "Remove from Watch Later"
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = NisIcons.Close,
                    contentDescription = "Remove",
                    tint = NisTheme.colors.textSecondary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = item.title,
            color = NisTheme.colors.textPrimary,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        item.releaseYear?.take(4)?.let { year ->
            Text(
                text = year,
                color = NisTheme.colors.textMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
