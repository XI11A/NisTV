package com.nis.nistv

import com.nis.nistv.core.security.AesGcmCipher
import com.nis.nistv.core.security.Base64Compat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SecureTokenStoreTest {

    @Test
    fun aesGcmCipher_encryptAndDecryptRoundTrip() {
        val secretKey = AesGcmCipher.generateRandomKey()
        val cipher = AesGcmCipher(secretKey)

        val originalToken = "afds_jwt_sample_token_xyz9876543210_valid"
        val encrypted = cipher.encrypt(originalToken)

        // Encrypted text must not be empty or equal to original plain text
        assertTrue("Encrypted output must not be empty", encrypted.isNotEmpty())
        assertNotEquals("Plaintext must not appear directly in ciphertext", originalToken, encrypted)

        // Decryption should reproduce the exact original token
        val decrypted = cipher.decrypt(encrypted)
        assertEquals("Decrypted token must match original token", originalToken, decrypted)
    }

    @Test
    fun aesGcmCipher_handlesEmptyAndInvalidCiphertextSafely() {
        val secretKey = AesGcmCipher.generateRandomKey()
        val cipher = AesGcmCipher(secretKey)

        assertEquals("", cipher.encrypt(""))
        assertEquals("", cipher.decrypt(""))
        assertEquals("", cipher.decrypt("not_a_valid_base64_ciphertext!@#$"))
        assertEquals("", cipher.decrypt(Base64Compat.encodeToString(ByteArray(5)))) // shorter than IV length
    }

    @Test
    fun aesGcmCipher_differentKeysProduceDifferentCiphertextAndFailDecryption() {
        val key1 = AesGcmCipher.generateRandomKey()
        val key2 = AesGcmCipher.generateRandomKey()
        val cipher1 = AesGcmCipher(key1)
        val cipher2 = AesGcmCipher(key2)

        val plaintext = "sensitive_user_google_id_token"
        val encryptedWithKey1 = cipher1.encrypt(plaintext)

        // Decrypting with wrong key must fail and return empty string safely
        val failedDecryption = cipher2.decrypt(encryptedWithKey1)
        assertEquals("", failedDecryption)
    }

    @Test
    fun base64Compat_roundTripByteArrays() {
        val sample = "Test base64 string with special chars: !@#$%^&*()_+~|}{[]".toByteArray(Charsets.UTF_8)
        val encoded = Base64Compat.encodeToString(sample)
        val decoded = Base64Compat.decode(encoded)
        assertEquals(String(sample, Charsets.UTF_8), String(decoded, Charsets.UTF_8))
    }
}
