package com.nis.nistv.feature.livetv.playlist

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nis.nistv.NisTvApplication
import com.nis.nistv.core.database.entity.PlaylistEntity
import com.nis.nistv.core.designsystem.component.NisBadge
import com.nis.nistv.core.designsystem.component.NisEmptyState
import com.nis.nistv.core.designsystem.component.NisPrimaryButton
import com.nis.nistv.core.designsystem.component.NisSecondaryButton
import com.nis.nistv.core.designsystem.icon.NisIcons
import com.nis.nistv.core.designsystem.theme.*
import com.nis.nistv.data.playlist.M3uParser
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PlaylistsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as NisTvApplication
    val viewModel: PlaylistsViewModel = viewModel(
        factory = PlaylistsViewModel.provideFactory(
            playlistDao = app.container.database.playlistDao(),
            playlistSyncManager = app.container.playlistSyncManager
        )
    )

    val playlists by viewModel.playlists.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var playlistToDelete by remember { mutableStateOf<PlaylistEntity?>(null) }
    var playlistToRename by remember { mutableStateOf<PlaylistEntity?>(null) }

    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearMessages()
        }
        uiState.successMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NisTheme.colors.background)
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = NisDimens.MaxContentWidth)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = NisIcons.Back,
                        contentDescription = "Back",
                        tint = NisTheme.colors.textPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "My Playlists",
                    color = NisTheme.colors.textPrimary,
                    style = MaterialTheme.typography.titleLarge
                )
            }

            IconButton(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(NisTheme.colors.surfaceElevated)
            ) {
                Icon(
                    imageVector = NisIcons.Add,
                    contentDescription = "Add Playlist",
                    tint = NisTheme.colors.primary
                )
            }
        }

        // Content
        if (playlists.isEmpty() && !uiState.isLoading) {
            NisEmptyState(
                title = "No Custom Playlists",
                message = "Import your own M3U or M3U8 playlists by URL or local file to enjoy personalized live TV channels.",
                icon = NisIcons.Playlists,
                actionText = "Add Playlist",
                onActionClick = { showAddDialog = true },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = NisDimens.MaxContentWidth),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(playlists, key = { it.id }) { playlist ->
                    PlaylistCard(
                        playlist = playlist,
                        isSyncing = uiState.syncingPlaylistId == playlist.id,
                        onToggleEnabled = { enabled -> viewModel.setEnabled(playlist.id, enabled) },
                        onRefresh = { viewModel.syncPlaylist(playlist.id) },
                        onRename = { playlistToRename = playlist },
                        onDelete = { playlistToDelete = playlist }
                    )
                }
            }
        }
    }

    // Add Playlist Dialog
    if (showAddDialog) {
        AddPlaylistDialog(
            isLoading = uiState.isLoading,
            onDismiss = { showAddDialog = false },
            onAdd = { name, sourceType, urlOrPath, autoRefresh ->
                viewModel.addPlaylist(name, sourceType, urlOrPath, autoRefresh)
                showAddDialog = false
            }
        )
    }

    // Rename Dialog
    playlistToRename?.let { playlist ->
        RenamePlaylistDialog(
            currentName = playlist.name,
            onDismiss = { playlistToRename = null },
            onConfirm = { newName ->
                viewModel.renamePlaylist(playlist.id, newName)
                playlistToRename = null
            }
        )
    }

    // Delete Confirmation Dialog
    playlistToDelete?.let { playlist ->
        AlertDialog(
            onDismissRequest = { playlistToDelete = null },
            containerColor = NisTheme.colors.surfaceElevated,
            title = {
                Text(
                    text = "Delete Playlist?",
                    color = NisTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This will permanently remove \"${playlist.name}\" and all of its associated channels, streams, and favorites. This action cannot be undone.",
                    color = NisTheme.colors.textSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePlaylist(playlist.id)
                        playlistToDelete = null
                    }
                ) {
                    Text(text = "Delete", color = NisTheme.colors.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { playlistToDelete = null }) {
                    Text(text = "Cancel", color = NisTheme.colors.textMuted)
                }
            }
        )
    }
}

