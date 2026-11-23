package com.nis.nistv.core.designsystem.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nis.nistv.core.designsystem.theme.NisAccentCyan
import com.nis.nistv.core.designsystem.theme.NisAccentPrimary
import com.nis.nistv.core.designsystem.theme.NisAccentViolet
import com.nis.nistv.core.designsystem.theme.NisBackground
import com.nis.nistv.core.designsystem.theme.NisPillShape
import com.nis.nistv.core.designsystem.theme.NisSurfaceElevated
import com.nis.nistv.core.designsystem.theme.NisTextMuted

/**
 * Netflix-style cinematic loading screen:
 * - Animated NtvLogo with radial breathing glow
 * - Track in NisSurfaceElevated
 * - Horizontal sweep loading bar using NisBrandGradient
 */
@Composable
fun NetflixLoadingScreen(
    message: String = "Loading Channels...",
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "NetflixLoading")

    // Breathing logo scale & alpha
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LogoPulseScale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowAlpha"
    )

    // Sweep position for Netflix-style horizon loading bar
    val sweepProgress by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SweepBar"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NisBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // NTV Emblem with animated glow and scale
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.scale(scale)
            ) {
                Canvas(modifier = Modifier.size(160.dp)) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                NisAccentCyan.copy(alpha = glowAlpha * 0.35f),
                                NisAccentPrimary.copy(alpha = glowAlpha * 0.15f),
                                Color.Transparent
                            ),
                            radius = size.width / 2
                        )
                    )
                }

                // Main Logo
                NtvLogo(
                    size = 72.dp,
                    showWordmark = true,
                    fontSize = 32
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Horizon loading bar with NisBrandGradient sweep
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(3.dp)
                    .clip(NisPillShape)
                    .background(NisSurfaceElevated)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val barWidth = size.width
                    val segmentWidth = barWidth * 0.45f
                    val startX = sweepProgress * barWidth

                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                NisAccentCyan,
                                NisAccentPrimary,
                                NisAccentViolet,
                                Color.Transparent
                            ),
                            startX = startX - segmentWidth,
                            endX = startX + segmentWidth
                        ),
                        topLeft = Offset(0f, 0f),
                        size = size
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Status message
            Text(
                text = message,
                color = NisTextMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
