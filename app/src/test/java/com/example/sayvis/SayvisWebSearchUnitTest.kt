package com.example.sayvis

import com.example.sayvis.ai.AssistantCommandEngine
import com.example.sayvis.ai.WebSearchService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the keyless web-grounding pipeline of the assistant: the DDG HTML
 * parser, the DDG redirect unwrapping, the Wikipedia JSON parser and the
 * search-intent decision (explicit triggers + open questions).
 */
class SayvisWebSearchUnitTest {

    private val fixtureHtml = "<html><body>" +
        "<a rel=\"nofollow\" class=\"result__a\" href=\"//duckduckgo.com/l/?uddg=https%3A%2F%2Fexample.com%2Fgemini&amp;rut=abc123\">Gemini <b>API</b> pricing</a>" +
        "<a class=\"result__snippet\" href=\"x\">Pricing <b>details</b> for the Gemini API.</a>" +
        "<a rel=\"nofollow\" class=\"result__a\" href=\"https://direct.example.org/page\">Direct link result</a>" +
        "<a class=\"result__snippet\" href=\"y\">A snippet with &amp; entities.</a>" +
        "</body></html>"

    @Test
    fun `ddg html parser extracts title url and snippet pairs`() {
        val results = WebSearchService.parseDuckDuckGoHtml(fixtureHtml)
        assertEquals(2, results.size)
        assertEquals("Gemini API pricing", results[0].title)
        assertEquals("https://example.com/gemini", results[0].url)
        assertTrue(results[0].snippet.contains("Pricing details"))
        assertEquals("https://direct.example.org/page", results[1].url)
        assertTrue(results.all { it.source == "DuckDuckGo" })
    }

    @Test
    fun `garbage html yields no results and never throws`() {
        assertTrue(WebSearchService.parseDuckDuckGoHtml("<p>no results here</p>").isEmpty())
        assertTrue(WebSearchService.parseDuckDuckGoHtml("").isEmpty())
    }

    @Test
    fun `wikipedia json parser builds wiki links and strips tags`() {
        val json = "{\"query\":{\"search\":[" +
            "{\"title\":\"Persian Gulf\",\"snippet\":\"a <span class='searchmatch'>sea</span> between Iran and Arabia\"}," +
            "{\"title\":\"Tehran\",\"snippet\":\"capital of Iran\"}]}}"
        val results = WebSearchService.parseWikipediaJson(json, "en")
        assertEquals(2, results.size)
        assertEquals("https://en.wikipedia.org/wiki/Persian_Gulf", results[0].url)
        assertTrue(results[0].snippet.startsWith("a sea between"))
        assertEquals("Wikipedia", results[0].source)
    }

    @Test
    fun `search decision fires on explicit triggers with slot extraction`() {
        val decision = WebSearchService.shouldSearch(
            AssistantCommandEngine.normalize("جستجو کن درباره قیمت طلا")
        )
        assertNotNull(decision)
        assertTrue(decision!!.explicit)
        assertEquals("درباره قیمت طلا", decision.query)
    }

    @Test
    fun `search decision fires automatically on open questions`() {
        assertNotNull(WebSearchService.shouldSearch("gemini api چیست؟"))
        assertNotNull(WebSearchService.shouldSearch("what is the price of gold"))
        assertNotNull(WebSearchService.shouldSearch("اخبار امروز"))
        assertNull(WebSearchService.shouldSearch("سلام"))
        assertNull(WebSearchService.shouldSearch(""))
        // Structured command-style text without question cues stays off the web.
        assertNull(WebSearchService.shouldSearch("show missions please"))
    }
}
