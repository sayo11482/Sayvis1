package com.example.sayvis.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.sayvis.model.ActionType
import com.example.sayvis.model.AwareOpportunity
import com.example.sayvis.model.OpportunityStatus
import com.example.sayvis.model.ProposedAction
import com.example.sayvis.model.RiskLevel

@Entity(tableName = "aware_opportunities")
data class AwareOpportunityEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val triggerReason: String,
    val actionType: String,
    val targetPayload: String,
    val requiredPermission: String,
    val executionSummary: String,
    val riskLevel: String,
    val status: String,
    val createdAt: Long,
    val executedAt: Long?
) {
    fun toDomain(): AwareOpportunity = AwareOpportunity(
        id = id,
        title = title,
        description = description,
        triggerReason = triggerReason,
        proposedAction = ProposedAction(
            actionType = runCatching { ActionType.valueOf(actionType) }.getOrDefault(ActionType.SCHEDULE_FOCUS_BLOCK),
            targetPayload = targetPayload,
            requiredPermission = requiredPermission,
            executionSummary = executionSummary
        ),
        riskLevel = runCatching { RiskLevel.valueOf(riskLevel) }.getOrDefault(RiskLevel.LOW_RISK),
        status = runCatching { OpportunityStatus.valueOf(status) }.getOrDefault(OpportunityStatus.PENDING),
        createdAt = createdAt,
        executedAt = executedAt
    )

    companion object {
        fun fromDomain(model: AwareOpportunity): AwareOpportunityEntity = AwareOpportunityEntity(
            id = model.id,
            title = model.title,
            description = model.description,
            triggerReason = model.triggerReason,
            actionType = model.proposedAction.actionType.name,
            targetPayload = model.proposedAction.targetPayload,
            requiredPermission = model.proposedAction.requiredPermission,
            executionSummary = model.proposedAction.executionSummary,
            riskLevel = model.riskLevel.name,
            status = model.status.name,
            createdAt = model.createdAt,
            executedAt = model.executedAt
        )
    }
}
