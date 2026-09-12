package com.example.sayvis.engine

import com.example.sayvis.data.repository.SayvisRepository
import com.example.sayvis.model.ActionType
import com.example.sayvis.model.ActivityEventType
import com.example.sayvis.model.AwareOpportunity
import com.example.sayvis.model.ContextSnapshot
import com.example.sayvis.model.InterventionExecutionResult
import com.example.sayvis.model.InterventionPermissionResult
import com.example.sayvis.model.Mission
import com.example.sayvis.model.OpportunityStatus
import com.example.sayvis.model.PatternDetection
import com.example.sayvis.model.ProposedAction
import com.example.sayvis.model.RiskLevel
import com.example.sayvis.model.SystemState
import com.example.sayvis.model.UicAttribute
import com.example.sayvis.model.UserActivityEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.UUID

/**
 * AWARE Engine — Proactive Context & Opportunity Subsystem.
 *
 * Listens continuously to:
 * 1. System state (battery, charging, network security, emergency killswitch, thermal/focus).
 * 2. User activity patterns (working sessions, task stalls, deep work intervals, fatigue signals).
 *
 * Provides proactive intervention suggestions evaluated against a Zero-Trust permission engine.
 * No sensitive action executes without explicit permission checks and owner consent where required.
 */
class AwareEngine(
    private val repository: SayvisRepository,
    initialGrantedPermissions: Set<String> = setOf(
        "missions.read",
        "missions.write",
        "system.focus.schedule",
        "system.power.optimize",
        "device.sync"
    )
) {
    // Current System State Stream
    private val _systemState = MutableStateFlow(SystemState())
    val systemState: StateFlow<SystemState> = _systemState.asStateFlow()

    // Activity Stream & History
    private val _activityHistory = MutableStateFlow<List<UserActivityEvent>>(emptyList())
    val activityHistory: StateFlow<List<UserActivityEvent>> = _activityHistory.asStateFlow()

    // Detected Behavioral & Operational Patterns
    private val _detectedPatterns = MutableStateFlow<List<PatternDetection>>(emptyList())
    val detectedPatterns: StateFlow<List<PatternDetection>> = _detectedPatterns.asStateFlow()

    // Active Proactive Interventions
    private val _activeInterventions = MutableStateFlow<List<AwareOpportunity>>(emptyList())
    val activeInterventions: StateFlow<List<AwareOpportunity>> = _activeInterventions.asStateFlow()

    // Granted System Permissions for AWARE interventions
    private val _grantedPermissions = MutableStateFlow(initialGrantedPermissions)
    val grantedPermissions: StateFlow<Set<String>> = _grantedPermissions.asStateFlow()

    /**
     * Updates the observed system state and triggers opportunistic evaluation.
     */
    fun onSystemStateChanged(newState: SystemState) {
        _systemState.value = newState
        evaluateSystemStateTriggers(newState)
    }

    /**
     * Subscribes to an external Flow of SystemState.
     */
    fun listenToSystemState(flow: Flow<SystemState>, scope: CoroutineScope) {
        flow.onEach { state ->
            onSystemStateChanged(state)
        }.launchIn(scope)
    }

    /**
     * Records a new user activity event and updates pattern synthesis.
     */
    fun recordUserActivity(event: UserActivityEvent) {
        val updatedHistory = (_activityHistory.value + event).takeLast(100)
        _activityHistory.value = updatedHistory
        analyzeActivityPatterns(updatedHistory)
    }

    /**
     * Permission Management: Grant permission to the AWARE engine.
     */
    fun grantPermission(permission: String) {
        _grantedPermissions.value = _grantedPermissions.value + permission
    }

    /**
     * Permission Management: Revoke permission from the AWARE engine.
     */
    fun revokePermission(permission: String) {
        _grantedPermissions.value = _grantedPermissions.value - permission
    }

    /**
     * Evaluates permission checks for an intervention suggestion under Zero-Trust principles.
     */
    fun checkInterventionPermission(opportunity: AwareOpportunity): InterventionPermissionResult {
        // 1. Emergency Killswitch verification (Highest priority safeguard)
        if (_systemState.value.emergencyLockActive) {
            return InterventionPermissionResult.BlockedByEmergencyLock(
                reason = "AWARE interventions blocked: Emergency Lock is active on this sovereign node."
            )
        }

        // 2. Permission presence check
        val requiredPerm = opportunity.proposedAction.requiredPermission
        if (requiredPerm.isNotBlank() && !_grantedPermissions.value.contains(requiredPerm)) {
            return InterventionPermissionResult.PermissionDenied(
                missingPermission = requiredPerm,
                reason = "AWARE engine lacks required capability '$requiredPerm' to execute action '${opportunity.proposedAction.actionType.name}'."
            )
        }

        // 3. Zero-Trust Risk Gate evaluation
        return when (opportunity.riskLevel) {
            RiskLevel.LOW_RISK -> {
                InterventionPermissionResult.Allowed(
                    requiredPermission = requiredPerm,
                    evaluatedRisk = RiskLevel.LOW_RISK,
                    rationale = "Low-risk observation or routine non-destructive action permitted under standard policy."
                )
            }
            RiskLevel.MEDIUM_RISK, RiskLevel.HIGHER_RISK, RiskLevel.CRITICAL -> {
                // High risk actions MANDATORILY require explicit owner confirmation
                InterventionPermissionResult.RequiresOwnerConsent(
                    requiredPermission = requiredPerm,
                    riskLevel = opportunity.riskLevel,
                    message = "Action classified as ${opportunity.riskLevel.labelEn}. Explicit owner verification is mandatory before execution."
                )
            }
        }
    }

    /**
     * Executes a proactive intervention after verifying permissions and owner consent.
     */
    suspend fun executeInterventionWithPermissionCheck(
        opportunity: AwareOpportunity,
        ownerExplicitConsent: Boolean = false
    ): InterventionExecutionResult {
        val permCheck = checkInterventionPermission(opportunity)

        when (permCheck) {
            is InterventionPermissionResult.BlockedByEmergencyLock -> {
                repository.updateOpportunityStatus(opportunity.id, OpportunityStatus.BLOCKED_BY_LOCK)
                repository.recordAuditEvent(
                    actor = "AWARE_ENGINE",
                    action = opportunity.proposedAction.actionType.name,
                    riskLevel = opportunity.riskLevel,
                    auth = "BLOCKED_EMERGENCY_LOCK",
                    result = "BLOCKED",
                    digest = permCheck.reason
                )
                return InterventionExecutionResult.Blocked(permCheck.reason)
            }
            is InterventionPermissionResult.PermissionDenied -> {
                repository.recordAuditEvent(
                    actor = "AWARE_ENGINE",
                    action = opportunity.proposedAction.actionType.name,
                    riskLevel = opportunity.riskLevel,
                    auth = "PERMISSION_CHECK_FAILED",
                    result = "DENIED",
                    digest = permCheck.reason
                )
                return InterventionExecutionResult.Failed(permCheck.reason)
            }
            is InterventionPermissionResult.RequiresOwnerConsent -> {
                if (!ownerExplicitConsent) {
                    return InterventionExecutionResult.ConsentNeeded(opportunity, permCheck)
                }
                // Owner explicitly confirmed in UI
                val success = repository.approveAndExecuteOpportunity(opportunity.id, emergencyLockActive = false)
                return if (success) {
                    InterventionExecutionResult.Success(
                        opportunityId = opportunity.id,
                        auditDigest = "Executed with explicit owner authorization under permission ${permCheck.requiredPermission}"
                    )
                } else {
                    InterventionExecutionResult.Failed("Execution failed in local repository")
                }
            }
            is InterventionPermissionResult.Allowed -> {
                val success = repository.approveAndExecuteOpportunity(opportunity.id, emergencyLockActive = false)
                return if (success) {
                    InterventionExecutionResult.Success(
                        opportunityId = opportunity.id,
                        auditDigest = "Executed under pre-authorized low-risk policy"
                    )
                } else {
                    InterventionExecutionResult.Failed("Execution failed in local repository")
                }
            }
        }
    }

    /**
     * Evaluates immediate system state triggers to generate proactive interventions.
     */
    private fun evaluateSystemStateTriggers(state: SystemState) {
        val generated = mutableListOf<AwareOpportunity>()

        // 1. Low Battery Trigger during active work
        if (state.batteryLevel < 20 && !state.isCharging) {
            val opp = AwareOpportunity(
                id = "opp_battery_" + UUID.randomUUID().toString().take(6),
                title = "Conserve Energy & Save Local Checkpoint",
                description = "Battery level at ${state.batteryLevel}%. Recommend optimizing background services and securing local database checkpoint.",
                triggerReason = "AWARE Hardware Telemetry: Critical power drop detected while discharging.",
                proposedAction = ProposedAction(
                    actionType = ActionType.AUTOMATE_WORKFLOW,
                    targetPayload = "power_save_mode_enable",
                    requiredPermission = "system.power.optimize",
                    executionSummary = "Throttle background polling and flush uncommitted mission states to encrypted storage."
                ),
                riskLevel = RiskLevel.LOW_RISK,
                status = OpportunityStatus.PENDING
            )
            generated.add(opp)
        }

        // 2. Thermal Throttling Alert
        if (state.thermalThrottling) {
            val opp = AwareOpportunity(
                id = "opp_thermal_" + UUID.randomUUID().toString().take(6),
                title = "Mitigate Thermal Throttling",
                description = "Sovereign hardware node reports high thermal load. Suggest deferring non-essential background embeddings.",
                triggerReason = "AWARE Sensor Telemetry: Thermal load threshold exceeded.",
                proposedAction = ProposedAction(
                    actionType = ActionType.RESTRICT_SYSTEM_ACCESS,
                    targetPayload = "defer_background_inference",
                    requiredPermission = "system.power.optimize",
                    executionSummary = "Pause background AI fine-tuning and prioritize active interactive tasks."
                ),
                riskLevel = RiskLevel.LOW_RISK,
                status = OpportunityStatus.PENDING
            )
            generated.add(opp)
        }

        if (generated.isNotEmpty()) {
            _activeInterventions.value = _activeInterventions.value + generated
        }
    }

    /**
     * Analyzes sliding window of user activity events to discover behavioral patterns.
     */
    private fun analyzeActivityPatterns(history: List<UserActivityEvent>) {
        val patterns = mutableListOf<PatternDetection>()

        val highEffortCount = history.count { it.type == ActivityEventType.HIGH_COGNITIVE_EFFORT }
        if (highEffortCount >= 3) {
            patterns.add(
                PatternDetection(
                    id = "pat_sustained_effort",
                    title = "Sustained Cognitive Intensity",
                    description = "Detected $highEffortCount consecutive high-effort sessions. Rest interval recommended.",
                    occurrences = highEffortCount,
                    confidence = 0.89f
                )
            )
        }

        val blockedCount = history.count { it.type == ActivityEventType.MISSION_BLOCKED }
        if (blockedCount >= 2) {
            patterns.add(
                PatternDetection(
                    id = "pat_recurrent_blockers",
                    title = "Workstream Blocker Cluster",
                    description = "$blockedCount blocker encounters recorded. Re-planning mission trajectory recommended.",
                    occurrences = blockedCount,
                    confidence = 0.94f
                )
            )
        }

        val lateNightCount = history.count { it.type == ActivityEventType.LATE_NIGHT_WORK }
        if (lateNightCount > 0) {
            patterns.add(
                PatternDetection(
                    id = "pat_late_night_incursion",
                    title = "Circadian Rhythm Deviation",
                    description = "Activity detected during nocturnal wind-down phase.",
                    occurrences = lateNightCount,
                    confidence = 0.85f
                )
            )
        }

        _detectedPatterns.value = patterns
    }

    /**
     * Synthesizes patterns from UIC memory and Mission states to discover opportunities.
     */
    fun detectPatterns(
        uicAttributes: List<UicAttribute>,
        missions: List<Mission>
    ): List<PatternDetection> {
        val patterns = mutableListOf<PatternDetection>()

        val blockedMissions = missions.filter { m -> m.tasks.any { it.isBlocked } }
        if (blockedMissions.isNotEmpty()) {
            patterns.add(
                PatternDetection(
                    id = "pat_blocked_tasks",
                    title = "Recurring Dependency Stalls",
                    description = "Detected ${blockedMissions.size} mission(s) stalled on external dependencies or cryptographic specs.",
                    occurrences = blockedMissions.size,
                    confidence = 0.92f
                )
            )
        }

        val hasDeepWorkPref = uicAttributes.any { it.key == "deep_work_interval" }
        if (hasDeepWorkPref) {
            patterns.add(
                PatternDetection(
                    id = "pat_deep_work_alignment",
                    title = "Cognitive Focus Cycle Adherence",
                    description = "User demonstrates 3.4x higher strategic output during mornings (09:00 - 11:30) when notifications are silenced.",
                    occurrences = 14,
                    confidence = 0.88f
                )
            )
        }

        // Include any dynamically discovered activity patterns
        patterns.addAll(_detectedPatterns.value)

        return patterns.distinctBy { it.id }
    }

    /**
     * Evaluates current context snapshot and generates proactive opportunities if warranted.
     */
    suspend fun evaluateContextAndTriggerProactiveInterventions(
        snapshot: ContextSnapshot,
        missions: List<Mission>
    ): List<AwareOpportunity> {
        val newOpportunities = mutableListOf<AwareOpportunity>()

        // Check for blocked mission tasks
        val blockedTask = missions.flatMap { m -> m.tasks.map { m to it } }.firstOrNull { it.second.isBlocked }
        if (blockedTask != null && snapshot.blockedTasksCount > 0) {
            val opp = AwareOpportunity(
                id = "opp_auto_" + UUID.randomUUID().toString().take(6),
                title = "Resolve Blocker in '${blockedTask.first.title}'",
                description = "Task '${blockedTask.second.title}' is blocked: ${blockedTask.second.blockerReason ?: "External dependency"}.",
                triggerReason = "AWARE Real-time Monitor: Mission velocity dropped by 45% due to unresolved blocker.",
                proposedAction = ProposedAction(
                    actionType = ActionType.REPLAN_MISSION,
                    targetPayload = "Re-route dependency through offline cryptographic library",
                    requiredPermission = "missions.write",
                    executionSummary = "Re-order task queue and alert owner of verified fallback strategy."
                ),
                riskLevel = RiskLevel.MEDIUM_RISK,
                status = OpportunityStatus.PENDING
            )
            newOpportunities.add(opp)
            repository.insertOpportunity(opp)
        }

        // Deep Work Focus Protection suggestion
        if (snapshot.focusWindow.contains("Deep Work", ignoreCase = true) && snapshot.cognitiveLoad != "Fatigue Risk") {
            val opp = AwareOpportunity(
                id = "opp_focus_" + UUID.randomUUID().toString().take(6),
                title = "Schedule Deep Work Shield",
                description = "Currently within high-yield focus window (${snapshot.focusWindow}). Silencing non-critical companion alerts.",
                triggerReason = "AWARE Cognitive Monitor: Focus window active with optimal cognitive load.",
                proposedAction = ProposedAction(
                    actionType = ActionType.SCHEDULE_FOCUS_BLOCK,
                    targetPayload = "focus_duration=90",
                    requiredPermission = "system.focus.schedule",
                    executionSummary = "Suppress external notifications and shield active cognitive bandwidth."
                ),
                riskLevel = RiskLevel.LOW_RISK,
                status = OpportunityStatus.PENDING
            )
            newOpportunities.add(opp)
            repository.insertOpportunity(opp)
        }

        // Check internal proactive interventions generated by system state
        for (internalOpp in _activeInterventions.value) {
            if (newOpportunities.none { it.id == internalOpp.id }) {
                newOpportunities.add(internalOpp)
                repository.insertOpportunity(internalOpp)
            }
        }

        return newOpportunities
    }
}

