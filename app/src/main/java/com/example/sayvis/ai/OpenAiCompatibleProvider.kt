package com.example.sayvis.ai

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
 * Generic OpenAI-compatible chat-completions provider.
 *
 * Powers:
 * - OpenRouter (https://openrouter.ai/api/v1) -> access to hundreds of models
 * - Groq      (https://api.groq.com/openai/v1) -> LPU-fast open models
 *
 * Both use the same wire format: POST {base}/chat/completions with a Bearer key.
 */
class OpenAiCompatibleProvider(
    override val providerType: ProviderType,
    private val baseUrl: String,
    private val defaultModel: String,
    private val keyProvider: () -> String,
    private val modelProvider: () -> String
) : AIProvider {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun apiKey(): String = keyProvider().trim()

    override val isAvailable: Boolean
        get() = apiKey().isNotBlank()

    override suspend fun generateResponse(
        prompt: String,
        uicContext: String,
        systemContext: String,
        languageFa: Boolean
    ): AIResponse = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        val key = apiKey()
        if (key.isBlank()) {
            throw IllegalStateException("${providerType.displayName} API key is not configured.")
        }

        val messages = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "system")
                put("content", PromptFactory.buildSystemPrompt(uicContext, systemContext, languageFa))
            })
            put(JSONObject().apply {
                put("role", "user")
                put("content", prompt)
            })
        }

        val body = JSONObject().apply {
            put("model", modelProvider().ifBlank { defaultModel })
            put("messages", messages)
        }

        val request = Request.Builder()
            .url("$baseUrl/chat/completions")
            .addHeader("Authorization", "Bearer $key")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            val errBody = response.body?.string() ?: ""
            throw IllegalStateException("${providerType.displayName} HTTP ${response.code}: ${errBody.take(300)}")
        }

        val resJson = JSONObject(response.body?.string() ?: "{}")
        val text = resJson.optJSONArray("choices")
            ?.optJSONObject(0)
            ?.optJSONObject("message")
            ?.optString("content")
            ?: "No output received from ${providerType.displayName}."

        AIResponse(
            text = text,
            providerUsed = providerType,
            isOfflineMode = false,
            suggestedAction = null,
            processingTimeMs = System.currentTimeMillis() - start
        )
    }
}
