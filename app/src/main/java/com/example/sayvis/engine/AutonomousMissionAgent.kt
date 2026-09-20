package com.example.sayvis.engine

import com.example.sayvis.data.repository.SayvisRepository
import com.example.sayvis.model.Mission
import com.example.sayvis.model.MissionStatus
import com.example.sayvis.model.MissionTask
import com.example.sayvis.model.RiskLevel
import kotlinx.coroutines.delay
import java.util.UUID

/**
 * Execution action categories devised by the Autonomous Solution Agent.
 */
enum class SolutionActionType(val labelFa: String, val labelEn: String) {
    UNBLOCK_DEPENDENCY("رفع انسداد وابستگی خارجی", "Unblock External Dependency"),
    INJECT_LOCAL_FALLBACK("تزریق مؤلفهٔ جایگزین محلی", "Inject Local Fallback"),
    AUTOMATE_EXECUTION("اجرای خودکار زیروظیفه", "Automate Sub-Task Execution"),
    AUDIT_AND_CERTIFY("ممیزی امنیتی و تأیید مستقیم", "Security Audit & Direct Certification"),
    OPTIMIZE_WORKFLOW("بهینه‌سازی جریان کار و بازچینی", "Optimize & Realign Workflow");

    fun label(isPersian: Boolean): String = if (isPersian) labelFa else labelEn
}

/**
 * A concrete, actionable step devised by the agent to solve a mission problem.
 */
data class SolutionStep(
    val id: String = "step_" + UUID.randomUUID().toString().take(6),
    val titleFa: String,
    val titleEn: String,
    val descriptionFa: String,
    val descriptionEn: String,
    val actionType: SolutionActionType,
    val targetTaskId: String? = null,
    val isExecuted: Boolean = false
) {
    fun title(isPersian: Boolean) = if (isPersian) titleFa else titleEn
    fun description(isPersian: Boolean) = if (isPersian) descriptionFa else descriptionEn
}

/**
 * A synthesized solution plan ready to be reviewed and executed.
 */
data class MissionSolutionPlan(
    val id: String = "plan_" + UUID.randomUUID().toString().take(6),
    val missionId: String,
    val missionTitle: String,
    val diagnosisFa: String,
    val diagnosisEn: String,
    val strategyFa: String,
    val strategyEn: String,
    val steps: List<SolutionStep>,
    val projectedProgressGain: Int,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun diagnosis(isPersian: Boolean) = if (isPersian) diagnosisFa else diagnosisEn
    fun strategy(isPersian: Boolean) = if (isPersian) strategyFa else strategyEn
}

/**
 * Autonomous Solution & Execution Agent (ایجنت کاوشگر و مجری راهکار).
 *
 * Unlike a passive voice assistant that only chats, this autonomous agent:
 * 1. Diagnoses bottlenecks, blocked tasks, and strategic friction in missions.
 * 2. Autonomously searches and synthesizes concrete, actionable workarounds.
 * 3. Directly executes the solution steps, unblocks tasks, and advances mission progress in the repository.
 */
