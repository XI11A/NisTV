package com.nis.nistv.data.download.provider

/**
 * AFDS Authentication Service
 *
 * Handles HashHackers AFDS authentication:
 * - Email OTP (dispatched to Telegram bot or email)
 * - OTP Verification
 * - Google ID Token Exchange
 * - User Profile & Telegram ID metadata
 *
 * Adapted from AFDS-Android (https://github.com/CloudflareHackers/AFDS-Android)
 * Licensed under the MIT License - Copyright (c) 2024 CloudflareHackers
 */

import android.util.Log
import com.nis.nistv.core.preferences.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

@Serializable
data class AfdsOtpResponse(
    val token: String? = null,
    val error: String? = null,
    @SerialName("login_type") val loginType: String? = null,
    @SerialName("bot_id") val botId: String? = null,
    val message: String? = null
)

@Serializable
data class AfdsSignupResponse(
    val message: String? = null,
    val error: String? = null
)

class AfdsAccountNotFoundException(
    val email: String,
    message: String = "No account found with this email address."
) : IOException(message)

@Serializable
data class AfdsProfileResponse(
    val id: Int? = null,
    val email: String? = null,
    @SerialName("memberSince") val memberSince: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("user_id_updated_at") val userIdUpdatedAt: String? = null,
    @SerialName("channel_id") val channelId: String? = null,
    @SerialName("daily_links_used") val dailyLinksUsed: Int? = null,
    @SerialName("daily_links_limit") val dailyLinksLimit: Int? = null,
    @SerialName("daily_sends_used") val dailySendsUsed: Int? = null,
    @SerialName("daily_sends_limit") val dailySendsLimit: Int? = null,
    val error: String? = null
)

@Serializable
private data class LoginRequestBody(val email: String)

@Serializable
private data class OtpRequestBody(val email: String, val code: String)

@Serializable
internal data class SignupRequestBody(
    val firstName: String,
    val lastName: String,
    val email: String,
    val inviteCode: String
)

@Serializable
internal data class VerifySignupRequestBody(
    val email: String,
    val code: String
)

@Serializable
private data class GoogleAuthRequestBody(val credential: String)

@Serializable
private data class AuthTokenResponse(
    val token: String? = null,
    val error: String? = null
)

