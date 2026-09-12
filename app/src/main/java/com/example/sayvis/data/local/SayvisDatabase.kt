package com.example.sayvis.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UicAttributeEntity::class,
        AwareOpportunityEntity::class,
        MissionEntity::class,
        AuditEventEntity::class,
        MemoryItemEntity::class,
        DeviceEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SayvisDatabase : RoomDatabase() {
    abstract fun uicDao(): UicDao
    abstract fun awareDao(): AwareDao
    abstract fun missionDao(): MissionDao
    abstract fun auditEventDao(): AuditEventDao
    abstract fun memoryDao(): MemoryDao
    abstract fun deviceDao(): DeviceDao

    companion object {
        @Volatile
        private var INSTANCE: SayvisDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): SayvisDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SayvisDatabase::class.java,
                    "sayvis_operating_layer.db"
                )
                .fallbackToDestructiveMigration()
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        scope.launch(Dispatchers.IO) {
                            seedInitialData(getDatabase(context, scope))
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun seedInitialData(database: SayvisDatabase) {
            val now = System.currentTimeMillis()

            // 1. Seed UIC (User Cognitive Model) Attributes
            val initialUic = listOf(
                UicAttributeEntity(
                    id = "uic_pref_01",
                    category = "PREFERENCES",
                    key = "primary_language",
                    title = "Primary Language & Tone",
                    value = "Bilingual Persian/English - Formal, Precise, High-Composure",
                    provenance = "Explicit Owner Onboarding Config",
                    confidence = 1.0f,
                    status = "CONFIRMED",
                    privacyLevel = "STANDARD",
                    firstObservedAt = now - 86400000L * 7,
                    lastConfirmedAt = now - 86400000L,
                    updatedAt = now
                ),
                UicAttributeEntity(
                    id = "uic_pattern_01",
                    category = "WORKING_PATTERNS",
                    key = "deep_work_interval",
                    title = "Optimal Deep Work Window",
                    value = "09:00 - 11:30 AM (Peak focus, zero-interruption preference)",
                    provenance = "AWARE telemetry analysis over 14 days",
                    confidence = 0.88f,
                    status = "INFERRED",
                    privacyLevel = "PROTECTED",
                    firstObservedAt = now - 86400000L * 10,
                    lastConfirmedAt = null,
                    updatedAt = now
                ),
                UicAttributeEntity(
                    id = "uic_goal_01",
                    category = "GOALS",
                    key = "quarterly_milestone",
                    title = "Global Launch of SAYVIS Core",
                    value = "Deploy multi-device gateway, native Android node, and secure agent pipeline",
                    provenance = "Owner Strategic Mission Entry",
                    confidence = 1.0f,
                    status = "CONFIRMED",
                    privacyLevel = "STANDARD",
                    firstObservedAt = now - 86400000L * 5,
                    lastConfirmedAt = now - 86400000L * 2,
                    updatedAt = now
                ),
                UicAttributeEntity(
                    id = "uic_habit_01",
                    category = "HABITS",
                    key = "nightly_review",
                    title = "Nightly Mission Reconciliation",
                    value = "Reviews pending tasks and approves next-day agenda between 21:30 and 22:15",
                    provenance = "Interaction frequency clustering",
                    confidence = 0.76f,
                    status = "OBSERVED",
                    privacyLevel = "PROTECTED",
                    firstObservedAt = now - 86400000L * 3,
                    lastConfirmedAt = null,
                    updatedAt = now
                ),
                UicAttributeEntity(
                    id = "uic_constraint_01",
                    category = "CONSTRAINTS",
                    key = "zero_trust_strictness",
                    title = "Strict Zero Trust Policy",
                    value = "Never allow automated file deletion or financial actions without biometric/passkey approval",
                    provenance = "Security Governance Rule",
                    confidence = 1.0f,
                    status = "CONFIRMED",
                    privacyLevel = "CONFIDENTIAL",
                    firstObservedAt = now - 86400000L * 14,
                    lastConfirmedAt = now - 86400000L * 2,
                    updatedAt = now
                ),
                UicAttributeEntity(
                    id = "uic_lto_01",
                    category = "LONG_TERM_OBJECTIVES",
                    key = "autonomous_personal_os",
                    title = "Lifelong Cognitive Augmentation",
                    value = "Empower personal agency, protect sovereign data ownership, and eliminate cognitive drift",
                    provenance = "Product Vision Master Directive",
                    confidence = 0.95f,
                    status = "CONFIRMED",
                    privacyLevel = "STANDARD",
                    firstObservedAt = now - 86400000L * 20,
                    lastConfirmedAt = now - 86400000L * 5,
                    updatedAt = now
                )
            )
            database.uicDao().insertAllAttributes(initialUic)

            // 2. Seed AWARE Opportunities
            val initialAware = listOf(
                AwareOpportunityEntity(
                    id = "opp_aware_01",
                    title = "Protect Peak Cognitive Window",
                    description = "Detected scheduled meeting conflicting with your 09:30 AM peak deep-work baseline.",
                    triggerReason = "Working Pattern correlation: 88% confidence peak window vs incoming calendar slot.",
                    actionType = "SCHEDULE_FOCUS_BLOCK",
                    targetPayload = "Block notifications & reschedule advisory check-in to 14:00",
                    requiredPermission = "calendar.write, notifications.modify",
                    executionSummary = "Deflect non-urgent alerts & request slot postponement via calendar gateway.",
                    riskLevel = "MEDIUM_RISK",
                    status = "PENDING",
                    createdAt = now - 1800000L,
                    executedAt = null
                ),
                AwareOpportunityEntity(
                    id = "opp_aware_02",
                    title = "Mission Critical Blocker Mitigation",
                    description = "Task 'Crypto Key Exchange Protocol' in Mission 'SAYVIS Core' has been blocked for 48h.",
                    triggerReason = "Mission Engine telemetry: Dependency timeout on external cryptographic spec.",
                    actionType = "REPLAN_MISSION",
                    targetPayload = "Inject Fallback Ed25519 Spec & reassign priority to high",
                    requiredPermission = "missions.modify",
                    executionSummary = "Unblock task #2 and update roadmap milestone dependencies.",
                    riskLevel = "LOW_RISK",
                    status = "PENDING",
                    createdAt = now - 7200000L,
                    executedAt = null
                ),
                AwareOpportunityEntity(
                    id = "opp_aware_03",
                    title = "Unattended Remote Device Session",
                    description = "Windows Workstation (Node-Win01) idle for 18h with active privileged token.",
                    triggerReason = "Gateway Session Monitor: Inactivity threshold exceeded.",
                    actionType = "RESTRICT_SYSTEM_ACCESS",
                    targetPayload = "Demote session token to read-only safe mode",
                    requiredPermission = "security.token.revoke",
                    executionSummary = "Rotate session key and enforce re-authentication on next interactive packet.",
                    riskLevel = "HIGHER_RISK",
                    status = "PENDING",
                    createdAt = now - 3600000L,
                    executedAt = null
                )
            )
            database.awareDao().insertAllOpportunities(initialAware)

            // 3. Seed Missions
            val initialMissions = listOf(
                MissionEntity(
                    id = "mission_01",
                    title = "Deploy SAYVIS Multi-Device Gateway",
                    description = "Architect and verify Zero-Trust Gateway connecting Android, Windows, and Web nodes.",
                    priority = "CRITICAL",
                    status = "ACTIVE",
                    progressPercent = 75,
                    deadline = "Tomorrow, 18:00 UTC",
                    tasksSerialized = "t1||Implement Zero-Trust Action Model||true||false||;;t2||Device Cryptographic Identity & Pairing||true||false||;;t3||Integrate AWARE Opportunity Engine||true||false||;;t4||Enforce Strict Emergency Lockout Mechanism||false||false||",
                    createdAt = now - 86400000L * 2
                ),
                MissionEntity(
                    id = "mission_02",
                    title = "LIT Trading Intelligence - Risk Engine Gate",
                    description = "Calibrate liquidity inversion analysis and enforce paper-trading kill switch safeguards.",
                    priority = "HIGH",
                    status = "ACTIVE",
                    progressPercent = 40,
                    deadline = "Sept 18, 2026",
                    tasksSerialized = "t201||Order Block & BOS Detection Module||true||false||;;t202||Drawdown Hard Cap Guardrails||true||false||;;t203||Broker Adapter Sandbox Audit||false||true||Waiting for audit signoff",
                    createdAt = now - 86400000L
                )
            )
            database.missionDao().insertAllMissions(initialMissions)

            // 4. Seed Connected Devices
            val initialDevices = listOf(
                DeviceEntity(
                    id = "dev_android_primary",
                    name = "SAYVIS Mobile Node (Galaxy S24 / Pixel)",
                    type = "ANDROID_PHONE",
                    publicKeyFingerprint = "SHA256:7e:92:4a:c1:0b:38:de:89:12",
                    isTrusted = true,
                    isRevoked = false,
                    lastActiveAt = now,
                    capabilitiesCsv = "biometric_auth,voice_vad,local_storage,camera,push_notifications"
                ),
                DeviceEntity(
                    id = "dev_win_workstation",
                    name = "SAYVIS Master Rig (Windows 11 Agent)",
                    type = "WINDOWS_PC",
                    publicKeyFingerprint = "SHA256:55:ab:c3:19:9e:f2:01:77:4b",
                    isTrusted = true,
                    isRevoked = false,
                    lastActiveAt = now - 3600000L,
                    capabilitiesCsv = "hardware_gpu,filesystem,controlled_powershell,local_llm"
                ),
                DeviceEntity(
                    id = "dev_web_control",
                    name = "SAYVIS Cloud Web Console",
                    type = "WEB_CLIENT",
                    publicKeyFingerprint = "SHA256:a1:00:88:fe:90:3d:41:c8:10",
                    isTrusted = true,
                    isRevoked = false,
                    lastActiveAt = now - 86400000L,
                    capabilitiesCsv = "dashboard_management,audit_viewer,remote_emergency_lock"
                )
            )
            database.deviceDao().insertAllDevices(initialDevices)

            // 5. Seed Initial Audit Log
            val initialAudit = AuditEventEntity(
                eventId = "audit_init_boot",
                actor = "SAYVIS_SECURITY_CORE",
                deviceId = "dev_android_primary",
                action = "system.boot.verify_integrity",
                riskLevel = "LOW_RISK",
                authorization = "OWNER_CONFIRMED",
                timestamp = now,
                result = "SUCCESS",
                payloadDigest = "HMAC-SHA256-VALIDATED"
            )
            database.auditEventDao().insertAuditEvent(initialAudit)
        }
    }
}
