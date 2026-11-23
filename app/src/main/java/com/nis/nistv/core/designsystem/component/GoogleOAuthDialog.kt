package com.nis.nistv.core.designsystem.component

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nis.nistv.core.auth.GoogleAuthManager
import com.nis.nistv.core.designsystem.theme.*

/**
 * Embedded Google OAuth Web Dialog for AFDS Authentication.
 *
 * Directly executes the official AFDS Google Identity OAuth 2.0 flow:
 * https://accounts.google.com/o/oauth2/v2/auth
 * with client_id = 58094879805-2k4u6f17pfn7fm68kg31fcr4ah7slm0d.apps.googleusercontent.com
 * and redirect_uri = https://afds.hashhackers.com/login.html
 *
 * Once user authenticates with Google, the browser redirects with the signed
 * OIDC id_token in the URL fragment (#id_token=...). The WebViewClient intercepts
 * the redirect, extracts the genuine JWT id_token, and hands it directly to
 * afdsAuthService.loginWithGoogle(idToken).
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GoogleOAuthDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onTokenReceived: (idToken: String) -> Unit,
    loginHint: String? = null,
    onError: (message: String) -> Unit = {}
) {
    if (!isOpen) return

    var isLoading by remember { mutableStateOf(true) }

    val oauthUrl = remember(loginHint) {
        val nonce = System.currentTimeMillis().toString(36)
        val cleanHint = loginHint?.trim()
        val hintParam = if (!cleanHint.isNullOrBlank()) {
            "&login_hint=" + java.net.URLEncoder.encode(cleanHint, "UTF-8")
        } else {
            "&prompt=select_account"
        }
        "https://accounts.google.com/o/oauth2/v2/auth?" +
                "client_id=${GoogleAuthManager.AFDS_GOOGLE_CLIENT_ID}" +
                "&redirect_uri=https%3A%2F%2Fafds.hashhackers.com%2Flogin.html" +
                "&response_type=token%20id_token" +
                "&scope=openid%20email%20profile" +
                "&nonce=$nonce" +
                hintParam
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .clip(NisDialogShape),
            color = NisTheme.colors.surfaceElevated,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NisTheme.colors.surfaceElevated)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NisTheme.colors.surfaceHigh)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GoogleLogoIcon(modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sign in with Google",
                            color = NisTheme.colors.textPrimary,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Continue to AFDS Hash Hackers",
                            color = NisTheme.colors.textSecondary,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = NisAccentCyan,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = NisTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // WebView Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.White)
                ) {
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    useWideViewPort = true
                                    loadWithOverviewMode = true
                                    cacheMode = WebSettings.LOAD_DEFAULT
                                    // Use standard Mobile Chrome User-Agent to prevent Google's "disallowed_useragent" WebView block
                                    userAgentString = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"
                                }

                                val cookieManager = CookieManager.getInstance()
                                cookieManager.setAcceptCookie(true)
                                cookieManager.setAcceptThirdPartyCookies(this, true)

                                webViewClient = object : WebViewClient() {
                                    private fun checkRedirect(url: String?): Boolean {
                                        if (url == null) return false

                                        // AFDS OAuth redirect target: https://afds.hashhackers.com/login.html#...
                                        if (url.contains("id_token=")) {
                                            try {
                                                // Extract fragment or query parameters
                                                val fragment = when {
                                                    url.contains("#") -> url.substringAfter("#")
                                                    url.contains("?") -> url.substringAfter("?")
                                                    else -> ""
                                                }
                                                val params = fragment.split("&").associate { param ->
                                                    val parts = param.split("=", limit = 2)
                                                    parts[0] to (parts.getOrNull(1) ?: "")
                                                }
                                                val idToken = params["id_token"]
                                                if (!idToken.isNullOrBlank()) {
                                                    Log.d("GoogleOAuthDialog", "Successfully intercepted Google ID Token (${idToken.take(15)}...)")
                                                    onTokenReceived(idToken)
                                                    return true
                                                }
                                                val err = params["error"]
                                                if (!err.isNullOrBlank()) {
                                                    onError("Google sign-in error: $err")
                                                    onDismiss()
                                                    return true
                                                }
                                            } catch (e: Exception) {
                                                Log.e("GoogleOAuthDialog", "Error parsing OAuth redirect: ${e.message}", e)
                                                onError(e.message ?: "Failed to parse Google sign-in response")
                                            }
                                        } else if (url.contains("error=") && (url.contains("afds.hashhackers.com") || url.contains("login"))) {
                                            try {
                                                val fragment = when {
                                                    url.contains("#") -> url.substringAfter("#")
                                                    url.contains("?") -> url.substringAfter("?")
                                                    else -> ""
                                                }
                                                val params = fragment.split("&").associate { param ->
                                                    val parts = param.split("=", limit = 2)
                                                    parts[0] to (parts.getOrNull(1) ?: "")
                                                }
                                                val err = params["error"]
                                                if (!err.isNullOrBlank()) {
                                                    onError("Google sign-in error: $err")
                                                    onDismiss()
                                                    return true
                                                }
                                            } catch (_: Exception) {}
                                        }
                                        return false
                                    }

                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        super.onPageStarted(view, url, favicon)
                                        isLoading = true
                                        if (checkRedirect(url)) {
                                            view?.stopLoading()
                                        }
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        isLoading = false
                                        if (checkRedirect(url)) {
                                            view?.stopLoading()
                                        }
                                    }

                                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                        val url = request?.url?.toString()
                                        if (checkRedirect(url)) {
                                            view?.stopLoading()
                                            return true
                                        }
                                        return false
                                    }

                                    override fun onReceivedError(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                        error: WebResourceError?
                                    ) {
                                        super.onReceivedError(view, request, error)
                                        // Ignore subresource aborts or expected redirect cancellations
                                        val isMain = request?.isForMainFrame == true
                                        if (isMain && error?.errorCode != ERROR_FAILED_SSL_HANDSHAKE) {
                                            isLoading = false
                                        }
                                    }
                                }

                                loadUrl(oauthUrl)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(NisTheme.colors.surface.copy(alpha = 0.85f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = NisAccentCyan)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Connecting to Google Auth...",
                                    color = NisTheme.colors.textSecondary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
