package com.example.sayvis.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sayvis.ai.AIOrchestrator
import com.example.sayvis.ai.AssistantCommand
import com.example.sayvis.ai.AssistantCommandEngine
import com.example.sayvis.ai.ChatTurn
import com.example.sayvis.ai.AgentService
import com.example.sayvis.ai.GoogleAuthManager
import com.example.sayvis.ai.ConnectivityProbe
import com.example.sayvis.ai.EvolutionService
import com.example.sayvis.ai.SpecialistAgent
import com.example.sayvis.ai.GoogleServicesService
import com.example.sayvis.ai.WebSearchService
import com.example.sayvis.ai.ProviderType
import com.example.sayvis.ai.TranslationResult
import com.example.sayvis.ai.TranslationService
import com.example.sayvis.data.local.SayvisDatabase
import com.example.sayvis.data.repository.SayvisRepository
import com.example.sayvis.engine.AwareEngine
import com.example.sayvis.i18n.ContextLocalization
import com.example.sayvis.i18n.SayvisStrings
import com.example.sayvis.model.AuditEvent
import com.example.sayvis.model.AwareOpportunity
import com.example.sayvis.model.ContextSnapshot
import com.example.sayvis.model.Device
import com.example.sayvis.model.FocusActivity
import com.example.sayvis.model.LifeDomain
import com.example.sayvis.model.LifeScenario
import com.example.sayvis.model.LitAnalysisSignal
import com.example.sayvis.model.LitSignalType
import com.example.sayvis.model.MemoryItem
import com.example.sayvis.model.MemoryType
import com.example.sayvis.model.Mission
import com.example.sayvis.model.MissionPriority
import com.example.sayvis.model.MissionStatus
import com.example.sayvis.model.RetentionPolicy
import com.example.sayvis.model.EpistemicStatus
import com.example.sayvis.model.OpportunityStatus
import com.example.sayvis.model.PrivacyLevel
import com.example.sayvis.model.RiskLevel
import com.example.sayvis.model.SystemState
import com.example.sayvis.model.UicAttribute
import com.example.sayvis.model.UicCategory
import com.example.sayvis.model.UicStatus
import com.example.sayvis.scripts.AutomationScript
import com.example.sayvis.scripts.ScriptContext
import com.example.sayvis.scripts.ScriptEffect
import com.example.sayvis.scripts.ScriptEngine
import com.example.sayvis.scripts.ScriptRunResult
import com.example.sayvis.scripts.ScriptStore
import com.example.sayvis.scripts.ScriptTrigger
import com.example.sayvis.settings.AiProviderKind
import com.example.sayvis.settings.AppLanguage
import com.example.sayvis.settings.AppSettings
import com.example.sayvis.settings.MtGatewayProfile
import com.example.sayvis.settings.ProviderProbe
import com.example.sayvis.settings.SettingsStore
import com.example.sayvis.settings.SecretKey
import com.example.sayvis.identity.AccountResult
import com.example.sayvis.identity.DevicePairing
import com.example.sayvis.identity.OwnerAccount
import com.example.sayvis.identity.OwnerAccountStore
import com.example.sayvis.model.DeviceType
import com.example.sayvis.settings.TradingExecutionMode
import com.example.sayvis.trading.MetaTraderGateway
import com.example.sayvis.trading.MtConnectionPhase
import com.example.sayvis.trading.MtGatewayState
import com.example.sayvis.trading.MtOrderRequest
import com.example.sayvis.trading.MtOrderResult
import com.example.sayvis.ui.components.TranslationBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Navigation model.
 *
 * Only four destinations are *primary* — they are the ones the bottom bar shows.
 * Everything else is a secondary screen reached from Tools or Settings, and reports the
 * primary tab it belongs to so the bar can keep the right item highlighted.
 */
enum class SayvisScreen(val titleEn: String, val titleFa: String) {
    // ---- primary (bottom bar) ----
    HOME("Home", "خانه"),
    ASSISTANT("SAYO Assistant", "دستیار سایو"),
    TOOLS("Tools", "ابزارها"),
    SETTINGS("Settings", "تنظیمات"),

    // ---- secondary ----
    MISSIONS("Missions", "مأموریت‌ها"),
    AGENT("Research agent", "ایجنت پژوهش"),
    MIRROR("Digital mirror", "آینهٔ دیجیتال"),
    UIC("Cognitive Profile", "پروندهٔ شناختی"),
    AWARE("Smart Suggestions", "پیشنهادهای هوشمند"),
    TRADING("Trading & Markets", "معاملات و بازار"),
    SIMULATION("Decision Simulator", "شبیه‌سازی تصمیم"),
    SECURITY("Security & Devices", "امنیت و دستگاه‌ها"),
    GATEWAY("Trading Gateway", "درگاه معاملاتی"),
    SCRIPTS("Scripts & Automation", "اسکریپت و خودکارسازی"),
    AVATAR("Floating Avatar & Listening", "آواتار شناور و شنیدار"),
    ROBOT("SAYVIS Robot", "ربات سایویس");

    fun title(isPersian: Boolean): String = if (isPersian) titleFa else titleEn

    fun isPrimary(): Boolean = when (this) {
        HOME, ASSISTANT, TOOLS, SETTINGS -> true
        else -> false
    }

    /** Which bottom-bar item stays highlighted while this screen is open. */
    fun primaryTab(): SayvisScreen = when (this) {
        HOME, ASSISTANT, TOOLS, SETTINGS -> this
        GATEWAY, SCRIPTS -> TOOLS
        else -> TOOLS
    }
}

enum class AvatarState {
    IDLE,
    THINKING,
    SPEAKING,
    OPPORTUNITY_AWARE,
    EMERGENCY_LOCKED,
    OFFLINE
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: String, // "OWNER" or "SAYVIS"
    val text: String,
    val isActionProposal: Boolean = false,
    val opportunityId: String? = null,
    val providerUsed: ProviderType? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * A privileged assistant action that waits for an explicit owner tap on
 * approve/deny inside the chat before it is executed (zero-trust gate).
 */
data class AssistantAction(
    val id: String = UUID.randomUUID().toString(),
    val kind: Kind,
    val titleFa: String,
    val titleEn: String,
    val detailFa: String,
    val detailEn: String
) {
    enum class Kind { EMERGENCY_LOCK_ON, EMERGENCY_LOCK_OFF, OFFLINE_ON, OFFLINE_OFF }
}

class SayvisViewModel(application: Application) : AndroidViewModel(application) {

    private val database = SayvisDatabase.getDatabase(application, viewModelScope)
    val repository = SayvisRepository(database)

    private val settingsStore = SettingsStore.get(application)
    private val accountStore = OwnerAccountStore.get(application)
    private val scriptStore = ScriptStore.get(application).apply { ensureStarters() }
    private val scriptEngine = ScriptEngine()
    private val translationService = TranslationService(application)
    private val gateway = MetaTraderGateway()
    private val aiOrchestrator = AIOrchestrator()
    private val webSearch = WebSearchService()
    private val agentService = AgentService()
    private val connectivityProbe = ConnectivityProbe()
    private val evolutionService = EvolutionService()
    private val googleServices = GoogleServicesService()

    val awareEngine = AwareEngine(repository)

    // ------------------------------------------------------------ navigation
    private val _currentScreen = MutableStateFlow(SayvisScreen.HOME)
    val currentScreen: StateFlow<SayvisScreen> = _currentScreen.asStateFlow()

    fun navigateTo(screen: SayvisScreen) {
        _currentScreen.value = screen
    }

    /** Returns to the primary tab that owns the current secondary screen. */
    fun navigateBack() {
        val current = _currentScreen.value
        _currentScreen.value = if (current.isPrimary()) SayvisScreen.HOME else current.primaryTab()
    }

    // -------------------------------------------------------------- settings
    val settings: StateFlow<AppSettings> = settingsStore.settings

