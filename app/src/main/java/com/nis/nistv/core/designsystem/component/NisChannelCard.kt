package com.nis.nistv.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.nis.nistv.core.database.entity.ChannelEntity
import com.nis.nistv.core.designsystem.icon.NisIcons
import com.nis.nistv.core.designsystem.theme.NisBadgeShape
import com.nis.nistv.core.designsystem.theme.NisChannelCardShape
import com.nis.nistv.core.designsystem.theme.NisDimens
import com.nis.nistv.core.designsystem.theme.NisTheme
import com.nis.nistv.core.designsystem.theme.NisThumbnailShape

/**
 * Premium dark cinematic ChannelCard:
 * - NisSurface container with 1dp NisBorderSubtle and medium (14dp) corner radius
 * - Neutral rounded tile (10dp radius) for channel logos
 * - Monogram fallback tile when logo is missing or unavailable
 * - Tactile press feedback via nisPressable with bounded ripple
 * - Dedicated 48dp hit area for favorite toggle button
 */
@Composable
fun NisChannelCard(
    channel: ChannelEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
    isLocked: Boolean = false,
    reliabilityDotColor: Color? = null,
    onFavoriteToggle: (() -> Unit)? = null,
    showFavoriteButton: Boolean = true
) {
    var isImageError by remember(channel.logoUrl) { mutableStateOf(false) }
    val colors = NisTheme.colors

    Box(
        modifier = modifier
            .clip(NisChannelCardShape)
            .background(colors.surface)
            .border(1.dp, colors.borderSubtle, NisChannelCardShape)
            .nisPressable(
                onClick = onClick,
                role = Role.Button,
                onClickLabel = "Watch ${channel.cleanName.ifEmpty { channel.name }}",
                shape = NisChannelCardShape
            )
            .padding(12.dp)
    ) {
        Column {
            // Header Row: Logo Tile + Badges + Favorite Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ChannelLogoTile(
                    logoUrl = channel.logoUrl,
                    channelName = channel.cleanName.ifEmpty { channel.name },
                    isImageError = isImageError,
                    onError = { isImageError = true },
                    size = 46.dp
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isLocked) {
                        Icon(
                            imageVector = NisIcons.Lock,
                            contentDescription = "Restricted content",
                            tint = colors.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (channel.resolution.isNotBlank() && !channel.resolution.equals("SD", ignoreCase = true)) {
                        NisResolutionBadge(resolution = channel.resolution)
                    }

                    if (showFavoriteButton && onFavoriteToggle != null) {
                        Box(
                            modifier = Modifier
                                .defaultMinSize(minWidth = NisDimens.TouchTarget, minHeight = NisDimens.TouchTarget)
                                .nisPressable(
                                    onClick = onFavoriteToggle,
                                    role = Role.Button,
                                    onClickLabel = if (isFavorite) "Remove favorite" else "Add favorite",
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isFavorite) NisIcons.Favorites else NisIcons.FavoritesBorder,
                                contentDescription = if (isFavorite) "In Favorites" else "Add to Favorites",
                                tint = if (isFavorite) colors.live else colors.textMuted,
                                modifier = Modifier.size(NisDimens.IconSmall)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Channel Name
            Text(
                text = channel.cleanName.ifEmpty { channel.name },
                color = colors.textPrimary,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Category & Language
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (reliabilityDotColor != null) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(reliabilityDotColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = channel.category.ifEmpty { "General" },
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (channel.language.isNotBlank()) {
                    Text(
                        text = " • ",
                        color = colors.textMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = channel.language,
                        color = colors.textMuted,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Horizontal rail variant card for horizontal carousels.
 */
@Composable
fun NisRailChannelCard(
    channel: ChannelEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
    onFavoriteToggle: (() -> Unit)? = null
) {
    var isImageError by remember(channel.logoUrl) { mutableStateOf(false) }
    val colors = NisTheme.colors

    Box(
        modifier = modifier
            .width(148.dp)
            .clip(NisChannelCardShape)
            .background(colors.surface)
            .border(1.dp, colors.borderSubtle, NisChannelCardShape)
            .nisPressable(
                onClick = onClick,
                role = Role.Button,
                onClickLabel = "Play ${channel.cleanName.ifEmpty { channel.name }}",
                shape = NisChannelCardShape
            )
            .padding(10.dp)
    ) {
        Column {
            // Logo Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .clip(NisThumbnailShape)
                    .background(colors.surfaceElevated)
                    .border(1.dp, colors.borderSubtle, NisThumbnailShape),
                contentAlignment = Alignment.Center
            ) {
                if (channel.logoUrl.isNotBlank() && !isImageError) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(channel.logoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = channel.name,
                        onError = { isImageError = true },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    ChannelMonogram(
                        name = channel.cleanName.ifEmpty { channel.name },
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                if (isFavorite) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(8.dp)
                            .clip(NisBadgeShape)
                            .background(colors.live)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Channel Title
            Text(
                text = channel.cleanName.ifEmpty { channel.name },
                color = colors.textPrimary,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Channel Subtitle
            Text(
                text = if (channel.language.isNotBlank()) channel.language else channel.category,
                color = colors.textSecondary,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ChannelLogoTile(
    logoUrl: String,
    channelName: String,
    isImageError: Boolean,
    onError: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp
) {
    val context = LocalContext.current
    val colors = NisTheme.colors

    Box(
        modifier = modifier
            .size(size)
            .clip(NisThumbnailShape)
            .background(colors.surfaceElevated)
            .border(1.dp, colors.borderSubtle, NisThumbnailShape),
        contentAlignment = Alignment.Center
    ) {
        if (logoUrl.isNotBlank() && !isImageError) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(logoUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = channelName,
                onError = { onError() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(5.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            ChannelMonogram(name = channelName)
        }
    }
}

@Composable
fun ChannelMonogram(
    name: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    fontSize: TextUnit = TextUnit.Unspecified
) {
    val monogram = remember(name) {
        val parts = name.trim().split(" ").filter { it.isNotBlank() }
        when {
            parts.size >= 2 -> "${parts[0].take(1)}${parts[1].take(1)}".uppercase()
            parts.isNotEmpty() && parts[0].length >= 2 -> parts[0].take(2).uppercase()
            parts.isNotEmpty() -> parts[0].take(1).uppercase()
            else -> "TV"
        }
    }
    val colors = NisTheme.colors

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        colors.surfaceElevated,
                        colors.surfaceHigh
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = monogram,
            color = colors.primary,
            fontWeight = FontWeight.Bold,
            style = style,
            fontSize = fontSize,
            letterSpacing = 0.5.sp
        )
    }
}
