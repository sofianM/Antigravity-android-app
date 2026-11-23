package com.sofianm.antigravity

import com.sofianm.antigravity.data.UrlValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UrlValidatorTest {

    @Test
    fun normalizeUrl_addsHttpsForPublicDomain() {
        assertEquals("https://antigravity.example.com", UrlValidator.normalizeUrl("antigravity.example.com"))
        assertEquals("https://antigravity.example.com/app", UrlValidator.normalizeUrl("  antigravity.example.com/app  "))
    }

    @Test
    fun normalizeUrl_usesHttpForLocalAndLanHosts() {
        assertEquals("http://localhost:3000", UrlValidator.normalizeUrl("localhost:3000"))
        assertEquals("http://192.168.1.42:8080/chat", UrlValidator.normalizeUrl("192.168.1.42:8080/chat"))
        assertEquals("http://10.0.0.5", UrlValidator.normalizeUrl("10.0.0.5"))
        assertEquals("http://devbox.local:5173", UrlValidator.normalizeUrl("devbox.local:5173"))
    }

    @Test
    fun normalizeUrl_rejectsUnsafeAndInvalidInputs() {
        assertNull(UrlValidator.normalizeUrl(""))
        assertNull(UrlValidator.normalizeUrl("   "))
        assertNull(UrlValidator.normalizeUrl("javascript:alert(1)"))
        assertNull(UrlValidator.normalizeUrl("file:///etc/passwd"))
        assertNull(UrlValidator.normalizeUrl("data:text/html,<h1>x</h1>"))
        assertNull(UrlValidator.normalizeUrl("not a domain"))
    }

    @Test
    fun extractUrlFromQrPayload_handlesRawJsonAndText() {
        assertEquals(
            "https://app.example.com/workspace",
            UrlValidator.extractUrlFromQrPayload("https://app.example.com/workspace")
        )
        assertEquals(
            "https://antigravity.example.com/s/123",
            UrlValidator.extractUrlFromQrPayload("""{"name":"Antigravity","url":"https://antigravity.example.com/s/123"}""")
        )
        assertEquals(
            "https://portal.example.com/open",
            UrlValidator.extractUrlFromQrPayload("Open dit op je telefoon: https://portal.example.com/open.")
        )
        assertNull(UrlValidator.extractUrlFromQrPayload("WIFI:T:WPA;S:MyNetwork;P:secret;;"))
    }

    @Test
    fun suggestProfileName_extractsCleanName() {
        assertEquals("Antigravity", UrlValidator.suggestProfileName("https://antigravity.google.com/chat"))
        assertEquals("Portal", UrlValidator.suggestProfileName("https://www.portal.example.com/home"))
    }
}
