package com.nis.nistv.core.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object ParentalPinHelper {

    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 10000
    private const val KEY_LENGTH = 256
    private const val SALT_LENGTH_BYTES = 16
    const val MAX_ATTEMPTS = 5
    const val LOCKOUT_DURATION_MS = 5 * 60 * 1000L // 5 minutes

    fun generateSalt(): String {
        val salt = ByteArray(SALT_LENGTH_BYTES)
        SecureRandom().nextBytes(salt)
        return Base64Compat.encodeToString(salt)
    }

    fun hashPin(pin: String, saltBase64: String): String {
        val salt = Base64Compat.decode(saltBase64)
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        val hash = factory.generateSecret(spec).encoded
        return Base64Compat.encodeToString(hash)
    }

    fun verifyPin(enteredPin: String, storedHash: String, saltBase64: String): Boolean {
        if (enteredPin.isBlank() || storedHash.isBlank() || saltBase64.isBlank()) return false
        val computedHash = hashPin(enteredPin, saltBase64)
        return MessageDigest.isEqual(
            computedHash.toByteArray(Charsets.UTF_8),
            storedHash.toByteArray(Charsets.UTF_8)
        )
    }

    /**
     * Calculates remaining lockout milliseconds. Returns 0 if not locked out.
     */
    fun calculateLockoutRemainingMs(lockoutUntilMs: Long, currentTimeMs: Long = System.currentTimeMillis()): Long {
        val diff = lockoutUntilMs - currentTimeMs
        return if (diff > 0) diff else 0L
    }

    /**
     * Determines whether an attempt should trigger lockout or increment attempt counter.
     * Returns Pair(newAttemptsCount, newLockoutUntilMs).
     */
    fun onFailedAttempt(
        currentAttempts: Int,
        currentTimeMs: Long = System.currentTimeMillis()
    ): Pair<Int, Long> {
        val nextAttempts = currentAttempts + 1
        return if (nextAttempts >= MAX_ATTEMPTS) {
            Pair(0, currentTimeMs + LOCKOUT_DURATION_MS)
        } else {
            Pair(nextAttempts, 0L)
        }
    }
}
