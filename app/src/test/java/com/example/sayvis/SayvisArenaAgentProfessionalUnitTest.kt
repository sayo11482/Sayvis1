package com.example.sayvis

import com.example.sayvis.agent.ArenaAgent
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/**
 * Professional-grade bug-free guarantee for Arena graphical agent.
 * Covers edge cases, input validation, state consistency and error handling.
 * Every test is deterministic and must pass before any release.
 */
class SayvisArenaAgentProfessionalUnitTest {

    // ---- blank / edge input handling ----

    @Test
    fun `blank goal returns safe no-op plan`() {
        val plan = ArenaAgent.plan("   ")
        assertEquals(1, plan.steps.size)
        assertEquals(ArenaAgent.ToolKind.DONE, plan.steps[0].tool)
        assertEquals(0, plan.estimatedMinutes)
        assertTrue(plan.goal.isEmpty())
    }

    @Test
    fun `empty goal html still valid`() {
        val html = ArenaAgent.generateHtmlPreview("")
        assertTrue(html.contains("SAYVIS Arena"))
        assertTrue(html.contains("<html"))
        assertTrue(html.contains("LIVE PREVIEW"))
    }

    @Test
    fun `very long goal is truncated safely`() {
        val longGoal = "سایت ".repeat(200) // 1000 chars
        val plan = ArenaAgent.plan(longGoal)
        assertTrue(plan.goal.length == longGoal.trim().length) // plan stores original trimmed
        val html = ArenaAgent.generateHtmlPreview(longGoal)
        // title is taken as take(40) so html must not contain the full 1000 chars in title tag
        val titleTag = html.substringAfter("<title>").substringBefore("</title>")
        assertTrue(titleTag.length <= 60) // 40 + " — SAYVIS Arena"
        assertTrue(html.length > 1000) // but body still works
    }

    @Test
    fun `special characters in goal do not break html`() {
        val goal = "<script>alert('xss')</script> & \"quotes\" 'single' — سایت"
        val html = ArenaAgent.generateHtmlPreview(goal)
        // The title is inserted verbatim (no escaping) but must not break the HTML structure
        // At minimum the file must still contain the required tags
        assertTrue(html.contains("<html"))
        assertTrue(html.contains("</html>"))
        assertTrue(html.contains("● LIVE PREVIEW"))
    }

    // ---- plan correctness for each domain ----

    @Test
    fun `web plan has six steps and correct order`() {
        val plan = ArenaAgent.plan("یک سایت و لندینگ html بساز")
        assertEquals(6, plan.steps.size)
        assertEquals(ArenaAgent.ToolKind.READ, plan.steps[0].tool)
        assertEquals(ArenaAgent.ToolKind.WEB_SEARCH, plan.steps[1].tool)
        assertEquals(ArenaAgent.ToolKind.WRITE, plan.steps[2].tool)
        assertEquals(ArenaAgent.ToolKind.PREVIEW, plan.steps[3].tool)
        assertEquals(ArenaAgent.ToolKind.EDIT, plan.steps[4].tool)
        assertEquals(ArenaAgent.ToolKind.DONE, plan.steps[5].tool)
        assertEquals(8, plan.estimatedMinutes)
    }

    @Test
    fun `app plan uses kotlin compose skeleton`() {
        val plan = ArenaAgent.plan("اپ اندروید مدیریت مأموریت بساز")
        assertTrue(plan.steps.any { it.description.contains("MainScreen") })
        assertTrue(plan.steps.any { it.tool == ArenaAgent.ToolKind.PREVIEW })
        assertEquals(8, plan.estimatedMinutes)
    }

    @Test
    fun `image plan uses generate image`() {
        val plan = ArenaAgent.plan("image گرافیک برای اینستاگرام")
        assertTrue(plan.steps.any { it.tool == ArenaAgent.ToolKind.GENERATE_IMAGE })
        assertTrue(plan.steps.any { it.tool == ArenaAgent.ToolKind.IMAGE_SEARCH })
        assertEquals(5, plan.estimatedMinutes)
    }

