package com.nis.nistv.feature.player.controls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import com.nis.nistv.core.designsystem.component.NisLinearProgressBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nis.nistv.core.database.entity.DownloadEntity
import com.nis.nistv.core.designsystem.theme.*
import kotlinx.coroutines.delay

@Composable
fun AutoplayNextOverlay(
    nextEpisode: DownloadEntity,
    countdownSeconds: Int = 10,
    onPlayNow: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var remainingSeconds by remember(nextEpisode.id) { mutableIntStateOf(countdownSeconds) }

    LaunchedEffect(nextEpisode.id) {
        while (remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds -= 1
        }
        onPlayNow()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onCancel),
        contentAlignment = Alignment.BottomEnd
    ) {
        Card(
            modifier = Modifier
                .padding(24.dp)
                .widthIn(max = 380.dp)
                .clip(NisFeaturedCardShape)
                .clickable(enabled = false) {},
            colors = CardDefaults.cardColors(containerColor = NisSurfaceElevated),
            border = androidx.compose.foundation.BorderStroke(1.dp, NisBorder)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Next episode in ${remainingSeconds}s",
                        color = NisAccentCyan,
                        style = MaterialTheme.typography.labelMedium
                    )

                    Text(
                        text = "Cancel",
                        color = NisTextMuted,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .clickable(onClick = onCancel)
                            .padding(4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                val epTag = if (nextEpisode.seasonNumber != null && nextEpisode.episodeNumber != null) {
                    "S${nextEpisode.seasonNumber} E${nextEpisode.episodeNumber} • "
                } else ""

                Text(
                    text = "$epTag${nextEpisode.title}",
                    color = NisTextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!nextEpisode.episodeName.isNullOrBlank()) {
                    Text(
                        text = nextEpisode.episodeName,
                        color = NisTextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Progress Bar
                NisLinearProgressBar(
                    progress = 1f - (remainingSeconds.toFloat() / countdownSeconds.toFloat()),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(NisShapes.extraSmall),
                    color = NisAccentCyan,
                    trackColor = NisBorder,
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onPlayNow,
                        colors = ButtonDefaults.buttonColors(containerColor = NisAccentPrimary),
                        shape = NisButtonShape,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Play Now",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}
