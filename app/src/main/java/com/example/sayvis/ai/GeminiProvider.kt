package com.example.sayvis.ai

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
 * Google Gemini provider.
 *
 * The API key and model now come from [AiSettings] at runtime — i.e. from the value the
 * owner typed into Settings → AI & API — instead of only from a compile-time BuildConfig
 * field. The BuildConfig value is still honoured as a fallback so AI Studio secret
 * injection keeps working for packaged builds.
 */
class GeminiProvider(
    private val buildConfigKeyProvider: () -> String = { readBuildConfigKey() }
) : AIProvider {

    override val providerType: ProviderType = ProviderType.GEMINI

    override fun isConfigured(settings: AiSettings): Boolean = resolveKey(settings).isNotBlank()

    private fun resolveKey(settings: AiSettings): String {
        val runtime = settings.geminiApiKey.trim()
        if (runtime.isNotBlank() && runtime != PLACEHOLDER) return runtime
        val packaged = buildConfigKeyProvider().trim()
        return if (packaged.isNotBlank() && packaged != PLACEHOLDER) packaged else ""
    }

    private fun clientFor(settings: AiSettings): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(settings.timeoutSeconds.toLong(), TimeUnit.SECONDS)
            .readTimeout((settings.timeoutSeconds + 10L), TimeUnit.SECONDS)
            .build()

    override suspend fun generateResponse(
        context: AiRequestContext,
        settings: AiSettings
    ): AIResponse = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        val apiKey = resolveKey(settings)
        val model = settings.geminiModel.ifBlank { DEFAULT_MODEL }

        if (apiKey.isBlank()) {
            return@withContext failure(
                model,
                start,
                "Gemini API key is not configured. Open Settings → AI & API and paste your key."
            )
        }

        val systemInstruction = SystemPromptBuilder.build(context)

        val contents = JSONArray()
        // Carry the recent conversation so the assistant keeps context between turns.
        context.history.forEach { turn ->
            contents.put(
                JSONObject().apply {
                    put("role", if (turn.role == "OWNER") "user" else "model")
                    put("parts", JSONArray().put(JSONObject().put("text", turn.text)))
                }
            )
        }
        contents.put(
            JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(JSONObject().put("text", context.prompt)))
            }
        )

        val body = JSONObject().apply {
            put("contents", contents)
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", systemInstruction)))
            })
            put(
                "generationConfig",
                JSONObject().apply {
                    put("temperature", context.temperature)
                    put("maxOutputTokens", context.maxOutputTokens)
                }
            )
        }

        runCatching {
            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey")
                .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            clientFor(settings).newCall(request).execute().use { response ->
                val payload = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext failure(model, start, "HTTP ${response.code}: ${summarise(payload)}")
                }
                val text = JSONObject(payload)
                    .optJSONArray("candidates")
                    ?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.let { parts -> buildString { for (i in 0 until parts.length()) append(parts.optJSONObject(i)?.optString("text") ?: "") } }
                    ?.trim()
                    .orEmpty()

                if (text.isEmpty()) {
                    failure(model, start, "The model returned an empty answer (it may have been blocked by a safety filter).")
                } else {
                    AIResponse(
                        text = text,
                        providerUsed = ProviderType.GEMINI,
                        model = model,
                        isOfflineMode = false,
                        processingTimeMs = System.currentTimeMillis() - start
                    )
                }
            }
        }.getOrElse { e -> failure(model, start, e.message ?: "Unknown network error") }
    }

    /** Lightweight reachability + credential check used by the Settings screen. */
    suspend fun probe(settings: AiSettings): ProbeOutcome = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        val apiKey = resolveKey(settings)
        if (apiKey.isBlank()) {
            return@withContext ProbeOutcome(false, 0, "no-key", "کلید API وارد نشده است", "No API key entered")
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
        providerUsed = ProviderType.GEMINI,
        model = model,
        isOfflineMode = false,
        processingTimeMs = System.currentTimeMillis() - start,
        errorMessage = message
    )

    private fun summarise(body: String): String = runCatching {
        JSONObject(body).optJSONObject("error")?.optString("message") ?: body.take(220)
    }.getOrElse { body.take(220) }

    companion object {
        const val DEFAULT_MODEL = "gemini-2.5-flash"
        private const val PLACEHOLDER = "MY_GEMINI_API_KEY"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        private fun readBuildConfigKey(): String = runCatching {
            val field = com.example.BuildConfig::class.java.getField("GEMINI_API_KEY")
            (field.get(null) as? String)?.trim() ?: ""
        }.getOrElse { "" }
    }
}

/** Result of a provider reachability test. */
data class ProbeOutcome(
    val success: Boolean,
    val latencyMs: Long,
    val model: String,
    val messageFa: String,
    val messageEn: String
) {
    fun message(isPersian: Boolean) = if (isPersian) messageFa else messageEn
}