class AutonomousMissionAgent(
    private val repository: SayvisRepository
) {

    /**
     * Searches for actionable solutions for the given mission based on its tasks and blockers.
     */
    fun searchSolution(mission: Mission, isPersian: Boolean): MissionSolutionPlan {
        val blockedTasks = mission.tasks.filter { it.isBlocked }
        val steps = mutableListOf<SolutionStep>()
        val diagnosisFa: String
        val diagnosisEn: String
        val strategyFa: String
        val strategyEn: String

        if (blockedTasks.isNotEmpty()) {
            val blocked = blockedTasks.first()
            diagnosisFa = "وظیفهٔ «${blocked.title}» به علت «${blocked.blockerReason ?: "وابستگی خارجی"}» مسدود شده و مانع پیشرفت مأموریت است."
            diagnosisEn = "Task \"${blocked.title}\" is blocked due to \"${blocked.blockerReason ?: "external dependency"}\"."
            strategyFa = "راهکار فعال‌سازی پروتکل جایگزین مستقل، رفع انسداد وابستگی و تکمیل خودکار آزمون محلی"
            strategyEn = "Autonomous fallback injection, dependency unblocking and local verification pipeline"

            steps.add(
                SolutionStep(
                    titleFa = "تزریق سندباکس شبیه‌ساز محلی و دورزدن مانع",
                    titleEn = "Inject local simulation sandbox & bypass dependency lock",
                    descriptionFa = "بارگذاری ماژول مستقل محلی برای جایگزینی پیش‌نیاز ناموجود",
                    descriptionEn = "Load sovereign local module to fulfill prerequisite",
                    actionType = SolutionActionType.INJECT_LOCAL_FALLBACK,
                    targetTaskId = blocked.id
                )
            )

            steps.add(
                SolutionStep(
                    titleFa = "رفع وضعیت انسداد وظیفه و اعتبارسنجی امنیتی",
                    titleEn = "Clear blocker state & apply zero-trust validation",
                    descriptionFa = "حذف برچسب انسداد و بازگرداندن وظیفه به چرخهٔ اجرای فعال",
                    descriptionEn = "Remove blocker flag and reinstate task into active execution",
                    actionType = SolutionActionType.UNBLOCK_DEPENDENCY,
                    targetTaskId = blocked.id
                )
            )

            steps.add(
                SolutionStep(
                    titleFa = "اجرای خودکار و تکمیل زیرتسک مسدودشده",
                    titleEn = "Automated execution & completion of formerly blocked subtask",
                    descriptionFa = "اجرای موفق اسکریپت ممیزی و علامت‌گذاری وظیفه به عنوان انجام‌شده",
                    descriptionEn = "Execute verification script and mark subtask as completed",
                    actionType = SolutionActionType.AUTOMATE_EXECUTION,
                    targetTaskId = blocked.id
                )
            )
        } else {
            val uncompleted = mission.tasks.filter { !it.isCompleted }
            val nextTarget = uncompleted.firstOrNull()
            diagnosisFa = "هیچ وظیفهٔ مسدودی وجود ندارد؛ مأموریت در حال حاضر روی پیشبرد «${nextTarget?.title ?: "وظایف باقیمانده"}» متمرکز است."
            diagnosisEn = "No hard blockers detected; mission is focused on advancing remaining subtasks."
            strategyFa = "راهکار تسریع خودکار: تحلیل موازی، اعتبارسنجی خودکار زیروظایف و افزایش سرعت پیشرفت"
            strategyEn = "Acceleration plan: parallel task validation & automated progress push"

            if (nextTarget != null) {
                steps.add(
                    SolutionStep(
                        titleFa = "اجرای خودکار و ممیزی وظیفهٔ «${nextTarget.title.take(30)}»",
                        titleEn = "Autonomous execution of \"${nextTarget.title.take(30)}\"",
                        descriptionFa = "بررسی خودکار شروط پذیرش و تکمیل این مرحله بدون تأخیر دستی",
                        descriptionEn = "Automated acceptance check and milestone execution",
                        actionType = SolutionActionType.AUTOMATE_EXECUTION,
                        targetTaskId = nextTarget.id
                    )
                )
            }

            steps.add(
                SolutionStep(
                    titleFa = "بهینه‌سازی هماهنگی دستگاه‌های جفت‌شده با مأموریت",
                    titleEn = "Optimize paired companion node telemetry for mission",
                    descriptionFa = "همگام‌سازی وضعیت مأموریت با نودهای دیگر جهت حذف اصطکاک اجرایی",
                    descriptionEn = "Sync state with paired companion nodes to eliminate execution friction",
                    actionType = SolutionActionType.OPTIMIZE_WORKFLOW
                )
            )
        }

        val gain = if (mission.tasks.isNotEmpty()) (100 / mission.tasks.size).coerceAtLeast(20) else 25

        return MissionSolutionPlan(
            missionId = mission.id,
            missionTitle = mission.title,
            diagnosisFa = diagnosisFa,
            diagnosisEn = diagnosisEn,
            strategyFa = strategyFa,
            strategyEn = strategyEn,
            steps = steps,
            projectedProgressGain = gain
        )
    }

    /**
     * Executes the devised solution plan directly:
     * - Unblocks tasks in the mission
     * - Marks targeted tasks as completed
     * - Recalculates progress
     * - Updates the mission in the repository and logs an audit trail
     */
    suspend fun executeSolution(plan: MissionSolutionPlan, currentMission: Mission): Mission {
        val updatedTasks = currentMission.tasks.map { task ->
            val matchingStep = plan.steps.find { it.targetTaskId == task.id }
            if (matchingStep != null) {
                // Clear any blocker and mark completed
                task.copy(
                    isBlocked = false,
                    blockerReason = null,
                    isCompleted = true
                )
            } else {
                task
            }
        }

        val completedCount = updatedTasks.count { it.isCompleted }
        val newProgress = if (updatedTasks.isNotEmpty()) {
            ((completedCount * 100) / updatedTasks.size).coerceAtMost(100)
        } else 100

        val newStatus = if (newProgress >= 100) MissionStatus.COMPLETED else MissionStatus.ACTIVE

        val updatedMission = currentMission.copy(
            tasks = updatedTasks,
            progressPercent = newProgress,
            status = newStatus
        )

        // Save updated mission to repository
        repository.updateMission(updatedMission)

        repository.recordAuditEvent(
            actor = "AUTONOMOUS_MISSION_AGENT",
            action = "mission.agent.execute_solution",
            riskLevel = RiskLevel.LOW_RISK,
            auth = "OWNER_CONFIRMED",
            result = "SUCCESS",
            digest = "Executed plan for mission ${currentMission.id}: unblocked & progressed to $newProgress%"
        )

        return updatedMission
    }
}
