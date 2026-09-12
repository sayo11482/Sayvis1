package com.example.sayvis.ai

enum class ProviderType(val displayName: String) {
    GEMINI("Google Gemini 3.5 Flash / Pro"),
    LOCAL_COGNITIVE("Sovereign Local On-Device Node"),
    GROQ_ROUTER("Groq LPU Fast Gateway"),
    OPEN_ROUTER("OpenRouter Multi-Model Orchestrator")
}

data class AIResponse(
    val text: String,
    val providerUsed: ProviderType,
    val isOfflineMode: Boolean,
    val suggestedAction: String? = null,
    val processingTimeMs: Long = 0
)

interface AIProvider {
    val providerType: ProviderType
    val isAvailable: Boolean
    suspend fun generateResponse(
        prompt: String,
        uicContext: String,
        systemContext: String,
        languageFa: Boolean
    ): AIResponse
}
