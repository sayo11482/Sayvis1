package com.example.sayvis.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/**
 * Reactive, persisted owner settings.
 *
 * Non-secret values live in a JSON blob in SharedPreferences. Secrets (AI API keys,
 * broker password, bridge tokens) live in [SecureVault] and are merged back into the
 * in-memory [AppSettings] at load time, so the rest of the app can read a single
 * consistent object without ever touching storage details.
 */
class SettingsStore private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("sayvis_settings", Context.MODE_PRIVATE)

    val vault = SecureVault(appContext)

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    /** Last persisted AI round-trip result, surfaced in the Settings screen. */
    private val _connectionProbe = MutableStateFlow<ProviderProbe?>(null)
    val connectionProbe: StateFlow<ProviderProbe?> = _connectionProbe.asStateFlow()

    init {
        _settings.value = load()
    }

    fun current(): AppSettings = _settings.value

    /** Applies [mutator] and persists the result atomically. */
    fun update(mutator: (AppSettings) -> AppSettings): AppSettings {
        val next = mutator(_settings.value)
        _settings.value = next
        persist(next)
        return next
    }

    fun setLanguage(language: AppLanguage) = update { it.copy(localization = it.localization.copy(language = language)) }

    fun setPersianDigits(enabled: Boolean) = update { it.copy(localization = it.localization.copy(persianDigits = enabled)) }

    fun setAutoTranslate(enabled: Boolean) = update { it.copy(localization = it.localization.copy(autoTranslateFreeText = enabled)) }

    fun setProvider(provider: AiProviderKind) = update { it.copy(ai = it.ai.copy(provider = provider)) }

    fun setForceOffline(enabled: Boolean) = update { it.copy(forceOfflineMode = enabled) }

    fun setEmergencyLock(active: Boolean) = update { it.copy(emergencyLockActive = active) }

    fun setExecutionMode(mode: TradingExecutionMode) = update { it.copy(trading = it.trading.copy(executionMode = mode)) }

    fun setAppearance(mode: AppearanceMode) = update { it.copy(appearance = mode) }

    fun recordProbe(probe: ProviderProbe) {
        _connectionProbe.value = probe
    }

    // ---------------------------------------------------------------- secrets

    /** Writes a secret into the Keystore vault (never into the JSON blob). */
    fun putSecret(key: SecretKey, value: String) {
        if (value.isEmpty()) vault.remove(key.vaultKey) else vault.put(key.vaultKey, value)
    }

    fun getSecret(key: SecretKey): String = vault.get(key.vaultKey)

    // ------------------------------------------------------------ persistence

    private fun load(): AppSettings {
        val raw = prefs.getString(KEY_SETTINGS_JSON, null)
        val base = if (raw.isNullOrBlank()) AppSettings() else runCatching { decode(raw) }.getOrElse { AppSettings() }

        // Merge secrets back in from the vault.
        val ai = base.ai.copy(
            geminiApiKey = getSecret(SecretKey.GEMINI_API_KEY),
            openRouterApiKey = getSecret(SecretKey.OPENROUTER_API_KEY),
            groqApiKey = getSecret(SecretKey.GROQ_API_KEY),
            openAiApiKey = getSecret(SecretKey.OPENAI_API_KEY),
            xaiApiKey = getSecret(SecretKey.XAI_API_KEY),
            customApiKey = getSecret(SecretKey.CUSTOM_API_KEY)
        )
        val trading = base.trading.copy(
            password = getSecret(SecretKey.MT_PASSWORD),
            bridgeToken = getSecret(SecretKey.MT_BRIDGE_TOKEN)
        )
        return base.copy(ai = ai, trading = trading)
    }

    private fun persist(settings: AppSettings) {
        // Secrets are stripped from the JSON blob and pushed to the vault instead.
        putSecret(SecretKey.GEMINI_API_KEY, settings.ai.geminiApiKey)
        putSecret(SecretKey.OPENROUTER_API_KEY, settings.ai.openRouterApiKey)
        putSecret(SecretKey.GROQ_API_KEY, settings.ai.groqApiKey)
        putSecret(SecretKey.OPENAI_API_KEY, settings.ai.openAiApiKey)
        putSecret(SecretKey.XAI_API_KEY, settings.ai.xaiApiKey)
        putSecret(SecretKey.CUSTOM_API_KEY, settings.ai.customApiKey)
        putSecret(SecretKey.LINKED_SITES, settings.linked.linkedSites)
        putSecret(SecretKey.MT_PASSWORD, settings.trading.password)
        putSecret(SecretKey.MT_BRIDGE_TOKEN, settings.trading.bridgeToken)

        val redacted = settings.copy(
            ai = settings.ai.copy(
                geminiApiKey = "", openRouterApiKey = "", groqApiKey = "",
                openAiApiKey = "", xaiApiKey = "", customApiKey = ""
            ),
            linked = settings.linked.copy(linkedSites = ""),
            trading = settings.trading.copy(password = "", bridgeToken = "")
        )
        prefs.edit().putString(KEY_SETTINGS_JSON, encode(redacted)).apply()
    }

    private fun encode(s: AppSettings): String = JSONObject().apply {
        put("schema", s.settingsSchemaVersion)
        put("appearance", s.appearance.name)
        put("forceOffline", s.forceOfflineMode)
        put("emergencyLock", s.emergencyLockActive)
        put("requireConfirmHighRisk", s.requireConfirmationForHighRisk)
        put("keepAuditLog", s.keepAuditLogOnDevice)
        put("runScripts", s.runAutomationScripts)
        put("haptics", s.hapticFeedback)
        put("compactNav", s.compactBottomNav)
        put("visualStyle", s.visualStyle.name)
        put("roboticVoice", s.roboticVoiceReplies)
        put("onboardingDone", s.onboardingCompleted)

        put("loc", JSONObject().apply {
            put("language", s.localization.language.name)
            put("persianDigits", s.localization.persianDigits)
            put("autoTranslate", s.localization.autoTranslateFreeText)
            put("markMt", s.localization.markMachineTranslated)
            put("forceRtl", s.localization.forceRtlForPersian)
        })

        put("ai", JSONObject().apply {
            put("provider", s.ai.provider.name)
            put("geminiModel", s.ai.geminiModel)
            put("openRouterModel", s.ai.openRouterModel)
            put("groqModel", s.ai.groqModel)
            put("openAiModel", s.ai.openAiModel)
            put("xaiModel", s.ai.xaiModel)
            put("customBaseUrl", s.ai.customBaseUrl)
            put("customModel", s.ai.customModel)
            put("persona", s.ai.systemPersona)
            put("temperature", s.ai.temperature)
            put("maxTokens", s.ai.maxOutputTokens)
            put("timeout", s.ai.timeoutSeconds)
            put("forceLang", s.ai.forceResponseLanguage)
        })

        put("evolutionBacklog", s.evolutionBacklog)
        put("tradeAuto", s.tradeAutomationEnabled)
        put("tradeTuning", s.tradeTuningJson)
        put("assistantBrain", s.assistantBrain)
        put("searchTaste", s.searchTasteJson)
        put("bizDirectory", s.bizDirectoryJson)
        put("linked", JSONObject().apply {
            put("instagramHandle", s.linked.instagramHandle)
        })

        put("google", JSONObject().apply {
            put("clientId", s.google.clientId)
            put("email", s.google.email)
            put("name", s.google.displayName)
            put("photo", s.google.pictureUrl)
            put("signedInAt", s.google.signedInAtEpochMs)
            put("scopes", s.google.grantedScopes)
            put("requireSignIn", s.google.requireSignInAtLaunch)
        })

        put("mt", JSONObject().apply {
            val t = s.trading
            put("id", t.id)
            put("name", t.profileName)
            put("bridge", t.bridgeKind.name)
            put("version", t.terminalVersion.name)
            put("accountType", t.accountType.name)
            put("broker", t.brokerName)
            put("server", t.serverAddress)
            put("login", t.login)
            put("bridgeUrl", t.bridgeUrl)
            put("execMode", t.executionMode.name)
            put("maxLoss", t.maxDailyLossUsd)
            put("maxLot", t.maxLotSize)
            put("autoClose", t.autoCloseOnDrawdown)
            put("enabled", t.enabled)
        })
    }.toString()

    private fun decode(raw: String): AppSettings {
        val root = JSONObject(raw)
        val loc = root.optJSONObject("loc") ?: JSONObject()
        val ai = root.optJSONObject("ai") ?: JSONObject()
        val mt = root.optJSONObject("mt") ?: JSONObject()

        return AppSettings(
            settingsSchemaVersion = root.optInt("schema", 3),
            appearance = enumOr(root.optString("appearance"), AppearanceMode.DARK_SPACE),
            forceOfflineMode = root.optBoolean("forceOffline", false),
            emergencyLockActive = root.optBoolean("emergencyLock", false),
            requireConfirmationForHighRisk = root.optBoolean("requireConfirmHighRisk", true),
            keepAuditLogOnDevice = root.optBoolean("keepAuditLog", true),
            runAutomationScripts = root.optBoolean("runScripts", true),
            hapticFeedback = root.optBoolean("haptics", true),
            compactBottomNav = root.optBoolean("compactNav", true),
            visualStyle = enumOr(root.optString("visualStyle"), AiVisualStyle.GEOMETRIC),
            roboticVoiceReplies = root.optBoolean("roboticVoice", true),
            onboardingCompleted = root.optBoolean("onboardingDone", false),
            localization = LocalizationSettings(
                language = enumOr(loc.optString("language"), AppLanguage.PERSIAN),
                persianDigits = loc.optBoolean("persianDigits", true),
                autoTranslateFreeText = loc.optBoolean("autoTranslate", true),
                markMachineTranslated = loc.optBoolean("markMt", true),
                forceRtlForPersian = loc.optBoolean("forceRtl", true)
            ),
            ai = AiSettings(
                provider = enumOr(ai.optString("provider"), AiProviderKind.LOCAL),
                geminiModel = ai.optString("geminiModel", "gemini-3.6-flash")
                    .let { stored -> if (stored == "gemini-2.5-flash") "gemini-3.6-flash" else stored }, // retired-model migration
                openRouterModel = ai.optString("openRouterModel", "anthropic/claude-3.5-sonnet"),
                groqModel = ai.optString("groqModel", "llama-3.3-70b-versatile"),
                openAiModel = ai.optString("openAiModel", "gpt-4o-mini"),
                xaiModel = ai.optString("xaiModel", "grok-3-mini"),
                customBaseUrl = ai.optString("customBaseUrl", ""),
                customModel = ai.optString("customModel", ""),
                systemPersona = ai.optString("persona", ""),
                temperature = ai.optDouble("temperature", 0.7),
                maxOutputTokens = ai.optInt("maxTokens", 2048),
                timeoutSeconds = ai.optInt("timeout", 25),
                forceResponseLanguage = ai.optBoolean("forceLang", true)
            ),
            evolutionBacklog = root.optString("evolutionBacklog", ""),
            tradeAutomationEnabled = root.optBoolean("tradeAuto", false),
            tradeTuningJson = root.optString("tradeTuning", ""),
            assistantBrain = root.optString("assistantBrain", "AUTO"),
            searchTasteJson = root.optString("searchTaste", ""),
            bizDirectoryJson = root.optString("bizDirectory", ""),
            linked = run {
                val l = root.optJSONObject("linked") ?: JSONObject()
                LinkedAccountSettings(
                    instagramHandle = l.optString("instagramHandle", ""),
                    linkedSites = getSecret(SecretKey.LINKED_SITES)
                )
            },
            google = run {
                val g = root.optJSONObject("google") ?: JSONObject()
                GoogleAccountSettings(
                    clientId = g.optString("clientId", ""),
                    email = g.optString("email", ""),
                    displayName = g.optString("name", ""),
                    pictureUrl = g.optString("photo", ""),
                    signedInAtEpochMs = g.optLong("signedInAt", 0L),
                    grantedScopes = g.optString("scopes", ""),
                    requireSignInAtLaunch = g.optBoolean("requireSignIn", false)
                )
            },
            trading = MtGatewayProfile(
                id = mt.optString("id", "mt_primary"),
                profileName = mt.optString("name", ""),
                bridgeKind = enumOr(mt.optString("bridge"), MtBridgeKind.METAAPI),
                terminalVersion = enumOr(mt.optString("version"), MtTerminalVersion.MT5),
                accountType = enumOr(mt.optString("accountType"), MtAccountType.DEMO),
                brokerName = mt.optString("broker", ""),
                serverAddress = mt.optString("server", ""),
                login = mt.optString("login", ""),
                bridgeUrl = mt.optString("bridgeUrl", ""),
                executionMode = enumOr(mt.optString("execMode"), TradingExecutionMode.PAPER_SIMULATION),
                maxDailyLossUsd = mt.optDouble("maxLoss", 50.0),
                maxLotSize = mt.optDouble("maxLot", 0.10),
                autoCloseOnDrawdown = mt.optBoolean("autoClose", true),
                enabled = mt.optBoolean("enabled", false)
            )
        )
    }

    private inline fun <reified T : Enum<T>> enumOr(raw: String?, fallback: T): T =
        if (raw.isNullOrBlank()) fallback else runCatching { enumValueOf<T>(raw) }.getOrDefault(fallback)

    /** Factory for wiping every owner-configured value (keeps audit log intact). */
    fun resetToDefaults() {
        prefs.edit().clear().apply()
        SecretKey.entries.forEach { vault.remove(it.vaultKey) }
        _settings.value = AppSettings()
        _connectionProbe.value = null
    }

    companion object {
        private const val KEY_SETTINGS_JSON = "settings_json_v3"

        @Volatile
        private var INSTANCE: SettingsStore? = null

        fun get(context: Context): SettingsStore =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsStore(context).also { INSTANCE = it }
            }
    }
}

