package com.nis.nistv.feature.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nis.nistv.core.cast.CastManager
import com.nis.nistv.core.designsystem.icon.NisIcons
import com.nis.nistv.core.designsystem.theme.*

@Composable
fun ScreencastDialog(
    streamTitle: String,
    streamUrl: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val castManager = CastManager.getInstance(context)
    val isConnected by castManager.isExternalDisplayConnected.collectAsState()
    val connectedName by castManager.connectedDisplayName.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NisScrimStrong)
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(320.dp)
                .clip(NisDialogShape)
                .background(NisTheme.colors.surfaceElevated)
                .border(1.dp, NisTheme.colors.borderSubtle, NisDialogShape)
                .padding(20.dp)
                .clickable(enabled = false) {}
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(NisThumbnailShape)
                            .background(if (isConnected) NisSuccess.copy(alpha = 0.2f) else NisAccentCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isConnected) NisIcons.CastConnected else NisIcons.Cast,
                            contentDescription = "Cast",
                            tint = if (isConnected) NisSuccess else NisAccentCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Screencast & TV",
                            color = NisTheme.colors.textPrimary,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = streamTitle.ifBlank { "Live Stream" },
                            color = NisTheme.colors.textMuted,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(NisTheme.colors.borderSubtle)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = NisIcons.Close,
                        contentDescription = "Close",
                        tint = NisTheme.colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Display Status Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(NisMediaCardShape)
                    .background(if (isConnected) NisSuccess.copy(alpha = 0.15f) else NisTheme.colors.borderSubtle)
                    .border(
                        1.dp,
                        if (isConnected) NisSuccess.copy(alpha = 0.35f) else NisTheme.colors.borderSubtle,
                        NisMediaCardShape
                    )
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isConnected) NisSuccess else NisTheme.colors.textMuted)
                    )
                    Column {
                        Text(
                            text = if (isConnected) "Connected: ${connectedName ?: "External Screen"}" else "No Display Connected",
                            color = if (isConnected) NisSuccess else NisTheme.colors.textPrimary,
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = if (isConnected) "Screen mirroring active on external display" else "Ready to cast to TV or smart screen",
                            color = NisTheme.colors.textMuted,
                            style = NisTheme.textExtras.micro
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action 1: System Cast / Screen Mirror
            CastActionRow(
                icon = NisIcons.Cast,
                title = "System Screen Cast",
                subtitle = "Chromecast, Miracast, Smart View, Fire TV",
                iconTint = NisAccentCyan,
                onClick = {
                    castManager.openSystemCastSettings(context)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Action 2: Send Stream to Cast/DLNA App
            CastActionRow(
                icon = NisIcons.LiveTv,
                title = "Cast Stream to TV / App",
                subtitle = "Web Video Caster, BubbleUPnP, VLC",
                iconTint = NisAccentPrimary,
                onClick = {
                    castManager.castStreamToExternalApp(context, streamUrl, streamTitle)
                    onDismiss()
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Action 3: Copy Stream Link
            CastActionRow(
                icon = NisIcons.Info,
                title = "Copy Stream URL",
                subtitle = "For external media players or Kodi",
                iconTint = NisAccentViolet,
                onClick = {
                    castManager.copyStreamLink(context, streamUrl)
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Helpful footer tip
            Text(
                text = "Tip: Ensure your TV/receiver and phone are on the same Wi-Fi network.",
                color = NisTheme.colors.textMuted,
                style = NisTheme.textExtras.micro
            )
        }
    }
}

@Composable
private fun CastActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(NisMediaCardShape)
            .background(NisTheme.colors.borderSubtle)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(NisShapes.small)
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = NisTheme.colors.textPrimary,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = NisTheme.colors.textMuted,
                style = NisTheme.textExtras.micro,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
