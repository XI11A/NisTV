package com.nis.nistv.feature.downloads.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nis.nistv.core.designsystem.icon.NisIcons
import com.nis.nistv.core.designsystem.theme.*
import com.nis.nistv.core.preferences.*
import java.util.Locale

data class MediaTrackOption(
    val groupIndex: Int,
    val trackIndex: Int,
    val type: Int, // C.TRACK_TYPE_AUDIO or C.TRACK_TYPE_TEXT
    val label: String,
    val language: String?,
    val isSelected: Boolean
)

enum class TrackDialogTab(val title: String) {
    AUDIO("Audio"),
    SUBTITLES("Subtitles"),
    STYLE("Style & Preview")
}

@Composable
fun OfflinePlayerTrackDialog(
    initialTab: TrackDialogTab = TrackDialogTab.SUBTITLES,
    audioTracks: List<MediaTrackOption>,
    subtitleTracks: List<MediaTrackOption>,
    isSubtitlesDisabled: Boolean,
    subtitleStyle: SubtitleStylePreferences,
    onSelectAudioTrack: (MediaTrackOption) -> Unit,
    onSelectSubtitleTrack: (MediaTrackOption?) -> Unit,
    onUpdateSubtitleStyle: (SubtitleStylePreferences) -> Unit,
    onDismiss: () -> Unit,
    onAddSubtitleFile: (() -> Unit)? = null,
    selectedSecondarySubtitleTrack: MediaTrackOption? = null,
    onSelectSecondarySubtitleTrack: ((MediaTrackOption?) -> Unit)? = null,
    primaryDelayMs: Long = 0L,
    onUpdatePrimaryDelay: ((Long) -> Unit)? = null,
    secondaryDelayMs: Long = 0L,
    onUpdateSecondaryDelay: ((Long) -> Unit)? = null
) {
    var selectedTab by remember { mutableStateOf(initialTab) }
    var currentStyle by remember(subtitleStyle) { mutableStateOf(subtitleStyle) }
    var previewText by remember(subtitleStyle.customPreviewText) {
        mutableStateOf(subtitleStyle.customPreviewText.ifBlank { "Here is how your subtitles will look during playback" })
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 560.dp)
                    .fillMaxHeight(0.88f)
                    .clickable(enabled = false) {}, // prevent click-through
                shape = NisDialogShape,
                colors = CardDefaults.cardColors(containerColor = NisTheme.colors.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, NisTheme.colors.borderSubtle)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp)
                ) {
                    // Header Row with Title and Close Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = when (selectedTab) {
                                    TrackDialogTab.AUDIO -> NisIcons.Audiotrack
                                    TrackDialogTab.SUBTITLES -> NisIcons.Subtitles
                                    TrackDialogTab.STYLE -> NisIcons.Tune
                                },
                                contentDescription = null,
                                tint = NisAccentPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "Audio & Subtitles",
                                color = NisTheme.colors.textPrimary,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(NisTheme.colors.surfaceElevated)
                        ) {
                            Icon(
                                imageVector = NisIcons.Close,
                                contentDescription = "Close",
                                tint = NisTheme.colors.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Tab Navigation Strip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(NisMediaCardShape)
                            .background(NisTheme.colors.surfaceElevated)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TrackDialogTab.values().forEach { tab ->
                            val isSelected = selectedTab == tab
                            val bgColor by animateColorAsState(
                                if (isSelected) NisAccentPrimary else Color.Transparent,
                                label = "tabBg"
                            )
                            val textColor by animateColorAsState(
                                if (isSelected) Color.White else NisTheme.colors.textSecondary,
                                label = "tabText"
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(NisShapes.small)
                                    .background(bgColor)
                                    .clickable { selectedTab = tab }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val count = when (tab) {
                                    TrackDialogTab.AUDIO -> audioTracks.size
                                    TrackDialogTab.SUBTITLES -> subtitleTracks.size
                                    TrackDialogTab.STYLE -> null
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = tab.title,
                                        color = textColor,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                    if (count != null && count > 0) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (isSelected) Color.White.copy(alpha = 0.25f) else NisTheme.colors.surface
                                        ) {
                                            Text(
                                                text = count.toString(),
                                                color = textColor,
                                                style = NisTheme.textExtras.micro,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Tab Body
                    Box(modifier = Modifier.weight(1f)) {
                        when (selectedTab) {
                            TrackDialogTab.AUDIO -> {
                                AudioTracksTab(
                                    audioTracks = audioTracks,
                                    onSelect = {
                                        onSelectAudioTrack(it)
                                        onDismiss()
                                    }
                                )
                            }
                            TrackDialogTab.SUBTITLES -> {
                                SubtitlesTab(
                                    subtitleTracks = subtitleTracks,
                                    isSubtitlesDisabled = isSubtitlesDisabled,
                                    selectedSecondaryTrack = selectedSecondarySubtitleTrack,
                                    primaryDelayMs = primaryDelayMs,
                                    secondaryDelayMs = secondaryDelayMs,
                                    onSelect = onSelectSubtitleTrack,
                                    onSelectSecondary = onSelectSecondarySubtitleTrack,
                                    onUpdatePrimaryDelay = onUpdatePrimaryDelay,
                                    onUpdateSecondaryDelay = onUpdateSecondaryDelay,
                                    onOpenStyle = { selectedTab = TrackDialogTab.STYLE },
                                    onAddSubtitleFile = onAddSubtitleFile
                                )
                            }
                            TrackDialogTab.STYLE -> {
                                SubtitleStyleTab(
                                    style = currentStyle,
                                    previewText = previewText,
                                    onPreviewTextChange = {
                                        previewText = it
                                        val updated = currentStyle.copy(customPreviewText = it)
                                        currentStyle = updated
                                        onUpdateSubtitleStyle(updated)
                                    },
                                    onStyleChange = { updated ->
                                        currentStyle = updated
                                        onUpdateSubtitleStyle(updated)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioTracksTab(
    audioTracks: List<MediaTrackOption>,
    onSelect: (MediaTrackOption) -> Unit
) {
    if (audioTracks.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No audio stream information available",
                color = NisTheme.colors.textMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(audioTracks) { track ->
            TrackCard(
                label = track.label,
                badge = track.language?.uppercase() ?: "AUDIO",
                isSelected = track.isSelected,
                onClick = { onSelect(track) }
            )
        }
    }
}

@Composable
private fun SubtitleDelayControl(
    label: String,
    delayMs: Long,
    onDelayChange: ((Long) -> Unit)?
) {
    if (onDelayChange == null) return
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = NisThumbnailShape,
        colors = CardDefaults.cardColors(containerColor = NisTheme.colors.surfaceElevated),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, NisTheme.colors.borderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    color = NisTheme.colors.textPrimary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold
                )
                val formatted = when {
                    delayMs == 0L -> "0 ms (In sync)"
                    delayMs > 0L -> "+${delayMs} ms (${String.format(Locale.US, "+%.2f s", delayMs / 1000.0)})"
                    else -> "${delayMs} ms (${String.format(Locale.US, "%.2f s", delayMs / 1000.0)})"
                }
                Text(
                    text = formatted,
                    color = if (delayMs != 0L) NisAccentPrimary else NisTheme.colors.textMuted,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = { onDelayChange((delayMs - 50L).coerceAtLeast(-10_000L)) },
                    modifier = Modifier
                        .size(32.dp)
                        .background(NisTheme.colors.surface, CircleShape)
                ) {
                    Text("-50ms", color = NisTheme.colors.textPrimary, style = NisTheme.textExtras.micro, fontWeight = FontWeight.Bold)
                }

                if (delayMs != 0L) {
                    TextButton(
                        onClick = { onDelayChange(0L) },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Reset", color = NisError, style = MaterialTheme.typography.labelSmall)
                    }
                }

                IconButton(
                    onClick = { onDelayChange((delayMs + 50L).coerceAtMost(10_000L)) },
                    modifier = Modifier
                        .size(32.dp)
                        .background(NisTheme.colors.surface, CircleShape)
                ) {
                    Text("+50ms", color = NisTheme.colors.textPrimary, style = NisTheme.textExtras.micro, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SubtitlesTab(
    subtitleTracks: List<MediaTrackOption>,
    isSubtitlesDisabled: Boolean,
    selectedSecondaryTrack: MediaTrackOption?,
    primaryDelayMs: Long,
    secondaryDelayMs: Long,
    onSelect: (MediaTrackOption?) -> Unit,
    onSelectSecondary: ((MediaTrackOption?) -> Unit)?,
    onUpdatePrimaryDelay: ((Long) -> Unit)?,
    onUpdateSecondaryDelay: ((Long) -> Unit)?,
    onOpenStyle: () -> Unit,
    onAddSubtitleFile: (() -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Quick Action Row for Styling
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Primary Subtitle Track",
                color = NisTheme.colors.textSecondary,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )

            TextButton(
                onClick = onOpenStyle,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = NisIcons.Tune,
                    contentDescription = null,
                    tint = NisAccentPrimary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Customize Appearance",
                    color = NisAccentPrimary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Option: Add Subtitle File
            if (onAddSubtitleFile != null) {
                item(key = "add_external_sub") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(NisMediaCardShape)
                            .clickable(onClick = onAddSubtitleFile),
                        shape = NisMediaCardShape,
                        colors = CardDefaults.cardColors(containerColor = NisTheme.colors.surfaceElevated),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NisAccentPrimary.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = NisIcons.Subtitles,
                                contentDescription = null,
                                tint = NisAccentPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Add External Subtitle File (.srt, .vtt, .ass)",
                                color = NisAccentPrimary,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Option 1: Off (Disable Subtitles)
            item(key = "track_off") {
                TrackCard(
                    label = "Off (No Subtitles)",
                    badge = "NONE",
                    isSelected = isSubtitlesDisabled,
                    onClick = { onSelect(null) }
                )
            }

            if (subtitleTracks.isEmpty()) {
                item(key = "no_subs") {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = NisTheme.colors.surfaceElevated),
                        shape = NisMediaCardShape
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "No Embedded Subtitles",
                                color = NisTheme.colors.textPrimary,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "This media file does not contain embedded subtitle tracks.\n\nTip: You can place a .srt, .vtt, or .ass file with the same filename in the download folder and it will load automatically.",
                                color = NisTheme.colors.textSecondary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            } else {
                items(subtitleTracks) { track ->
                    TrackCard(
                        label = track.label,
                        badge = track.language?.uppercase() ?: "CC",
                        isSelected = !isSubtitlesDisabled && track.isSelected,
                        onClick = { onSelect(track) }
                    )
                }

                // Primary Delay Control (if primary is selected)
                if (!isSubtitlesDisabled && onUpdatePrimaryDelay != null) {
                    item(key = "primary_delay_control") {
                        SubtitleDelayControl(
                            label = "Primary Subtitle Delay",
                            delayMs = primaryDelayMs,
                            onDelayChange = onUpdatePrimaryDelay
                        )
                    }
                }

                // --- Second Subtitle (Dual Mode - Top of Screen) ---
                if (onSelectSecondary != null) {
                    item(key = "secondary_sub_header") {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = NisIcons.Subtitles,
                                contentDescription = null,
                                tint = NisAccentCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Second Subtitle (Dual Mode - Top of Screen)",
                                color = NisAccentCyan,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // None / Off for second subtitle
                    item(key = "sec_track_none") {
                        TrackCard(
                            label = "None (Single Subtitle Only)",
                            badge = "OFF",
                            isSelected = selectedSecondaryTrack == null,
                            onClick = { onSelectSecondary(null) }
                        )
                    }

                    // Secondary track options
                    items(subtitleTracks) { track ->
                        val isSecondarySelected = selectedSecondaryTrack?.groupIndex == track.groupIndex &&
                                selectedSecondaryTrack?.trackIndex == track.trackIndex
                        TrackCard(
                            label = track.label,
                            badge = track.language?.uppercase() ?: "CC",
                            isSelected = isSecondarySelected,
                            onClick = { onSelectSecondary(track) }
                        )
                    }

                    // Secondary Delay Control (if secondary track is selected)
                    if (selectedSecondaryTrack != null && onUpdateSecondaryDelay != null) {
                        item(key = "secondary_delay_control") {
                            SubtitleDelayControl(
                                label = "Second Subtitle Delay",
                                delayMs = secondaryDelayMs,
                                onDelayChange = onUpdateSecondaryDelay
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackCard(
    label: String,
    badge: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) NisAccentPrimary else NisTheme.colors.borderSubtle
    val bgColor = if (isSelected) NisAccentPrimary.copy(alpha = 0.12f) else NisTheme.colors.surfaceElevated

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(NisMediaCardShape)
            .clickable(onClick = onClick),
        shape = NisMediaCardShape,
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = NisBadgeShape,
                    color = if (isSelected) NisAccentPrimary else NisTheme.colors.borderSubtle
                ) {
                    Text(
                        text = badge,
                        color = Color.White,
                        style = NisTheme.textExtras.micro,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = label,
                    color = if (isSelected) NisAccentPrimary else NisTheme.colors.textPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = NisIcons.Check,
                    contentDescription = "Selected",
                    tint = NisAccentPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun SubtitleStyleTab(
    style: SubtitleStylePreferences,
    previewText: String,
    onPreviewTextChange: (String) -> Unit,
    onStyleChange: (SubtitleStylePreferences) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ==========================================
        // 1. LIVE SUBTITLE PREVIEW BOX
        // ==========================================
        Text(
            text = "LIVE SUBTITLE PREVIEW",
            color = NisTheme.colors.textSecondary,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        val shadow = when (style.edgeStyle) {
            SubtitleEdgeStyle.NONE -> null
            SubtitleEdgeStyle.OUTLINE -> Shadow(
                color = Color.Black,
                offset = androidx.compose.ui.geometry.Offset(0f, 0f),
                blurRadius = 4f
            )
            SubtitleEdgeStyle.DROP_SHADOW -> Shadow(
                color = Color.Black,
                offset = androidx.compose.ui.geometry.Offset(3f, 3f),
                blurRadius = 3f
            )
            SubtitleEdgeStyle.RAISED -> Shadow(
                color = Color.Black,
                offset = androidx.compose.ui.geometry.Offset(-2f, -2f),
                blurRadius = 2f
            )
        }

        val fontFamily = when (style.fontStyle) {
            SubtitleFontStyle.SANS_SERIF -> FontFamily.SansSerif
            SubtitleFontStyle.SERIF -> FontFamily.Serif
            SubtitleFontStyle.MONOSPACE -> FontFamily.Monospace
            SubtitleFontStyle.BOLD -> FontFamily.SansSerif
        }

        val fontWeight = if (style.fontStyle == SubtitleFontStyle.BOLD) FontWeight.Bold else FontWeight.Medium
        val previewFontSize = (14 * style.textSize.scale).sp

        // Simulated Movie Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(NisMediaCardShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            NisTheme.colors.surfaceElevated,
                            NisTheme.colors.surface,
                            NisTheme.colors.background
                        )
                    )
                )
                .border(1.dp, NisTheme.colors.borderSubtle, NisMediaCardShape)
                .padding(12.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Simulated video background hint
            Text(
                text = "▶ VIDEO PLAYBACK SCENE",
                color = Color.White.copy(alpha = 0.15f),
                style = NisTheme.textExtras.micro,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopStart)
            )

            // Styled Subtitle Chip
            Box(
                modifier = Modifier
                    .clip(NisBadgeShape)
                    .background(style.backgroundColor.color)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = previewText.ifBlank { "Sample subtitle text" },
                    color = style.textColor.color,
                    fontSize = previewFontSize,
                    fontFamily = fontFamily,
                    fontWeight = fontWeight,
                    style = TextStyle(shadow = shadow),
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // ==========================================
        // 2. CHANGEABLE PREVIEW TEXT INPUT
        // ==========================================
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Preview Text (User Changeable)",
                color = NisTheme.colors.textPrimary,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedTextField(
                value = previewText,
                onValueChange = onPreviewTextChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = NisThumbnailShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NisAccentPrimary,
                    unfocusedBorderColor = NisTheme.colors.borderSubtle,
                    focusedTextColor = NisTheme.colors.textPrimary,
                    unfocusedTextColor = NisTheme.colors.textPrimary,
                    focusedContainerColor = NisTheme.colors.surfaceElevated,
                    unfocusedContainerColor = NisTheme.colors.surfaceElevated
                ),
                placeholder = {
                    Text("Type custom preview text...", color = NisTheme.colors.textMuted, style = MaterialTheme.typography.bodySmall)
                }
            )

            // Quick Preset Suggestions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "Standard" to "Here is how your subtitles will look",
                    "Dialogue" to "Where are we heading next?",
                    "Action" to "Hold on! Watch out for incoming fire!"
                ).forEach { (tag, text) ->
                    Surface(
                        shape = NisBadgeShape,
                        color = NisTheme.colors.surfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, NisTheme.colors.borderSubtle),
                        modifier = Modifier.clickable { onPreviewTextChange(text) }
                    ) {
                        Text(
                            text = tag,
                            color = NisTheme.colors.textSecondary,
                            style = NisTheme.textExtras.micro,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = NisTheme.colors.borderSubtle, thickness = 0.8.dp)

        // ==========================================
        // 3. TEXT SIZE SELECTOR
        // ==========================================
        StyleSection(title = "Text Size") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SubtitleTextSize.values().forEach { size ->
                    val isSelected = style.textSize == size
                    SelectChip(
                        label = size.label.substringBefore(" ("),
                        isSelected = isSelected,
                        modifier = Modifier.weight(1f),
                        onClick = { onStyleChange(style.copy(textSize = size)) }
                    )
                }
            }
        }

        // ==========================================
        // 4. TEXT COLOR SELECTOR
        // ==========================================
        StyleSection(title = "Text Color") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SubtitleColor.values().forEach { color ->
                    val isSelected = style.textColor == color
                    val borderColor = if (isSelected) NisAccentPrimary else NisTheme.colors.borderSubtle

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clip(NisThumbnailShape)
                            .clickable { onStyleChange(style.copy(textColor = color)) },
                        shape = NisThumbnailShape,
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) NisAccentPrimary.copy(alpha = 0.15f) else NisTheme.colors.surfaceElevated
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(color.color)
                                    .border(1.dp, Color.Black.copy(alpha = 0.5f), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = color.label,
                                color = if (isSelected) NisAccentPrimary else NisTheme.colors.textPrimary,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // 5. BACKGROUND SELECTOR
        // ==========================================
        StyleSection(title = "Background Box") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SubtitleBackground.values().forEach { bg ->
                    SelectChip(
                        label = bg.label,
                        isSelected = style.backgroundColor == bg,
                        modifier = Modifier.weight(1f),
                        onClick = { onStyleChange(style.copy(backgroundColor = bg)) }
                    )
                }
            }
        }

        // ==========================================
        // 6. EDGE / OUTLINE STYLE
        // ==========================================
        StyleSection(title = "Edge Style / Shadow") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SubtitleEdgeStyle.values().forEach { edge ->
                    SelectChip(
                        label = edge.label,
                        isSelected = style.edgeStyle == edge,
                        modifier = Modifier.weight(1f),
                        onClick = { onStyleChange(style.copy(edgeStyle = edge)) }
                    )
                }
            }
        }

        // ==========================================
        // 7. FONT STYLE SELECTOR
        // ==========================================
        StyleSection(title = "Font Style") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SubtitleFontStyle.values().forEach { font ->
                    SelectChip(
                        label = font.label,
                        isSelected = style.fontStyle == font,
                        modifier = Modifier.weight(1f),
                        onClick = { onStyleChange(style.copy(fontStyle = font)) }
                    )
                }
            }
        }

        // ==========================================
        // 8. RESET BUTTON
        // ==========================================
        OutlinedButton(
            onClick = {
                val defaultStyle = SubtitleStylePreferences()
                onStyleChange(defaultStyle)
                onPreviewTextChange("Here is how your subtitles will look during playback")
            },
            modifier = Modifier.fillMaxWidth(),
            shape = NisThumbnailShape,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = NisTheme.colors.textSecondary),
            border = androidx.compose.foundation.BorderStroke(1.dp, NisTheme.colors.borderSubtle)
        ) {
            Icon(imageVector = NisIcons.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Reset Subtitle Style to Defaults", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun StyleSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            color = NisTheme.colors.textSecondary,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )
        content()
    }
}

@Composable
private fun SelectChip(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) NisAccentPrimary else NisTheme.colors.borderSubtle
    val bgColor = if (isSelected) NisAccentPrimary.copy(alpha = 0.15f) else NisTheme.colors.surfaceElevated
    val textColor = if (isSelected) NisAccentPrimary else NisTheme.colors.textPrimary

    Card(
        modifier = modifier
            .clip(NisShapes.small)
            .clickable(onClick = onClick),
        shape = NisShapes.small,
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = textColor,
                style = NisTheme.textExtras.micro,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
