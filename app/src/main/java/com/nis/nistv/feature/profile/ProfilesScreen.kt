package com.nis.nistv.feature.profile

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nis.nistv.NisTvApplication
import com.nis.nistv.core.database.entity.ProfileEntity
import com.nis.nistv.core.designsystem.component.NisPrimaryButton
import com.nis.nistv.core.designsystem.component.NisSecondaryButton
import com.nis.nistv.core.designsystem.component.NisProfileAvatar
import com.nis.nistv.core.designsystem.icon.NisIcons
import com.nis.nistv.core.designsystem.theme.NisDimens
import com.nis.nistv.core.designsystem.theme.NisMotion
import com.nis.nistv.core.designsystem.theme.NisTheme
import com.nis.nistv.core.designsystem.theme.isReducedMotionEnabled
import com.nis.nistv.feature.parental.ParentalPinDialog
import kotlinx.coroutines.launch

@Composable
fun ProfilesScreen(
    fromLaunch: Boolean = false,
    onProfileSelected: (ProfileEntity) -> Unit,
    onEditProfile: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfilesViewModel = viewModel(
        factory = run {
            val app = LocalContext.current.applicationContext as NisTvApplication
            ProfilesViewModel.provideFactory(
                profileRepository = app.container.profileRepository,
                parentalControlManager = app.container.parentalControlManager,
                userPreferences = app.container.userPreferences
            )
        }
    )
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val reducedMotion = remember(context) { isReducedMotionEnabled(context) }
    val scope = rememberCoroutineScope()

    // Handle back press
    BackHandler {
        if (fromLaunch) {
            (context as? Activity)?.finish()
        } else {
            onBack()
        }
    }

    var pendingTargetProfile by remember { mutableStateOf<ProfileEntity?>(null) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showPinDialog by remember { mutableStateOf(false) }
    var pinDialogTitle by remember { mutableStateOf("Enter Parental PIN") }
    var pinErrorMessage by remember { mutableStateOf<String?>(null) }

    val verifyAndExecute: (title: String, action: () -> Unit) -> Unit = { title, action ->
        if (uiState.isParentalPinSet) {
            pinDialogTitle = title
            pinErrorMessage = null
            pendingAction = action
            showPinDialog = true
        } else {
            action()
        }
    }

    val onProfileClick: (ProfileEntity) -> Unit = { profile ->
        if (uiState.isManageMode) {
            // Edit profile clicked
            if (uiState.activeProfile?.isKids == true) {
                verifyAndExecute("Enter PIN to edit profiles") {
                    onEditProfile(profile.id)
                }
            } else {
                onEditProfile(profile.id)
            }
        } else {
            // Switch profile clicked
            val currentIsKids = uiState.activeProfile?.isKids == true
            val targetIsLocked = profile.isLocked

            if (currentIsKids && profile.id != uiState.activeProfile?.id) {
                verifyAndExecute("Enter PIN to leave Kids profile") {
                    if (targetIsLocked) {
                        verifyAndExecute("Enter PIN to open ${profile.name}") {
                            viewModel.switchProfile(profile.id) {
                                onProfileSelected(profile)
                            }
                        }
                    } else {
                        viewModel.switchProfile(profile.id) {
                            onProfileSelected(profile)
                        }
                    }
                }
            } else if (targetIsLocked && profile.id != uiState.activeProfile?.id) {
                verifyAndExecute("Enter PIN to open ${profile.name}") {
                    viewModel.switchProfile(profile.id) {
                        onProfileSelected(profile)
                    }
                }
            } else {
                viewModel.switchProfile(profile.id) {
                    onProfileSelected(profile)
                }
            }
        }
    }

    val onAddClick: () -> Unit = {
        if (uiState.activeProfile?.isKids == true) {
            verifyAndExecute("Enter PIN to add a profile") {
                onEditProfile("new")
            }
        } else {
            onEditProfile("new")
        }
    }

    val onToggleManage: () -> Unit = {
        if (uiState.activeProfile?.isKids == true && !uiState.isManageMode) {
            verifyAndExecute("Enter PIN to manage profiles") {
                viewModel.toggleManageMode()
            }
        } else {
            viewModel.toggleManageMode()
        }
    }

    // Scale animation on enter
    val scaleAnim by animateFloatAsState(
        targetValue = 1f,
        animationSpec = if (reducedMotion) tween(0) else tween(
            durationMillis = NisMotion.DurationNormalMs,
            easing = NisMotion.EasingEmphasized
        ),
        label = "profiles_scale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NisTheme.colors.background)
            .statusBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .scale(scaleAnim),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (uiState.isManageMode) "Manage Profiles" else "Who's watching?",
                style = MaterialTheme.typography.headlineMedium,
                color = NisTheme.colors.textPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Profiles Grid
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 110.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(uiState.profiles, key = { it.id }) { profile ->
                    ProfileGridTile(
                        profile = profile,
                        isActive = profile.id == uiState.activeProfile?.id,
                        isManageMode = uiState.isManageMode,
                        onClick = { onProfileClick(profile) }
                    )
                }

                if (uiState.profiles.size < 5) {
                    item(key = "add_profile_tile") {
                        AddProfileGridTile(
                            onClick = onAddClick
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Manage Profiles Action Button
            if (uiState.isManageMode) {
                NisPrimaryButton(
                    text = "Done",
                    onClick = onToggleManage
                )
            } else {
                NisSecondaryButton(
                    text = "Manage Profiles",
                    onClick = onToggleManage
                )
            }
        }
    }

    if (showPinDialog) {
        ParentalPinDialog(
            title = pinDialogTitle,
            subtitle = "Please enter your 4-6 digit PIN to proceed.",
            errorMessage = pinErrorMessage,
            onDismissRequest = {
                showPinDialog = false
                pendingAction = null
                pinErrorMessage = null
            },
            onConfirm = { enteredPin ->
                scope.launch {
                    val valid = viewModel.verifyPin(enteredPin)
                    if (valid) {
                        showPinDialog = false
                        val act = pendingAction
                        pendingAction = null
                        pinErrorMessage = null
                        act?.invoke()
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

@Composable
private fun ProfileGridTile(
    profile: ProfileEntity,
    isActive: Boolean,
    isManageMode: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(
                role = Role.Button,
                onClick = onClick,
                onClickLabel = if (isManageMode) "Edit ${profile.name}" else "Select ${profile.name}"
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            NisProfileAvatar(
                avatarId = profile.avatarId,
                name = profile.name,
                size = 96.dp,
                isKids = profile.isKids,
                isLocked = profile.isLocked,
                isSelected = isActive,
                isEditMode = isManageMode
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = profile.name,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isActive) NisTheme.colors.primary else NisTheme.colors.textPrimary,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AddProfileGridTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(
                role = Role.Button,
                onClick = onClick,
                onClickLabel = "Add Profile"
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(NisTheme.colors.surfaceElevated)
                .border(2.dp, NisTheme.colors.borderSubtle, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = NisIcons.Add,
                contentDescription = "Add Profile",
                tint = NisTheme.colors.textMuted,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Add Profile",
            style = MaterialTheme.typography.bodyLarge,
            color = NisTheme.colors.textMuted,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
