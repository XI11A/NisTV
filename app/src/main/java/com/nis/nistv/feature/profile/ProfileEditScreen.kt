package com.nis.nistv.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nis.nistv.NisTvApplication
import com.nis.nistv.core.designsystem.component.NisIconButton
import com.nis.nistv.core.designsystem.component.NisPrimaryButton
import com.nis.nistv.core.designsystem.component.NisProfileAvatar
import com.nis.nistv.core.designsystem.component.NisSecondaryButton
import com.nis.nistv.core.designsystem.icon.NisIcons
import com.nis.nistv.core.designsystem.theme.NisAccentPrimary
import com.nis.nistv.core.designsystem.theme.NisChannelCardShape
import com.nis.nistv.core.designsystem.theme.NisChipShape
import com.nis.nistv.core.designsystem.theme.NisDialogShape
import com.nis.nistv.core.designsystem.theme.NisDimens
import com.nis.nistv.core.designsystem.theme.NisTheme
import com.nis.nistv.core.security.KidsContentFilter
import com.nis.nistv.feature.parental.ParentalPinDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileEditScreen(
    profileId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileEditViewModel = viewModel(
        factory = run {
            val app = LocalContext.current.applicationContext as NisTvApplication
            ProfileEditViewModel.provideFactory(
                profileId = profileId,
                profileRepository = app.container.profileRepository,
                userPreferences = app.container.userPreferences
            )
        }
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var pinDialogTitle by remember { mutableStateOf("Enter Parental PIN") }
    var pinDialogSubtitle by remember { mutableStateOf("Enter your PIN to verify this change.") }
    var pinErrorMessage by remember { mutableStateOf<String?>(null) }
    var pendingPinAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showPinNotSetNotice by remember { mutableStateOf(false) }

    val languages = listOf("Hindi", "Tamil", "Telugu", "Malayalam", "Bengali", "Kannada", "Marathi", "Gujarati", "Punjabi", "English")
    val countries = listOf("IN", "US", "GB", "CA", "AU", "DE", "FR")
    val avatarIds = (1..12).map { "avatar_%02d".format(it) }

    val onToggleLock: (Boolean) -> Unit = { shouldLock ->
        if (shouldLock) {
            if (!uiState.isParentalPinSet) {
                showPinNotSetNotice = true
            } else {
                pinDialogTitle = "Lock Profile"
                pinDialogSubtitle = "Enter your Parental PIN to enable lock on this profile."
                pinErrorMessage = null
                pendingPinAction = { viewModel.setLocked(true) }
                showPinDialog = true
            }
        } else {
            if (uiState.isParentalPinSet) {
                pinDialogTitle = "Unlock Profile"
                pinDialogSubtitle = "Enter your Parental PIN to remove lock from this profile."
                pinErrorMessage = null
                pendingPinAction = { viewModel.setLocked(false) }
                showPinDialog = true
            } else {
                viewModel.setLocked(false)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NisTheme.colors.background)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NisIconButton(
                    icon = NisIcons.Back,
                    contentDescription = "Back",
                    onClick = onBack
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (uiState.isNew) "New Profile" else "Edit Profile",
                    style = MaterialTheme.typography.titleLarge,
                    color = NisTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            // Avatar Preview & Picker
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                NisProfileAvatar(
                    avatarId = uiState.avatarId,
                    name = uiState.name.ifBlank { "Nis" },
                    size = 80.dp,
                    isKids = uiState.isKids,
                    isLocked = uiState.isLocked
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Choose Avatar",
                    style = MaterialTheme.typography.titleSmall,
                    color = NisTheme.colors.textSecondary
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(avatarIds, key = { it }) { avId ->
                        val isSelected = avId == uiState.avatarId
                        Box(
                            modifier = Modifier
                                .clickable(
                                    role = Role.Button,
                                    onClick = { viewModel.setAvatarId(avId) },
                                    onClickLabel = "Select avatar $avId"
                                )
                                .padding(2.dp)
                        ) {
                            NisProfileAvatar(
                                avatarId = avId,
                                size = 52.dp,
                                isSelected = isSelected
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Profile Name Input
            Text(
                text = "Profile Name",
                style = MaterialTheme.typography.titleSmall,
                color = NisTheme.colors.textPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::setName,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = uiState.nameError != null,
                supportingText = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = uiState.nameError ?: "",
                            color = NisTheme.colors.primary,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "${uiState.name.length}/16",
                            color = NisTheme.colors.textMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NisTheme.colors.primary,
                    unfocusedBorderColor = NisTheme.colors.borderSubtle,
                    focusedTextColor = NisTheme.colors.textPrimary,
                    unfocusedTextColor = NisTheme.colors.textPrimary,
                    focusedContainerColor = NisTheme.colors.surfaceElevated,
                    unfocusedContainerColor = NisTheme.colors.surfaceElevated
                ),
                shape = NisChannelCardShape
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Preferred Language
            Text(
                text = "Preferred Language",
                style = MaterialTheme.typography.titleSmall,
                color = NisTheme.colors.textPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                languages.forEach { lang ->
                    val isSelected = lang.equals(uiState.preferredLanguage, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .clip(NisChipShape)
                            .background(
                                if (isSelected) NisTheme.colors.primary else NisTheme.colors.surfaceElevated
                            )
                            .border(
                                1.dp,
                                if (isSelected) NisTheme.colors.primary else NisTheme.colors.borderSubtle,
                                NisChipShape
                            )
                            .clickable(
                                role = Role.Button,
                                onClick = { viewModel.setPreferredLanguage(lang) }
                            )
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = lang,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else NisTheme.colors.textPrimary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Preferred Country
            Text(
                text = "Country / Region",
                style = MaterialTheme.typography.titleSmall,
                color = NisTheme.colors.textPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                countries.forEach { cCode ->
                    val isSelected = cCode.equals(uiState.preferredCountry, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .clip(NisChipShape)
                            .background(
                                if (isSelected) NisTheme.colors.primary else NisTheme.colors.surfaceElevated
                            )
                            .border(
                                1.dp,
                                if (isSelected) NisTheme.colors.primary else NisTheme.colors.borderSubtle,
                                NisChipShape
                            )
                            .clickable(
                                role = Role.Button,
                                onClick = { viewModel.setPreferredCountry(cCode) }
                            )
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = cCode,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else NisTheme.colors.textPrimary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Kids Profile Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(NisChannelCardShape)
                    .background(NisTheme.colors.surfaceElevated)
                    .border(1.dp, NisTheme.colors.borderSubtle, NisChannelCardShape)
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Kids Profile",
                                style = MaterialTheme.typography.titleMedium,
                                color = NisTheme.colors.textPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = KidsContentFilter.KIDS_HELP_TEXT,
                                style = MaterialTheme.typography.bodySmall,
                                color = NisTheme.colors.textSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = uiState.isKids,
                            onCheckedChange = viewModel::setKids,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = NisTheme.colors.primary,
                                uncheckedThumbColor = NisTheme.colors.textMuted,
                                uncheckedTrackColor = NisTheme.colors.surface
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Lock Profile Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(NisChannelCardShape)
                    .background(NisTheme.colors.surfaceElevated)
                    .border(1.dp, NisTheme.colors.borderSubtle, NisChannelCardShape)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Lock Profile",
                            style = MaterialTheme.typography.titleMedium,
                            color = NisTheme.colors.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (uiState.isParentalPinSet) {
                                "Require your 4-6 digit Parental PIN to open this profile."
                            } else {
                                "Parental PIN must be created to lock this profile."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = NisTheme.colors.textSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = uiState.isLocked,
                        onCheckedChange = onToggleLock,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = NisTheme.colors.primary,
                            uncheckedThumbColor = NisTheme.colors.textMuted,
                            uncheckedTrackColor = NisTheme.colors.surface
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Error display
            uiState.error?.let { err ->
                Text(
                    text = err,
                    color = NisTheme.colors.primary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // Save Action Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NisPrimaryButton(
                    text = if (uiState.isSaving) "Saving..." else "Save",
                    onClick = { viewModel.save(onSuccess = onBack) },
                    enabled = !uiState.isSaving && !uiState.isDeleting,
                    modifier = Modifier.weight(1f)
                )
            }

            // Delete Action Button (if applicable)
            if (!uiState.isNew && uiState.totalProfilesCount > 1) {
                Spacer(modifier = Modifier.height(16.dp))
                NisSecondaryButton(
                    text = "Delete Profile",
                    icon = NisIcons.Delete,
                    onClick = { showDeleteConfirmDialog = true },
                    enabled = !uiState.isSaving && !uiState.isDeleting,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            shape = NisDialogShape,
            containerColor = NisTheme.colors.surface,
            icon = {
                Icon(
                    imageVector = NisIcons.Delete,
                    contentDescription = null,
                    tint = NisAccentPrimary,
                    modifier = Modifier.size(NisDimens.IconLarge)
                )
            },
            title = {
                Text(
                    text = "Delete Profile?",
                    style = MaterialTheme.typography.titleMedium,
                    color = NisTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Deleting \"${uiState.name}\" will permanently delete this profile and all its favorites, watch later items, watch history, continue watching progress, and recommendations. This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NisTheme.colors.textSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.delete(onSuccess = onBack)
                    }
                ) {
                    Text(
                        text = "Delete",
                        color = NisAccentPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(
                        text = "Cancel",
                        color = NisTheme.colors.textMuted
                    )
                }
            }
        )
    }

    // Parental PIN Dialog for lock toggle
    if (showPinDialog) {
        ParentalPinDialog(
            title = pinDialogTitle,
            subtitle = pinDialogSubtitle,
            errorMessage = pinErrorMessage,
            onDismissRequest = {
                showPinDialog = false
                pendingPinAction = null
                pinErrorMessage = null
            },
            onConfirm = { enteredPin ->
                scope.launch {
                    val valid = viewModel.verifyPin(enteredPin)
                    if (valid) {
                        showPinDialog = false
                        val act = pendingPinAction
                        pendingPinAction = null
                        pinErrorMessage = null
                        act?.invoke()
                    } else {
                        pinErrorMessage = "Incorrect PIN. Please try again."
                    }
                }
            },
            onForgotPinClick = {
                pinErrorMessage = "Reset PIN in Parental Controls (Settings)"
            }
        )
    }

    // PIN Not Set Notice Dialog
    if (showPinNotSetNotice) {
        AlertDialog(
            onDismissRequest = { showPinNotSetNotice = false },
            shape = NisDialogShape,
            containerColor = NisTheme.colors.surface,
            title = {
                Text(
                    text = "Parental PIN Required",
                    style = MaterialTheme.typography.titleMedium,
                    color = NisTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "To lock a profile, you must first create a Parental PIN in Settings > Parental Controls.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NisTheme.colors.textSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { showPinNotSetNotice = false }) {
                    Text(
                        text = "OK",
                        color = NisAccentPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }
}
