package com.example.sayvis.data.repository

import com.example.sayvis.data.local.AuditEventEntity
import com.example.sayvis.data.local.AwareOpportunityEntity
import com.example.sayvis.data.local.DeviceEntity
import com.example.sayvis.data.local.MemoryItemEntity
import com.example.sayvis.data.local.MissionEntity
import com.example.sayvis.data.local.SayvisDatabase
import com.example.sayvis.data.local.UicAttributeEntity
import com.example.sayvis.model.AuditEvent
import com.example.sayvis.model.AwareOpportunity
import com.example.sayvis.model.CognitiveLoadLevel
import com.example.sayvis.model.ContextSnapshot
import com.example.sayvis.model.Device
import com.example.sayvis.model.FocusActivity
import com.example.sayvis.model.MemoryItem
import com.example.sayvis.model.Mission
import com.example.sayvis.model.OpportunityStatus
import com.example.sayvis.model.RiskLevel
import com.example.sayvis.model.UicAttribute
import com.example.sayvis.model.UicCategory
import com.example.sayvis.model.UicStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class SayvisRepository(private val database: SayvisDatabase) {

    // --- UIC (User Cognitive Model) ---
    val allUicAttributes: Flow<List<UicAttribute>> = database.uicDao().getAllAttributesFlow().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun updateUicStatus(id: String, newStatus: UicStatus) {
        val now = System.currentTimeMillis()
        database.uicDao().updateStatus(id, newStatus.name, now)
        recordAuditEvent(
            actor = "OWNER",
            action = "uic.attribute.update_status",
            riskLevel = RiskLevel.LOW_RISK,
            auth = "OWNER_CONFIRMED",
            result = "SUCCESS",
            digest = "AttributeId: $id -> ${newStatus.name}"
        )
    }

    suspend fun addUicAttribute(attribute: UicAttribute) {
        database.uicDao().insertAttribute(UicAttributeEntity.fromDomain(attribute))
        recordAuditEvent(
            actor = "OWNER",
            action = "uic.attribute.create",
            riskLevel = RiskLevel.LOW_RISK,
            auth = "OWNER_CONFIRMED",
            result = "SUCCESS",
            digest = "Key: ${attribute.key} [${attribute.status.name}]"
        )
    }

    suspend fun insertUicAttribute(attribute: UicAttribute) = addUicAttribute(attribute)
    suspend fun confirmUicAttribute(id: String) = updateUicStatus(id, UicStatus.CONFIRMED)
    suspend fun revokeUicAttribute(id: String) = updateUicStatus(id, UicStatus.REVOKED)

    suspend fun deleteUicAttribute(id: String) {
        database.uicDao().deleteAttribute(id)
        recordAuditEvent(
            actor = "OWNER",
            action = "uic.attribute.delete",
            riskLevel = RiskLevel.MEDIUM_RISK,
            auth = "OWNER_CONFIRMED",
            result = "SUCCESS",
            digest = "DeletedAttribute: $id"
        )
    }

    // --- Long-term memory (assistant "remember / recall") ---
    val allMemories: Flow<List<MemoryItem>> = database.memoryDao().getAllMemoriesFlow().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun addMemory(item: MemoryItem) {
        database.memoryDao().insertMemory(MemoryItemEntity.fromDomain(item))
        recordAuditEvent(
            actor = "SAYVIS_AGENT",
            action = "memory.item.create",
            riskLevel = RiskLevel.LOW_RISK,
            auth = "OWNER_CONFIRMED",
            result = "SUCCESS",
            digest = "Memory: ${item.content.take(60)}"
        )
    }

    suspend fun deleteMemory(id: String) {
        database.memoryDao().deleteMemory(id)
    }

    /** Keyword search over stored facts, newest & most important first. */
    suspend fun searchMemories(needle: String): List<MemoryItem> {
        val key = needle.trim()
        if (key.isEmpty()) return emptyList()
        val variants = linkedSetOf(key)
        // Persian users type with/without ZWNJ and with Arabic yeh/kaf variants.
        variants.add(key.replace("\u200c".toRegex(), " "))
        variants.add(key.replace('ی', 'ي').replace('ک', 'ك'))
        val found = LinkedHashMap<String, MemoryItem>()
        for (variant in variants) {
            for (entity in database.memoryDao().searchMemories(variant)) {
                found.putIfAbsent(entity.id, entity.toDomain())
            }
        }
        return found.values.toList()
    }

    // --- AWARE Engine ---
    val allOpportunities: Flow<List<AwareOpportunity>> = database.awareDao().getAllOpportunitiesFlow().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun approveAndExecuteOpportunity(opportunityId: String, emergencyLockActive: Boolean): Boolean {
        if (emergencyLockActive) {
            recordAuditEvent(
                actor = "AWARE_ENGINE",
                action = "opportunity.execute.attempt",
                riskLevel = RiskLevel.CRITICAL,
                auth = "BLOCKED_EMERGENCY_LOCK",
                result = "BLOCKED",
                digest = "Blocked execution of $opportunityId under Emergency Lock"
            )
            return false
        }

        val now = System.currentTimeMillis()
        database.awareDao().updateOpportunityStatus(opportunityId, OpportunityStatus.EXECUTED.name, now)
        recordAuditEvent(
            actor = "AWARE_ENGINE",
            action = "opportunity.execute",
            riskLevel = RiskLevel.MEDIUM_RISK,
            auth = "OWNER_CONFIRMED",
            result = "SUCCESS",
            digest = "Executed opportunity: $opportunityId"
        )
        return true
    }

    suspend fun dismissOpportunity(opportunityId: String) {
        database.awareDao().updateOpportunityStatus(opportunityId, OpportunityStatus.DISMISSED.name, null)
        recordAuditEvent(
            actor = "OWNER",
            action = "opportunity.dismiss",
            riskLevel = RiskLevel.LOW_RISK,
            auth = "OWNER_CONFIRMED",
            result = "SUCCESS",
            digest = "Dismissed: $opportunityId"
        )
    }

    suspend fun insertOpportunity(opp: AwareOpportunity) {
        database.awareDao().insertOpportunity(AwareOpportunityEntity.fromDomain(opp))
    }

    suspend fun updateOpportunityStatus(id: String, status: OpportunityStatus) {
        val executedAt = if (status == OpportunityStatus.EXECUTED) System.currentTimeMillis() else null
        database.awareDao().updateOpportunityStatus(id, status.name, executedAt)
    }

    // --- Missions ---
    val allMissions: Flow<List<Mission>> = database.missionDao().getAllMissionsFlow().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun updateTaskCompletion(missionId: String, taskId: String, isCompleted: Boolean) {
        val entity = database.missionDao().getMissionById(missionId) ?: return
        val currentMission = entity.toDomain()
        val updatedTasks = currentMission.tasks.map { task ->
            if (task.id == taskId) task.copy(isCompleted = isCompleted) else task
        }
        val completedCount = updatedTasks.count { it.isCompleted }
        val newProgress = if (updatedTasks.isNotEmpty()) (completedCount * 100) / updatedTasks.size else 0

        val updatedMission = currentMission.copy(
            tasks = updatedTasks,
            progressPercent = newProgress
        )
        database.missionDao().updateMission(MissionEntity.fromDomain(updatedMission))
        recordAuditEvent(
            actor = "OWNER",
            action = "mission.task.toggle",
            riskLevel = RiskLevel.LOW_RISK,
            auth = "OWNER_CONFIRMED",
            result = "SUCCESS",
            digest = "Mission: $missionId Task: $taskId -> $isCompleted ($newProgress%)"
        )
    }

    /**
     * v5.3.1 — the AUTO executor's finish line: every task ticked, progress 100,
     * status COMPLETED. Only ever called when the executor's own metrics say so.
     */
    suspend fun completeMission(missionId: String) {
        val entity = database.missionDao().getMissionById(missionId) ?: return
        val current = entity.toDomain()
        val updated = current.copy(
            status = com.example.sayvis.model.MissionStatus.COMPLETED,
            progressPercent = 100,
            tasks = current.tasks.map { it.copy(isCompleted = true) }
        )
        database.missionDao().updateMission(MissionEntity.fromDomain(updated))
        recordAuditEvent(
            actor = "SAYVIS_AGENT",
            action = "mission.auto_complete",
            riskLevel = RiskLevel.LOW_RISK,
            auth = "OWNER_CONFIRMED",
            result = "SUCCESS",
            digest = "Mission $missionId auto-completed by the executor agent"
        )
    }

    suspend fun addMission(mission: Mission) {
        database.missionDao().insertMission(MissionEntity.fromDomain(mission))
        recordAuditEvent(
            actor = "OWNER",
            action = "mission.create",
            riskLevel = RiskLevel.LOW_RISK,
            auth = "OWNER_CONFIRMED",
            result = "SUCCESS",
            digest = "Mission: ${mission.title}"
        )
    }

    // --- Audit & Security ---
    val allAuditEvents: Flow<List<AuditEvent>> = database.auditEventDao().getAllAuditEventsFlow().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun recordAuditEvent(event: AuditEvent) {
        database.auditEventDao().insertAuditEvent(AuditEventEntity.fromDomain(event))
    }

    suspend fun recordAuditEvent(
        actor: String,
        action: String,
        riskLevel: RiskLevel,
        auth: String,
        result: String,
        digest: String,
        deviceId: String = "dev_android_primary"
    ) {
        val event = AuditEvent(
            eventId = "evt_" + UUID.randomUUID().toString().take(8),
            actor = actor,
            deviceId = deviceId,
            action = action,
            riskLevel = riskLevel,
            authorization = auth,
            timestamp = System.currentTimeMillis(),
            result = result,
            payloadDigest = digest
        )
        database.auditEventDao().insertAuditEvent(AuditEventEntity.fromDomain(event))
    }

    // --- Devices ---
    val allDevices: Flow<List<Device>> = database.deviceDao().getAllDevicesFlow().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun toggleDeviceTrust(deviceId: String, currentTrust: Boolean) {
        val newTrust = !currentTrust
        database.deviceDao().updateDeviceStatus(deviceId, isRevoked = false, isTrusted = newTrust)
        recordAuditEvent(
            actor = "OWNER",
            action = "device.trust.toggle",
            riskLevel = RiskLevel.HIGHER_RISK,
            auth = "OWNER_CONFIRMED",
            result = "SUCCESS",
            digest = "Device: $deviceId -> Trust: $newTrust"
        )
    }

    /** Adds a device that passed the pairing handshake. It starts UNTRUSTED. */
    suspend fun registerPairedDevice(device: Device) {
        database.deviceDao().insertDevice(DeviceEntity.fromDomain(device))
        recordAuditEvent(
            actor = "OWNER",
            action = "device.pair",
            riskLevel = RiskLevel.HIGHER_RISK,
            auth = "OWNER_CONFIRMED_PAIRING_CODE",
            result = "SUCCESS",
            digest = "Paired ${device.type.name} ${device.name} fp=${device.publicKeyFingerprint}",
            deviceId = device.id
        )
    }

    suspend fun revokeDevice(deviceId: String) {
        database.deviceDao().updateDeviceStatus(deviceId, isRevoked = true, isTrusted = false)
        recordAuditEvent(
            actor = "OWNER",
            action = "device.revoke",
            riskLevel = RiskLevel.CRITICAL,
            auth = "OWNER_CONFIRMED",
            result = "SUCCESS",
            digest = "Revoked Device: $deviceId"
        )
    }

    // Helper to build real-time context snapshot for AWARE engine.
    // The snapshot is language-neutral: it stores enum keys and raw values, and the UI
    // renders it through ContextLocalization so Persian users never see English prose.
    fun getContextSnapshot(
        activeMissionsCount: Int,
        blockedTasksCount: Int,
        emergencyLockActive: Boolean,
        isOnline: Boolean,
        batteryPercent: Int = 85,
        isCharging: Boolean = false,
        focusWindowActive: Boolean = false,
        currentActivity: FocusActivity = FocusActivity.STRATEGIC_EXECUTION
    ): ContextSnapshot {
        val load = when {
            emergencyLockActive -> CognitiveLoadLevel.ELEVATED
            blockedTasksCount >= 3 -> CognitiveLoadLevel.FATIGUE_RISK
            blockedTasksCount > 0 -> CognitiveLoadLevel.ELEVATED
            else -> CognitiveLoadLevel.OPTIMAL
        }
        return ContextSnapshot(
            timestamp = System.currentTimeMillis(),
            focusWindowActive = focusWindowActive,
            focusWindowStart = "09:00",
            focusWindowEnd = "11:30",
            currentActivity = currentActivity,
            cognitiveLoad = load,
            activeMissionsCount = activeMissionsCount,
            blockedTasksCount = blockedTasksCount,
            isOnline = isOnline,
            gatewaySecure = isOnline,
            emergencyLockActive = emergencyLockActive,
            batteryPercent = batteryPercent,
            isCharging = isCharging
        )
    }
}
