package com.nis.nistv.feature.profile

import android.accounts.AccountManager
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.nis.nistv.BuildConfig
import com.nis.nistv.core.designsystem.component.AppUpdateDialog
import com.nis.nistv.core.designsystem.component.ChangelogDialog
import com.nis.nistv.core.update.AppUpdateChecker
import com.nis.nistv.core.update.UpdateInfo
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import com.nis.nistv.core.designsystem.component.NisLinearProgressBar
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import com.nis.nistv.core.designsystem.theme.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.semantics.Role
import com.nis.nistv.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.nis.nistv.NisTvApplication
import com.nis.nistv.core.preferences.AppThemeConfig
import com.nis.nistv.core.designsystem.component.AfdsAuthGateDialog
import com.nis.nistv.core.designsystem.component.GoogleLogoIcon
import com.nis.nistv.core.designsystem.component.NisLogo
import com.nis.nistv.core.designsystem.component.NisProfileAvatar
import com.nis.nistv.core.designsystem.component.NisCategoryChip
import com.nis.nistv.core.designsystem.component.NisPrimaryButton
import com.nis.nistv.core.designsystem.component.NisSecondaryButton
import com.nis.nistv.core.designsystem.component.NisSectionHeader
import com.nis.nistv.core.designsystem.icon.NisIcons
import com.nis.nistv.core.designsystem.theme.NisAccentCyan
import com.nis.nistv.core.designsystem.theme.NisAccentPrimary
import com.nis.nistv.core.designsystem.theme.NisBackground
import com.nis.nistv.core.designsystem.theme.NisBorder
import com.nis.nistv.core.designsystem.theme.NisChannelCardShape
import com.nis.nistv.core.designsystem.theme.NisChipShape
import com.nis.nistv.core.designsystem.theme.NisError
import com.nis.nistv.core.designsystem.theme.NisSurface
import com.nis.nistv.core.designsystem.theme.NisSurfaceElevated
import com.nis.nistv.core.designsystem.theme.NisTextMuted
import com.nis.nistv.core.designsystem.theme.NisTextPrimary
import com.nis.nistv.core.designsystem.theme.NisTextSecondary

/**
 * Redesigned Settings & Profile screen per iptv-ui-redesign and iptv-data-layer:
 * Grouped lists for:
 * 1. Account & Profile
 * 2. Playback & Video (Background playback, PiP, Preferred Language)
 * 3. Parental Controls & Filters (NSFW filter toggle)
 * 4. Cache & Data Sync (Clear cache, catalogue refresh, channel count)
 * 5. Legal Attribution & About (IPTV-org disclaimer)
 */