/** Vault slot identifiers for every secret SAYVIS can hold. */
enum class SecretKey(val vaultKey: String, val labelFa: String, val labelEn: String) {
    GEMINI_API_KEY("sec_gemini_key", "کلید API گوگل جمینای", "Google Gemini API key"),
    OPENROUTER_API_KEY("sec_openrouter_key", "کلید API اوپن‌روتر", "OpenRouter API key"),
    GROQ_API_KEY("sec_groq_key", "کلید API گروک", "Groq API key"),
    OPENAI_API_KEY("sec_openai_key", "کلید API چت‌جی‌پی‌تی", "ChatGPT / OpenAI API key"),
    XAI_API_KEY("sec_xai_key", "کلید API گراک", "Grok / xAI API key"),
    GOOGLE_REFRESH_TOKEN("sec_google_refresh", "توکن تازه‌سازی گوگل", "Google refresh token"),
    LINKED_SITES("sec_linked_sites", "نشست حساب‌های متصل", "Linked site sessions"),
    CUSTOM_API_KEY("sec_custom_key", "کلید سرویس دلخواه", "Custom service key"),
    MT_PASSWORD("sec_mt_password", "رمز حساب متاتریدر", "MetaTrader account password"),
    MT_BRIDGE_TOKEN("sec_mt_token", "توکن پل ارتباطی", "Bridge / gateway token");

    fun label(isPersian: Boolean): String = if (isPersian) labelFa else labelEn
}

/** Result of a live provider reachability test. */
data class ProviderProbe(
    val provider: AiProviderKind,
    val model: String,
    val success: Boolean,
    val latencyMs: Long,
    val messageFa: String,
    val messageEn: String,
    val at: Long = System.currentTimeMillis()
) {
    fun message(isPersian: Boolean): String = if (isPersian) messageFa else messageEn
}
