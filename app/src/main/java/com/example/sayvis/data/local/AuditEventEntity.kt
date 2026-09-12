package com.example.sayvis.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.sayvis.model.AuditEvent
import com.example.sayvis.model.RiskLevel

@Entity(tableName = "audit_events")
data class AuditEventEntity(
    @PrimaryKey val eventId: String,
    val actor: String,
    val deviceId: String,
    val action: String,
    val riskLevel: String,
    val authorization: String,
    val timestamp: Long,
    val result: String,
    val payloadDigest: String
) {
    fun toDomain(): AuditEvent = AuditEvent(
        eventId = eventId,
        actor = actor,
        deviceId = deviceId,
        action = action,
        riskLevel = runCatching { RiskLevel.valueOf(riskLevel) }.getOrDefault(RiskLevel.LOW_RISK),
        authorization = authorization,
        timestamp = timestamp,
        result = result,
        payloadDigest = payloadDigest
    )

    companion object {
        fun fromDomain(model: AuditEvent): AuditEventEntity = AuditEventEntity(
            eventId = model.eventId,
            actor = model.actor,
            deviceId = model.deviceId,
            action = model.action,
            riskLevel = model.riskLevel.name,
            authorization = model.authorization,
            timestamp = model.timestamp,
            result = model.result,
            payloadDigest = model.payloadDigest
        )
    }
}
