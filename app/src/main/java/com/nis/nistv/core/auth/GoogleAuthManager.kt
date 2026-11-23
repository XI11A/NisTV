package com.nis.nistv.core.auth

import android.accounts.Account
import android.accounts.AccountManager
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.ContactsContract
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.nis.nistv.core.preferences.GoogleUserInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class GoogleAuthManager(
    private val context: Context,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val TAG = "GoogleAuthManager"

        /**
         * AFDS Google Client ID from AFDS-Android (ApiClient.GOOGLE_CLIENT_ID).
         * Used to exchange Google ID Tokens with the AFDS backend (https://tga-hd.api.hashhackers.com/auth/google).
         */
        const val AFDS_GOOGLE_CLIENT_ID = "58094879805-2k4u6f17pfn7fm68kg31fcr4ah7slm0d.apps.googleusercontent.com"
        const val GOOGLE_WEB_CLIENT_ID = AFDS_GOOGLE_CLIENT_ID
        const val GOOGLE_ANDROID_CLIENT_ID = "182486343383-7oobd53pjtqh90iohv9lu36t6rccke8s.apps.googleusercontent.com"

        val ALLOWED_CLIENT_IDS = listOf(
            AFDS_GOOGLE_CLIENT_ID,
            "182486343383-2m99vsslokda8ng8keppok2bsmkihu4v.apps.googleusercontent.com",
            "182486343383-7oobd53pjtqh90iohv9lu36t6rccke8s.apps.googleusercontent.com"
        )
    }

    /**
     * Prompts the user via Android Credential Manager using Google ID Token Option.
     * Matches AFDS-Android GoogleLoginScreen implementation with AFDS_GOOGLE_CLIENT_ID.
     * Returns GoogleUserInfo populated with the genuine OIDC idToken accepted by the AFDS server.
     */
    suspend fun signInWithCredentialManager(activity: Activity): Result<GoogleUserInfo> = withContext(Dispatchers.Main) {
        val credentialManager = CredentialManager.create(activity)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(AFDS_GOOGLE_CLIENT_ID)
            .setAutoSelectEnabled(false)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        try {
            val result = credentialManager.getCredential(activity, request)
            val credential = result.credential
            if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val email = googleIdTokenCredential.id
                val displayName = googleIdTokenCredential.displayName ?: email.substringBefore("@")
                val profilePic = googleIdTokenCredential.profilePictureUri?.toString()
                val given = googleIdTokenCredential.givenName ?: displayName.substringBefore(" ")
                val family = googleIdTokenCredential.familyName ?: displayName.substringAfter(" ", "")

                val sub = "google_" + Math.abs(email.hashCode())
                val finalAvatar = if (!profilePic.isNullOrBlank()) {
                    if (profilePic.contains("=s96-c")) profilePic.replace("=s96-c", "=s256-c") else profilePic
                } else {
                    val encodedName = try { URLEncoder.encode(displayName, "UTF-8") } catch (_: Exception) { "User" }
                    "https://ui-avatars.com/api/?name=$encodedName&background=4285F4&color=ffffff&size=256&bold=true&rounded=true"
                }

                Result.success(
                    GoogleUserInfo(
                        id = sub,
                        name = displayName,
                        givenName = given,
                        familyName = family,
                        email = email,
                        avatarUrl = finalAvatar,
                        idToken = idToken
                    )
                )
            } else {
                Result.failure(Exception("Unsupported credential type: ${credential.type}"))
            }
        } catch (e: GetCredentialCancellationException) {
            Result.failure(e)
        } catch (e: NoCredentialException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Creates native Android account picker intent to choose a Google account on device.
     * Compatible with all Android versions and does not require pre-registered debug SHA-1.
     */
    fun createChooseAccountIntent(): Intent {
        return AccountManager.newChooseAccountIntent(
            null,
            null,
            arrayOf("com.google"),
            null,
            null,
            null,
            null
        )
    }

    /**
     * Requests a genuine Google ID token with audience = AFDS_GOOGLE_CLIENT_ID for an account.
     */
    suspend fun fetchGoogleIdToken(accountName: String, activity: Activity? = null): String? = withContext(Dispatchers.IO) {
        try {
            val account = Account(accountName, "com.google")
            val idTokenScope = "audience:server:client_id:$AFDS_GOOGLE_CLIENT_ID"
            GoogleAuthUtil.getToken(context, account, idTokenScope)
        } catch (e: com.google.android.gms.auth.UserRecoverableAuthException) {
            Log.w(TAG, "User consent required for $accountName: ${e.message}")
            if (activity != null) {
                withContext(Dispatchers.Main) {
                    try {
                        activity.startActivity(e.intent)
                    } catch (_: Exception) {}
                }
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to retrieve Google ID token for $accountName: ${e.message}")
            null
        }
    }

    /**
     * Fetches user profile (First & Last Name, Profile Picture) for selected Google account.
     * 1. Retrieves genuine Google ID Token for AFDS backend verification.
     * 2. Attempts GoogleAuthUtil OAuth userinfo request.
     * 3. Falls back to device ContactsContract.Profile (which syncs primary Google user).
     * 4. Falls back to smart name parsing from the account email.
     */
    suspend fun fetchGoogleUserProfile(accountName: String, activity: Activity? = null): GoogleUserInfo = withContext(Dispatchers.IO) {
        var fetchedGiven: String? = null
        var fetchedFamily: String? = null
        var fetchedName: String? = null
        var fetchedAvatar: String? = null
        var genuineIdToken: String? = null

        // 1. Fetch real Google ID Token with AFDS server client ID
        genuineIdToken = fetchGoogleIdToken(accountName, activity)

        // 2. Try GoogleAuthUtil to fetch OAuth token and call Google UserInfo API
        try {
            val account = Account(accountName, "com.google")
            val scope = "oauth2:https://www.googleapis.com/auth/userinfo.profile https://www.googleapis.com/auth/userinfo.email"
            val token = GoogleAuthUtil.getToken(context, account, scope)
            if (!token.isNullOrBlank()) {
                val req = Request.Builder()
                    .url("https://www.googleapis.com/oauth2/v3/userinfo")
                    .header("Authorization", "Bearer $token")
                    .build()
                val resp = client.newCall(req).execute()
                if (resp.isSuccessful) {
                    val body = resp.body?.string()
                    if (!body.isNullOrBlank()) {
                        val json = JSONObject(body)
                        val name = json.optString("name")
                        val given = json.optString("given_name")
                        val family = json.optString("family_name")
                        val pic = json.optString("picture")
                        if (name.isNotBlank()) fetchedName = name
                        if (given.isNotBlank()) fetchedGiven = given
                        if (family.isNotBlank()) fetchedFamily = family
                        if (pic.isNotBlank()) fetchedAvatar = pic
                    }
                }
            }
        } catch (_: Exception) {}

        // 3. Query device Contact Profile (ContactsContract.Profile)
        if (fetchedAvatar.isNullOrBlank() || fetchedName.isNullOrBlank()) {
            try {
                val cursor = context.contentResolver.query(
                    ContactsContract.Profile.CONTENT_URI,
                    arrayOf(
                        ContactsContract.Profile.DISPLAY_NAME,
                        ContactsContract.Profile.PHOTO_URI
                    ),
                    null, null, null
                )
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIdx = it.getColumnIndex(ContactsContract.Profile.DISPLAY_NAME)
                        val photoIdx = it.getColumnIndex(ContactsContract.Profile.PHOTO_URI)
                        if (nameIdx >= 0 && fetchedName.isNullOrBlank()) {
                            val n = it.getString(nameIdx)
                            if (!n.isNullOrBlank()) fetchedName = n
                        }
                        if (photoIdx >= 0 && fetchedAvatar.isNullOrBlank()) {
                            val p = it.getString(photoIdx)
                            if (!p.isNullOrBlank()) fetchedAvatar = p
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // 4. Fallback to smart parsing of first and last name from account email
        val parsedParts = accountName.substringBefore("@")
            .replace(Regex("[0-9]"), "")
            .split(".", "_", "-")
            .filter { it.isNotBlank() }

        val finalGiven: String
        val finalFamily: String
        val finalName: String

        if (!fetchedName.isNullOrBlank()) {
            finalName = fetchedName!!
            finalGiven = fetchedGiven ?: fetchedName!!.substringBefore(" ")
            finalFamily = fetchedFamily ?: fetchedName!!.substringAfter(" ", "")
        } else if (parsedParts.isNotEmpty()) {
            finalGiven = parsedParts.first().replaceFirstChar { it.uppercase() }
            finalFamily = if (parsedParts.size > 1) {
                parsedParts.drop(1).joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            } else {
                ""
            }
            finalName = if (finalFamily.isNotBlank()) "$finalGiven $finalFamily" else finalGiven
        } else {
            finalGiven = "Google"
            finalFamily = "User"
            finalName = "Google User"
        }

        // 5. Format avatar URL (scaling up Google CDN photos if present)
        val finalAvatar = if (!fetchedAvatar.isNullOrBlank()) {
            val raw = fetchedAvatar!!
            if (raw.contains("=s96-c")) raw.replace("=s96-c", "=s256-c") else raw
        } else {
            val encodedName = try { URLEncoder.encode(finalName, "UTF-8") } catch (_: Exception) { "User" }
            "https://ui-avatars.com/api/?name=$encodedName&background=4285F4&color=ffffff&size=256&bold=true&rounded=true"
        }

        val sub = "google_" + Math.abs(accountName.hashCode())
        GoogleUserInfo(
            id = sub,
            name = finalName,
            givenName = finalGiven,
            familyName = finalFamily,
            email = accountName.trim(),
            avatarUrl = finalAvatar,
            idToken = genuineIdToken ?: "",
            isDemo = false,
            demoExpiresAt = 0L
        )
    }

    fun handleAccountSelected(accountName: String): GoogleUserInfo {
        return createVerifiedGoogleUser(email = accountName)
    }

    fun createVerifiedGoogleUser(
        email: String,
        firstName: String? = null,
        lastName: String? = null,
        avatarUrl: String? = null,
        idToken: String? = null
    ): GoogleUserInfo {
        val cleanFirst = firstName?.trim().orEmpty()
        val cleanLast = lastName?.trim().orEmpty()

        val parsedFirst: String
        val parsedLast: String
        val fullName: String

        if (cleanFirst.isNotBlank()) {
            parsedFirst = cleanFirst
            parsedLast = cleanLast
            fullName = if (cleanLast.isNotBlank()) "$cleanFirst $cleanLast" else cleanFirst
        } else {
            val parts = email.substringBefore("@")
                .replace(Regex("[0-9]"), "")
                .split(".", "_", "-")
                .filter { it.isNotBlank() }
            parsedFirst = parts.firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "Google"
            parsedLast = if (parts.size > 1) parts.drop(1).joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } } else ""
            fullName = if (parsedLast.isNotBlank()) "$parsedFirst $parsedLast" else parsedFirst
        }

        val sub = "google_" + Math.abs(email.hashCode())
        val encodedName = try { URLEncoder.encode(fullName, "UTF-8") } catch (_: Exception) { "User" }
        val finalAvatar = if (!avatarUrl.isNullOrBlank()) {
            avatarUrl
        } else {
            "https://ui-avatars.com/api/?name=$encodedName&background=4285F4&color=ffffff&size=256&bold=true&rounded=true"
        }

        return GoogleUserInfo(
            id = sub,
            name = fullName,
            givenName = parsedFirst,
            familyName = parsedLast,
            email = email.trim(),
            avatarUrl = finalAvatar,
            idToken = idToken.orEmpty()
        )
    }

    suspend fun verifyGoogleIdToken(idToken: String): Result<GoogleUserInfo> = withContext(Dispatchers.IO) {
        try {
            val url = "https://oauth2.googleapis.com/tokeninfo?id_token=$idToken"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Google token verification failed (HTTP ${response.code})"))
            }

            val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response from Google tokeninfo"))
            val json = JSONObject(body)

            val aud = json.optString("aud")
            if (!ALLOWED_CLIENT_IDS.contains(aud)) {
                return@withContext Result.failure(Exception("Token audience does not match configured Google Client IDs ($aud)"))
            }

            val sub = json.optString("sub", "google_user_${System.currentTimeMillis()}")
            val email = json.optString("email", "")
            val given = json.optString("given_name", "")
            val family = json.optString("family_name", "")
            val name = json.optString("name", listOfNotNull(given.takeIf { it.isNotBlank() }, family.takeIf { it.isNotBlank() }).joinToString(" "))
            val picture = json.optString("picture", "")

            Result.success(
                GoogleUserInfo(
                    id = sub,
                    name = name,
                    givenName = given,
                    familyName = family,
                    email = email,
                    avatarUrl = picture,
                    idToken = idToken
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getOAuthUrl(redirectUri: String = "https://afds.hashhackers.com/login.html"): String {
        val encodedClientId = URLEncoder.encode(AFDS_GOOGLE_CLIENT_ID, "UTF-8")
        val encodedRedirect = URLEncoder.encode(redirectUri, "UTF-8")
        val encodedScope = URLEncoder.encode("openid email profile", "UTF-8")
        val nonce = System.currentTimeMillis().toString(36)

        return "https://accounts.google.com/o/oauth2/v2/auth?" +
                "client_id=$encodedClientId" +
                "&redirect_uri=$encodedRedirect" +
                "&response_type=token%20id_token" +
                "&scope=$encodedScope" +
                "&nonce=$nonce" +
                "&prompt=select_account"
    }

    fun signOut(onComplete: () -> Unit = {}) {
        onComplete()
    }

    fun createDemoUser(): GoogleUserInfo {
        val now = System.currentTimeMillis()
        val expiresAt = now + 60_000L // 1 minute (60 seconds) grace period
        return GoogleUserInfo(
            id = "demo_guest_$now",
            name = "Demo Guest",
            givenName = "Demo",
            familyName = "Guest",
            email = "demo.preview@nistv.app",
            avatarUrl = "https://ui-avatars.com/api/?name=Demo+Guest&background=6366F1&color=ffffff&size=256&bold=true&rounded=true",
            idToken = "demo_token_$now",
            isDemo = true,
            demoExpiresAt = expiresAt
        )
    }
}
