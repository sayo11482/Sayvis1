package com.example.sayvis

import com.example.sayvis.ai.AgentService
import com.example.sayvis.ai.AssistantCommandEngine
import com.example.sayvis.ai.GoogleAuthManager
import com.example.sayvis.ai.GoogleServicesService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the pure logic of the Google OAuth sign-in, the Google capability
 * router and the research agent: PKCE + authorize URL, id_token profile
 * parsing, base64url codec, agent triggers, page-paragraph picking, the
 * extractive fallback answer and readable-text extraction.
 */
class SayvisAgentGoogleUnitTest {

    // ------------------------------------------------------------ base64url

    @Test
    fun `base64url round trip survives jwt alphabet`() {
        val samples = listOf("", "f", "fo", "foo", "foob", "fooba", "foobar", "{\"email\":\"a@b.c\",\"name\":\"علی\"}")
        for (sample in samples) {
            val encoded = GoogleAuthManager.base64UrlEncode(sample.toByteArray(Charsets.UTF_8))
            assertFalse(encoded.contains('+') || encoded.contains('/') || encoded.contains('='))
            assertEquals(sample, String(GoogleAuthManager.base64UrlDecode(encoded)!!, Charsets.UTF_8))
        }
    }

    @Test
    fun `authorize url carries pkce and read only scopes`() {
        val verifier = GoogleAuthManager.generateVerifier()
        val url = GoogleAuthManager.buildAuthorizeUrl("1234.apps.googleusercontent.com", verifier, "state123")
        assertTrue(url.startsWith("https://accounts.google.com/o/oauth2/v2/auth?"))
        assertTrue(url.contains("client_id=1234.apps.googleusercontent.com"))
        assertTrue(url.contains("response_type=code"))
        assertTrue(url.contains("code_challenge_method=S256"))
        assertTrue(url.contains("redirect_uri=sayvis%3A%2F%2Foauth2"))
        assertTrue(url.contains("access_type=offline"))
        assertTrue(url.contains("gmail.readonly"))
        assertTrue(url.contains("calendar.readonly"))
        assertTrue(url.contains("drive.readonly"))
        // challenge is BASE64URL(SHA256(verifier)) — 43 chars, no padding
        val challenge = url.substringAfter("code_challenge=").substringBefore("&")
        assertEquals(43, challenge.length)
        assertEquals(GoogleAuthManager.codeChallenge(verifier), challenge)
        assertTrue(verifier.length in 43..128)
    }

    @Test
    fun `id token payload decodes into profile`() {
        val header = GoogleAuthManager.base64UrlEncode("{\"alg\":\"RS256\"}".toByteArray())
        val payload = GoogleAuthManager.base64UrlEncode(
            ("{\"email\":\"owner@gmail.com\",\"name\":\"Sara Ahmadi\"," +
                "\"picture\":\"https://lh3.googleusercontent.com/a/x.jpg\"}").toByteArray()
        )
        val profile = GoogleAuthManager.parseIdToken("$header.$payload.sig")
        assertEquals("owner@gmail.com", profile!!.email)
        assertEquals("Sara Ahmadi", profile.name)
        assertTrue(profile.picture.startsWith("https://lh3"))
        assertNull(GoogleAuthManager.parseIdToken("not-a-jwt"))
    }

    // ------------------------------------------------- google capability route

    @Test
    fun `capability router maps persian and english intents`() {
        assertEquals(GoogleServicesService.Capability.GMAIL, GoogleServicesService.matchCapability("ایمیل‌های اخیرم را نشان بده"))
        assertEquals(GoogleServicesService.Capability.CALENDAR, GoogleServicesService.matchCapability("رویدادهای تقویم هفته"))
        assertEquals(GoogleServicesService.Capability.DRIVE, GoogleServicesService.matchCapability("فایل‌هایم در درایو چیست"))
        assertEquals(GoogleServicesService.Capability.GMAIL, GoogleServicesService.matchCapability("check my gmail inbox"))
        assertNull(GoogleServicesService.matchCapability("قیمت طلا چند است"))
    }

    // ------------------------------------------------------------ agent brain

    @Test
    fun `agent triggers fire on explicit commands with slot extraction`() {
        assertEquals("قیمت طلا", AgentService.triggerGoal(AssistantCommandEngine.normalize("ایجنت: قیمت طلا")))
        assertEquals("weather in berlin", AgentService.triggerGoal("agent: weather in berlin"))
        assertEquals("تاریخ ایران", AgentService.triggerGoal(AssistantCommandEngine.normalize("تحقیق کن درباره تاریخ ایران")))
        assertNull(AgentService.triggerGoal("سلام خوبی"))
        assertNull(AgentService.triggerGoal("چطور آشپزی کنم"))
    }

    @Test
    fun `readable text keeps paragraphs and drops scripts`() {
        val html = "<html><head><style>body{color:red}</style></head><body>" +
            "<script>alert('x')</script>" +
            "<h1>Gemini Pricing</h1>" +
            "<p>The Gemini API offers a free tier with rate limits per minute and per day for developers worldwide.</p>" +
            "<p>Paid tiers unlock higher throughput and Gemini Pro usage with pay-as-you-go billing.</p>" +
            "</body></html>"
        val text = AgentService.readableText(html)
        assertFalse(text.contains("alert"))
        assertFalse(text.contains("color:red"))
        assertTrue(text.contains("free tier with rate limits"))
        assertTrue(text.contains('\n'))
    }

    @Test
    fun `paragraph picker scores keyword overlap`() {
        val page = "nav header junk line too short\n" +
            "The agent found that gold prices rose after central bank purchases increased across Asia this quarter.\n" +
            "Random paragraph about cooking pasta with tomato sauce and basil leaves for dinner tonight.\n" +
            "Gold price analysis: analysts attribute the gold rally to safe-haven demand and gold ETF inflows worldwide."
        val picked = AgentService.pickBestParagraphs(page, "why did the gold price rise", max = 2)
        assertEquals(2, picked.size)
        assertTrue(picked[0].contains("gold"))
        assertFalse(picked.any { it.contains("pasta") })
    }

    @Test
    fun `extractive fallback cites numbered sources`() {
        val extract = AgentService.Extract(
            result = com.example.sayvis.ai.WebSearchService.WebResult(
                title = "Gold Market News",
                url = "https://example.com/gold",
                snippet = "",
                source = "DuckDuckGo"
            ),
            bestParagraphs = listOf("Gold prices climbed after the central bank announced new reserves today morning.")
        )
        val answer = AgentService.extractiveAnswer("gold price", listOf(extract), emptyList(), languageFa = false)
        assertTrue(answer.contains("🛰"))
        assertTrue(answer.contains("[1]"))
        assertTrue(answer.contains("Gold Market News"))
        assertTrue(answer.contains("https://example.com/gold"))
    }
}
