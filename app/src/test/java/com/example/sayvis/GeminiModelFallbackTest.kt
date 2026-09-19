package com.example.sayvis

import com.example.sayvis.ai.GeminiProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** v5.0.1 — Google retired gemini-2.5-flash for new users; the provider must fall forward. */
class GeminiModelFallbackTest {

    @Test
    fun `chain starts with the requested model then live fallbacks`() {
        assertEquals(
            listOf("gemini-3.6-flash"),
            GeminiProvider.modelFallbackChain("gemini-3.6-flash")
        )
        assertEquals(
            listOf("gemini-2.5-flash", "gemini-3.6-flash", "gemini-flash-latest", "gemini-2.0-flash"),
            GeminiProvider.modelFallbackChain("gemini-2.5-flash")
        )
        assertEquals(
            listOf(
                GeminiProvider.DEFAULT_MODEL,
                "gemini-flash-latest",
                "gemini-2.0-flash"
            ),
            GeminiProvider.modelFallbackChain("")
        )
    }

    @Test
    fun `retired model errors are recognised`() {
        val real = "HTTP 404: This model models/gemini-2.5-flash is no longer available to new users."
        assertTrue(GeminiProvider.isModelRetiredError(real))
        assertTrue(GeminiProvider.isModelRetiredError("HTTP 404: models/gemini-9.9 is not found for api version v1beta"))
        assertFalse(GeminiProvider.isModelRetiredError("HTTP 403: location restriction — VPN needed"))
        assertFalse(GeminiProvider.isModelRetiredError("HTTP 400: API key not valid"))
        assertFalse(GeminiProvider.isModelRetiredError("timeout"))
    }

    @Test
    fun `default model moved off the retired alias`() {
        assertFalse(GeminiProvider.DEFAULT_MODEL.startsWith("gemini-2.5"))
        assertTrue(GeminiProvider.FALLBACK_MODELS.contains(GeminiProvider.DEFAULT_MODEL))
    }
}
