package com.example.sayvis.ai

import com.example.sayvis.settings.AiProviderKind
import com.example.sayvis.settings.AiSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * One implementation covering every OpenAI-compatible chat-completions endpoint:
 * OpenRouter, Groq and any self-hosted / third-party gateway the owner points at.
 *
 * This is what makes the "custom endpoint" option real instead of decorative — the
 * owner supplies a base URL, a model id and a key, and SAYVIS talks to it.
 */
class OpenAiCompatibleProvider(private val kind: AiProviderKind) : AIProvider {

    override val providerType: ProviderType = ProviderType.from(kind)

    override fun isConfigured(settings: AiSettings): Boolean = when (kind) {
        AiProviderKind.OPENROUTER -> settings.openRouterApiKey.isNotBlank()
        AiProviderKind.GROQ -> settings.groqApiKey.isNotBlank()
        AiProviderKind.CUSTOM -> settings.customBaseUrl.isNotBlank() && settings.customModel.isNotBlank()
        else -> false
    }

    private fun endpoint(settings: AiSettings): String = when (kind) {
        AiProviderKind.OPENROUTER -> "https://openrouter.ai/api/v1/chat/completions"
        AiProviderKind.GROQ -> "https://api.groq.com/openai/v1/chat/completions"
        AiProviderKind.CUSTOM -> normalise(settings.customBaseUrl) + "/chat/completions"
        else -> ""
    }

    private fun apiKey(settings: AiSettings): String = when (kind) {
        AiProviderKind.OPENROUTER -> settings.openRouterApiKey.trim()
        AiProviderKind.GROQ -> settings.groqApiKey.trim()
        AiProviderKind.CUSTOM -> settings.customApiKey.trim()
        else -> ""
    }

    private fun model(settings: AiSettings): String = when (kind) {
        AiProviderKind.OPENROUTER -> settings.openRouterModel.trim()
        AiProviderKind.GROQ -> settings.groqModel.trim()
        AiProviderKind.CUSTOM -> settings.customModel.trim()
        else -> ""
    }

    private fun normalise(url: String): String {
        var out = url.trim().removeSuffix("/")
        if (out.endsWith("/v1")) out = out.removeSuffix("/v1")
        if (!out.startsWith("http://") && !out.startsWith("https://")) out = "https://$out"
        return out + "/v1"
    }

    override suspend fun generateResponse(
        context: AiRequestContext,
        settings: AiSettings
    ): AIResponse = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        val key = apiKey(settings)
        val target = endpoint(settings)
        val selectedModel = model(settings)

        if (target.isBlank() || selectedModel.isBlank()) {
            return@withContext failure(selectedModel, start, "Endpoint or model is not configured for this provider.")
        }
        if (key.isBlank() && kind != AiProviderKind.CUSTOM) {
            return@withContext failure(selectedModel, start, "API key is not configured for this provider.")
        }

        val messages = JSONArray()
        messages.put(JSONObject().apply {
            put("role", "system")
            put("content", SystemPromptBuilder.build(context))
        })
        context.history.forEach { turn ->
            messages.put(JSONObject().apply {
                put("role", if (turn.role == "OWNER") "user" else "assistant")
                put("content", turn.text)
            })
        }
        messages.put(JSONObject().apply {
            put("role", "user")
            put("content", context.prompt)
        })

        val body = JSONObject().apply {
            put("model", selectedModel)
            put("messages", messages)
            put("temperature", context.temperature)
            put("max_tokens", context.maxOutputTokens)
        }

        runCatching {
            val builder = Request.Builder()
                .url(target)
                .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
                .header("Authorization", "Bearer $key")
                .header("Content-Type", "application/json")
            if (kind == AiProviderKind.OPENROUTER) {
                builder.header("HTTP-Referer", "https://sayvis.local")
                builder.header("X-Title", "SAYVIS")
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(settings.timeoutSeconds.toLong(), TimeUnit.SECONDS)
                .readTimeout((settings.timeoutSeconds + 10L), TimeUnit.SECONDS)
                .build()

            client.newCall(builder.build()).execute().use { response ->
                val payload = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext failure(selectedModel, start, "HTTP ${response.code}: ${summarise(payload)}")
                }
                val text = JSONObject(payload)
                    .optJSONArray("choices")
                    ?.optJSONObject(0)
                    ?.optJSONObject("message")
                    ?.optString("content")
                    ?.trim()
                    .orEmpty()

                if (text.isEmpty()) {
                    failure(selectedModel, start, "The model returned an empty answer.")
                } else {
                    AIResponse(
                        text = text,
                        providerUsed = providerType,
                        model = selectedModel,
                        isOfflineMode = false,
                        processingTimeMs = System.currentTimeMillis() - start
                    )
                }
            }
        }.getOrElse { e -> failure(selectedModel, start, e.message ?: "Unknown network error") }
    }

    suspend fun probe(settings: AiSettings): ProbeOutcome = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        if (!isConfigured(settings)) {
            return@withContext ProbeOutcome(
                false, 0, model(settings),
                "تنظیمات این سرویس کامل نیست", "This provider is not fully configured"
            )
        }
        val response = generateResponse(
            AiRequestContext(prompt = "ping", languageFa = false, maxOutputTokens = 8),
            settings
        )
        ProbeOutcome(
            response.isSuccess,
            System.currentTimeMillis() - start,
            response.model,
            if (response.isSuccess) "اتصال برقرار است" else (response.errorMessage ?: "خطا"),
            if (response.isSuccess) "Connection successful" else (response.errorMessage ?: "Error")
        )
    }

    private fun failure(model: String, start: Long, message: String) = AIResponse(
        text = "",
        providerUsed = providerType,
        model = model,
        isOfflineMode = false,
        processingTimeMs = System.currentTimeMillis() - start,
        errorMessage = message
    )

    private fun summarise(body: String): String = runCatching {
        val json = JSONObject(body)
        json.optJSONObject("error")?.optString("message")
            ?: json.optString("error")
            ?: body.take(220)
    }.getOrElse { body.take(220) }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
