package com.example.sayvis.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import com.example.sayvis.net.SayvisNet

/**
 * Links the owner's Google account to the Gemini API.
 *
 * The Gemini API authenticates exclusively with keys issued by Google AI
 * Studio — and AI Studio is the one official "sign in with Google" surface
 * every personal Google account can use (Google does not offer in-app OAuth
 * for generativelanguage.googleapis.com to third-party apps). SAYVIS therefore
 * links the account by walking the owner into AI Studio and then capturing the
 * issued key wherever it surfaces — share sheet, text-selection menu
 * ("Save key in SAYVIS") or clipboard — verifying it live against the Gemini
 * models endpoint and storing it in the Keystore-backed vault.
 */
object GoogleLinkManager {

    /** How a captured credential was issued. */
    enum class KeyKind { AUTH_AQ, STANDARD_AIZA, UNKNOWN }

    /** Outcome of the live verification against Google. */
    enum class CheckStatus { VALID, QUOTA_EXHAUSTED, GEO_BLOCKED, INVALID, NETWORK }

    // AQ.Ab… — the new "Auth key" format AI Studio issues by default (2026+).
    private val AUTH_KEY = Regex("""AQ\.[A-Za-z0-9_\-]{16,512}""")
    // AIzaSy… — the classic 39-character standard key.
    private val STANDARD_KEY = Regex("""AIza[0-9A-Za-z_\-]{35}""")

    /** Google AI Studio key page — reached after signing in with Google. */
    const val STUDIO_KEY_URL: String = "https://aistudio.google.com/apikey"

    /** Finds the first Gemini key inside arbitrary copied or shared text. */
    fun extractKeyFromText(text: String): String? {
        val cleaned = text.trim()
        if (cleaned.isEmpty()) return null
        STANDARD_KEY.find(cleaned)?.let { return it.value }
        AUTH_KEY.find(cleaned)?.let { return it.value }
        return null
    }

    fun classify(key: String): KeyKind = when {
        STANDARD_KEY.matches(key) -> KeyKind.STANDARD_AIZA
        AUTH_KEY.matches(key) -> KeyKind.AUTH_AQ
        else -> KeyKind.UNKNOWN
    }

    /** "AQ.Ab8R…xY2" — the full key is never rendered. */
    fun redact(key: String): String {
        val k = key.trim()
        return if (k.length <= 12) k else k.take(8) + "…" + k.takeLast(4)
    }

    /** True when a 403 body indicates Google's regional availability block. */
    fun geoBlockedBody(body: String): Boolean {
        val lower = body.lowercase()
        return lower.contains("location") || lower.contains("region") || lower.contains("country")
    }

    data class LinkCheck(
        val status: CheckStatus,
        val httpCode: Int,
        val modelCount: Int,
        val messageFa: String,
        val messageEn: String
    ) {
        /** The key is usable (possibly quota- or region-limited) — persist it. */
        val keyUsable: Boolean
            get() = status == CheckStatus.VALID ||
                status == CheckStatus.QUOTA_EXHAUSTED ||
                status == CheckStatus.GEO_BLOCKED
    }

    /**
     * Verifies the key live against the Gemini models endpoint. The key always
     * travels in the x-goog-api-key header so it never lands in a URL/log.
     */
    suspend fun validateKey(key: String): LinkCheck = withContext(Dispatchers.IO) {
        val client = SayvisNet.client(12, 12)
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models?pageSize=50")
            .header("x-goog-api-key", key.trim())
            .get()
            .build()
        runCatching {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                when {
                    response.isSuccessful -> LinkCheck(
                        CheckStatus.VALID,
                        response.code,
                        runCatching { JSONObject(body).getJSONArray("models").length() }.getOrDefault(0),
                        "کلید معتبر است و Gemini پاسخ داد — اتصال برقرار شد",
                        "Key is valid — Gemini responded; you are connected"
                    )
                    response.code == 429 -> LinkCheck(
                        CheckStatus.QUOTA_EXHAUSTED,
                        429,
                        0,
                        "کلید معتبر است اما سهمیهٔ رایگان امروز پر شده؛ فردا دوباره امتحان کنید",
                        "Key is valid but today's free quota is used up; try again tomorrow"
                    )
                    response.code == 403 && geoBlockedBody(body) -> LinkCheck(
                        CheckStatus.GEO_BLOCKED,
                        403,
                        0,
                        GEO_MESSAGE_FA,
                        GEO_MESSAGE_EN
                    )
                    response.code == 400 || response.code == 401 || response.code == 403 -> LinkCheck(
                        CheckStatus.INVALID,
                        response.code,
                        0,
                        "کلید پذیرفته نشد؛ در AI Studio یک کلید تازه بسازید",
                        "Key rejected — create a fresh one in AI Studio"
                    )
                    else -> LinkCheck(
                        CheckStatus.NETWORK,
                        response.code,
                        0,
                        "پاسخ غیرمنتظرهٔ گوگل (کد ${response.code})",
                        "Unexpected Google response (code ${response.code})"
                    )
                }
            }
        }.getOrElse {
            LinkCheck(
                CheckStatus.NETWORK,
                0,
                0,
                "شبکه در دسترس نیست؛ کلید ذخیره شد و بعداً با «آزمودن اتصال» بررسی کنید",
                "Network unreachable; the key is stored — re-check with “Test connection” later"
            )
        }
    }

    const val GEO_MESSAGE_FA: String =
        "گوگل دسترسی از موقعیت جغرافیایی شما را مسدود کرده (403 location). " +
            "با VPN دوباره بررسی کنید — یا با هستهٔ محلی و وب‌جست‌وجوی سایویس ادامه دهید."
    const val GEO_MESSAGE_EN: String =
        "Google blocks this API from your region (403 location). " +
            "Retry over a VPN — or continue with SAYVIS's local core and web search."
}