    @Test
    fun `data plan builds dashboard`() {
        val plan = ArenaAgent.plan("تحلیل داده نمودار طلا chart بساز")
        assertTrue(plan.steps.any { it.description.contains("dashboard") })
        assertTrue(plan.steps.any { it.tool == ArenaAgent.ToolKind.WEB_SEARCH })
        assertEquals(6, plan.estimatedMinutes)
    }

    @Test
    fun `fallback plan for unknown goal still has preview`() {
        val plan = ArenaAgent.plan("برنامه ریزی سفر به کیش")
        assertTrue(plan.steps.any { it.tool == ArenaAgent.ToolKind.PREVIEW })
        assertTrue(plan.steps.last().tool == ArenaAgent.ToolKind.DONE)
    }

    // ---- mock workspace consistency ----

    @Test
    fun `mock workspace for web contains html and css`() {
        val plan = ArenaAgent.plan("وب سایت فروشگاهی html")
        val tree = ArenaAgent.mockWorkspace(plan)
        val workspace = tree.first()
        assertTrue(workspace.isDir)
        assertTrue(workspace.children.any { it.name == "index.html" && it.content?.contains("<html") == true })
        assertTrue(workspace.children.any { it.name == "style.css" })
        assertTrue(workspace.children.any { it.name == "README.md" })
    }

    @Test
    fun `mock workspace for image contains gallery`() {
        val plan = ArenaAgent.plan("تصویر گرافیک image")
        val tree = ArenaAgent.mockWorkspace(plan)
        val gallery = tree.first().children.firstOrNull { it.name == "gallery" }
        assertNotNull(gallery)
        assertTrue(gallery!!.children.any { it.name == "generated_01.png" })
        assertTrue(gallery.children.any { it.name == "generated_02.png" })
    }

    @Test
    fun `mock workspace for app contains src`() {
        val plan = ArenaAgent.plan("اپ اندروید")
        val tree = ArenaAgent.mockWorkspace(plan)
        val src = tree.first().children.firstOrNull { it.name == "src" }
        assertNotNull(src)
        assertTrue(src!!.children.any { it.name == "Main.kt" })
    }

    // ---- simulateTool correctness ----

    @Test
    fun `simulateTool returns correct graphical type per kind`() = runTest {
        val mapping = mapOf(
            ArenaAgent.ToolKind.READ to ArenaAgent.GraphType.FILE_TREE,
            ArenaAgent.ToolKind.WEB_SEARCH to ArenaAgent.GraphType.WEB_RESULTS,
            ArenaAgent.ToolKind.WRITE to ArenaAgent.GraphType.DIFF,
            ArenaAgent.ToolKind.PREVIEW to ArenaAgent.GraphType.HTML_PREVIEW,
            ArenaAgent.ToolKind.EDIT to ArenaAgent.GraphType.DIFF,
            ArenaAgent.ToolKind.BASH to ArenaAgent.GraphType.TERMINAL,
            ArenaAgent.ToolKind.GENERATE_IMAGE to ArenaAgent.GraphType.IMAGE,
            ArenaAgent.ToolKind.IMAGE_SEARCH to ArenaAgent.GraphType.IMAGE,
            ArenaAgent.ToolKind.DONE to ArenaAgent.GraphType.CANVAS
        )
        for ((kind, expected) in mapping) {
            val step = ArenaAgent.PlanStep("fa", "en", kind, "desc")
            val call = ArenaAgent.simulateTool(step, 0)
            assertEquals("kind $kind", expected, call.graphicalData?.type)
            assertTrue(call.output.isNotBlank())
            assertTrue(call.id.startsWith("call_"))
            assertTrue(call.isSuccess)
            assertFalse(call.isRunning)
        }
    }

    @Test
    fun `simulateTool preview contains valid html`() = runTest {
        val step = ArenaAgent.PlanStep("پیش‌نمایش", "Preview", ArenaAgent.ToolKind.PREVIEW, "render")
        val call = ArenaAgent.simulateTool(step, 3)
        val html = call.graphicalData?.content ?: ""
        assertTrue(html.contains("<html"))
        assertTrue(html.contains("SAYVIS Arena"))
        assertTrue(html.contains("LIVE PREVIEW"))
    }

