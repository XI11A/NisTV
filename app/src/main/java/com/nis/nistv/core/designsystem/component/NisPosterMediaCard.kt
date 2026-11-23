@file:OptIn(ExperimentalSharedTransitionApi::class)
package com.nis.nistv.core.designsystem.component

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.nis.nistv.core.designsystem.theme.NisBadgeShape
import com.nis.nistv.core.designsystem.theme.NisMediaCardShape
import com.nis.nistv.core.designsystem.theme.NisTheme
import com.nis.nistv.core.designsystem.transition.SharedKeys
import com.nis.nistv.core.designsystem.transition.SharedRole
import com.nis.nistv.core.designsystem.transition.debouncedClickable
import com.nis.nistv.core.designsystem.transition.nisSharedBounds
import com.nis.nistv.data.tmdb.model.MediaPreview

/**
 * Vertical 2:3 TMDB Poster Media Card used in horizontal rails and grids across
 * Home, Discover, Search, and Favorites.
 * Derives its clip shape from the canonical NisMediaCardShape token for seamless
 * shared bounds morphing to MediaDetailsScreen.
 */
@Composable
fun NisPosterMediaCard(
    preview: MediaPreview,
    onClick: (MediaPreview, String) -> Unit,
    modifier: Modifier = Modifier,
    origin: String? = null,
    fixedWidth: Boolean = true,
    showBookmark: Boolean = true
) {
    val shape = NisMediaCardShape
    val effectiveOrigin = origin ?: "default"
    val colors = NisTheme.colors
    Column(
        modifier = modifier
            .then(if (fixedWidth) Modifier.width(115.dp) else Modifier.fillMaxWidth())
            .then(
                if (!origin.isNullOrBlank()) {
                    Modifier.nisSharedBounds(
                        key = SharedKeys.of(preview.mediaType, preview.tmdbId, SharedRole.CONTAINER, effectiveOrigin),
                        clipShape = shape,
                        zIndex = 1f
                    )
                } else Modifier
            )
            .nisPressable(
                role = Role.Button,
                onClickLabel = preview.title,
                shape = shape,
                debounced = true,
                onClick = { onClick(preview, effectiveOrigin) }
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (fixedWidth) Modifier.height(165.dp) else Modifier.aspectRatio(2f / 3f))
                .clip(shape)
                .background(colors.surface)
                .border(1.dp, colors.borderSubtle, shape)
        ) {
            val posterUrl = preview.posterUrl ?: preview.backdropUrl
            if (!posterUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(posterUrl)
                        .memoryCacheKey(preview.posterCacheKey)
                        .crossfade(true)
                        .build(),
                    contentDescription = preview.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (!origin.isNullOrBlank()) {
                                Modifier.nisSharedBounds(
                                    key = SharedKeys.of(preview.mediaType, preview.tmdbId, SharedRole.IMAGE, effectiveOrigin),
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
                        .background(colors.surfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = preview.title.take(1),
                        color = colors.textMuted,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Rating pill badge top-end
            val rating = preview.voteAverage ?: 0.0
            if (rating > 0.0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(NisBadgeShape)
                        .background(colors.scrimMedium)
                        .border(0.5.dp, colors.borderSubtle, NisBadgeShape)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = String.format(java.util.Locale.US, "★ %.1f", rating),
                        color = colors.warning,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            // Watch later bookmark top-start
            if (showBookmark) {
                NisBookmarkButton(
                    mediaType = preview.mediaType,
                    mediaId = preview.tmdbId.toLong(),
                    title = preview.title,
                    posterPath = preview.posterPath,
                    backdropPath = preview.backdropPath,
                    releaseYear = preview.releaseYear,
                    voteAverage = preview.voteAverage,
                    size = 28.dp,
                    iconSize = 15.dp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(5.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = preview.title,
            color = colors.textPrimary,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = if (!origin.isNullOrBlank()) {
                Modifier.nisSharedBounds(
                    key = SharedKeys.of(preview.mediaType, preview.tmdbId, SharedRole.TITLE, effectiveOrigin),
                    resizeMode = SharedTransitionScope.ResizeMode.ScaleToBounds(
                        ContentScale.FillWidth,
                        Alignment.CenterStart
                    ),
                    zIndex = 3f
                )
            } else Modifier
        )

        preview.releaseYear?.let { year ->
            Text(
                text = year,
                color = colors.textMuted,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
        }
    }
}
