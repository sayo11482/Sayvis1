package com.example.sayvis.ai

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiProvider : AIProvider {
    override val providerType: ProviderType = ProviderType.GEMINI

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private fun getApiKey(): String {
        return try {
            val field = BuildConfig::class.java.getField("GEMINI_API_KEY")
            (field.get(null) as? String)?.trim() ?: ""
        } catch (_: Throwable) {
            ""
        }
    }

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
            throw IllegalStateException("Gemini API key is unconfigured in AI Studio Secrets panel.")
        }

        val systemInstruction = """
            You are SAYVIS (Persian: سایویس / سایو), a sovereign personal AI operating layer and agent for your human owner.
            You are NOT a casual chatbot. You are precise, calm, highly competent, security-minded, and respectful of owner consent.
            User Cognitive Model (UIC) Context:
            $uicContext
            
            System State & Context:
            $systemContext
            
            Language Preference: ${if (languageFa) "Farsi / Persian (فارسی)" else "English"}
        """.trimIndent()

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
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
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
        val text = parts?.optJSONObject(0)?.optString("text") ?: "No output received from SAYVIS Gemini Node."

        AIResponse(
            text = text,
            providerUsed = ProviderType.GEMINI,
            isOfflineMode = false,
            suggestedAction = null,
            processingTimeMs = System.currentTimeMillis() - start
        )
    }
}
