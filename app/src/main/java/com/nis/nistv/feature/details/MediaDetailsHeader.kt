@file:OptIn(ExperimentalSharedTransitionApi::class)
package com.nis.nistv.feature.details

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.nis.nistv.core.designsystem.theme.*
import com.nis.nistv.core.designsystem.transition.SharedKeys
import com.nis.nistv.core.designsystem.transition.SharedRole
import com.nis.nistv.core.designsystem.transition.nisSharedBounds
import com.nis.nistv.data.tmdb.model.MediaPreview

/**
 * Details Screen Header featuring:
 * 1. Main Landscape Backdrop Image across the top with a subtle cinematic gradient.
 * 2. Vertical Poster Card positioned in the left-side corner under/overlapping the landscape backdrop,
 *    serving as the exact destination for container transform shared bounds from NisPosterMediaCard.
 * 3. Title, media type, rating, and year displayed on the right of the vertical poster.
 */
@Composable
fun MediaDetailsHeader(
    preview: MediaPreview,
    origin: String,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val effectiveOrigin = origin.ifBlank { "default" }
    val cardShape = NisMediaCardShape

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 400.dp)
    ) {
        // 1. Main Landscape Image (Backdrop)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .background(NisTheme.colors.surface)
        ) {
            val backdropUrl = preview.backdropUrl ?: preview.posterUrl
            if (!backdropUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(ctx)
                        .data(backdropUrl)
                        .memoryCacheKey(preview.backdropCacheKey)
                        .crossfade(true)
                        .build(),
                    contentDescription = "${preview.title} backdrop",
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(NisTheme.colors.surfaceElevated)
                )
            }

            // Cinematic gradient: soft dark top for back button, fading into NisBackground at bottom
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to (if (NisTheme.colors.isDark) NisScrimMedium else NisTheme.colors.background.copy(alpha = 0.85f)),
                            0.30f to Color.Transparent,
                            0.65f to Color.Transparent,
                            1.0f to NisTheme.colors.background
                        )
                    )
            )
        }

        // 2. Vertical Poster Card in left side corner + Title & Meta on right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Vertical Poster Card (Shared Element Destination)
            Box(
                modifier = Modifier
                    .width(115.dp)
                    .height(165.dp)
                    .nisSharedBounds(
                        key = SharedKeys.of(preview.mediaType, preview.tmdbId, SharedRole.CONTAINER, effectiveOrigin),
                        clipShape = cardShape,
                        zIndex = 2f
                    )
                    .clip(cardShape)
                    .background(NisTheme.colors.surface)
                    .border(1.5.dp, NisTheme.colors.border, cardShape)
            ) {
                val posterUrl = preview.posterUrl ?: preview.backdropUrl
                if (!posterUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(ctx)
                            .data(posterUrl)
                            .memoryCacheKey(preview.posterCacheKey)
                            .crossfade(true)
                            .build(),
                        contentDescription = preview.title,
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.TopCenter,
                        modifier = Modifier
                            .fillMaxSize()
                            .nisSharedBounds(
                                key = SharedKeys.of(preview.mediaType, preview.tmdbId, SharedRole.IMAGE, effectiveOrigin),
                                clipShape = cardShape,
                                zIndex = 3f
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
                            text = preview.title.take(1),
                            color = NisTheme.colors.textMuted,
                            style = MaterialTheme.typography.headlineLarge
                        )
                    }
                }
            }

            // Right side metadata column beside vertical poster
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 6.dp)
            ) {
                // Media Type Badge + Rating + Release Year
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(NisBadgeShape)
                            .background(NisTheme.colors.primary)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (preview.mediaType.equals("tv", ignoreCase = true)) "SERIES" else "MOVIE",
                            color = Color.White,
                            style = NisTheme.textExtras.micro,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    val rating = preview.voteAverage ?: 0.0
                    if (rating > 0.0) {
                        Box(
                            modifier = Modifier
                                .clip(NisBadgeShape)
                                .background(NisTheme.colors.warning)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = String.format(java.util.Locale.US, "★ %.1f", rating),
                                color = Color.White,
                                style = NisTheme.textExtras.micro,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    preview.releaseYear?.let { year ->
                        Text(
                            text = year,
                            color = NisTheme.colors.textSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Title with Shared Element Bounds
                Text(
                    text = preview.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = NisTheme.colors.textPrimary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.nisSharedBounds(
                        key = SharedKeys.of(preview.mediaType, preview.tmdbId, SharedRole.TITLE, effectiveOrigin),
                        resizeMode = SharedTransitionScope.ResizeMode.ScaleToBounds(
                            ContentScale.FillWidth,
                            Alignment.CenterStart
                        ),
                        zIndex = 4f
                    )
                )
            }
        }
    }
}
