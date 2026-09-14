package com.example.sayvis.ai

import android.util.Log
import com.example.sayvis.settings.AiProviderKind
import com.example.sayvis.settings.AiSettings

/**
 * Routes every inference request to the provider the owner selected in Settings, and
 * always keeps a working offline answer available as a fallback.
 *
 * Order of operations for a normal query:
 *  1. Emergency lock engaged -> local provider only, prefixed with a lock notice.
 *  2. Forced offline mode, or no network provider configured -> local provider.
 *  3. Selected network provider -> on any failure, degrade to the local provider and
 *     surface the real error so the owner can see *why* it fell back.
 */
class AIOrchestrator(
    private val geminiProvider: GeminiProvider = GeminiProvider(),
    private val openRouterProvider: OpenAiCompatibleProvider = OpenAiCompatibleProvider(AiProviderKind.OPENROUTER),
    private val groqProvider: OpenAiCompatibleProvider = OpenAiCompatibleProvider(AiProviderKind.GROQ),
    private val openAiProvider: OpenAiCompatibleProvider = OpenAiCompatibleProvider(AiProviderKind.OPENAI),
    private val xaiProvider: OpenAiCompatibleProvider = OpenAiCompatibleProvider(AiProviderKind.XAI),
    private val customProvider: OpenAiCompatibleProvider = OpenAiCompatibleProvider(AiProviderKind.CUSTOM),
    private val localProvider: LocalCognitiveProvider = LocalCognitiveProvider()
) {

    /** Which provider is *selected* (not necessarily reachable). */
    fun selectedProvider(settings: AiSettings): ProviderType = ProviderType.from(settings.provider)

    /** True when the app can currently reach a network model. */
    fun isCloudReady(settings: AiSettings, forceOffline: Boolean): Boolean =
        !forceOffline && settings.provider != AiProviderKind.LOCAL && settings.isProviderConfigured()

    private fun networkProviderFor(kind: AiProviderKind): AIProvider? = when (kind) {
        AiProviderKind.GEMINI -> geminiProvider
        AiProviderKind.OPENROUTER -> openRouterProvider
        AiProviderKind.GROQ -> groqProvider
        AiProviderKind.OPENAI -> openAiProvider
        AiProviderKind.XAI -> xaiProvider
        AiProviderKind.CUSTOM -> customProvider
        AiProviderKind.LOCAL -> null
    }

    /**
     * Main entry point. [forceOffline] overrides the settings so the top-bar toggle and
     * the emergency lock keep working even when a cloud provider is configured.
     */
    suspend fun querySAYVIS(
        prompt: String,
        uicContext: String,
        systemContext: String,
        languageFa: Boolean,
        emergencyLockActive: Boolean,
        forceOffline: Boolean = false,
        settings: AiSettings = AiSettings(),
        history: List<ChatTurn> = emptyList(),
        taskInstruction: String? = null
    ): AIResponse {
        val context = AiRequestContext(
            prompt = prompt,
            uicContext = uicContext,
            systemContext = systemContext,
            languageFa = languageFa,
            persona = settings.systemPersona,
            temperature = settings.temperature,
            maxOutputTokens = settings.maxOutputTokens,
            taskInstruction = taskInstruction,
            history = history
        )

        if (emergencyLockActive) {
            val notice = if (languageFa) {
                "⚠️ هشدار امنیتی: قفل اضطراری سایویس فعال است. همهٔ اقدامات اجرایی، ابزارها و خودکارسازی‌ها مسدود هستند و فقط راهنمایی خواندنی ارائه می‌شود."
            } else {
                "⚠️ SECURITY ALERT: the SAYVIS emergency lock is engaged. All tool execution, automations and privileged actions are BLOCKED. Read-only guidance only."
            }
            val base = localProvider.generateResponse(context, settings)
            return base.copy(text = "$notice\n\n${base.text}")
        }

        val effectiveOffline = forceOffline || settings.provider == AiProviderKind.LOCAL
        if (!effectiveOffline) {
            val provider = networkProviderFor(settings.provider)
            if (provider != null && provider.isConfigured(settings)) {
                val response = runCatching { provider.generateResponse(context, settings) }
                    .getOrElse { e ->
                        Log.w(TAG, "Network provider failed: ${e.message}")
                        null
                    }
                if (response != null && response.isSuccess) return response

                val reason = response?.errorMessage ?: "provider exception"
                Log.w(TAG, "Primary provider failed; trying automatic failover. Reason: $reason")

                // AUTOMATIC FAILOVER: try every OTHER configured cloud brain
                // before degrading to the local core — the owner asked for a
                // fully automatic pipeline, not silence.
                val tried = mutableListOf("${settings.provider.labelFa} ($reason)")
                for (candidate in failoverChain(settings.provider)) {
                    val provider = networkProviderFor(candidate) ?: continue
                    if (!provider.isConfigured(settings)) continue
                    val failoverResponse = runCatching { provider.generateResponse(context, settings) }
                        .getOrElse { null }
                    if (failoverResponse != null && failoverResponse.isSuccess) {
                        val note = if (languageFa) {
                            "🔁 سرویس اصلی («${settings.provider.labelFa}») پاسخ نداد؛ پاسخ از «${candidate.labelFa}» گرفته شد.\n"
                        } else {
                            "🔁 The primary service (\"${settings.provider.labelEn}\") was unreachable; answered via \"${candidate.labelEn}\".\n"
                        }
                        return failoverResponse.copy(text = note + "\n" + failoverResponse.text)
                    }
                    tried += "${candidate.labelFa} (${failoverResponse?.errorMessage ?: "provider exception"})"
                }

                val fallback = localProvider.generateResponse(context, settings)
                val warn = if (languageFa) {
                    "⚠️ پاسخ از هستهٔ محلی آفلاین تولید شد؛ هیچ سرویس ابری پاسخ نداد.\n" +
                        tried.joinToString("\n") { "• $it" } + "\n" +
                        "راهنما: وضعیت شبکه را بررسی کنید (🟠 یعنی VPN لازم است) یا در تنظیمات کلید یکی از سرویس‌ها را وصل کنید."
                } else {
                    "⚠️ Answered by the offline local core; no cloud service responded.\n" +
                        tried.joinToString("\n") { "• $it" } + "\n" +
                        "Hint: check connectivity (🟠 means a VPN is needed) or connect at least one provider key in Settings."
                }
                return fallback.copy(text = warn + "\n" + fallback.text, errorMessage = tried.firstOrNull() ?: reason)
            }
        }

        return localProvider.generateResponse(context, settings)
    }

    /**
     * Automatic failover order: every cloud provider except the selected one,
     * in a stable preference order (unit-tested).
     */
    fun failoverChain(selected: AiProviderKind): List<AiProviderKind> =
        listOf(
            AiProviderKind.GEMINI,
            AiProviderKind.OPENAI,
            AiProviderKind.XAI,
            AiProviderKind.OPENROUTER,
            AiProviderKind.GROQ,
            AiProviderKind.CUSTOM
        ).filter { it != selected && it != AiProviderKind.LOCAL }

    /** Runs a live credential/reachability check for the Settings screen. */
    suspend fun probe(settings: AiSettings): ProbeOutcome = when (settings.provider) {
        AiProviderKind.GEMINI -> geminiProvider.probe(settings)
        AiProviderKind.OPENROUTER -> openRouterProvider.probe(settings)
        AiProviderKind.GROQ -> groqProvider.probe(settings)
        AiProviderKind.OPENAI -> openAiProvider.probe(settings)
        AiProviderKind.XAI -> xaiProvider.probe(settings)
        AiProviderKind.CUSTOM -> customProvider.probe(settings)
        AiProviderKind.LOCAL -> ProbeOutcome(
            success = true,
            latencyMs = 0,
            model = "sayvis-local-core",
            messageFa = "هستهٔ محلی همیشه در دسترس است (بدون نیاز به اینترنت)",
            messageEn = "The local core is always available (no internet required)"
        )
    }

    /** Exposes the raw providers for specialised jobs such as translation. */
    fun providerFor(kind: AiProviderKind): AIProvider? = networkProviderFor(kind)

    companion object {
        private const val TAG = "AIOrchestrator"
    }
}
