package com.example.sayvis.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sayvis.ai.AIOrchestrator
import com.example.sayvis.ai.AIResponse
import com.example.sayvis.ai.ProviderType
import com.example.sayvis.data.local.SayvisDatabase
import com.example.sayvis.data.repository.SayvisRepository
import com.example.sayvis.engine.AwareEngine
import com.example.sayvis.model.AuditEvent
import com.example.sayvis.model.AwareOpportunity
import com.example.sayvis.model.ContextSnapshot
import com.example.sayvis.model.Device
import com.example.sayvis.model.LifeDomain
import com.example.sayvis.model.LifeScenario
import com.example.sayvis.model.LitAnalysisSignal
import com.example.sayvis.model.LitSignalType
import com.example.sayvis.model.Mission
import com.example.sayvis.model.OpportunityStatus
import com.example.sayvis.model.PrivacyLevel
import com.example.sayvis.model.RiskLevel
import com.example.sayvis.model.TradingGateState
import com.example.sayvis.model.UicAttribute
import com.example.sayvis.model.UicCategory
import com.example.sayvis.model.UicStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class SayvisScreen(val titleEn: String, val titleFa: String) {
    HOME("Overview", "نمای کلی"),
    CHAT("SAYVIS AI", "هوش مصنوعی سایو"),
    UIC("Cognitive (UIC)", "مدل شناختی"),
    AWARE("AWARE Engine", "موتور ادراک AWARE"),
    MISSIONS("Missions", "مأموریت‌ها"),
    SECURITY("Security & Devices", "امنیت و دستگاه‌ها"),
    SIMULATION("Life & Trading", "شبیه‌سازی و تحلیل")
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
    private val aiOrchestrator = AIOrchestrator()
    val awareEngine = AwareEngine(repository)

    init {
        viewModelScope.launch {
            contextSnapshot.collect { snap ->
                awareEngine.onSystemStateChanged(
                    com.example.sayvis.model.SystemState(
                        batteryLevel = if (snap.cognitiveLoad == "Fatigue Risk") 18 else 85,
                        isCharging = false,
                        networkType = if (snap.networkStatus.contains("Offline")) "NONE" else "WIFI",
                        activeMissionsCount = snap.activeMissionsCount,
                        blockedTasksCount = snap.blockedTasksCount,
                        emergencyLockActive = snap.emergencyLockActive,
                        focusWindowActive = snap.focusWindow.contains("Deep Work", ignoreCase = true)
                    )
                )
            }
        }
    }

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
                text = "Welcome, Commander. SAYVIS Personal Operating Layer online.\nCognitive Model (UIC) initialized, Zero-Trust security active, 3 devices paired. How may I augment your agency today?",
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
            isOnline = !offlineForced
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

    // --- Chat & AI Interaction ---
    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val userMsg = ChatMessage(sender = "OWNER", text = text)
        _chatMessages.value = _chatMessages.value + userMsg

        _avatarState.value = AvatarState.THINKING

        viewModelScope.launch {
            val uicSummary = uicAttributes.value.joinToString("\n") {
                "- [${it.category.name}] ${it.title}: ${it.value} (Status: ${it.status.name}, Confidence: ${it.confidence})"
            }
            val sysSnapshot = contextSnapshot.value
            val sysContext = "Focus: ${sysSnapshot.focusWindow}, Load: ${sysSnapshot.cognitiveLoad}, Blocked: ${sysSnapshot.blockedTasksCount}, Lock: ${_emergencyLockActive.value}"

            val response = aiOrchestrator.querySAYVIS(
                prompt = text,
                uicContext = uicSummary,
                systemContext = sysContext,
                languageFa = _isPersian.value,
                emergencyLockActive = _emergencyLockActive.value,
                forceOffline = _forceOfflineMode.value
            )

            _avatarState.value = AvatarState.SPEAKING

            val aiMsg = ChatMessage(
                sender = "SAYVIS",
                text = response.text,
                providerUsed = response.providerUsed
            )
            _chatMessages.value = _chatMessages.value + aiMsg

            _avatarState.value = if (_emergencyLockActive.value) AvatarState.EMERGENCY_LOCKED else AvatarState.IDLE

            // Record audit
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