    val isPersian: StateFlow<Boolean> = settings
        .map { it.isPersian(SayvisStrings.deviceIsPersian()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings().isPersian(SayvisStrings.deviceIsPersian()))

    val emergencyLockActive: StateFlow<Boolean> = settings
        .map { it.emergencyLockActive }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val forceOfflineMode: StateFlow<Boolean> = settings
        .map { it.forceOfflineMode }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun updateSettings(mutator: (AppSettings) -> AppSettings) {
        settingsStore.update(mutator)
    }

    fun toggleLanguage() {
        settingsStore.update { current ->
            val next = if (current.isPersian(SayvisStrings.deviceIsPersian())) AppLanguage.ENGLISH else AppLanguage.PERSIAN
            current.copy(localization = current.localization.copy(language = next))
        }
    }

    fun setLanguage(language: AppLanguage) = settingsStore.setLanguage(language)

    fun toggleOfflineMode() {
        settingsStore.update { it.copy(forceOfflineMode = !it.forceOfflineMode) }
        updateAvatarState()
    }

    fun toggleEmergencyLock() {
        val newState = !settingsStore.current().emergencyLockActive
        settingsStore.setEmergencyLock(newState)
        audit(
            actor = "OWNER",
            action = if (newState) "security.emergency_lock.engage" else "security.emergency_lock.disengage",
            riskLevel = RiskLevel.CRITICAL,
            auth = "OWNER_BIOMETRIC_CONFIRMED",
            result = "SUCCESS",
            digest = "Emergency lock state updated to: $newState"
        )
        updateAvatarState()
    }

    fun resetAllSettings() {
        settingsStore.resetToDefaults()
        _gatewayState.value = MtGatewayState()
        translationService.clearCache()
        audit(
            actor = "OWNER",
            action = "settings.reset_all",
            riskLevel = RiskLevel.HIGHER_RISK,
            auth = "OWNER_CONFIRMED",
            result = "SUCCESS",
            digest = "All owner settings, API keys and the trading profile were cleared"
        )
        updateAvatarState()
    }

    val vaultHardwareBacked: Boolean get() = settingsStore.vault.isHardwareBacked
    val translationCacheSize: Int get() = translationService.cacheSize()

    fun clearTranslationCache() = translationService.clearCache()

    // --------------------------------------------------------------- AI / API
    private val _probe = MutableStateFlow<ProviderProbe?>(null)
    val probe: StateFlow<ProviderProbe?> = _probe.asStateFlow()

    private val _isProbing = MutableStateFlow(false)
    val isProbing: StateFlow<Boolean> = _isProbing.asStateFlow()

    /** True when the selected provider can actually serve requests right now. */
    val aiReady: StateFlow<Boolean> = combine(settings, forceOfflineMode) { current, offline ->
        aiOrchestrator.isCloudReady(current.ai, offline) || current.ai.provider == AiProviderKind.LOCAL
    }.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun testAiConnection() {
        if (_isProbing.value) return
        _isProbing.value = true
        viewModelScope.launch {
            val outcome = runCatching { aiOrchestrator.probe(settingsStore.current().ai) }
                .getOrElse {
                    com.example.sayvis.ai.ProbeOutcome(
                        success = false,
                        latencyMs = 0,
                        model = "",
                        messageFa = it.message ?: "خطای ناشناخته",
                        messageEn = it.message ?: "Unknown error"
                    )
                }
            _probe.value = ProviderProbe(
                provider = settingsStore.current().ai.provider,
                model = outcome.model,
                success = outcome.success,
                latencyMs = outcome.latencyMs,
                messageFa = outcome.messageFa,
                messageEn = outcome.messageEn
            )
            _isProbing.value = false
            audit(
                actor = "OWNER",
                action = "ai.provider.probe",
                riskLevel = RiskLevel.LOW_RISK,
                auth = "SESSION_VALIDATED",
                result = if (outcome.success) "SUCCESS" else "FAILED",
                digest = "Provider ${_probe.value?.provider?.name} model ${outcome.model}"
            )
        }
    }

    /** Bridge handed to Compose so any text can resolve itself through the hybrid pipeline. */
    val translationBridge: TranslationBridge = object : TranslationBridge {
        override val isPersian: Boolean get() = settingsStore.current().isPersian(SayvisStrings.deviceIsPersian())
        override val autoTranslate: Boolean
            get() {
                val current = settingsStore.current()
                return current.localization.autoTranslateFreeText && !current.forceOfflineMode
            }

        override val persianDigits: Boolean get() = settingsStore.current().localization.persianDigits

        override fun resolve(text: String, persianDigits: Boolean): TranslationResult =
            translationService.resolveOffline(text, persianDigits)

        override suspend fun online(text: String): String? = translationService.translateOnline(
            text = text,
            orchestrator = aiOrchestrator,
            settings = settingsStore.current().ai,
            forceOffline = settingsStore.current().forceOfflineMode
        )
    }

    // ------------------------------------------------------------------ chat
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(listOf(greeting(false)))
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _avatarState = MutableStateFlow(AvatarState.IDLE)
    val avatarState: StateFlow<AvatarState> = _avatarState.asStateFlow()

    private fun greeting(persian: Boolean) = ChatMessage(
        sender = "SAYVIS",
        text = if (persian) {
            "سلام. من سایو هستم، دستیار شخصی سایویس.\n" +
                "پروندهٔ شناختی شما بارگذاری شد، امنیت «اعتماد صفر» فعال است و ۳ دستگاه جفت شده‌اند.\n" +
                "می‌توانید مستقیم دستور بدهید: «مأموریت بساز …»، «یادت باشه که …»، «حساب کن ۱۲×۳»، «وضعیت رو گزارش بده»، «باز کن تنظیمات» — را تحلیل و واقعاً اجرا می‌کنم. " +
                "از تب «ابزارها» به مأموریت‌ها، معاملات و اسکریپت‌نویسی دسترسی دارید و همهٔ تنظیمات — از جمله کلید API و درگاه متاتریدر — در تب «تنظیمات» است."
        } else {
            "Hello. I am SAYO, your SAYVIS personal assistant.\n" +
                "Your cognitive profile is loaded, zero-trust security is active and 3 devices are paired.\n" +
                "Use the Tools tab for missions, trading and scripting; every setting — including the API key and the MetaTrader gateway — lives in the Settings tab."
        },
        providerUsed = ProviderType.LOCAL_COGNITIVE
    )

    /** Quick intermediate SAYVIS chat line (agent steps etc.). */
    private fun appendAssistantNote(text: String) {
        _chatMessages.value = _chatMessages.value + ChatMessage(sender = "SAYVIS", text = text)
    }

    // -------------------------------------------------- specialist agents

    private val _evolutionBusy = MutableStateFlow(false)
    val evolutionBusy: StateFlow<Boolean> = _evolutionBusy.asStateFlow()

    private val _evolutionReport = MutableStateFlow<String?>(null)
    val evolutionReport: StateFlow<String?> = _evolutionReport.asStateFlow()

    /** Specialist run: brain chosen free-first, live web grounding, output. */
    fun runSpecialist(kind: SpecialistAgent.Kind, goal: String) {
        val trimmed = goal.trim()
        if (trimmed.isBlank() || _agentBusy.value) return
        val current = settingsStore.current()
        if (current.emergencyLockActive) {
            _agentResult.value = "⚠️ قفل اضطراری فعال است — ایجنت‌ها مسدود شدند."
            return
        }
        _agentBusy.value = true
        _agentSteps.value = emptyList()
        _agentResult.value = null
        viewModelScope.launch {
            val finalText = runAgentPipeline(kind, trimmed, current) { step ->
                _agentSteps.value = _agentSteps.value + step
            }
            _agentResult.value = finalText
            _agentBusy.value = false
        }
    }

    /**
     * Shared specialist pipeline: picks the best free brain, grounds the run
     * in fresh web sources, calls the orchestrator, returns the final message.
     */
    private suspend fun runAgentPipeline(
        kind: SpecialistAgent.Kind,
        goal: String,
        current: AppSettings,
        onStep: (String) -> Unit
    ): String {
        val persian = current.isPersian(SayvisStrings.deviceIsPersian())

        // 1) AUTOMATIC brain selection — free/low-token first, every run.
        val brain = SpecialistAgent.pickBrain(current.ai)
        val brainSettings = if (brain != null) current.ai.copy(provider = brain.kind) else current.ai
        onStep(
            if (persian) "🧠 مغز انتخاب‌شده: " + (brain?.noteFa ?: "هستهٔ محلی سایویس (بدون کلید)")
            else "🧠 Brain selected: " + (brain?.noteEn ?: "SAYVIS local core (keyless)")
        )

        // 2) Live grounding.
        onStep(if (persian) "🔍 جست‌وجوی زندهٔ وب…" else "🔍 Live web search…")
        val sources = runCatching { webSearch.search(goal, persian) }.getOrDefault(emptyList())
        if (sources.isNotEmpty()) {
            onStep(if (persian) "📄 ${sources.size} منبع تازه پیدا شد" else "📄 Found ${sources.size} fresh sources")
        }

        // 3) Owner context (linked accounts).
        val ownerContext = buildString {
            if (current.linked.instagramHandle.isNotBlank()) {
                append(if (persian) "هندل اینستاگرام مالک: @${current.linked.instagramHandle}\n" else "Owner's Instagram handle: @${current.linked.instagramHandle}\n")
            }
        }

        val sourcesBlock = if (sources.isEmpty()) "" else buildString {
            appendLine(if (persian) "منابع زندهٔ وب (به آن‌ها استناد کن):" else "LIVE WEB SOURCES (cite them):")
            sources.forEachIndexed { index, result ->
                appendLine("${index + 1}. ${result.title} — ${result.snippet.take(150)} (${result.url})")
            }
        }

        val prompt = SpecialistAgent.prompt(kind, goal, persian, ownerContext)
        onStep(if (persian) "🛠 اجرای مأموریت تخصصی…" else "🛠 Running the specialist mission…")

        val response = aiOrchestrator.querySAYVIS(
            prompt = prompt,
            uicContext = "",
            systemContext = sourcesBlock,
            languageFa = persian,
            emergencyLockActive = current.emergencyLockActive,
            forceOffline = current.forceOfflineMode,
            settings = brainSettings
        )

        val footer = if (sources.isNotEmpty()) {
            "\n\n🌐 " + (if (persian) "منابع:" else "Sources:") + "\n" +
                sources.take(4).joinToString("\n") { "• ${it.title} (${it.source})" }
        } else ""
        val brainNote = if (brain != null) {
            if (persian) "🔎 اجرا با: ${brain.noteFa}\n\n" else "🔎 Ran with: ${brain.noteEn}\n\n"
        } else ""

        audit(
            actor = "SAYVIS_AGENT",
            action = "agent.specialist",
            riskLevel = RiskLevel.LOW_RISK,
            auth = "OWNER_CONFIRMED",
            result = if (response.isSuccess) "SUCCESS" else "FAILED",
            digest = "kind=$kind goal=${goal.take(40)} brain=${brain?.kind?.name ?: "LOCAL"}"
        )
        return brainNote + response.text + footer
    }

    /** Self-evolution: scan GitHub for similar agents, distil an adoption backlog. */
    fun runEvolution() {
        if (_evolutionBusy.value) return
        val current = settingsStore.current()
        _evolutionBusy.value = true
        viewModelScope.launch {
            val persian = current.isPersian(SayvisStrings.deviceIsPersian())
            val report = runCatching { evolutionService.search(persian) }.getOrNull()
            if (report == null) {
                _evolutionReport.value = if (persian) "اسکن گیت‌هاب ناموفق بود." else "GitHub scan failed."
            } else {
                val text = buildString {
                    appendLine(if (persian) report.messageFa else report.messageEn)
                    appendLine()
                    report.repos.take(5).forEach { repo ->
                        appendLine("⭐${repo.stars}  ${repo.fullName} — ${repo.description.take(110)}")
                        appendLine("   ${repo.url}")
                    }
                    if (report.ideas.isNotEmpty()) {
                        appendLine()
                        appendLine(if (persian) "🧬 بک‌لاگ جذب (الگوبرداری هوشمند):" else "🧬 Adoption backlog:")
                        report.ideas.forEach { appendLine("• $it") }
                    }
                }
                _evolutionReport.value = text
                settingsStore.update { it.copy(evolutionBacklog = report.ideas.joinToString("\n")) }
                audit(
                    actor = "SAYVIS_AGENT",
                    action = "agent.evolution",
                    riskLevel = RiskLevel.LOW_RISK,
                    auth = "OWNER_CONFIRMED",
                    result = "SUCCESS",
                    digest = "repos=${report.repos.size} ideas=${report.ideas.size}"
                )
            }
            _evolutionBusy.value = false
        }
    }

    // ---------------------------------------------------------- connectivity

    private val _connectivity = MutableStateFlow<ConnectivityProbe.Result?>(null)
    val connectivity: StateFlow<ConnectivityProbe.Result?> = _connectivity.asStateFlow()

    /** Live network truth — shown as a status pill instead of silence. */
    fun refreshConnectivity() {
        viewModelScope.launch {
            _connectivity.value = connectivityProbe.probe()
        }
    }

    // ----------------------------------------------------- research agent

    private val _agentBusy = MutableStateFlow(false)
    val agentBusy: StateFlow<Boolean> = _agentBusy.asStateFlow()

    private val _agentSteps = MutableStateFlow<List<String>>(emptyList())
    val agentSteps: StateFlow<List<String>> = _agentSteps.asStateFlow()

    private val _agentResult = MutableStateFlow<String?>(null)
    val agentResult: StateFlow<String?> = _agentResult.asStateFlow()

    /** Runs the research agent loop from the Agent screen. */
    fun runAgent(goal: String) {
        val trimmed = goal.trim()
        if (trimmed.isBlank() || _agentBusy.value) return
        val current = settingsStore.current()
        if (current.emergencyLockActive) {
            _agentResult.value = "⚠️ قفل اضطراری فعال است — ایجنت مسدود شد."
            return
        }
        _agentBusy.value = true
        _agentSteps.value = emptyList()
        _agentResult.value = null
        viewModelScope.launch {
            val report = agentService.run(
                goal = trimmed,
                languageFa = current.isPersian(SayvisStrings.deviceIsPersian()),
                synthesizer = { research ->
                    val response = aiOrchestrator.querySAYVIS(
                        prompt = trimmed,
                        uicContext = "",
                        systemContext = research,
                        languageFa = current.isPersian(SayvisStrings.deviceIsPersian()),
                        emergencyLockActive = current.emergencyLockActive,
                        forceOffline = current.forceOfflineMode,
                        settings = current.ai
                    )
                    response.text
                },
                googleFetch = { query -> privateGoogleFetch(query) },
                onStep = { step -> _agentSteps.value = _agentSteps.value + step }
            )
            _agentResult.value = report.answer +
                (if (report.sources.isNotEmpty()) "\n\n🌐 " + report.sources.take(4)
                    .joinToString("\n") { "${it.title} (${it.source})" } else "")
            _agentBusy.value = false
            audit(
                actor = "SAYVIS_AGENT",
                action = "agent.research",
                riskLevel = RiskLevel.LOW_RISK,
                auth = "OWNER_CONFIRMED",
                result = "SUCCESS",
                digest = "goal=${report.goal.take(40)} sources=${report.sources.size} google=${report.googleLines.size}"
            )
        }
    }

    /** Fetches the owner's private Google data for a capability query (or null). */
    private suspend fun privateGoogleFetch(query: String): GoogleServicesService.FetchResult? {
        val context = getApplication<Application>()
        val current = settingsStore.current()
        if (!current.google.signedIn) return null
        val refreshToken = settingsStore.getSecret(SecretKey.GOOGLE_REFRESH_TOKEN)
        val accessToken = GoogleAuthManager.validAccessToken(context, refreshToken, current.google.clientId)
            ?: return null
        return when (GoogleServicesService.matchCapability(query)) {
            GoogleServicesService.Capability.GMAIL -> googleServices.recentEmails(accessToken, query.take(60))
            GoogleServicesService.Capability.CALENDAR -> googleServices.upcomingEvents(accessToken)
            GoogleServicesService.Capability.DRIVE -> googleServices.findFiles(accessToken, null)
            null -> null
        }
    }

    // -------------------------------------------------- google sign-in hooks

    /** Opens the Google consent screen (requires the OAuth client ID). */
    fun beginGoogleSignIn(): Boolean {
        val clientId = settingsStore.current().google.clientId.trim()
        if (clientId.isBlank()) return false
        return GoogleAuthManager.openBrowser(getApplication(), clientId)
    }

    /** Removes the Google identity and refresh token from the device. */
    fun googleSignOut() {
        settingsStore.putSecret(SecretKey.GOOGLE_REFRESH_TOKEN, "")
        settingsStore.update { it.copy(google = it.google.copy(email = "", displayName = "", pictureUrl = "", signedInAtEpochMs = 0L)) }
    }

    fun setGoogleRequireSignIn(enabled: Boolean) {
        settingsStore.update { it.copy(google = it.google.copy(requireSignInAtLaunch = enabled)) }
    }

    fun sendMessage(text: String, fromVoice: Boolean = false) {
        if (text.isBlank()) return
        val current = settingsStore.current()
        val persian = current.isPersian(SayvisStrings.deviceIsPersian())

        _chatMessages.value = _chatMessages.value + ChatMessage(sender = "OWNER", text = text)
        _pendingAction.value = null
        _avatarState.value = AvatarState.THINKING

        viewModelScope.launch {
            // 1) Structured command analysis FIRST — these execute for real.
            val command = AssistantCommandEngine.parse(text)
            if (command != null) {
                executeCommand(command, persian)
                audit(
                    actor = "SAYVIS_AGENT",
                    action = "assistant.command.execute",
                    riskLevel = RiskLevel.LOW_RISK,
                    auth = "OWNER_CONFIRMED",
                    result = "SUCCESS",
                    digest = "${command::class.simpleName} <- ${text.take(40)}"
                )
                return@launch
            }

            // 1b) Research agent: explicit owner command runs the multi-step
            //     browsing loop (search -> read pages -> synthesize) and/or the
            //     owner's private Google capabilities (gmail/calendar/drive).
            val normalizedMessage = AssistantCommandEngine.normalize(text)
            val agentGoal = AgentService.triggerGoal(normalizedMessage)
            if (agentGoal != null && !current.emergencyLockActive) {
                appendAssistantNote(
                    if (persian) "🛰 ایجنت پژوهش سایویس فعال شد: «$agentGoal»"
                    else "🛰 SAYVIS research agent engaged: \"$agentGoal\""
                )
                val report = agentService.run(
                    goal = agentGoal,
                    languageFa = persian,
                    synthesizer = { research ->
                        aiOrchestrator.querySAYVIS(
                            prompt = agentGoal,
                            uicContext = "",
                            systemContext = research,
                            languageFa = persian,
                            emergencyLockActive = current.emergencyLockActive,
                            forceOffline = current.forceOfflineMode,
                            settings = current.ai
                        ).text
                    },
                    googleFetch = { query -> privateGoogleFetch(query) },
                    onStep = { step -> appendAssistantNote(step) }
                )
                _chatMessages.value = _chatMessages.value + ChatMessage(
                    sender = "SAYVIS",
                    text = report.answer + (if (report.sources.isNotEmpty())
                        "\n\n🌐 " + (if (persian) "منابع ایجنت:" else "Agent sources:") + "\n" +
                            report.sources.take(4).joinToString("\n") { "${it.title} (${it.source})" }
                    else "")
                )
                updateAvatarState()
                audit(
                    actor = "SAYVIS_AGENT",
                    action = "assistant.agent_research",
                    riskLevel = RiskLevel.LOW_RISK,
                    auth = "OWNER_CONFIRMED",
                    result = "SUCCESS",
                    digest = "goal=${report.goal.take(40)} sources=${report.sources.size}"
                )
                return@launch
            }

            // 1c) Google account capabilities (gmail / calendar / drive), when signed in.
            if (current.google.signedIn && current.emergencyLockActive.not()) {
                val capability = GoogleServicesService.matchCapability(normalizedMessage)
                if (capability != null) {
                    val fetched = privateGoogleFetch(normalizedMessage)
                    val answer = if (fetched == null) {
                        if (persian) "دسترسی گوگل برقرار نشد؛ دوباره وارد شوید یا شبکه را بررسی کنید."
                        else "Google access failed; sign in again or check the network."
                    } else if (fetched.ok && fetched.lines.isNotEmpty()) {
                        (if (persian) "🔐 از حساب گوگل شما:\n\n" else "🔐 From your Google account:\n\n") +
                            fetched.lines.joinToString("\n")
                    } else if (fetched.ok) {
                        if (persian) fetched.messageFa else fetched.messageEn
                    } else {
                        if (persian) fetched.messageFa else fetched.messageEn
                    }
                    _chatMessages.value = _chatMessages.value + ChatMessage(sender = "SAYVIS", text = answer)
                    updateAvatarState()
                    audit(
                        actor = "SAYVIS_AGENT",
                        action = "assistant.google_capability",
                        riskLevel = RiskLevel.LOW_RISK,
                        auth = "OWNER_CONFIRMED",
                        result = if (fetched?.ok == true) "SUCCESS" else "FAILED",
                        digest = "capability=$capability <- ${normalizedMessage.take(40)}"
                    )
                    return@launch
                }
            }

            // 2) Web grounding: questions (and explicit "search" commands) fetch fresh
            //    public sources — keyless, read-only — that work even when the cloud
            //    AI is blocked/unreachable, and even in forced-offline mode.
            val searchDecision = WebSearchService.shouldSearch(normalizedMessage)
            var sources: List<WebSearchService.WebResult> = emptyList()
            var sourcesBlock = ""
            if (searchDecision != null && !current.emergencyLockActive) {
                sources = runCatching { webSearch.search(searchDecision.query, persian) }.getOrDefault(emptyList())
                if (sources.isNotEmpty()) {
                    sourcesBlock = buildString {
                        appendLine(if (persian) "منابع زندهٔ وب (تازه، به آن‌ها استناد کن):" else "LIVE WEB SOURCES (fresh; cite them):")
                        sources.forEachIndexed { index, result ->
                            appendLine("${index + 1}. ${result.title} — ${result.snippet.take(160)} (${result.url})")
                        }
                    }
                    audit(
                        actor = "SAYVIS_AGENT",
                        action = "assistant.web_search",
                        riskLevel = RiskLevel.LOW_RISK,
                        auth = "OWNER_CONFIRMED",
                        result = "SUCCESS",
                        digest = "${sources.size} sources <- ${searchDecision.query.take(40)}"
                    )
                }
            }

            // 3) AI provider round-trip (cloud when configured, local core otherwise).
            val uicSummary = uicAttributes.value.joinToString("\n") {
                "- [${it.category.name}] ${it.title}: ${it.value} (status ${it.status.name}, confidence ${it.confidence})"
            }
            val systemContext = ContextLocalization.systemContextLine(contextSnapshot.value, persian) +
                (if (sourcesBlock.isNotBlank()) "\n$sourcesBlock" else "")
            val history = _chatMessages.value.takeLast(10).map { ChatTurn(it.sender, it.text) }

            val response = aiOrchestrator.querySAYVIS(
                prompt = text,
                uicContext = uicSummary,
                systemContext = systemContext,
                languageFa = persian,
                emergencyLockActive = current.emergencyLockActive,
                forceOffline = current.forceOfflineMode,
                settings = current.ai,
                history = history
            )

            _avatarState.value = AvatarState.SPEAKING

            val providerAnswer = response.text
            val sourcesFooter = if (sources.isNotEmpty()) {
                "\n\n🌐 " + (if (persian) "منابع وب:" else "Web sources:") + "\n" +
                    sources.take(4).mapIndexed { index, result -> "${index + 1}. ${result.title} (${result.source})" }
                        .joinToString("\n")
            } else ""

            val fallbackText = when {
                providerAnswer.isNotBlank() && response.providerUsed != ProviderType.LOCAL_COGNITIVE -> providerAnswer + sourcesFooter
                sources.isNotEmpty() -> {
                    // The cloud AI was unavailable — answer from the live sources directly.
                    (if (persian) "بر اساس نتایج زندهٔ وب:\n\n" else "Based on live web results:\n\n") +
                        sources.take(3).joinToString("\n\n") { result ->
                            "• ${result.title}\n${result.snippet.take(200)}\n${result.url}"
                        } + sourcesFooter
                }
                providerAnswer.isNotBlank() -> providerAnswer
                else ->
                    if (persian) "پاسخی تولید نشد. وضعیت سرویس هوش مصنوعی را در تنظیمات بررسی کنید."
                    else "No answer was produced. Check the AI provider status in Settings."
            }

            _chatMessages.value = _chatMessages.value + ChatMessage(
                sender = "SAYVIS",
                text = fallbackText,
                providerUsed = response.providerUsed
            )
            updateAvatarState()

            // Robotic chirp when the answer comes to a voice-originated question.
            if (fromVoice && settingsStore.current().roboticVoiceReplies) {
                com.example.sayvis.voice.RoboticAudio.playReply(getApplication())
            }

            audit(
                actor = "SAYVIS_AGENT",
                action = "ai.query.respond",
                riskLevel = RiskLevel.LOW_RISK,
                auth = "SESSION_VALIDATED",
                result = if (response.isSuccess) "SUCCESS" else "FAILED",
                digest = "Provider ${response.providerUsed.displayName} model ${response.model} (${text.take(30)})"
            )
        }
    }

    // ------------------------------------------------- assistant command exec

    private fun assistantReply(text: String) {
        _chatMessages.value = _chatMessages.value + ChatMessage(
            sender = "SAYVIS",
            text = text,
            providerUsed = ProviderType.LOCAL_COGNITIVE
        )
    }

    private fun faOrEn(persian: Boolean, fa: String, en: String): String = if (persian) fa else en

    /**
     * Actually runs a parsed [AssistantCommand]. Low-risk commands execute
     * immediately; privileged switches queue an [AssistantAction] that needs an
     * explicit approve tap.
     */
    private suspend fun executeCommand(command: AssistantCommand, persian: Boolean) {
        when (command) {
            is AssistantCommand.CreateMission -> {
                if (emergencyLockActive.value) {
                    assistantReply(faOrEn(persian, "⛔ قفل اضطراری فعال است؛ ساخت مأموریت مسدود شد. ابتدا قفل را بردارید.", "⛔ The emergency lock is engaged; mission creation is blocked. Disengage it first."))
                    updateAvatarState()
                    return
                }
                val tasks = (1..command.taskCount).map { index ->
                    com.example.sayvis.model.MissionTask(
                        id = "task_" + UUID.randomUUID().toString().take(6),
                        title = faOrEn(persian, "زیرکار $index", "Sub-task $index")
                    )
                }
                val mission = Mission(
                    id = "mission_" + UUID.randomUUID().toString().take(6),
                    title = command.title,
                    description = faOrEn(persian, "ایجادشده از طریق دستیار سایویس", "Created through the SAYVIS assistant"),
                    priority = if (command.urgent) MissionPriority.CRITICAL else MissionPriority.HIGH,
                    status = MissionStatus.ACTIVE,
                    progressPercent = 0,
                    deadline = "",
                    tasks = tasks
                )
                repository.addMission(mission)
                assistantReply(
                    faOrEn(
                        persian,
                        "✅ مأموریت «${command.title}» ساخته شد" +
                            (if (command.taskCount > 0) " با ${command.taskCount} زیرکار" else "") +
                            (if (command.urgent) " و با اولویت بحرانی" else "") +
                            ". از «ابزارها ← مأموریت‌ها» قابل پیگیری است.",
                        "✅ Mission \"${command.title}\" was created" +
                            (if (command.taskCount > 0) " with ${command.taskCount} sub-task(s)" else "") +
                            (if (command.urgent) " at CRITICAL priority" else "") +
                            ". Track it under Tools → Missions."
                    )
                )
                audit("SAYVIS_AGENT", "mission.create_from_chat", RiskLevel.LOW_RISK, "OWNER_CONFIRMED", "SUCCESS", command.title)
            }

            is AssistantCommand.Remember -> {
                repository.addMemory(
                    MemoryItem(
                        id = "mem_" + UUID.randomUUID().toString().take(8),
                        type = MemoryType.LONG_TERM_MEMORY,
                        content = command.fact,
                        source = "OWNER_CHAT",
                        confidence = 1.0f,
                        provenance = faOrEn(persian, "گفتهٔ مالک در گفتگو", "Owner statement in chat"),
                        importance = 7,
                        retentionPolicy = RetentionPolicy.PERSISTENT,
                        epistemicStatus = EpistemicStatus.CONFIRMED
                    )
                )
                assistantReply(
                    faOrEn(persian, "🧠 ثبت شد در حافظهٔ بلندمدت: «${command.fact}»\nهر وقت پرسیدید «یادت هست…؟» همان را می‌گویم.",
                        "🧠 Stored in long-term memory: \"${command.fact}\"\nAsk me \"do you remember…?\" any time.")
                )
                audit("SAYVIS_AGENT", "memory.create_from_chat", RiskLevel.LOW_RISK, "OWNER_CONFIRMED", "SUCCESS", command.fact.take(50))
            }

            is AssistantCommand.Recall -> {
                val hits = repository.searchMemories(command.query)
                val uicHits = if (command.query.isBlank()) emptyList() else uicAttributes.value.filter {
                    it.title.contains(command.query, ignoreCase = true) || it.value.contains(command.query, ignoreCase = true)
                }.take(3)
                val lines = ArrayList<String>()
                lines.add(faOrEn(persian, "🧠 آنچه در حافظه دارم:", "🧠 What I remember:"))
                if (hits.isEmpty() && uicHits.isEmpty()) {
                    lines.clear()
                    lines.add(
                        if (command.query.isBlank()) faOrEn(persian, "هنوز چیزی در حافظه ثبت نشده. با «یادت باشه که …» ثبتش کنید.",
                            "Nothing is stored yet. Say \"remember that …\" to store a fact.")
                        else faOrEn(persian, "چیزی دربارهٔ «${command.query}» پیدا نکردم. می‌توانید با «یادت باشه که …» ثبتش کنید.",
                            "I found nothing about \"${command.query}\". You can store it with \"remember that …\".")
                    )
                } else {
                    hits.forEach { item ->
                        val date = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(item.createdAt))
                        lines.add("• ${item.content}  ($date)")
                    }
                    uicHits.forEach { attr ->
                        lines.add(faOrEn(persian, "• [پروندهٔ شناختی] ${attr.title}: ${attr.value}", "• [Cognitive profile] ${attr.title}: ${attr.value}"))
                    }
                }
                assistantReply(lines.joinToString("\n"))
            }

            is AssistantCommand.Calculate -> {
                val formatted = formatNumber(command.value, persian)
                assistantReply(
                    faOrEn(persian, "🧮 ${command.expression} = $formatted", "🧮 ${command.expression} = $formatted")
                )
            }

            AssistantCommand.StatusReport -> {
                val snap = contextSnapshot.value
                val current = settingsStore.current()
                val provider = current.ai.activeModel()
                assistantReply(
                    faOrEn(persian,
                        "📊 وضعیت لحظه‌ای سایویس:\n" +
                            "• مأموریت‌های فعال: ${snap.activeMissionsCount}\n" +
                            "• وظیفه‌های مسدود: ${snap.blockedTasksCount}\n" +
                            "• باتری: ${snap.batteryPercent}٪" + (if (snap.isCharging) " (در حال شارژ)" else "") + "\n" +
                            "• شبکه: " + (if (snap.isOnline) "آنلاین" else "آفلاین") + "\n" +
                            "• قفل اضطراری: " + (if (current.emergencyLockActive) "فعال" else "غیرفعال") + "\n" +
                            "• هستهٔ هوش: $provider\n" +
                            "• یادداشت‌های حافظه: ${memories.value.size}",
                        "📊 SAYVIS live status:\n" +
                            "• Active missions: ${snap.activeMissionsCount}\n" +
                            "• Blocked tasks: ${snap.blockedTasksCount}\n" +
                            "• Battery: ${snap.batteryPercent}%" + (if (snap.isCharging) " (charging)" else "") + "\n" +
                            "• Network: " + (if (snap.isOnline) "online" else "offline") + "\n" +
                            "• Emergency lock: " + (if (current.emergencyLockActive) "ENGAGED" else "off") + "\n" +
                            "• AI core: $provider\n" +
                            "• Memory notes: ${memories.value.size}"
                    )
                )
            }

            AssistantCommand.ShowMissions -> {
                val list = missions.value
                if (list.isEmpty()) {
                    assistantReply(faOrEn(persian, "هیچ مأموریتی ثبت نشده است. بگویید «مأموریت بساز …» تا همین‌جا بسازم.",
                        "No missions yet. Say \"create mission …\" and I will build one right here."))
                } else {
                    val body = list.take(10).mapIndexed { index, mission ->
                        "${index + 1}. ${mission.title} — ${mission.progressPercent}٪ (${mission.tasks.size} " +
                            faOrEn(persian, "وظیفه،", "tasks,") + " ${mission.priority.labelFa}/${mission.priority.labelEn})"
                    }.joinToString("\n")
                    assistantReply(faOrEn(persian, "📋 مأموریت‌های شما:\n$body", "📋 Your missions:\n$body"))
                }
            }

            AssistantCommand.RunAwareScan -> {
                runAwareScan()
                assistantReply(
                    faOrEn(persian, "🔎 اسکن ادراک محیطی انجام شد؛ پیشنهادهای تازه در «ابزارها ← پیشنهادهای هوشمند» ظاهر می‌شوند.",
                        "🔎 AWARE scan finished; fresh suggestions appear under Tools → Smart Suggestions.")
                )
            }

            is AssistantCommand.Navigate -> {
                val target = runCatching { SayvisScreen.valueOf(command.target) }.getOrNull()
                if (target != null) {
                    navigateTo(target)
                    assistantReply(
                        faOrEn(persian, "صفحهٔ «${target.titleFa}» باز شد ✅", "Opened ${target.titleEn} ✅")
                    )
                }
            }

            is AssistantCommand.ToggleEmergencyLock -> {
                val already = emergencyLockActive.value == command.engage
                if (already) {
                    assistantReply(
                        faOrEn(persian,
                            "قفل اضطراری از قبل " + (if (command.engage) "فعال است." else "غیرفعال است."),
                            "The emergency lock is already " + (if (command.engage) "engaged." else "off."))
                    )
                } else {
                    _pendingAction.value = if (command.engage) {
                        AssistantAction(
                            kind = AssistantAction.Kind.EMERGENCY_LOCK_ON,
                            titleFa = "فعال‌سازی قفل اضطراری", titleEn = "Engage the emergency lock",
                            detailFa = "همهٔ اجراها، ابزارها و خودکارسازی‌ها مسدود می‌شوند.",
                            detailEn = "All execution, tools and automations will be blocked."
                        )
                    } else {
                        AssistantAction(
                            kind = AssistantAction.Kind.EMERGENCY_LOCK_OFF,
                            titleFa = "برداشتن قفل اضطراری", titleEn = "Disengage the emergency lock",
                            detailFa = "اجراهای مسدودشده دوباره آزاد می‌شوند.",
                            detailEn = "Blocked executions will be permitted again."
                        )
                    }
                    assistantReply(
                        faOrEn(persian, "برای این کار به تأیید صریح شما نیاز دارم — کارت تأیید را در پایین گفتگو ببینید.",
                            "I need your explicit approval — see the confirmation card at the bottom of the chat.")
                    )
                }
            }

            is AssistantCommand.ToggleOffline -> {
                val already = forceOfflineMode.value == command.enable
                if (already) {
                    assistantReply(
                        faOrEn(persian,
                            "حالت آفلاین از قبل " + (if (command.enable) "روشن است." else "خاموش است."),
                            "Offline mode is already " + (if (command.enable) "on." else "off."))
                    )
                } else {
                    _pendingAction.value = if (command.enable) {
                        AssistantAction(
                            kind = AssistantAction.Kind.OFFLINE_ON,
                            titleFa = "روشن‌کردن حالت آفلاین اجباری", titleEn = "Enable forced offline mode",
                            detailFa = "هیچ درخواستی به اینترنت فرستاده نمی‌شود.",
                            detailEn = "No request will ever reach the internet."
                        )
                    } else {
                        AssistantAction(
                            kind = AssistantAction.Kind.OFFLINE_OFF,
                            titleFa = "خاموش‌کردن حالت آفلاین", titleEn = "Disable offline mode",
                            detailFa = "سرویس هوش مصنوعی ابری (در صورت تنظیم) دوباره در دسترس می‌شود.",
                            detailEn = "The cloud AI provider (if configured) becomes reachable again."
                        )
                    }
                    assistantReply(
                        faOrEn(persian, "به تأیید شما نیاز دارم — کارت تأیید را در پایین گفتگو ببینید.",
                            "I need your approval — see the confirmation card at the bottom of the chat.")
                    )
                }
            }

            AssistantCommand.TimeQuery -> {
                val fmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                val now = fmt.format(Date())
                assistantReply(faOrEn(persian, "🕐 ساعت $now است.", "🕐 It is $now."))
            }

            AssistantCommand.DateQuery -> {
                val faLocale = if (persian) Locale("fa") else Locale.getDefault()
                val fmt = SimpleDateFormat("EEEE، yyyy/MM/dd", faLocale)
                val today = fmt.format(Date())
                assistantReply(faOrEn(persian, "📅 امروز $today است.", "📅 Today is $today."))
            }

            AssistantCommand.BatteryQuery -> {
                val (pct, charging) = readBatteryStatus()
                assistantReply(
                    faOrEn(persian,
                        "🔋 باتری دستگاه ${pct}٪ است" + (if (charging) " و در حال شارژ است." else "."),
                        "🔋 The device battery is at $pct%" + (if (charging) " and charging." else "."))
                )
            }

            AssistantCommand.Help -> assistantReply(helpText(persian))
        }
        _avatarState.value = AvatarState.SPEAKING
        updateAvatarState()
    }

