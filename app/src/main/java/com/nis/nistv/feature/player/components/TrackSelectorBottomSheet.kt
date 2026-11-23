package com.nis.nistv.feature.player.components

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nis.nistv.core.designsystem.icon.NisIcons
import com.nis.nistv.core.designsystem.theme.*
import com.nis.nistv.playback.AvailableTrack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackSelectorBottomSheet(
    audioTracks: List<AvailableTrack>,
    subtitleTracks: List<AvailableTrack>,
    onSelectAudio: (groupIndex: Int, trackIndex: Int) -> Unit,
    onSelectSubtitle: (groupIndex: Int, trackIndex: Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Audio, 1 = Subtitles

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NisTheme.colors.surfaceElevated,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Audio & Subtitle Tracks",
                color = NisTheme.colors.textPrimary,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Tab Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(NisButtonShape)
                    .background(NisTheme.colors.surface)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(NisBadgeShape)
                        .background(if (selectedTab == 0) NisAccentPrimary else Color.Transparent)
                        .clickable { selectedTab = 0 }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Audio (${audioTracks.size})",
                        color = if (selectedTab == 0) Color.White else NisTheme.colors.textSecondary,
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(NisBadgeShape)
                        .background(if (selectedTab == 1) NisAccentPrimary else Color.Transparent)
                        .clickable { selectedTab = 1 }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Subtitles (${subtitleTracks.size})",
                        color = if (selectedTab == 1) Color.White else NisTheme.colors.textSecondary,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == 0) {
                // Audio Tracks List
                if (audioTracks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No additional audio tracks found for this stream.",
                            color = NisTheme.colors.textMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(audioTracks) { track ->
                            TrackOptionItem(
                                title = track.label,
                                subtitle = track.language,
                                isSelected = track.isSelected,
                                onClick = {
                                    onSelectAudio(track.groupIndex, track.trackIndex)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
            } else {
                // Subtitles List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Option to turn off subtitles
                    val isAnySelected = subtitleTracks.any { it.isSelected }
                    item {
                        TrackOptionItem(
                            title = "Off",
                            subtitle = "Disable subtitles",
                            isSelected = !isAnySelected,
                            onClick = {
                                onSelectSubtitle(0, -1)
                                onDismiss()
                            }
                        )
                    }

                    if (subtitleTracks.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No subtitle tracks available for this channel.",
                                    color = NisTheme.colors.textMuted,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    } else {
                        items(subtitleTracks) { track ->
                            TrackOptionItem(
                                title = track.label,
                                subtitle = track.language,
                                isSelected = track.isSelected,
                                onClick = {
                                    onSelectSubtitle(track.groupIndex, track.trackIndex)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackOptionItem(
    title: String,
    subtitle: String?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(NisShapes.small)
            .background(if (isSelected) NisTheme.colors.surface else Color.Transparent)
            .border(
                1.dp,
                if (isSelected) NisAccentPrimary else NisTheme.colors.borderSubtle,
                NisShapes.small
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = title,
                color = if (isSelected) NisAccentPrimary else NisTheme.colors.textPrimary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    color = NisTheme.colors.textMuted,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        if (isSelected) {
            Icon(
                imageVector = NisIcons.Check,
                contentDescription = "Selected",
                tint = NisAccentPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
