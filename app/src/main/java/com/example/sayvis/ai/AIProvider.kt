package com.example.sayvis.ai

import com.example.sayvis.settings.AiProviderKind
import com.example.sayvis.settings.AiSettings

/** Identity of the engine that actually produced an answer. */
enum class ProviderType(val displayName: String, val displayNameFa: String) {
    GEMINI("Google Gemini", "گوگل جمینای"),
    OPEN_ROUTER("OpenRouter", "اوپن‌روتر"),
    GROQ_ROUTER("Groq LPU", "گروک"),
    CUSTOM_ENDPOINT("Custom endpoint", "سرویس دلخواه"),
    LOCAL_COGNITIVE("SAYVIS local core", "هستهٔ محلی سایویس");

    fun display(isPersian: Boolean): String = if (isPersian) displayNameFa else displayName

    companion object {
        fun from(kind: AiProviderKind): ProviderType = when (kind) {
            AiProviderKind.LOCAL -> LOCAL_COGNITIVE
            AiProviderKind.GEMINI -> GEMINI
            AiProviderKind.OPENROUTER -> OPEN_ROUTER
            AiProviderKind.GROQ -> GROQ_ROUTER
            AiProviderKind.CUSTOM -> CUSTOM_ENDPOINT
        }
    }
}

fun AiProviderKind.toProviderType(): ProviderType = ProviderType.from(this)

data class AIResponse(
    val text: String,
    val providerUsed: ProviderType,
    val model: String = "",
    val isOfflineMode: Boolean,
    val suggestedAction: String? = null,
    val processingTimeMs: Long = 0,
    val errorMessage: String? = null
) {
    val isSuccess: Boolean get() = errorMessage == null
}

/** Everything a provider needs to answer one turn. */
data class AiRequestContext(
    val prompt: String,
    val uicContext: String = "",
    val systemContext: String = "",
    val languageFa: Boolean = false,
    val persona: String = "",
    val temperature: Double = 0.7,
    val maxOutputTokens: Int = 2048,
    /** Extra system rule injected for specialised jobs such as translation. */
    val taskInstruction: String? = null,
    val history: List<ChatTurn> = emptyList()
)

data class ChatTurn(val role: String, val text: String)

interface AIProvider {
    val providerType: ProviderType

    /** True when this provider has everything it needs to run right now. */
    fun isConfigured(settings: AiSettings): Boolean

    suspend fun generateResponse(context: AiRequestContext, settings: AiSettings): AIResponse
}

/** Builds the SAYVIS system instruction shared by every network provider. */
internal object SystemPromptBuilder {

    fun build(context: AiRequestContext): String {
        val languageRule = if (context.languageFa) {
            "LANGUAGE RULE (MANDATORY): Answer entirely in fluent, natural Persian (فارسی). " +
                "Do not leave English words, headings, bullet labels or technical jargon untranslated " +
                "unless the term is a proper noun or a standard symbol (BTC, API, MT5). " +
                "Use Persian digits where natural and write right-to-left."
        } else {
            "LANGUAGE RULE: Answer entirely in clear English."
        }

        val persona = context.persona.trim()

        return buildString {
            appendLine("You are SAYVIS (Persian: سایویس), a sovereign personal AI operating layer for its human owner.")
            appendLine("You are precise, calm, competent and security-minded. You propose actions; you never claim to have executed anything.")
            appendLine()
            appendLine(languageRule)
            if (persona.isNotEmpty()) {
                appendLine()
                appendLine("OWNER PERSONA / CUSTOM RULES:")
                appendLine(persona)
            }
            if (context.uicContext.isNotBlank()) {
                appendLine()
                appendLine("USER COGNITIVE MODEL (UIC) CONTEXT:")
                appendLine(context.uicContext)
            }
            if (context.systemContext.isNotBlank()) {
                appendLine()
                appendLine("LIVE SYSTEM CONTEXT:")
                appendLine(context.systemContext)
            }
            if (context.taskInstruction != null) {
                appendLine()
                appendLine("SPECIALISED TASK:")
                appendLine(context.taskInstruction)
            }
        }.trim()
    }
}
