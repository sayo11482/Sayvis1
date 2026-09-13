package com.example.sayvis.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sayvis.ai.AIOrchestrator
import com.example.sayvis.ai.AIResponse
import com.example.sayvis.ai.GeminiProvider
import com.example.sayvis.ai.OpenAiCompatibleProvider
import com.example.sayvis.ai.ProviderType
import com.example.sayvis.core.DeviceTelemetry
import com.example.sayvis.core.SettingsStore
import com.example.sayvis.core.TelemetryProvider
import com.example.sayvis.data.local.SayvisDatabase
import com.example.sayvis.data.repository.SayvisRepository
import com.example.sayvis.engine.AwareEngine
import com.example.sayvis.model.AuditEvent
import com.example.sayvis.model.AwareOpportunity
import com.example.sayvis.model.ContextSnapshot
import com.example.sayvis.model.Device
import com.example.sayvis.model.EpistemicStatus
import com.example.sayvis.model.LifeDomain
import com.example.sayvis.model.LifeScenario
import com.example.sayvis.model.LitAnalysisSignal
import com.example.sayvis.model.LitSignalType
import com.example.sayvis.model.MemoryItem
import com.example.sayvis.model.MemoryType
import com.example.sayvis.model.Mission
import com.example.sayvis.model.OpportunityStatus
import com.example.sayvis.model.PrivacyLevel
import com.example.sayvis.model.RetentionPolicy
import com.example.sayvis.model.RiskLevel
import com.example.sayvis.model.TradingGateState
import com.example.sayvis.model.UicAttribute
import com.example.sayvis.model.UicCategory
import com.example.sayvis.model.UicStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID

enum class SayvisScreen(val titleEn: String, val titleFa: String) {
    HOME("Overview", "نمای کلی"),
    CHAT("SAYVIS AI", "هوش مصنوعی سایو"),
    UIC("Cognitive (UIC)", "مدل شناختی"),
    AWARE("AWARE Engine", "موتور ادراک AWARE"),
    MISSIONS("Missions", "مأموریت‌ها"),
    SECURITY("Security & Devices", "امنیت و دستگاه‌ها"),
    SIMULATION("Life & Trading", "شبیه‌سازی و تحلیل"),
    MEMORY("Memory", "حافظه"),
    SETTINGS("Settings", "تنظیمات")
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

class SayvisViewModel(application: Application) : AndroidViewModel(application) {

    private val database = SayvisDatabase.getDatabase(application, viewModelScope)
    val repository = SayvisRepository(database)

    // Central multi-model AI core: Gemini + OpenRouter + Groq, with sovereign local fallback.
    // API keys are supplied at runtime from SettingsStore (in-app Settings screen).
    private val aiOrchestrator = AIOrchestrator(
        cloudProviders = listOf(
            GeminiProvider(),
            OpenAiCompatibleProvider(
                providerType = ProviderType.OPEN_ROUTER,
                baseUrl = "https://openrouter.ai/api/v1",
                defaultModel = SettingsStore.DEFAULT_OPENROUTER_MODEL,
                keyProvider = { SettingsStore.openRouterKey },
                modelProvider = { SettingsStore.openRouterModel }
            ),
            OpenAiCompatibleProvider(
                providerType = ProviderType.GROQ_ROUTER,
                baseUrl = "https://api.groq.com/openai/v1",
                defaultModel = SettingsStore.DEFAULT_GROQ_MODEL,
                keyProvider = { SettingsStore.groqKey },
                modelProvider = { SettingsStore.groqModel }
            )
        )
    )
    val awareEngine = AwareEngine(repository)

    // NOTE: The AWARE system-state listener previously lived in an `init` block near the
    // top of this class. Kotlin initializes properties and init blocks in textual order,
    // so `contextSnapshot` (declared further below) was still NULL when that block
    // collected it, which crashed the app on launch. All init blocks now live at the
    // BOTTOM of this class, after every state property.

    // Screen navigation
    private val _currentScreen = MutableStateFlow(SayvisScreen.HOME)
    val currentScreen: StateFlow<SayvisScreen> = _currentScreen.asStateFlow()

    // Global settings & toggles
    private val _isPersian = MutableStateFlow(false) // Toggle Persian/English
    val isPersian: StateFlow<Boolean> = _isPersian.asStateFlow()

    private val _emergencyLockActive = MutableStateFlow(false)
    val emergencyLockActive: StateFlow<Boolean> = _emergencyLockActive.asStateFlow()

