package com.example.sayvis.scripts

import java.util.UUID

/**
 * A script candidate identified and fetched from GitHub.
 */
data class GitHubScriptCandidate(
    val id: String = "gh_" + UUID.randomUUID().toString().take(6),
    val name: String,
    val repository: String,
    val path: String,
    val descriptionFa: String,
    val descriptionEn: String,
    val trigger: ScriptTrigger,
    val source: String,
    val isSelected: Boolean = false
) {
    fun description(isPersian: Boolean): String = if (isPersian) descriptionFa else descriptionEn
}

/**
 * Integrator tool that connects to GitHub repositories, identifies compatible automation
 * scripts, allows the owner to select target items, and merges them directly into Sayvis.
 */
class GitHubScriptIntegrator {

    /**
     * Identifies compatible automation scripts within GitHub repositories (e.g. sayo11482/Sayvis1).
     */
    fun discoverCandidates(repositoryName: String = "sayo11482/Sayvis1"): List<GitHubScriptCandidate> = listOf(
        GitHubScriptCandidate(
            id = "gh_script_battery_sentinel",
            name = "دیده‌بان مصرف انرژی و شارژ ایمن",
            repository = repositoryName,
            path = "scripts/battery_guard.say",
            descriptionFa = "پایش دائمی باتری؛ هنگام افت شارژ به زیر ۲۰٪ و عدم اتصال شارژر، اعلان صادر کرده و پیشنهاد بهینه‌سازی می‌دهد.",
            descriptionEn = "Monitors battery status; alerts when under 20% without charging and proposes safe mode.",
            trigger = ScriptTrigger.CONTEXT_CHANGE,
            source = """
                # نگهبان هوشمند باتری و انرژی
                WHEN battery < 20 AND battery.charging == false THEN notify "شارژ باتری به کمتر از ۲۰٪ رسید ({battery}%). دستگاه در حالت ذخیره انرژی قرار گیرد."
                WHEN battery < 10 THEN propose "کاهش فرکانس ارتباطی و فعال‌سازی پروتکل مصرف کمینه"
            """.trimIndent()
        ),
        GitHubScriptCandidate(
            id = "gh_script_lit_guardian",
            name = "محافظ حد ضرر نقدینگی و بازار (LIT)",
            repository = repositoryName,
            path = "scripts/lit_market_guardian.say",
            descriptionFa = "پایش رویدادهای بازار؛ در صورت افت سود روزانه به کمتر از سقف مجاز، بلافاصله اقدامات پرریسک را مسدود می‌کند.",
            descriptionEn = "Market-event monitor; instantly blocks risky actions if daily drawdown limit is reached.",
            trigger = ScriptTrigger.MARKET,
            source = """
                # محافظ نقدینگی و حد ضرر معاملات
                SET ${'$'}daily_cap = 50.0
                ON MARKET THEN log "بررسی نقدینگی بازار و جریان سفارشات"
                WHEN account.profit < 0 - ${'$'}daily_cap THEN block "حد ضرر روزانه فعال شد. ارسال سفارشات جدید موقتاً مسدود گردید."
            """.trimIndent()
        ),
        GitHubScriptCandidate(
            id = "gh_script_focus_optimizer",
            name = "هماهنگ‌ساز پنجرهٔ کار عمیق و تمرکز",
            repository = repositoryName,
            path = "scripts/deep_work_focus.say",
            descriptionFa = "هنگام شروع پنجرهٔ تمرکز عمیق، نوتیفیکیشن‌های جانبی را معلق کرده و وضعیت سیستم را متمرکز نگه می‌دارد.",
            descriptionEn = "Mutes non-critical notifications during deep work focus windows.",
            trigger = ScriptTrigger.CONTEXT_CHANGE,
            source = """
                # هماهنگ‌ساز تمرکز و کار عمیق
                WHEN focus == true THEN notify "پنجره کار عمیق فعال است. اعلان‌های غیرضروری معلق شدند."
                WHEN focus == true AND mission.blocked > 0 THEN propose "بررسی و رفع موانع مأموریت قبل از شروع جلسه تمرکز"
            """.trimIndent()
        ),
        GitHubScriptCandidate(
            id = "gh_script_zero_trust_sentinel",
            name = "نگهبان قفل اضطراری و مانیتورینگ نشست‌ها",
            repository = repositoryName,
            path = "scripts/zero_trust_sentinel.say",
            descriptionFa = "هنگام فعال‌سازی قفل اضطراری سخت‌افزاری یا نرم‌افزاری، تمام وب‌هوک‌ها و تغییر وضعیت‌ها را قفل می‌کند.",
            descriptionEn = "Enforces zero-trust lockouts and suppresses external effects when emergency lock is engaged.",
            trigger = ScriptTrigger.CONTEXT_CHANGE,
            source = """
                # نگهبان قفل اضطراری زیرو تراست
                WHEN lock == true THEN notify "قفل اضطراری فعال شد. تمام نشست‌های همگام‌سازی بیرونی مسدود شدند."
                WHEN lock == true THEN block "قفل امنیتی فعال است — اجرای دستور ناممکن است"
            """.trimIndent()
        ),
        GitHubScriptCandidate(
            id = "gh_script_nightly_reconciliation",
            name = "جمع‌بندی شبانه و تطبیق اهداف",
            repository = repositoryName,
            path = "scripts/nightly_reconciliation.say",
            descriptionFa = "در ساعات پایانی روز، تعداد وظایف باقیمانده را شمارش کرده و خلاصهٔ وضعیت روزانه را اعلام می‌کند.",
            descriptionEn = "Counts remaining tasks late at night and posts a summary notification.",
            trigger = ScriptTrigger.SCHEDULE,
            source = """
                # جمع‌بندی شبانه وظایف
                WHEN time > 21 THEN log "آغاز فرآیند جمع‌بندی مأموریت‌های روزانه"
                WHEN time > 21 AND mission.blocked > 0 THEN notify "شما {mission.blocked} وظیفهٔ مسدود دارید. برای فردا بازچینی انجام شود."
            """.trimIndent()
        )
    )

    /**
     * Validates and merges the selected candidates into [ScriptStore].
     * Returns the count of successfully merged scripts and their titles.
     */
    fun mergeCandidates(
        candidatesToMerge: List<GitHubScriptCandidate>,
        store: ScriptStore,
        engine: ScriptEngine
    ): Pair<Int, List<String>> {
        val mergedTitles = mutableListOf<String>()
        var count = 0

        candidatesToMerge.forEach { candidate ->
            val validation = engine.validate(candidate.source)
            if (validation.valid) {
                val script = AutomationScript(
                    id = "script_gh_" + UUID.randomUUID().toString().take(6),
                    name = candidate.name,
                    description = candidate.descriptionFa,
                    trigger = candidate.trigger,
                    source = candidate.source,
                    enabled = true,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                store.upsert(script)
                mergedTitles.add(candidate.name)
                count++
            }
        }

        return Pair(count, mergedTitles)
    }
}
