package com.nis.nistv.core.security

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

interface TokenCipher {
    fun encrypt(plainText: String): String
    fun decrypt(cipherText: String): String
}

/**
 * Standard AES-GCM Cipher using a provided SecretKey.
 * Suitable for both JVM unit testing and Android runtime.
 */
class AesGcmCipher(
    private val secretKey: SecretKey
) : TokenCipher {

    companion object {
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_LENGTH_BYTES = 12
        private const val TAG_LENGTH_BITS = 128

        fun generateRandomKey(): SecretKey {
            val keyBytes = ByteArray(32) // 256-bit AES
            SecureRandom().nextBytes(keyBytes)
            return SecretKeySpec(keyBytes, "AES")
        }
    }

    override fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val iv = ByteArray(IV_LENGTH_BYTES).also { SecureRandom().nextBytes(it) }
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(TAG_LENGTH_BITS, iv))

        val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + cipherBytes.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherBytes, 0, combined, iv.size, cipherBytes.size)

        return Base64Compat.encodeToString(combined)
    }

    override fun decrypt(cipherText: String): String {
        if (cipherText.isEmpty()) return ""
        return try {
            val combined = Base64Compat.decode(cipherText)
            if (combined.size <= IV_LENGTH_BYTES) return ""

            val iv = combined.copyOfRange(0, IV_LENGTH_BYTES)
            val encryptedBytes = combined.copyOfRange(IV_LENGTH_BYTES, combined.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(TAG_LENGTH_BITS, iv))
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (_: Exception) {
            ""
        }
    }
}

/**
 * Android Keystore backed AES-GCM Cipher.
 * Stores and protects the 256-bit AES key in hardware-backed keystore on Android.
 */
class AndroidKeystoreCipher(
    private val keyAlias: String = "nistv_token_key"
) : TokenCipher {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_LENGTH_BYTES = 12
        private const val TAG_LENGTH_BITS = 128
    }

    private val secretKey: SecretKey by lazy {
        getOrCreateSecretKey()
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (!keyStore.containsAlias(keyAlias)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            val spec = KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()

            keyGenerator.init(spec)
            return keyGenerator.generateKey()
        }
        val entry = keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry
            ?: throw IllegalStateException("KeyStore entry is not SecretKeyEntry")
        return entry.secretKey
    }

    override fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        val combined = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)

        return Base64Compat.encodeToString(combined)
    }

    override fun decrypt(cipherText: String): String {
        if (cipherText.isEmpty()) return ""
        return try {
            val combined = Base64Compat.decode(cipherText)
            if (combined.size <= IV_LENGTH_BYTES) return ""

            val iv = combined.copyOfRange(0, IV_LENGTH_BYTES)
            val encryptedBytes = combined.copyOfRange(IV_LENGTH_BYTES, combined.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(TAG_LENGTH_BITS, iv))
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (_: Exception) {
            ""
        }
    }
}
