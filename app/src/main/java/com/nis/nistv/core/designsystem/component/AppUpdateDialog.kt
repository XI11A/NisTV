package com.nis.nistv.core.designsystem.component

import android.app.DownloadManager
import android.content.Intent
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nis.nistv.core.designsystem.icon.NisIcons
import com.nis.nistv.core.designsystem.theme.*
import com.nis.nistv.core.update.UpdateInfo

@Composable
fun AppUpdateDialog(
    updateInfo: UpdateInfo,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var downloadStarted by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NisScrimStrong)
            .clickable(enabled = !updateInfo.forceUpdate || downloadStarted, onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(340.dp)
                .clip(NisDialogShape)
                .background(NisTheme.colors.surfaceElevated)
                .border(1.dp, NisTheme.colors.borderSubtle, NisDialogShape)
                .padding(24.dp)
                .clickable(enabled = false) {}
        ) {
            if (!downloadStarted) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(NisThumbnailShape)
                        .background(
                            Brush.linearGradient(
                                listOf(NisAccentPrimary, NisAccentCyan)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = NisIcons.Download,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title & Version
                Text(
                    text = "Update Available",
                    color = NisTheme.colors.textPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(NisBadgeShape)
                            .background(NisAccentCyan.copy(alpha = 0.16f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "v${updateInfo.versionName}",
                            color = NisAccentCyan,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "${updateInfo.apkSize} • ${updateInfo.releaseDate}",
                        color = NisTheme.colors.textMuted,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Changelog Box
                Text(
                    text = "WHAT'S NEW",
                    color = NisTheme.colors.textMuted,
                    style = NisTheme.textExtras.micro,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(NisShapes.medium)
                        .background(NisTheme.colors.surfaceHigh)
                        .padding(12.dp)
                ) {
                    if (updateInfo.changelog.isNotEmpty()) {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(updateInfo.changelog) { note ->
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 5.dp)
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(NisAccentCyan)
                                    )
                                    Text(
                                        text = note,
                                        color = NisTheme.colors.textPrimary,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "Bug fixes, stream stability improvements, and performance enhancements.",
                            color = NisTheme.colors.textPrimary,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Button(
                    onClick = {
                        downloadStarted = true
                        onUpdate()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = NisButtonShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NisAccentPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = NisIcons.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Update Now",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (!updateInfo.forceUpdate) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = NisButtonShape,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = NisTheme.colors.textMuted
                        ),
                        border = null
                    ) {
                        Text(
                            text = "Later",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            } else {
                // Download Started View (prevents any blank screen)
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(NisDialogShape)
                        .background(
                            Brush.linearGradient(
                                listOf(NisSuccess, NisAccentCyan)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = NisIcons.DownloadDone,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Download Started!",
                    color = NisTheme.colors.textPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Downloading NisTV v${updateInfo.versionName} (${updateInfo.apkSize})",
                    color = NisAccentCyan,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Instructions Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(NisThumbnailShape)
                        .background(NisTheme.colors.surfaceHigh)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(NisAccentCyan.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "1",
                                color = NisAccentCyan,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Check the download progress in your notification bar or browser.",
                            color = NisTheme.colors.textPrimary,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(NisAccentCyan.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "2",
                                color = NisAccentCyan,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "If warned \"File might be harmful\", choose \"Download anyway\".",
                            color = NisTheme.colors.textPrimary,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(NisAccentCyan.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "3",
                                color = NisAccentCyan,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Once finished, tap the notification or file to complete installation.",
                            color = NisTheme.colors.textPrimary,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Button(
                    onClick = {
                        try {
                            val intent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = NisButtonShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NisSuccess,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = NisIcons.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Open Downloads",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = NisButtonShape,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = NisTextMuted
                    ),
                    border = null
                ) {
                    Text(
                        text = "Got It",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