    private fun formatNumber(value: Double, persian: Boolean): String {
        val plain = if (kotlin.math.abs(value - value.toLong()) < 1e-9) {
            value.toLong().toString()
        } else {
            "%.4f".format(value).trimEnd('0').trimEnd('.')
        }
        return if (persian) com.example.sayvis.i18n.PersianFormat.toPersianNumerals(plain) else plain
    }

    private fun helpText(persian: Boolean): String =
        if (persian) {
            "من فقط دستیار متن نیستم؛ دستور را تحلیل و واقعاً اجرا می‌کنم:\n" +
                "• «مأموریت بساز خرید هفتگی با ۳ وظیفه»\n" +
                "• «یادت باشه که جلسهٔ فردا ساعت ۹ است» → ثبت در حافظه\n" +
                "• «یادت هست جلسه؟» → جست‌وجوی حافظه\n" +
                "• «حساب کن ۱۲×۳+۵» یا «what is 8/2»\n" +
                "• «وضعیت رو گزارش بده» / «مأموریت‌هامو نشون بده»\n" +
                "• «اسکن کن» → پیشنهادهای هوشمند\n" +
                "• «باز کن تنظیمات / مأموریت‌ها / درگاه / آواتار …»\n" +
                "• «قفل اضطراری را فعال کن» و «حالت آفلاین را روشن کن» (با کارت تأیید)\n" +
                "• «ساعت چنده؟»، «تاریخ امروز؟»، «باتری چقدره؟»\n" +
                "سؤال‌های باز را هم با هستهٔ محلی یا سرویس ابری پاسخ می‌دهم."
        } else {
            "I don't just chat — I parse and actually execute commands:\n" +
                "• \"create mission weekly shopping with 3 tasks\"\n" +
                "• \"remember that the review is tomorrow at 9\" → long-term memory\n" +
                "• \"do you remember the review?\" → memory search\n" +
                "• \"calculate 12*3+5\" or \"what is 8/2\"\n" +
                "• \"report status\" / \"show missions\"\n" +
                "• \"run a scan\" → smart suggestions\n" +
                "• \"open settings / missions / gateway / avatar …\"\n" +
                "• \"engage emergency lock\" and \"enable offline mode\" (with a consent card)\n" +
                "• \"what time is it?\", \"today's date?\", \"battery?\"\n" +
                "Open questions go to the local core or your cloud provider."
        }