class AfdsAuthService(
    private val okHttpClient: OkHttpClient,
    private val userPreferences: UserPreferences
) {
    companion object {
        const val BASE_URL = "https://tga-hd.api.hashhackers.com"
        const val PROXY_BASE_URL = "https://afds.hashhackers.com/proxyapi"
        private const val TAG = "AfdsAuthService"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        fun normalizeEmail(email: String): String {
            val trimmed = email.trim().lowercase()
            if (trimmed.endsWith("@gmail.com") || trimmed.endsWith("@googlemail.com")) {
                val parts = trimmed.split("@")
                if (parts.size == 2) {
                    val localWithoutAlias = parts[0].split("+")[0]
                    val localClean = localWithoutAlias.replace(".", "")
                    return "$localClean@gmail.com"
                }
            }
            return trimmed
        }
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private fun executeLoginOtpCall(path: String, email: String): Result<AfdsOtpResponse> {
        val cleanEmail = normalizeEmail(email)
        val rawEmail = email.trim()
        val candidateEmails = if (cleanEmail != rawEmail) listOf(cleanEmail, rawEmail) else listOf(cleanEmail)
        val endpoints = listOf(BASE_URL, PROXY_BASE_URL)
        var lastException: Exception? = null

        for (targetEmail in candidateEmails) {
            val body = json.encodeToString(LoginRequestBody.serializer(), LoginRequestBody(targetEmail))
            for (endpoint in endpoints) {
                try {
                    val request = Request.Builder()
                        .url("$endpoint$path")
                        .post(body.toRequestBody(JSON_MEDIA_TYPE))
                        .build()

                    okHttpClient.newCall(request).execute().use { response ->
                        val bodyStr = response.body?.string().orEmpty()
                        val parsed = json.decodeFromString(AfdsOtpResponse.serializer(), bodyStr)
                        if (response.isSuccessful && parsed.error == null) {
                            return Result.success(parsed)
                        } else {
                            val errMsg = parsed.error ?: "Failed to request login code (HTTP ${response.code})"
                            if (errMsg.contains("No account found", ignoreCase = true)) {
                                lastException = AfdsAccountNotFoundException(targetEmail, errMsg)
                            } else {
                                lastException = IOException(errMsg)
                            }
                        }
                    }
                } catch (e: IOException) {
                    lastException = e
                    Log.w(TAG, "Login OTP call to $endpoint$path failed: ${e.message}. Trying next endpoint...")
                } catch (e: Exception) {
                    return Result.failure(e)
                }
            }
        }
        return Result.failure(lastException ?: IOException("Failed to connect to AFDS server."))
    }

    suspend fun requestLoginOtp(email: String): Result<AfdsOtpResponse> = withContext(Dispatchers.IO) {
        try {
            executeLoginOtpCall("/request-login-otp", email)
        } catch (e: Exception) {
            Log.e(TAG, "requestLoginOtp error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun requestLoginOtpEmail(email: String): Result<AfdsOtpResponse> = withContext(Dispatchers.IO) {
        try {
            executeLoginOtpCall("/request-login-otp-email", email)
        } catch (e: Exception) {
            Log.e(TAG, "requestLoginOtpEmail error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun requestSignupVerification(
        firstName: String,
        lastName: String,
        email: String,
        inviteCode: String = "WELCOME"
    ): Result<AfdsSignupResponse> = withContext(Dispatchers.IO) {
        try {
            val cleanEmail = normalizeEmail(email)
            val cleanInviteCode = inviteCode.trim().uppercase().ifBlank { "WELCOME" }
            val body = json.encodeToString(
                SignupRequestBody.serializer(),
                SignupRequestBody(
                    firstName = firstName.trim(),
                    lastName = lastName.trim(),
                    email = cleanEmail,
                    inviteCode = cleanInviteCode
                )
            )
            executeSignupCall("/api/request-verification", body)
        } catch (e: Exception) {
            Log.e(TAG, "requestSignupVerification error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun verifySignup(
        email: String,
        code: String
    ): Result<AfdsSignupResponse> = withContext(Dispatchers.IO) {
        try {
            val cleanEmail = normalizeEmail(email)
            val body = json.encodeToString(
                VerifySignupRequestBody.serializer(),
                VerifySignupRequestBody(
                    email = cleanEmail,
                    code = code.trim()
                )
            )
            executeSignupCall("/api/verify", body)
        } catch (e: Exception) {
            Log.e(TAG, "verifySignup error: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun executeSignupCall(path: String, jsonBody: String): Result<AfdsSignupResponse> {
        val urls = listOf("$BASE_URL$path", "$PROXY_BASE_URL$path")
        var lastException: Exception? = null

        for (url in urls) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    val bodyStr = response.body?.string().orEmpty()
                    val parsed = json.decodeFromString(AfdsSignupResponse.serializer(), bodyStr)
                    return if (response.isSuccessful && parsed.error == null) {
                        Result.success(parsed)
                    } else {
                        Result.failure(IOException(parsed.error ?: "Request failed (HTTP ${response.code})"))
                    }
                }
            } catch (e: IOException) {
                lastException = e
                Log.w(TAG, "Signup call to $url failed: ${e.message}. Trying fallback...")
            } catch (e: Exception) {
                return Result.failure(e)
            }
        }

        return Result.failure(lastException ?: IOException("Failed to connect to AFDS server."))
    }

    suspend fun verifyLoginOtp(email: String, code: String): Result<String> = withContext(Dispatchers.IO) {
        val cleanEmail = normalizeEmail(email)
        val rawEmail = email.trim()
        val candidateEmails = if (cleanEmail != rawEmail) listOf(cleanEmail, rawEmail) else listOf(cleanEmail)
        val endpoints = listOf(BASE_URL, PROXY_BASE_URL)
        var lastException: Exception? = null

        for (targetEmail in candidateEmails) {
            val body = json.encodeToString(OtpRequestBody.serializer(), OtpRequestBody(targetEmail, code.trim()))
            for (endpoint in endpoints) {
                try {
                    val request = Request.Builder()
                        .url("$endpoint/verify-login-otp")
                        .post(body.toRequestBody(JSON_MEDIA_TYPE))
                        .build()

                    okHttpClient.newCall(request).execute().use { response ->
                        val bodyStr = response.body?.string().orEmpty()
                        val parsed = json.decodeFromString(AfdsOtpResponse.serializer(), bodyStr)
                        val token = parsed.token
                        if (response.isSuccessful && !token.isNullOrBlank()) {
                            var profileEmail = targetEmail
                            var userId = ""
                            var channelId = ""
                            getProfile(token).onSuccess { profile ->
                                profileEmail = profile.email ?: targetEmail
                                userId = profile.userId.orEmpty()
                                channelId = profile.channelId.orEmpty()
                            }
                            userPreferences.saveAfdsSession(token, profileEmail, userId, channelId)
                            return@withContext Result.success(token)
                        } else {
                            lastException = IOException(parsed.error ?: "Verification failed (HTTP ${response.code})")
                        }
                    }
                } catch (e: IOException) {
                    lastException = e
                    Log.w(TAG, "verifyLoginOtp call to $endpoint failed: ${e.message}. Trying next endpoint...")
                } catch (e: Exception) {
                    return@withContext Result.failure(e)
                }
            }
        }
        Result.failure(lastException ?: IOException("Failed to verify login code."))
    }

    suspend fun loginWithGoogle(idToken: String): Result<String> = withContext(Dispatchers.IO) {
        if (idToken.isBlank() || idToken.startsWith("google_auth_") || idToken.startsWith("verified_google_auth_")) {
            return@withContext Result.failure(IllegalArgumentException("Google ID Token is missing or invalid. Please sign in with Google again."))
        }
        val endpoints = listOf(BASE_URL, PROXY_BASE_URL)
        var lastException: Exception? = null

        for (endpoint in endpoints) {
            try {
                val body = json.encodeToString(GoogleAuthRequestBody.serializer(), GoogleAuthRequestBody(idToken))
                val request = Request.Builder()
                    .url("$endpoint/auth/google")
                    .post(body.toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    val bodyStr = response.body?.string().orEmpty()
                    if (response.code == 404) {
                        return@withContext Result.failure(AfdsAccountNotFoundException("", "No AFDS account linked to this Google account."))
                    }
                    val parsed = json.decodeFromString(AuthTokenResponse.serializer(), bodyStr)
                    val token = parsed.token
                    if (response.isSuccessful && !token.isNullOrBlank()) {
                        var userEmail = ""
                        var userId = ""
                        var channelId = ""
                        getProfile(token).onSuccess { profile ->
                            userEmail = profile.email.orEmpty()
                            userId = profile.userId.orEmpty()
                            channelId = profile.channelId.orEmpty()
                        }
                        userPreferences.saveAfdsSession(token, userEmail, userId, channelId)
                        return@withContext Result.success(token)
                    } else {
                        lastException = IOException(parsed.error ?: "Google sign-in failed (HTTP ${response.code})")
                    }
                }
            } catch (e: IOException) {
                lastException = e
                Log.w(TAG, "loginWithGoogle call to $endpoint failed: ${e.message}. Trying next endpoint...")
            } catch (e: Exception) {
                return@withContext Result.failure(e)
            }
        }
        Result.failure(lastException ?: IOException("Failed to authenticate with Google."))
    }

    suspend fun getProfile(token: String): Result<AfdsProfileResponse> = withContext(Dispatchers.IO) {
        val endpoints = listOf(BASE_URL, PROXY_BASE_URL)
        var lastException: Exception? = null

        for (endpoint in endpoints) {
            try {
                val request = Request.Builder()
                    .url("$endpoint/profile")
                    .addHeader("Authorization", "Bearer $token")
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    val bodyStr = response.body?.string().orEmpty()
                    if (response.isSuccessful) {
                        val parsed = json.decodeFromString(AfdsProfileResponse.serializer(), bodyStr)
                        if (parsed.dailyLinksLimit != null || parsed.dailyLinksUsed != null) {
                            userPreferences.saveAfdsQuota(
                                linksUsed = parsed.dailyLinksUsed ?: 0,
                                linksLimit = parsed.dailyLinksLimit ?: 100,
                                sendsUsed = parsed.dailySendsUsed ?: 0,
                                sendsLimit = parsed.dailySendsLimit ?: 50
                            )
                        }
                        return@withContext Result.success(parsed)
                    } else {
                        lastException = IOException("Failed to get AFDS profile (HTTP ${response.code})")
                    }
                }
            } catch (e: IOException) {
                lastException = e
                Log.w(TAG, "getProfile call to $endpoint failed: ${e.message}. Trying next endpoint...")
            } catch (e: Exception) {
                return@withContext Result.failure(e)
            }
        }
        Result.failure(lastException ?: IOException("Failed to fetch AFDS profile."))
    }

    suspend fun refreshProfile(): Result<AfdsProfileResponse> = withContext(Dispatchers.IO) {
        val token = userPreferences.afdsAuthToken.firstOrNull()?.trim().orEmpty()
        if (token.isBlank()) return@withContext Result.failure(IllegalStateException("Not logged in to AFDS"))
        getProfile(token)
    }

    suspend fun logout() {
        userPreferences.clearAfdsSession()
    }
}
