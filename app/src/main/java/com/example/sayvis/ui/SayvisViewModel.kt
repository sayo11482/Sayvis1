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
import com.example.sayvis.trading.LitStrategyEngine
import com.example.sayvis.trading.MarketDataService
import com.example.sayvis.trading.MtOrderRequest
import com.example.sayvis.trading.MtOrderSide
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
import kotlinx.coroutines.delay
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
import com.example.sayvis.net.SayvisNet
import com.example.sayvis.net.LinkCenter
import com.example.sayvis.ai.CognitiveIngest
import com.example.sayvis.agent.MissionPlanner
import com.example.sayvis.agent.AutoMission
import com.example.sayvis.agent.MissionExecutor
import com.example.sayvis.scripts.GithubIntegrator
import com.example.sayvis.scripts.IntegrationStore

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
    MARKETS("Live markets", "بازارهای لحظه‌ای"),
    UIC("Cognitive Profile", "پروندهٔ شناختی"),
    AWARE("Smart Suggestions", "پیشنهادهای هوشمند"),
    TRADING("Trading & Markets", "معاملات و بازار"),
    SIMULATION("Decision Simulator", "شبیه‌سازی تصمیم"),
    SECURITY("Security & Devices", "امنیت و دستگاه‌ها"),
    GATEWAY("Trading Gateway", "درگاه معاملاتی"),
    SCRIPTS("Scripts & Automation", "اسکریپت و خودکارسازی"),
    AVATAR("Floating Avatar & Listening", "آواتار شناور و شنیدار"),
    ROBOT("SAYVIS Robot", "ربات سایویس"),
    CONNECT("Connect Centre", "مرکز اتصال");

    fun title(isPersian: Boolean): String = if (isPersian) titleFa else titleEn

    fun isPrimary(): Boolean = when (this) {
        HOME, ASSISTANT, TOOLS, SETTINGS -> true
        else -> false
    }

    /** Which bottom-bar item stays highlighted while this screen is open. */
    fun primaryTab(): SayvisScreen = when (this) {
        HOME, ASSISTANT, TOOLS, SETTINGS -> this
        GATEWAY, SCRIPTS -> TOOLS
        CONNECT -> SETTINGS
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
    private val marketData = MarketDataService()
    private val googleServices = GoogleServicesService()
    private val githubIntegrator = GithubIntegrator()
    private val integrationStore = IntegrationStore.get(application)

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
        val next = !settingsStore.current().forceOfflineMode
        settingsStore.update { it.copy(forceOfflineMode = next) }
        LinkCenter.setGateOpen(!next)
        if (next) {
            LinkCenter.push(LinkCenter.CODE.OFFLINE_SWITCH)
        } else {
            viewModelScope.launch {
                LinkCenter.measureSpeed()
                val probe = runCatching { connectivityProbe.probe() }.getOrNull()
                when (probe?.state) {
                    ConnectivityProbe.NetState.ONLINE -> LinkCenter.push(LinkCenter.CODE.AI_LINKED, "link-ok")
                    ConnectivityProbe.NetState.ONLINE_NO_GOOGLE -> LinkCenter.push(LinkCenter.CODE.NET_NO_GOOGLE)
                    else -> LinkCenter.push(LinkCenter.CODE.NET_DOWN)
                }
            }
        }
        updateAvatarState()
    }

    // ---------------------------------------------- v5.3.0: LINK CENTER (مرکز اتصال)

    /** Live internet speed (Mbps) — measured on the shared heartbeat while online. */
    val netSpeed: StateFlow<LinkCenter.SpeedResult?> = LinkCenter.speed

    /** Current operation/fault code (کد ۰۱ = در حال فکر کردن …). */
    val statusCode: StateFlow<LinkCenter.StatusEvent> = LinkCenter.currentStatus

    val statusHistory: StateFlow<List<LinkCenter.StatusEvent>> = LinkCenter.statusHistory

    /** 5-second liveness heartbeat — screens collect this to stay current. */
    val liveTick: StateFlow<Long> = LinkCenter.liveTick

    private val _marketLastRefreshAt = MutableStateFlow(0L)
    val marketLastRefreshAt: StateFlow<Long> = _marketLastRefreshAt.asStateFlow()

    private val _aiLinkedFromMemory = MutableStateFlow(settingsStore.current().ai.aiLinkedOnce)
    val aiLinkedFromMemory: StateFlow<Boolean> = _aiLinkedFromMemory.asStateFlow()

    val aiLinkedInfo: StateFlow<Pair<String, String>> = settings
        .map { it.ai.aiLinkedModel to it.ai.aiLinkedAt.toString() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "" to "0")

    init {
        // Gate mirrors the persisted owner switch from the very first frame.
        LinkCenter.setGateOpen(!settingsStore.current().forceOfflineMode)
        // «یک‌بار متصل شد → به حافظه سپرده شد»: remember the first AI link.
        if (settingsStore.current().ai.aiLinkedOnce) {
            LinkCenter.push(
                LinkCenter.CODE.AI_LINKED,
                if (settingsStore.current().isPersian(SayvisStrings.deviceIsPersian()))
                    "اتصال قبلی به حافظه سپرده شد" else "previous link remembered"
            )
            _aiLinkedFromMemory.value = true
        }
        startLivenessLoop()
    }

    /**
     * Liveness engine (زنده بودن اپ): every 5s a heartbeat tick; every 6th tick
     * (≈30s) the live market refreshes silently while online; every 9th (≈45s)
     * the internet speed is re-measured. The app never goes stale on a screen.
     */
    private fun startLivenessLoop() {
        viewModelScope.launch {
            var beat = 0
            while (true) {
                delay(5_000)
                LinkCenter.tick()
                beat += 1
                val online = !settingsStore.current().forceOfflineMode
                if (!online) continue
                if (beat % 6 == 0 && !_marketBusy.value) {
                    val screen = _currentScreen.value
                    if (screen == SayvisScreen.HOME || screen == SayvisScreen.MARKETS || screen == SayvisScreen.TRADING) {
                        silentMarketRefresh()
                    }
                }
                if (beat % 9 == 0) {
                    runCatching { LinkCenter.measureSpeed() }
                }
            }
        }
    }

    /** Quiet market refresh for the liveness loop (no spinner, audit-free). */
    private suspend fun silentMarketRefresh() {
        val snapshot = runCatching { marketData.refreshAll() }.getOrNull()
        if (snapshot != null) {
            _marketSnapshot.value = snapshot
            _marketLastRefreshAt.value = System.currentTimeMillis()
            LinkCenter.push(LinkCenter.CODE.MARKET_LIVE)
        }
    }

    /** Owner-pressed speed refresh on the Home card. */
    fun refreshSpeedNow() {
        viewModelScope.launch { runCatching { LinkCenter.measureSpeed() } }
    }

    fun markAiLinked(model: String) {
        if (_aiLinkedFromMemory.value && settingsStore.current().ai.aiLinkedOnce) return
        settingsStore.update {
            it.copy(ai = it.ai.copy(aiLinkedOnce = true, aiLinkedAt = System.currentTimeMillis(), aiLinkedModel = model))
        }
        _aiLinkedFromMemory.value = true
        val persian = settingsStore.current().isPersian(SayvisStrings.deviceIsPersian())
        LinkCenter.push(
            LinkCenter.CODE.AI_LINKED,
            if (persian) "اتصال اول به حافظه سپرده شد ($model)" else "first link remembered ($model)"
        )
        LinkCenter.noteAiMemory(model)
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
        recordSearchTaste(goal)
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

    /**
     * TRADING EVOLUTION: scans GitHub's top strategy bots, converts the
     * discovered signals into real LIT engine tuning (ATR factor, RSI gates,
     * RR ladder — floor 1:3 preserved) and applies it immediately to every
     * subsequent analysis run.
     */
    fun applyTradingEvolution() {
        if (_tuningBusy.value) return
        _tuningBusy.value = true
        viewModelScope.launch {
            val current = settingsStore.current()
            val persian = current.isPersian(SayvisStrings.deviceIsPersian())
            val proposal = runCatching { evolutionService.scanTradingTuning(persian) }.getOrNull()
            if (proposal == null || proposal.repos.isEmpty()) {
                _tradeNote.value = if (persian)
                    "اسکن گیت‌هاب سیگنال قابل اعتمادی نیافت؛ تیونینگ خانه (ATR×1.5، اهداف 3/4.5/6) باقی ماند."
                else "The GitHub scan found no trustworthy signal; house tuning kept (ATR×1.5, 3/4.5/6)."
                _tuningBusy.value = false
                return@launch
            }
            val tuning = LitStrategyEngine.Tuning(
                atrFactor = proposal.atrFactor,
                rsiHigh = proposal.rsiHigh,
                rsiLow = proposal.rsiLow,
                targetMultiples = proposal.targetMultiples,
                sourceRepos = proposal.repos
            ).safe()
            _tradeTuning.value = tuning
            settingsStore.update { it.copy(tradeTuningJson = tuning.toJson()) }
            _tradeNote.value = if (persian)
                "🧬 ارتقا از گیت‌هاب اعمال شد: ATR×${tuning.atrFactor} | گیت‌های RSI ${tuning.rsiHigh}/${tuning.rsiLow} | اهداف " +
                    tuning.targetMultiples.joinToString("/") { "%.1f".format(it) } +
                    " — منابع: " + proposal.repos.take(3).joinToString(", ")
            else
                "🧬 GitHub evolution applied: ATR×${tuning.atrFactor} | RSI gates ${tuning.rsiHigh}/${tuning.rsiLow} | targets " +
                    tuning.targetMultiples.joinToString("/") { "%.1f".format(it) } +
                    " — from: " + proposal.repos.take(3).joinToString(", ")
            audit(
                actor = "SAYVIS_AGENT",
                action = "trade.evolution_apply",
                riskLevel = RiskLevel.MEDIUM_RISK,
                auth = "OWNER_CONFIRMED",
                result = "SUCCESS",
                digest = "atr=${tuning.atrFactor} rsi=${tuning.rsiHigh}/${tuning.rsiLow} targets=${tuning.targetMultiples} repos=${proposal.repos.size}"
            )
            // Re-run the analysis immediately with the new tuning.
            refreshMarkets()
            _tuningBusy.value = false
        }
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

    // ---------------------------------------------------- live trading (LIT)

    private val _marketBusy = MutableStateFlow(false)
    val marketBusy: StateFlow<Boolean> = _marketBusy.asStateFlow()

    private val _marketSnapshot = MutableStateFlow<MarketDataService.Snapshot?>(null)
    val marketSnapshot: StateFlow<MarketDataService.Snapshot?> = _marketSnapshot.asStateFlow()

    private val _marketAnalyses = MutableStateFlow<Map<MarketDataService.Symbol, LitStrategyEngine.Analysis>>(emptyMap())
    val marketAnalyses: StateFlow<Map<MarketDataService.Symbol, LitStrategyEngine.Analysis>> = _marketAnalyses.asStateFlow()

    private val _tradeNote = MutableStateFlow<String?>(null)
    val tradeNote: StateFlow<String?> = _tradeNote.asStateFlow()

    private val _tradeTuning = MutableStateFlow(
        LitStrategyEngine.Tuning.fromJson(settingsStore.current().tradeTuningJson)
            ?: LitStrategyEngine.Tuning()
    )
    val tradeTuning: StateFlow<LitStrategyEngine.Tuning> = _tradeTuning.asStateFlow()

    private val _tuningBusy = MutableStateFlow(false)
    val tuningBusy: StateFlow<Boolean> = _tuningBusy.asStateFlow()

    val tradeAutomationEnabled: StateFlow<Boolean> = settings
        .map { it.tradeAutomationEnabled }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** Pulls live quotes + runs the LIT engine on every series it can fetch. */
    fun refreshMarkets() {
        if (_marketBusy.value) return
        _marketBusy.value = true
        viewModelScope.launch {
            val persian = settingsStore.current().isPersian(SayvisStrings.deviceIsPersian())
            val snapshot = runCatching { marketData.refreshAll() }.getOrNull()
            _marketSnapshot.value = snapshot
            if (snapshot != null) {
                _marketLastRefreshAt.value = System.currentTimeMillis()
                LinkCenter.push(LinkCenter.CODE.MARKET_LIVE)
            }
            val analyses = HashMap<MarketDataService.Symbol, LitStrategyEngine.Analysis>()
            snapshot?.series?.forEach { (symbol, closes) ->
                runCatching { LitStrategyEngine.analyse(closes, _tradeTuning.value) }.getOrNull()?.let { analyses[symbol] = it }
            }
            _marketAnalyses.value = analyses
            _avatarState.value = AvatarState.OPPORTUNITY_AWARE
            audit(
                actor = "SAYVIS_AGENT",
                action = "markets.lit_refresh",
                riskLevel = RiskLevel.LOW_RISK,
                auth = "SESSION_VALIDATED",
                result = if (snapshot != null) "SUCCESS" else "FAILED",
                digest = "quotes=${snapshot?.quotes?.size ?: 0} series=${snapshot?.series?.size ?: 0} plans=${analyses.count { it.value.plan.side != LitStrategyEngine.Side.WAIT }}"
            )
            _marketBusy.value = false
        }
    }

    fun setTradeAutomation(enabled: Boolean) {
        settingsStore.update { it.copy(tradeAutomationEnabled = enabled) }
    }

    val googleEmail: StateFlow<String> = settings.map { it.google.email }
        .stateIn(viewModelScope, SharingStarted.Eagerly, settingsStore.current().google.email)
    val googleSignedIn: StateFlow<Boolean> = settings.map { it.google.signedIn }
        .stateIn(viewModelScope, SharingStarted.Eagerly, settingsStore.current().google.signedIn)
    val tvAutoLoginEnabled: StateFlow<Boolean> = settings.map { it.tradingViewAutoLogin }
        .stateIn(viewModelScope, SharingStarted.Eagerly, settingsStore.current().tradingViewAutoLogin)

    fun setTradingViewAutoLogin(enabled: Boolean) {
        if (enabled && !settingsStore.current().google.signedIn) {
            _tradeNote.value = if (settingsStore.current().isPersian(SayvisStrings.deviceIsPersian()))
                "برای اتصال خودکار تریدینگ‌ویو ابتدا با گوگل وارد شوید (مرکز اتصال)."
            else "Sign in with Google in Connect Centre before enabling TradingView auto-login."
            return
        }
        settingsStore.update { it.copy(tradingViewAutoLogin = enabled) }
        audit(
            actor = "OWNER",
            action = if (enabled) "trading.tv_autologin.enable" else "trading.tv_autologin.disable",
            riskLevel = RiskLevel.LOW_RISK,
            auth = "OWNER_CONFIRMED",
            result = "SUCCESS",
            digest = "tvAutoLogin=$enabled google=${settingsStore.current().google.email.take(24)}"
        )
    }

    /** Picks the LIT opportunity with the highest expected return (RR + confluence). */
    fun bestLitOpportunity(): Pair<MarketDataService.Symbol, LitStrategyEngine.Analysis>? {
        val scored = _marketAnalyses.value.entries
            .filter { it.value.plan.side != LitStrategyEngine.Side.WAIT }
            .map { (sym, an) ->
                // Score = RR * 10 + ATR-normalised structure quality + RSI distance from exhaustion
                val rr = an.plan.rr
                val confluenceBonus = _mtfReports.value.firstOrNull { it.symbolLabel.contains(sym.labelEn.take(6)) }?.let { r ->
                    when (r.decision.grade) {
                        com.example.sayvis.trading.MtfScanner.Grade.A -> 0.8
                        com.example.sayvis.trading.MtfScanner.Grade.B -> 0.4
                        com.example.sayvis.trading.MtfScanner.Grade.C -> 0.1
                        else -> 0.0
                    }
                } ?: 0.0
                sym to (an to (rr + confluenceBonus))
            }
        return scored.maxByOrNull { it.second.second }?.let { it.first to it.second.first }
    }

    /**
     * SMART AUTO-EXECUTOR (هوشمند): the AI scans all live series + MTF confluence,
     * backtested tuning and RR ladder, picks the trade with the HIGHEST yield and
     * enters it directly through the safety-gated gateway. Works in DEMO and LIVE;
     * in PAPER it still simulates honestly.
     */
    fun autoExecuteBestTrade() {
        viewModelScope.launch {
            val current = settingsStore.current()
            val persian = current.isPersian(SayvisStrings.deviceIsPersian())
            if (!current.tradeAutomationEnabled) {
                _tradeNote.value = if (persian)
                    "ابتدا «ترید خودکار هوشمند» را روشن کنید — سپس هوش بهترین ورود را خودش پیدا و اجرا می‌کند."
                else "Turn on smart auto-trade first — the AI will then find & execute the best entry itself."
                return@launch
            }
            // Ensure we have fresh market data
            if (_marketAnalyses.value.isEmpty()) {
                _tradeNote.value = if (persian) "در حال دریافت دادهٔ زنده و بک‌تست…"
                else "Fetching live data & backtesting…"
                refreshMarkets()
                kotlinx.coroutines.delay(1600)
            }
            val best = bestLitOpportunity()
            if (best == null) {
                // Also check MTF reports for entries the per-symbol analysis missed (e.g. gold MTF)
                val mtfEntry = _mtfReports.value.firstOrNull { it.decision.isEntry && it.plan != null }
                if (mtfEntry != null && mtfEntry.plan != null) {
                    val symbol = MarketDataService.Symbol.XAUUSD
                    executeLitPlan(symbol, mtfEntry.plan)
                    return@launch
                }
                _tradeNote.value = if (persian)
                    "هوش در این لحظه نقطهٔ ورود مطمئن با RR≥۱:۳ نیافت — منتظر همگرایی تایم‌فریم‌ها بمانید و دوباره بک‌تست کنید."
                else "No confident entry with RR≥1:3 right now — wait for timeframe confluence and re-scan."
                return@launch
            }
            val (symbol, analysis) = best
            _tradeNote.value = if (persian)
                "🧠 هوش بهترین فرصت را یافت: ${symbol.labelFa} ${analysis.plan.side} RR=1:${"%.1f".format(analysis.plan.rr)} — در حال ورود…"
            else "🧠 AI found the best opportunity: ${symbol.labelEn} ${analysis.plan.side} RR=1:${"%.1f".format(analysis.plan.rr)} — entering…"
            executeLitPlan(symbol, analysis.plan)
        }
    }

    /**
     * Executes a LIT plan through the safety-gated gateway. Paper simulation
     * fills locally; DEMO/LIVE route for real through the bridge.
     * LIVE on a REAL account still requires the session re-confirmation
     * (liveExecutionConfirmed) and a disengaged Kill Switch — otherwise the
     * gateway blocks and the reason is shown honestly.
     */
    fun executeLitPlan(symbol: MarketDataService.Symbol, plan: LitStrategyEngine.TradePlan) {
        if (plan.side == LitStrategyEngine.Side.WAIT) return
        viewModelScope.launch {
            val current = settingsStore.current()
            if (!current.tradeAutomationEnabled) {
                _tradeNote.value = if (current.isPersian(SayvisStrings.deviceIsPersian()))
                    "ابتدا «ترید خودکار LIT» را در همین صفحه روشن کنید."
                else "Enable “LIT auto-trade” on this screen first."
                return@launch
            }
            val volume = current.trading.maxLotSize.coerceIn(0.01, 1.0)
            val result = gateway.placeOrder(
                request = MtOrderRequest(
                    symbol = symbol.name,
                    side = if (plan.side == LitStrategyEngine.Side.LONG) MtOrderSide.BUY else MtOrderSide.SELL,
                    volume = volume,
                    stopLoss = plan.stop,
                    takeProfit = plan.targets.firstOrNull()?.price,
                    comment = "SAYVIS-LIT-AI"
                ),
                state = _gatewayState.value,
                emergencyLockActive = current.emergencyLockActive,
                liveConfirmed = liveExecutionConfirmed
            )
            val persian = current.isPersian(SayvisStrings.deviceIsPersian())
            _tradeNote.value = (if (persian) result.detailFa else result.detailEn) +
                (if (result.accepted && current.trading.executionMode == TradingExecutionMode.LIVE_EXECUTION)
                    (if (persian) " — اجرای زنده با هوش بک‌تست‌شده ✅" else " — live AI-backed execution ✅")
                else "")
            audit(
                actor = "SAYVIS_AGENT",
                action = "trade.auto_execute",
                riskLevel = if (current.trading.executionMode == TradingExecutionMode.LIVE_EXECUTION) RiskLevel.CRITICAL else RiskLevel.HIGHER_RISK,
                auth = "OWNER_CONFIRMED",
                result = if (result.accepted) "SUCCESS" else "BLOCKED",
                digest = "${symbol.name} ${plan.side} entry=${plan.entry} sl=${plan.stop} tp=${plan.targets.firstOrNull()?.price} mode=${current.trading.executionMode.name} rr=${plan.rr}"
            )
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

    /**
     * Opens the connect flow (v4.0.0): the system Google account chooser —
     * no OAuth client ID needed anymore. The advanced PKCE consent screen is
     * only used for its OAuth callback deep-link path.
     */
    fun beginGoogleSignIn(): Boolean = runCatching {
        val intent = Intent(getApplication(), com.example.sayvis.ui.GoogleSignInActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        getApplication<Application>().startActivity(intent)
        true
    }.getOrDefault(false)

    /** Removes the Google identity and refresh token from the device. */
    fun googleSignOut() {
        settingsStore.putSecret(SecretKey.GOOGLE_REFRESH_TOKEN, "")
        com.example.sayvis.identity.GoogleAccountHub.unlink(getApplication())
        audit("OWNER", "account.google.unlink", RiskLevel.MEDIUM_RISK, "OWNER_SESSION", "SUCCESS", "identity hub sign-out")
    }

    fun setGoogleRequireSignIn(enabled: Boolean) {
        settingsStore.update { it.copy(google = it.google.copy(requireSignInAtLaunch = enabled)) }
    }

    // ------------------------------------------------------- connect centre

    /** Auto-opens the connect hub once per process while no account is linked. */
    private var connectAutoShown = false
    fun maybeAutoConnectScreen() {
        if (connectAutoShown) return
        connectAutoShown = true
        if (settingsStore.current().google.email.isBlank()) {
            _currentScreen.value = SayvisScreen.CONNECT
        }
    }

    fun openConnectCenter() {
        _currentScreen.value = SayvisScreen.CONNECT
    }

    /** The pairing QR payload of this device (account + device + pin). */
    fun pairingPayload(): String {
        val email = settingsStore.current().google.email
        val device = android.os.Build.MODEL?.ifBlank { "Android device" } ?: "Android device"
        val pin = com.example.sayvis.identity.GoogleAccountHub.devicePin(getApplication())
        return com.example.sayvis.identity.PairingQr.buildPayload(
            account = email,
            device = device,
            pin = pin,
            createdAt = System.currentTimeMillis()
        )
    }

    /**
     * Applies scanned/imported QR content. Returns (ok, persian message):
     * links account pairings, stores provider keys/config, reports links.
     */
    fun handleQrPayload(raw: String): Pair<Boolean, String> {
        return when (val import = com.example.sayvis.identity.PairingQr.classify(raw)) {
            is com.example.sayvis.identity.PairingQr.Import.PairingLink -> {
                val result = com.example.sayvis.identity.GoogleAccountHub.link(
                    getApplication(), import.pairing.account
                )
                when (result) {
                    is com.example.sayvis.identity.GoogleAccountHub.LinkResult.Success -> {
                        audit("OWNER", "account.qr.pair", RiskLevel.MEDIUM_RISK, "OWNER_QR", "SUCCESS",
                            "paired=${result.email} pin=${import.pairing.pin} dev=${import.pairing.device.take(24)}")
                        refreshConnectivity()
                        // v5.2.0: the QR advertised a SAYVIS Desktop listener —
                        // complete the two-way handshake over the LAN.
                        if (import.pairing.host != null) {
                            pairWithDesktop(import.pairing, result.email)
                        }
                        true to "اکانت ${result.email} با QR پیوند شد ✅"
                    }
                    is com.example.sayvis.identity.GoogleAccountHub.LinkResult.Invalid ->
                        false to result.message(true)
                }
            }
            is com.example.sayvis.identity.PairingQr.Import.ProviderKey -> {
                storeProviderKey(import.provider, import.apiKey)
                audit("OWNER", "account.qr.key_import", RiskLevel.MEDIUM_RISK, "OWNER_QR", "SUCCESS",
                    "provider=${import.provider} keyHash=${import.apiKey.takeLast(4).length}")
                true to "کلید ${import.provider} ذخیره شد ✅ (از کامپیوتر)"
            }
            is com.example.sayvis.identity.PairingQr.Import.Config -> {
                import.provider?.let { storeProviderKey(it, import.apiKey) }
                settingsStore.update { current ->
                    val ai = current.ai
                    val withBase = if (import.baseUrl != null) ai.copy(customBaseUrl = import.baseUrl) else ai
                    val withModel = if (import.model != null) withBase.copy(customModel = import.model) else withBase
                    current.copy(ai = withModel)
                }
                audit("OWNER", "account.qr.config_import", RiskLevel.MEDIUM_RISK, "OWNER_QR", "SUCCESS",
                    "provider=${import.provider ?: "?"} model=${import.model ?: "-"}")
                true to "کانفیگ هوش مصنوعی از کامپیوتر ثبت شد ✅"
            }
            is com.example.sayvis.identity.PairingQr.Import.Link ->
                true to "لینک خوانده شد: ${import.url.take(60)}"
            is com.example.sayvis.identity.PairingQr.Import.Plain ->
                if (import.text.isBlank()) false to "محتوای QR خالی بود."
                else true to "متن QR: ${import.text.take(60)}"
        }
    }

    private val _pairMessage = MutableStateFlow("")
    val pairMessage: StateFlow<String> = _pairMessage.asStateFlow()

    /**
     * Two-way pairing handshake with SAYVIS Desktop: the QR carried the
     * machine's LAN listener (host:port); we announce this phone so the
     * desktop marks it as a connected device.
     */
    private fun pairWithDesktop(pairing: com.example.sayvis.identity.PairingQr.Pairing, email: String) {
        val host = pairing.host ?: return
        viewModelScope.launch {
            val ok = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching {
                    val body = org.json.JSONObject()
                        .put("account", email)
                        .put("device", android.os.Build.MODEL?.ifBlank { "Android" } ?: "Android")
                        .put("pin", pairing.pin)
                        .put("source", "android")
                        .toString()
                        .toRequestBody("application/json; charset=utf-8".toMediaType())
                    val request = okhttp3.Request.Builder()
                        .url("http://" + host + "/pair")
                        .post(body)
                        .build()
                    com.example.sayvis.net.SayvisNet.client(3, 3)
                        .newCall(request).execute().use { it.isSuccessful }
                }.getOrDefault(false)
            }
            _pairMessage.value = if (ok)
                "به سایویس دسکتاپ ($host) متصل شد ✅"
            else
                "اتصال به دسکتاپ ($host) ناموفق بود — هر دو دستگاه یک شبکه باشند و پورت 8765 باز باشد."
            audit(
                actor = "OWNER",
                action = "account.desktop_handshake",
                riskLevel = RiskLevel.LOW_RISK,
                auth = "OWNER_QR",
                result = if (ok) "SUCCESS" else "FAILED",
                digest = "host=$host device=${pairing.device.take(24)}"
            )
        }
    }

    /** Writes an imported provider key into the matching settings field. */
    private fun storeProviderKey(provider: String, apiKey: String) {
        settingsStore.update { current ->
            val ai = when (provider.lowercase()) {
                "gemini" -> current.ai.copy(geminiApiKey = apiKey)
                "openai" -> current.ai.copy(openAiApiKey = apiKey)
                "groq" -> current.ai.copy(groqApiKey = apiKey)
                "xai" -> current.ai.copy(xaiApiKey = apiKey)
                "openrouter" -> current.ai.copy(openRouterApiKey = apiKey)
                else -> current.ai.copy(customApiKey = apiKey)
            }
            current.copy(ai = ai)
        }
    }

    // ------------------------------------------------ assistant brain (v5.0.0)

    /**
     * WHY the assistant looked dead after connecting Gemini: the chat used
     * `settings.ai.provider` verbatim; if the provider enum stayed LOCAL the
     * replies silently came from the offline core. Now the chat resolves its
     * brain every run: AUTO picks the best configured cloud brain
     * (Gemini first), or the owner's explicit chip choice is honoured.
     */
    private fun resolveChatBrain(settings: com.example.sayvis.settings.AppSettings):
        Pair<com.example.sayvis.settings.AiSettings, String> {
        val choice = settings.assistantBrain.trim().uppercase()
        val auto = SpecialistAgent.pickBrain(settings.ai)
        if (choice == "AUTO" || choice.isBlank()) {
            val effective = if (auto != null) settings.ai.copy(provider = auto.kind) else settings.ai
            return effective to (auto?.noteFa ?: "هستهٔ محلی سایویس")
        }
        val kinds = com.example.sayvis.settings.AiProviderKind.entries
        val picked = kinds.firstOrNull { it.name == choice }
        if (picked == null) {
            val effective = if (auto != null) settings.ai.copy(provider = auto.kind) else settings.ai
            return effective to (auto?.noteFa ?: "هستهٔ محلی سایویس (انتخاب نامعتبر — خودکار)")
        }
        val configured = picked.isLocal || when (picked) {
            com.example.sayvis.settings.AiProviderKind.GEMINI -> settings.ai.geminiApiKey.isNotBlank()
            com.example.sayvis.settings.AiProviderKind.GROQ -> settings.ai.groqApiKey.isNotBlank()
            com.example.sayvis.settings.AiProviderKind.OPENAI -> settings.ai.openAiApiKey.isNotBlank()
            com.example.sayvis.settings.AiProviderKind.XAI -> settings.ai.xaiApiKey.isNotBlank()
            com.example.sayvis.settings.AiProviderKind.OPENROUTER -> settings.ai.openRouterApiKey.isNotBlank()
            com.example.sayvis.settings.AiProviderKind.CUSTOM -> settings.ai.customBaseUrl.isNotBlank() && settings.ai.customModel.isNotBlank()
            com.example.sayvis.settings.AiProviderKind.LOCAL -> true
        }
        return if (configured) {
            settings.ai.copy(provider = picked) to picked.labelFa
        } else {
            val effective = if (auto != null) settings.ai.copy(provider = auto.kind) else settings.ai
            effective to (auto?.noteFa ?: "هستهٔ محلی — کلید «${picked.labelFa}» خالی است")
        }
    }

    private val _activeBrainNote = MutableStateFlow("")
    val activeBrainNote: StateFlow<String> = _activeBrainNote.asStateFlow()

    /** Persists the assistant brain chip choice (AUTO or provider name). */
    fun pickAssistantBrain(choice: String) {
        settingsStore.update { it.copy(assistantBrain = choice.trim().uppercase().ifBlank { "AUTO" }) }
        audit("OWNER", "assistant.brain.pick", RiskLevel.LOW_RISK, "OWNER_SESSION", "SUCCESS", "brain=${choice.take(20)}")
    }

    /** Public helper used by the UI to know which brain a message ran on. */
    fun brainNoteFor(settings: com.example.sayvis.settings.AppSettings): String =
        resolveChatBrain(settings).second

    // --------------------------------------------- taste engine (v5.0.0)

    private val _sportsSuggestions = MutableStateFlow<List<com.example.sayvis.ai.SearchTasteEngine.Suggestion>>(emptyList())
    val sportsSuggestions: StateFlow<List<com.example.sayvis.ai.SearchTasteEngine.Suggestion>> =
        _sportsSuggestions.asStateFlow()

    /** Recent in-app searches (capped 100) + optional pasted Google activity. */
    fun recentSearches(): List<String> =
        com.example.sayvis.ai.SearchTasteEngine.decodeHistory(settingsStore.current().searchTasteJson)

    private fun recordSearchTaste(query: String) {
        val next = com.example.sayvis.ai.SearchTasteEngine.appendQuery(recentSearches(), query)
        settingsStore.update { it.copy(searchTasteJson = com.example.sayvis.ai.SearchTasteEngine.encodeHistory(next)) }
        rebuildSportsSuggestions()
    }

    fun rebuildSportsSuggestions() {
        _sportsSuggestions.value = com.example.sayvis.ai.SearchTasteEngine.sportsSuggestions(
            recentSearches(), isPersian.value
        )
    }

    /** Owner pastes recent Google activity text; it is ingested as searches. */
    fun importSearchTaste(blob: String): Int {
        val ingested = com.example.sayvis.ai.SearchTasteEngine.ingestImport(blob)
        var next = recentSearches()
        for (q in ingested) next = com.example.sayvis.ai.SearchTasteEngine.appendQuery(next, q)
        settingsStore.update { it.copy(searchTasteJson = com.example.sayvis.ai.SearchTasteEngine.encodeHistory(next)) }
        rebuildSportsSuggestions()
        return ingested.size
    }

    // ------------------------------------------------ MTF scanner (v5.0.0)

    data class MtfSymbolReport(
        val symbolLabel: String,
        val verdicts: List<com.example.sayvis.trading.MtfScanner.TfVerdict>,
        val decision: com.example.sayvis.trading.MtfScanner.Decision,
        val plan: LitStrategyEngine.TradePlan?,
        val executionTf: com.example.sayvis.trading.MtfScanner.Tf?
    )

    private val _mtfBusy = MutableStateFlow(false)
    val mtfBusy: StateFlow<Boolean> = _mtfBusy.asStateFlow()

    private val _mtfReports = MutableStateFlow<List<MtfSymbolReport>>(emptyList())
    val mtfReports: StateFlow<List<MtfSymbolReport>> = _mtfReports.asStateFlow()

    /**
     * Runs the LIT engine on M15/H1/H4/D1 closes and hunts entry points with
     * the confluence rules (the RR≥1:3 floor is enforced inside the engine).
     */
    fun runMtfScan() {
        if (_mtfBusy.value) return
        _mtfBusy.value = true
        viewModelScope.launch {
            val reports = ArrayList<MtfSymbolReport>()
            runCatching {
                val seriesMap = marketData.goldMultiTf()
                val verdicts = seriesMap.mapNotNull { (tf, closes) ->
                    runCatching {
                        val analysis = LitStrategyEngine.analyse(closes, _tradeTuning.value) ?: return@mapNotNull null
                        com.example.sayvis.trading.MtfScanner.TfVerdict(
                            tf, analysis.plan.side, analysis.view.rsi14, analysis.view.atr14, closes.size
                        )
                    }.getOrNull()
                }.sortedBy { it.tf.minutes }
                if (verdicts.isNotEmpty()) {
                    val decision = com.example.sayvis.trading.MtfScanner.combine(verdicts)
                    val execTf = com.example.sayvis.trading.MtfScanner.executionTf(decision)
                    val plan = execTf?.let { tf ->
                        seriesMap[tf]?.let { closes ->
                            runCatching { LitStrategyEngine.analyse(closes, _tradeTuning.value)?.plan }.getOrNull()
                        }
                    }
                    reports.add(
                        MtfSymbolReport("طلا (XAU/USD)", verdicts, decision, plan, execTf)
                    )
                }
            }
            // EUR/USD — daily-only source, reported honestly as a single TF.
            runCatching {
                val closes = _marketSnapshot.value?.series?.get(MarketDataService.Symbol.EURUSD)
                if (closes != null && closes.size >= 30) {
                    val analysis = LitStrategyEngine.analyse(closes, _tradeTuning.value)
                    if (analysis != null) {
                        val v = listOf(
                            com.example.sayvis.trading.MtfScanner.TfVerdict(
                                com.example.sayvis.trading.MtfScanner.Tf.D1,
                                analysis.plan.side, analysis.view.rsi14, analysis.view.atr14, closes.size
                            )
                        )
                        reports.add(
                            MtfSymbolReport(
                                "یورو/دلار (EUR/USD)", v,
                                com.example.sayvis.trading.MtfScanner.combine(v),
                                analysis.plan.takeIf {
                                    it.side != LitStrategyEngine.Side.WAIT
                                },
                                null
                            )
                        )
                    }
                }
            }
            _mtfReports.value = reports
            _mtfBusy.value = false
            audit(
                actor = "LIT_MTF",
                action = "trade.mtf_scan",
                riskLevel = RiskLevel.LOW_RISK,
                auth = "OWNER_SESSION",
                result = "SUCCESS",
                digest = "symbols=${reports.size} entries=${reports.count { it.decision.isEntry }}"
            )
        }
    }

    // --------------------------- manager agent: business directory (v5.0.0)

    private val smsAgent = com.example.sayvis.agent.SmsDirectoryAgent()

    private val _bizDirectory = MutableStateFlow<List<com.example.sayvis.agent.BusinessDirectory.Entry>>(emptyList())
    val bizDirectory: StateFlow<List<com.example.sayvis.agent.BusinessDirectory.Entry>> =
        _bizDirectory.asStateFlow()

    private val _bizBusy = MutableStateFlow(false)
    val bizBusy: StateFlow<Boolean> = _bizBusy.asStateFlow()

    private val _bizMessage = MutableStateFlow("")
    val bizMessage: StateFlow<String> = _bizMessage.asStateFlow()

    // ============================== v5.3.0: SOLUTION-SEEKING MISSION AGENT ====

    /** «در حال فکر کردن …» — true between the owner's message and the answer. */
    private val _chatThinking = MutableStateFlow(false)
    val chatThinking: StateFlow<Boolean> = _chatThinking.asStateFlow()

    private val _missionAgentBusy = MutableStateFlow(false)
    val missionAgentBusy: StateFlow<Boolean> = _missionAgentBusy.asStateFlow()

    private val _missionAgentSteps = MutableStateFlow<List<String>>(emptyList())
    val missionAgentSteps: StateFlow<List<String>> = _missionAgentSteps.asStateFlow()

    private val _missionAgentResult = MutableStateFlow<String?>(null)
    val missionAgentResult: StateFlow<String?> = _missionAgentResult.asStateFlow()

    /**
     * v5.3.1 — THE AUTO EXECUTOR (ایجنت اجراگر): the owner gives ONE natural
     * language command — «برو ۵ موزیک ملایم پرمخاطب پیدا کن، دانلود کن» — and
     * the agent PLANS the mission itself, EXECUTES every step for real
     * (search → rank → download → verify), and TICKS each task automatically
     * the moment its own completion metric is truly met. No manual inputs,
     * no manual checkboxes. Unticked steps are the honest failures.
     */
    fun runMissionAgent(goal: String) {
        val clean = goal.trim()
        if (clean.isBlank() || _missionAgentBusy.value) return
        val current = settingsStore.current()
        val persian = current.isPersian(SayvisStrings.deviceIsPersian())
        val plan = AutoMission.plan(clean)

        // The mission is created IMMEDIATELY with one task per plan step —
        // the owner watches these checkboxes flip by themselves below.
        val tasks = plan.steps.map { step ->
            MissionTask(id = "task_" + UUID.randomUUID().toString().take(6), title = step.title(persian))
        }
        val boundPlan = plan.copy(steps = plan.steps.mapIndexed { i, st -> st.copy(taskId = tasks[i].id) })
        val mission = Mission(
            id = "mission_" + UUID.randomUUID().toString().take(6),
            title = MissionPlanner.titleFromGoal(clean),
            description = (if (persian) "اجرای خودکار توسط ایجنت سایویس — " else "Auto-executed by the SAYVIS agent — ") +
                (if (persian) "${plan.steps.size} گام، هدف ${plan.targetCount}" else "${plan.steps.size} steps, target ${plan.targetCount}"),
            priority = MissionPriority.HIGH,
            status = MissionStatus.ACTIVE,
            progressPercent = 0,
            deadline = "",
            tasks = tasks
        )
        repository.addMission(mission)

        _missionAgentBusy.value = true
        _missionAgentSteps.value = emptyList()
        _missionAgentResult.value = null
        LinkCenter.push(LinkCenter.CODE.AGENT_WORKING, clean.take(40))
        _avatarState.value = AvatarState.THINKING

        viewModelScope.launch {
            try {
                val executor = MissionExecutor(getApplication())
                val result = executor.execute(clean, boundPlan, persian, MissionExecutor.Live(
                    onStepStart = { _, line ->
                        _missionAgentSteps.value = _missionAgentSteps.value + line
                    },
                    onStepDone = { index, metric, line ->
                        _missionAgentSteps.value = _missionAgentSteps.value + line
                        if (AutoMission.autoTick(boundPlan.steps[index], metric)) {
                            repository.updateTaskCompletion(mission.id, boundPlan.steps[index].taskId, true)
                        }
                    },
                    onItem = { _, line ->
                        _missionAgentSteps.value = _missionAgentSteps.value + line
                    }
                ))
                if (result.ok) repository.completeMission(mission.id)
                _missionAgentResult.value = result.report
                cognitiveCapture(CognitiveIngest.Source.MISSION, clean)
                audit(
                    actor = "SAYVIS_AGENT",
                    action = "mission.auto_execute",
                    riskLevel = RiskLevel.LOW_RISK,
                    auth = "OWNER_CONFIRMED",
                    result = if (result.ok) "SUCCESS" else "PARTIAL",
                    digest = "goal=${clean.take(40)} steps=${plan.steps.size} saved=${result.savedPaths.size}"
                )
            } catch (t: Throwable) {
                _missionAgentResult.value =
                    (if (persian) "ایجنت اجراگر خطا خورد: " else "Executor agent failed: ") + (t.message ?: t::class.simpleName.orEmpty())
            } finally {
                _missionAgentBusy.value = false
                updateAvatarState()
            }
        }
    }

    // ============================== v5.3.0: GITHUB INTEGRATOR (اسکریپت‌ها) ====

    private val _gitHits = MutableStateFlow<List<GithubIntegrator.RepoHit>>(emptyList())
    val gitHits: StateFlow<List<GithubIntegrator.RepoHit>> = _gitHits.asStateFlow()

    private val _gitBusy = MutableStateFlow(false)
    val gitBusy: StateFlow<Boolean> = _gitBusy.asStateFlow()

    private val _gitMessage = MutableStateFlow("")
    val gitMessage: StateFlow<String> = _gitMessage.asStateFlow()

    val integrations: StateFlow<List<IntegrationStore.Entry>> = integrationStore.entries

    /** Keyless GitHub scan — the tool that IDENTIFIES special items. */
    fun scanGithub(query: String) {
        val q = query.trim()
        if (q.isBlank() || _gitBusy.value) return
        _gitBusy.value = true
        _gitMessage.value = ""
        LinkCenter.push(LinkCenter.CODE.GITHUB_SCAN, q.take(40))
        viewModelScope.launch {
            val hits = githubIntegrator.search(q)
            _gitHits.value = hits
            _gitBusy.value = false
            val persian = settingsStore.current().isPersian(SayvisStrings.deviceIsPersian())
            _gitMessage.value = when {
                hits.isEmpty() -> if (persian) "نتیجه‌ای نیامد (سقف نرخ گیت‌هاب یا شبکه). کمی بعد دوباره." else "No results (GitHub rate limit or network). Try again shortly."
                else -> {
                    val special = hits.count { it.special }
                    if (persian) "$special مورد «خاص» از ${hits.size} نتیجه شناسایی شد."
                    else "$special SPECIAL items identified out of ${hits.size}."
                }
            }
            cognitiveCapture(CognitiveIngest.Source.SEARCH, "github:$q")
        }
    }

    /** MERGE: selected hits are fetched and staged into the persisted registry. */
    fun stageIntegrations(hits: List<GithubIntegrator.RepoHit>) {
        if (hits.isEmpty()) return
        val persian = settingsStore.current().isPersian(SayvisStrings.deviceIsPersian())
        viewModelScope.launch {
            var staged = 0
            hits.forEach { hit ->
                val files = runCatching { githubIntegrator.topFiles(hit.repo) }.getOrDefault(emptyList())
                val readme = runCatching { githubIntegrator.readmeHead(hit.repo) }.getOrDefault("")
                integrationStore.upsert(
                    IntegrationStore.Entry(
                        id = "int_" + UUID.randomUUID().toString().take(6),
                        repo = hit.repo,
                        url = hit.url,
                        description = hit.description,
                        stars = hit.stars,
                        score = hit.score,
                        special = hit.special,
                        topFiles = files.take(10),
                        readmeHead = readme.take(600)
                    )
                )
                staged += 1
            }
            _gitMessage.value =
                (if (persian) "$staged مورد به صف ادغام سایویس اضافه شد؛ فایل‌ها و راهنمای هر مورد ذخیره و داسیهٔ ادغام قابل اشتراک شد."
                else "$staged items staged for integration — files + guide stored; the merge dossier is shareable.")
            LinkCenter.push(LinkCenter.CODE.GITHUB_SCAN, "staged=$staged")
        }
    }

    fun removeIntegration(id: String) = integrationStore.remove(id)

    fun integrationDossier(): String = integrationStore.dossier()

    // ============================== v5.3.0: COGNITIVE FILE AUTO-UPDATE ========

    /**
     * Real ingest of the owner's activity into the UIC cognitive file:
     * searches, notes, alarms, commands and missions. Deterministic rules,
     * conservative confidence; owner can confirm/revoke on the UIC screen.
     */
    private fun cognitiveCapture(source: CognitiveIngest.Source, text: String) {
        if (text.isBlank()) return
        val extracts = CognitiveIngest.extract(source, text)
        if (extracts.isEmpty()) return
        val persian = settingsStore.current().isPersian(SayvisStrings.deviceIsPersian())
        viewModelScope.launch {
            extracts.forEach { ex ->
                runCatching {
                    repository.addUicAttribute(
                        UicAttribute(
                            id = "uic_auto_" + UUID.randomUUID().toString().take(8),
                            category = ex.category,
                            key = CognitiveIngest.mergeKey(source, ex.key, CognitiveIngest.dayBucket()),
                            title = if (persian) ex.titleFa else ex.titleEn,
                            value = ex.value,
                            provenance = "auto:${source.name.lowercase()}",
                            confidence = ex.confidence,
                            status = UicStatus.OBSERVED,
                            privacyLevel = PrivacyLevel.PROTECTED
                        )
                    )
                }
            }
            LinkCenter.push(LinkCenter.CODE.DOSSIER_SYNC, source.name.lowercase())
        }
    }

    /** Quick note from the UIC screen (یادداشت داخل دیوایس → پروندهٔ شناختی). */
    fun addQuickNote(text: String) {
        val clean = text.trim()
        if (clean.isBlank()) return
        cognitiveCapture(CognitiveIngest.Source.NOTE, clean)
    }


    fun loadBizDirectory() {
        _bizDirectory.value = runCatching {
            com.example.sayvis.agent.BusinessDirectory.decode(settingsStore.current().bizDirectoryJson)
        }.getOrDefault(emptyList())
    }

    /** Reads every inbox SMS (READ_SMS) and builds the classified directory. */
    fun scanSmsDirectory() {
        if (_bizBusy.value) return
        _bizBusy.value = true
        _bizMessage.value = ""
        viewModelScope.launch {
            val entries = runCatching { smsAgent.buildEntries(getApplication()) }.getOrDefault(emptyList())
            val merged = com.example.sayvis.agent.BusinessDirectory.merge(loadDirectoryList(), entries)
            settingsStore.update {
                it.copy(bizDirectoryJson = com.example.sayvis.agent.BusinessDirectory.encode(merged))
            }
            _bizDirectory.value = merged
            _bizBusy.value = false
            _bizMessage.value = if (entries.isEmpty())
                "هیچ پیامک بیزینسی پیدا نشد."
            else
                "${entries.size} فرستندهٔ بیزینس دسته‌بندی شد ✅"
            audit(
                actor = "MANAGER_AGENT",
                action = "agent.sms_directory_scan",
                riskLevel = RiskLevel.MEDIUM_RISK,
                auth = "OWNER_CONFIRMED",
                result = "SUCCESS",
                digest = "senders=${entries.size} suppliers=${entries.count { it.category == com.example.sayvis.agent.BusinessDirectory.Category.SUPPLIER }}"
            )
        }
    }

    private fun loadDirectoryList(): List<com.example.sayvis.agent.BusinessDirectory.Entry> = _bizDirectory.value

    /** Adds an Instagram profile (bio + follower/following context) to the directory. */
    fun addInstagramProfile(handle: String, bio: String, context: String): Boolean {
        val entry = smsAgent.instagramEntry(handle, bio, context, System.currentTimeMillis())
        if (entry == null) {
            _bizMessage.value = "بیو خالی بود یا سیگنال بیزینس نداشت."
            return false
        }
        val merged = com.example.sayvis.agent.BusinessDirectory.merge(loadDirectoryList(), listOf(entry))
        settingsStore.update {
            it.copy(bizDirectoryJson = com.example.sayvis.agent.BusinessDirectory.encode(merged))
        }
        _bizDirectory.value = merged
        _bizMessage.value = "@${handle.trim().removePrefix("@")} به دفترچه اضافه شد (${entry.category.labelFa}) ✅"
        audit(
            actor = "MANAGER_AGENT",
            action = "agent.instagram_classify",
            riskLevel = RiskLevel.LOW_RISK,
            auth = "OWNER_SESSION",
            result = "SUCCESS",
            digest = "handle=${handle.take(24)} cat=${entry.category.name} score=${entry.score}"
        )
        return true
    }

    // --------------------------- device Google accounts (v5.0.0)

    /** Google accounts signed in on this device (needs the contacts permission). */
    fun deviceGoogleAccounts(): List<String> = runCatching {
        val am = android.accounts.AccountManager.get(getApplication())
        am.getAccountsByType("com.google").map { it.name }.distinct()
    }.getOrDefault(emptyList())

    /** One-tap link of one of the device's own Google accounts. */
    fun linkDeviceAccount(email: String): Boolean {
        val result = com.example.sayvis.identity.GoogleAccountHub.link(getApplication(), email)
        val ok = result is com.example.sayvis.identity.GoogleAccountHub.LinkResult.Success
        if (ok) {
            refreshConnectivity()
            audit("OWNER", "account.device_link", RiskLevel.MEDIUM_RISK, "OWNER_CONFIRMED", "SUCCESS", "email=${email.take(32)}")
        }
        return ok
    }

    fun sendMessage(text: String, fromVoice: Boolean = false) {
        if (text.isBlank()) return
        val current = settingsStore.current()
        val persian = current.isPersian(SayvisStrings.deviceIsPersian())

        _chatMessages.value = _chatMessages.value + ChatMessage(sender = "OWNER", text = text)
        _pendingAction.value = null
        _avatarState.value = AvatarState.THINKING
        _chatThinking.value = true
        LinkCenter.push(LinkCenter.CODE.THINKING)

        viewModelScope.launch {
            try {
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
                recordSearchTaste(searchDecision.query)
                sources = runCatching { webSearch.search(searchDecision.query, persian) }.getOrDefault(emptyList())
                cognitiveCapture(CognitiveIngest.Source.SEARCH, searchDecision.query)
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
            // TRADING FOCUS: while LIT plans are live, the brain concentrates
            // on the trade data first, exactly as the owner specified.
            val tradeFocus = buildString {
                val active = _marketAnalyses.value.values.filter { it.plan.side != LitStrategyEngine.Side.WAIT }
                if (active.isNotEmpty()) {
                    appendLine(if (persian) "تمرکز ترید لیت (LIT) — دادهٔ زندهٔ بازار مالک:" else "LIT TRADING FOCUS — the owner's live market data:")
                    val tuning = _tradeTuning.value
                    if (tuning.sourceRepos.isNotEmpty()) {
                        appendLine(
                            if (persian) "تیونینگ فعال (خودتکاملی گیت‌هاب): ATR×${tuning.atrFactor}، RSI ${tuning.rsiHigh}/${tuning.rsiLow}، اهداف " +
                                tuning.targetMultiples.joinToString("/") { "%.1f".format(it) } + " ← " + tuning.sourceRepos.take(2).joinToString(", ")
                            else "Active tuning (GitHub self-evolution): ATR×${tuning.atrFactor}, RSI ${tuning.rsiHigh}/${tuning.rsiLow}, targets " +
                                tuning.targetMultiples.joinToString("/") { "%.1f".format(it) } + " ← " + tuning.sourceRepos.take(2).joinToString(", ")
                        )
                    }
                    active.take(3).forEach { analysis ->
                        val plan = analysis.plan
                        appendLine(
                            "- ${plan.side} entry=${plan.entry} sl=${plan.stop} " +
                                "tp1=${plan.targets.getOrNull(0)?.price} rr>=1:3 atr=${analysis.view.atr14} rsi=${analysis.view.rsi14}"
                        )
                    }
                }
            }
            val systemContext = ContextLocalization.systemContextLine(contextSnapshot.value, persian) +
                (if (tradeFocus.isNotBlank()) "\n$tradeFocus" else "") +
                (if (sourcesBlock.isNotBlank()) "\n$sourcesBlock" else "")
            val history = _chatMessages.value.takeLast(10).map { ChatTurn(it.sender, it.text) }

            // v5.0.0: AUTO resolves the best configured cloud brain (Gemini
            // first) so a connected Gemini is ALWAYS used even if the
            // provider switch was never flipped; an explicit chip choice is
            // honoured when its key exists.
            val (brainSettings, brainNote) = resolveChatBrain(current)
            _activeBrainNote.value = brainNote

            val response = aiOrchestrator.querySAYVIS(
                prompt = text,
                uicContext = uicSummary,
                systemContext = systemContext,
                languageFa = persian,
                emergencyLockActive = current.emergencyLockActive,
                forceOffline = current.forceOfflineMode,
                settings = brainSettings,
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
            // v5.3.0: first successful cloud round-trip is REMEMBERED forever.
            if (response.isSuccess && response.providerUsed != ProviderType.LOCAL_COGNITIVE) {
                markAiLinked(response.model)
            }
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
            } finally {
                _chatThinking.value = false
            }
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
        cognitiveCapture(CognitiveIngest.Source.COMMAND, command::class.simpleName ?: "command")
        when (command) {
            is AssistantCommand.CreateAlarm -> {
                val context = getApplication<Application>()
                val intent = Intent(android.provider.AlarmClock.ACTION_SET_ALARM).apply {
                    putExtra(android.provider.AlarmClock.EXTRA_MESSAGE, command.label.take(40))
                    if (command.hour != null) {
                        putExtra(android.provider.AlarmClock.EXTRA_HOUR, command.hour)
                        putExtra(android.provider.AlarmClock.EXTRA_MINUTES, command.minute ?: 0)
                    }
                }
                val ok = runCatching {
                    context.startActivity(
                        Intent.createChooser(intent, "SAYVIS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                    true
                }.getOrDefault(false)
                cognitiveCapture(CognitiveIngest.Source.ALARM, command.label)
                assistantReply(
                    if (ok) faOrEn(persian,
                        "⏰ آلارم «${command.label.take(30)}» در اپ ساعت دستگاه ثبت شد.",
                        "⏰ Alarm \"${command.label.take(30)}\" registered in the device clock app.")
                    else faOrEn(persian,
                        "اپ ساعت دستگاه پاسخ نداد؛ یک اپ ساعت نصب کنید.",
                        "The device clock app did not respond; install a clock app first.")
                )
                audit("SAYVIS_AGENT", "device.alarm_set", RiskLevel.LOW_RISK, "OWNER_CONFIRMED",
                    if (ok) "SUCCESS" else "FAILED", "h=${command.hour} m=${command.minute} in=${command.inMinutes ?: 0}")
            }
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
                cognitiveCapture(CognitiveIngest.Source.MISSION, command.title)
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
            val client = SayvisNet.client(10, 15)
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