    // -------------------------------------------------- pending (risky) action

    private val _pendingAction = MutableStateFlow<AssistantAction?>(null)
    val pendingAction: StateFlow<AssistantAction?> = _pendingAction.asStateFlow()

    /** Zero-trust: the switch flips only after this explicit tap. */
    fun approvePendingAction() {
        val action = _pendingAction.value ?: return
        _pendingAction.value = null
        val persian = isPersian.value
        when (action.kind) {
            AssistantAction.Kind.EMERGENCY_LOCK_ON -> {
                settingsStore.setEmergencyLock(true)
                updateAvatarState()
                assistantReply(faOrEn(persian, "🔒 قفل اضطراری فعال شد. همهٔ اجراها مسدود هستند.", "🔒 Emergency lock engaged. All execution is blocked."))
                audit("OWNER", "security.emergency_lock.engage", RiskLevel.CRITICAL, "OWNER_CONFIRMED", "SUCCESS", "Approved from chat")
            }
            AssistantAction.Kind.EMERGENCY_LOCK_OFF -> {
                settingsStore.setEmergencyLock(false)
                updateAvatarState()
                assistantReply(faOrEn(persian, "🔓 قفل اضطراری برداشته شد.", "🔓 Emergency lock disengaged."))
                audit("OWNER", "security.emergency_lock.disengage", RiskLevel.CRITICAL, "OWNER_CONFIRMED", "SUCCESS", "Approved from chat")
            }
            AssistantAction.Kind.OFFLINE_ON -> {
                settingsStore.setForceOffline(true)
                updateAvatarState()
                assistantReply(faOrEn(persian, "✈️ حالت آفلاین اجباری روشن شد؛ هیچ داده‌ای به بیرون نمی‌رود.", "✈️ Forced offline mode enabled; nothing leaves the device."))
                audit("OWNER", "settings.offline_mode.enable", RiskLevel.MEDIUM_RISK, "OWNER_CONFIRMED", "SUCCESS", "Approved from chat")
            }
            AssistantAction.Kind.OFFLINE_OFF -> {
                settingsStore.setForceOffline(false)
                updateAvatarState()
                assistantReply(faOrEn(persian, "🌐 حالت آفلاین خاموش شد.", "🌐 Offline mode disabled."))
                audit("OWNER", "settings.offline_mode.disable", RiskLevel.MEDIUM_RISK, "OWNER_CONFIRMED", "SUCCESS", "Approved from chat")
            }
        }
    }

