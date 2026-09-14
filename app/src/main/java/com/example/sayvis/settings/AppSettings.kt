package com.example.sayvis.settings

/**
 * Global, owner-controlled configuration surface for SAYVIS.
 *
 * Everything here is persisted on-device (see [SettingsStore]) and every change is
 * routed through the Zero-Trust audit trail. No value in this file ever leaves the
 * device unless the owner explicitly enables a network provider or trading gateway.
 */

/** UI language policy. */
enum class AppLanguage(val labelFa: String, val labelEn: String) {
    PERSIAN("فارسی", "Persian"),
    ENGLISH("انگلیسی", "English"),
    SYSTEM("خودکار (زبان دستگاه)", "Auto (device language)");

    fun label(isPersian: Boolean): String = if (isPersian) labelFa else labelEn
}

/** Selectable AI inference backends. */
enum class AiProviderKind(val labelFa: String, val labelEn: String, val isLocal: Boolean) {
    LOCAL("هستهٔ محلی سایویس (آفلاین)", "SAYVIS Local Core (offline)", true),
    GEMINI("گوگل جمینای", "Google Gemini", false),
    OPENROUTER("اوپن‌روتر (چندمدلی)", "OpenRouter (multi-model)", false),
    GROQ("گروک (پاسخ سریع)", "Groq (fast LPU)", false),
    CUSTOM("سرویس دلخواه سازگار با OpenAI", "Custom OpenAI-compatible", false)
    OPENAI("چت‌جی‌پی‌تی (OpenAI)", "ChatGPT (OpenAI)", false),
    XAI("گراک (xAI)", "Grok (xAI)", false),;

    fun label(isPersian: Boolean): String = if (isPersian) labelFa else labelEn
}

/** Visual density / theme preference. */
enum class AppearanceMode(val labelFa: String, val labelEn: String) {
    DARK_SPACE("تیرهٔ فضایی (پیش‌فرض)", "Deep Space Dark (default)"),
    HIGH_CONTRAST("کنتراست بالا", "High Contrast"),
    COMPACT("فشرده", "Compact");

    fun label(isPersian: Boolean): String = if (isPersian) labelFa else labelEn
}

/**
 * The four professional multidimensional looks for the AI avatar: geometric
 * wireframes, stereologic cross-sections, binary 0/1 code rain and hologram.
 */
enum class AiVisualStyle(val labelFa: String, val labelEn: String) {
    GEOMETRIC("ژئومتریک", "Geometric"),
    STEREOLOGY("استرولوژی", "Stereology"),
    BINARY("دودویی ۰/۱", "Binary 0/1"),
    HOLOGRAM("هولوگرام", "Hologram");

    fun label(isPersian: Boolean): String = if (isPersian) labelFa else labelEn

    companion object {
        /** Lenient parser used by the settings store (unknown -> default). */
        fun fromNameOrDefault(raw: String?): AiVisualStyle =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: GEOMETRIC
    }
}

/** MetaTrader terminal generation. */
enum class MtTerminalVersion(val labelFa: String, val labelEn: String) {
    MT5("متاتریدر ۵", "MetaTrader 5"),
    MT4("متاتریدر ۴", "MetaTrader 4");

    fun label(isPersian: Boolean): String = if (isPersian) labelFa else labelEn
}

/** Account class on the broker side. */
enum class MtAccountType(val labelFa: String, val labelEn: String) {
    DEMO("دمو (آزمایشی)", "Demo"),
    REAL("واقعی", "Real");

    fun label(isPersian: Boolean): String = if (isPersian) labelFa else labelEn
}

/**
 * How far SAYVIS is allowed to go with a connected trading terminal.
 *
 * PAPER_SIMULATION  -> terminal data may be read, orders are simulated locally only.
 * DEMO_EXECUTION    -> orders may be sent to a DEMO account through the bridge.
 * LIVE_EXECUTION    -> orders may be sent to a REAL account. Requires an explicit,
 *                        re-confirmed owner unlock plus a disengaged emergency lock.
 */
enum class TradingExecutionMode(val labelFa: String, val labelEn: String, val maxRiskTier: Int) {
    PAPER_SIMULATION("شبیه‌سازی کاغذی (بدون ارسال سفارش)", "Paper simulation (no order routing)", 0),
    DEMO_EXECUTION("اجرای واقعی روی حساب دمو", "Real routing on DEMO account", 1),
    LIVE_EXECUTION("اجرای زنده روی حساب واقعی", "Live routing on REAL account", 2);

    fun label(isPersian: Boolean): String = if (isPersian) labelFa else labelEn
}

/** Transport used to reach a MetaTrader server from Android. */
enum class MtBridgeKind(val labelFa: String, val labelEn: String) {
    METAAPI("MetaApi (سرویس ابری آماده)", "MetaApi (cloud gateway)"),
    SELF_HOSTED("پل شخصی (Expert Advisor + REST/WebSocket)", "Self-hosted EA bridge (REST/WebSocket)"),
    OFFLINE_SIM("شبیه‌ساز محلی (بدون اتصال)", "Local simulator (no connection)");

    fun label(isPersian: Boolean): String = if (isPersian) labelFa else labelEn
}

