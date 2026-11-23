package com.nis.nistv.core.security

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SecureTokenStore(
    context: Context,
    private val cipher: TokenCipher = try {
        AndroidKeystoreCipher()
    } catch (_: Throwable) {
        AesGcmCipher(AesGcmCipher.generateRandomKey())
    }
) {
    companion object {
        private const val PREFS_NAME = "nistv_secure_store"
        private const val KEY_AFDS_TOKEN_ENC = "afds_auth_token_enc"
        private const val KEY_GOOGLE_ID_TOKEN_ENC = "google_id_token_enc"
        private const val KEY_MIGRATION_COMPLETED = "token_migration_completed"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _afdsTokenFlow = MutableStateFlow(getAfdsToken())
    val afdsTokenFlow: StateFlow<String> = _afdsTokenFlow.asStateFlow()

    private val _googleIdTokenFlow = MutableStateFlow(getGoogleIdToken())
    val googleIdTokenFlow: StateFlow<String> = _googleIdTokenFlow.asStateFlow()

    fun getAfdsToken(): String {
        val encrypted = prefs.getString(KEY_AFDS_TOKEN_ENC, null) ?: return ""
        return cipher.decrypt(encrypted)
    }

    fun saveAfdsToken(token: String) {
        if (token.isBlank()) {
            clearAfdsToken()
            return
        }
        val encrypted = cipher.encrypt(token)
        prefs.edit().putString(KEY_AFDS_TOKEN_ENC, encrypted).apply()
        _afdsTokenFlow.value = token
    }

    fun clearAfdsToken() {
        prefs.edit().remove(KEY_AFDS_TOKEN_ENC).apply()
        _afdsTokenFlow.value = ""
    }

    fun getGoogleIdToken(): String {
        val encrypted = prefs.getString(KEY_GOOGLE_ID_TOKEN_ENC, null) ?: return ""
        return cipher.decrypt(encrypted)
    }

    fun saveGoogleIdToken(token: String) {
        if (token.isBlank()) {
            clearGoogleIdToken()
            return
        }
        val encrypted = cipher.encrypt(token)
        prefs.edit().putString(KEY_GOOGLE_ID_TOKEN_ENC, encrypted).apply()
        _googleIdTokenFlow.value = token
    }

    fun clearGoogleIdToken() {
        prefs.edit().remove(KEY_GOOGLE_ID_TOKEN_ENC).apply()
        _googleIdTokenFlow.value = ""
    }

    fun clearAllTokens() {
        prefs.edit()
            .remove(KEY_AFDS_TOKEN_ENC)
            .remove(KEY_GOOGLE_ID_TOKEN_ENC)
            .apply()
        _afdsTokenFlow.value = ""
        _googleIdTokenFlow.value = ""
    }

    fun isMigrationCompleted(): Boolean =
        prefs.getBoolean(KEY_MIGRATION_COMPLETED, false)

    fun markMigrationCompleted() {
        prefs.edit().putBoolean(KEY_MIGRATION_COMPLETED, true).apply()
    }

    /**
     * Migrates legacy plaintext tokens into encrypted keystore storage on first run.
     * Returns true if a migration was performed.
     */
    fun migratePlaintextTokens(plaintextAfds: String?, plaintextGoogle: String?): Boolean {
        if (isMigrationCompleted()) return false

        var migrated = false
        if (!plaintextAfds.isNullOrBlank() && getAfdsToken().isBlank()) {
            saveAfdsToken(plaintextAfds)
            migrated = true
        }
        if (!plaintextGoogle.isNullOrBlank() && getGoogleIdToken().isBlank()) {
            saveGoogleIdToken(plaintextGoogle)
            migrated = true
        }

        markMigrationCompleted()
        return migrated
    }
}