    private val _forceOfflineMode = MutableStateFlow(false)
    val forceOfflineMode: StateFlow<Boolean> = _forceOfflineMode.asStateFlow()

    private val _avatarState = MutableStateFlow(AvatarState.IDLE)
    val avatarState: StateFlow<AvatarState> = _avatarState.asStateFlow()

    // Chat Conversation
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = "SAYVIS",
                text = "سلام فرمانده! من سایویس هستم 🌟\n\nهسته‌ی محلی من فعال است (آفلاین کار می‌کند).\nبرای فعال‌سازی مغز ابری (Gemini / OpenRouter / Groq) کلید API رایگان را در تب «تنظیمات» وارد کنید — آن موقع تحلیل واقعی، یادگیری و بازطراحی ظاهرم فعال می‌شود.\n\nHello, Commander! Local core online. Add an API key in the Settings tab to activate my cloud brain.",
                providerUsed = ProviderType.LOCAL_COGNITIVE
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    // Database reactive streams
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

    // Self-learning memory stream (everything SAYVIS learns from conversations)
    val memories: StateFlow<List<MemoryItem>> = repository.allMemories.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    // Live hardware telemetry (real battery / network / storage / device info)
    private val _telemetry = MutableStateFlow(TelemetryProvider.snapshot(application))
    val telemetry: StateFlow<DeviceTelemetry> = _telemetry.asStateFlow()

    // Pending file attachment for the next AI message (AI file analysis)
    private val _pendingAttachment = MutableStateFlow<String?>(null)
    val pendingAttachment: StateFlow<String?> = _pendingAttachment.asStateFlow()

    // Runtime UI accent override applied by the AI itself (self-restyle protocol)
    private val _runtimeAccent = MutableStateFlow<Long?>(null)
    val runtimeAccent: StateFlow<Long?> = _runtimeAccent.asStateFlow()

    // Cloud connection test feedback
    private val _connectionTestStatus = MutableStateFlow<String?>(null)
    val connectionTestStatus: StateFlow<String?> = _connectionTestStatus.asStateFlow()

    // LIT Trading State (strictly analysis first, live execution blocked)
    private val _tradingGate = MutableStateFlow(TradingGateState())
    val tradingGate: StateFlow<TradingGateState> = _tradingGate.asStateFlow()

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

    // Life Simulation Scenarios
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

