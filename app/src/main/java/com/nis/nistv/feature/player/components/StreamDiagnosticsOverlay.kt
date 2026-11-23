package com.nis.nistv.feature.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nis.nistv.core.designsystem.icon.NisIcons
import com.nis.nistv.core.designsystem.theme.*
import com.nis.nistv.playback.StreamDiagnostics

@Composable
fun StreamDiagnosticsOverlay(
    diagnostics: StreamDiagnostics,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(NisThumbnailShape)
            .background(NisPlayerScrim)
            .border(1.dp, NisBorder.copy(alpha = 0.8f), NisThumbnailShape)
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.width(260.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(NisBadgeShape)
                            .background(NisLiveEmerald)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "STREAM DIAGNOSTICS",
                        color = NisAccentCyan,
                        style = NisTheme.textExtras.mono,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = NisIcons.Close,
                        contentDescription = "Close Diagnostics",
                        tint = NisTextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Diagnostic Rows
            DiagnosticRow(label = "Resolution", value = diagnostics.resolution ?: "Auto / Scaling")
            DiagnosticRow(label = "Codec", value = diagnostics.videoCodec?.substringAfterLast('/') ?: "Auto")
            DiagnosticRow(label = "Bitrate", value = if (diagnostics.bitrateKbps > 0) "${diagnostics.bitrateKbps} kbps" else "Variable / Live")
            DiagnosticRow(
                label = "Audio",
                value = if (diagnostics.audioSampleRate > 0) "${diagnostics.audioSampleRate}Hz • ${diagnostics.audioChannels}ch" else "Default"
            )
            DiagnosticRow(label = "Dropped Frames", value = "${diagnostics.droppedFrames}")
            DiagnosticRow(label = "Buffer Health", value = "${"%.1f".format(diagnostics.bufferHealthSeconds)} s")
            DiagnosticRow(label = "Stream Source", value = "${diagnostics.sourceIndex} of ${diagnostics.totalSources}")
            DiagnosticRow(label = "Host Latency", value = if (diagnostics.latencyMs > 0) "${diagnostics.latencyMs} ms" else "< 50 ms")
        }
    }
}

@Composable
private fun DiagnosticRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = NisTextMuted,
            style = NisTheme.textExtras.mono
        )
        Text(
            text = value,
            color = NisTextPrimary,
            style = NisTheme.textExtras.mono,
            fontWeight = FontWeight.Medium
        )
    }
}
