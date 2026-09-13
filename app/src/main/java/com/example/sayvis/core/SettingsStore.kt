package com.example.sayvis.core

import android.content.Context
import android.content.SharedPreferences

/**
 * Local, on-device settings store for SAYVIS runtime configuration.
 *
 * API keys and provider selection live in private SharedPreferences and never leave
 * the device except in direct HTTPS calls to the provider the owner explicitly chose.
 * This replaces the old build-time-only key injection, so the owner can activate the
 * cloud brain at runtime without rebuilding the APK.
 */
object SettingsStore {

    private const val PREFS_NAME = "sayvis_settings"

    const val DEFAULT_GEMINI_MODEL = "gemini-3.5-flash"
    const val DEFAULT_OPENROUTER_MODEL = "openai/gpt-4o-mini"
    const val DEFAULT_GROQ_MODEL = "llama-3.3-70b-versatile"

    const val PROVIDER_AUTO = "auto"
    const val PROVIDER_GEMINI = "gemini"
    const val PROVIDER_OPENROUTER = "openrouter"
    const val PROVIDER_GROQ = "groq"
    const val PROVIDER_LOCAL = "local"

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            synchronized(this) {
                if (prefs == null) {
                    prefs = context.applicationContext
                        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                }
            }
        }
    }

    private fun read(key: String): String = prefs?.getString(key, null)?.trim() ?: ""

    private fun write(key: String, value: String) {
        prefs?.edit()?.putString(key, value.trim())?.apply()
    }

    var geminiKey: String
        get() = read("gemini_key")
        set(value) = write("gemini_key", value)

    var openRouterKey: String
        get() = read("openrouter_key")
        set(value) = write("openrouter_key", value)

    var groqKey: String
        get() = read("groq_key")
        set(value) = write("groq_key", value)

    var geminiModel: String
        get() = read("gemini_model").ifBlank { DEFAULT_GEMINI_MODEL }
        set(value) = write("gemini_model", value)

    var openRouterModel: String
        get() = read("openrouter_model").ifBlank { DEFAULT_OPENROUTER_MODEL }
        set(value) = write("openrouter_model", value)

    var groqModel: String
        get() = read("groq_model").ifBlank { DEFAULT_GROQ_MODEL }
        set(value) = write("groq_model", value)

    /** auto | gemini | openrouter | groq | local */
    var providerChoice: String
        get() = read("provider_choice").ifBlank { PROVIDER_AUTO }
        set(value) = write("provider_choice", value)

    fun anyCloudConfigured(): Boolean =
        geminiKey.isNotBlank() || openRouterKey.isNotBlank() || groqKey.isNotBlank()
}