    // Live combined context snapshot
    val contextSnapshot: StateFlow<ContextSnapshot> = combine(
        missions,
        emergencyLockActive,
        forceOfflineMode
    ) { missionList, lockActive, offlineForced ->
        val blockedCount = missionList.flatMap { it.tasks }.count { it.isBlocked }
        repository.getContextSnapshot(
            activeMissionsCount = missionList.count { it.status == com.example.sayvis.model.MissionStatus.ACTIVE },
            blockedTasksCount = blockedCount,
            emergencyLockActive = lockActive,
            isOnline = !offlineForced && _telemetry.value.isOnline
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ContextSnapshot(
            focusWindow = "Deep Work Window",
            currentActivity = "System Booting",
            cognitiveLoad = "Optimal",
            activeMissionsCount = 2,
            blockedTasksCount = 1,
            networkStatus = "Online"
        )
    )

    // NOTE: All init blocks MUST stay AFTER every state property declaration above.
    // Kotlin runs property initializers and init blocks top-to-bottom; collecting a
    // StateFlow before it is assigned throws an NPE at app launch.
    init {
        SettingsStore.init(getApplication())

        // Feed REAL device telemetry + context into the AWARE engine
        viewModelScope.launch {
            contextSnapshot.collect { snap ->
                val t = _telemetry.value
                awareEngine.onSystemStateChanged(
                    com.example.sayvis.model.SystemState(
                        batteryLevel = t.batteryLevel,
                        isCharging = t.isCharging,
                        networkType = t.networkType,
                        activeMissionsCount = snap.activeMissionsCount,
                        blockedTasksCount = snap.blockedTasksCount,
                        emergencyLockActive = snap.emergencyLockActive,
                        focusWindowActive = snap.focusWindow.contains("Deep Work", ignoreCase = true)
                    )
                )
            }
        }

        // Periodically refresh real hardware telemetry (battery, network, storage)
        viewModelScope.launch {
            while (true) {
                delay(30_000L)
                _telemetry.value = TelemetryProvider.snapshot(getApplication())
            }
        }
    }

    fun navigateTo(screen: SayvisScreen) {
        _currentScreen.value = screen
    }

    fun toggleLanguage() {
        _isPersian.value = !_isPersian.value
    }

    fun toggleOfflineMode() {
        _forceOfflineMode.value = !_forceOfflineMode.value
        updateAvatarState()
    }

    fun toggleEmergencyLock() {
        val newState = !_emergencyLockActive.value
        _emergencyLockActive.value = newState
        viewModelScope.launch {
            repository.recordAuditEvent(
                actor = "OWNER",
                action = if (newState) "security.emergency_lock.engage" else "security.emergency_lock.disengage",
                riskLevel = RiskLevel.CRITICAL,
                auth = "OWNER_BIOMETRIC_CONFIRMED",
                result = "SUCCESS",
                digest = "Emergency lock state updated to: $newState"
            )
        }
        updateAvatarState()
    }

    private fun updateAvatarState() {
        when {
            _emergencyLockActive.value -> _avatarState.value = AvatarState.EMERGENCY_LOCKED
            _forceOfflineMode.value -> _avatarState.value = AvatarState.OFFLINE
            else -> _avatarState.value = AvatarState.IDLE
        }
    }

    // --- Chat & AI Interaction (multi-model, memory-aware, file-aware) ---
    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val attachment = _pendingAttachment.value
        val userMsg = ChatMessage(
            sender = "OWNER",
            text = if (attachment != null) "$text 📎" else text
        )
        _chatMessages.value = _chatMessages.value + userMsg
        _pendingAttachment.value = null

        _avatarState.value = AvatarState.THINKING

        viewModelScope.launch {
            // 1. Cognitive context: UIC attributes + self-learned memories
            val uicSummary = uicAttributes.value.joinToString("\n") {
                "- [${it.category.name}] ${it.title}: ${it.value} (Status: ${it.status.name}, Confidence: ${it.confidence})"
            }.ifBlank { "No UIC attributes recorded yet." }

            val recentMemories = memories.value
                .filter { it.type == MemoryType.INTERACTION_HISTORY || it.type == MemoryType.LONG_TERM_MEMORY }
                .takeLast(10)
                .joinToString("\n") { "- ${it.content.take(400)}" }

            val uicContext = if (recentMemories.isBlank()) {
                uicSummary
            } else {
                "$uicSummary\nRECENT OWNER MEMORY (self-learned):\n$recentMemories"
            }

            // 2. Real device telemetry + live system state
            val t = _telemetry.value
            val snap = contextSnapshot.value
            val systemContext = buildString {
                appendLine("Device: ${t.deviceModel}, ${t.androidVersion}")
                appendLine("Battery: ${t.batteryLevel}% (charging: ${t.isCharging})")
                appendLine("Network: ${t.networkType} (online: ${t.isOnline})")
                appendLine("Storage: ${"%.1f".format(t.storageFreeGb)} GB free of ${"%.1f".format(t.storageTotalGb)} GB")
                t.ramTotalGb?.let { appendLine("RAM total: ${"%.1f".format(it)} GB") }
                appendLine("Focus window: ${snap.focusWindow}; Cognitive load: ${snap.cognitiveLoad}")
                appendLine("Active missions: ${snap.activeMissionsCount}; Blocked tasks: ${snap.blockedTasksCount}")
                appendLine("Emergency lock: ${_emergencyLockActive.value}; Force offline: ${_forceOfflineMode.value}")
            }.trim()

            // 3. Include the attached file (if any) in the prompt
            val prompt = if (attachment != null) "$attachment\n\nOwner message: $text" else text

            val response = aiOrchestrator.querySAYVIS(
                prompt = prompt,
                uicContext = uicContext,
                systemContext = systemContext,
                languageFa = _isPersian.value,
                emergencyLockActive = _emergencyLockActive.value,
                forceOffline = _forceOfflineMode.value
            )

            // 4. Apply any AI self-restyle directive, show the cleaned text
            val displayText = applyUiDirective(response.text)

            _avatarState.value = AvatarState.SPEAKING

            val aiMsg = ChatMessage(
                sender = "SAYVIS",
                text = displayText,
                providerUsed = response.providerUsed
            )
            _chatMessages.value = _chatMessages.value + aiMsg

            _avatarState.value = if (_emergencyLockActive.value) AvatarState.EMERGENCY_LOCKED else AvatarState.IDLE

            // 5. Self-learning: persist this interaction into long-term memory
            repository.addMemory(
                MemoryItem(
                    id = "mem_" + UUID.randomUUID().toString().take(10),
                    type = MemoryType.INTERACTION_HISTORY,
                    content = "Owner: ${text.take(400)} → SAYVIS: ${displayText.take(600)}",
                    source = response.providerUsed.name,
                    confidence = 0.9f,
                    provenance = "SELF_LEARNED_INTERACTION",
                    importance = 5,
                    retentionPolicy = RetentionPolicy.PERSISTENT,
                    epistemicStatus = EpistemicStatus.OBSERVED
                )
            )

            // 6. Audit
            repository.recordAuditEvent(
                actor = "SAYVIS_AGENT",
                action = "ai.query.respond",
                riskLevel = RiskLevel.LOW_RISK,
                auth = "SESSION_VALIDATED",
                result = "SUCCESS",
                digest = "Provider: ${response.providerUsed.displayName} (Prompt: ${text.take(30)}...)"
            )
        }
    }

