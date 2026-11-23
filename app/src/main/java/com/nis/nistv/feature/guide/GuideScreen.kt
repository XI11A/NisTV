package com.nis.nistv.feature.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.nis.nistv.NisTvApplication
import com.nis.nistv.core.database.entity.ChannelEntity
import com.nis.nistv.core.designsystem.component.ChannelGridSkeleton
import com.nis.nistv.core.designsystem.component.NisBadge
import com.nis.nistv.core.designsystem.component.NisCategoryChip
import com.nis.nistv.core.designsystem.component.NisPrimaryButton
import com.nis.nistv.core.designsystem.component.NisSecondaryButton
import com.nis.nistv.core.designsystem.icon.NisIcons
import com.nis.nistv.core.designsystem.theme.*
import com.nis.nistv.data.epg.TimelineProgram
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val CHANNEL_COLUMN_WIDTH = 130.dp
private val SLOT_WIDTH = 160.dp
private val TOTAL_TIMELINE_WIDTH = SLOT_WIDTH * 6 // 6 x 30min slots = 3h

data class SelectedProgramDetails(
    val channel: ChannelEntity,
    val program: TimelineProgram
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideScreen(
    onBackClick: () -> Unit,
    onChannelClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as NisTvApplication
    val viewModel: GuideViewModel = viewModel(
        factory = GuideViewModel.provideFactory(
            repository = app.container.channelRepository,
            epgManager = app.container.epgManager,
            reminderManager = app.container.programReminderManager
        )
    )

    val uiState by viewModel.uiState.collectAsState()
    val horizontalScrollState = rememberScrollState()

    var selectedProgram by remember { mutableStateOf<SelectedProgramDetails?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val now = System.currentTimeMillis()

    // Scroll initial view so "now" is centered or visible
    LaunchedEffect(uiState.windowStartMs) {
        if (uiState.windowEndMs > uiState.windowStartMs) {
            val totalMs = (uiState.windowEndMs - uiState.windowStartMs).toFloat()
            val nowOffsetMs = (now - uiState.windowStartMs).toFloat().coerceIn(0f, totalMs)
            val fraction = nowOffsetMs / totalMs
            val density = context.resources.displayMetrics.density
            val totalPx = (TOTAL_TIMELINE_WIDTH.value * density)
            val targetPx = (totalPx * fraction) - (300 * density)
            horizontalScrollState.scrollTo(targetPx.toInt().coerceAtLeast(0))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NisTheme.colors.background)
            .statusBarsPadding()
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = NisIcons.Back,
                        contentDescription = "Back",
                        tint = NisTheme.colors.textPrimary
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "TV Guide",
                    color = NisTheme.colors.textPrimary,
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Text(
                text = timeFormat.format(Date(now)),
                color = NisTheme.colors.accentCyan,
                style = MaterialTheme.typography.labelLarge
            )
        }

        // Category Filter Chips
        if (uiState.categories.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                item {
                    NisCategoryChip(
                        text = "All",
                        isSelected = uiState.selectedCategory == null,
                        onClick = { viewModel.selectCategory(null) }
                    )
                }
                items(uiState.categories, key = { it }) { cat ->
                    NisCategoryChip(
                        text = cat,
                        isSelected = uiState.selectedCategory == cat,
                        onClick = { viewModel.selectCategory(cat) }
                    )
                }
            }
        }

        if (uiState.isLoading) {
            ChannelGridSkeleton(modifier = Modifier.fillMaxSize())
        } else {
            // Main Virtualized EPG Grid
            Column(modifier = Modifier.fillMaxSize()) {
                // Sticky Time Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NisTheme.colors.surfaceElevated)
                        .border(1.dp, NisTheme.colors.border)
                        .padding(vertical = 8.dp)
                ) {
                    // Left column header placeholder
                    Box(
                        modifier = Modifier
                            .width(CHANNEL_COLUMN_WIDTH)
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "Channels",
                            color = NisTheme.colors.textMuted,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Synchronized Time Slots Header
                    Row(
                        modifier = Modifier
                            .width(TOTAL_TIMELINE_WIDTH)
                            .horizontalScroll(horizontalScrollState)
                    ) {
                        val slotDurationMs = 30 * 60 * 1000L
                        for (i in 0 until 6) {
                            val slotTime = uiState.windowStartMs + (i * slotDurationMs)
                            Box(
                                modifier = Modifier
                                    .width(SLOT_WIDTH)
                                    .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = timeFormat.format(Date(slotTime)),
                                    color = NisTheme.colors.textSecondary,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }

                // Virtualized Channel Rows
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(uiState.channels, key = { it.id }) { channel ->
                            // Trigger loading schedule for channel
                            LaunchedEffect(channel.id) {
                                viewModel.loadScheduleForChannel(channel)
                            }

                            val schedule = uiState.schedules[channel.id] ?: emptyList()

                            GuideChannelRow(
                                channel = channel,
                                schedule = schedule,
                                windowStartMs = uiState.windowStartMs,
                                windowEndMs = uiState.windowEndMs,
                                horizontalScrollState = horizontalScrollState,
                                activeReminders = uiState.activeReminders,
                                onChannelClick = { onChannelClick(channel.id) },
                                onProgramClick = { prog ->
                                    selectedProgram = SelectedProgramDetails(channel, prog)
                                }
                            )
                        }
                    }

                    // Synchronized "NOW" line
                    if (now in uiState.windowStartMs until uiState.windowEndMs) {
                        val totalWindowMs = (uiState.windowEndMs - uiState.windowStartMs).toFloat()
                        val fraction = (now - uiState.windowStartMs).toFloat() / totalWindowMs
                        val nowOffsetDp = CHANNEL_COLUMN_WIDTH + (TOTAL_TIMELINE_WIDTH * fraction)

                        // Offset by the horizontal scroll position
                        val scrollOffsetDp = with(LocalDensity.current) {
                            horizontalScrollState.value.toDp()
                        }
                        val finalOffset = nowOffsetDp - scrollOffsetDp

                        if (finalOffset >= CHANNEL_COLUMN_WIDTH) {
                            Box(
                                modifier = Modifier
                                    .offset(x = finalOffset)
                                    .width(2.dp)
                                    .fillMaxHeight()
                                    .background(NisTheme.colors.error.copy(alpha = 0.85f))
                            )
                        }
                    }
                }
            }
        }
    }

    // Program Details Bottom Sheet
    selectedProgram?.let { details ->
        ModalBottomSheet(
            onDismissRequest = { selectedProgram = null },
            sheetState = sheetState,
            containerColor = NisTheme.colors.surfaceElevated
        ) {
            val prog = details.program
            val ch = details.channel
            val isAiringNow = now in prog.startEpochMs until prog.endEpochMs
            val reminderKey = "${ch.id}_${prog.startEpochMs}"
            val hasReminder = uiState.activeReminders.contains(reminderKey)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header: Channel name & badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = ch.cleanName.ifEmpty { ch.name },
                        color = NisTheme.colors.accentCyan,
                        style = MaterialTheme.typography.labelLarge
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (isAiringNow) {
                            NisBadge(
                                text = "AIRING NOW",
                                backgroundColor = NisTheme.colors.success.copy(alpha = 0.2f),
                                textColor = NisTheme.colors.success
                            )
                        }
                        if (prog.isEstimated) {
                            NisBadge(
                                text = "Estimated",
                                backgroundColor = NisTheme.colors.surface,
                                textColor = NisTheme.colors.textMuted
                            )
                        }
                    }
                }

                // Program Title
                Text(
                    text = prog.title,
                    color = NisTheme.colors.textPrimary,
                    style = MaterialTheme.typography.titleLarge
                )

                // Time info
                Text(
                    text = "${timeFormat.format(Date(prog.startEpochMs))} - ${timeFormat.format(Date(prog.endEpochMs))} • ${(prog.durationMs / 60000)} min",
                    color = NisTheme.colors.textSecondary,
                    style = MaterialTheme.typography.bodySmall
                )

                // Description
                Text(
                    text = prog.description.ifEmpty { "No detailed program summary available." },
                    color = NisTheme.colors.textMuted,
                    style = MaterialTheme.typography.bodySmall
                )

                if (prog.isEstimated) {
                    Text(
                        text = "Notice: Estimated guide entries are generated based on category patterns. Reminders can only be scheduled for verified broadcast entries.",
                        color = NisTheme.colors.textMuted.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Actions
                if (isAiringNow) {
                    NisPrimaryButton(
                        text = "Watch Channel Live",
                        onClick = {
                            selectedProgram = null
                            onChannelClick(ch.id)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if (!prog.isEstimated && prog.startEpochMs > now) {
                    if (hasReminder) {
                        NisSecondaryButton(
                            text = "Cancel Reminder",
                            icon = NisIcons.Delete,
                            onClick = {
                                viewModel.cancelReminder(ch.id, prog.startEpochMs)
                                selectedProgram = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        NisPrimaryButton(
                            text = "Remind Me (5 min before)",
                            icon = NisIcons.Alarm,
                            onClick = {
                                viewModel.setReminder(ch.id, prog.title, prog.startEpochMs)
                                selectedProgram = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun GuideChannelRow(
    channel: ChannelEntity,
    schedule: List<TimelineProgram>,
    windowStartMs: Long,
    windowEndMs: Long,
    horizontalScrollState: androidx.compose.foundation.ScrollState,
    activeReminders: Set<String>,
    onChannelClick: () -> Unit,
    onProgramClick: (TimelineProgram) -> Unit
) {
    val context = LocalContext.current
    val totalWindowDurationMs = (windowEndMs - windowStartMs).coerceAtLeast(1L)
    val now = System.currentTimeMillis()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .border(0.5.dp, NisTheme.colors.border.copy(alpha = 0.5f)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Channel Header Column (Fixed)
        Row(
            modifier = Modifier
                .width(CHANNEL_COLUMN_WIDTH)
                .fillMaxHeight()
                .background(NisTheme.colors.surface)
                .clickable(onClick = onChannelClick)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(channel.logoUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(32.dp)
                    .clip(NisBadgeShape)
                    .background(NisTheme.colors.surfaceElevated)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = channel.cleanName.ifEmpty { channel.name },
                color = NisTheme.colors.textPrimary,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Right: Synchronized Timeline Programs Row
        Row(
            modifier = Modifier
                .width(TOTAL_TIMELINE_WIDTH)
                .fillMaxHeight()
                .horizontalScroll(horizontalScrollState)
        ) {
            if (schedule.isEmpty()) {
                Box(
                    modifier = Modifier
                        .width(TOTAL_TIMELINE_WIDTH)
                        .fillMaxHeight()
                        .padding(4.dp)
                        .clip(NisBadgeShape)
                        .background(NisTheme.colors.surfaceElevated.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Loading schedule...",
                        color = NisTheme.colors.textMuted,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            } else {
                for (prog in schedule) {
                    val durationFraction = (prog.durationMs.toFloat() / totalWindowDurationMs).coerceIn(0f, 1f)
                    val blockWidth = TOTAL_TIMELINE_WIDTH * durationFraction
                    val isCurrent = now in prog.startEpochMs until prog.endEpochMs
                    val reminderKey = "${channel.id}_${prog.startEpochMs}"
                    val hasReminder = activeReminders.contains(reminderKey)

                    Box(
                        modifier = Modifier
                            .width(blockWidth)
                            .fillMaxHeight()
                            .padding(2.dp)
                            .clip(NisBadgeShape)
                            .background(
                                if (isCurrent) NisTheme.colors.surfaceElevated else NisTheme.colors.surface.copy(alpha = 0.6f)
                            )
                            .border(
                                1.dp,
                                if (isCurrent) NisTheme.colors.success.copy(alpha = 0.6f) else NisTheme.colors.border,
                                NisBadgeShape
                            )
                            .clickable { onProgramClick(prog) }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = prog.title,
                                    color = if (isCurrent) NisTheme.colors.textPrimary else NisTheme.colors.textSecondary,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )

                                if (hasReminder) {
                                    Icon(
                                        imageVector = NisIcons.AlarmOn,
                                        contentDescription = "Reminder Set",
                                        tint = NisTheme.colors.accentCyan,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (prog.isEstimated) {
                                    Text(
                                        text = "Est • ",
                                        color = NisTheme.colors.textMuted,
                                        style = NisTheme.textExtras.micro
                                    )
                                }
                                Text(
                                    text = "${(prog.durationMs / 60000)}m",
                                    color = NisTheme.colors.textMuted,
                                    style = NisTheme.textExtras.micro
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
