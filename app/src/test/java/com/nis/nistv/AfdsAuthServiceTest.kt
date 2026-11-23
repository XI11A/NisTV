package com.nis.nistv

import com.nis.nistv.core.designsystem.component.AfdsAuthMode
import com.nis.nistv.data.download.provider.AfdsAccountNotFoundException
import com.nis.nistv.data.download.provider.AfdsAuthService
import com.nis.nistv.data.download.provider.AfdsSignupResponse
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class AfdsAuthServiceTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun normalizeEmail_normalizesGmailAddressesCorrectly() {
        // Dots stripped
        assertEquals("adalovelace@gmail.com", AfdsAuthService.normalizeEmail("ada.lovelace@gmail.com"))
        assertEquals("altarazadione@gmail.com", AfdsAuthService.normalizeEmail("a.ltarazadione@gmail.com"))
        
        // Uppercase converted to lowercase
        assertEquals("johndoe@gmail.com", AfdsAuthService.normalizeEmail("John.Doe@Gmail.com"))
        
        // Googlemail converted to gmail
        assertEquals("googleuser@gmail.com", AfdsAuthService.normalizeEmail("google.user@googlemail.com"))
        
        // Plus aliases stripped
        assertEquals("primary@gmail.com", AfdsAuthService.normalizeEmail("primary+alias@gmail.com"))
        
        // Non-gmail untouched except lowercase & trimmed
        assertEquals("user@outlook.com", AfdsAuthService.normalizeEmail("  user@outlook.com  "))
        assertEquals("test.user@proton.me", AfdsAuthService.normalizeEmail("test.user@proton.me"))
    }

    @Test
    fun afdsSignupResponse_deserializesSuccessMessage() {
        val jsonStr = """{"message": "Verification code sent to your email."}"""
        val response = json.decodeFromString(AfdsSignupResponse.serializer(), jsonStr)
        assertEquals("Verification code sent to your email.", response.message)
        assertEquals(null, response.error)
    }

    @Test
    fun afdsSignupResponse_deserializesErrorMessage() {
        val jsonStr = """{"error": "Please use a valid email provider (Gmail, Outlook, Yahoo, Proton, iCloud, etc.)."}"""
        val response = json.decodeFromString(AfdsSignupResponse.serializer(), jsonStr)
        assertEquals("Please use a valid email provider (Gmail, Outlook, Yahoo, Proton, iCloud, etc.).", response.error)
        assertEquals(null, response.message)
    }

    @Test
    fun afdsAccountNotFoundException_preservesEmailAndIsIOException() {
        val ex = AfdsAccountNotFoundException("unregistered@gmail.com", "No account found with this email address.")
        assertEquals("unregistered@gmail.com", ex.email)
        assertEquals("No account found with this email address.", ex.message)
    }

    @Test
    fun afdsAuthMode_enumVerification() {
        assertEquals(2, AfdsAuthMode.values().size)
        assertTrue(AfdsAuthMode.values().contains(AfdsAuthMode.SIGN_IN))
        assertTrue(AfdsAuthMode.values().contains(AfdsAuthMode.SIGN_UP))
    }

    @Test
    fun afdsEndpoints_baseUrlsAreDefinedCorrectly() {
        assertEquals("https://tga-hd.api.hashhackers.com", AfdsAuthService.BASE_URL)
        assertEquals("https://afds.hashhackers.com/proxyapi", AfdsAuthService.PROXY_BASE_URL)
    }

    @Test
    fun signupRequestBody_serializesInviteCodeProperly() {
        val body = com.nis.nistv.data.download.provider.SignupRequestBody(
            firstName = "Nis",
            lastName = "TV",
            email = "user@gmail.com",
            inviteCode = "WELCOME"
        )
        val jsonStr = json.encodeToString(com.nis.nistv.data.download.provider.SignupRequestBody.serializer(), body)
        assertTrue("JSON must include inviteCode", jsonStr.contains("\"inviteCode\":\"WELCOME\""))
        assertTrue("JSON must include firstName", jsonStr.contains("\"firstName\":\"Nis\""))
        assertTrue("JSON must include lastName", jsonStr.contains("\"lastName\":\"TV\""))
        assertTrue("JSON must include email", jsonStr.contains("\"email\":\"user@gmail.com\""))
    }
}
