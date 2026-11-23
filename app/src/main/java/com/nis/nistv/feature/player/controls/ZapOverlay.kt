package com.nis.nistv.feature.player.controls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.nis.nistv.core.database.entity.ChannelEntity
import com.nis.nistv.core.designsystem.theme.*

@Composable
fun ZapOverlay(
    visible: Boolean,
    channel: ChannelEntity?,
    channelNumber: Int,
    totalChannels: Int,
    contextTitle: String,
    onDoubleTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible && channel != null,
        enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { -it / 2 }),
        modifier = modifier
    ) {
        if (channel == null) return@AnimatedVisibility
        val context = LocalContext.current

        Box(
            modifier = Modifier
                .clip(NisFeaturedCardShape)
                .background(NisSurfaceElevated.copy(alpha = 0.92f))
                .border(1.dp, NisCardBorder, NisFeaturedCardShape)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { onDoubleTap() }
                    )
                }
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Channel number pill
                Box(
                    modifier = Modifier
                        .clip(NisShapes.small)
                        .background(NisAccentPrimary.copy(alpha = 0.18f))
                        .border(1.dp, NisAccentPrimary.copy(alpha = 0.45f), NisShapes.small)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "CH $channelNumber",
                        color = NisAccentPrimary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                // Channel Logo or Monogram
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!channel.logoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(channel.logoUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = channel.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(28.dp)
                                .padding(2.dp)
                        )
                    } else {
                        Text(
                            text = channel.name.take(2).uppercase(),
                            color = NisTextPrimary,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }

                // Name and context metadata
                Column {
                    Text(
                        text = channel.cleanName.ifEmpty { channel.name },
                        color = NisTextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${channel.category} • ($channelNumber/$totalChannels in $contextTitle)",
                        color = NisTextMuted,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