    @Test
    fun `simulateTool ids are index-based and unique per index`() = runTest {
        val step = ArenaAgent.PlanStep("x", "x", ArenaAgent.ToolKind.BASH, "bash")
        val c0 = ArenaAgent.simulateTool(step, 0)
        val c1 = ArenaAgent.simulateTool(step, 1)
        val c2 = ArenaAgent.simulateTool(step, 2)
        assertEquals("call_0", c0.id)
        assertEquals("call_1", c1.id)
        assertEquals("call_2", c2.id)
        assertNotEquals(c0.id, c1.id)
    }

    @Test
    fun `html preview generation is deterministic and contains goal title`() {
        val goal = "تست تکراری"
        val h1 = ArenaAgent.generateHtmlPreview(goal)
        val h2 = ArenaAgent.generateHtmlPreview(goal)
        assertEquals(h1, h2)
        assertTrue(h1.contains(goal.take(40)))
    }

    @Test
    fun `html preview for different goals differs`() {
        val h1 = ArenaAgent.generateHtmlPreview("سایت کافی‌شاپ")
        val h2 = ArenaAgent.generateHtmlPreview("سایت بوتیک لباس")
        assertNotEquals(h1, h2)
    }

    // ---- plan title bilingual ----

    @Test
    fun `plan step titles bilingual non-empty`() {
        val plan = ArenaAgent.plan("web سایت")
        for (step in plan.steps) {
            assertTrue(step.titleFa.isNotBlank())
            assertTrue(step.titleEn.isNotBlank())
            assertTrue(step.title(true).isNotBlank())
            assertTrue(step.title(false).isNotBlank())
            assertFalse(step.title(true).isBlank())
        }
    }

    @Test
    fun `all toolkinds covered in simulateTool without exception`() = runBlocking {
        // Ensures no enum branch is missing — a common source of bugs when adding new tools
        for (kind in ArenaAgent.ToolKind.entries) {
            val step = ArenaAgent.PlanStep("t", "t", kind, "d")
            val call = ArenaAgent.simulateTool(step, 0)
            assertNotNull(call.graphicalData)
        }
    }

    @Test
    fun `file node structure is valid`() {
        val node = ArenaAgent.FileNode("test.txt", "/test.txt", false, content = "hello", modified = true)
        assertEquals("test.txt", node.name)
        assertFalse(node.isDir)
        assertTrue(node.modified)
        assertEquals("hello", node.content)
        assertTrue(node.children.isEmpty())
    }

    @Test
    fun `graph type enum covers all canvas cases`() {
        // Ensures ArenaCanvas when branches are exhaustive
        val types = ArenaAgent.GraphType.entries
        assertTrue(types.contains(ArenaAgent.GraphType.TERMINAL))
        assertTrue(types.contains(ArenaAgent.GraphType.FILE_TREE))
        assertTrue(types.contains(ArenaAgent.GraphType.DIFF))
        assertTrue(types.contains(ArenaAgent.GraphType.HTML_PREVIEW))
        assertTrue(types.contains(ArenaAgent.GraphType.IMAGE))
        assertTrue(types.contains(ArenaAgent.GraphType.WEB_RESULTS))
        assertTrue(types.contains(ArenaAgent.GraphType.CANVAS))
        assertEquals(7, types.size)
    }

    @Test
    fun `html escaping prevents broken structure for adversarial input`() {
        val adversarial = "<script>alert(1)</script>"
        val html = ArenaAgent.generateHtmlPreview(adversarial)
        assertFalse(html.contains("<script>alert(1)</script>"))
        assertTrue(html.contains("&lt;script&gt;"))
        assertTrue(html.contains("&amp;") || html.contains("&lt;"))
        assertTrue(html.contains("<html"))
        assertTrue(html.contains("</html>"))
    }

    @Test
    fun `escapeHtml is correct for all special chars`() {
        assertEquals("&lt;div&gt;", ArenaAgent.escapeHtml("<div>"))
        assertEquals("&amp;", ArenaAgent.escapeHtml("&"))
        assertEquals("&quot;", ArenaAgent.escapeHtml("""))
        assertEquals("&#39;", ArenaAgent.escapeHtml("'"))
        assertEquals("a &amp; b", ArenaAgent.escapeHtml("a & b"))
    }

}
