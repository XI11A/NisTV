package com.nis.nistv.feature.player.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

val PLAYBACK_SPEED_OPTIONS = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackSpeedSheet(
    currentSpeed: Float,
    onSelectSpeed: (Float) -> Unit,
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
                text = "Playback Speed",
                color = NisTheme.colors.textPrimary,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            PLAYBACK_SPEED_OPTIONS.forEach { speed ->
                val isSelected = Math.abs(currentSpeed - speed) < 0.05f
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(NisMediaCardShape)
                        .background(if (isSelected) NisAccentPrimary.copy(alpha = 0.15f) else Color.Transparent)
                        .clickable {
                            onSelectSpeed(speed)
                            onDismiss()
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (speed == 1.0f) "Normal (1.0x)" else "${speed}x",
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
