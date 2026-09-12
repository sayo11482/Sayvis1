package com.example.sayvis.model

/**
 * Immutable Audit Event recording every sensitive or proactive action.
 */
data class AuditEvent(
    val eventId: String,
    val actor: String, // e.g. "AWARE_ENGINE", "OWNER", "AGENT_ORCHESTRATOR"
    val deviceId: String,
    val action: String,
    val riskLevel: RiskLevel,
    val authorization: String, // e.g. "OWNER_CONFIRMED", "POLICY_PERMITTED", "BLOCKED_EMERGENCY_LOCK"
    val timestamp: Long = System.currentTimeMillis(),
    val result: String, // "SUCCESS", "REJECTED", "BLOCKED"
    val payloadDigest: String = ""
)

/**
 * Overall security state of SAYVIS.
 */
data class SystemSecurityState(
    val emergencyLockActive: Boolean = false,
    val trustedDevicesCount: Int = 3,
    val zeroTrustPolicyEnforced: Boolean = true,
    val lastAuditTimestamp: Long = System.currentTimeMillis()
)
