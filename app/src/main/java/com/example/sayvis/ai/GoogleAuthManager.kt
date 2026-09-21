package com.example.sayvis.ai

import android.content.Context
import android.content.Intent
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder
import java.security.SecureRandom
import java.security.MessageDigest

/**
 * Real Google OAuth 2.0 (RFC 6749 + RFC 7636 PKCE) sign-in for the app itself.
 *
 * The owner creates an OAuth client once in Google Cloud Console (APIs &
 * Services → Credentials → OAuth client ID → Web application, redirect
 * `sayvis://oauth2`), pastes the client ID into Settings → Google account, and
 * from then on "Sign in with Google" runs the standard browser consent flow:
 * Chrome Custom Tab → accounts.google.com → consent → redirect back into
 * SAYVIS with an authorization code → token exchange (PKCE, no client
 * secret needed for installed apps) → profile from the id_token.
 *
 * The refresh token lives only in the Keystore-backed vault; the short-lived
 * access token and the PKCE verifier of an in-flight flow live in the
 * `sayvis_oauth` preferences file.
 */
object GoogleAuthManager {

    const val REDIRECT_URI: String = "sayvis://oauth2"
    const val AUTHORIZE_URL: String = "https://accounts.google.com/o/oauth2/v2/auth"
    const val TOKEN_URL: String = "https://oauth2.googleapis.com/token"

    /** Read-only Google capabilities SAYVIS asks consent for. */
    val SCOPES: List<String> = listOf(
        "openid", "email", "profile",
        "https://www.googleapis.com/auth/gmail.readonly",
        "https://www.googleapis.com/auth/calendar.readonly",
        "https://www.googleapis.com/auth/drive.readonly"
    )

    const val PREFS: String = "sayvis_oauth"

    data class Profile(val email: String, val name: String, val picture: String)

    data class TokenSet(
        val accessToken: String,
        val refreshToken: String?,
        val idToken: String?,
        val expiresInSeconds: Long
    )

    // ------------------------------------------------------------ PKCE (pure)

    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"

    fun generateState(): String {
        val random = SecureRandom()
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return bytes.joinToString("") { String.format("%02x", it) }
    }

    fun generateVerifier(): String {
        val random = SecureRandom()
        val bytes = ByteArray(48) // 64 base64url chars — well inside 43..128
        random.nextBytes(bytes)
        return bytes.map { ALPHABET[it.toInt().and(0x3F)] }.joinToString("")
    }

