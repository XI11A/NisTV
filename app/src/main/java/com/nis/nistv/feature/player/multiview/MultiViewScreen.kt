package com.nis.nistv.feature.player.multiview

import android.content.res.Configuration
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.nis.nistv.core.database.entity.ChannelEntity
import com.nis.nistv.core.designsystem.icon.NisIcons
import com.nis.nistv.core.designsystem.theme.*
import com.nis.nistv.playback.multiview.MultiViewController
import com.nis.nistv.playback.multiview.MultiViewSlot

@OptIn(UnstableApi::class)
@Composable
fun MultiViewScreen(
    controller: MultiViewController,
    onOpenFullscreen: (ChannelEntity) -> Unit,
    onCloseMultiView: () -> Unit,
    onReplaceChannel: (MultiViewSlot) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by controller.uiState.collectAsState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    var activeActionSlot by remember { mutableStateOf<MultiViewSlot?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            controller.pauseBoth()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (isLandscape) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Primary Slot
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(4.dp)
                ) {
                    MultiViewTile(
                        slot = MultiViewSlot.PRIMARY,
                        channel = state.primarySlot.channel,
                        isAudioActive = state.activeAudioSlot == MultiViewSlot.PRIMARY,
                        isBuffering = state.primarySlot.isBuffering,
                        errorMessage = state.primarySlot.errorMessage,
                        player = controller.primaryPlayer,
                        onTap = { controller.setAudioSlot(MultiViewSlot.PRIMARY) },
                        onDoubleTap = { state.primarySlot.channel?.let { onOpenFullscreen(it) } },
                        onLongPress = { activeActionSlot = MultiViewSlot.PRIMARY }
                    )
                }

                // Secondary Slot
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(4.dp)
                ) {
                    MultiViewTile(
                        slot = MultiViewSlot.SECONDARY,
                        channel = state.secondarySlot.channel,
                        isAudioActive = state.activeAudioSlot == MultiViewSlot.SECONDARY,
                        isBuffering = state.secondarySlot.isBuffering,
                        errorMessage = state.secondarySlot.errorMessage,
                        player = controller.secondaryPlayer,
                        onTap = { controller.setAudioSlot(MultiViewSlot.SECONDARY) },
                        onDoubleTap = { state.secondarySlot.channel?.let { onOpenFullscreen(it) } },
                        onLongPress = { activeActionSlot = MultiViewSlot.SECONDARY }
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Primary Slot
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    MultiViewTile(
                        slot = MultiViewSlot.PRIMARY,
                        channel = state.primarySlot.channel,
                        isAudioActive = state.activeAudioSlot == MultiViewSlot.PRIMARY,
                        isBuffering = state.primarySlot.isBuffering,
                        errorMessage = state.primarySlot.errorMessage,
                        player = controller.primaryPlayer,
                        onTap = { controller.setAudioSlot(MultiViewSlot.PRIMARY) },
                        onDoubleTap = { state.primarySlot.channel?.let { onOpenFullscreen(it) } },
                        onLongPress = { activeActionSlot = MultiViewSlot.PRIMARY }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Secondary Slot
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    MultiViewTile(
                        slot = MultiViewSlot.SECONDARY,
                        channel = state.secondarySlot.channel,
                        isAudioActive = state.activeAudioSlot == MultiViewSlot.SECONDARY,
                        isBuffering = state.secondarySlot.isBuffering,
                        errorMessage = state.secondarySlot.errorMessage,
                        player = controller.secondaryPlayer,
                        onTap = { controller.setAudioSlot(MultiViewSlot.SECONDARY) },
                        onDoubleTap = { state.secondarySlot.channel?.let { onOpenFullscreen(it) } },
                        onLongPress = { activeActionSlot = MultiViewSlot.SECONDARY }
                    )
                }
            }
        }

        // Top bar exit button
        IconButton(
            onClick = onCloseMultiView,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
        ) {
            Icon(
                imageVector = NisIcons.Close,
                contentDescription = "Close Multi-View",
                tint = NisTextPrimary
            )
        }

        // Slot action dialog on long-press
        activeActionSlot?.let { slot ->
            val channel = if (slot == MultiViewSlot.PRIMARY) state.primarySlot.channel else state.secondarySlot.channel
            androidx.compose.ui.window.Dialog(onDismissRequest = { activeActionSlot = null }) {
                Column(
                    modifier = Modifier
                        .clip(NisDialogShape)
                        .background(NisSurfaceElevated)
                        .border(1.dp, NisCardBorder, NisDialogShape)
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = channel?.cleanName?.ifEmpty { channel.name } ?: "Channel Options",
                        color = NisTextPrimary,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    androidx.compose.material3.TextButton(
                        onClick = {
                            activeActionSlot = null
                            channel?.let { onOpenFullscreen(it) }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open Fullscreen", color = NisAccentPrimary)
                    }
                    androidx.compose.material3.TextButton(
                        onClick = {
                            activeActionSlot = null
                            onReplaceChannel(slot)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Replace Channel", color = NisTextPrimary)
                    }
                    androidx.compose.material3.TextButton(
                        onClick = {
                            activeActionSlot = null
                            controller.closeSlot(slot)
                            if (!controller.uiState.value.isActive) {
                                onCloseMultiView()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Close Tile", color = NisLiveRed)
                    }
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun MultiViewTile(
    slot: MultiViewSlot,
    channel: ChannelEntity?,
    isAudioActive: Boolean,
    isBuffering: Boolean,
    errorMessage: String?,
    player: androidx.media3.exoplayer.ExoPlayer,
    onTap: () -> Unit,
    onDoubleTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isAudioActive) NisAccentPrimary else NisCardBorder

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(NisMediaCardShape)
            .background(NisSurface)
            .border(if (isAudioActive) 2.dp else 1.dp, borderColor, NisMediaCardShape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = { onDoubleTap() },
                    onLongPress = { onLongPress() }
                )
            }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    this.player = player
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Audio Active Indicator Badge
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .clip(NisBadgeShape)
                .background(if (isAudioActive) NisAccentPrimary else Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isAudioActive) NisIcons.VolumeUp else NisIcons.VolumeMute,
                    contentDescription = if (isAudioActive) "Audio On" else "Muted",
                    tint = if (isAudioActive) Color.Black else NisTextMuted,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isAudioActive) "AUDIO" else "MUTED",
                    color = if (isAudioActive) Color.Black else NisTextMuted,
                    style = NisTheme.textExtras.micro,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Channel Name Banner at bottom
        channel?.let { ch ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = ch.cleanName.ifEmpty { ch.name },
                    color = NisTextPrimary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }

        // Buffering Indicator
        if (isBuffering) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.Center),
                color = NisAccentPrimary,
                strokeWidth = 3.dp
            )
        }

        // Error Banner
        if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.8f), NisShapes.small)
                    .padding(12.dp)
            ) {
                Text(
                    text = errorMessage,
                    color = NisLiveRed,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
