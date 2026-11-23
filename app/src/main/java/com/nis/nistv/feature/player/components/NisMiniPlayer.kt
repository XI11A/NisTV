package com.nis.nistv.feature.player.components

import androidx.annotation.OptIn
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import com.nis.nistv.core.database.entity.ChannelEntity
import com.nis.nistv.core.designsystem.component.ChannelMonogram
import com.nis.nistv.core.designsystem.icon.NisIcons
import com.nis.nistv.core.designsystem.theme.*
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Premium docked MiniPlayer.
 * Features:
 * - Live video preview canvas / fallback channel monogram
 * - Real-time channel and programme titles
 * - Play/Pause and close actions (touch target >= 44dp)
 * - Horizontal swipe-to-dismiss gesture
 * - 1-tap expansion to full player
 */
@OptIn(UnstableApi::class)
@Composable
fun NisMiniPlayer(
    channel: ChannelEntity,
    programmeTitle: String?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    exoPlayer: ExoPlayer,
    onExpand: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onClose: () -> Unit,
    isDemo: Boolean = false,
    demoRemainingSeconds: Int = 0,
    modifier: Modifier = Modifier
) {
    val offsetX = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val dismissThresholdPx = with(density) { 90.dp.toPx() }
    val colors = NisTheme.colors

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .offset { IntOffset(offsetX.value.roundToInt(), 0) }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (abs(offsetX.value) > dismissThresholdPx) {
                            coroutineScope.launch {
                                offsetX.animateTo(
                                    targetValue = if (offsetX.value > 0) 1000f else -1000f,
                                    animationSpec = tween(NisMotion.DurationNormalMs, easing = NisMotion.EasingEmphasized)
                                )
                                onClose()
                                offsetX.snapTo(0f)
                            }
                        } else {
                            coroutineScope.launch {
                                offsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = tween(NisMotion.DurationNormalMs, easing = NisMotion.EasingStandard)
                                )
                            }
                        }
                    },
                    onDragCancel = {
                        coroutineScope.launch {
                            offsetX.animateTo(
                                targetValue = 0f,
                                animationSpec = tween(NisMotion.DurationFastMs, easing = NisMotion.EasingStandard)
                            )
                        }
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        coroutineScope.launch {
                            offsetX.snapTo(offsetX.value + dragAmount)
                        }
                    }
                )
            }
            .clip(NisMiniPlayerShape)
            .background(colors.surfaceElevated)
            .border(1.dp, colors.borderSubtle, NisMiniPlayerShape)
            .clickable(onClick = onExpand)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Live 16:9 Thumbnail Preview or Logo Tile
                Box(
                    modifier = Modifier
                        .size(width = 88.dp, height = 50.dp)
                        .clip(NisThumbnailShape)
                        .background(colors.surfaceElevated)
                        .border(1.dp, colors.borderSubtle, NisThumbnailShape)
                ) {
                    if (channel.logoUrl.isNotBlank()) {
                        AsyncImage(
                            model = channel.logoUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp)
                        )
                    } else {
                        ChannelMonogram(
                            name = channel.cleanName.ifEmpty { channel.name },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    // Live ExoPlayer Surface
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                useController = false
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            }
                        },
                        update = { view ->
                            view.player = exoPlayer
                        },
                        onRelease = { view ->
                            view.player = null
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Buffering Spinner
                    if (isBuffering) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(NisScrimMedium),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = NisAccentPrimary
                            )
                        }
                    }

                    // Live Badge in corner
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(3.dp)
                            .background(NisLiveRed, NisBadgeShape)
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .background(Color.White, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "LIVE",
                            color = Color.White,
                            style = NisTheme.textExtras.micro,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 2. Channel & Program Info
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp, end = 6.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = channel.cleanName.ifEmpty { channel.name },
                        color = colors.textPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (!programmeTitle.isNullOrBlank()) {
                            programmeTitle
                        } else {
                            channel.category.ifEmpty { "Live TV" }
                        },
                        color = colors.primary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Demo Mode remaining time chip if in demo
                if (isDemo && demoRemainingSeconds > 0) {
                    Box(
                        modifier = Modifier
                            .clip(NisBadgeShape)
                            .background(colors.warning.copy(alpha = 0.15f))
                            .border(0.5.dp, colors.warning.copy(alpha = 0.5f), NisBadgeShape)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${demoRemainingSeconds}s",
                            color = colors.warning,
                            style = NisTheme.textExtras.micro,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                // 3. Play / Pause & Close Controls (guaranteed 48dp touch target)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = onTogglePlayPause,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) NisIcons.Pause else NisIcons.Play,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = NisIcons.Close,
                            contentDescription = "Close mini player",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Accent bottom progress line in brand gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .background(Brush.horizontalGradient(colors.brandGradient))
            )
        }
    }
}
