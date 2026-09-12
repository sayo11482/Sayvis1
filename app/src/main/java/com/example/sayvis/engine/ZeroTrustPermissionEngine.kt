package com.example.sayvis.engine

import com.example.sayvis.data.repository.SayvisRepository
import com.example.sayvis.model.AuditEvent
import com.example.sayvis.model.AwareOpportunity
import com.example.sayvis.model.OpportunityStatus
import com.example.sayvis.model.RiskLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

sealed class PermissionDecision {
    data class Allowed(val auditEvent: AuditEvent) : PermissionDecision()
    data class RequiresConfirmation(val requiredConsentLevel: String, val message: String) : PermissionDecision()
    data class Blocked(val reason: String, val auditEvent: AuditEvent) : PermissionDecision()
}

class ZeroTrustPermissionEngine(
    private val repository: SayvisRepository
) {
    private val _emergencyLockActive = MutableStateFlow(false)
    val emergencyLockActive: StateFlow<Boolean> = _emergencyLockActive.asStateFlow()

    suspend fun setEmergencyLock(active: Boolean, reason: String = "User manual trigger") {
        _emergencyLockActive.value = active
        repository.recordAuditEvent(
            AuditEvent(
                eventId = UUID.randomUUID().toString(),
                actor = "SECURITY_MANAGER",
                deviceId = "DEV_ANDROID_LOCAL",
                action = if (active) "EMERGENCY_LOCK_ENGAGED" else "EMERGENCY_LOCK_RELEASED",
                riskLevel = RiskLevel.CRITICAL,
                authorization = "OWNER_EXPLICIT",
                timestamp = System.currentTimeMillis(),
                result = if (active) "LOCKED" else "UNLOCKED",
                payloadDigest = reason
            )
        )
    }

    suspend fun evaluateAction(opportunity: AwareOpportunity): PermissionDecision {
        // 1. Emergency Lock check - Highest priority Zero-Trust safeguard
        if (_emergencyLockActive.value) {
            val event = AuditEvent(
                eventId = UUID.randomUUID().toString(),
                actor = "ZERO_TRUST_ENGINE",
                deviceId = "DEV_ANDROID_LOCAL",
                action = opportunity.proposedAction.actionType.name,
                riskLevel = opportunity.riskLevel,
                authorization = "BLOCKED_BY_EMERGENCY_LOCK",
                timestamp = System.currentTimeMillis(),
                result = "BLOCKED",
                payloadDigest = opportunity.proposedAction.targetPayload
            )
            repository.recordAuditEvent(event)
            repository.updateOpportunityStatus(opportunity.id, OpportunityStatus.BLOCKED_BY_LOCK)
            return PermissionDecision.Blocked("Emergency Lock is ACTIVE. All proactive actions and tools are strictly blocked.", event)
        }

        // 2. Risk evaluation according to Zero-Trust Action Model
        return when (opportunity.riskLevel) {
            RiskLevel.LOW_RISK -> {
                // Low risk actions can proceed with standard preview/approval
                val event = AuditEvent(
                    eventId = UUID.randomUUID().toString(),
                    actor = "AWARE_ENGINE",
                    deviceId = "DEV_ANDROID_LOCAL",
                    action = opportunity.proposedAction.actionType.name,
                    riskLevel = RiskLevel.LOW_RISK,
                    authorization = "POLICY_APPROVED_LOW_RISK",
                    timestamp = System.currentTimeMillis(),
                    result = "SUCCESS",
                    payloadDigest = opportunity.proposedAction.targetPayload
                )
                repository.recordAuditEvent(event)
                repository.updateOpportunityStatus(opportunity.id, OpportunityStatus.EXECUTED)
                PermissionDecision.Allowed(event)
            }
            RiskLevel.MEDIUM_RISK, RiskLevel.HIGHER_RISK, RiskLevel.CRITICAL -> {
                // Medium/High/Critical actions MANDATORILY require explicit owner consent dialog
                PermissionDecision.RequiresConfirmation(
                    requiredConsentLevel = "OWNER_EXPLICIT_CONFIRMATION",
                    message = "Action classified as ${opportunity.riskLevel.labelEn}. Explicit owner authorization is required before execution."
                )
            }
        }
    }

    suspend fun executeConfirmedAction(opportunity: AwareOpportunity): AuditEvent {
        if (_emergencyLockActive.value) {
            val event = AuditEvent(
                eventId = UUID.randomUUID().toString(),
                actor = "ZERO_TRUST_ENGINE",
                deviceId = "DEV_ANDROID_LOCAL",
                action = opportunity.proposedAction.actionType.name,
                riskLevel = opportunity.riskLevel,
                authorization = "BLOCKED_BY_EMERGENCY_LOCK",
                timestamp = System.currentTimeMillis(),
                result = "BLOCKED",
                payloadDigest = opportunity.proposedAction.targetPayload
            )
            repository.recordAuditEvent(event)
            repository.updateOpportunityStatus(opportunity.id, OpportunityStatus.BLOCKED_BY_LOCK)
            return event
        }

        val event = AuditEvent(
            eventId = UUID.randomUUID().toString(),
            actor = "OWNER_AUTHORIZED_ACTION",
            deviceId = "DEV_ANDROID_LOCAL",
            action = opportunity.proposedAction.actionType.name,
            riskLevel = opportunity.riskLevel,
            authorization = "OWNER_CONFIRMED",
            timestamp = System.currentTimeMillis(),
            result = "SUCCESS",
            payloadDigest = opportunity.proposedAction.targetPayload
        )
        repository.recordAuditEvent(event)
        repository.updateOpportunityStatus(opportunity.id, OpportunityStatus.EXECUTED)
        return event
    }

    suspend fun dismissAction(opportunity: AwareOpportunity) {
        val event = AuditEvent(
            eventId = UUID.randomUUID().toString(),
            actor = "OWNER",
            deviceId = "DEV_ANDROID_LOCAL",
            action = opportunity.proposedAction.actionType.name,
            riskLevel = opportunity.riskLevel,
            authorization = "OWNER_DISMISSED",
            timestamp = System.currentTimeMillis(),
            result = "DISMISSED",
            payloadDigest = opportunity.proposedAction.targetPayload
        )
        repository.recordAuditEvent(event)
        repository.updateOpportunityStatus(opportunity.id, OpportunityStatus.DISMISSED)
    }
}
