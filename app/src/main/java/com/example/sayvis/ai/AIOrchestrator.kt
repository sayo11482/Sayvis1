package com.example.sayvis.ai

import android.util.Log
import com.example.sayvis.core.SettingsStore

/**
 * Central AI Orchestration Core.
 *
 * Routes every query across multiple cloud model providers (Gemini, OpenRouter, Groq)
 * according to the owner's provider choice, with automatic fallback to the sovereign
 * on-device cognitive provider when offline, unconfigured or on provider failure.
 */
class AIOrchestrator(
    private val cloudProviders: List<AIProvider> = listOf(GeminiProvider()),
    private val localProvider: LocalCognitiveProvider = LocalCognitiveProvider(),
    private val choiceProvider: () -> String = { SettingsStore.providerChoice }
) {

    suspend fun querySAYVIS(
        prompt: String,
        uicContext: String,
        systemContext: String,
        languageFa: Boolean,
        emergencyLockActive: Boolean,
        forceOffline: Boolean = false
    ): AIResponse {
        if (emergencyLockActive) {
            val notice = if (languageFa) {
                "⚠️ هشدار امنیتی: قفل اضطراری سایویس (Emergency Lock) فعال است. کلیه اقدامات اجرایی، ابزارها و اتوماسیون‌ها مسدود هستند. فقط راهنمایی خواندنی در دسترس است."
            } else {
                "⚠️ SECURITY ALERT: SAYVIS Emergency Lock is engaged. All tool execution, automations, and privileged actions are BLOCKED. Safe read-only guidance only."
            }
            val base = localProvider.generateResponse(prompt, uicContext, systemContext, languageFa)
            return base.copy(text = "$notice\n\n${base.text}")
        }

        val choice = choiceProvider()
        val ordered: List<AIProvider> = when (choice) {
            SettingsStore.PROVIDER_GEMINI ->
                cloudProviders.filter { it.providerType == ProviderType.GEMINI }
            SettingsStore.PROVIDER_OPENROUTER ->
                cloudProviders.filter { it.providerType == ProviderType.OPEN_ROUTER }
            SettingsStore.PROVIDER_GROQ ->
                cloudProviders.filter { it.providerType == ProviderType.GROQ_ROUTER }
            SettingsStore.PROVIDER_LOCAL ->
                emptyList()
            else -> cloudProviders // auto: try every configured provider in order
        }

        if (!forceOffline) {
            for (provider in ordered) {
                if (provider.isAvailable) {
                    try {
                        return provider.generateResponse(prompt, uicContext, systemContext, languageFa)
                    } catch (e: Exception) {
                        Log.w(
                            "AIOrchestrator",
                            "${provider.providerType.name} failed, trying next provider: ${e.message}"
                        )
                    }
                }
            }
        }

        // Fallback to the sovereign on-device provider
        return localProvider.generateResponse(prompt, uicContext, systemContext, languageFa)
    }
}
