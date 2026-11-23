package com.nis.nistv.feature.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nis.nistv.core.database.entity.ChannelEntity
import com.nis.nistv.core.designsystem.component.ChannelMonogram
import com.nis.nistv.core.designsystem.component.NisCategoryChip
import com.nis.nistv.core.designsystem.icon.NisIcons
import com.nis.nistv.core.designsystem.theme.*

/**
 * Channel quick-switcher drawer per iptv-player-ux:
 * Bottom sheet on portrait, side panel on landscape.
 * Includes search & category filters.
 */
@Composable
fun ChannelQuickDrawer(
    visible: Boolean,
    currentChannelId: String,
    channels: List<ChannelEntity>,
    isLandscape: Boolean,
    onSelectChannel: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = remember(channels) {
        val cats = mutableListOf("All")
        cats.addAll(channels.map { it.category }.distinct().filter { it.isNotBlank() }.sorted())
        cats
    }

    val filteredChannels = remember(channels, searchQuery, selectedCategory) {
        val catFiltered = when (selectedCategory) {
            "All" -> channels
            else -> channels.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        }
        if (searchQuery.isBlank()) {
            catFiltered
        } else {
            val q = searchQuery.trim().lowercase()
            catFiltered.filter {
                it.name.lowercase().contains(q) ||
                it.cleanName.lowercase().contains(q) ||
                it.category.lowercase().contains(q) ||
                it.language.lowercase().contains(q)
            }
        }
    }

    if (visible) {
        val drawerShape = if (isLandscape) NisSideDrawerShape else NisBottomSheetShape
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(NisScrimMedium)
                .clickable { onClose() },
            contentAlignment = if (isLandscape) Alignment.CenterEnd else Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .then(
                        if (isLandscape) {
                            Modifier
                                .fillMaxHeight()
                                .width(380.dp)
                                .clip(drawerShape)
                        } else {
                            Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.75f)
                                .clip(drawerShape)
                        }
                    )
                    .background(NisTheme.colors.surface)
                    .border(
                        width = 1.dp,
                        color = NisTheme.colors.borderSubtle,
                        shape = drawerShape
                    )
                    .clickable(enabled = false) {}
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 12.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Quick Switcher",
                                color = NisTheme.colors.textPrimary,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "(${filteredChannels.size})",
                                color = NisAccentPrimary,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = NisIcons.Close,
                                contentDescription = "Close",
                                tint = NisTheme.colors.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Search input
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text("Filter channels...", color = NisTheme.colors.textMuted, style = MaterialTheme.typography.bodySmall)
                            },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = NisTheme.colors.surfaceElevated,
                                unfocusedContainerColor = NisTheme.colors.surfaceElevated,
                                focusedTextColor = NisTheme.colors.textPrimary,
                                unfocusedTextColor = NisTheme.colors.textPrimary,
                                focusedIndicatorColor = NisAccentPrimary,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            shape = NisThumbnailShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        )
                    }

                    // Categories chip row
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categories) { cat ->
                            NisCategoryChip(
                                text = cat,
                                isSelected = selectedCategory.equals(cat, ignoreCase = true),
                                onClick = { selectedCategory = cat }
                            )
                        }
                    }

                    // Channels List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredChannels, key = { it.id }) { ch ->
                            val isCurrent = ch.id == currentChannelId
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(NisMediaCardShape)
                                    .background(if (isCurrent) NisAccentPrimary.copy(alpha = 0.15f) else NisTheme.colors.surfaceElevated)
                                    .border(
                                        width = 1.dp,
                                        color = if (isCurrent) NisAccentPrimary else NisTheme.colors.borderSubtle,
                                        shape = NisMediaCardShape
                                    )
                                    .clickable {
                                        onSelectChannel(ch.id)
                                        onClose()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                // Channel Logo / Avatar
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(NisShapes.small)
                                        .background(NisTheme.colors.surface)
                                        .border(1.dp, NisTheme.colors.borderSubtle, NisShapes.small),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (ch.logoUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = ch.logoUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    } else {
                                        ChannelMonogram(name = ch.cleanName, style = MaterialTheme.typography.labelMedium)
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = ch.cleanName.ifEmpty { ch.name },
                                            color = if (isCurrent) NisAccentPrimary else NisTheme.colors.textPrimary,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (isCurrent) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(NisLiveRed)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = "${ch.category} • ${ch.language}",
                                        color = NisTheme.colors.textMuted,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Resolution pill
                                Box(
                                    modifier = Modifier
                                        .clip(NisBadgeShape)
                                        .background(NisTheme.colors.surface)
                                        .border(0.5.dp, NisTheme.colors.borderSubtle, NisBadgeShape)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = ch.resolution.ifEmpty { "HD" },
                                        color = NisAccentPrimary,
                                        style = NisTheme.textExtras.micro,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
