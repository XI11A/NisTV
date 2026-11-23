package com.nis.nistv.feature.parental

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nis.nistv.core.designsystem.component.NisPrimaryButton
import com.nis.nistv.core.designsystem.icon.NisIcons
import com.nis.nistv.core.designsystem.theme.*
import com.nis.nistv.core.designsystem.theme.NisTextPrimary
import com.nis.nistv.core.designsystem.theme.NisTextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ParentalControlsScreen(
    viewModel: ParentalControlsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showSetPinDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showDisableConfirmDialog by remember { mutableStateOf(false) }
    var showNoLockExplanation by remember { mutableStateOf(false) }

    val keyguardManager = remember {
        context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
    }

    val credentialLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.resetPinViaDeviceCredential()
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
                title = { Text("Parental Controls", color = NisTheme.colors.textPrimary, fontWeight = FontWeight.Bold) },
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
            // 1. Status Banner
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
                            NisIcons.Lock,
                            contentDescription = null,
                            tint = if (uiState.isParentalEnabled) NisAccentPrimary else NisTextMuted,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                if (uiState.isParentalEnabled) "Parental Protection Active" else "Parental Controls Inactive",
                                color = NisTextPrimary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                if (uiState.isParentalEnabled)
                                    "Adult channels and selected categories are locked with your PIN."
                                else
                                    "Set a PIN to restrict adult channels and lock specific Live TV categories.",
                                color = NisTextMuted,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            // 2. PIN Management Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = NisCardBackground),
                    shape = NisShapes.medium
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "PIN Security",
                            color = NisTextPrimary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (!uiState.hasPinSet) {
                            Text(
                                "No parental PIN has been configured yet.",
                                color = NisTextMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            NisPrimaryButton(
                                text = "Set Parental PIN",
                                onClick = { showSetPinDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "PIN Protection",
                                    color = NisTextSecondary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Row {
                                    TextButton(onClick = { showChangePinDialog = true }) {
                                        Text("Change PIN", color = NisAccentPrimary)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    TextButton(onClick = { showDisableConfirmDialog = true }) {
                                        Text("Disable", color = NisTextMuted)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Category Locks Card (Only when PIN is configured)
            if (uiState.hasPinSet) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = NisCardBackground),
                        shape = NisShapes.medium
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Restricted Categories",
                                color = NisTextPrimary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Select categories to hide from Live TV and Search. Channels flagged as NSFW are always protected.",
                                color = NisTextMuted,
                                style = MaterialTheme.typography.labelSmall
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                uiState.availableCategories.forEach { category ->
                                    val isLocked = uiState.lockedCategories.contains(category)
                                    FilterChip(
                                        selected = isLocked,
                                        onClick = { viewModel.toggleCategoryLock(category) },
                                        label = {
                                            Text(
                                                category,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isLocked) NisAccentPrimary else NisTextSecondary
                                            )
                                        },
                                        leadingIcon = if (isLocked) {
                                            { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp), tint = NisAccentPrimary) }
                                        } else null,
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = NisAccentPrimary.copy(alpha = 0.15f),
                                            containerColor = NisSurface
                                        )
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

    // Dialog: Set Initial PIN
    if (showSetPinDialog) {
        var newPin by remember { mutableStateOf("") }
        var confirmPin by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showSetPinDialog = false },
            shape = NisDialogShape,
            containerColor = NisSurface,
            title = { Text("Create Parental PIN", color = NisTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter a 4 to 6 digit security PIN.", color = NisTextSecondary, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) newPin = it },
                        label = { Text("4-6 Digit PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NisAccentPrimary,
                            unfocusedBorderColor = NisTextMuted,
                            focusedTextColor = NisTextPrimary,
                            unfocusedTextColor = NisTextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) confirmPin = it },
                        label = { Text("Confirm PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NisAccentPrimary,
                            unfocusedBorderColor = NisTextMuted,
                            focusedTextColor = NisTextPrimary,
                            unfocusedTextColor = NisTextPrimary
                        )
                    )
                    error?.let {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(it, color = NisAccentPrimary, style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPin.length !in 4..6) {
                            error = "PIN must be 4 to 6 digits"
                        } else if (newPin != confirmPin) {
                            error = "PINs do not match"
                        } else {
                            viewModel.setInitialPin(newPin)
                            showSetPinDialog = false
                        }
                    }
                ) {
                    Text("Save PIN", color = NisAccentPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSetPinDialog = false }) {
                    Text("Cancel", color = NisTextMuted)
                }
            }
        )
    }

    // Dialog: Change PIN
    if (showChangePinDialog) {
        var currentPin by remember { mutableStateOf("") }
        var newPin by remember { mutableStateOf("") }
        var confirmPin by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showChangePinDialog = false },
            shape = NisDialogShape,
            containerColor = NisSurface,
            title = { Text("Change Parental PIN", color = NisTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = currentPin,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) currentPin = it },
                        label = { Text("Current PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NisAccentPrimary,
                            unfocusedBorderColor = NisTextMuted,
                            focusedTextColor = NisTextPrimary,
                            unfocusedTextColor = NisTextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) newPin = it },
                        label = { Text("New 4-6 Digit PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NisAccentPrimary,
                            unfocusedBorderColor = NisTextMuted,
                            focusedTextColor = NisTextPrimary,
                            unfocusedTextColor = NisTextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) confirmPin = it },
                        label = { Text("Confirm New PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NisAccentPrimary,
                            unfocusedBorderColor = NisTextMuted,
                            focusedTextColor = NisTextPrimary,
                            unfocusedTextColor = NisTextPrimary
                        )
                    )
                    error?.let {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(it, color = NisAccentPrimary, style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPin.length !in 4..6) {
                            error = "New PIN must be 4 to 6 digits"
                        } else if (newPin != confirmPin) {
                            error = "New PINs do not match"
                        } else {
                            viewModel.changePin(currentPin, newPin)
                            showChangePinDialog = false
                        }
                    }
                ) {
                    Text("Change", color = NisAccentPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePinDialog = false }) {
                    Text("Cancel", color = NisTextMuted)
                }
            }
        )
    }

    // Dialog: Disable Parental Controls
    if (showDisableConfirmDialog) {
        var pin by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showDisableConfirmDialog = false },
            shape = NisDialogShape,
            containerColor = NisSurface,
            title = { Text("Disable Parental Controls?", color = NisTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter your current PIN to turn off all parental restrictions.", color = NisTextSecondary, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) pin = it },
                        label = { Text("Current PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NisAccentPrimary,
                            unfocusedBorderColor = NisTextMuted,
                            focusedTextColor = NisTextPrimary,
                            unfocusedTextColor = NisTextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.disableParentalControls(pin)
                        showDisableConfirmDialog = false
                    }
                ) {
                    Text("Disable", color = NisAccentPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisableConfirmDialog = false }) {
                    Text("Cancel", color = NisTextMuted)
                }
            }
        )
    }

    if (showNoLockExplanation) {
        AlertDialog(
            onDismissRequest = { showNoLockExplanation = false },
            shape = NisDialogShape,
            containerColor = NisSurface,
            title = { Text("No Device Screen Lock", color = NisTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Your device does not have a secure screen lock (PIN, pattern, or biometric) set up. Please configure a screen lock in your Android Device Settings to enable device credential verification.",
                    color = NisTextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                TextButton(onClick = { showNoLockExplanation = false }) {
                    Text("OK", color = NisAccentPrimary)
                }
            }
        )
    }
}