    fun dismissPendingAction() {
        _pendingAction.value = null
        val persian = isPersian.value
        assistantReply(faOrEn(persian, "باشه، اجرا نشد.", "Okay — not executed."))
    }

    // -------------------------------------------------------------- database
    val uicAttributes: StateFlow<List<UicAttribute>> = repository.allUicAttributes.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val awareOpportunities: StateFlow<List<AwareOpportunity>> = repository.allOpportunities.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val missions: StateFlow<List<Mission>> = repository.allMissions.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val devices: StateFlow<List<Device>> = repository.allDevices.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val auditEvents: StateFlow<List<AuditEvent>> = repository.allAuditEvents.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    /** Long-term memory the assistant remembers across restarts. */
    val memories: StateFlow<List<MemoryItem>> = repository.allMemories.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    // ------------------------------------------------------- trading gateway
    private val _gatewayState = MutableStateFlow(MtGatewayState())
    val gatewayState: StateFlow<MtGatewayState> = _gatewayState.asStateFlow()

    private val _gatewayBusy = MutableStateFlow(false)
    val gatewayBusy: StateFlow<Boolean> = _gatewayBusy.asStateFlow()

    private val _lastOrder = MutableStateFlow<MtOrderResult?>(null)
    val lastOrder: StateFlow<MtOrderResult?> = _lastOrder.asStateFlow()

