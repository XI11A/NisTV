package com.nis.nistv.feature.player.multiview

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.nis.nistv.core.database.entity.ChannelEntity
import com.nis.nistv.core.designsystem.theme.*

@Composable
fun MultiViewChannelPicker(
    availableChannels: List<ChannelEntity>,
    onChannelSelected: (ChannelEntity) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    val filtered = remember(searchQuery, availableChannels) {
        if (searchQuery.isBlank()) {
            availableChannels.take(30)
        } else {
            availableChannels.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.cleanName.contains(searchQuery, ignoreCase = true) ||
                        it.category.contains(searchQuery, ignoreCase = true)
            }.take(30)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = modifier
                .clip(NisDialogShape)
                .background(NisSurfaceElevated)
                .border(1.dp, NisCardBorder, NisDialogShape)
                .padding(20.dp)
        ) {
            Text(
                text = "Select Second Channel",
                color = NisTextPrimary,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Stream two live channels simultaneously in Multi-View",
                color = NisTextMuted,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                singleLine = true,
                placeholder = { Text("Search channel...", color = NisTextMuted) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NisAccentPrimary,
                    unfocusedBorderColor = NisCardBorder,
                    focusedTextColor = NisTextPrimary,
                    unfocusedTextColor = NisTextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.id }) { channel ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(NisThumbnailShape)
                            .background(Color.Black.copy(alpha = 0.3f))
                            .clickable { onChannelSelected(channel) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Logo
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!channel.logoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(channel.logoUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = channel.name,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .padding(2.dp)
                                )
                            } else {
                                Text(
                                    text = channel.name.take(2).uppercase(),
                                    color = NisTextPrimary,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = channel.cleanName.ifEmpty { channel.name },
                                color = NisTextPrimary,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${channel.category} • ${channel.language}",
                                color = NisTextMuted,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = NisTextMuted)
                }
            }
        }
    }
}
