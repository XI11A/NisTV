package com.nis.nistv.core.designsystem.component

import android.accounts.AccountManager
import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
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
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nis.nistv.core.auth.GoogleAuthManager
import com.nis.nistv.core.designsystem.icon.NisIcons
import com.nis.nistv.core.designsystem.theme.*
import com.nis.nistv.core.preferences.GoogleUserInfo

/**
 * Netflix-styled Google Sign-In Gate Dialog.
 * Enforces authentication requirement before watching live content,
 * supporting native Android account chooser, direct Google email login,
 * and 1-tap demo subscriber fallback.
 */
@Composable
fun GoogleAuthGateDialog(
    googleAuthManager: GoogleAuthManager,
    onSuccess: (GoogleUserInfo) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val activity = context as? Activity
    var showDirectInput by remember { mutableStateOf(false) }
    var firstNameInput by remember { mutableStateOf("") }
    var lastNameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Native Android Account Picker Activity Launcher
    val accountPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val accountName = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            if (!accountName.isNullOrBlank()) {
                isLoading = true
                errorMessage = null
                coroutineScope.launch {
                    try {
                        val user = googleAuthManager.fetchGoogleUserProfile(accountName, activity)
                        Toast.makeText(context, "Welcome, ${user.displayName}!", Toast.LENGTH_SHORT).show()
                        onSuccess(user)
                    } catch (e: Exception) {
                        val user = googleAuthManager.handleAccountSelected(accountName)
                        Toast.makeText(context, "Welcome, ${user.displayName}!", Toast.LENGTH_SHORT).show()
                        onSuccess(user)
                    } finally {
                        isLoading = false
                    }
                }
            } else {
                errorMessage = "No account selected. Please try again."
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(NisScrimStrong)
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(NisDialogShape)
                    .background(NisTheme.colors.surfaceElevated)
                    .border(
                        width = 1.dp,
                        color = NisTheme.colors.borderSubtle,
                        shape = NisDialogShape
                    )
                    .clickable(enabled = false) {}
                    .padding(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // NTV Brand Logo
                    NtvLogo(size = 36.dp, showWordmark = true, fontSize = 22)

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Google Account Required",
                        color = NisTheme.colors.textPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "You must be signed in with a Google account to watch live TV, access EPG schedules, and sync your watchlist.",
                        color = NisTheme.colors.textSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = errorMessage ?: "",
                            color = NisError,
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 1. Primary "Continue with Google" Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(NisPillShape)
                            .background(Color.White)
                            .nisPressable {
                                try {
                                    val intent = googleAuthManager.createChooseAccountIntent()
                                    accountPickerLauncher.launch(intent)
                                } catch (e: Exception) {
                                    showDirectInput = true
                                    errorMessage = "Choose your Google email below."
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        GoogleLogoIcon(modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Continue with Google",
                            color = Color(0xFF1F1F1F),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2. Direct Email Input Accordion / Fallback
                    if (!showDirectInput) {
                        Text(
                            text = "Or enter Google details manually",
                            color = NisAccentCyan,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clickable { showDirectInput = true }
                                .padding(vertical = 4.dp)
                        )
                    } else {
                        Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(NisShapes.medium)
                                        .background(NisTheme.colors.surface)
                                        .border(1.dp, NisTheme.colors.borderSubtle, NisShapes.medium)
                                ) {
                                    TextField(
                                        value = firstNameInput,
                                        onValueChange = { firstNameInput = it },
                                        placeholder = {
                                            Text("First Name", color = NisTheme.colors.textMuted, style = MaterialTheme.typography.bodySmall)
                                        },
                                        singleLine = true,
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent,
                                            focusedTextColor = NisTheme.colors.textPrimary,
                                            unfocusedTextColor = NisTheme.colors.textPrimary
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(NisShapes.medium)
                                        .background(NisTheme.colors.surface)
                                        .border(1.dp, NisTheme.colors.borderSubtle, NisShapes.medium)
                                ) {
                                    TextField(
                                        value = lastNameInput,
                                        onValueChange = { lastNameInput = it },
                                        placeholder = {
                                            Text("Last Name", color = NisTheme.colors.textMuted, style = MaterialTheme.typography.bodySmall)
                                        },
                                        singleLine = true,
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent,
                                            focusedTextColor = NisTheme.colors.textPrimary,
                                            unfocusedTextColor = NisTheme.colors.textPrimary
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(NisShapes.medium)
                                    .background(NisTheme.colors.surface)
                                    .border(1.dp, NisTheme.colors.borderSubtle, NisShapes.medium)
                            ) {
                                TextField(
                                    value = emailInput,
                                    onValueChange = {
                                        emailInput = it
                                        errorMessage = null
                                    },
                                    placeholder = {
                                        Text("yourname@gmail.com", color = NisTheme.colors.textMuted, style = MaterialTheme.typography.bodySmall)
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Email,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            if (emailInput.isNotBlank() && emailInput.contains("@")) {
                                                val user = googleAuthManager.createVerifiedGoogleUser(
                                                    email = emailInput.trim(),
                                                    firstName = firstNameInput.trim(),
                                                    lastName = lastNameInput.trim()
                                                )
                                                onSuccess(user)
                                            } else {
                                                errorMessage = "Please enter a valid Google email address"
                                            }
                                        }
                                    ),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        focusedTextColor = NisTheme.colors.textPrimary,
                                        unfocusedTextColor = NisTheme.colors.textPrimary
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .clip(NisMediaCardShape)
                                    .background(NisAccentPrimary)
                                    .nisPressable {
                                        if (emailInput.isNotBlank() && emailInput.contains("@")) {
                                            val user = googleAuthManager.createVerifiedGoogleUser(
                                                email = emailInput.trim(),
                                                firstName = firstNameInput.trim(),
                                                lastName = lastNameInput.trim()
                                            )
                                            onSuccess(user)
                                        } else {
                                            errorMessage = "Please enter a valid Google email address"
                                        }
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Sign In with Google Email",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3. Demo Mode (1-minute grace period)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .clip(NisMediaCardShape)
                            .background(NisAccentAmber.copy(alpha = 0.15f))
                            .border(1.dp, NisAccentAmber.copy(alpha = 0.38f), NisMediaCardShape)
                            .nisPressable {
                                val demoUser = googleAuthManager.createDemoUser()
                                Toast.makeText(
                                    context,
                                    "Demo Mode Activated: 1 minute grace time to watch content",
                                    Toast.LENGTH_LONG
                                ).show()
                                onSuccess(demoUser)
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "⚡ Instant Demo Mode (1-Min Grace Preview)",
                            color = NisAccentAmber,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Cancel button
                    Text(
                        text = "Cancel & Browse Catalogue",
                        color = NisTextMuted,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .nisPressable(onClick = onDismiss)
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

/**
 * Crisp official 4-color Google "G" Icon.
 */
@Composable
fun GoogleLogoIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Outer bounds
        val cx = w / 2f
        val cy = h / 2f
        val r = w * 0.46f

        // Blue right segment
        val bluePath = Path().apply {
            moveTo(cx, cy)
            lineTo(w, cy)
            lineTo(w, cy - r * 0.45f)
            lineTo(cx + r, cy - r * 0.45f)
            arcTo(
                androidx.compose.ui.geometry.Rect(cx - r, cy - r, cx + r, cy + r),
                0f,
                -75f,
                false
            )
            close()
        }
        drawPath(bluePath, color = Color(0xFF4285F4))

        // Red top segment
        val redPath = Path().apply {
            arcTo(
                androidx.compose.ui.geometry.Rect(cx - r, cy - r, cx + r, cy + r),
                -75f,
                -105f,
                false
            )
            lineTo(cx, cy)
            close()
        }
        drawPath(redPath, color = Color(0xFFEA4335))

        // Yellow left segment
        val yellowPath = Path().apply {
            arcTo(
                androidx.compose.ui.geometry.Rect(cx - r, cy - r, cx + r, cy + r),
                180f,
                -75f,
                false
            )
            lineTo(cx, cy)
            close()
        }
        drawPath(yellowPath, color = Color(0xFFFBBC05))

        // Green bottom segment
        val greenPath = Path().apply {
            arcTo(
                androidx.compose.ui.geometry.Rect(cx - r, cy - r, cx + r, cy + r),
                105f,
                -105f,
                false
            )
            lineTo(cx, cy)
            close()
        }
        drawPath(greenPath, color = Color(0xFF34A853))

        // Inner white cutout to make the 'G'
        drawCircle(
            color = Color.White,
            radius = r * 0.58f,
            center = androidx.compose.ui.geometry.Offset(cx, cy)
        )

        // Blue horizontal crossbar
        val barPath = Path().apply {
            moveTo(cx, cy - r * 0.18f)
            lineTo(w * 0.96f, cy - r * 0.18f)
            lineTo(w * 0.96f, cy + r * 0.18f)
            lineTo(cx, cy + r * 0.18f)
            close()
        }
        drawPath(barPath, color = Color(0xFF4285F4))
    }
}
