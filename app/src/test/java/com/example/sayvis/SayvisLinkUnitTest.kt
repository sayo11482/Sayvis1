package com.example.sayvis

import com.example.sayvis.ai.AssistantCommandEngine
import com.example.sayvis.ai.CognitiveIngest
import com.example.sayvis.agent.MissionPlanner
import com.example.sayvis.model.UicCategory
import com.example.sayvis.net.LinkCenter
import com.example.sayvis.net.SayvisNet
import com.example.sayvis.scripts.GithubIntegrator
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SAYVIS v5.3.0 — Link Center, status codes, mission planner, cognitive
 * ingest, GitHub integrator scoring and the alarm command — all pure or
 * gate-level, running on the JVM.
 */
class SayvisLinkUnitTest {

    // ------------------------------------------------------------ gate (قطع کامل)

    @Test
    fun gate_offline_blocks_every_request_with_offline_exception() {
        LinkCenter.setGateOpen(false)
        try {
            val request = okhttp3.Request.Builder().url("http://127.0.0.1:9/").build()
            val thrown = runCatching {
                SayvisNet.shared.newCall(request).execute()
            }.exceptionOrNull()
            assertNotNull("request must not pass the closed gate", thrown)
            assertTrue(
                "expected OfflineException but got ${thrown?.javaClass?.simpleName}",
                thrown is LinkCenter.OfflineException
            )
        } finally {
            LinkCenter.setGateOpen(true)
        }
    }

    @Test
    fun gate_online_does_not_throw_offline_exception() {
        LinkCenter.setGateOpen(true)
        val request = okhttp3.Request.Builder().url("http://127.0.0.1:9/").build()
        val thrown = runCatching {
            SayvisNet.shared.newCall(request).execute()
        }.exceptionOrNull()
        // A local connect failure is fine — what must NOT happen is the gate veto.
        assertFalse(thrown is LinkCenter.OfflineException)
    }

    // ------------------------------------------------------------ speed

    @Test
    fun speed_mbps_math() {
        assertEquals(8.0, LinkCenter.computeMbps(1_000_000, 1_000)!!, 0.01)
        assertEquals(80.0, LinkCenter.computeMbps(10_000_000, 1_000)!!, 0.01)
        assertNull(LinkCenter.computeMbps(0, 100))
        assertNull(LinkCenter.computeMbps(1000, 0))
        assertNull(LinkCenter.computeMbps(-5, 100))
    }

    // ------------------------------------------------------------ codes (کد ۰۱)

    @Test
    fun status_code_01_is_thinking_and_labels_are_owner_friendly() {
        assertEquals("01", LinkCenter.CODE.THINKING.code)
        assertTrue(LinkCenter.CODE.THINKING.fa.contains("فکر"))
        assertTrue(LinkCenter.CODE.OFFLINE_SWITCH.fa.contains("قطع"))
        assertEquals(11, LinkCenter.CODE.entries.size)
    }

    @Test
    fun status_history_is_capped_and_newest_first() {
        repeat(32) { LinkCenter.push(LinkCenter.CODE.MARKET_LIVE, "h$it") }
        val history = LinkCenter.statusHistory.value
        assertEquals(30, history.size)
        assertEquals("h31", history.first().detail)
    }

    // ------------------------------------------------------------ mission planner

    @Test
    fun planner_extracts_bulleted_steps_as_tasks() {
        val report = """
            برای رسیدن به هدف این‌ها لازم است:
            - حساب آزمایشی در بروکر بساز
            ۱. کتابخانهٔ نمودار را نصب کن
            • ثبت تنظیمات و اجرای اولین بک‌تست ضروری است
            این جمله خیلی طولانی است و تکرار می‌شود این جمله خیلی طولانی است و تکرار می‌شود این جمله خیلی طولانی است و تکرار می‌شود این جمله خیلی طولانی است و تکرار می‌شود
        """.trimIndent()
        val tasks = MissionPlanner.tasksFromReport(report)
        assertTrue(tasks.size >= 2)
        assertTrue(tasks.any { it.contains("حساب آزمایشی") })
        assertTrue(tasks.any { it.contains("کتابخانه") })
    }

    @Test
    fun planner_title_truncates() {
        val long = "ی".repeat(120)
        assertTrue(MissionPlanner.titleFromGoal(long).length <= 49)
        assertEquals("هدف روشن", MissionPlanner.titleFromGoal("هدف روشن"))
    }