/** Connection profile for one trading terminal account. */
data class MtGatewayProfile(
    val id: String = "mt_primary",
    val profileName: String = "",
    val bridgeKind: MtBridgeKind = MtBridgeKind.METAAPI,
    val terminalVersion: MtTerminalVersion = MtTerminalVersion.MT5,
    val accountType: MtAccountType = MtAccountType.DEMO,
    val brokerName: String = "",
    val serverAddress: String = "",
    val login: String = "",
    /** Stored ONLY inside the Keystore-backed vault, never in the JSON blob. */
    val password: String = "",
    /** Bridge/API token (MetaApi account token or self-hosted bridge secret). */
    val bridgeToken: String = "",
    val bridgeUrl: String = "",
    val executionMode: TradingExecutionMode = TradingExecutionMode.PAPER_SIMULATION,
    val maxDailyLossUsd: Double = 50.0,
    val maxLotSize: Double = 0.10,
    val autoCloseOnDrawdown: Boolean = true,
    val enabled: Boolean = false
)

/** AI inference configuration block. */
data class AiSettings(
    val provider: AiProviderKind = AiProviderKind.LOCAL,
    val geminiApiKey: String = "",
    val geminiModel: String = "gemini-2.5-flash",
    val openRouterApiKey: String = "",
    val openRouterModel: String = "anthropic/claude-3.5-sonnet",
    val groqApiKey: String = "",
    val groqModel: String = "llama-3.3-70b-versatile",
    val openAiApiKey: String = "",
    val openAiModel: String = "gpt-4o-mini",
    val xaiApiKey: String = "",
    val xaiModel: String = "grok-3-mini",
    val customBaseUrl: String = "",
    val customApiKey: String = "",
    val customModel: String = "",
    val systemPersona: String = "",
    val temperature: Double = 0.7,
    val maxOutputTokens: Int = 2048,
    val timeoutSeconds: Int = 25,
    /** Automatically answer in Persian when the UI language is Persian. */
    val forceResponseLanguage: Boolean = true
) {
    /** True when the selected provider has everything it needs to actually run. */
    fun isProviderConfigured(): Boolean = when (provider) {
        AiProviderKind.LOCAL -> true
        AiProviderKind.GEMINI -> geminiApiKey.isNotBlank()
        AiProviderKind.OPENROUTER -> openRouterApiKey.isNotBlank()
        AiProviderKind.GROQ -> groqApiKey.isNotBlank()
        AiProviderKind.OPENAI -> openAiApiKey.isNotBlank()
        AiProviderKind.XAI -> xaiApiKey.isNotBlank()
        AiProviderKind.CUSTOM -> customBaseUrl.isNotBlank() && customModel.isNotBlank()
    }

    fun activeModel(): String = when (provider) {
        AiProviderKind.LOCAL -> "sayvis-local-core"
        AiProviderKind.GEMINI -> geminiModel
        AiProviderKind.OPENROUTER -> openRouterModel
        AiProviderKind.GROQ -> groqModel
        AiProviderKind.OPENAI -> openAiModel
        AiProviderKind.XAI -> xaiModel
        AiProviderKind.CUSTOM -> customModel
    }
}

/** Google account identity used for in-app sign-in and Google capabilities. */
data class GoogleAccountSettings(
    /** OAuth client ID from Google Cloud Console (public — not a secret). */
    val clientId: String = "",
    val email: String = "",
    val displayName: String = "",
    val pictureUrl: String = "",
    val signedInAtEpochMs: Long = 0L,
    val grantedScopes: String = "",
    /** When on (and OAuth configured), launch shows the Google sign-in gate. */
    val requireSignInAtLaunch: Boolean = false
) {
    val signedIn: Boolean get() = email.isNotBlank()
}

/** Localisation & rendering preferences. */
data class LocalizationSettings(
    val language: AppLanguage = AppLanguage.PERSIAN,
    /** Render 0-9 as ۰-۹ throughout the app. */
    val persianDigits: Boolean = true,
    /** Use the online AI translator for free-form text with no dictionary entry. */
    val autoTranslateFreeText: Boolean = true,
    /** Show a small marker next to machine-translated text. */
    val markMachineTranslated: Boolean = true,
    /** Always mirror the layout direction to the active language. */
    val forceRtlForPersian: Boolean = true
)

/** Complete owner-editable configuration. */
data class AppSettings(
    val localization: LocalizationSettings = LocalizationSettings(),
    val ai: AiSettings = AiSettings(),
    val trading: MtGatewayProfile = MtGatewayProfile(),
    val appearance: AppearanceMode = AppearanceMode.DARK_SPACE,
    val forceOfflineMode: Boolean = false,
    val emergencyLockActive: Boolean = false,
    val requireConfirmationForHighRisk: Boolean = true,
    val keepAuditLogOnDevice: Boolean = true,
    val runAutomationScripts: Boolean = true,
    val hapticFeedback: Boolean = true,
    val compactBottomNav: Boolean = true,
    /** Which of the four multidimensional AI views the avatar renders. */
    val visualStyle: AiVisualStyle = AiVisualStyle.GEOMETRIC,
    /** Play the robotic chirp when SAYVIS answers a voice-originated message. */
    val roboticVoiceReplies: Boolean = true,
    val onboardingCompleted: Boolean = false
    val google: GoogleAccountSettings = GoogleAccountSettings(),,
    val settingsSchemaVersion: Int = 3
) {
    /** Convenience: is the active language Persian? */
    fun isPersian(deviceLanguagePersian: Boolean = false): Boolean = when (localization.language) {
        AppLanguage.PERSIAN -> true
        AppLanguage.ENGLISH -> false
        AppLanguage.SYSTEM -> deviceLanguagePersian
    }
}
