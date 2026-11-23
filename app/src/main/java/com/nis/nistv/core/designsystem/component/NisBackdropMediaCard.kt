@file:OptIn(ExperimentalSharedTransitionApi::class)
package com.nis.nistv.core.designsystem.component

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.nis.nistv.core.designsystem.theme.NisAccentAmber
import com.nis.nistv.core.designsystem.theme.NisBadgeShape
import com.nis.nistv.core.designsystem.theme.NisMediaCardShape
import com.nis.nistv.core.designsystem.theme.NisTheme
import com.nis.nistv.core.designsystem.transition.SharedKeys
import com.nis.nistv.core.designsystem.transition.SharedRole
import com.nis.nistv.core.designsystem.transition.debouncedClickable
import com.nis.nistv.core.designsystem.transition.nisSharedBounds
import com.nis.nistv.data.tmdb.model.MediaPreview

/**
 * 16:9 Backdrop Media Card used in Home rails for Netflix-style shared element
 * container transform transitions to the Details poster header.
 * Derives its clip shape from canonical NisMediaCardShape.
 */
@Composable
fun NisBackdropMediaCard(
    preview: MediaPreview,
    origin: String,
    onClick: (MediaPreview, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val shape = NisMediaCardShape
    val imageUrl = preview.backdropUrl ?: preview.posterUrl
    val colors = NisTheme.colors

    Box(
        modifier
            .nisSharedBounds(
                key = SharedKeys.of(preview.mediaType, preview.tmdbId, SharedRole.CONTAINER, origin),
                clipShape = shape,
                zIndex = 1f,
            )
            .width(280.dp)
            .aspectRatio(16f / 9f)
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.borderSubtle, shape)
            .nisPressable(
                role = Role.Button,
                onClickLabel = preview.title,
                shape = shape,
                debounced = true,
                onClick = { onClick(preview, origin) }
            )
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(ctx)
                    .data(imageUrl)
                    .memoryCacheKey(preview.backdropCacheKey)
                    .crossfade(true)
                    .build(),
                contentDescription = preview.title,
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .nisSharedBounds(
                        key = SharedKeys.of(preview.mediaType, preview.tmdbId, SharedRole.IMAGE, origin),
                        clipShape = shape,
                        zIndex = 2f,
                    ),
            )
        }

        // Bottom scrim for title contrast using standard scrim tokens
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, colors.immersiveScrimMedium.copy(alpha = 0.4f), colors.immersiveScrimStrong)
                    )
                )
        )

        // Rating pill top-end
        if (preview.voteAverage != null && preview.voteAverage > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(NisBadgeShape)
                    .background(colors.immersiveScrimMedium)
                    .border(0.5.dp, colors.borderSubtle, NisBadgeShape)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = String.format(java.util.Locale.US, "★ %.1f", preview.voteAverage),
                    color = colors.warning,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        Text(
            text = preview.title,
            style = MaterialTheme.typography.titleMedium,
            color = colors.immersiveTextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
                .nisSharedBounds(
                    key = SharedKeys.of(preview.mediaType, preview.tmdbId, SharedRole.TITLE, origin),
                    resizeMode = SharedTransitionScope.ResizeMode.ScaleToBounds(
                        ContentScale.FillWidth,
                        Alignment.CenterStart
                    ),
                    zIndex = 3f,
                ),
        )
    }
}
