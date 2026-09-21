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
        val forCurrent = GeminiProvider.modelFallbackChain("gemini-3.6-flash")
        assertTrue(
            "chain(current)=" + chainText(forCurrent),
            forCurrent.first() == "gemini-3.6-flash" && forCurrent.size >= 1 &&
                forCurrent.toSet().size == forCurrent.size
        )

        val forRetired = GeminiProvider.modelFallbackChain("gemini-2.5-flash")
        assertTrue(
            "chain(retired)=" + chainText(forRetired),
            forRetired.first() == "gemini-2.5-flash" &&
                forRetired.contains("gemini-3.6-flash") &&
                forRetired.contains("gemini-flash-latest") &&
                forRetired.contains("gemini-2.0-flash") &&
                forRetired.indexOf("gemini-3.6-flash") < forRetired.indexOf("gemini-flash-latest") &&
                forRetired.indexOf("gemini-flash-latest") < forRetired.indexOf("gemini-2.0-flash") &&
                forRetired.toSet().size == forRetired.size
        )

        val forBlank = GeminiProvider.modelFallbackChain("")
        assertTrue(
            "chain(blank)=" + chainText(forBlank),
            forBlank.first() == GeminiProvider.DEFAULT_MODEL
        )
        assertEquals("gemini-3.6-flash", GeminiProvider.DEFAULT_MODEL)
    }

    @Test
    fun `retired model errors are recognised`() {
        val real = "HTTP 404: This model models/gemini-2.5-flash is no longer available to new users."
        assertTrue(
            "retired(real)=" + real,
            GeminiProvider.isModelRetiredError(real)
        )
        val notFound = "HTTP 404: models/gemini-9.9 is not found for api version v1beta"
        assertTrue(
            "retired(notFound)=" + notFound,
            GeminiProvider.isModelRetiredError(notFound)
        )
        assertFalse(
            "region-block must not be treated as retired",
            GeminiProvider.isModelRetiredError("HTTP 403: location restriction — VPN needed")
        )
        assertFalse(
            "bad key must not be treated as retired",
            GeminiProvider.isModelRetiredError("HTTP 400: API key not valid")
        )
        assertFalse(
            "timeout must not be treated as retired",
            GeminiProvider.isModelRetiredError("timeout")
        )
    }

    @Test
    fun `default model moved off the retired alias`() {
        assertFalse(GeminiProvider.DEFAULT_MODEL.startsWith("gemini-2.5"))
        assertTrue(GeminiProvider.FALLBACK_MODELS.contains(GeminiProvider.DEFAULT_MODEL))
    }

    private fun chainText(list: List<String>): String = "[" + list.joinToString(",") + "]"
}
