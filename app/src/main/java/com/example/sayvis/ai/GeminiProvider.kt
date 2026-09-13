package com.example.sayvis.ai

import com.example.BuildConfig
import com.example.sayvis.core.SettingsStore
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
 * Reads the build-time key injected by the secrets plugin / AI Studio, if present.
 * Top-level function so it is usable from constructor default parameter expressions.
 */
private fun buildConfigGeminiKey(): String {
    return try {
        val field = BuildConfig::class.java.getField("GEMINI_API_KEY")
        (field.get(null) as? String)?.trim() ?: ""
    } catch (_: Throwable) {
        ""
    }
}

/**
 * Google Gemini provider (generativelanguage REST API).
 *
 * Key resolution order:
 * 1. Runtime key entered by the owner in the in-app Settings screen (SharedPreferences).
 * 2. Build-time key injected by the secrets plugin / AI Studio (BuildConfig.GEMINI_API_KEY).
 */
class GeminiProvider(
    private val keyProvider: () -> String = { SettingsStore.geminiKey.ifBlank { buildConfigGeminiKey() } },
    private val modelProvider: () -> String = { SettingsStore.geminiModel }
) : AIProvider {

    override val providerType: ProviderType = ProviderType.GEMINI

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun getApiKey(): String = keyProvider().trim()

    override val isAvailable: Boolean
        get() {
            val key = getApiKey()
            return key.isNotBlank() && key != "MY_GEMINI_API_KEY"
        }

    override suspend fun generateResponse(
        prompt: String,
        uicContext: String,
        systemContext: String,
        languageFa: Boolean
    ): AIResponse = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        val apiKey = getApiKey()

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException(
                "Gemini API key is not set. Open Settings and paste a free key from aistudio.google.com/apikey"
            )
        }

        val systemInstruction = PromptFactory.buildSystemPrompt(uicContext, systemContext, languageFa)
        val model = modelProvider().ifBlank { SettingsStore.DEFAULT_GEMINI_MODEL }

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", "$systemInstruction\n\nOwner Input: $prompt"))
                    })
                })
            })
        }

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            val errBody = response.body?.string() ?: ""
            throw IllegalStateException("Gemini API error HTTP ${response.code}: $errBody")
        }

        val resBody = response.body?.string() ?: ""
        val resJson = JSONObject(resBody)
        val candidates = resJson.optJSONArray("candidates")
        val content = candidates?.optJSONObject(0)?.optJSONObject("content")
        val parts = content?.optJSONArray("parts")
        val text = parts?.optJSONObject(0)?.optString("text")
            ?: "No output received from SAYVIS Gemini Node."

        AIResponse(
            text = text,
            providerUsed = ProviderType.GEMINI,
            isOfflineMode = false,
            suggestedAction = null,
            processingTimeMs = System.currentTimeMillis() - start
        )
    }
}
