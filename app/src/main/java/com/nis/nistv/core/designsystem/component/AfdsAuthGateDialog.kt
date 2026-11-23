package com.nis.nistv.core.designsystem.component

import android.accounts.AccountManager
import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.nis.nistv.core.designsystem.theme.*
import com.nis.nistv.data.download.provider.AfdsAccountNotFoundException
import com.nis.nistv.data.download.provider.AfdsAuthService
import kotlinx.coroutines.launch

/**
 * Authentication modes supported by the AFDS Auth Gate Dialog.
 */
enum class AfdsAuthMode {
    SIGN_IN,
    SIGN_UP
}

/**
 * Unified AFDS Sign-In & Sign-Up Gate Dialog.
 *
 * Serves as the primary authentication gate for the NisTV application,
 * authenticating both the NisTV App and the HashHackers AFDS Cloud Archive
 * in a single unified session.
 *
 * Supports:
 * - Direct Sign In via Telegram Bot OTP or Email OTP
 * - Google Sign-In & Google Web OAuth fallback
 * - Bearer Token manual entry
 * - In-App Sign Up with automatic redirection when an email is not registered
 * - Two-step Sign Up verification (Details -> 6-digit OTP -> Auto Login Handoff)
 */
@Composable
fun AfdsAuthGateDialog(
    afdsAuthService: AfdsAuthService,
    googleAuthManager: GoogleAuthManager? = null,
    initialEmail: String = "",
    onSuccess: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val activity = remember(context) {
        var ctx = context
        while (ctx is android.content.ContextWrapper) {
            if (ctx is Activity) return@remember ctx
            ctx = ctx.baseContext
        }
        null
    }

    var authMode by remember { mutableStateOf(AfdsAuthMode.SIGN_IN) }

    // Sign In State
    var step by remember { mutableIntStateOf(0) } // 0 = email / methods, 1 = OTP verify
    var emailInput by remember { mutableStateOf(initialEmail) }
    var otpInput by remember { mutableStateOf("") }
    var manualTokenInput by remember { mutableStateOf("") }
    var showManualToken by remember { mutableStateOf(false) }
    var showGoogleOAuthDialog by remember { mutableStateOf(false) }
    var botId by remember { mutableStateOf<String?>(null) }
    var loginType by remember { mutableStateOf<String?>("telegram") }

    // Sign Up State
    var signUpStep by remember { mutableIntStateOf(0) } // 0 = details, 1 = OTP verify
    var firstNameInput by remember { mutableStateOf("") }
    var lastNameInput by remember { mutableStateOf("") }
    var signUpEmailInput by remember { mutableStateOf(initialEmail) }
    var inviteCodeInput by remember { mutableStateOf("WELCOME") }
    var signUpOtpInput by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }

    fun switchToSignUp(prefilledEmail: String, notice: String? = null) {
        authMode = AfdsAuthMode.SIGN_UP
        signUpStep = 0
        if (prefilledEmail.isNotBlank()) {
            signUpEmailInput = prefilledEmail
        }
        errorMessage = null
        infoMessage = notice ?: "No account found with '$prefilledEmail'. Create your AFDS account below to get started!"
    }

    fun requestOtp(sendViaEmail: Boolean) {
        val cleanEmail = emailInput.trim()
        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            errorMessage = "Please enter a valid email address."
            return
        }

        isLoading = true
        errorMessage = null
        infoMessage = null

        coroutineScope.launch {
            if (sendViaEmail) {
                afdsAuthService.requestLoginOtpEmail(cleanEmail)
                    .onSuccess { resp ->
                        step = 1
                        botId = null
                        loginType = "email"
                        errorMessage = null
                        infoMessage = resp.message ?: "Login code sent to your email!"
                    }
                    .onFailure { err ->
                        errorMessage = err.message ?: "Failed to send verification email"
                    }
            } else {
                afdsAuthService.requestLoginOtp(cleanEmail)
                    .onSuccess { resp ->
                        step = 1
                        botId = resp.botId
                        loginType = resp.loginType ?: "telegram"
                        errorMessage = null
                        infoMessage = resp.message ?: if (resp.botId != null) "Code sent to Telegram @${resp.botId}" else "Login code sent to your email!"
                    }
                    .onFailure { tgErr ->
                        // Auto fallback to email if bot delivery fails
                        afdsAuthService.requestLoginOtpEmail(cleanEmail)
                            .onSuccess { resp ->
                                step = 1
                                botId = null
                                loginType = "email"
                                errorMessage = null
                                infoMessage = resp.message ?: "Login code sent to your email!"
                            }
                            .onFailure { emailErr ->
                                errorMessage = emailErr.message ?: tgErr.message ?: "Failed to send code"
                            }
                    }
            }
            isLoading = false
        }
    }

    // Native Android Google Account Picker & Token Exchange with AFDS Server
    val accountPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val accountName = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            if (!accountName.isNullOrBlank() && googleAuthManager != null) {
                emailInput = accountName
                isLoading = true
                errorMessage = null
                infoMessage = null
                coroutineScope.launch {
                    try {
                        val googleUser = googleAuthManager.fetchGoogleUserProfile(accountName, activity)
                        if (googleUser.idToken.isNotBlank() && !googleUser.idToken.startsWith("google_auth_")) {
                            val res = afdsAuthService.loginWithGoogle(googleUser.idToken)
                            if (res.isSuccess) {
                                Toast.makeText(context, "Welcome to NisTV!", Toast.LENGTH_SHORT).show()
                                onSuccess()
                                isLoading = false
                                return@launch
                            } else {
                                Log.i("AfdsAuthGateDialog", "Google OAuth not linked for $accountName. Falling back to OTP delivery...")
                            }
                        }

                        // If Google Play Services cannot issue an ID token for AFDS cross-client directly
                        // or if the account is not linked via Google OAuth in AFDS,
                        // use the selected Google account to request login OTP immediately (zero password typing).
                        infoMessage = "Requesting login code for $accountName..."
                        requestOtp(sendViaEmail = false)
                    } catch (e: Exception) {
                        requestOtp(sendViaEmail = false)
                    } finally {
                        isLoading = false
                    }
                }
            }
        }
    }

    fun verifyOtp() {
        val cleanEmail = emailInput.trim()
        val cleanCode = otpInput.trim()
        if (cleanCode.length != 6) {
            errorMessage = "Please enter the complete 6-digit code."
            return
        }

        isLoading = true
        errorMessage = null
        infoMessage = null

        coroutineScope.launch {
            afdsAuthService.verifyLoginOtp(cleanEmail, cleanCode)
                .onSuccess {
                    Toast.makeText(context, "Signed in to NisTV & AFDS!", Toast.LENGTH_SHORT).show()
                    onSuccess()
                }
                .onFailure { err ->
                    errorMessage = err.message ?: "Verification failed. Check the code and try again."
                }
            isLoading = false
        }
    }

    fun submitManualToken() {
        val cleanToken = manualTokenInput.trim()
        if (cleanToken.isBlank()) {
            errorMessage = "Please enter your AFDS Bearer token."
            return
        }

        isLoading = true
        errorMessage = null
        infoMessage = null

        coroutineScope.launch {
            afdsAuthService.getProfile(cleanToken)
                .onSuccess { profile ->
                    afdsAuthService.logout()
                    val email = profile.email.orEmpty().ifBlank { "member@afds" }
                    val userId = profile.userId.orEmpty()
                    val channelId = profile.channelId.orEmpty()
                    val app = context.applicationContext as? com.nis.nistv.NisTvApplication
                    app?.container?.userPreferences?.saveAfdsSession(cleanToken, email, userId, channelId)
                    Toast.makeText(context, "Connected to AFDS!", Toast.LENGTH_SHORT).show()
                    onSuccess()
                }
                .onFailure { err ->
                    errorMessage = "Invalid token or network error: ${err.message}"
                }
            isLoading = false
        }
    }

    fun requestSignUpCode() {
        val cleanFirstName = firstNameInput.trim()
        val cleanLastName = lastNameInput.trim()
        val cleanEmail = signUpEmailInput.trim()
        val cleanInviteCode = inviteCodeInput.trim().uppercase().ifBlank { "WELCOME" }

        if (cleanFirstName.isBlank() || cleanLastName.isBlank()) {
            errorMessage = "First and last name are required."
            return
        }
        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            errorMessage = "Please enter a valid email address."
            return
        }
        if (cleanEmail.contains("+")) {
            errorMessage = "Email addresses with '+' sign are not allowed by AFDS."
            return
        }

        isLoading = true
        errorMessage = null
        infoMessage = null

        coroutineScope.launch {
            afdsAuthService.requestSignupVerification(
                firstName = cleanFirstName,
                lastName = cleanLastName,
                email = cleanEmail,
                inviteCode = cleanInviteCode
            ).onSuccess { resp ->
                signUpStep = 1
                errorMessage = null
                infoMessage = resp.message ?: "Verification code sent to $cleanEmail!"
            }.onFailure { err ->
                errorMessage = err.message ?: "Failed to send signup verification code."
            }
            isLoading = false
        }
    }

    fun verifySignUpCode() {
        val cleanEmail = signUpEmailInput.trim()
        val cleanCode = signUpOtpInput.trim()
        if (cleanCode.length != 6) {
            errorMessage = "Please enter the complete 6-digit code."
            return
        }

        isLoading = true
        errorMessage = null
        infoMessage = null

        coroutineScope.launch {
            afdsAuthService.verifySignup(cleanEmail, cleanCode)
                .onSuccess {
                    // Account successfully created! Automatically request a login code and switch to login OTP view
                    emailInput = cleanEmail
                    authMode = AfdsAuthMode.SIGN_IN
                    step = 1
                    signUpStep = 0
                    signUpOtpInput = ""

                    afdsAuthService.requestLoginOtpEmail(cleanEmail)
                        .onSuccess {
                            loginType = "email"
                            botId = null
                            infoMessage = "Account created! A login code has been sent to $cleanEmail. Enter it below to sign in."
                            Toast.makeText(context, "Account created! Check your email for login code.", Toast.LENGTH_LONG).show()
                        }
                        .onFailure {
                            infoMessage = "Account created! You can now sign in using your email."
                        }
                }
                .onFailure { err ->
                    errorMessage = err.message ?: "Verification failed. Check the code and try again."
                }
            isLoading = false
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
                    .padding(22.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Logo & Brand Header
                    NtvLogo(size = 36.dp, showWordmark = true, fontSize = 22)

                    Spacer(modifier = Modifier.height(14.dp))

                    // Mode Segmented Switcher: [ Sign In | Create Account ]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(NisShapes.medium)
                            .background(NisTheme.colors.surface)
                            .border(1.dp, NisTheme.colors.borderSubtle, NisShapes.medium)
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(NisThumbnailShape)
                                .background(if (authMode == AfdsAuthMode.SIGN_IN) NisTheme.colors.surfaceElevated else Color.Transparent)
                                .clickable(enabled = !isLoading) {
                                    authMode = AfdsAuthMode.SIGN_IN
                                    errorMessage = null
                                    infoMessage = null
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Sign In",
                                color = if (authMode == AfdsAuthMode.SIGN_IN) NisTheme.colors.textPrimary else NisTheme.colors.textMuted,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (authMode == AfdsAuthMode.SIGN_IN) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(NisThumbnailShape)
                                .background(if (authMode == AfdsAuthMode.SIGN_UP) NisAccentPrimary else Color.Transparent)
                                .clickable(enabled = !isLoading) {
                                    authMode = AfdsAuthMode.SIGN_UP
                                    if (signUpEmailInput.isBlank() && emailInput.isNotBlank()) {
                                        signUpEmailInput = emailInput
                                    }
                                    errorMessage = null
                                    infoMessage = null
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Create Account",
                                color = if (authMode == AfdsAuthMode.SIGN_UP) Color.White else NisTextMuted,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (authMode == AfdsAuthMode.SIGN_UP) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Header Title
                    Text(
                        text = when (authMode) {
                            AfdsAuthMode.SIGN_IN -> if (step == 0) "Sign In to NisTV" else "Verify Login Code"
                            AfdsAuthMode.SIGN_UP -> if (signUpStep == 0) "Create AFDS Account" else "Verify Your Email"
                        },
                        color = NisTheme.colors.textPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Subtitle
                    Text(
                        text = when (authMode) {
                            AfdsAuthMode.SIGN_IN -> {
                                if (step == 0) {
                                    "Sign in with your AFDS account to watch Live TV, download movies & series, and sync your library."
                                } else {
                                    if (botId != null) {
                                        "Enter the 6-digit verification code sent to Telegram @$botId for ${emailInput.trim()}"
                                    } else {
                                        "Enter the 6-digit verification code sent to ${emailInput.trim()}"
                                    }
                                }
                            }
                            AfdsAuthMode.SIGN_UP -> {
                                if (signUpStep == 0) {
                                    "Join free — no credit card needed. Instant access to cloud downloads & media archive."
                                } else {
                                    "Enter the 6-digit code sent to ${signUpEmailInput.trim()} to create your account."
                                }
                            }
                        },
                        color = NisTextSecondary,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    // Info Message Banner (e.g. Account not found prompt, code sent notices)
                    if (infoMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(NisShapes.small)
                                .background(NisLiveEmerald.copy(alpha = 0.15f))
                                .border(1.dp, NisLiveEmerald.copy(alpha = 0.4f), NisShapes.small)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = infoMessage ?: "",
                                color = NisLiveEmerald,
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Error Message Banner
                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(NisShapes.small)
                                .background(NisLiveRed.copy(alpha = 0.15f))
                                .border(1.dp, NisLiveRed.copy(alpha = 0.4f), NisShapes.small)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = errorMessage ?: "",
                                    color = NisLiveRed,
                                    style = MaterialTheme.typography.labelSmall,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (authMode == AfdsAuthMode.SIGN_IN &&
                                    (errorMessage?.contains("No account found", ignoreCase = true) == true ||
                                     errorMessage?.contains("No AFDS account", ignoreCase = true) == true)) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Don't have an account yet? Create one here →",
                                        color = NisAccentCyan,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .clickable {
                                                switchToSignUp(emailInput)
                                            }
                                            .padding(vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Loading Indicator
                    if (isLoading) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = NisAccentPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Contacting AFDS server...",
                                color = NisTextSecondary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ==========================================
                    // SIGN IN MODE UI
                    // ==========================================
                    if (authMode == AfdsAuthMode.SIGN_IN) {
                        if (step == 0) {
                            // Email input
                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = { emailInput = it },
                                label = { Text("Account Email", color = NisTextMuted, style = MaterialTheme.typography.bodySmall) },
                                singleLine = true,
                                enabled = !isLoading,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = { requestOtp(false) }),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = NisSurfaceElevated,
                                    unfocusedContainerColor = NisSurfaceElevated,
                                    focusedTextColor = NisTextPrimary,
                                    unfocusedTextColor = NisTextPrimary,
                                    focusedIndicatorColor = NisAccentPrimary,
                                    unfocusedIndicatorColor = NisBorder
                                ),
                                shape = NisShapes.medium,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Primary Action: Telegram Bot Code
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(NisPillShape)
                                    .background(if (!isLoading && emailInput.isNotBlank()) NisAccentPrimary else NisSurfaceElevated)
                                    .clickable(enabled = !isLoading && emailInput.isNotBlank()) {
                                        requestOtp(false)
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Send Code via Telegram Bot",
                                    color = if (!isLoading && emailInput.isNotBlank()) Color.White else NisTextMuted,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Secondary Action: Email Code
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .clip(NisPillShape)
                                    .background(NisSurface)
                                    .border(1.dp, NisBorder, NisPillShape)
                                    .clickable(enabled = !isLoading && emailInput.isNotBlank()) {
                                        requestOtp(true)
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Send Code via Email Instead",
                                    color = NisTextSecondary,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Google Sign In via AFDS Backend
                            if (googleAuthManager != null) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .clip(NisPillShape)
                                        .background(Color.White)
                                        .clickable(enabled = !isLoading) {
                                            isLoading = true
                                            errorMessage = null
                                            infoMessage = null
                                            coroutineScope.launch {
                                                var credHandled = false
                                                if (activity != null) {
                                                    val credResult = googleAuthManager.signInWithCredentialManager(activity)
                                                    if (credResult.isSuccess) {
                                                        val googleUser = credResult.getOrNull()
                                                        if (googleUser != null && googleUser.idToken.isNotBlank() && !googleUser.idToken.startsWith("google_auth_")) {
                                                            val res = afdsAuthService.loginWithGoogle(googleUser.idToken)
                                                            res.onSuccess {
                                                                Toast.makeText(context, "Welcome to NisTV!", Toast.LENGTH_SHORT).show()
                                                                onSuccess()
                                                                isLoading = false
                                                                credHandled = true
                                                                return@launch
                                                            }.onFailure { err ->
                                                                // Google OAuth not linked; fall back to requesting login OTP for googleUser.email
                                                                val fallbackEmail = googleUser.email.ifBlank { emailInput }
                                                                if (fallbackEmail.isNotBlank()) {
                                                                    emailInput = fallbackEmail
                                                                    infoMessage = "Google OAuth not linked. Requesting login code for $fallbackEmail..."
                                                                    requestOtp(sendViaEmail = false)
                                                                    isLoading = false
                                                                    credHandled = true
                                                                    return@launch
                                                                } else {
                                                                    errorMessage = err.message ?: "Google sign-in to AFDS failed"
                                                                }
                                                            }
                                                        }
                                                    } else {
                                                        val err = credResult.exceptionOrNull()
                                                        if (err is androidx.credentials.exceptions.GetCredentialCancellationException) {
                                                            isLoading = false
                                                            return@launch
                                                        }
                                                    }
                                                }

                                                // Fallback to Native Android System Account Picker on device
                                                if (!credHandled) {
                                                    isLoading = false
                                                    try {
                                                        accountPickerLauncher.launch(googleAuthManager.createChooseAccountIntent())
                                                    } catch (_: Exception) {
                                                        showGoogleOAuthDialog = true
                                                    }
                                                }
                                            }
                                        },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    GoogleLogoIcon(modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Continue with Google (AFDS)",
                                        color = Color(0xFF1F1F1F),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Having trouble? Sign in via Google Web Portal",
                                    color = NisTextMuted,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier
                                        .clickable { showGoogleOAuthDialog = true }
                                        .padding(4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Don't have an account? Create one link
                            Row(
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .clickable(enabled = !isLoading) {
                                        switchToSignUp(emailInput)
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Don't have an AFDS account? ",
                                    color = NisTextSecondary,
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    text = "Create Account",
                                    color = NisAccentCyan,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Expandable Bearer Token
                            if (!showManualToken) {
                                Text(
                                    text = "Or paste AFDS Bearer Token",
                                    color = NisAccentCyan,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .clickable { showManualToken = true }
                                        .padding(vertical = 4.dp)
                                )
                            } else {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = manualTokenInput,
                                        onValueChange = { manualTokenInput = it },
                                        label = { Text("AFDS JWT Token", color = NisTextMuted, style = MaterialTheme.typography.labelSmall) },
                                        singleLine = true,
                                        enabled = !isLoading,
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = NisSurfaceElevated,
                                            unfocusedContainerColor = NisSurfaceElevated,
                                            focusedTextColor = NisTextPrimary,
                                            unfocusedTextColor = NisTextPrimary,
                                            focusedIndicatorColor = NisAccentCyan,
                                            unfocusedIndicatorColor = NisBorder
                                        ),
                                        shape = NisThumbnailShape,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(40.dp)
                                            .clip(NisPillShape)
                                            .background(NisAccentCyan.copy(alpha = 0.2f))
                                            .border(1.dp, NisAccentCyan, NisPillShape)
                                            .clickable(enabled = !isLoading && manualTokenInput.isNotBlank()) {
                                                submitManualToken()
                                            },
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "Apply Token & Sign In",
                                            color = NisAccentCyan,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        } else {
                            // Step 1: 6-Digit OTP Verification
                            OutlinedTextField(
                                value = otpInput,
                                onValueChange = { input ->
                                    if (input.length <= 6) {
                                        otpInput = input.filter { it.isDigit() }
                                    }
                                },
                                label = { Text("6-Digit Login Code", color = NisTextMuted, style = MaterialTheme.typography.bodySmall) },
                                singleLine = true,
                                enabled = !isLoading,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.NumberPassword,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = { verifyOtp() }),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = NisSurfaceElevated,
                                    unfocusedContainerColor = NisSurfaceElevated,
                                    focusedTextColor = NisTextPrimary,
                                    unfocusedTextColor = NisTextPrimary,
                                    focusedIndicatorColor = NisLiveEmerald,
                                    unfocusedIndicatorColor = NisBorder
                                ),
                                shape = NisShapes.medium,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Verify Button
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(NisPillShape)
                                    .background(if (!isLoading && otpInput.length == 6) NisLiveEmerald else NisSurfaceElevated)
                                    .clickable(enabled = !isLoading && otpInput.length == 6) {
                                        verifyOtp()
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Verify & Sign In",
                                    color = if (!isLoading && otpInput.length == 6) Color.Black else NisTextMuted,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Back to change email or resend
                            TextButton(
                                onClick = {
                                    step = 0
                                    otpInput = ""
                                    errorMessage = null
                                    infoMessage = null
                                },
                                enabled = !isLoading
                            ) {
                                Text(
                                    text = "Change Email or Resend Code",
                                    color = NisAccentCyan,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    } else {
                        // ==========================================
                        // SIGN UP MODE UI
                        // ==========================================
                        if (signUpStep == 0) {
                            // Step 0: User Details Form
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = firstNameInput,
                                    onValueChange = { firstNameInput = it },
                                    label = { Text("First Name", color = NisTextMuted, style = MaterialTheme.typography.labelSmall) },
                                    placeholder = { Text("Ada", color = NisTextMuted.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall) },
                                    singleLine = true,
                                    enabled = !isLoading,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = NisSurfaceElevated,
                                        unfocusedContainerColor = NisSurfaceElevated,
                                        focusedTextColor = NisTextPrimary,
                                        unfocusedTextColor = NisTextPrimary,
                                        focusedIndicatorColor = NisAccentPrimary,
                                        unfocusedIndicatorColor = NisBorder
                                    ),
                                    shape = NisShapes.medium,
                                    modifier = Modifier.weight(1f)
                                )

                                OutlinedTextField(
                                    value = lastNameInput,
                                    onValueChange = { lastNameInput = it },
                                    label = { Text("Last Name", color = NisTextMuted, style = MaterialTheme.typography.labelSmall) },
                                    placeholder = { Text("Lovelace", color = NisTextMuted.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall) },
                                    singleLine = true,
                                    enabled = !isLoading,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = NisSurfaceElevated,
                                        unfocusedContainerColor = NisSurfaceElevated,
                                        focusedTextColor = NisTextPrimary,
                                        unfocusedTextColor = NisTextPrimary,
                                        focusedIndicatorColor = NisAccentPrimary,
                                        unfocusedIndicatorColor = NisBorder
                                    ),
                                    shape = NisShapes.medium,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Email Field
                            OutlinedTextField(
                                value = signUpEmailInput,
                                onValueChange = { signUpEmailInput = it },
                                label = { Text("Email Address", color = NisTextMuted, style = MaterialTheme.typography.bodySmall) },
                                placeholder = { Text("you@gmail.com", color = NisTextMuted.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall) },
                                singleLine = true,
                                enabled = !isLoading,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = NisSurfaceElevated,
                                    unfocusedContainerColor = NisSurfaceElevated,
                                    focusedTextColor = NisTextPrimary,
                                    unfocusedTextColor = NisTextPrimary,
                                    focusedIndicatorColor = NisAccentPrimary,
                                    unfocusedIndicatorColor = NisBorder
                                ),
                                shape = NisShapes.medium,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Email Live Validation Preview
                            if (signUpEmailInput.contains("+")) {
                                Text(
                                    text = "⚠️ Email addresses with '+' sign are not allowed by AFDS",
                                    color = NisLiveRed,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp, start = 4.dp)
                                )
                            } else if (signUpEmailInput.contains("@gmail.com", ignoreCase = true) || signUpEmailInput.contains("@googlemail.com", ignoreCase = true)) {
                                val normalized = AfdsAuthService.normalizeEmail(signUpEmailInput)
                                if (normalized != signUpEmailInput.trim().lowercase()) {
                                    Text(
                                        text = "📧 Will be registered as: $normalized",
                                        color = NisAccentCyan,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp, start = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Invite Code Field
                            OutlinedTextField(
                                value = inviteCodeInput,
                                onValueChange = { inviteCodeInput = it },
                                label = { Text("Invite Code", color = NisTextMuted, style = MaterialTheme.typography.bodySmall) },
                                placeholder = { Text("WELCOME", color = NisTextMuted.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall) },
                                singleLine = true,
                                enabled = !isLoading,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = NisSurfaceElevated,
                                    unfocusedContainerColor = NisSurfaceElevated,
                                    focusedTextColor = NisTextPrimary,
                                    unfocusedTextColor = NisTextPrimary,
                                    focusedIndicatorColor = NisAccentPrimary,
                                    unfocusedIndicatorColor = NisBorder
                                ),
                                shape = NisShapes.medium,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "Default public code is WELCOME",
                                color = NisTextMuted,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 3.dp, start = 4.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Submit Button
                            val isSignUpValid = firstNameInput.isNotBlank() &&
                                lastNameInput.isNotBlank() &&
                                signUpEmailInput.isNotBlank() &&
                                !signUpEmailInput.contains("+")

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(NisPillShape)
                                    .background(if (!isLoading && isSignUpValid) NisAccentPrimary else NisSurfaceElevated)
                                    .clickable(enabled = !isLoading && isSignUpValid) {
                                        requestSignUpCode()
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Send Verification Code",
                                    color = if (!isLoading && isSignUpValid) Color.White else NisTextMuted,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Switch to Sign In link
                            Row(
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .clickable(enabled = !isLoading) {
                                        authMode = AfdsAuthMode.SIGN_IN
                                        errorMessage = null
                                        infoMessage = null
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Already have an account? ",
                                    color = NisTextSecondary,
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    text = "Sign In",
                                    color = NisAccentCyan,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            // Step 1: Sign Up OTP Code Verification
                            OutlinedTextField(
                                value = signUpOtpInput,
                                onValueChange = { input ->
                                    if (input.length <= 6) {
                                        signUpOtpInput = input.filter { it.isDigit() }
                                    }
                                },
                                label = { Text("6-Digit Verification Code", color = NisTextMuted, style = MaterialTheme.typography.bodySmall) },
                                singleLine = true,
                                enabled = !isLoading,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.NumberPassword,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = { verifySignUpCode() }),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = NisSurfaceElevated,
                                    unfocusedContainerColor = NisSurfaceElevated,
                                    focusedTextColor = NisTextPrimary,
                                    unfocusedTextColor = NisTextPrimary,
                                    focusedIndicatorColor = NisLiveEmerald,
                                    unfocusedIndicatorColor = NisBorder
                                ),
                                shape = NisShapes.medium,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Verify & Complete Signup Button
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(NisPillShape)
                                    .background(if (!isLoading && signUpOtpInput.length == 6) NisLiveEmerald else NisSurfaceElevated)
                                    .clickable(enabled = !isLoading && signUpOtpInput.length == 6) {
                                        verifySignUpCode()
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Verify & Complete Registration",
                                    color = if (!isLoading && signUpOtpInput.length == 6) Color.Black else NisTextMuted,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            TextButton(
                                onClick = {
                                    signUpStep = 0
                                    signUpOtpInput = ""
                                    errorMessage = null
                                    infoMessage = null
                                },
                                enabled = !isLoading
                            ) {
                                Text(
                                    text = "Back to Details",
                                    color = NisAccentCyan,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Cancel & Dismiss
                    Text(
                        text = "Cancel & Browse Catalogue",
                        color = NisTextMuted,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clickable(onClick = onDismiss)
                            .padding(6.dp)
                    )
                }
            }
        }
    }

    // Google OAuth Web Flow Dialog Fallback for AFDS Authentication
    if (showGoogleOAuthDialog) {
        GoogleOAuthDialog(
            isOpen = showGoogleOAuthDialog,
            onDismiss = { showGoogleOAuthDialog = false },
            onTokenReceived = { idToken ->
                showGoogleOAuthDialog = false
                isLoading = true
                errorMessage = null
                infoMessage = null
                coroutineScope.launch {
                    val res = afdsAuthService.loginWithGoogle(idToken)
                    res.onSuccess {
                        Toast.makeText(context, "Welcome to NisTV!", Toast.LENGTH_SHORT).show()
                        onSuccess()
                    }.onFailure { err ->
                        if (emailInput.isNotBlank()) {
                            infoMessage = "Google OAuth not linked. Requesting login code for $emailInput..."
                            requestOtp(sendViaEmail = false)
                        } else {
                            errorMessage = err.message ?: "Google sign-in to AFDS failed"
                        }
                    }
                    isLoading = false
                }
            },
            loginHint = emailInput.takeIf { it.isNotBlank() },
            onError = { err ->
                errorMessage = err
            }
        )
    }
}
