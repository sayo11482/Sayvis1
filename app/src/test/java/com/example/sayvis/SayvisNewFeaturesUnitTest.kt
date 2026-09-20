package com.example.sayvis

import com.example.sayvis.data.repository.SayvisRepository
import com.example.sayvis.engine.AutonomousMissionAgent
import com.example.sayvis.engine.CognitiveProfileSyncEngine
import com.example.sayvis.engine.ConnectionQuality
import com.example.sayvis.engine.IngestionSourceKind
import com.example.sayvis.engine.NetworkAiStabilityManager
import com.example.sayvis.engine.SolutionActionType
import com.example.sayvis.engine.StabilityMode
import com.example.sayvis.model.Mission
import com.example.sayvis.model.MissionPriority
import com.example.sayvis.model.MissionStatus
import com.example.sayvis.model.MissionTask
import com.example.sayvis.model.UicCategory
import com.example.sayvis.model.UicStatus
import com.example.sayvis.scripts.GitHubScriptIntegrator
import com.example.sayvis.scripts.ScriptEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SayvisNewFeaturesUnitTest {

    @Test
    fun networkStabilityManager_onlineAndOffline_enforcesCompleteCutoff() {
        val testScope = TestScope()
        val manager = NetworkAiStabilityManager(testScope)

        assertTrue(manager.isOnline.value)
        assertTrue(manager.speedKbps.value > 0.0)
        assertEquals(ConnectionQuality.EXCELLENT, manager.connectionQuality.value)

        // Enforce complete offline disconnect
        manager.setNetworkAccess(false)
        assertFalse(manager.isOnline.value)
        assertEquals(0.0, manager.speedKbps.value, 0.001)
        assertEquals(0L, manager.latencyMs.value)
        assertEquals(ConnectionQuality.OFFLINE, manager.connectionQuality.value)
        assertEquals("۰ کیلوبایت/ث", manager.formattedSpeed(isPersian = true))
        assertEquals("0 KB/s", manager.formattedSpeed(isPersian = false))

        // Restore complete connection
        manager.setNetworkAccess(true)
        assertTrue(manager.isOnline.value)
        assertTrue(manager.speedKbps.value > 10000.0)
        assertTrue(manager.formattedSpeed(isPersian = true).contains("مگابیت/ث"))
    }

    @Test
    fun networkStabilityManager_stabilityModes_switchesCorrectly() {
        val testScope = TestScope()
        val manager = NetworkAiStabilityManager(testScope)

        manager.setStabilityMode(StabilityMode.CONSERVATIVE)
        assertEquals(StabilityMode.CONSERVATIVE, manager.stabilityMode.value)
        assertEquals(30, manager.stabilityMode.value.heartbeatSec)

        manager.setAutoRetry(false)
        assertFalse(manager.autoRetryEnabled.value)

        manager.setAutoLocalFailover(true)
        assertTrue(manager.autoLocalFailover.value)
    }

    @Test
    fun autonomousMissionAgent_identifiesBlockersAndDevisesActionablePlan() {
        val mockRepo = createDummyRepository()
        val agent = AutonomousMissionAgent(mockRepo)

        val missionWithBlocker = Mission(
            id = "m_test_blocker",
            title = "LIT Trading Integration",
            description = "Setup liquidity analysis",
            priority = MissionPriority.HIGH,
            status = MissionStatus.BLOCKED,
            progressPercent = 30,
            deadline = "Tomorrow",
            tasks = listOf(
                MissionTask(id = "t1", title = "Setup broker bridge", isCompleted = true),
                MissionTask(id = "t2", title = "Broker Adapter Sandbox Audit", isCompleted = false, isBlocked = true, blockerReason = "Waiting for external certification"),
                MissionTask(id = "t3", title = "Deploy safeguard guardrails", isCompleted = false)
            )
        )

        val planFa = agent.searchSolution(missionWithBlocker, isPersian = true)
        assertNotNull(planFa)
        assertEquals("m_test_blocker", planFa.missionId)
        assertTrue("Diagnosis should mention the blocker", planFa.diagnosisFa.contains("Broker Adapter Sandbox Audit"))
        assertTrue("Must devise at least one step", planFa.steps.isNotEmpty())

        val unblockStep = planFa.steps.firstOrNull { it.targetTaskId == "t2" }
        assertNotNull("Must target the blocked task", unblockStep)
    }

    @Test
    fun cognitiveProfileSyncEngine_discoversGoogleSearchesNotesAndAlarms() {
        val mockRepo = createDummyRepository()
        val syncEngine = CognitiveProfileSyncEngine(mockRepo)

        val items = syncEngine.discoverIngestionItems()
        assertTrue("Should have multiple discovered ingestion items", items.size >= 5)

        val googleItems = items.filter { it.sourceKind == IngestionSourceKind.GOOGLE_SEARCH }
        val notesItems = items.filter { it.sourceKind == IngestionSourceKind.DEVICE_NOTES }
        val alarmItems = items.filter { it.sourceKind == IngestionSourceKind.DEVICE_ALARMS }

        assertTrue("Must include Google searches", googleItems.isNotEmpty())
        assertTrue("Must include Device notes", notesItems.isNotEmpty())
        assertTrue("Must include Alarms & directives", alarmItems.isNotEmpty())

        // Check alarm commands extraction
        val earlyAlarm = alarmItems.firstOrNull { it.suggestedKey.contains("morning") || it.suggestedKey.contains("wake") }
        assertNotNull(earlyAlarm)
        assertEquals(UicCategory.HABITS, earlyAlarm!!.targetCategory)
        assertEquals(UicStatus.OBSERVED, earlyAlarm.epistemicStatus)
    }

    @Test
    fun gitHubScriptIntegrator_candidateScriptsAreValidInScriptEngine() {
        val integrator = GitHubScriptIntegrator()
        val engine = ScriptEngine()

        val candidates = integrator.discoverCandidates("sayo11482/Sayvis1")
        assertTrue("Must discover GitHub scripts", candidates.isNotEmpty())

        candidates.forEach { candidate ->
            val validation = engine.validate(candidate.source)
            assertTrue("Script '${candidate.name}' should parse cleanly: ${validation.errorEn}", validation.valid)
        }
    }

    private fun createDummyRepository(): SayvisRepository {
        val constructor = SayvisRepository::class.java.constructors.first()
        val nullArgs = arrayOfNulls<Any>(constructor.parameterTypes.size)
        return try {
            constructor.newInstance(*nullArgs) as SayvisRepository
        } catch (e: Exception) {
            val unsafeClass = Class.forName("sun.misc.Unsafe")
            val field = unsafeClass.getDeclaredField("theUnsafe")
            field.isAccessible = true
            val unsafe = field.get(null)
            val allocateInstance = unsafeClass.getMethod("allocateInstance", Class::class.java)
            allocateInstance.invoke(unsafe, SayvisRepository::class.java) as SayvisRepository
        }
    }
}
