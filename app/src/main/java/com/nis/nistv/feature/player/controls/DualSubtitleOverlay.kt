package com.nis.nistv.feature.player.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nis.nistv.core.designsystem.theme.NisBadgeShape
import com.nis.nistv.data.subtitles.SubtitleCue
import com.nis.nistv.core.designsystem.theme.NisBadgeShape
import com.nis.nistv.data.subtitles.SubtitleCueParser

@Composable
fun DualSubtitleOverlay(
    currentPositionMs: Long,
    primaryCues: List<SubtitleCue>?,
    primaryDelayMs: Long = 0L,
    secondaryCues: List<SubtitleCue>?,
    secondaryDelayMs: Long = 0L,
    modifier: Modifier = Modifier
) {
    // Only recompute when currentPosition passes a cue boundary
    val activePrimaryText by remember(primaryCues, currentPositionMs, primaryDelayMs) {
        derivedStateOf {
            if (primaryCues.isNullOrEmpty()) null
            else SubtitleCueParser.findActiveCue(primaryCues, currentPositionMs, primaryDelayMs)?.text
        }
    }

    val activeSecondaryText by remember(secondaryCues, currentPositionMs, secondaryDelayMs) {
        derivedStateOf {
            if (secondaryCues.isNullOrEmpty()) null
            else SubtitleCueParser.findActiveCue(secondaryCues, currentPositionMs, secondaryDelayMs)?.text
        }
    }

    if (activePrimaryText == null && activeSecondaryText == null) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 28.dp)
    ) {
        // Secondary Subtitle (Top - Lighter Yellow)
        activeSecondaryText?.let { secondaryText ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .background(Color.Black.copy(alpha = 0.65f), shape = NisBadgeShape)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = secondaryText,
                    color = Color(0xFFFFF176), // Soft light yellow
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color.Black,
                            blurRadius = 4f
                        )
                    )
                )
            }
        }

        // Primary Subtitle (Bottom - High-contrast White)
        activePrimaryText?.let { primaryText ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.65f), shape = NisBadgeShape)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = primaryText,
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color.Black,
                            blurRadius = 4f
                        )
                    )
                )
            }
        }
    }
}
