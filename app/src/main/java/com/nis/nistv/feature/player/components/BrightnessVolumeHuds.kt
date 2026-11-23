package com.nis.nistv.feature.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nis.nistv.core.designsystem.icon.NisIcons
import com.nis.nistv.core.designsystem.theme.*

@Composable
fun VerticalHudSlider(
    value: Float, // 0f .. 1f
    visible: Boolean,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(NisPillShape)
                .background(NisScrimStrong)
                .border(1.dp, NisBorder, NisPillShape)
                .padding(vertical = 16.dp, horizontal = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(NisBorder),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Vertical Bar Track
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .height(120.dp)
                    .clip(NisBadgeShape)
                    .background(NisBorder),
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight(value.coerceIn(0f, 1f))
                        .width(8.dp)
                        .clip(NisBadgeShape)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(accentColor, accentColor.copy(alpha = 0.7f))
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "${(value * 100).toInt()}%",
                color = NisTextPrimary,
                style = NisTheme.textExtras.mono,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun BrightnessHud(
    brightness: Float,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    VerticalHudSlider(
        value = brightness,
        visible = visible,
        icon = NisIcons.Brightness,
        accentColor = NisAccentCyan,
        modifier = modifier
    )
}

@Composable
fun VolumeHud(
    volume: Float,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    VerticalHudSlider(
        value = volume,
        visible = visible,
        icon = if (volume <= 0.01f) NisIcons.VolumeMute else NisIcons.VolumeUp,
        accentColor = NisAccentPrimary,
        modifier = modifier
    )
}
