package com.example.sayvis.identity

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.content.IntentSender
import com.example.sayvis.ai.GoogleAuthManager
import com.example.sayvis.net.SayvisNet
import com.example.sayvis.settings.SettingsStore

/**
 * The identity centre (v4.0.0): the owner's Google account is the single hub
 * behind every login and every AI usage in SAYVIS.
 *
 * Why this replaces the old flow: the previous sign-in path required an OAuth
 * client ID created in Google Cloud Console and stayed disabled without it —
 * the login problem the owner reported. The system Google account chooser
 * (`AccountManager.newChooseAccountIntent`) needs NO console setup, NO SHA-1
 * fingerprint and NO client ID: it is the same one-tap sheet every mainstream
 * app uses, and it works on any GMS device out of the box. The advanced
 * PKCE/OAuth flow is still available for the Gmail/Calendar/Drive scopes,
 * but it is optional.
 *
 * After linking, [SayvisNet.identityAccount] carries the account into every
 * outgoing request, so the account is factually the centre of all logins and
 * of SAYVIS's AI/network usage.
 */
object GoogleAccountHub {

    private const val GOOGLE_TYPE = "com.google"
    private const val PREFS = "sayvis_account_hub"
    private const val K_PIN = "devicePin"

    sealed class LinkResult {
        data class Success(val email: String, val displayName: String) : LinkResult()
        data class Invalid(val reasonFa: String, val reasonEn: String) : LinkResult() {
            fun message(isPersian: Boolean) = if (isPersian) reasonFa else reasonEn
        }
    }

    // ------------------------------------------------------------ pure parts

    fun normalizeEmail(raw: String): String = raw.trim().lowercase()

    /** Simple, dependency-free e-mail plausibility check (unit-tested). */
    fun isValidEmail(raw: String): Boolean {
        val v = normalizeEmail(raw)
        val parts = v.split("@")
        if (parts.size != 2) return false
        val local = parts[0]
        val domain = parts[1]
        if (local.isEmpty() || local.length > 64) return false
        if (domain.isEmpty() || !domain.contains('.')) return false
        if (domain.startsWith(".") || domain.endsWith(".") || domain.contains("..")) return false
        return domain.all { it.isLetterOrDigit() || it == '.' || it == '-' }
    }

    /** Human display name derived from the e-mail local part. */
    fun displayNameFor(email: String): String {
        val local = normalizeEmail(email).substringBefore('@')
        if (local.isEmpty()) return "Google"
        return local.split('.', '_', '-', '+')
            .filter { it.isNotBlank() }
            .joinToString(" ") { part ->
                part.replaceFirstChar { it.uppercase() }.take(24)
            }
            .ifBlank { "Google" }
    }

    // ------------------------------------------------------- device chooser

    /**
     * The system "choose a Google account" sheet. Returns the launch
     * IntentSender, or null when the chooser is unavailable on this device.
     */
    fun chooseAccountSender(context: Context): IntentSender? =
        AccountManagerProxy.choose(context.applicationContext, GOOGLE_TYPE)

    // ---------------------------------------------------------- link/unlink

    /**
     * Persists the Google account as the owner identity: settings, the
     * network identity header, and the device pin used by pairing QRs.
     */
    fun link(context: Context, rawEmail: String): LinkResult {
        val email = normalizeEmail(rawEmail)
        if (!isValidEmail(email)) {
            return LinkResult.Invalid("این آدرس ایمیل معتبر نیست.", "This e-mail address is not valid.")
        }
        val store = SettingsStore.get(context.applicationContext)
        store.update { current ->
            current.copy(
                google = current.google.copy(
                    email = email,
                    displayName = displayNameFor(email),
                    signedInAtEpochMs = System.currentTimeMillis()
                )
            )
        }
        SayvisNet.identityAccount = email
        ensureDevicePin(context.applicationContext)
        return LinkResult.Success(email, displayNameFor(email))
    }

    /** Clears the Google identity everywhere (hub sign-out). */
    fun unlink(context: Context) {
        val app = context.applicationContext
        SettingsStore.get(app).update { current ->
            current.copy(
                google = current.google.copy(
                    email = "",
                    displayName = "",
                    pictureUrl = "",
                    signedInAtEpochMs = 0L,
                    grantedScopes = ""
                )
            )
        }
        SayvisNet.identityAccount = ""
        runCatching {
            app.getSharedPreferences(GoogleAuthManager.PREFS, Context.MODE_PRIVATE)
                .edit().clear().apply()
        }
    }

    /** Stable 6-digit pairing pin for the QR payloads of this device. */
    fun ensureDevicePin(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getString(K_PIN, null)?.let { return it }
        val pin = (100000..999999).random().toString()
        prefs.edit().putString(K_PIN, pin).apply()
        return pin
    }

    fun devicePin(context: Context): String = ensureDevicePin(context)
}

/**
 * Isolated so the JVM unit tests never touch the deprecated platform method —
 * uses the deprecated instance form of newChooseAccountIntent, which works on
 * every supported API level (the static variant is API 26+).
 */
internal object AccountManagerProxy {
    @Suppress("DEPRECATION")
    fun choose(context: Context, accountType: String): IntentSender =
        AccountManager.get(context).newChooseAccountIntent(
            null as Account?,
            null,
            arrayOf(accountType),
            false,
            null,
            null,
            null
        )
}
