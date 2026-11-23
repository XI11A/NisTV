package com.nis.nistv.feature.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nis.nistv.core.designsystem.component.NisPrimaryButton
import com.nis.nistv.core.designsystem.icon.NisIcons
import com.nis.nistv.core.designsystem.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    viewModel: BackupRestoreViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var isProcessingExport by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isProcessingExport = true
                try {
                    val exportJson = viewModel.generateExportData()
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { os ->
                            os.write(exportJson.toByteArray(Charsets.UTF_8))
                            os.flush()
                        }
                    }
                    snackbarHostState.showSnackbar("Backup successfully exported")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Export failed: ${e.message}")
                } finally {
                    isProcessingExport = false
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val content = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            inputStream.bufferedReader(Charsets.UTF_8).readText()
                        }
                    }
                    if (!content.isNullOrBlank()) {
                        viewModel.onBackupFileRead(content)
                    } else {
                        snackbarHostState.showSnackbar("Selected backup file is empty")
                    }
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Failed to read file: ${e.message}")
                }
            }
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = NisTheme.colors.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Backup & Restore", color = NisTheme.colors.textPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NisTheme.colors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NisTheme.colors.background)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = NisDimens.MaxContentWidth),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // 1. Info / Security Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = NisSurface),
                    shape = NisShapes.medium
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = NisAccentCyan,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Safe Data Portability",
                                color = NisTextPrimary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "Backup includes favorites, watch later, playlists, and settings. Sensitive tokens (AFDS, Google, TMDB keys, PIN hashes) are strictly excluded.",
                                color = NisTextMuted,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            // 2. Export Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = NisCardBackground),
                    shape = NisShapes.medium
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(NisIcons.Save, contentDescription = null, tint = NisAccentPrimary, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export Backup", color = NisTextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Create an encrypted-checksum JSON file to transfer your NisTV data to another device or keep a local backup.",
                            color = NisTextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Include Recent Searches", color = NisTextPrimary, style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = uiState.includeRecentSearches,
                                onCheckedChange = { viewModel.setIncludeRecentSearches(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = NisAccentPrimary
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(NisSurface, NisShapes.small)
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = NisAccentAmber, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Note: Custom playlist URLs may contain embedded query access tokens.",
                                color = NisTextMuted,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        NisPrimaryButton(
                            text = if (isProcessingExport) "Preparing Export..." else "Export to File",
                            onClick = {
                                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                                exportLauncher.launch("nistv_backup_$timestamp.json")
                            },
                            enabled = !isProcessingExport,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // 3. Import Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = NisCardBackground),
                    shape = NisShapes.medium
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(NisIcons.Download, contentDescription = null, tint = NisAccentCyan, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Restore from Backup", color = NisTextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Restore saved favorites, watch later items, and settings from a NisTV backup JSON file.",
                            color = NisTextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        NisPrimaryButton(
                            text = if (uiState.isImporting) "Restoring..." else "Select Backup File",
                            onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                            enabled = !uiState.isImporting,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

    // Preview / Restore Confirmation Dialog
    uiState.preview?.let { preview ->
        var replaceExisting by remember { mutableStateOf(false) }

        val dateStr = remember(preview.createdAt) {
            SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(preview.createdAt))
        }

        AlertDialog(
            onDismissRequest = { viewModel.clearPreview() },
            shape = NisDialogShape,
            containerColor = NisSurface,
            title = {
                Text("Restore Backup Preview", color = NisTextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("App Version: ${preview.appVersion} • Created: $dateStr", color = NisTextMuted, style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Contents to Restore:", color = NisTextPrimary, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("• ${preview.favoritesCount} Favorites", color = NisTextSecondary, style = MaterialTheme.typography.bodySmall)
                    Text("• ${preview.watchLaterCount} Watch Later Items", color = NisTextSecondary, style = MaterialTheme.typography.bodySmall)
                    Text("• ${preview.playlistsCount} Custom Playlists", color = NisTextSecondary, style = MaterialTheme.typography.bodySmall)
                    if (preview.recentSearchesCount > 0) {
                        Text("• ${preview.recentSearchesCount} Recent Searches", color = NisTextSecondary, style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Restore Mode:", color = NisTextPrimary, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = !replaceExisting, onClick = { replaceExisting = false }),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = !replaceExisting,
                            onClick = { replaceExisting = false },
                            colors = RadioButtonDefaults.colors(selectedColor = NisAccentPrimary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Merge (Recommended)", color = NisTextPrimary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                            Text("Combines with existing items without duplicates.", color = NisTextMuted, style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = replaceExisting, onClick = { replaceExisting = true }),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = replaceExisting,
                            onClick = { replaceExisting = true },
                            colors = RadioButtonDefaults.colors(selectedColor = NisAccentPrimary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Replace All", color = NisTextPrimary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                            Text("Overwrites current favorites and playlists.", color = NisTextMuted, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.executeRestore(replaceExisting)
                    }
                ) {
                    Text("Confirm Restore", color = NisAccentPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.clearPreview() }) {
                    Text("Cancel", color = NisTextMuted)
                }
            }
        )
    }
}
