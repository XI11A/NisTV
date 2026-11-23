package com.nis.nistv.core.security

/**
 * JVM and Android compatible Base64 encoder/decoder.
 * Prioritizes standard java.util.Base64 (available in JVM tests and Android API 26+),
 * with fallback to android.util.Base64 for Android API 24-25.
 */
object Base64Compat {

    fun encodeToString(bytes: ByteArray): String {
        return try {
            java.util.Base64.getEncoder().encodeToString(bytes)
        } catch (_: Throwable) {
            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        }
    }

    fun decode(base64Str: String): ByteArray {
        return try {
            java.util.Base64.getDecoder().decode(base64Str)
        } catch (_: Throwable) {
            android.util.Base64.decode(base64Str, android.util.Base64.NO_WRAP)
        }
    }
}