    /** S256 code challenge: BASE64URL(SHA256(verifier)) without padding. */
    fun codeChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
        return base64UrlEncode(digest)
    }

    fun buildAuthorizeUrl(clientId: String, verifier: String, state: String): String {
        val scope = SCOPES.joinToString(" ")
        return AUTHORIZE_URL +
            "?client_id=" + urlencode(clientId) +
            "&redirect_uri=" + urlencode(REDIRECT_URI) +
            "&response_type=code" +
            "&scope=" + urlencode(scope) +
            "&state=" + urlencode(state) +
            "&code_challenge=" + codeChallenge(verifier) +
            "&code_challenge_method=S256" +
            "&access_type=offline" +
            "&prompt=consent" +
            "&include_granted_scopes=true"
    }

    // ------------------------------------------------------- id_token (pure)

    /** Decodes the JWT payload of an id_token into a profile (no signature check —
     *  the token arrives directly from Google's TLS token endpoint). */
    fun parseIdToken(idToken: String): Profile? {
        val parts = idToken.split(".")
        if (parts.size < 2) return null
        val payloadJson = runCatching {
            String(base64UrlDecode(parts[1]) ?: return null, Charsets.UTF_8)
        }.getOrNull() ?: return null
        val email = miniJsonString(payloadJson, "email") ?: return null
        return Profile(
            email = email,
            name = miniJsonString(payloadJson, "name").orEmpty(),
            picture = miniJsonString(payloadJson, "picture").orEmpty()
        )
    }

    /** Minimal escaped-JSON string field extractor (unit-test friendly). */
    fun miniJsonString(json: String, field: String): String? {
        val regex = Regex("\\\"$field\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"")
        val hit = regex.find(json) ?: return null
        return WebSearchService.jsonUnescape(hit.groupValues[1])
    }

    // -------------------------------------------------------- Base64URL (pure)

    fun base64UrlEncode(bytes: ByteArray): String {
        val map = ALPHABET
        val out = StringBuilder()
        var buffer = 0
        var bits = 0
        for (b in bytes) {
            buffer = (buffer shl 8) or (b.toInt() and 0xFF)
            bits += 8
            while (bits >= 6) {
                bits -= 6
                out.append(map[(buffer shr bits) and 0x3F])
            }
        }
        if (bits > 0) out.append(map[(buffer shl (6 - bits)) and 0x3F])
        return out.toString()
    }

    fun base64UrlDecode(value: String): ByteArray? {
        val clean = value.trim().trimEnd('=')
        val buffer = ByteArray(clean.length * 3 / 4 + 3)
        var written = 0
        var acc = 0
        var bits = 0
        for (ch in clean) {
            val v = ALPHABET.indexOf(ch)
            if (v < 0) return null
            acc = (acc shl 6) or v
            bits += 6
            if (bits >= 8) {
                bits -= 8
                buffer[written] = ((acc shr bits) and 0xFF).toByte()
                written++
            }
        }
        return buffer.copyOf(written)
    }

    private fun urlencode(value: String): String =
        URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    // ------------------------------------------------- flow state (device IO)

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Persists the in-flight PKCE pair and returns the consent-screen Uri. */
    fun beginSignIn(context: Context, clientId: String): Uri? {
        val trimmed = clientId.trim()
        if (trimmed.isBlank()) return null
        val verifier = generateVerifier()
        val state = generateState()
        prefs(context).edit()
            .putString("verifier", verifier)
            .putString("state", state)
            .putLong("startedAt", System.currentTimeMillis())
            .apply()
        return Uri.parse(buildAuthorizeUrl(trimmed, verifier, state))
    }

    fun openBrowser(context: Context, clientId: String): Boolean {
        val uri = beginSignIn(context, clientId) ?: return false
        return runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        }.getOrDefault(false)
    }

    /** Verifier + state of the pending flow, or null (expired after 10 minutes). */
    fun pendingFlow(context: Context): Pair<String, String>? {
        val prefs = prefs(context)
        val startedAt = prefs.getLong("startedAt", 0L)
        if (startedAt == 0L || System.currentTimeMillis() - startedAt > 10 * 60_000L) return null
        val verifier = prefs.getString("verifier", null) ?: return null
        val state = prefs.getString("state", null) ?: return null
        return verifier to state
    }

    fun clearPendingFlow(context: Context) {
        prefs(context).edit().clear().apply()
    }

    // -------------------------------------------------- token cache in prefs

    fun cachedAccessToken(context: Context): String? {
        val prefs = prefs(context)
        val token = prefs.getString("accessToken", null) ?: return null
        val expiry = prefs.getLong("accessExpiry", 0L)
        return if (System.currentTimeMillis() < expiry - 60_000L) token else null
    }

    fun cacheAccessToken(context: Context, tokenSet: TokenSet) {
        prefs(context).edit()
            .putString("accessToken", tokenSet.accessToken)
            .putLong("accessExpiry", System.currentTimeMillis() + tokenSet.expiresInSeconds * 1000L)
            .apply()
    }

    /**
     * A usable access token: from cache when fresh, otherwise refreshed with the
     * vault-stored refresh token. Returns null when the owner is not signed in.
     */
    suspend fun validAccessToken(
        context: Context,
        refreshToken: String,
        clientId: String
    ): String? {
        cachedAccessToken(context)?.let { return it }
        if (refreshToken.isBlank() || clientId.isBlank()) return null
        val refreshed = runCatching { refresh(clientId, refreshToken) }.getOrNull() ?: return null
        cacheAccessToken(context, refreshed)
        return refreshed.accessToken
    }

    // -------------------------------------------------------- token endpoint

    suspend fun exchange(clientId: String, code: String, verifier: String): TokenSet =
        tokenRequest(
            mapOf(
                "code" to code,
                "client_id" to clientId.trim(),
                "code_verifier" to verifier,
                "redirect_uri" to REDIRECT_URI,
                "grant_type" to "authorization_code"
            )
        )

    suspend fun refresh(clientId: String, refreshToken: String): TokenSet =
        tokenRequest(
            mapOf(
                "refresh_token" to refreshToken,
                "client_id" to clientId.trim(),
                "grant_type" to "refresh_token"
            )
        )

    private suspend fun tokenRequest(form: Map<String, String>): TokenSet =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val body = form.entries.joinToString("&") {
                urlencode(it.key) + "=" + urlencode(it.value)
            }
            val request = okhttp3.Request.Builder()
                .url(TOKEN_URL)
                .post(body.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                .build()
            com.example.sayvis.net.SayvisNet.client(15, 15)
                .newCall(request)
                .execute()
                .use { response ->
                    val raw = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        throw IllegalStateException("token endpoint ${response.code}: ${raw.take(200)}")
                    }
                    val json = JSONObject(raw)
                    TokenSet(
                        accessToken = json.optString("access_token"),
                        refreshToken = json.optString("refresh_token").ifBlank { null },
                        idToken = json.optString("id_token").ifBlank { null },
                        expiresInSeconds = json.optLong("expires_in", 3600L)
                    )
                }
        }
}
