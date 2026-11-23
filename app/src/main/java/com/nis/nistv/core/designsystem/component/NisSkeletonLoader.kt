package com.nis.nistv.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nis.nistv.core.designsystem.theme.NisBadgeShape
import com.nis.nistv.core.designsystem.theme.NisChannelCardShape
import com.nis.nistv.core.designsystem.theme.NisMotion
import com.nis.nistv.core.designsystem.theme.NisShapes
import com.nis.nistv.core.designsystem.theme.NisTheme
import com.nis.nistv.core.designsystem.theme.NisThumbnailShape

/**
 * Shimmer brush for skeleton loaders:
 * - Surface -> Elevated -> High gradient progression
 * - Reduced-motion safety (static gradient when animations are disabled)
 * - Duration derived from NisMotion tokens
 */
@Composable
fun rememberShimmerBrush(): Brush {
    val context = LocalContext.current
    val reducedMotion = remember(context) { isReducedMotionEnabled(context) }
    val colors = NisTheme.colors

    if (reducedMotion) {
        return Brush.linearGradient(
            colors = listOf(
                colors.surface,
                colors.surfaceElevated,
                colors.surface
            )
        )
    }

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = NisMotion.DurationSlowMs * 3, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )

    return Brush.linearGradient(
        colors = listOf(
            colors.surface,
            colors.surfaceElevated,
            colors.surfaceHigh,
            colors.surfaceElevated,
            colors.surface
        ),
        start = Offset(translateAnim - 400f, translateAnim - 400f),
        end = Offset(translateAnim, translateAnim)
    )
}

/**
 * Skeleton placeholder for a single ChannelCard in grids.
 */
@Composable
fun ChannelCardSkeleton(
    modifier: Modifier = Modifier
) {
    val brush = rememberShimmerBrush()
    val colors = NisTheme.colors
    Box(
        modifier = modifier
            .clip(NisChannelCardShape)
            .background(colors.surface)
            .border(1.dp, colors.borderSubtle, NisChannelCardShape)
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(NisThumbnailShape)
                        .background(brush)
                )
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 20.dp)
                        .clip(NisBadgeShape)
                        .background(brush)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(14.dp)
                    .clip(NisShapes.extraSmall)
                    .background(brush)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.45f)
                    .height(11.dp)
                    .clip(NisShapes.extraSmall)
                    .background(brush)
            )
        }
    }
}

/**
 * Skeleton placeholder for a horizontal channel rail card.
 */
@Composable
fun ChannelRailCardSkeleton(
    modifier: Modifier = Modifier
) {
    val brush = rememberShimmerBrush()
    val colors = NisTheme.colors
    Box(
        modifier = modifier
            .width(148.dp)
            .clip(NisChannelCardShape)
            .background(colors.surface)
            .border(1.dp, colors.borderSubtle, NisChannelCardShape)
            .padding(10.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .clip(NisThumbnailShape)
                    .background(brush)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(13.dp)
                    .clip(NisShapes.extraSmall)
                    .background(brush)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(10.dp)
                    .clip(NisShapes.extraSmall)
                    .background(brush)
            )
        }
    }
}

/**
 * Skeleton placeholder for an entire horizontal rail.
 */
@Composable
fun ChannelRailSkeleton(
    modifier: Modifier = Modifier,
    titleWidthFraction: Float = 0.4f
) {
    val brush = rememberShimmerBrush()
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .fillMaxWidth(titleWidthFraction)
                .height(20.dp)
                .clip(NisShapes.extraSmall)
                .background(brush)
        )
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            userScrollEnabled = false
        ) {
            items(4) {
                ChannelRailCardSkeleton()
            }
        }
    }
}

/**
 * Skeleton placeholder for channel grid.
 */
@Composable
fun ChannelGridSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 6
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = false
    ) {
        items(itemCount) {
            ChannelCardSkeleton()
        }
    }
}

/**
 * Hero Billboard skeleton placeholder.
 */
@Composable
fun HeroBannerSkeleton(
    modifier: Modifier = Modifier
) {
    val brush = rememberShimmerBrush()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(340.dp)
            .background(brush)
    )
}