    // --- AI self-restyle: parse ```sayvis-ui {"accent_color":..,"language":..}``` ---
    private fun applyUiDirective(raw: String): String {
        return try {
            val regex = Regex("```sayvis-ui\\s*([\\s\\S]*?)```", RegexOption.IGNORE_CASE)
            val match = regex.find(raw) ?: return raw
            val cleaned = raw.replace(match.value, "").trim()
            runCatching {
                val json = JSONObject(match.groupValues[1].trim())
                val hex = json.optString("accent_color").trim()
                if (hex.matches(Regex("^#[0-9a-fA-F]{6}$"))) {
                    _runtimeAccent.value = android.graphics.Color.parseColor(hex).toLong() and 0xFFFFFFFFL
                }
                when (json.optString("language").trim().lowercase()) {
                    "fa" -> _isPersian.value = true
                    "en" -> _isPersian.value = false
                }
            }
            cleaned
        } catch (_: Throwable) {
            raw
        }
    }

    // --- File attachment (AI file analysis) ---
    fun attachFile(uri: Uri) {
        viewModelScope.launch {
            _pendingAttachment.value = readFileContext(getApplication(), uri)
        }
    }

    fun clearAttachment() {
        _pendingAttachment.value = null
    }

    private suspend fun readFileContext(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            var displayName = "file"
            var sizeBytes = 0L
            runCatching {
                resolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        val sizeIdx = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                        if (nameIdx >= 0) displayName = cursor.getString(nameIdx) ?: "file"
                        if (sizeIdx >= 0) sizeBytes = cursor.getLong(sizeIdx)
                    }
                }
            }
            val mime = resolver.getType(uri)
            val extension = displayName.substringAfterLast('.', "").lowercase()
            val isProbablyText = mime?.startsWith("text/") == true ||
                extension in setOf(
                    "txt", "md", "json", "xml", "csv", "kt", "java", "py", "js", "ts",
                    "html", "css", "log", "yml", "yaml", "sql", "sh", "c", "cpp"
                )
            val content = if (isProbablyText && sizeBytes < 512_000L) {
                resolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText().take(40_000) }
                    ?: "(could not open file stream)"
            } else {
                "(binary or large file - only metadata is available for analysis)"
            }
            "[ATTACHED FILE: $displayName | size: ${sizeBytes / 1024} KB | type: ${mime ?: extension.ifBlank { "unknown" }}]\n$content"
        }.getOrElse { e ->
            "[ATTACHED FILE ERROR: ${e.message ?: "unreadable"}]"
        }
    }

    // --- Runtime settings (stored on-device via SettingsStore) ---
    fun setProviderChoice(choice: String) { SettingsStore.providerChoice = choice }
    fun setGeminiKey(value: String) { SettingsStore.geminiKey = value }
    fun setOpenRouterKey(value: String) { SettingsStore.openRouterKey = value }
    fun setGroqKey(value: String) { SettingsStore.groqKey = value }
    fun setGeminiModel(value: String) { SettingsStore.geminiModel = value }
    fun setOpenRouterModel(value: String) { SettingsStore.openRouterModel = value }
    fun setGroqModel(value: String) { SettingsStore.groqModel = value }

    fun cloudStatusText(): String {
        val configured = mutableListOf<String>()
        if (SettingsStore.geminiKey.isNotBlank()) configured += "Gemini"
        if (SettingsStore.openRouterKey.isNotBlank()) configured += "OpenRouter"
        if (SettingsStore.groqKey.isNotBlank()) configured += "Groq"
        return if (configured.isEmpty()) {
            "LOCAL BRAIN • add an API key in Settings"
        } else {
            "ONLINE • " + configured.joinToString(" + ")
        }
    }

    fun testCloudConnection() {
        viewModelScope.launch {
            if (!SettingsStore.anyCloudConfigured()) {
                _connectionTestStatus.value = "⚠️ No API key configured. Paste at least one key above."
                return@launch
            }
            _connectionTestStatus.value = "⏳ Testing connection..."
            val result = runCatching {
                aiOrchestrator.querySAYVIS(
                    prompt = "Connection test. Reply with exactly: OK",
                    uicContext = "test",
                    systemContext = "connection test",
                    languageFa = false,
                    emergencyLockActive = false,
                    forceOffline = false
                )
            }
            _connectionTestStatus.value = result.fold(
                onSuccess = { "✅ ${it.providerUsed.displayName} responded: ${it.text.take(60)}" },
                onFailure = { "❌ Failed: ${it.message?.take(160) ?: "unknown error"}" }
            )
        }
    }

    // --- Memory management (owner sovereignty over learned data) ---
    fun deleteMemory(id: String) {
        viewModelScope.launch { repository.deleteMemory(id) }
    }

    fun clearAllMemories() {
        viewModelScope.launch { repository.clearMemories() }
    }

    // --- UIC Actions ---
    fun confirmUicAttribute(id: String) {
        viewModelScope.launch {
            repository.updateUicStatus(id, UicStatus.CONFIRMED)
        }
    }

    fun revokeUicAttribute(id: String) {
        viewModelScope.launch {
            repository.updateUicStatus(id, UicStatus.REVOKED)
        }
    }

    fun addCustomUicAttribute(category: UicCategory, title: String, key: String, value: String) {
        val attr = UicAttribute(
            id = "uic_custom_" + UUID.randomUUID().toString().take(6),
            category = category,
            key = key,
            title = title,
            value = value,
            provenance = "Explicit Owner Entry",
            confidence = 1.0f,
            status = UicStatus.CONFIRMED,
            privacyLevel = PrivacyLevel.STANDARD
        )
        viewModelScope.launch {
            repository.addUicAttribute(attr)
        }
    }

    fun deleteUicAttribute(id: String) {
        viewModelScope.launch {
            repository.deleteUicAttribute(id)
        }
    }

    // --- AWARE Opportunities Actions ---
    fun approveOpportunity(opportunityId: String) {
        viewModelScope.launch {
            val success = repository.approveAndExecuteOpportunity(opportunityId, _emergencyLockActive.value)
            if (!success) {
                // Post warning to chat
                _chatMessages.value = _chatMessages.value + ChatMessage(
                    sender = "SAYVIS",
                    text = "⚠️ Execution blocked: Emergency Lock is active. Disengage emergency lock in Security settings to permit action execution."
                )
            }
        }
    }

    fun dismissOpportunity(opportunityId: String) {
        viewModelScope.launch {
            repository.dismissOpportunity(opportunityId)
        }
    }

    fun runAwareScan() {
        viewModelScope.launch {
            val generated = awareEngine.evaluateContextAndTriggerProactiveInterventions(
                snapshot = contextSnapshot.value,
                missions = missions.value
            )
            if (generated.isNotEmpty()) {
                _avatarState.value = AvatarState.OPPORTUNITY_AWARE
            }
        }
    }

    // --- Missions Actions ---
    fun toggleMissionTask(missionId: String, taskId: String, currentCompleted: Boolean) {
        viewModelScope.launch {
            repository.updateTaskCompletion(missionId, taskId, !currentCompleted)
        }
    }

    fun addMission(mission: Mission) {
        viewModelScope.launch {
            repository.addMission(mission)
        }
    }

    // --- Device Management Actions ---
    fun toggleDeviceTrust(deviceId: String, currentTrust: Boolean) {
        viewModelScope.launch {
            repository.toggleDeviceTrust(deviceId, currentTrust)
        }
    }

    fun revokeDevice(deviceId: String) {
        viewModelScope.launch {
            repository.revokeDevice(deviceId)
        }
    }
}
