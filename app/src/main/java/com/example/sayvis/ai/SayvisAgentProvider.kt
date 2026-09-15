package com.example.sayvis.ai

import com.example.sayvis.settings.AiProviderKind
import com.example.sayvis.settings.AiSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * SAYVIS Professional AI Agent Provider
 * Connects to self-hosted SAYVIS Agent backend (FastAPI + n8n + Ollama + Qdrant)
 * 
 * This provider enables the Android app to use the professional self-hosted agent
 * that runs via: docker compose --profile cpu up
 * 
 * Endpoints:
 * - /api/v1/chat (main chat)
 * - /v1/chat/completions (OpenAI-compatible)
 * - /health (health check)
 */
class SayvisAgentProvider : AIProvider {

    override val providerType: ProviderType = ProviderType.SAYVIS_AGENT

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun isConfigured(settings: AiSettings): Boolean {
        return settings.sayvisAgentBaseUrl.isNotBlank()
    }

    override suspend fun generateResponse(
        context: AiRequestContext,
        settings: AiSettings
    ): AIResponse = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        
        try {
            val baseUrl = settings.sayvisAgentBaseUrl.trimEnd('/')
            val url = "$baseUrl/api/v1/chat"
            
            // Build request payload for SAYVIS Agent API
            val payload = JSONObject().apply {
                put("message", context.prompt)
                put("session_id", "android-${System.currentTimeMillis()}")
                put("language", if (context.languageFa) "fa" else "en")
                put("use_tools", true)
                put("use_memory", true)
                put("use_rag", true)
                put("context", JSONObject().apply {
                    put("uic_context", context.uicContext)
                    put("system_context", context.systemContext)
                    put("persona", context.persona)
                    put("history", context.history.joinToString("\n") { "${it.role}: ${it.text}" })
                })
            }

            val requestBuilder = Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .header("Content-Type", "application/json")

            // Add API key if configured
            if (settings.sayvisAgentApiKey.isNotBlank()) {
                requestBuilder.header("X-API-Key", settings.sayvisAgentApiKey)
                requestBuilder.header("Authorization", "Bearer ${settings.sayvisAgentApiKey}")
            }

            val request = requestBuilder.build()
            
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                
                if (!response.isSuccessful) {
                    throw Exception("SAYVIS Agent returned HTTP ${response.code}: ${body.take(300)}")
                }

                val json = JSONObject(body)
                val text = json.optString("response").ifBlank { 
                    json.optString("text").ifBlank { body }
                }
                
                val model = json.optString("model", settings.sayvisAgentModel)
                val providerUsed = json.optString("provider_used", "sayvis-agent")
                val toolsUsed = json.optJSONArray("tools_used")
                val processingTime = json.optLong("processing_time_ms", System.currentTimeMillis() - start)

                // Build enriched response with tool info
                val enrichedText = buildString {
                    append(text)
                    if (toolsUsed != null && toolsUsed.length() > 0) {
                        append("\n\n---\n")
                        if (context.languageFa) {
                            append("🔧 ابزارهای استفاده شده: ")
                        } else {
                            append("🔧 Tools used: ")
                        }
                        for (i in 0 until minOf(toolsUsed.length(), 3)) {
                            val tool = toolsUsed.optJSONObject(i)
                            if (tool != null) {
                                append("${tool.optString("tool")} [${tool.optString("risk_level")}] ")
                            }
                        }
                    }
                }

                return@withContext AIResponse(
                    text = enrichedText,
                    providerUsed = ProviderType.SAYVIS_AGENT,
                    model = model,
                    isOfflineMode = false,
                    processingTimeMs = processingTime,
                    errorMessage = null
                )
            }

        } catch (e: Exception) {
            // Try OpenAI-compatible fallback endpoint
            try {
                return@withContext tryOpenAICompatible(context, settings, start)
            } catch (fallbackError: Exception) {
                return@withContext AIResponse(
                    text = "",
                    providerUsed = ProviderType.SAYVIS_AGENT,
                    model = settings.sayvisAgentModel,
                    isOfflineMode = false,
                    processingTimeMs = System.currentTimeMillis() - start,
                    errorMessage = "SAYVIS Agent error: ${e.message}. Fallback also failed: ${fallbackError.message}"
                )
            }
        }
    }

    private fun tryOpenAICompatible(
        context: AiRequestContext,
        settings: AiSettings,
        startTime: Long
    ): AIResponse {
        val baseUrl = settings.sayvisAgentBaseUrl.trimEnd('/')
        val url = "$baseUrl/v1/chat/completions"

        val messages = org.json.JSONArray().apply {
            if (context.systemContext.isNotBlank() || context.uicContext.isNotBlank()) {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", SystemPromptBuilder.build(context))
                })
            }
            // Add history
            context.history.forEach { turn ->
                put(JSONObject().apply {
                    put("role", if (turn.role == "SAYVIS") "assistant" else "user")
                    put("content", turn.text)
                })
            }
            put(JSONObject().apply {
                put("role", "user")
                put("content", context.prompt)
            })
        }

        val payload = JSONObject().apply {
            put("model", settings.sayvisAgentModel.ifBlank { "sayvis-agent" })
            put("messages", messages)
            put("temperature", context.temperature)
            put("max_tokens", context.maxOutputTokens)
        }

        val requestBuilder = Request.Builder()
            .url(url)
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .header("Content-Type", "application/json")

        if (settings.sayvisAgentApiKey.isNotBlank()) {
            requestBuilder.header("Authorization", "Bearer ${settings.sayvisAgentApiKey}")
        }

        client.newCall(requestBuilder.build()).execute().use { response ->
            val body = response.body?.string() ?: ""
            
            if (!response.isSuccessful) {
                throw Exception("OpenAI-compat endpoint HTTP ${response.code}: ${body.take(200)}")
            }

            val json = JSONObject(body)
            val choices = json.optJSONArray("choices")
            val firstChoice = choices?.optJSONObject(0)
            val message = firstChoice?.optJSONObject("message")
            val content = message?.optString("content") ?: ""

            if (content.isBlank()) {
                throw Exception("Empty response from SAYVIS Agent OpenAI endpoint")
            }

            return AIResponse(
                text = content,
                providerUsed = ProviderType.SAYVIS_AGENT,
                model = json.optString("model", settings.sayvisAgentModel),
                isOfflineMode = false,
                processingTimeMs = System.currentTimeMillis() - startTime
            )
        }
    }

    fun probe(settings: AiSettings): ProbeOutcome {
        val start = System.currentTimeMillis()
        
        if (settings.sayvisAgentBaseUrl.isBlank()) {
            return ProbeOutcome(
                success = false,
                latencyMs = 0,
                model = "",
                messageFa = "نشانی عامل سایویس وارد نشده است. مثال: http://192.168.1.100:8000",
                messageEn = "SAYVIS Agent URL is missing. Example: http://192.168.1.100:8000"
            )
        }

        return try {
            val baseUrl = settings.sayvisAgentBaseUrl.trimEnd('/')
            
            // Try health endpoint first
            val healthUrl = "$baseUrl/health"
            val request = Request.Builder()
                .url(healthUrl)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val latency = System.currentTimeMillis() - start
                val body = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    val json = try { JSONObject(body) } catch (e: Exception) { JSONObject() }
                    val version = json.optString("version", "1.0.0")
                    
                    ProbeOutcome(
                        success = true,
                        latencyMs = latency,
                        model = "sayvis-agent-$version",
                        messageFa = "✅ عامل حرفه‌ای سایویس متصل است! نسخه: $version | تاخیر: ${latency}ms\nسرویس‌ها: n8n, Ollama, Qdrant, Postgres",
                        messageEn = "✅ SAYVIS Professional Agent connected! Version: $version | Latency: ${latency}ms\nServices: n8n, Ollama, Qdrant, Postgres"
                    )
                } else {
                    // Try /api/v1/health as fallback
                    val fallbackRequest = Request.Builder()
                        .url("$baseUrl/api/v1/health")
                        .get()
                        .build()
                    
                    client.newCall(fallbackRequest).execute().use { fallbackResponse ->
                        val fallbackLatency = System.currentTimeMillis() - start
                        if (fallbackResponse.isSuccessful) {
                            ProbeOutcome(
                                success = true,
                                latencyMs = fallbackLatency,
                                model = settings.sayvisAgentModel,
                                messageFa = "✅ عامل سایویس متصل است (مسیر جایگزین) | تاخیر: ${fallbackLatency}ms",
                                messageEn = "✅ SAYVIS Agent connected (fallback route) | Latency: ${fallbackLatency}ms"
                            )
                        } else {
                            ProbeOutcome(
                                success = false,
                                latencyMs = fallbackLatency,
                                model = "",
                                messageFa = "❌ اتصال به عامل سایویس ناموفق بود. HTTP ${response.code}: ${body.take(200)}\nبررسی کنید: docker compose --profile cpu up",
                                messageEn = "❌ Failed to connect to SAYVIS Agent. HTTP ${response.code}: ${body.take(200)}\nCheck: docker compose --profile cpu up"
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            ProbeOutcome(
                success = false,
                latencyMs = System.currentTimeMillis() - start,
                model = "",
                messageFa = "❌ خطای اتصال به عامل سایویس: ${e.message}\n\nراهنما:\n1. مطمئن شوید Docker اجرا است: docker compose --profile cpu up\n2. نشانی را بررسی کنید (IP محلی، نه localhost اگر روی گوشی تست می‌کنید)\n3. فایروال پورت 8000 را باز کند",
                messageEn = "❌ Connection error to SAYVIS Agent: ${e.message}\n\nGuide:\n1. Ensure Docker is running: docker compose --profile cpu up\n2. Check URL (use local IP, not localhost if testing from phone)\n3. Firewall allows port 8000"
            )
        }
    }
}
