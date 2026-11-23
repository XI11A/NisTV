package com.nis.nistv.feature.home

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import com.nis.nistv.NisTvApplication
import com.nis.nistv.core.database.entity.ProfileEntity
import com.nis.nistv.core.designsystem.component.NisPrimaryButton
import com.nis.nistv.core.designsystem.component.NisProfileAvatar
import com.nis.nistv.core.designsystem.component.NisSecondaryButton
import com.nis.nistv.core.designsystem.icon.NisIcons
import com.nis.nistv.core.designsystem.theme.NisBottomSheetShape
import com.nis.nistv.core.designsystem.theme.NisChannelCardShape
import com.nis.nistv.core.designsystem.theme.NisDimens
import com.nis.nistv.core.designsystem.theme.NisTheme
import com.nis.nistv.core.security.ParentalPinHelper
import com.nis.nistv.feature.parental.ParentalPinDialog
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeProfileSwitcherSheet(
    profiles: List<ProfileEntity>,
    activeProfile: ProfileEntity?,
    onSwitchProfile: (ProfileEntity) -> Unit,
    onManageProfiles: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val app = LocalContext.current.applicationContext as NisTvApplication
    val scope = rememberCoroutineScope()

    var showPinDialog by remember { mutableStateOf(false) }
    var pinTitle by remember { mutableStateOf("Enter Parental PIN") }
    var pinErrorMessage by remember { mutableStateOf<String?>(null) }
    var pendingSwitchProfile by remember { mutableStateOf<ProfileEntity?>(null) }

    val handleProfileClick: (ProfileEntity) -> Unit = { target ->
        if (target.id == activeProfile?.id) {
            onDismiss()
        } else {
            scope.launch {
                val pinHash = app.container.userPreferences.parentalPinHash.firstOrNull().orEmpty()
                val currentIsKids = activeProfile?.isKids == true
                val targetIsLocked = target.isLocked

                if (pinHash.isNotBlank() && (currentIsKids || targetIsLocked)) {
                    pinTitle = if (currentIsKids) "Enter PIN to leave Kids profile" else "Enter PIN to open ${target.name}"
                    pinErrorMessage = null
                    pendingSwitchProfile = target
                    showPinDialog = true
                } else {
                    onSwitchProfile(target)
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NisTheme.colors.surface,
        shape = NisBottomSheetShape,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = NisTheme.colors.borderSubtle)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Switch Profile",
                    style = MaterialTheme.typography.titleMedium,
                    color = NisTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "${profiles.size}/5 profiles",
                    style = MaterialTheme.typography.bodySmall,
                    color = NisTheme.colors.textMuted
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(profiles, key = { it.id }) { prof ->
                    val isActive = prof.id == activeProfile?.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(NisChannelCardShape)
                            .background(
                                if (isActive) NisTheme.colors.surfaceElevated else NisTheme.colors.surface
                            )
                            .border(
                                1.dp,
                                if (isActive) NisTheme.colors.primary else NisTheme.colors.borderSubtle,
                                NisChannelCardShape
                            )
                            .clickable(
                                role = Role.Button,
                                onClick = { handleProfileClick(prof) }
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NisProfileAvatar(
                            avatarId = prof.avatarId,
                            name = prof.name,
                            size = 48.dp,
                            isKids = prof.isKids,
                            isLocked = prof.isLocked,
                            isSelected = isActive
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = prof.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isActive) NisTheme.colors.primary else NisTheme.colors.textPrimary,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                            )

                            if (prof.isKids) {
                                Text(
                                    text = "Kids Profile",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NisTheme.colors.textSecondary
                                )
                            }
                        }

                        if (isActive) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(NisTheme.colors.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = NisIcons.Check,
                                    contentDescription = "Active Profile",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NisSecondaryButton(
                    text = "Manage Profiles",
                    onClick = onManageProfiles,
                    modifier = Modifier.weight(1f)
                )
                NisSecondaryButton(
                    text = "Settings",
                    onClick = onOpenSettings,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (showPinDialog) {
        ParentalPinDialog(
            title = pinTitle,
            subtitle = "Please enter your 4-6 digit PIN to switch to this profile.",
            errorMessage = pinErrorMessage,
            onDismissRequest = {
                showPinDialog = false
                pendingSwitchProfile = null
                pinErrorMessage = null
            },
            onConfirm = { enteredPin ->
                scope.launch {
                    val hash = app.container.userPreferences.parentalPinHash.firstOrNull().orEmpty()
                    val salt = app.container.userPreferences.parentalPinSalt.firstOrNull().orEmpty()
                    if (ParentalPinHelper.verifyPin(enteredPin, hash, salt)) {
                        showPinDialog = false
                        val target = pendingSwitchProfile
                        pendingSwitchProfile = null
                        pinErrorMessage = null
                        if (target != null) {
                            onSwitchProfile(target)
                        }
                    } else {
                        pinErrorMessage = "Incorrect PIN. Please try again."
                    }
                }
            },
            onForgotPinClick = {
                pinErrorMessage = "Reset PIN from Parental Controls in Settings"
            }
        )
    }
}