@Composable
fun ProfileScreen(
    onNavigateToPlaylists: () -> Unit = {},
    onNavigateToHistoryPrivacy: () -> Unit = {},
    onNavigateToParentalControls: () -> Unit = {},
    onNavigateToBackupRestore: () -> Unit = {},
    onNavigateToProfiles: () -> Unit = {},
    onNavigateToProfileEdit: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as NisTvApplication
    val viewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModel.provideFactory(
            context = context,
            database = app.container.database,
            repository = app.container.channelRepository,
            preferences = app.container.userPreferences,
            googleAuthManager = app.container.googleAuthManager,
            afdsAuthService = app.container.afdsAuthService
        )
    )

    val uiState by viewModel.uiState.collectAsState()
    var showProfileDialog by remember { mutableStateOf(false) }
    var showNewProfileDialog by remember { mutableStateOf(false) }
    var newProfileName by remember { mutableStateOf("") }
    var showTmdbKeyDialog by remember { mutableStateOf(false) }
    var tmdbKeyInput by remember { mutableStateOf("") }
    var showAfdsAuthDialog by remember { mutableStateOf(false) }
    var showManualTokenDialog by remember { mutableStateOf(false) }
    var afdsTokenInput by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()
    val updateChecker = remember { AppUpdateChecker() }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var availableUpdate by remember { mutableStateOf<UpdateInfo?>(null) }
    var showChangelogDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NisTheme.colors.background)
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = NisDimens.MaxContentWidth)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Settings",
                color = NisTheme.colors.textPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = NisDimens.MaxContentWidth),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Group 1: Account & Profile
            item {
                SettingsGroup(title = "Account & Profile") {
                    // Profile Header Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val avatar = uiState.googleUser?.avatarUrl
                        val initials = remember(uiState.afdsUserEmail, uiState.activeProfile) {
                            val email = uiState.afdsUserEmail.trim()
                            if (email.isNotBlank()) {
                                email.take(2).uppercase()
                            } else {
                                val dName = uiState.activeProfile?.name ?: "U"
                                val parts = dName.trim().split(" ").filter { it.isNotBlank() }
                                if (parts.size >= 2) {
                                    "${parts[0].take(1)}${parts[1].take(1)}".uppercase()
                                } else {
                                    dName.take(1).uppercase()
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(NisSurfaceElevated)
                                .border(1.5.dp, NisAccentPrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!avatar.isNullOrBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(avatar)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Profile Photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )
                            } else {
                                Text(
                                    text = initials,
                                    color = NisAccentPrimary,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            val fullDisplayName = when {
                                uiState.isAfdsAuthenticated && uiState.afdsUserEmail.isNotBlank() -> uiState.afdsUserEmail
                                uiState.isAfdsAuthenticated && uiState.afdsUserId.isNotBlank() -> "AFDS User #${uiState.afdsUserId}"
                                else -> uiState.activeProfile?.name ?: "Guest User"
                            }

                            Text(
                                text = fullDisplayName,
                                color = NisTextPrimary,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = when {
                                    uiState.isAfdsAuthenticated -> {
                                        val idPart = if (uiState.afdsUserId.isNotBlank()) " • ID: ${uiState.afdsUserId}" else ""
                                        "AFDS Account Connected$idPart"
                                    }
                                    else -> "Sign in with AFDS to watch & download"
                                },
                                color = if (uiState.isAfdsAuthenticated) NisLiveEmerald else NisTextSecondary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        if (uiState.isAfdsAuthenticated) {
                            Text(
                                text = "Sign Out",
                                color = NisError,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clickable(role = Role.Button, onClick = viewModel::signOut)
                                    .padding(8.dp)
                            )
                        } else {
                            Text(
                                text = "Sign In",
                                color = NisAccentPrimary,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable(role = Role.Button, onClick = { showAfdsAuthDialog = true })
                                    .padding(8.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = NisBorder, thickness = 0.5.dp)

                    // Active Profile Card
                    val activeProf = uiState.activeProfile
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NisProfileAvatar(
                            avatarId = activeProf?.avatarId ?: "avatar_01",
                            name = activeProf?.name ?: "Me",
                            size = 48.dp,
                            isKids = activeProf?.isKids == true,
                            isLocked = activeProf?.isLocked == true
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = activeProf?.name ?: "Default",
                                    color = NisTextPrimary,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (activeProf?.isKids == true) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Kids",
                                        color = NisAccentCyan,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Language: ${activeProf?.preferredLanguage ?: "Hindi"} • Country: ${activeProf?.preferredCountry ?: "IN"}",
                                color = NisTextSecondary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    // Profile Actions
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NisSecondaryButton(
                            text = "Switch Profile",
                            onClick = onNavigateToProfiles,
                            modifier = Modifier.weight(1f)
                        )
                        if (uiState.profiles.size < 5) {
                            NisSecondaryButton(
                                text = "Add Profile",
                                onClick = { onNavigateToProfileEdit("new") },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            NisSecondaryButton(
                                text = "Manage",
                                onClick = onNavigateToProfiles,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (uiState.profiles.size <= 1) {
                        Text(
                            text = "Add up to 5 profiles for family members with custom watchlists and recommendations.",
                            color = NisTextMuted,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }

                    HorizontalDivider(color = NisBorder, thickness = 0.5.dp)

                    // Ask who's watching on launch switch
                    val askProfileOnLaunch by app.container.userPreferences.askProfileOnLaunch.collectAsState(initial = uiState.profiles.size > 1)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Ask who's watching on launch",
                                color = NisTextPrimary,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Show profile chooser screen when opening the app",
                                color = NisTextSecondary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = askProfileOnLaunch,
                            onCheckedChange = { checked ->
                                coroutineScope.launch {
                                    app.container.userPreferences.setAskProfileOnLaunch(checked)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = NisAccentPrimary,
                                uncheckedThumbColor = NisTextMuted,
                                uncheckedTrackColor = NisSurface
                            )
                        )
                    }
                }
            }

            // Group 2: Playback & Streaming Preferences
            item {
                SettingsGroup(title = "Playback & Display") {
                    val isKids = uiState.activeProfile?.isKids == true
                    // My Playlists Navigation Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isKids) { onNavigateToPlaylists() }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = NisIcons.Playlists,
                                contentDescription = null,
                                tint = if (isKids) NisTextMuted else NisAccentCyan,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "My Playlists",
                                    color = if (isKids) NisTextMuted else NisTextPrimary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (isKids) "Not available in Kids profile" else "Add and manage custom M3U/M3U8 playlists",
                                    color = NisTextSecondary,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                        if (!isKids) {
                            Icon(
                                imageVector = NisIcons.OpenInNew,
                                contentDescription = "Open",
                                tint = NisTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = NisBorder, thickness = 0.5.dp)

                    // Theme Selection (System / Dark / Light)
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Theme",
                            color = NisTextPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Choose between deep obsidian dark, crisp light, or follow system appearance",
                            color = NisTextSecondary,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppThemeConfig.values().forEach { theme ->
                                val label = when (theme) {
                                    AppThemeConfig.SYSTEM -> "System"
                                    AppThemeConfig.DARK -> "Dark"
                                    AppThemeConfig.LIGHT -> "Light"
                                }
                                NisCategoryChip(
                                    text = label,
                                    isSelected = uiState.appTheme == theme,
                                    onClick = { viewModel.setAppTheme(theme) }
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = NisBorder, thickness = 0.5.dp)

                    // Background Playback Switch
                    SettingsSwitchRow(
                        title = "Background Audio Playback",
                        subtitle = "Continue playing audio when app is in the background",
                        checked = uiState.isBackgroundPlaybackEnabled,
                        onCheckedChange = viewModel::toggleBackgroundPlayback
                    )

                    HorizontalDivider(color = NisBorder, thickness = 0.5.dp)

                    // Picture-in-Picture Switch
                    SettingsSwitchRow(
                        title = "Picture-in-Picture (PiP)",
                        subtitle = "Automatically pop out mini player when switching apps",
                        checked = uiState.isPipEnabled,
                        onCheckedChange = viewModel::togglePip
                    )

                    HorizontalDivider(color = NisBorder, thickness = 0.5.dp)

                    // Autoplay Next Episode Switch
                    SettingsSwitchRow(
                        title = "Autoplay Next Episode",
                        subtitle = "Prompt and play the next downloaded episode when finished",
                        checked = uiState.isAutoplayNextEpisodeEnabled,
                        onCheckedChange = viewModel::toggleAutoplayNextEpisode
                    )

                    HorizontalDivider(color = NisBorder, thickness = 0.5.dp)

                    // Episode Radar Master Switch
                    SettingsSwitchRow(
                        title = "New Episode Alerts",
                        subtitle = "Notify when new episodes of your tracked TV series air",
                        checked = uiState.isEpisodeRadarEnabled,
                        onCheckedChange = viewModel::setEpisodeRadarEnabled
                    )

                    HorizontalDivider(color = NisBorder, thickness = 0.5.dp)

                    // Stream Health Intelligence
                    SettingsSwitchRow(
                        title = "Stream Reliability Dot",
                        subtitle = "Display colored health status dot on live channel cards",
                        checked = uiState.isStreamReliabilityDotEnabled,
                        onCheckedChange = viewModel::setStreamReliabilityDotEnabled
                    )

                    HorizontalDivider(color = NisBorder, thickness = 0.5.dp)

                    SettingsSwitchRow(
                        title = "Hide Dead Channels",
                        subtitle = "Automatically hide channels failing repeatedly for 3+ days",
                        checked = uiState.isHideDeadChannelsEnabled,
                        onCheckedChange = viewModel::setHideDeadChannelsEnabled
                    )

                    HorizontalDivider(color = NisBorder, thickness = 0.5.dp)

                    // Data Saver Suite
                    SettingsSwitchRow(
                        title = "Auto-Listen on Screen Off",
                        subtitle = "Switch live streams to audio-only listen mode when screen is off on cellular",
                        checked = uiState.isAutoListenOnCellularScreenOffEnabled,
                        onCheckedChange = viewModel::setAutoListenOnCellularScreenOffEnabled
                    )

                    HorizontalDivider(color = NisBorder, thickness = 0.5.dp)

                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Cellular Video Quality Cap",
                            color = NisTextPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Limit maximum video stream resolution when on metered cellular data",
                            color = NisTextSecondary,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("OFF", "480P", "720P").forEach { cap ->
                                NisCategoryChip(
                                    text = if (cap == "OFF") "Off (Original)" else cap,
                                    isSelected = uiState.cellularQualityCap.equals(cap, ignoreCase = true),
                                    onClick = { viewModel.setCellularQualityCap(cap) }
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = NisBorder, thickness = 0.5.dp)

                    // Preferred Language Selection
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Primary Language Priority",
                            color = NisTextPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Used to spotlight recommended live channels first",
                            color = NisTextSecondary,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(uiState.availableLanguages) { lang ->
                                NisCategoryChip(
                                    text = lang,
                                    isSelected = lang.equals(uiState.preferredLanguage, ignoreCase = true),
                                    onClick = { viewModel.setPreferredLanguage(lang) }
                                )
                            }
                        }
                    }
                }
            }

            // Group: Privacy & Security
            item {
                SettingsGroup(title = "Privacy & Security") {
                    // History & Privacy Navigation Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToHistoryPrivacy() }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = NisIcons.Info,
                                contentDescription = null,
                                tint = NisAccentCyan,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "History & Privacy",
                                    color = NisTextPrimary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Manage watch history, pause tracking & reset recommendations",
                                    color = NisTextSecondary,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                        Icon(
                            imageVector = NisIcons.OpenInNew,
                            contentDescription = "Open",
                            tint = NisTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    HorizontalDivider(color = NisBorder, thickness = 0.5.dp)

                    val isKidsActive = uiState.activeProfile?.isKids == true

                    // Parental Controls Navigation Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isKidsActive) { onNavigateToParentalControls() }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = NisIcons.Lock,
                                contentDescription = null,
                                tint = if (isKidsActive) NisTextMuted else NisAccentPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Parental Controls",
                                    color = if (isKidsActive) NisTextMuted else NisTextPrimary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (isKidsActive) "Restricted in Kids profile. Switch profile to edit." else "Set security PIN and restrict adult channels & categories",
                                    color = NisTextSecondary,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                        if (!isKidsActive) {
                            Icon(
                                imageVector = NisIcons.OpenInNew,
                                contentDescription = "Open",
                                tint = NisTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = NisBorder, thickness = 0.5.dp)

                    // Backup & Restore Navigation Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isKidsActive) { onNavigateToBackupRestore() }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = NisIcons.Download,
                                contentDescription = null,
                                tint = if (isKidsActive) NisTextMuted else NisLiveEmerald,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Backup & Restore",
                                    color = if (isKidsActive) NisTextMuted else NisTextPrimary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (isKidsActive) "Restricted in Kids profile. Switch profile to edit." else "Export and import favorites, playlists and preferences",
                                    color = NisTextSecondary,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                        if (!isKidsActive) {
                            Icon(
                                imageVector = NisIcons.OpenInNew,
                                contentDescription = "Open",
                                tint = NisTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Group 3: Discovery & Region Preferences
            item {
                SettingsGroup(title = "Discovery & Region") {
                    // Region / Country Priority
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Content Region & Watch Providers",
                            color = NisTextPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Current: ${uiState.preferredCountry} (Used for TMDB trending, release dates, and streaming providers)",
                            color = NisTextSecondary,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(uiState.availableCountries) { country ->
                                NisCategoryChip(
                                    text = country,
                                    isSelected = country.equals(uiState.preferredCountry, ignoreCase = true),
                                    onClick = { viewModel.setPreferredCountry(country) }
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = NisBorder, thickness = 0.5.dp)

                    // TMDB API Key Override
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                tmdbKeyInput = uiState.tmdbApiKeyOverride
                                showTmdbKeyDialog = true
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(text = "TMDB API Key", color = NisTextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(
                                text = if (uiState.tmdbApiKeyOverride.isNotBlank()) "Custom API Key configured" else "Default build key active (tap to customize)",
                                color = NisTextSecondary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Text(text = "Configure", color = NisAccentPrimary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                    }

                    HorizontalDivider(color = NisBorder, thickness = 0.5.dp)

                    // AFDS Cloud Archive Auth & Token
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (uiState.isAfdsAuthenticated) {
                                    afdsTokenInput = uiState.afdsAuthToken
                                    showManualTokenDialog = true
                                } else {
                                    viewModel.resetAfdsAuthFlow()
                                    showAfdsAuthDialog = true
                                }
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (uiState.isAfdsAuthenticated) NisLiveEmerald else NisAccentAmber)
                                )
                                Text(
                                    text = "AFDS Cloud Archive",
                                    color = NisTextPrimary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (uiState.isAfdsAuthenticated) {
                                    val emailPart = uiState.afdsUserEmail.ifBlank { "Connected" }
                                    val idPart = if (uiState.afdsUserId.isNotBlank()) " • ID: ${uiState.afdsUserId}" else ""
                                    "$emailPart$idPart"
                                } else {
                                    "Not connected • Tap to sign in and enable downloads"
                                },
                                color = if (uiState.isAfdsAuthenticated) NisLiveEmerald else NisTextSecondary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        if (uiState.isAfdsAuthenticated) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(
                                    onClick = {
                                        afdsTokenInput = uiState.afdsAuthToken
                                        showManualTokenDialog = true
                                    },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Text("Token", color = NisAccentCyan, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                                }
                                TextButton(
                                    onClick = { viewModel.signOut() },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Text("Sign Out", color = NisLiveRed, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        } else {
                            Text(
                                text = "Sign In",
                                color = NisAccentPrimary,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    HorizontalDivider(color = NisBorder, thickness = 0.5.dp)

                    // NSFW Switch
                    SettingsSwitchRow(
                        title = "Allow Adult Content (NSFW Filter)",
                        subtitle = "Adult content is hidden by default. Turn on to include 18+ streams",
                        checked = uiState.isNsfwEnabled,
                        onCheckedChange = viewModel::toggleNsfw
                    )
                }
            }

            // Group 4: Storage & Data Sync
            item {
                SettingsGroup(title = "Data & Storage") {
                    // AFDS Server Today's Quota & Download Limit
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(text = "AFDS Today's Usage", color = NisTextPrimary, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            if (uiState.isAfdsAuthenticated) {
                                val used = uiState.afdsDailyLinksUsed
                                val limit = uiState.afdsDailyLinksLimit
                                val fraction = if (limit > 0) (used.toFloat() / limit.toFloat()).coerceIn(0f, 1f) else 0f
                                Text(
                                    text = "$used / $limit downloads used today",
                                    color = NisTextSecondary,
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                NisLinearProgressBar(
                                    progress = fraction,
                                    modifier = Modifier
                                        .fillMaxWidth(0.9f)
                                        .height(6.dp)
                                        .clip(NisBadgeShape),
                                    color = when {
                                        fraction >= 1.0f -> NisLiveRed
                                        fraction >= 0.8f -> NisAccentAmber
                                        else -> NisAccentPrimary
                                    },
                                    trackColor = NisSurfaceElevated
                                )
                                if (uiState.afdsDailySendsLimit > 0 && uiState.afdsUserId.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Channel Sends: ${uiState.afdsDailySendsUsed} / ${uiState.afdsDailySendsLimit}",
                                        color = com.nis.nistv.core.designsystem.theme.NisTextMuted,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                Text(
                                    text = "Fetched from AFDS server • Resets midnight UTC",
                                    color = com.nis.nistv.core.designsystem.theme.NisTextMuted,
                                    style = NisTheme.textExtras.micro,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            } else {
                                Text(
                                    text = "Sign in to AFDS to fetch your daily download quota",
                                    color = NisTextSecondary,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }

                        if (uiState.isAfdsAuthenticated) {
                            NisSecondaryButton(
                                text = if (uiState.isAfdsLoading) "Syncing..." else "Refresh",
                                onClick = viewModel::refreshAfdsQuota,
                                enabled = !uiState.isAfdsLoading
                            )
                        } else {
                            NisSecondaryButton(
                                text = "Sign In",
                                onClick = { showAfdsAuthDialog = true }
                            )
                        }
                    }

                    HorizontalDivider(color = NisBorder, thickness = 0.5.dp)

                    // Multi-Threaded Download Concurrency
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Download Acceleration Threads",
                            color = NisTheme.colors.textPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Multi-threaded parallel chunk engine accelerates movie and episode downloads. Higher threads maximize Wi-Fi bandwidth.",
                            color = NisTheme.colors.textSecondary,
                            style = MaterialTheme.typography.labelSmall
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            listOf(1, 2, 4, 6, 8).forEach { threads ->
                                val isSelected = uiState.downloadThreads == threads
                                NisCategoryChip(
                                    text = if (threads == 1) "1 (Single)" else "$threads Threads",
                                    isSelected = isSelected,
                                    onClick = { viewModel.setDownloadThreads(threads) }
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = NisBorder, thickness = 0.5.dp)

                    // Channel Count & Sync Status
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Channel Catalogue", color = NisTextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(
                                text = "${uiState.totalChannelsCount} channels cached locally",
                                color = NisTextSecondary,
                                style = MaterialTheme.typography.labelSmall
                            )
                            if (!uiState.syncStatusMessage.isNullOrBlank()) {
                                Text(
                                    text = uiState.syncStatusMessage ?: "",
                                    color = NisTextMuted,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                        NisSecondaryButton(
                            text = if (uiState.isSyncing) "Syncing..." else "Refresh",
                            onClick = viewModel::triggerCatalogueSync,
                            enabled = !uiState.isSyncing
                        )
                    }

                    HorizontalDivider(color = NisBorder, thickness = 0.5.dp)

                    // Clear Cache Action
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.clearCache() }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Clear Image & Search Cache", color = NisTextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(text = "Frees temporary storage and clears recent queries", color = NisTextSecondary, style = MaterialTheme.typography.labelSmall)
                        }
                        Text(text = "Clear", color = NisAccentPrimary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Group 5: About & Legal Attribution (IPTV-org, TMDB, AFDS)
            item {
                SettingsGroup(title = "About & Legal") {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(bottom = 10.dp)
                        ) {
                            NisLogo(size = 42.dp)
                            Column {
                                Text(
                                    text = "NisTV Revamped",
                                    color = NisTextPrimary,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) • TMDB & Downloads Edition",
                                    color = NisTextSecondary,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }

                        // App Updates & Changelog Actions
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Check for Updates Button
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(NisThumbnailShape)
                                    .background(NisAccentCyan.copy(alpha = 0.12f))
                                    .border(1.dp, NisAccentCyan.copy(alpha = 0.3f), NisThumbnailShape)
                                    .clickable(enabled = !isCheckingUpdate) {
                                        coroutineScope.launch {
                                            isCheckingUpdate = true
                                            val update = updateChecker.checkForUpdate()
                                            isCheckingUpdate = false
                                            if (update != null) {
                                                availableUpdate = update
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "You are on the latest version (v${BuildConfig.VERSION_NAME})",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }
                                    .padding(vertical = 10.dp, horizontal = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (isCheckingUpdate) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            color = NisAccentCyan,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = NisIcons.Refresh,
                                            contentDescription = null,
                                            tint = NisAccentCyan,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                    Text(
                                        text = if (isCheckingUpdate) "Checking..." else "Check for Update",
                                        color = NisAccentCyan,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Changelog Button
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(NisThumbnailShape)
                                    .background(NisSurfaceElevated)
                                    .border(1.dp, NisBorder, NisThumbnailShape)
                                    .clickable { showChangelogDialog = true }
                                    .padding(vertical = 10.dp, horizontal = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = NisIcons.Info,
                                        contentDescription = null,
                                        tint = NisTextPrimary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "Changelog",
                                        color = NisTextPrimary,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // TMDB Required Attribution
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(NisShapes.small)
                                .background(NisSurfaceElevated)
                                .border(1.dp, NisBorder, NisShapes.small)
                                .padding(10.dp)
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(NisBadgeShape)
                                            .background(NisAccentCyan)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "TMDB",
                                            color = Color.White,
                                            style = NisTheme.textExtras.micro,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                    Text(
                                        text = "The Movie Database",
                                        color = NisTextPrimary,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "This product uses the TMDB API but is not endorsed or certified by TMDB.",
                                    color = NisTextSecondary,
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Visit themoviedb.org →",
                                    color = NisAccentCyan,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable {
                                        try {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.themoviedb.org")))
                                        } catch (_: Exception) {}
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // AFDS-Android Attribution
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(NisShapes.small)
                                .background(NisSurfaceElevated)
                                .border(1.dp, NisBorder, NisShapes.small)
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(
                                    text = "AFDS Provider Integration",
                                    color = NisTextPrimary,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Download resolution powered by AFDS-Android (MIT License - Copyright (c) 2024 CloudflareHackers).",
                                    color = NisTextSecondary,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // IPTV-org Attribution
                        Text(
                            text = "Live Stream Notice:\nAll live television channel streams and metadata are indexed from publicly available community-maintained sources via the iptv-org open-source project. NisTV does not host, broadcast, or store any proprietary media streams.",
                            color = NisTextMuted,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // Switch Profile Modal
    if (showProfileDialog) {
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            shape = NisDialogShape,
            containerColor = NisSurface,
            title = {
                Text(text = "Select Profile", color = NisTextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.profiles.forEach { profile ->
                        val isSelected = profile.id == uiState.activeProfile?.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(NisShapes.small)
                                .background(if (isSelected) NisAccentPrimary.copy(alpha = 0.15f) else NisSurfaceElevated)
                                .clickable {
                                    viewModel.switchProfile(profile.id)
                                    showProfileDialog = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = profile.name,
                                color = if (isSelected) NisAccentPrimary else NisTextPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            if (isSelected) {
                                Text(text = "Active", color = NisAccentPrimary, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showProfileDialog = false
                        showNewProfileDialog = true
                    }
                ) {
                    Text("+ Add Profile", color = NisAccentPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showProfileDialog = false }) {
                    Text("Done", color = NisTextSecondary)
                }
            }
        )
    }

    // New Profile Dialog
    if (showNewProfileDialog) {
        AlertDialog(
            onDismissRequest = { showNewProfileDialog = false },
            shape = NisDialogShape,
            containerColor = NisSurface,
            title = {
                Text(text = "Create Profile", color = NisTextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            },
            text = {
                TextField(
                    value = newProfileName,
                    onValueChange = { newProfileName = it },
                    placeholder = { Text("Profile Name (e.g. Family)", color = NisTextMuted) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = NisSurfaceElevated,
                        unfocusedContainerColor = NisSurfaceElevated,
                        focusedTextColor = NisTextPrimary,
                        unfocusedTextColor = NisTextPrimary,
                        focusedIndicatorColor = NisAccentPrimary,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(NisShapes.small)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newProfileName.isNotBlank()) {
                            viewModel.createProfile(newProfileName.trim())
                            showNewProfileDialog = false
                            newProfileName = ""
                        }
                    }
                ) {
                    Text("Create", color = NisAccentPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewProfileDialog = false }) {
                    Text("Cancel", color = NisTextSecondary)
                }
            }
        )
    }

    // TMDB API Key Dialog
    if (showTmdbKeyDialog) {
        AlertDialog(
            onDismissRequest = { showTmdbKeyDialog = false },
            shape = NisDialogShape,
            containerColor = NisSurface,
            title = {
                Text(text = "Custom TMDB API Key", color = NisTextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter your v3 TMDB API Key to power the Discover section. Leave blank to use build default.",
                        color = NisTextSecondary,
                        style = MaterialTheme.typography.labelSmall
                    )
                    TextField(
                        value = tmdbKeyInput,
                        onValueChange = { tmdbKeyInput = it },
                        placeholder = { Text("Paste TMDB API Key", color = NisTextMuted) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = NisSurfaceElevated,
                            unfocusedContainerColor = NisSurfaceElevated,
                            focusedTextColor = NisTextPrimary,
                            unfocusedTextColor = NisTextPrimary,
                            focusedIndicatorColor = NisAccentPrimary,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth().clip(NisShapes.small)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setTmdbApiKeyOverride(tmdbKeyInput)
                        showTmdbKeyDialog = false
                    }
                ) {
                    Text("Save", color = NisAccentPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTmdbKeyDialog = false }) {
                    Text("Cancel", color = NisTextSecondary)
                }
            }
        )
    }

    // Unified AFDS Auth Gate Dialog
    if (showAfdsAuthDialog) {
        AfdsAuthGateDialog(
            afdsAuthService = app.container.afdsAuthService,
            googleAuthManager = app.container.googleAuthManager,
            initialEmail = uiState.afdsUserEmail,
            onSuccess = {
                showAfdsAuthDialog = false
                viewModel.refreshAfdsQuota()
            },
            onDismiss = {
                showAfdsAuthDialog = false
            }
        )
    }

    // Manual AFDS Token Dialog
    if (showManualTokenDialog) {
        AlertDialog(
            onDismissRequest = { showManualTokenDialog = false },
            shape = NisDialogShape,
            containerColor = NisSurface,
            title = {
                Text(text = "AFDS Provider Token", color = NisTextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter your AFDS authorization token to resolve download links for personal use:",
                        color = NisTextSecondary,
                        style = MaterialTheme.typography.labelSmall
                    )
                    TextField(
                        value = afdsTokenInput,
                        onValueChange = { afdsTokenInput = it },
                        placeholder = { Text("Bearer token...", color = NisTextMuted) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = NisSurfaceElevated,
                            unfocusedContainerColor = NisSurfaceElevated,
                            focusedTextColor = NisTextPrimary,
                            unfocusedTextColor = NisTextPrimary,
                            focusedIndicatorColor = NisAccentPrimary,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth().clip(NisShapes.small)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setAfdsAuthToken(afdsTokenInput)
                        showManualTokenDialog = false
                    }
                ) {
                    Text("Save", color = NisAccentPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualTokenDialog = false }) {
                    Text("Cancel", color = NisTextSecondary)
                }
            }
        )
    }

    // Full Changelog Dialog
    if (showChangelogDialog) {
        ChangelogDialog(
            onDismiss = { showChangelogDialog = false }
        )
    }

    // App Update Dialog
    availableUpdate?.let { updateInfo ->
        AppUpdateDialog(
            updateInfo = updateInfo,
            onUpdate = {
                updateChecker.startDownloadAndInstall(context, updateInfo)
            },
            onDismiss = {
                availableUpdate = null
            }
        )
    }
}

@Composable
fun SettingsGroup(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title.uppercase(),
            color = NisTheme.colors.primary,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(NisChannelCardShape)
                .background(NisTheme.colors.surface)
                .border(1.dp, NisTheme.colors.border, NisChannelCardShape)
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                color = NisTheme.colors.textPrimary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = NisTheme.colors.textSecondary,
                style = MaterialTheme.typography.labelSmall
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = NisTheme.colors.background,
                checkedTrackColor = NisTheme.colors.primary,
                uncheckedThumbColor = NisTheme.colors.textSecondary,
                uncheckedTrackColor = NisTheme.colors.surfaceElevated,
                uncheckedBorderColor = NisTheme.colors.border
            )
        )
    }
}
