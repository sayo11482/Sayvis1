package com.example.sayvis.ai

import android.util.Log

class AIOrchestrator(
    private val geminiProvider: GeminiProvider = GeminiProvider(),
    private val localProvider: LocalCognitiveProvider = LocalCognitiveProvider()
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

        if (!forceOffline && geminiProvider.isAvailable) {
            try {
                return geminiProvider.generateResponse(prompt, uicContext, systemContext, languageFa)
            } catch (e: Exception) {
                Log.w("AIOrchestrator", "Gemini provider failed, falling back to Local Provider: ${e.message}")
            }
        }

        // Fallback to local on-device provider
        return localProvider.generateResponse(prompt, uicContext, systemContext, languageFa)
    }
}
