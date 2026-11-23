package com.sofianm.antigravity

import com.sofianm.antigravity.data.OAuthDetector
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OAuthDetectorTest {

    @Test
    fun detectsGoogleOAuthEndpoints() {
        assertTrue(OAuthDetector.isGoogleOAuthUrl("https://accounts.google.com/v3/signin/identifier"))
        assertTrue(OAuthDetector.isGoogleOAuthUrl("https://oauth2.googleapis.com/token"))
        assertTrue(OAuthDetector.isGoogleOAuthUrl("https://sub.google.com/o/oauth2/v2/auth"))
    }

    @Test
    fun ignoresStandardWebAppUrls() {
        assertFalse(OAuthDetector.isGoogleOAuthUrl("https://example.com/dashboard"))
        assertFalse(OAuthDetector.isGoogleOAuthUrl("https://github.com/sofianM/Antigravity-android-app"))
        assertFalse(OAuthDetector.isGoogleOAuthUrl("http://localhost:3000"))
    }

    @Test
    fun detectsExternalSchemes() {
        assertTrue(OAuthDetector.isExternalAppScheme("mailto:test@example.com"))
        assertTrue(OAuthDetector.isExternalAppScheme("tel:+31612345678"))
        assertFalse(OAuthDetector.isExternalAppScheme("https://example.com"))
    }
}
