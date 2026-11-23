package com.nis.nistv.core.designsystem.component

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nis.nistv.NisTvApplication
import com.nis.nistv.core.database.entity.WatchLaterEntity
import com.nis.nistv.core.designsystem.icon.NisIcons
import com.nis.nistv.core.designsystem.theme.NisAccentPrimary
import com.nis.nistv.core.designsystem.theme.NisBorderSubtle
import com.nis.nistv.core.designsystem.theme.NisDimens
import com.nis.nistv.core.designsystem.theme.NisMotion
import com.nis.nistv.core.designsystem.theme.NisScrimMedium
import com.nis.nistv.core.designsystem.theme.NisTextPrimary
import com.nis.nistv.data.tmdb.model.TmdbMediaItem
import com.nis.nistv.data.tmdb.model.TmdbMediaType
import kotlinx.coroutines.launch

/**
 * Cinematic Obsidian 2.0 Watch Later Bookmark Button:
 * - Guaranteed 48dp minimum touch target area
 * - Preserved visual size with high-contrast icon
 * - NisScrimMedium backdrop when enabled
 * - Active state transition to NisAccentPrimary
 */
@Composable
fun NisBookmarkButton(
    mediaType: String,
    mediaId: Long,
    title: String,
    posterPath: String? = null,
    backdropPath: String? = null,
    overview: String? = null,
    releaseYear: String? = null,
    voteAverage: Double? = null,
    genres: String? = null,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    iconSize: Dp = 18.dp,
    showBackground: Boolean = true,
    showToastFeedback: Boolean = true,
    onToggle: ((Boolean) -> Unit)? = null
) {
    val context = LocalContext.current
    val app = context.applicationContext as NisTvApplication
    val scope = rememberCoroutineScope()
    val cleanMediaType = mediaType.lowercase()

    val isSaved by app.container.watchLaterRepository.isInWatchLater(cleanMediaType, mediaId)
        .collectAsState(initial = false)

    val colorSpec = tween<Color>(
        durationMillis = NisMotion.DurationNormalMs,
        easing = NisMotion.EasingStandard
    )

    val iconTint by animateColorAsState(
        targetValue = if (isSaved) NisAccentPrimary else NisTextPrimary,
        animationSpec = colorSpec,
        label = "bookmarkTint"
    )

    val targetBgColor = if (showBackground) {
        if (isSaved) NisAccentPrimary.copy(alpha = 0.18f) else NisScrimMedium
    } else {
        Color.Transparent
    }

    val targetBorderColor = if (showBackground) {
        if (isSaved) NisAccentPrimary else NisBorderSubtle
    } else {
        Color.Transparent
    }

    val bgColor by animateColorAsState(
        targetValue = targetBgColor,
        animationSpec = colorSpec,
        label = "bookmarkBg"
    )

    val borderColor by animateColorAsState(
        targetValue = targetBorderColor,
        animationSpec = colorSpec,
        label = "bookmarkBorder"
    )

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = NisDimens.TouchTarget, minHeight = NisDimens.TouchTarget)
            .nisPressable(
                onClick = {
                    scope.launch {
                        val compositeId = "${cleanMediaType}_$mediaId"
                        var finalPoster = posterPath
                        var finalBackdrop = backdropPath
                        var finalOverview = overview
                        var finalYear = releaseYear
                        var finalVote = voteAverage
                        var finalGenres = genres

                        // Gracefully fetch full details if missing fields from lightweight card
                        if (finalBackdrop == null || finalOverview.isNullOrBlank() || finalGenres.isNullOrBlank()) {
                            try {
                                if (cleanMediaType == "movie") {
                                    val details = app.container.tmdbRepository.getMovieDetails(mediaId)
                                    finalPoster = finalPoster ?: details.posterPath
                                    finalBackdrop = finalBackdrop ?: details.backdropPath
                                    finalOverview = if (finalOverview.isNullOrBlank()) details.overview else finalOverview
                                    finalYear = finalYear ?: details.releaseDate?.take(4)
                                    finalVote = finalVote ?: details.voteAverage
                                    finalGenres = finalGenres ?: details.genres.joinToString(", ") { it.name }.ifBlank { null }
                                } else if (cleanMediaType == "tv") {
                                    val details = app.container.tmdbRepository.getTvDetails(mediaId)
                                    finalPoster = finalPoster ?: details.posterPath
                                    finalBackdrop = finalBackdrop ?: details.backdropPath
                                    finalOverview = if (finalOverview.isNullOrBlank()) details.overview else finalOverview
                                    finalYear = finalYear ?: details.firstAirDate?.take(4)
                                    finalVote = finalVote ?: details.voteAverage
                                    finalGenres = finalGenres ?: details.genres.joinToString(", ") { it.name }.ifBlank { null }
                                }
                            } catch (_: Exception) {
                                // Gracefully proceed with whatever is available
                            }
                        }

                        val entity = WatchLaterEntity(
                            id = compositeId,
                            mediaId = mediaId,
                            mediaType = cleanMediaType,
                            title = title,
                            posterPath = finalPoster,
                            backdropPath = finalBackdrop,
                            overview = finalOverview,
                            releaseYear = finalYear,
                            voteAverage = finalVote,
                            genres = finalGenres,
                            addedAt = System.currentTimeMillis()
                        )
                        val added = app.container.watchLaterRepository.toggleWatchLater(entity)
                        if (added) {
                            val domainItem = TmdbMediaItem(
                                id = mediaId,
                                title = title,
                                overview = overview.orEmpty(),
                                posterPath = posterPath,
                                backdropPath = backdropPath,
                                releaseYear = releaseYear,
                                voteAverage = voteAverage ?: 0.0,
                                mediaType = if (cleanMediaType == "tv") TmdbMediaType.TV else TmdbMediaType.MOVIE
                            )
                            app.container.recommendationEngine.recordMediaInteraction(
                                media = domainItem,
                                interactionType = "FAVORITE"
                            )
                            if (showToastFeedback) {
                                Toast.makeText(context, "Added to Watch Later", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            if (showToastFeedback) {
                                Toast.makeText(context, "Removed from Watch Later", Toast.LENGTH_SHORT).show()
                            }
                        }
                        onToggle?.invoke(added)
                    }
                },
                role = Role.Button,
                onClickLabel = if (isSaved) "Remove from Watch Later" else "Add to Watch Later",
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(bgColor)
                .border(if (showBackground) 1.dp else 0.dp, borderColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSaved) NisIcons.Bookmark else NisIcons.BookmarkBorder,
                contentDescription = if (isSaved) "Saved to Watch Later" else "Save to Watch Later",
                tint = iconTint,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@Composable
fun NisBookmarkButton(
    media: TmdbMediaItem,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    iconSize: Dp = 18.dp,
    showBackground: Boolean = true,
    showToastFeedback: Boolean = true,
    onToggle: ((Boolean) -> Unit)? = null
) {
    NisBookmarkButton(
        mediaType = media.mediaType.value,
        mediaId = media.id,
        title = media.title,
        posterPath = media.posterPath,
        backdropPath = media.backdropPath,
        overview = media.overview,
        releaseYear = media.releaseYear,
        voteAverage = media.voteAverage,
        genres = null,
        modifier = modifier,
        size = size,
        iconSize = iconSize,
        showBackground = showBackground,
        showToastFeedback = showToastFeedback,
        onToggle = onToggle
    )
}