@Composable
private fun PlaylistCard(
    playlist: PlaylistEntity,
    isSyncing: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()) }
    val lastSyncStr = remember(playlist.lastSyncedAt) {
        if (playlist.lastSyncedAt > 0L) dateFormat.format(Date(playlist.lastSyncedAt)) else "Never"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(NisMediaCardShape)
            .background(NisTheme.colors.surface)
            .border(1.dp, NisTheme.colors.border, NisMediaCardShape)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = playlist.name,
                        color = NisTheme.colors.textPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    NisBadge(
                        text = playlist.sourceType,
                        backgroundColor = if (playlist.sourceType == "URL") NisTheme.colors.accentCyan.copy(alpha = 0.2f) else NisTheme.colors.primary.copy(alpha = 0.2f),
                        textColor = if (playlist.sourceType == "URL") NisTheme.colors.accentCyan else NisTheme.colors.primary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${playlist.channelCount} channels • Synced: $lastSyncStr",
                    color = NisTheme.colors.textMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Switch(
                checked = playlist.enabled,
                onCheckedChange = onToggleEnabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = NisTheme.colors.primary,
                    checkedTrackColor = NisTheme.colors.primary.copy(alpha = 0.4f),
                    uncheckedThumbColor = NisTheme.colors.textMuted,
                    uncheckedTrackColor = NisTheme.colors.surfaceElevated
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Masked URL / Path
        Text(
            text = M3uParser.maskUrl(playlist.url),
            color = NisTheme.colors.textMuted,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Bottom Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSyncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = NisTheme.colors.primary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(16.dp))
            } else {
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = NisIcons.Refresh,
                        contentDescription = "Refresh",
                        tint = NisTheme.colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            IconButton(onClick = onRename) {
                Icon(
                    imageVector = NisIcons.Settings,
                    contentDescription = "Rename",
                    tint = NisTheme.colors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = NisIcons.Delete,
                    contentDescription = "Delete",
                    tint = NisTheme.colors.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun AddPlaylistDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onAdd: (name: String, sourceType: String, urlOrPath: String, autoRefresh: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var sourceType by remember { mutableStateOf("URL") } // "URL" or "FILE"
    var urlInput by remember { mutableStateOf("") }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    var autoRefresh by remember { mutableStateOf(true) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedFileUri = uri
            selectedFileName = uri.lastPathSegment?.substringAfterLast('/') ?: uri.toString()
            if (name.isBlank()) {
                name = selectedFileName.substringBeforeLast('.')
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NisTheme.colors.surfaceElevated,
        title = {
            Text(
                text = "Add M3U Playlist",
                color = NisTheme.colors.textPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Playlist Name") },
                    placeholder = { Text("e.g., Sports TV") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = NisTheme.colors.textPrimary,
                        unfocusedTextColor = NisTheme.colors.textPrimary,
                        focusedBorderColor = NisTheme.colors.primary,
                        unfocusedBorderColor = NisTheme.colors.border
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Source Type Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(NisButtonShape)
                        .background(NisTheme.colors.surface)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(NisBadgeShape)
                            .background(if (sourceType == "URL") NisTheme.colors.primary else Color.Transparent)
                            .clickable { sourceType = "URL" }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "From URL",
                            color = if (sourceType == "URL") Color.White else NisTheme.colors.textSecondary,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(NisBadgeShape)
                            .background(if (sourceType == "FILE") NisTheme.colors.primary else Color.Transparent)
                            .clickable { sourceType = "FILE" }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Local File",
                            color = if (sourceType == "FILE") Color.White else NisTheme.colors.textSecondary,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                // URL or File Input
                if (sourceType == "URL") {
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = { Text("Playlist URL (.m3u/.m3u8)") },
                        placeholder = { Text("https://example.com/playlist.m3u") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = NisTheme.colors.textPrimary,
                            unfocusedTextColor = NisTheme.colors.textPrimary,
                            focusedBorderColor = NisTheme.colors.primary,
                            unfocusedBorderColor = NisTheme.colors.border
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Auto-refresh every 24h",
                            color = NisTheme.colors.textSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Switch(
                            checked = autoRefresh,
                            onCheckedChange = { autoRefresh = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NisTheme.colors.primary,
                                checkedTrackColor = NisTheme.colors.primary.copy(alpha = 0.4f)
                            )
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(NisButtonShape)
                            .border(1.dp, NisTheme.colors.border, NisButtonShape)
                            .clickable {
                                filePickerLauncher.launch(
                                    arrayOf("audio/x-mpegurl", "application/vnd.apple.mpegurl", "text/plain", "*/*")
                                )
                            }
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = NisIcons.Playlists,
                            contentDescription = null,
                            tint = NisTheme.colors.accentCyan,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (selectedFileName.isNotEmpty()) selectedFileName else "Tap to choose .m3u or .m3u8 file",
                            color = if (selectedFileName.isNotEmpty()) NisTheme.colors.textPrimary else NisTheme.colors.textMuted,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selectedFileName.isNotEmpty()) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = NisTheme.colors.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isLoading && (
                    (sourceType == "URL" && urlInput.isNotBlank()) ||
                    (sourceType == "FILE" && selectedFileUri != null)
                ),
                onClick = {
                    val finalPath = if (sourceType == "URL") urlInput.trim() else selectedFileUri.toString()
                    val finalName = name.ifBlank { if (sourceType == "URL") "Web Playlist" else selectedFileName }
                    onAdd(finalName, sourceType, finalPath, autoRefresh)
                }
            ) {
                Text(text = "Add", color = NisTheme.colors.primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text(text = "Cancel", color = NisTheme.colors.textMuted)
            }
        }
    )
}

@Composable
private fun RenamePlaylistDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NisTheme.colors.surfaceElevated,
        title = {
            Text(text = "Rename Playlist", color = NisTheme.colors.textPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Playlist Name") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = NisTheme.colors.textPrimary,
                    unfocusedTextColor = NisTheme.colors.textPrimary,
                    focusedBorderColor = NisTheme.colors.primary,
                    unfocusedBorderColor = NisTheme.colors.border
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onConfirm(name.trim()) }
            ) {
                Text(text = "Save", color = NisTheme.colors.primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel", color = NisTheme.colors.textMuted)
            }
        }
    )
}
