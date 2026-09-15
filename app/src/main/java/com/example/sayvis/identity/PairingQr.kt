package com.example.sayvis.identity

import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Pure pairing-QR codec + scanner-import classifier (v4.0.0).
 *
 * Two real directions, both wired in the Connect hub:
 *
 *  1. SHOW  — SAYVIS renders a `sayvis://link?…` QR of the linked Google
 *     account + this device + a 6-digit pin. Any computer/phone QR reader
 *     reveals the pairing payload, and another SAYVIS device can scan it to
 *     link the same account (computer-assisted connection).
 *  2. SCAN  — the in-app camera reads a QR shown on a computer screen and
 *     accepts: a pairing payload, a provider API key (Gemini/OpenAI/Groq/
 *     xAI/OpenRouter — e.g. generated as a QR on the computer), a small
 *     config JSON, or a plain URL. This is what makes "connect via the
 *     computer" concrete without any backend.
 */
object PairingQr {

    const val SCHEME_PREFIX: String = "sayvis://link?"

    data class Pairing(
        val account: String,
        val device: String,
        val pin: String,
        val createdAt: Long
    )

    /** Builds the canonical pairing payload (all values URL-encoded). */
    fun buildPayload(account: String, device: String, pin: String, createdAt: Long): String =
        SCHEME_PREFIX +
            "v=1" +
            "&acct=" + urlencode(account) +
            "&dev=" + urlencode(device) +
            "&pin=" + urlencode(pin) +
            "&ts=" + createdAt

    /** Parses a `sayvis://link` payload; null when it is not a valid pairing. */
    fun parsePairing(raw: String): Pairing? {
        val trimmed = raw.trim()
        if (!trimmed.startsWith(SCHEME_PREFIX)) return null
        val query = trimmed.removePrefix(SCHEME_PREFIX)
        val params = HashMap<String, String>()
        for (piece in query.split("&")) {
            if (piece.isBlank()) continue
            val idx = piece.indexOf('=')
            val key = if (idx < 0) piece else piece.substring(0, idx)
            val value = if (idx < 0) "" else piece.substring(idx + 1)
            params[urldecode(key)] = urldecode(value)
        }
        if (params["v"] != "1") return null
        val account = params["acct"]?.trim().orEmpty()
        val pin = params["pin"]?.trim().orEmpty()
        if (!account.contains('@') || pin.length != 6 || pin.any { it !in '0'..'9' }) return null
        return Pairing(
            account = account,
            device = params["dev"]?.trim().orEmpty(),
            pin = pin,
            createdAt = params["ts"]?.toLongOrNull() ?: 0L
        )
    }

    // ------------------------------------------------------------ importing

    sealed class Import {
        /** Another SAYVIS device's account pairing — link the same account. */
        data class PairingLink(val pairing: Pairing) : Import()

        /** A bare provider API key — stored under the recognised provider. */
        data class ProviderKey(val provider: String, val apiKey: String) : Import()

        /** `{"provider":"gemini","apiKey":"…","baseUrl":"…","model":"…"}` */
        data class Config(
            val provider: String?,
            val apiKey: String,
            val baseUrl: String?,
            val model: String?
        ) : Import()

        /** A plain https link — shown to the owner. */
        data class Link(val url: String) : Import()

        /** Anything else — shown as text. */
        data class Plain(val text: String) : Import()
    }

    /** Classifies scanned QR content into a concrete import action. */
    fun classify(raw: String): Import {
        val text = raw.trim()
        if (text.isEmpty()) return Import.Plain("")
        parsePairing(text)?.let { return Import.PairingLink(it) }
        configFromJson(text)?.let { return it }
        providerForKey(text)?.let { return Import.ProviderKey(it, text) }
        if (text.startsWith("https://") || text.startsWith("http://")) return Import.Link(text)
        return Import.Plain(text)
    }

    /** Maps a bare key to its provider name, or null when not a known key. */
    fun providerForKey(value: String): String? {
        val v = value.trim()
        return when {
            v.length >= 39 && v.startsWith("AIza") -> "gemini"
            v.startsWith("AQ.") -> "gemini" // Google AI Studio copied key
            v.startsWith("sk-or-") -> "openrouter"
            v.startsWith("gsk_") -> "groq"
            v.startsWith("xai-") -> "xai"
            v.startsWith("sk-") -> "openai"
            else -> null
        }
    }

    /**
     * Minimal dependency-free config JSON parser — accepts the documented
     * shape {"provider":…,"apiKey":…,"baseUrl":…,"model":…}. Built without
     * org.json (a stub on the JVM test classpath).
     */
    fun configFromJson(text: String): Import.Config? {
        val t = text.trim()
        if (!t.startsWith("{") || !t.endsWith("}")) return null
        val provider = miniString(t, "provider")
        val apiKey = miniString(t, "apiKey") ?: return null
        if (apiKey.isBlank()) return null
        return Import.Config(
            provider = provider?.lowercase(),
            apiKey = apiKey,
            baseUrl = miniString(t, "baseUrl"),
            model = miniString(t, "model")
        )
    }

    /** Escaped-JSON string field reader (same semantics as GoogleAuthManager). */
    fun miniString(json: String, field: String): String? {
        // Pattern: "field"\s*:\s*"((?:\\.|[^"\\])*)"  — assembled without
        // literal backslashes so the Kotlin source stays verifiable.
        val B = 92.toChar()
        val Q = 34.toChar()
        val patternText =
            Q + field + Q + B + "s*:" + B + "s*" + Q + "((?:" + B + B + ".|[^" + Q + B + B + "])*)" + Q
        val hit = Regex(patternText).find(json) ?: return null
        return unescape(hit.groupValues[1])
    }

    /** Minimal JSON string unescape used by the codec (\" \\ \/ \n \r \t). */
    fun unescape(value: String): String {
        if (!value.contains(92.toChar())) return value
        val out = StringBuilder(value.length)
        var i = 0
        while (i < value.length) {
            val ch = value[i]
            if (ch != 92.toChar() || i + 1 >= value.length) {
                out.append(ch); i++; continue
            }
            when (val next = value[i + 1]) {
                34.toChar() -> out.append(34.toChar())
                92.toChar() -> out.append(92.toChar())
                '/' -> out.append('/')
                'n' -> out.append('\n')
                'r' -> out.append('\r')
                't' -> out.append('\t')
                else -> out.append(next)
            }
            i += 2
        }
        return out.toString()
    }

    private fun urlencode(value: String): String =
        URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    private fun urldecode(value: String): String =
        runCatching { URLDecoder.decode(value, "UTF-8") }.getOrDefault(value)
}