    // ------------------------------------------------------------ cognitive ingest

    @Test
    fun ingest_alarm_creates_habit_attribute() {
        val out = CognitiveIngest.extract(CognitiveIngest.Source.ALARM, "آلارم ۶:۳۰ صبح‌ها")
        assertEquals(1, out.size)
        assertEquals(UicCategory.HABITS, out[0].category)
        assertEquals("alarm_pattern", out[0].key)
    }

    @Test
    fun ingest_note_classifies_preference_goal_and_habit() {
        val pref = CognitiveIngest.extract(CognitiveIngest.Source.NOTE, "دوست دارم ترید را شب انجام بدهم")
        assertEquals(UicCategory.PREFERENCES, pref[0].category)
        val habit = CognitiveIngest.extract(CognitiveIngest.Source.NOTE, "هر روز صبح چارت را بررسی می‌کنم")
        assertEquals(UicCategory.HABITS, habit[0].category)
        val goal = CognitiveIngest.extract(CognitiveIngest.Source.NOTE, "هدف من رسیدن به حساب ۱۰هزار دلاری است")
        assertEquals(UicCategory.GOALS, goal[0].category)
    }

    @Test
    fun ingest_search_keywords_strip_stopwords() {
        val kw = CognitiveIngest.keywords("قیمت طلا و دلار در بازار")
        assertTrue(kw.contains("طلا"))
        assertFalse(kw.contains("و"))
        assertTrue(kw.size <= 3)
    }

    @Test
    fun ingest_merge_key_is_stable_per_day_bucket() {
        val a = CognitiveIngest.mergeKey(CognitiveIngest.Source.SEARCH, "search_interest", 7)
        val b = CognitiveIngest.mergeKey(CognitiveIngest.Source.SEARCH, "search_interest", 7)
        assertEquals(a, b)
        assertTrue(CognitiveIngest.dayBucket(86_400_000L * 365) > 0)
    }

    // ------------------------------------------------------------ github scoring

    @Test
    fun github_score_flags_kotlin_trading_special_items() {
        val s = GithubIntegrator.score("A kotlin trading engine with market data", "Kotlin", "MIT", 1500, listOf("trading", "kotlin"))
        assertTrue(s >= GithubIntegrator.SPECIAL_THRESHOLD)
        assertTrue(GithubIntegrator.score("", "", "", 3, emptyList()) < GithubIntegrator.SPECIAL_THRESHOLD)
        val gpl = GithubIntegrator.score("kotlin trading engine", "Kotlin", "GPL-3.0", 1500, listOf("trading"))
        assertTrue(gpl < s)
    }

    // ------------------------------------------------------------ alarm command

    @Test
    fun alarm_parses_persian_clock_duration_and_english() {
        val c1 = AssistantCommandEngine.parseAlarm("آلارم ساعت ۶:۳۰ بذار")
        assertNotNull(c1)
        assertEquals(6, c1!!.hour)
        assertEquals(30, c1.minute)

        val c2 = AssistantCommandEngine.parseAlarm("۲۰ دقیقه دیگر یادآوری کن")
        assertNotNull(c2)
        assertEquals(20, c2!!.inMinutes)

        val c3 = AssistantCommandEngine.parseAlarm("remind me in 15 minutes")
        assertNotNull(c3)
        assertEquals(15, c3!!.inMinutes)

        val c4 = AssistantCommandEngine.parseAlarm("set alarm at 06:45")
        assertNotNull(c4)
        assertEquals(6, c4!!.hour)
        assertEquals(45, c4.minute)

        assertNull(AssistantCommandEngine.parseAlarm("ساعت چنده؟"))
        assertNull(AssistantCommandEngine.parseAlarm("یادت باشه من چای دوست دارم"))
    }

    @Test
    fun digits_normalisation_covers_persian_and_arabic() {
        assertEquals("6:30", AssistantCommandEngine.toEnglishDigits("۶:۳۰"))
        assertEquals("12", AssistantCommandEngine.toEnglishDigits("١٢"))
    }

    // ------------------------------------------------------------ ai memory defaults

    @Test
    fun ai_memory_fields_default_to_unlinked() {
        val settings = com.example.sayvis.settings.AppSettings()
        assertFalse(settings.ai.aiLinkedOnce)
        assertEquals(0, settings.ai.aiLinkedAt)
        assertEquals("", settings.ai.aiLinkedModel)
    }
}