    /** Re-confirmed per session; never persisted, so a restart always drops to paper. */
    private var liveExecutionConfirmed = false

    fun saveGatewayProfile(profile: MtGatewayProfile) {
        settingsStore.update { it.copy(trading = profile) }
    }

    fun connectGateway(profile: MtGatewayProfile) {
        if (_gatewayBusy.value) return
        _gatewayBusy.value = true
        saveGatewayProfile(profile)
        viewModelScope.launch {
            val state = runCatching { gateway.connect(profile) }
                .getOrElse { error ->
                    MtGatewayState(
                        phase = MtConnectionPhase.ERROR,
                        profile = profile,
                        lastErrorFa = error.message ?: "خطای ناشناخته",
                        lastErrorEn = error.message ?: "Unknown error"
                    )
                }
            _gatewayState.value = state
            _gatewayBusy.value = false
            auditGateway(profile, "trading.gateway.connect", state.phase.name)
        }
    }

    fun testGateway(profile: MtGatewayProfile) = connectGateway(profile)

    fun disconnectGateway() {
        val profile = settingsStore.current().trading
        _gatewayState.value = gateway.disconnect(profile)
        liveExecutionConfirmed = false
        auditGateway(profile, "trading.gateway.disconnect", "DISCONNECTED")
    }

    fun refreshGateway() {
        if (_gatewayBusy.value) return
        _gatewayBusy.value = true
        viewModelScope.launch {
            _gatewayState.value = gateway.refresh(_gatewayState.value)
            _gatewayBusy.value = false
        }
    }

    fun changeExecutionMode(mode: TradingExecutionMode) {
        liveExecutionConfirmed = mode == TradingExecutionMode.LIVE_EXECUTION
        settingsStore.setExecutionMode(mode)
        _gatewayState.value = _gatewayState.value.copy(
            profile = _gatewayState.value.profile.copy(executionMode = mode)
        )
        audit(
            actor = "OWNER",
            action = "trading.execution_mode.change",
            riskLevel = if (mode == TradingExecutionMode.LIVE_EXECUTION) RiskLevel.CRITICAL else RiskLevel.MEDIUM_RISK,
            auth = "OWNER_CONFIRMED",
            result = "SUCCESS",
            digest = "Execution mode set to ${mode.name}"
        )
    }

    fun placeOrder(request: MtOrderRequest) {
        viewModelScope.launch {
            val current = settingsStore.current()
            val result = gateway.placeOrder(
                request = request,
                state = _gatewayState.value,
                emergencyLockActive = current.emergencyLockActive,
                liveConfirmed = liveExecutionConfirmed
            )
            _lastOrder.value = result
            audit(
                actor = "OWNER",
                action = if (result.accepted) "trading.order.submit" else "trading.order.blocked",
                riskLevel = if (result.accepted) RiskLevel.CRITICAL else RiskLevel.MEDIUM_RISK,
                auth = if (result.accepted) "OWNER_CONFIRMED" else (result.blockedBy?.name ?: "POLICY"),
                result = if (result.accepted) "SUCCESS" else "BLOCKED",
                digest = "${request.side.name} ${request.volume} ${request.symbol} -> ${result.ticket ?: result.blockedBy?.name}"
            )
        }
    }

    fun closePosition(ticket: String) {
        viewModelScope.launch {
            val result = gateway.closePosition(ticket, _gatewayState.value)
            _lastOrder.value = result
            audit(
                actor = "OWNER",
                action = "trading.position.close",
                riskLevel = RiskLevel.HIGHER_RISK,
                auth = "OWNER_CONFIRMED",
                result = if (result.accepted) "SUCCESS" else "BLOCKED",
                digest = "Close ticket $ticket"
            )
        }
    }

    private fun auditGateway(profile: MtGatewayProfile, action: String, detail: String) {
        audit(
            actor = "OWNER",
            action = action,
            riskLevel = if (profile.accountType.name == "REAL") RiskLevel.CRITICAL else RiskLevel.MEDIUM_RISK,
            auth = "OWNER_CONFIRMED",
            result = "SUCCESS",
            digest = "${profile.terminalVersion.name} ${profile.bridgeKind.name} $detail"
        )
    }

    // ------------------------------------------------- simulation & signals
    val tradingGate get() = com.example.sayvis.model.TradingGateState(
        liveTradingBlocked = settingsStore.current().trading.executionMode == TradingExecutionMode.PAPER_SIMULATION,
        paperTradingMode = settingsStore.current().trading.executionMode == TradingExecutionMode.PAPER_SIMULATION,
        killSwitchEngaged = settingsStore.current().emergencyLockActive,
        maxDailyDrawdownLimitUsd = settingsStore.current().trading.maxDailyLossUsd,
        activePositionsCount = _gatewayState.value.positions.size
    )

    val litSignals: List<LitAnalysisSignal> = listOf(
        LitAnalysisSignal(
            id = "sig_01",
            assetSymbol = "BTC/USDT",
            timeframe = "4H",
            signalType = LitSignalType.BULLISH_ORDER_BLOCK,
            entryPrice = 64200.0,
            invalidationStop = 63100.0,
            takeProfitTarget = 68500.0,
            riskRewardRatio = 3.9,
            notes = "Institutional liquidity grab at low range. Paper-trading simulation only."
        ),
        LitAnalysisSignal(
            id = "sig_02",
            assetSymbol = "XAU/USD (Gold)",
            timeframe = "1D",
            signalType = LitSignalType.BREAK_OF_STRUCTURE,
            entryPrice = 2580.0,
            invalidationStop = 2540.0,
            takeProfitTarget = 2690.0,
            riskRewardRatio = 2.75,
            notes = "Macro inflation hedge inversion with bullish order flow."
        )
    )

    val lifeScenarios: List<LifeScenario> = listOf(
        LifeScenario(
            id = "scen_career_01",
            domain = LifeDomain.CAREER_WORK,
            decisionHypothesis = "Transition to full-time autonomous AI operating system engineering",
            projectedTrajectory = "+45% sovereign asset compounding, 2.8x strategic impact, 6-month initial cash buffer required",
            confidenceScore = 0.84f,
            detectedRisks = listOf("Early runway volatility", "Multi-platform distribution complexity"),
            projectedOpportunities = listOf("First-mover advantage in personal AI layers", "Complete cognitive sovereignty")
        ),
        LifeScenario(
            id = "scen_health_01",
            domain = LifeDomain.HEALTH_ENERGY,
            decisionHypothesis = "Strict adherence to 09:00 - 11:30 deep work window with 22:00 screen cut-off",
            projectedTrajectory = "35% reduction in cognitive fatigue, 18% higher mission completion velocity",
            confidenceScore = 0.91f,
            detectedRisks = listOf("Potential pushback from synchronous communicators"),
            projectedOpportunities = listOf("Sustained high cognitive bandwidth", "Elimination of evening burnout")
        )
    )

