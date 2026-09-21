package com.example.sayvis

import com.example.sayvis.ai.AssistantCommand
import com.example.sayvis.ai.AssistantCommandEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the assistant's command-analysis layer: Persian/English intent
 * recognition, slot filling, safe arithmetic and the decision that free-form
 * questions fall through to the AI provider.
 */
class SayvisCommandEngineUnitTest {

    private fun parse(text: String): AssistantCommand? = AssistantCommandEngine.parse(text)

    // --------------------------------------------------------- mission create

    @Test
    fun `parses persian mission creation with task count`() {
        val cmd = parse("سایو لطفا یک مأموریت بساز خرید هفتگی با ۳ وظیفه")
        assertTrue(cmd is AssistantCommand.CreateMission)
        cmd as AssistantCommand.CreateMission
        assertEquals("خرید هفتگی", cmd.title)
        assertEquals(3, cmd.taskCount)
    }

    @Test
    fun `parses english mission creation`() {
        val cmd = parse("create mission quarterly tax review with 2 tasks")
        assertTrue(cmd is AssistantCommand.CreateMission)
        cmd as AssistantCommand.CreateMission
        assertEquals("quarterly tax review", cmd.title)
        assertEquals(2, cmd.taskCount)
    }

    @Test
    fun `flags urgent missions`() {
        val cmd = parse("مأموریت بساز تحویل گزارش فوری")
        assertTrue(cmd is AssistantCommand.CreateMission)
        assertTrue((cmd as AssistantCommand.CreateMission).urgent)
    }

    // --------------------------------------------------------------- memory

    @Test
    fun `parses remember command with the fact slot`() {
        val cmd = parse("یادت باشه که جلسهٔ فردا ساعت ۹ است")
        assertTrue(cmd is AssistantCommand.Remember)
        assertEquals("جلسهٔ فردا ساعت 9 است", (cmd as AssistantCommand.Remember).fact)
    }

    @Test
    fun `parses english remember command`() {
        val cmd = parse("remember that the server password rotates monthly")
        assertTrue(cmd is AssistantCommand.Remember)
        assertEquals("the server password rotates monthly", (cmd as AssistantCommand.Remember).fact)
    }

    @Test
    fun `parses recall command`() {
        val cmd = parse("یادت هست جلسه؟")
        assertTrue(cmd is AssistantCommand.Recall)
        assertEquals("جلسه؟", (cmd as AssistantCommand.Recall).query)
    }

    // ------------------------------------------------------------- calculator

    @Test
    fun `parses persian calculation with persian digits and times sign`() {
        val cmd = parse("حساب کن ۱۲×۳+۵")
        assertTrue(cmd is AssistantCommand.Calculate)
        assertEquals(41.0, (cmd as AssistantCommand.Calculate).value, 1e-9)
    }

    @Test
    fun `parses english what-is calculation`() {
        val cmd = parse("what is 8/2")
        assertTrue(cmd is AssistantCommand.Calculate)
        assertEquals(4.0, (cmd as AssistantCommand.Calculate).value, 1e-9)
    }

    @Test
    fun `bare arithmetic is recognised`() {
        val cmd = parse("3.5*(2+1)")
        assertTrue(cmd is AssistantCommand.Calculate)
        assertEquals(10.5, (cmd as AssistantCommand.Calculate).value, 1e-9)
    }

    @Test
    fun `safe math respects precedence and unary minus`() {
        assertEquals(14.0, AssistantCommandEngine.SafeMath.evaluate("2+3*4")!!, 1e-9)
        assertEquals(-6.0, AssistantCommandEngine.SafeMath.evaluate("2*-3")!!, 1e-9)
        assertEquals(8.0, AssistantCommandEngine.SafeMath.evaluate("2^3")!!, 1e-9)
        assertNull(AssistantCommandEngine.SafeMath.evaluate("5/0"))
        assertNull(AssistantCommandEngine.SafeMath.evaluate("(2+3"))
    }

    // ------------------------------------------------------- system commands

    @Test
    fun `status report and mission list`() {
        assertTrue(parse("وضعیت رو گزارش بده") is AssistantCommand.StatusReport)
        assertTrue(parse("report status") is AssistantCommand.StatusReport)
        assertTrue(parse("مأموریت‌هامو نشون بده") is AssistantCommand.ShowMissions)
        assertTrue(parse("show missions") is AssistantCommand.ShowMissions)
    }

    @Test
    fun `emergency lock intents parse with their direction`() {
        assertEquals(AssistantCommand.ToggleEmergencyLock(true), parse("قفل اضطراری را فعال کن"))
        assertEquals(AssistantCommand.ToggleEmergencyLock(false), parse("قفل اضطراری رو بردار"))
        assertEquals(AssistantCommand.ToggleEmergencyLock(true), parse("engage emergency lock"))
    }

    @Test
    fun `offline toggle is deprecated — sayvis is always online, offline phrases fall through to sovereign core`() {
        // SAYVIS has no offline mode — these must NOT be parsed as ToggleOffline; they flow to the AI.
        assertNull(parse("حالت آفلاین را روشن کن"))
        assertNull(parse("disable offline mode"))
        assertNull(parse("حالت افلاین را خاموش کن"))
        assertNull(parse("enable offline mode"))
    }

    @Test
    fun `navigation maps persian and english targets`() {
        assertEquals(AssistantCommand.Navigate("SETTINGS"), parse("باز کن تنظیمات"))
        assertEquals(AssistantCommand.Navigate("MISSIONS"), parse("برو به مأموریت‌ها"))
        assertEquals(AssistantCommand.Navigate("AVATAR"), parse("open the avatar settings"))
    }

    @Test
    fun `time date battery and scan`() {
        assertTrue(parse("ساعت چنده؟") is AssistantCommand.TimeQuery)
        assertTrue(parse("تاریخ امروز چنده؟") is AssistantCommand.DateQuery)
        assertTrue(parse("باتری چقدره؟") is AssistantCommand.BatteryQuery)
        assertTrue(parse("اسکن کن") is AssistantCommand.RunAwareScan)
        assertTrue(parse("run a scan") is AssistantCommand.RunAwareScan)
    }

    @Test
    fun `help is recognised`() {
        assertTrue(parse("چیکار میتونی بکنی؟") is AssistantCommand.Help)
        assertTrue(parse("what can you do?") is AssistantCommand.Help)
    }

    // ------------------------------------------------------------- fallthrough

    @Test
    fun `open questions fall through to the ai provider`() {
        assertNull(parse("نظرت دربارهٔ اقتصاد ایران چیه؟"))
        assertNull(parse("explain quantum computing in simple words"))
        assertNull(parse(""))
    }

    @Test
    fun `normalizer unifies arabic yeh kaf and persian digits`() {
        assertEquals("ماموریت12", AssistantCommandEngine.normalize("مأموريت۱۲"))
        assertTrue(AssistantCommandEngine.normalize("كيست").contains("کیست"))
    }
}
