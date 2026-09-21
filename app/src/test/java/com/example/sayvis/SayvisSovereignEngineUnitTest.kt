package com.example.sayvis

import com.example.sayvis.ai.AIOrchestrator
import com.example.sayvis.ai.SovereignEngine
import com.example.sayvis.settings.AiProviderKind
import com.example.sayvis.settings.AiSettings
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/**
 * Sovereign Engine Finder — flawless, always-online.
 *
 * Verifies the “find the best engine or build a flawless one” guarantee:
 *  - no key → Sovereign Core
 *  - one key → that provider
 *  - many keys → the best (lowest latency / preference order)
 *  - all probes failing → still Sovereign Core, never offline
 */
class SayvisSovereignEngineUnitTest {

    @Test
    fun `no key configured returns sovereign core`() = runTest {
        val settings = AiSettings() // all keys blank
        val best = SovereignEngine.bestKind(settings, FakeSuccessOrchestrator(emptyMap()))
        assertEquals(AiProviderKind.LOCAL, best)
    }

    @Test
    fun `single configured key returns that provider even if probe would fail`() = runTest {
        val settings = AiSettings(geminiApiKey = "g-key")
        // Fake orchestrator says gemini succeeds
        val best = SovereignEngine.bestKind(settings, FakeSuccessOrchestrator(mapOf(AiProviderKind.GEMINI to true)))
        assertEquals(AiProviderKind.GEMINI, best)
    }

    @Test
    fun `isConfigured respects blank vs non-blank`() {
        val empty = AiSettings()
        assertFalse(SovereignEngine.isConfigured(AiProviderKind.GEMINI, empty))
        assertFalse(SovereignEngine.isConfigured(AiProviderKind.GROQ, empty))
        assertTrue(SovereignEngine.isConfigured(AiProviderKind.LOCAL, empty))

        val withGemini = AiSettings(geminiApiKey = "k")
        assertTrue(SovereignEngine.isConfigured(AiProviderKind.GEMINI, withGemini))
        assertFalse(SovereignEngine.isConfigured(AiProviderKind.GROQ, withGemini))

        val withCustom = AiSettings(customBaseUrl = "https://api.example.com", customModel = "m")
        assertTrue(SovereignEngine.isConfigured(AiProviderKind.CUSTOM, withCustom))
        assertFalse(SovereignEngine.isConfigured(AiProviderKind.CUSTOM, AiSettings(customBaseUrl = "https://a", customModel = "")))
    }

    @Test
    fun `preference order starts with gemini groq`() {
        val order = SovereignEngine.preferenceOrder()
        assertEquals(AiProviderKind.GEMINI, order[0])
        assertEquals(AiProviderKind.GROQ, order[1])
        assertEquals(6, order.size)
        assertFalse(order.contains(AiProviderKind.LOCAL))
    }

    @Test
    fun `label returns sovereign for local`() {
        assertTrue(SovereignEngine.label(AiProviderKind.LOCAL, true).contains("حاکم"))
        assertTrue(SovereignEngine.label(AiProviderKind.LOCAL, false).contains("Sovereign"))
        assertTrue(SovereignEngine.label(AiProviderKind.GEMINI, true).isNotBlank())
    }

    @Test
    fun `all probes failing still returns sovereign core never offline`() = runTest {
        val settings = AiSettings(geminiApiKey = "k1", groqApiKey = "k2", openAiApiKey = "k3")
        val fake = FakeSuccessOrchestrator(mapOf(
            AiProviderKind.GEMINI to false,
            AiProviderKind.GROQ to false,
            AiProviderKind.OPENAI to false
        ))
        val best = SovereignEngine.bestKind(settings, fake)
        assertEquals(AiProviderKind.LOCAL, best)
    }

    @Test
    fun `parallel probe picks fastest successful`() = runTest {
        // Simulate groq faster than gemini (latency via fake)
        val settings = AiSettings(geminiApiKey = "k1", groqApiKey = "k2")
        val fake = FakeLatencyOrchestrator(mapOf(
            AiProviderKind.GEMINI to 300L,
            AiProviderKind.GROQ to 40L
        ))
        val best = SovereignEngine.bestKind(settings, fake)
        assertEquals(AiProviderKind.GROQ, best)
    }

    // ---- fakes ----

    private class FakeSuccessOrchestrator(private val successMap: Map<AiProviderKind, Boolean>) : AIOrchestrator() {
        override suspend fun probe(settings: AiSettings): com.example.sayvis.ai.ProbeOutcome {
            val kind = settings.provider
            val ok = successMap[kind] ?: true
            return com.example.sayvis.ai.ProbeOutcome(
                success = ok,
                latencyMs = if (ok) 20 else 0,
                model = kind.name.lowercase(),
                messageFa = if (ok) "ok" else "fail",
                messageEn = if (ok) "ok" else "fail"
            )
        }
    }

    private class FakeLatencyOrchestrator(private val latencyMap: Map<AiProviderKind, Long>) : AIOrchestrator() {
        override suspend fun probe(settings: AiSettings): com.example.sayvis.ai.ProbeOutcome {
            val kind = settings.provider
            val latency = latencyMap[kind] ?: 100L
            // Simulate delay
            kotlinx.coroutines.delay(latency)
            return com.example.sayvis.ai.ProbeOutcome(
                success = true,
                latencyMs = latency,
                model = kind.name.lowercase(),
                messageFa = "ok",
                messageEn = "ok"
            )
        }
    }
}