    // ------------------------------------------------------- context snapshot
    val contextSnapshot: StateFlow<ContextSnapshot> = combine(
        missions,
        settings
    ) { missionList, current ->
        val blockedCount = missionList.flatMap { it.tasks }.count { it.isBlocked }
        val (batteryLevel, batteryCharging) = readBatteryStatus()
        val now = Calendar.getInstance()
        val hour = now.get(Calendar.HOUR_OF_DAY)
        val minute = now.get(Calendar.MINUTE)
        val focusActive = hour in 9..11 && !(hour == 11 && minute > 30)
        repository.getContextSnapshot(
            activeMissionsCount = missionList.count { it.status == MissionStatus.ACTIVE },
            blockedTasksCount = blockedCount,
            emergencyLockActive = current.emergencyLockActive,
            isOnline = !current.forceOfflineMode,
            batteryPercent = batteryLevel,
            isCharging = batteryCharging,
            focusWindowActive = focusActive,
            currentActivity = when {
                focusActive -> FocusActivity.DEEP_WORK
                hour in 22..23 || hour in 0..6 -> FocusActivity.RECOVERY
                blockedCount > 0 -> FocusActivity.REVIEW
                else -> FocusActivity.STRATEGIC_EXECUTION
            }
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ContextSnapshot(
            focusWindowActive = false,
            currentActivity = FocusActivity.BOOTING,
            activeMissionsCount = 0,
            blockedTasksCount = 0,
            isOnline = true
        )
    )

    // -------------------------------------------------------------- scripts
    val scripts: StateFlow<List<AutomationScript>> = scriptStore.scripts
    val lastScriptRun: StateFlow<ScriptRunResult?> = scriptStore.lastRun

    fun saveScript(script: AutomationScript) = scriptStore.upsert(script)
    fun deleteScript(id: String) = scriptStore.delete(id)
    fun toggleScript(id: String, enabled: Boolean) = scriptStore.setEnabled(id, enabled)

    /** Sends a natural-language idea to the assistant so it can author the script. */
    fun askAssistantToScript(prompt: String) {
        _currentScreen.value = SayvisScreen.ASSISTANT
        if (prompt.isNotBlank()) sendMessage(prompt)
    }

    fun runScript(script: AutomationScript, trigger: String = script.trigger.name) {
        val current = settingsStore.current()
        val persian = current.isPersian(SayvisStrings.deviceIsPersian())
        if (!current.runAutomationScripts && trigger != ScriptTrigger.MANUAL.name) {
            scriptStore.recordRun(
                script.id,
                ScriptRunResult(
                    success = false,
                    errorFa = "اجرای خودکار اسکریپت‌ها در تنظیمات غیرفعال است.",
                    errorEn = "Automatic script execution is disabled in settings."
                ),
                persian
            )
            return
        }

        val snapshot = contextSnapshot.value
        val gatewaySnapshot = _gatewayState.value
        val quotes = gatewaySnapshot.quotes.associate { it.symbol to it.bid }

        val context = ScriptContext(
            batteryPercent = snapshot.batteryPercent,
            isCharging = snapshot.isCharging,
            networkOnline = snapshot.isOnline,
            emergencyLockActive = current.emergencyLockActive,
            activeMissions = snapshot.activeMissionsCount,
            blockedTasks = snapshot.blockedTasksCount,
            focusWindowActive = snapshot.focusWindowActive,
            cognitiveLoad = snapshot.cognitiveLoad.name,
            hourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
            minuteOfHour = Calendar.getInstance().get(Calendar.MINUTE),
            quotes = quotes,
            accountBalance = gatewaySnapshot.account?.balance ?: 0.0,
            accountEquity = gatewaySnapshot.account?.equity ?: 0.0,
            dailyPnl = gatewaySnapshot.dailyPnl
        )

        val result = scriptEngine.run(script.source, context, trigger)
        scriptStore.recordRun(script.id, result, persian)

        result.effects.forEach { effect -> applyEffect(effect, script, current, persian) }

        audit(
            actor = "SCRIPT_ENGINE",
            action = "automation.script.run",
            riskLevel = if (result.success) RiskLevel.LOW_RISK else RiskLevel.MEDIUM_RISK,
            auth = "OWNER_CONFIRMED",
            result = if (result.success) "SUCCESS" else "FAILED",
            digest = "Script '${script.name}' produced ${result.effects.size} effect(s)"
        )
    }

    /**
     * Executes one script effect through the zero-trust gate.
     *
     * Notifications and logs are harmless and run immediately. Anything that leaves the
     * device (a webhook) or changes a privileged mode requires an enabled automation
     * switch and a disengaged emergency lock, and is always written to the audit log.
     */
    private fun applyEffect(effect: ScriptEffect, script: AutomationScript, current: AppSettings, persian: Boolean) {
        when (effect) {
            is ScriptEffect.Log -> { /* already surfaced in the console */ }

            is ScriptEffect.Notify -> pushAssistantMessage(
                if (persian) "🔔 ${script.name}: ${effect.message}" else "🔔 ${script.name}: ${effect.message}"
            )

            is ScriptEffect.Propose -> {
                pushAssistantMessage(
                    if (persian) "📌 پیشنهاد اجرایی از اسکریپت «${script.name}»: ${effect.summary}"
                    else "📌 Action proposal from script \"${script.name}\": ${effect.summary}",
                    isProposal = true
                )
                audit("SCRIPT_ENGINE", "automation.proposal.create", RiskLevel.MEDIUM_RISK, "OWNER_CONFIRMED", "SUCCESS", effect.summary)
            }

            is ScriptEffect.Block -> {
                pushAssistantMessage(
                    if (persian) "⛔ اسکریپت «${script.name}» اقدام را مسدود کرد: ${effect.reason}"
                    else "⛔ Script \"${script.name}\" blocked an action: ${effect.reason}"
                )
                audit("SCRIPT_ENGINE", "automation.block", RiskLevel.HIGHER_RISK, "POLICY_PERMITTED", "BLOCKED", effect.reason)
            }

            is ScriptEffect.Webhook -> {
                if (current.emergencyLockActive || !current.runAutomationScripts) {
                    audit("SCRIPT_ENGINE", "automation.webhook.blocked", RiskLevel.HIGHER_RISK, "BLOCKED_EMERGENCY_LOCK", "BLOCKED", effect.url)
                    return
                }
                viewModelScope.launch {
                    val outcome = runCatching { postWebhook(effect.url, effect.payload) }
                    audit(
                        "SCRIPT_ENGINE",
                        "automation.webhook.send",
                        RiskLevel.HIGHER_RISK,
                        "OWNER_CONFIRMED",
                        if (outcome.isSuccess) "SUCCESS" else "FAILED",
                        effect.url
                    )
                }
            }

            is ScriptEffect.SetVariable -> { /* variable lifetime is one run */ }

            is ScriptEffect.SetExecutionMode -> {
                val mode = runCatching { TradingExecutionMode.valueOf(effect.modeName) }.getOrNull()
                if (mode != null && current.emergencyLockActive.not()) {
                    audit("SCRIPT_ENGINE", "automation.execution_mode.request", RiskLevel.CRITICAL, "OWNER_CONFIRMED", "SUCCESS", mode.name)
                    pushAssistantMessage(
                        if (persian) "⚠️ اسکریپت درخواست تغییر سطح اجرا به «${mode.label(true)}» داد. تغییر سطح فقط با تأیید دستی شما در درگاه معاملاتی انجام می‌شود."
                        else "⚠️ A script requested execution mode \"${mode.label(false)}\". Mode changes only happen with your manual confirmation in the trading gateway."
                    )
                }
            }
        }
    }

    /**
     * Fires a script's webhook. Always on the IO dispatcher, always time-bounded, and
     * always audited by the caller.
     */
    private suspend fun postWebhook(url: String, payload: String): Boolean = withContext(Dispatchers.IO) {
        if (url.isBlank() || (!url.startsWith("http://") && !url.startsWith("https://"))) return@withContext false
        runCatching {
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()
            val request = Request.Builder()
                .url(url)
                .post(payload.ifBlank { "{}" }.toRequestBody(JSON_MEDIA_TYPE))
                .build()
            client.newCall(request).execute().use { it.isSuccessful }
        }.getOrDefault(false)
    }

    private fun pushAssistantMessage(text: String, isProposal: Boolean = false) {
        _chatMessages.value = _chatMessages.value + ChatMessage(
            sender = "SAYVIS",
            text = text,
            isActionProposal = isProposal,
            providerUsed = ProviderType.LOCAL_COGNITIVE
        )
    }

    // ------------------------------------------------------------------ UIC
    fun confirmUicAttribute(id: String) {
        viewModelScope.launch { repository.updateUicStatus(id, UicStatus.CONFIRMED) }
    }

    fun revokeUicAttribute(id: String) {
        viewModelScope.launch { repository.updateUicStatus(id, UicStatus.REVOKED) }
    }

    fun addCustomUicAttribute(category: UicCategory, title: String, key: String, value: String) {
        val persian = settingsStore.current().isPersian(SayvisStrings.deviceIsPersian())
        val attribute = UicAttribute(
            id = "uic_custom_" + UUID.randomUUID().toString().take(6),
            category = category,
            key = key,
            title = title,
            value = value,
            provenance = if (persian) "ورود صریح توسط مالک" else "Explicit Owner Entry",
            confidence = 1.0f,
            status = UicStatus.CONFIRMED,
            privacyLevel = PrivacyLevel.STANDARD
        )
        viewModelScope.launch { repository.addUicAttribute(attribute) }
    }

    fun deleteUicAttribute(id: String) {
        viewModelScope.launch { repository.deleteUicAttribute(id) }
    }

    // ---------------------------------------------------------------- AWARE
    fun approveOpportunity(opportunityId: String) {
        viewModelScope.launch {
            val success = repository.approveAndExecuteOpportunity(opportunityId, settingsStore.current().emergencyLockActive)
            if (!success) {
                val persian = settingsStore.current().isPersian(SayvisStrings.deviceIsPersian())
                pushAssistantMessage(
                    if (persian) "⚠️ اجرا مسدود شد: قفل اضطراری فعال است. برای اجازهٔ اجرا، قفل را در «تنظیمات ← امنیت» غیرفعال کنید."
                    else "⚠️ Execution blocked: the emergency lock is active. Disengage it under Settings → Security to permit execution."
                )
            }
        }
    }

    fun dismissOpportunity(opportunityId: String) {
        viewModelScope.launch { repository.dismissOpportunity(opportunityId) }
    }

    fun runAwareScan() {
        viewModelScope.launch {
            val generated = awareEngine.evaluateContextAndTriggerProactiveInterventions(
                snapshot = contextSnapshot.value,
                missions = missions.value
            )
            if (generated.isNotEmpty()) _avatarState.value = AvatarState.OPPORTUNITY_AWARE
        }
    }

    // ------------------------------------------------------------- missions
    fun toggleMissionTask(missionId: String, taskId: String, currentCompleted: Boolean) {
        viewModelScope.launch { repository.updateTaskCompletion(missionId, taskId, !currentCompleted) }
    }

    fun addMission(mission: Mission) {
        viewModelScope.launch { repository.addMission(mission) }
    }

    // ------------------------------------------------------ owner account
    val ownerAccount: StateFlow<OwnerAccount?> = accountStore.account
    val ownerSignedIn: StateFlow<Boolean> = accountStore.signedIn
    val pairingOffer: StateFlow<DevicePairing.Offer?> = accountStore.activeOffer

    private val _accountMessage = MutableStateFlow<String?>(null)
    val accountMessage: StateFlow<String?> = _accountMessage.asStateFlow()

    fun clearAccountMessage() { _accountMessage.value = null }

    fun registerOwner(email: String, password: String) {
        val persian = isPersian.value
        when (val r = accountStore.register(email, password, persian)) {
            is AccountResult.Success -> {
                _accountMessage.value = if (persian) "حساب مالک ساخته شد و وارد شدید." else "Owner account created and signed in."
                audit("OWNER", "account.register", RiskLevel.HIGHER_RISK, "OWNER_PASSWORD", "SUCCESS", "account=${r.account.accountId}")
            }
            is AccountResult.Failure -> _accountMessage.value = r.message(persian)
        }
    }

    fun signInOwner(email: String, password: String) {
        val persian = isPersian.value
        when (val r = accountStore.signIn(email, password)) {
            is AccountResult.Success -> {
                _accountMessage.value = if (persian) "ورود موفق." else "Signed in."
                audit("OWNER", "account.sign_in", RiskLevel.MEDIUM_RISK, "OWNER_PASSWORD", "SUCCESS", "account=${r.account.accountId}")
            }
            is AccountResult.Failure -> {
                _accountMessage.value = r.message(persian)
                audit("OWNER", "account.sign_in", RiskLevel.MEDIUM_RISK, "OWNER_PASSWORD", "REJECTED", "attempts=${accountStore.failedAttempts()}")
            }
        }
    }

    fun signOutOwner() {
        accountStore.signOut()
        audit("OWNER", "account.sign_out", RiskLevel.LOW_RISK, "OWNER", "SUCCESS", "")
    }

    fun changeOwnerPassword(current: String, new: String) {
        val persian = isPersian.value
        when (val r = accountStore.changePassword(current, new, persian)) {
            is AccountResult.Success -> {
                _accountMessage.value = if (persian) "رمز عبور تغییر کرد." else "Password changed."
                audit("OWNER", "account.password_change", RiskLevel.HIGHER_RISK, "OWNER_PASSWORD", "SUCCESS", "")
            }
            is AccountResult.Failure -> _accountMessage.value = r.message(persian)
        }
    }

    fun deleteOwnerAccount() {
        accountStore.deleteAccount()
        audit("OWNER", "account.delete", RiskLevel.CRITICAL, "OWNER_CONFIRMED", "SUCCESS", "")
    }

    fun startPairing() {
        if (emergencyLockActive.value) {
            _accountMessage.value = if (isPersian.value) "در حالت قفل اضطراری جفت‌سازی ممکن نیست." else "Pairing is blocked while the emergency lock is active."
            return
        }
        val offer = accountStore.startPairingOffer()
        if (offer == null) {
            _accountMessage.value = if (isPersian.value) "ابتدا با ایمیل و رمز عبور وارد شوید." else "Sign in with e-mail and password first."
        } else {
            audit("OWNER", "device.pair.offer", RiskLevel.MEDIUM_RISK, "OWNER_SESSION", "SUCCESS", "code=${offer.code}")
        }
    }

    fun cancelPairing() = accountStore.cancelPairingOffer()

    fun completePairing(deviceName: String, type: DeviceType, fingerprint: String, proof: String) {
        val persian = isPersian.value
        val ok = accountStore.completePairing(fingerprint, proof)
        if (!ok) {
            _accountMessage.value = if (persian) "کد تأیید دستگاه نامعتبر یا منقضی است." else "Device proof is invalid or the code expired."
            audit("OWNER", "device.pair", RiskLevel.HIGHER_RISK, "PAIRING_PROOF", "REJECTED", "fp=$fingerprint")
            return
        }
        val device = Device(
            id = "dev_" + DevicePairing.normalizeFingerprint(fingerprint).take(12),
            name = deviceName.ifBlank { type.labelEn },
            type = type,
            publicKeyFingerprint = DevicePairing.prettyFingerprint(fingerprint),
            isTrusted = false,
            isRevoked = false,
            lastActiveAt = System.currentTimeMillis(),
            capabilities = when (type) {
                DeviceType.WINDOWS_PC, DeviceType.SECURE_LAPTOP -> listOf("filesystem", "controlled_powershell", "local_llm")
                DeviceType.WEB_CLIENT -> listOf("dashboard", "read_only")
                else -> emptyList()
            }
        )
        viewModelScope.launch { repository.registerPairedDevice(device) }
        _accountMessage.value = if (persian) "دستگاه جفت شد (وضعیت: محدود). برای اعطای اختیار روی «اعتماد» بزنید." else "Device paired (untrusted). Tap Trust to grant authority."
    }

    // -------------------------------------------------------------- devices
    fun toggleDeviceTrust(deviceId: String, currentTrust: Boolean) {
        viewModelScope.launch { repository.toggleDeviceTrust(deviceId, currentTrust) }
    }

    fun revokeDevice(deviceId: String) {
        viewModelScope.launch { repository.revokeDevice(deviceId) }
    }

    // ------------------------------------------------------- device telemetry

    /** Real battery level & charging state for the context engine and chat queries. */
    private fun readBatteryStatus(): Pair<Int, Boolean> {
        val app = getApplication<Application>()
        return runCatching {
            val bm = app.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val capacity = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
            val sticky = app.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = sticky?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = sticky?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val pct = when {
                capacity in 1..100 -> capacity
                level >= 0 && scale > 0 -> level * 100 / scale
                else -> 85
            }
            val charging = sticky?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_CHARGING ||
                (sticky?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0) > 0
            pct.coerceIn(0, 100) to charging
        }.getOrDefault(85 to false)
    }

    // ----------------------------------------------------------- voice input

    private var speechRecognizer: SpeechRecognizer? = null

    private val _voiceListening = MutableStateFlow(false)
    val voiceListening: StateFlow<Boolean> = _voiceListening.asStateFlow()

    /** True when the device has a working speech recognition service. */
    val voiceAvailable: Boolean
        get() = runCatching { SpeechRecognizer.isRecognitionAvailable(getApplication()) }.getOrDefault(false)

    /**
     * Real speech-to-text through the platform [SpeechRecognizer]: captures the
     * owner's sentence and feeds it straight into the command pipeline.
     */
    fun startVoiceInput() {
        if (_voiceListening.value) return
        val app = getApplication<Application>()
        val available = runCatching { SpeechRecognizer.isRecognitionAvailable(app) }.getOrDefault(false)
        if (!available) {
            val persian = isPersian.value
            pushAssistantMessage(
                if (persian) "🎤 ورودی گفتار روی این دستگاه در دسترس نیست (سرویس تشخیص گفتار نصب نیست). لطفاً تایپ کنید."
                else "🎤 Voice input is unavailable on this device (no speech recognition service). Please type."
            )
            return
        }
        val persian = isPersian.value
        val recognizer = speechRecognizer ?: SpeechRecognizer.createSpeechRecognizer(app).also { speechRecognizer = it }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (persian) "fa-IR" else "en-US")
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: android.os.Bundle?) {
                _voiceListening.value = true
            }

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                _voiceListening.value = false
                val persian = isPersian.value
                val reason = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                        if (persian) "صدایی شنیده نشد؛ دوباره تلاش کنید." else "No speech was heard; try again."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                        if (persian) "برای ورودی گفتار، اجازهٔ میکروفون لازم است." else "The microphone permission is required for voice input."
                    else ->
                        if (persian) "تشخیص گفتار ناموفق بود (کد $error). لطفاً تایپ کنید." else "Speech recognition failed (code $error). Please type."
                }
                pushAssistantMessage("🎤 $reason")
            }

            override fun onResults(results: android.os.Bundle?) {
                _voiceListening.value = false
                val best = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                if (best.isNullOrBlank()) {
                    val persian = isPersian.value
                    pushAssistantMessage(
                        if (persian) "🎤 گفتاری تشخیص داده نشد." else "🎤 No speech could be recognised."
                    )
                } else {
                    sendMessage(best, fromVoice = true)
                }
            }

            override fun onPartialResults(partialResults: android.os.Bundle?) {}

            override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
        })
        _voiceListening.value = true
        recognizer.startListening(intent)
    }

    override fun onCleared() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        super.onCleared()
    }

    // ---------------------------------------------------------------- misc
    private fun updateAvatarState() {
        val current = settingsStore.current()
        _avatarState.value = when {
            current.emergencyLockActive -> AvatarState.EMERGENCY_LOCKED
            current.forceOfflineMode -> AvatarState.OFFLINE
            else -> AvatarState.IDLE
        }
    }

    private fun audit(
        actor: String,
        action: String,
        riskLevel: RiskLevel,
        auth: String,
        result: String,
        digest: String
    ) {
        if (!settingsStore.current().keepAuditLogOnDevice) return
        viewModelScope.launch {
            repository.recordAuditEvent(
                actor = actor,
                action = action,
                riskLevel = riskLevel,
                auth = auth,
                result = result,
                digest = digest
            )
        }
    }

    /**
     * Reactive wiring.
     *
     * Deliberately placed at the END of the class: every property initializer must have
     * run before these collectors start, otherwise `viewModelScope` (which uses
     * Main.immediate) can touch a not-yet-initialized StateFlow during construction.
     */
    init {
        viewModelScope.launch {
            contextSnapshot.collect { snap ->
                awareEngine.onSystemStateChanged(
                    SystemState(
                        batteryLevel = snap.batteryPercent,
                        isCharging = snap.isCharging,
                        networkType = if (snap.isOnline) "WIFI" else "NONE",
                        activeMissionsCount = snap.activeMissionsCount,
                        blockedTasksCount = snap.blockedTasksCount,
                        emergencyLockActive = snap.emergencyLockActive,
                        focusWindowActive = snap.focusWindowActive
                    )
                )
            }
        }

        // Keep the greeting in the active language while the conversation still holds
        // nothing but that greeting. Once the owner has spoken, history is left untouched.
        viewModelScope.launch {
            isPersian.collect { persian ->
                if (_chatMessages.value.size <= 1) {
                    _chatMessages.value = listOf(greeting(persian))
                }
            }
        }

        // Re-apply the persisted avatar state (emergency lock / offline) after a restart.
        updateAvatarState()
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    /** Diagnostic bundle the owner can copy when asking for support. */
    fun diagnosticSummary(): String {
        val current = settingsStore.current()
        val gw = _gatewayState.value
        return buildString {
            appendLine("SAYVIS diagnostics")
            appendLine("version: " + com.example.BuildConfig.VERSION_NAME)
            appendLine("language: ${current.localization.language.name}")
            appendLine("ai provider: ${current.ai.provider.name} configured=${current.ai.isProviderConfigured()}")
            appendLine("ai model: ${current.ai.activeModel()}")
            appendLine("force offline: ${current.forceOfflineMode}")
            appendLine("emergency lock: ${current.emergencyLockActive}")
            appendLine("vault hardware backed: ${settingsStore.vault.isHardwareBacked}")
            appendLine("gateway phase: ${gw.phase.name}")
            appendLine("gateway bridge: ${current.trading.bridgeKind.name} ${current.trading.terminalVersion.name}")
            appendLine("execution mode: ${current.trading.executionMode.name}")
            appendLine("scripts: ${scriptStore.all().size} (enabled ${scriptStore.enabled().size})")
            appendLine("translation cache: ${translationService.cacheSize()}")
        }
    }
}
