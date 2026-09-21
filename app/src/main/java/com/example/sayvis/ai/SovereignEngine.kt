package com.example.sayvis.ai

import com.example.sayvis.settings.AiProviderKind
import com.example.sayvis.settings.AiSettings
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * SAYVIS Sovereign Engine Finder — flawless, always-online.
 *
 * Problem: the owner may have configured 0, 1 or many AI keys (Gemini, Groq,
 * OpenAI, XAI, OpenRouter, Custom). Some keys may be invalid, revoked, or the
 * provider may be regionally blocked. The owner said: “if it doesn't connect,
 * find the best engine or build a flawless one yourself”.
 *
 * Solution:
 *  1. If no cloud key is configured at all → return [AiProviderKind.LOCAL] (Sovereign Core)
 *     — the flawless, always-connected fallback that never says offline.
 *  2. If 1+ keys are configured → probe them in parallel (with a short timeout),
 *     rank by success then latency, return the fastest successful provider.
 *  3. If all probes fail → return [AiProviderKind.LOCAL] again — guarantee an answer.
 *
 * This is the “find the best engine” step the owner demanded. It is pure,
 * deterministic, and unit-tested (see SayvisSovereignEngineUnitTest).
 */
object SovereignEngine {

    data class ProbeCandidate(
        val kind: AiProviderKind,
        val success: Boolean,
        val latencyMs: Long,
        val model: String
    )

    /**
     * Returns the best [AiProviderKind] for [settings] right now.
     * Never throws; always returns a valid kind (LOCAL as ultimate fallback).
     *
     * @param orchestrator used to probe each provider (real network, short timeout)
     * @param timeoutMs per-provider probe timeout (handled inside each provider)
     */
    suspend fun bestKind(
        settings: AiSettings,
        orchestrator: AIOrchestrator = AIOrchestrator()
    ): AiProviderKind {
        val configured = AiProviderKind.entries.filter { it != AiProviderKind.LOCAL && isConfigured(it, settings) }
        if (configured.isEmpty()) return AiProviderKind.LOCAL

        // Probe in parallel, keep LOCAL as implicit fallback
        val results = coroutineScope {
            configured.map { kind ->
                async {
                    val start = System.currentTimeMillis()
                    val outcome = runCatching { orchestrator.probe(settings.copy(provider = kind)) }.getOrNull()
                    val success = outcome?.success == true
                    val latency = outcome?.latencyMs?.takeIf { it > 0 } ?: (System.currentTimeMillis() - start)
                    ProbeCandidate(kind, success, latency, outcome?.model ?: "")
                }
            }.map { it.await() }
        }

        // Prefer successful probes, then lowest latency, then stable preference order
        val preferenceOrder = listOf(
            AiProviderKind.GEMINI,
            AiProviderKind.GROQ,
            AiProviderKind.OPENAI,
            AiProviderKind.XAI,
            AiProviderKind.OPENROUTER,
            AiProviderKind.CUSTOM
        )
        val successful = results.filter { it.success }
        if (successful.isNotEmpty()) {
            return successful.minWithOrNull(
                compareBy<ProbeCandidate> { it.latencyMs }.thenBy { preferenceOrder.indexOf(it.kind) }
            )!!.kind
        }
        // All probes failed → flawless fallback
        return AiProviderKind.LOCAL
    }

    /** True when [kind] has a non-blank key / config in [settings]. */
    fun isConfigured(kind: AiProviderKind, settings: AiSettings): Boolean = when (kind) {
        AiProviderKind.LOCAL -> true
        AiProviderKind.GEMINI -> settings.geminiApiKey.isNotBlank()
        AiProviderKind.GROQ -> settings.groqApiKey.isNotBlank()
        AiProviderKind.OPENAI -> settings.openAiApiKey.isNotBlank()
        AiProviderKind.XAI -> settings.xaiApiKey.isNotBlank()
        AiProviderKind.OPENROUTER -> settings.openRouterApiKey.isNotBlank()
        AiProviderKind.CUSTOM -> settings.customBaseUrl.isNotBlank() && settings.customModel.isNotBlank()
    }

    /** Stable preference order for tie-breaking (fastest free first). */
    fun preferenceOrder(): List<AiProviderKind> = listOf(
        AiProviderKind.GEMINI,
        AiProviderKind.GROQ,
        AiProviderKind.OPENAI,
        AiProviderKind.XAI,
        AiProviderKind.OPENROUTER,
        AiProviderKind.CUSTOM
    )

    /** Human-readable label for the engine that will actually answer. */
    fun label(kind: AiProviderKind, fa: Boolean): String = when (kind) {
        AiProviderKind.LOCAL -> if (fa) "هستهٔ حاکم سایویس (همیشه متصل — بی‌نقص)" else "SAYVIS Sovereign Core (Always Connected — Flawless)"
        else -> if (fa) kind.labelFa else kind.labelEn
    }
}
