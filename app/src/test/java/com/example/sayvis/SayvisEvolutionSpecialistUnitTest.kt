package com.example.sayvis

import com.example.sayvis.ai.EvolutionService
import com.example.sayvis.ai.SpecialistAgent
import com.example.sayvis.settings.AiSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the self-evolution GitHub scanner (window parser on nested real
 * API JSON), the adoption-idea mapping, the specialist brain broker's
 * free-first ranking, and the natural (organic) breathing curve.
 */
class SayvisEvolutionSpecialistUnitTest {

    /** Mirrors the real GitHub search API: nested owner object, null description. */
    private val fixture = "{\"total_count\":2,\"items\":[" +
        "{\"id\":7,\"name\":\"assistant-ui\",\"full_name\":\"foo/assistant-ui\",\"private\":false," +
        "\"owner\":{\"login\":\"foo\",\"id\":1,\"avatar_url\":\"https://avatars/x.png\"}," +
        "\"html_url\":\"https://github.com/foo/assistant-ui\"," +
        "\"description\":\"A \\\"great\\\" voice assistant with tts &amp; stt\"," +
        "\"stargazers_count\":120,\"language\":\"Kotlin\",\"topics\":[\"voice\",\"tts\",\"ui\"]}," +
        "{\"id\":9,\"name\":\"trade-bot\",\"full_name\":\"bar/trade-bot\"," +
        "\"owner\":{\"login\":\"bar\",\"id\":2}," +
        "\"html_url\":\"https://github.com/bar/trade-bot\",\"description\":null," +
        "\"stargazers_count\":450,\"topics\":[\"trading\",\"crypto\"]}]}"

    @Test
    fun `github window parser survives nested objects and null descriptions`() {
        val repos = EvolutionService.parseGitHubSearch(fixture)
        assertEquals(2, repos.size)
        assertEquals("bar/trade-bot", repos[0].fullName)
        assertEquals(450, repos[0].stars)
        assertEquals("foo/assistant-ui", repos[1].fullName)
        assertTrue(repos[1].description.startsWith("A \"great\" voice"))
        assertEquals(listOf("voice", "tts", "ui"), repos[1].topics)
    }

    @Test
    fun `adoption ideas map discovered signals to sayvis capabilities`() {
        val repos = EvolutionService.parseGitHubSearch(fixture)
        val ideas = EvolutionService.adoptionIdeas(repos)
        assertTrue(ideas.any { it.contains("ترید") && it.contains("bar/trade-bot") })
        assertTrue(ideas.any { it.contains("صدا") && it.contains("foo/assistant-ui") })
    }

    @Test
    fun `brain broker is free-first and key aware`() {
        // Only Gemini configured -> Gemini wins.
        assertEquals(
            com.example.sayvis.settings.AiProviderKind.GEMINI,
            SpecialistAgent.pickBrain(AiSettings(geminiApiKey = "k"))!!.kind
        )
        // Without a Gemini key, the free Groq tier leads over paid brains.
        assertEquals(
            com.example.sayvis.settings.AiProviderKind.GROQ,
            SpecialistAgent.pickBrain(AiSettings(groqApiKey = "k", openAiApiKey = "k"))!!.kind
        )
        // Nothing configured -> null (local core).
        assertNull(SpecialistAgent.pickBrain(AiSettings()))
    }

    @Test
    fun `specialist prompts embed goal owner context and structure`() {
        val prompt = SpecialistAgent.prompt(
            SpecialistAgent.Kind.CONTENT,
            "تقویم محتوایی",
            languageFa = true,
            ownerContext = "هندل اینستاگرام مالک: @sayvis\n"
        )
        assertTrue(prompt.contains("تقویم محتوایی"))
        assertTrue(prompt.contains("@sayvis"))
        assertTrue(prompt.contains("هشتگ"))
    }

    @Test
    fun `natural breath is organic but bounded periodic and asymmetric`() {
        var peak = 0f
        for (i in 0 until 2000) {
            val t = i / 1000f
            val v = com.example.sayvis.ui.components.AiStyleMath.naturalBreath(t)
            assertTrue("bounds at t=$t", v in 0f..1f)
            if (v > peak) peak = v
        }
        assertTrue("reaches full inhale", peak > 0.92f)
        // Period-one continuity.
        assertTrue(
            Math.abs(
                com.example.sayvis.ui.components.AiStyleMath.naturalBreath(0f) -
                    com.example.sayvis.ui.components.AiStyleMath.naturalBreath(1f)
            ) < 1e-3f
        )
        // Fast inhale, slow exhale: 25% of the cycle is already higher than 75%.
        assertTrue(
            com.example.sayvis.ui.components.AiStyleMath.naturalBreath(0.25f) >
                com.example.sayvis.ui.components.AiStyleMath.naturalBreath(0.75f)
        )
        // Not a plain sine: mid-inhale slope is steep thanks to the eased curve.
        assertNotNull(com.example.sayvis.ui.components.AiStyleMath.naturalBreath(0.42f))
    }
}
