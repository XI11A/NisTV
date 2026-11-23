package com.nis.nistv.feature.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.nis.nistv.core.designsystem.component.NisLinearProgressBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nis.nistv.core.designsystem.theme.*
import com.nis.nistv.data.epg.ProgrammeInfo

@Composable
fun LiveEpgCard(
    programmeInfo: ProgrammeInfo?,
    modifier: Modifier = Modifier
) {
    var expandedTimeline by remember { mutableStateOf(false) }

    val info = programmeInfo ?: ProgrammeInfo()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(NisFeaturedCardShape)
            .background(NisTheme.colors.surface)
            .border(1.dp, NisTheme.colors.borderSubtle, NisFeaturedCardShape)
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Live Dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(NisLiveRed)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "ON AIR NOW",
                    color = NisLiveRed,
                    style = NisTheme.textExtras.micro,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Text(
                text = info.timeString,
                color = NisAccentPrimary,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Programme Title
        Text(
            text = info.title,
            color = NisTheme.colors.textPrimary,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Programme Description
        Text(
            text = info.description,
            color = NisTheme.colors.textSecondary,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Progress Bar
        NisLinearProgressBar(
            progress = (info.progressPercent / 100f).coerceIn(0.05f, 1f),
            color = NisAccentPrimary,
            trackColor = NisTheme.colors.surfaceElevated,
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(NisShapes.extraSmall)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Up Next snippet
        if (info.upNextTitle.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(NisShapes.small)
                    .background(NisTheme.colors.surfaceElevated)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "UP NEXT:",
                    color = NisAccentPrimary,
                    style = NisTheme.textExtras.micro,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = info.upNextTitle,
                    color = NisTheme.colors.textPrimary,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (info.upNextTime.isNotBlank()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = info.upNextTime,
                        color = NisTheme.colors.textMuted,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        // Toggle Full Timeline Button
        if (info.upcomingSchedule.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandedTimeline = !expandedTimeline }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (expandedTimeline) "Hide Schedule ▲" else "View Today's Schedule (${info.upcomingSchedule.size}) ▼",
                    color = NisAccentPrimary,
                    style = MaterialTheme.typography.labelMedium
                )
            }

            AnimatedVisibility(visible = expandedTimeline) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    info.upcomingSchedule.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(NisShapes.small)
                                .background(if (item.isCurrent) NisAccentCyan.copy(alpha = 0.15f) else NisTheme.colors.borderSubtle)
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.timeString,
                                color = if (item.isCurrent) NisAccentCyan else NisTheme.colors.textMuted,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (item.isCurrent) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.width(115.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    color = NisTheme.colors.textPrimary,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (item.isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (item.desc.isNotBlank()) {
                                    Text(
                                        text = item.desc,
                                        color = NisTheme.colors.textMuted,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
