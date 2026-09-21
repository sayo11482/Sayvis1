package com.example.sayvis.agent

/**
 * SAYVIS v5.3.0 — pure mission planner (unit-tested).
 *
 * After the solution-seeking agent researches a goal (real web search + page
 * reading + AI synthesis), [tasksFromReport] turns the report into concrete,
 * checkable mission tasks. Deterministic — no AI inside this class.
 */
object MissionPlanner {

    private val BULLET = Regex("^\\s*(?:[-•*]|\\d+[.)]|[۰-۹]+[.)])\\s+")

    /** Action-verb heuristics (fa/en) so vague lines don't become tasks. */
    private val ACTION_HINTS = listOf(
        "بگیر", "کن", "شود", "برو", "بخوان", "بررسی", "ثبت", "اجرا", "نصب", "خرید", "ارسال", "تماس", "تنظیم",
        "install", "check", "register", "run", "buy", "send", "call", "set up", "create", "open", "apply"
    )

    /** Extracts 0..6 task titles from a free-form report. */
    fun tasksFromReport(report: String): List<String> {
        val tasks = ArrayList<String>()
        report.lines().forEach { raw ->
            val line = BULLET.replace(raw.trim(), "").trim()
            if (line.length < 6 || line.length > 140) return@forEach
            val looksLikeStep = BULLET.containsMatchIn(raw.trim()) ||
                ACTION_HINTS.any { line.lowercase().contains(it) }
            if (looksLikeStep && tasks.none { it.equals(line, ignoreCase = true) }) {
                tasks += line
            }
        }
        return tasks.take(6)
    }

    /** A compact mission title from the owner's goal. */
    fun titleFromGoal(goal: String): String {
        val clean = goal.trim().replace(Regex("\\s+"), " ")
        return if (clean.length <= 48) clean else clean.take(45).trimEnd() + "…"
    }
}
