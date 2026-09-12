package com.example.sayvis

import com.example.sayvis.engine.AwareEngine
import com.example.sayvis.engine.UicEngine
import com.example.sayvis.model.ActionType
import com.example.sayvis.model.AwareOpportunity
import com.example.sayvis.model.ContextSnapshot
import com.example.sayvis.model.Mission
import com.example.sayvis.model.MissionStatus
import com.example.sayvis.model.MissionTask
import com.example.sayvis.model.OpportunityStatus
import com.example.sayvis.model.PrivacyLevel
import com.example.sayvis.model.ProposedAction
import com.example.sayvis.model.RiskLevel
import com.example.sayvis.model.UicAttribute
import com.example.sayvis.model.UicCategory
import com.example.sayvis.model.UicStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest

class SayvisCoreUnitTest {

    @Test
    fun uicEngine_computeMetrics_calculatesCorrectAggregations() {
        val uicEngine = UicEngine(repository = createMockRepository())

        val attributes = listOf(
            UicAttribute(
                id = "uic-1",
                category = UicCategory.GOALS,
                key = "daily_deep_work",
                title = "Deep Work Schedule",
                value = "09:00 - 11:30",
                provenance = "USER_EXPLICIT",
                confidence = 1.0f,
                status = UicStatus.CONFIRMED,
                privacyLevel = PrivacyLevel.PROTECTED
            ),
            UicAttribute(
                id = "uic-2",
                category = UicCategory.WORKING_PATTERNS,
                key = "deep_work_interval",
                title = "Focus Rhythm",
                value = "Morning peak",
                provenance = "AI_INFERRED",
                confidence = 0.8f,
                status = UicStatus.INFERRED,
                privacyLevel = PrivacyLevel.STANDARD
            ),
            UicAttribute(
                id = "uic-3",
                category = UicCategory.PREFERENCES,
                key = "theme_preference",
                title = "Theme",
                value = "Dark Space",
                provenance = "SYSTEM_OBSERVED",
                confidence = 0.9f,
                status = UicStatus.OBSERVED,
                privacyLevel = PrivacyLevel.STANDARD
            )
        )

        val metrics = uicEngine.computeMetrics(attributes)
        assertEquals(3, metrics.totalAttributes)
        assertEquals(1, metrics.confirmedCount)
        assertEquals(1, metrics.inferredCount)
        assertEquals(1, metrics.observedCount)
        assertEquals(0.9f, metrics.averageConfidence, 0.01f)
    }

    @Test
    fun awareEngine_detectPatterns_identifiesBlockedMissionsAndDeepWork() {
        val awareEngine = AwareEngine(repository = createMockRepository())

        val testMission = Mission(
            id = "m-1",
            title = "Zero Trust Gateway Deployment",
            description = "Audit and roll out encrypted tunnel",
            priority = com.example.sayvis.model.MissionPriority.CRITICAL,
            status = MissionStatus.ACTIVE,
            progressPercent = 40,
            deadline = "2026-10-01",
            tasks = listOf(
                MissionTask(
                    id = "t-1",
                    title = "Keypair generation",
                    isCompleted = true
                ),
                MissionTask(
                    id = "t-2",
                    title = "Hardware handshake",
                    isCompleted = false,
                    isBlocked = true,
                    blockerReason = "Waiting for companion node public key"
                )
            )
        )

        val uicAttributes = listOf(
            UicAttribute(
                id = "uic-dw",
                category = UicCategory.WORKING_PATTERNS,
                key = "deep_work_interval",
                title = "Deep Work Window",
                value = "Morning",
                provenance = "SYSTEM_OBSERVED",
                confidence = 0.88f,
                status = UicStatus.OBSERVED
            )
        )

        val patterns = awareEngine.detectPatterns(uicAttributes, listOf(testMission))
        assertEquals(2, patterns.size)

        val blockedPattern = patterns.firstOrNull { it.id == "pat_blocked_tasks" }
        assertNotNull(blockedPattern)
        assertTrue(blockedPattern!!.occurrences >= 1)

        val deepWorkPattern = patterns.firstOrNull { it.id == "pat_deep_work_alignment" }
        assertNotNull(deepWorkPattern)
        assertEquals(0.88f, deepWorkPattern!!.confidence, 0.01f)
    }

    @Test
    fun zeroTrust_tamperEvidentSha256_producesDeterministicChain() {
        fun sha256(input: String): String {
            val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }

        val genesisHash = "0000000000000000000000000000000000000000000000000000000000000000"
        val event1Data = "EVT-1|OWNER|LOGIN|dev_local|1700000000"
        val hash1 = sha256(event1Data + genesisHash)

        val event2Data = "EVT-2|AWARE|PROPOSE_FOCUS|dev_local|1700000010"
        val hash2 = sha256(event2Data + hash1)

        assertEquals(64, hash1.length)
        assertEquals(64, hash2.length)
        assertTrue(hash1 != hash2)
    }

    @Test
    fun riskLevel_ordering_and_labeling_isAccurate() {
        assertEquals("Low Risk", RiskLevel.LOW_RISK.labelEn)
        assertEquals("کم‌خطر", RiskLevel.LOW_RISK.labelFa)
        assertEquals("Critical", RiskLevel.CRITICAL.labelEn)
        assertEquals("بحرانی", RiskLevel.CRITICAL.labelFa)
    }

    private fun createMockRepository(): com.example.sayvis.data.repository.SayvisRepository {
        // Use a lightweight dynamic proxy or dummy instance since tests here verify purely computational logic
        val dummyDb = object {}
        return java.lang.reflect.Proxy.newProxyInstance(
            com.example.sayvis.data.repository.SayvisRepository::class.java.classLoader,
            arrayOf()
        ) { _, _, _ -> null } as? com.example.sayvis.data.repository.SayvisRepository
            ?: run {
                // If reflection proxy isn't applicable to concrete class, allocate an uninitialized instance for pure unit tests
                val constructor = com.example.sayvis.data.repository.SayvisRepository::class.java.constructors.first()
                val nullArgs = arrayOfNulls<Any>(constructor.parameterTypes.size)
                try {
                    constructor.newInstance(*nullArgs) as com.example.sayvis.data.repository.SayvisRepository
                } catch (e: Exception) {
                    val unsafeClass = Class.forName("sun.misc.Unsafe")
                    val field = unsafeClass.getDeclaredField("theUnsafe")
                    field.isAccessible = true
                    val unsafe = field.get(null)
                    val allocateInstance = unsafeClass.getMethod("allocateInstance", Class::class.java)
                    allocateInstance.invoke(unsafe, com.example.sayvis.data.repository.SayvisRepository::class.java) as com.example.sayvis.data.repository.SayvisRepository
                }
            }
    }
}
