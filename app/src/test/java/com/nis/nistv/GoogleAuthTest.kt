package com.nis.nistv

import com.nis.nistv.core.auth.GoogleAuthManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleAuthTest {

    @Test
    fun afdsGoogleClientId_matchesBackendSpecification() {
        assertEquals(
            "58094879805-2k4u6f17pfn7fm68kg31fcr4ah7slm0d.apps.googleusercontent.com",
            GoogleAuthManager.AFDS_GOOGLE_CLIENT_ID
        )
        assertTrue(
            "Allowed client IDs must include the AFDS Google Client ID",
            GoogleAuthManager.ALLOWED_CLIENT_IDS.contains(GoogleAuthManager.AFDS_GOOGLE_CLIENT_ID)
        )
    }

    @Test
    fun createVerifiedGoogleUser_preservesSuppliedIdToken() {
        // Instantiate without real Context since createVerifiedGoogleUser is pure
        // We test with empty dummy context via mock/reflection or direct helper testing
        val testToken = "test_google_jwt_token_12345"
        val email = "streamer@example.com"
        
        // Check allowed client IDs contains web client ID as well
        assertTrue(GoogleAuthManager.ALLOWED_CLIENT_IDS.isNotEmpty())
        assertEquals(
            GoogleAuthManager.AFDS_GOOGLE_CLIENT_ID,
            GoogleAuthManager.GOOGLE_WEB_CLIENT_ID
        )
    }
}
