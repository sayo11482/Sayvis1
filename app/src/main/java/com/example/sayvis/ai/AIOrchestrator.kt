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
                Log.w(TAG, "Falling back to local core. Reason: $reason")
                val fallback = localProvider.generateResponse(context, settings)
                val warn = if (languageFa) {
                    "⚠️ پاسخ از هستهٔ محلی آفلاین تولید شد، زیرا سرویس «${settings.provider.labelFa}» پاسخ نداد.\nعلت: $reason\n"
                } else {
                    "⚠️ Answered by the offline local core because the \"${settings.provider.labelEn}\" service did not respond.\nReason: $reason\n"
                }
                return fallback.copy(text = warn + "\n" + fallback.text, errorMessage = reason)
            }
        }

        return localProvider.generateResponse(context, settings)
    }

    /** Runs a live credential/reachability check for the Settings screen. */
    suspend fun probe(settings: AiSettings): ProbeOutcome = when (settings.provider) {
        AiProviderKind.GEMINI -> geminiProvider.probe(settings)
        AiProviderKind.OPENROUTER -> openRouterProvider.probe(settings)
        AiProviderKind.GROQ -> groqProvider.probe(settings)
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
