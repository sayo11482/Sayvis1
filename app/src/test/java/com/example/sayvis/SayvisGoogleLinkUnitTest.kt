package com.example.sayvis

import com.example.sayvis.ai.GoogleLinkManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the pure side of the Google-account link flow: pulling a Gemini key
 * out of arbitrary shared/selected text (AI Studio share sheet, text
 * selection, clipboard), classifying & redacting it, and recognising Google's
 * regional 403 wording.
 */
class SayvisGoogleLinkUnitTest {

    @Test
    fun `extractor finds AQ auth keys inside persian share text`() {
        val text = "کلید شما ساخته شد:\nAQ.Ab8Rf9k2LmN0pQrStUvWxYz1234567890abcd\nموفق باشید!"
        val key = GoogleLinkManager.extractKeyFromText(text)
        assertEquals("AQ.Ab8Rf9k2LmN0pQrStUvWxYz1234567890abcd", key)
        assertEquals(GoogleLinkManager.KeyKind.AUTH_AQ, GoogleLinkManager.classify(key!!))
    }

    @Test
    fun `extractor finds classic AIza keys`() {
        val text = "Your key: AIzaSyA1234567890abcdefghijklmnopqrstuv (keep it secret)"
        val key = GoogleLinkManager.extractKeyFromText(text)
        assertEquals("AIzaSyA1234567890abcdefghijklmnopqrstuv", key)
        assertEquals(GoogleLinkManager.KeyKind.STANDARD_AIZA, GoogleLinkManager.classify(key!!))
    }

    @Test
    fun `extractor rejects garbage and short fragments`() {
        assertNull(GoogleLinkManager.extractKeyFromText(""))
        assertNull(GoogleLinkManager.extractKeyFromText("AQ.short123"))
        assertNull(GoogleLinkManager.extractKeyFromText("key: AIza123"))
        assertNull(GoogleLinkManager.extractKeyFromText("امروز هوا آفتابی است و بازار باز"))
    }

    @Test
    fun `redaction never reveals the middle of the key`() {
        val key = "AQ.Ab8Rf9k2LmN0pQrStUvWxYzSecretMiddle1234wxyz"
        val shown = GoogleLinkManager.redact(key)
        assertTrue(shown.startsWith("AQ.Ab8R"))
        assertTrue(shown.endsWith("wxyz"))
        assertTrue(shown.contains("…"))
        assertFalse(shown.contains("SecretMiddle"))
    }

    @Test
    fun `geo detection follows google 403 wording`() {
        assertTrue(
            GoogleLinkManager.geoBlockedBody(
                "\"error\": \"User location is not supported for the API use.\""
            )
        )
        assertFalse(
            GoogleLinkManager.geoBlockedBody(
                "{\"error\": \"API key not valid. Please pass a valid API key.\"}"
            )
        )
    }
}
