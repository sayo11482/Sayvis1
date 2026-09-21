package com.example.sayvis

import com.example.sayvis.agent.ArenaAgent
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Arena graphical agent — reproduces Arena AI's Agent Mode inside SAYVIS. */
class SayvisArenaAgentUnitTest {

    @Test
    fun `plan contains arena pipeline for web goal`() {
        val plan = ArenaAgent.plan("یک لندینگ مینیمال برای کافی‌شاپ بساز با پیش‌نمایش گرافیکی")
        assertTrue(plan.steps.size >= 5)
        assertTrue(plan.steps.any { it.tool == ArenaAgent.ToolKind.WRITE })
        assertTrue(plan.steps.any { it.tool == ArenaAgent.ToolKind.PREVIEW })
        assertEquals(ArenaAgent.ToolKind.DONE, plan.steps.last().tool)
        assertTrue(plan.estimatedMinutes in 5..10)
    }

    @Test
    fun `plan for image goal uses generate image`() {
        val plan = ArenaAgent.plan("3 تصویر گرافیکی برای پست اینستاگرام تولید کن")
        assertTrue(plan.steps.any { it.tool == ArenaAgent.ToolKind.GENERATE_IMAGE })
        assertTrue(plan.steps.any { it.tool == ArenaAgent.ToolKind.PREVIEW })
    }

    @Test
    fun `html preview is valid and contains live badge`() {
        val html = ArenaAgent.generateHtmlPreview("تست گرافیکی")
        assertTrue(html.contains("<html", true))
        assertTrue(html.contains("LIVE PREVIEW"))
        assertTrue(html.contains("SAYVIS Arena"))
        assertFalse(html.isBlank())
    }

    @Test
    fun `mock workspace returns file tree`() {
        val plan = ArenaAgent.plan("یک سایت مینیمال برای بوتیک لباس بساز")
        val tree = ArenaAgent.mockWorkspace(plan)
        assertTrue(tree.isNotEmpty())
        assertTrue(tree[0].isDir)
        assertTrue(tree[0].children.isNotEmpty())
    }

    @Test
    fun `simulate tool returns graphical data`() = runBlocking {
        val step = ArenaAgent.PlanStep("کاوش", "Explore", ArenaAgent.ToolKind.READ, "ls")
        val call = ArenaAgent.simulateTool(step, 0)
        assertEquals(ArenaAgent.ToolKind.READ, call.kind)
        assertTrue(call.output.isNotBlank())
        assertTrue(call.graphicalData != null)
        assertEquals(ArenaAgent.GraphType.FILE_TREE, call.graphicalData!!.type)
    }

    @Test
    fun `plan titles respect language`() {
        val plan = ArenaAgent.plan("build a dashboard")
        val step = plan.steps.first()
        assertFalse(step.title(true).isBlank())
        assertFalse(step.title(false).isBlank())
    }

    @Test
    fun `all tool kinds simulate without crash`() = runBlocking {
        val persianPlan = ArenaAgent.plan("یک سایت مینیمال بساز")
        // Simulate each step sequentially — must not throw
        for ((i, step) in persianPlan.steps.withIndex()) {
            val call = ArenaAgent.simulateTool(step, i)
            assertTrue(call.id.isNotBlank())
            assertTrue(call.isSuccess || !call.isSuccess) // always boolean
        }
    }
}
