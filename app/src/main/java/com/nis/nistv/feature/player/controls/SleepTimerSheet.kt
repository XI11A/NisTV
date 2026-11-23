package com.nis.nistv.feature.player.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nis.nistv.core.designsystem.theme.*
import com.nis.nistv.core.playback.SleepTimerState

data class SleepTimerOption(
    val label: String,
    val minutes: Int?, // null = end of episode, 0 = off
    val isEndOfEpisode: Boolean = false
)

val SLEEP_TIMER_OPTIONS = listOf(
    SleepTimerOption("Off (Cancel timer)", minutes = 0),
    SleepTimerOption("15 minutes", minutes = 15),
    SleepTimerOption("30 minutes", minutes = 30),
    SleepTimerOption("45 minutes", minutes = 45),
    SleepTimerOption("60 minutes", minutes = 60),
    SleepTimerOption("End of episode", minutes = null, isEndOfEpisode = true)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerSheet(
    state: SleepTimerState,
    isEndOfEpisodeSupported: Boolean = true,
    onSelectMinutes: (Int) -> Unit,
    onSelectEndOfEpisode: () -> Unit,
    onCancelTimer: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = NisTheme.colors.surfaceElevated,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = NisTheme.colors.borderSubtle)
        },
        shape = NisBottomSheetShape
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = NisDimens.MaxContentWidth)
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Sleep Timer",
                color = NisTheme.colors.textPrimary,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Fades volume gently over 5 seconds and pauses playback when timer completes.",
                color = NisTheme.colors.textSecondary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            val options = if (isEndOfEpisodeSupported) SLEEP_TIMER_OPTIONS else SLEEP_TIMER_OPTIONS.filter { !it.isEndOfEpisode }

            options.forEach { option ->
                val isSelected = when (state) {
                    is SleepTimerState.Inactive -> option.minutes == 0
                    is SleepTimerState.Active -> {
                        if (option.isEndOfEpisode) {
                            state.isEndOfEpisode
                        } else {
                            !state.isEndOfEpisode && option.minutes != null && state.totalSeconds == (option.minutes * 60L)
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(NisMediaCardShape)
                        .background(if (isSelected) NisAccentPrimary.copy(alpha = 0.15f) else Color.Transparent)
                        .clickable {
                            if (option.minutes == 0) {
                                onCancelTimer()
                            } else if (option.isEndOfEpisode) {
                                onSelectEndOfEpisode()
                            } else if (option.minutes != null) {
                                onSelectMinutes(option.minutes)
                            }
                            onDismiss()
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = option.label,
                        color = if (isSelected) NisAccentPrimary else NisTheme.colors.textPrimary,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )

                    if (isSelected) {
                        Text(
                            text = "✓",
                            color = NisAccentPrimary,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SleepTimerChip(
    state: SleepTimerState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (state is SleepTimerState.Active) {
        val label = if (state.isEndOfEpisode) {
            "⏱ End of Ep"
        } else {
            val m = state.remainingSeconds / 60L
            val s = state.remainingSeconds % 60L
            String.format(java.util.Locale.ROOT, "⏱ %02d:%02d", m, s)
        }

        Box(
            modifier = modifier
                .clip(NisPillShape)
                .background(NisAccentAmber.copy(alpha = 0.2f))
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(
                text = label,
                color = NisAccentAmber,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
