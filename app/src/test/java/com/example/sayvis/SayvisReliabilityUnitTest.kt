package com.example.sayvis

import com.example.sayvis.ai.AIOrchestrator
import com.example.sayvis.ai.ConnectivityProbe
import com.example.sayvis.ai.WebSearchService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the reliability layer born from the live connectivity audit:
 * the Bing parser (real markup), the Bing redirect unwrap, the network-state
 * decision and the automatic AI-provider failover order.
 */
class SayvisReliabilityUnitTest {

    /** Mirrors the real Bing SERP markup observed in the live audit. */
    private val bingHtml = "<ol id=\"b_results\">" +
        "<li class=\"b_algo\" data-id=\"1\">" +
        "<h2 class=\" b_topTitle\"><a href=\"https://ai.google.dev/gemini-api/docs/pricing\" h=\"ID=SERP\">" +
        "<strong>Gemini</strong> Developer <strong>API pricing</strong> - Google AI</a></h2>" +
        "<div class=\"b_caption\"><p class=\"b_lineclamp b_lineclamp3\">Start building free of charge, then scale up.</p></div>" +
        "</li>" +
        "<li class=\"b_algo\"><h2><a href=\"https://example.org/page\">Other &amp; more</a></h2>" +
        "<p>A snippet with entities.</p></li>" +
        "</ol>"

    @Test
    fun `bing parser pairs titles with snippets block-wise`() {
        val results = WebSearchService.parseBingHtml(bingHtml)
        assertEquals(2, results.size)
        assertTrue(results[0].title.startsWith("Gemini Developer API pricing"))
        assertEquals("https://ai.google.dev/gemini-api/docs/pricing", results[0].url)
        assertTrue(results[0].snippet.startsWith("Start building free"))
        assertEquals("Bing", results[0].source)
        assertEquals("Other & more", results[1].title)
    }

    @Test
    fun `bing parser survives captcha pages with zero results`() {
        // The live DDG-style challenge page carries no b_algo blocks.
        assertTrue(WebSearchService.parseBingHtml("Unfortunately, bots use this engine too.").isEmpty())
        assertTrue(WebSearchService.parseBingHtml("").isEmpty())
    }

    @Test
    fun `bing ck redirect is unwrapped from the base64 u parameter`() {
        val packed = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString("https://real.example.org/x?a=1".toByteArray())
        val wrapped = "https://www.bing.com/ck/a?!&&p=abc&u=a1$packed&ntb=1"
        assertEquals("https://real.example.org/x?a=1", WebSearchService.resolveBingUrl(wrapped))
        assertEquals("https://plain.example.org/", WebSearchService.resolveBingUrl("https://plain.example.org/"))
    }

    @Test
    fun `network decision distinguishes blocked google from offline`() {
        val probe = ConnectivityProbe()
        assertEquals(ConnectivityProbe.NetState.ONLINE, probe.decide(transportOk = true, googleOk = true))
        assertEquals(ConnectivityProbe.NetState.ONLINE_NO_GOOGLE, probe.decide(transportOk = true, googleOk = false))
        assertEquals(ConnectivityProbe.NetState.OFFLINE, probe.decide(transportOk = false, googleOk = false))
    }

    @Test
    fun `failover chain covers every other cloud brain in stable order`() {
        val chain = AIOrchestrator().failoverChain(com.example.sayvis.settings.AiProviderKind.GEMINI)
        assertEquals(
            listOf(
                com.example.sayvis.settings.AiProviderKind.OPENAI,
                com.example.sayvis.settings.AiProviderKind.XAI,
                com.example.sayvis.settings.AiProviderKind.OPENROUTER,
                com.example.sayvis.settings.AiProviderKind.GROQ,
                com.example.sayvis.settings.AiProviderKind.CUSTOM
            ),
            chain
        )
        // The selected provider and LOCAL never appear in their own chain.
        assertTrue(!chain.contains(com.example.sayvis.settings.AiProviderKind.GEMINI))
        assertTrue(!chain.contains(com.example.sayvis.settings.AiProviderKind.LOCAL))
        assertEquals(5, AIOrchestrator().failoverChain(com.example.sayvis.settings.AiProviderKind.OPENAI).size)
    }
}
