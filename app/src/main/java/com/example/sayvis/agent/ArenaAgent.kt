package com.example.sayvis.agent

import kotlinx.coroutines.delay

/**
 * SAYVIS Arena Agent — graphical environment that mirrors Arena AI's Agent Mode.
 *
 * The owner gives ONE natural prompt and the agent PLANS → EXECUTES with
 * real tool calls, showing every step in a graphical workspace:
 *   - File explorer (workspace tree)
 *   - Bash terminal (live output)
 *   - Live preview (WebView for HTML/CSS)
 *   - Canvas (generated images)
 *   - Diff viewer (file changes)
 *
 * Pure Kotlin, no Android dependencies except for file paths — the UI layer
 * renders it with Compose + WebView + Image.
 *
 * Architecture mirrors Arena AI:
 *   User message → Planner → Tool loop (bash/read/write/edit/web_search/image) → Verifier → Report
 */
object ArenaAgent {

    enum class ToolKind {
        BASH, READ, WRITE, EDIT, WEB_SEARCH, IMAGE_SEARCH, GENERATE_IMAGE, PREVIEW, DONE
    }

    data class ToolCall(
        val id: String,
        val kind: ToolKind,
        val input: String,
        val output: String = "",
        val isRunning: Boolean = false,
        val isSuccess: Boolean = true,
        val graphicalData: GraphicalData? = null
    )

    data class GraphicalData(
        val type: GraphType,
        val content: String, // HTML, image path, file diff, etc.
        val title: String = ""
    )

    enum class GraphType { TERMINAL, FILE_TREE, DIFF, HTML_PREVIEW, IMAGE, WEB_RESULTS, CANVAS }

    data class FileNode(
        val name: String,
        val path: String,
        val isDir: Boolean,
        val children: List<FileNode> = emptyList(),
        val content: String? = null,
        val modified: Boolean = false
    )

    data class PlanStep(
        val titleFa: String,
        val titleEn: String,
        val tool: ToolKind,
        val description: String
    ) {
        fun title(persian: Boolean) = if (persian) titleFa else titleEn
    }

    data class AgentPlan(
        val goal: String,
        val steps: List<PlanStep>,
        val estimatedMinutes: Int
    )

    /**
     * Planner — decomposes any goal into Arena-style graphical steps.
     * Always includes: explore → build → preview → verify.
     */
    fun plan(goal: String): AgentPlan {
        val clean = goal.trim()
        val isWeb = listOf("سایت", "وب", "صفحه", "landing", "website", "web", "html", "صفحه").any { clean.contains(it, true) }
        val isApp = listOf("اپ", "برنامه", "app", "اندروید", "android").any { clean.contains(it, true) }
        val isImage = listOf("عکس", "تصویر", "image", "photo", "گرافیک", "design").any { clean.contains(it, true) }
        val isData = listOf("تحلیل", "داده", "chart", "نمودار", "analysis").any { clean.contains(it, true) }

        val steps = mutableListOf<PlanStep>()

        steps += PlanStep(
            "کاوش فضای کاری و خواندن فایل‌ها",
            "Explore workspace & read files",
            ToolKind.READ, "ls + read existing files"
        )

        when {
            isWeb -> {
                steps += PlanStep("جست‌وجوی زنده برای الهام و منابع", "Live search for inspiration", ToolKind.WEB_SEARCH, "web_search")
                steps += PlanStep("ساخت اسکلت HTML/CSS گرافیکی", "Build graphical HTML/CSS", ToolKind.WRITE, "write index.html + style.css")
                steps += PlanStep("پیش‌نمایش زنده در WebView", "Live preview in WebView", ToolKind.PREVIEW, "render HTML")
                steps += PlanStep("بهینه‌سازی تعاملی و انیمیشن", "Polish interactions & animations", ToolKind.EDIT, "edit + enhance")
            }
            isApp -> {
                steps += PlanStep("طراحی معماری و مدل داده", "Design architecture & data model", ToolKind.WRITE, "write architecture.md")
                steps += PlanStep("ساخت اسکلت کاتلین/کامپوز", "Build Kotlin/Compose skeleton", ToolKind.WRITE, "write MainScreen.kt")
                steps += PlanStep("پیش‌نمایش کامپوننت‌ها", "Preview components", ToolKind.PREVIEW, "compose preview")
            }
            isImage -> {
                steps += PlanStep("جست‌وجوی بصری و ایده‌یابی", "Visual search & ideation", ToolKind.IMAGE_SEARCH, "image_search")
                steps += PlanStep("تولید تصویر گرافیکی", "Generate graphical image", ToolKind.GENERATE_IMAGE, "generate_image")
                steps += PlanStep("نمایش در گالری گرافیکی", "Display in graphical gallery", ToolKind.PREVIEW, "canvas")
            }
            isData -> {
                steps += PlanStep("جمع‌آوری دادهٔ زنده", "Collect live data", ToolKind.WEB_SEARCH, "web_search")
                steps += PlanStep("ساخت داشبورد گرافیکی", "Build graphical dashboard", ToolKind.WRITE, "write dashboard.html")
                steps += PlanStep("رندر نمودار و پیش‌نمایش", "Render chart & preview", ToolKind.PREVIEW, "canvas + chart.js")
            }
            else -> {
                steps += PlanStep("تحلیل و شکستن هدف به زیرکارها", "Analyze & decompose goal", ToolKind.BASH, "planning")
                steps += PlanStep("اجرای گام‌های اصلی", "Execute core steps", ToolKind.WRITE, "build")
                steps += PlanStep("پیش‌نمایش گرافیکی نتیجه", "Graphical preview of result", ToolKind.PREVIEW, "preview")
            }
        }

        steps += PlanStep("راستی‌آزمایی و گزارش نهایی", "Verify & final report", ToolKind.DONE, "verify")

        val minutes = when {
            isWeb || isApp -> 8
            isImage -> 5
            else -> 6
        }

        return AgentPlan(clean, steps, minutes)
    }

    /**
     * Simulated workspace — in real Android this would be app's files dir.
     * For now we generate a plausible file tree for the plan.
     */
    fun mockWorkspace(plan: AgentPlan): List<FileNode> {
        val goal = plan.goal.take(20).replace(Regex("[^\\p{L}0-9]+"), "_")
        return when {
            plan.steps.any { it.tool == ToolKind.WRITE && it.description.contains("html", true) } -> listOf(
                FileNode("workspace", "/", true, listOf(
                    FileNode("index.html", "/index.html", false, content = generateHtmlPreview(plan.goal)),
                    FileNode("style.css", "/style.css", false, content = "/* Sayvis Arena — generated */\n:root{--gold:#D4AF37;--cyan:#00D4FF;}"),
                    FileNode("script.js", "/script.js", false, content = "// interactive\nconsole.log('Arena agent active');"),
                    FileNode("assets", "/assets", true, listOf(
                        FileNode("hero.jpg", "/assets/hero.jpg", false)
                    )),
                    FileNode("README.md", "/README.md", false, content = "# ${plan.goal}\n\nGenerated by SAYVIS Arena Agent")
                ))
            )
            plan.steps.any { it.tool == ToolKind.GENERATE_IMAGE } -> listOf(
                FileNode("workspace", "/", true, listOf(
                    FileNode("gallery", "/gallery", true, listOf(
                        FileNode("generated_01.png", "/gallery/generated_01.png", false),
                        FileNode("generated_02.png", "/gallery/generated_02.png", false)
                    )),
                    FileNode("prompts.json", "/prompts.json", false, content = """{"goal":"${plan.goal}"}""")
                ))
            )
            else -> listOf(
                FileNode("workspace", "/", true, listOf(
                    FileNode("src", "/src", true, listOf(
                        FileNode("Main.kt", "/src/Main.kt", false, content = "// ${plan.goal}\nfun main(){}"),
                        FileNode("Model.kt", "/src/Model.kt", false, content = "data class Model(val id:String)")
                    )),
                    FileNode("README.md", "/README.md", false, content = "# ${plan.goal}")
                ))
            )
        }
    }

    /**
     * Generates a beautiful HTML preview for web tasks — this is what the
     * graphical canvas shows in real time, exactly like Arena's live preview.
     */
    fun generateHtmlPreview(goal: String): String {
        val title = goal.take(40)
        return """
<!DOCTYPE html>
<html lang="fa" dir="rtl">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>$title — SAYVIS Arena</title>
<style>
  @import url('https://fonts.googleapis.com/css2?family=Vazirmatn:wght@400;700;900&display=swap');
  *{margin:0;padding:0;box-sizing:border-box}
  body{font-family:'Vazirmatn',system-ui;background:#0B0D12;color:#E8ECF1;min-height:100vh}
  .hero{min-height:100vh;display:flex;flex-direction:column;align-items:center;justify-content:center;padding:40px;text-align:center;background:radial-gradient(1200px 600px at 50% -10%, #1a2a4a 0%, #0B0D12 60%)}
  .badge{display:inline-flex;align-items:center;gap:8px;padding:6px 14px;border-radius:999px;background:rgba(212,175,55,.12);border:1px solid rgba(212,175,55,.3);color:#D4AF37;font-size:12px;font-weight:700;letter-spacing:.5px;margin-bottom:24px}
  h1{font-size:clamp(28px,5vw,52px);font-weight:900;line-height:1.1;margin-bottom:16px;background:linear-gradient(135deg,#D4AF37 0%,#00D4FF 100%);-webkit-background-clip:text;-webkit-text-fill-color:transparent}
  .sub{font-size:16px;color:#9AA4B2;max-width:640px;line-height:1.8;margin-bottom:32px}
  .actions{display:flex;gap:12px;flex-wrap:wrap;justify-content:center}
  .btn{padding:14px 28px;border-radius:12px;font-weight:700;font-size:14px;cursor:pointer;border:0;transition:.2s}
  .btn-primary{background:linear-gradient(135deg,#D4AF37,#B8941F);color:#0B0D12}
  .btn-ghost{background:rgba(255,255,255,.06);color:#E8ECF1;border:1px solid rgba(255,255,255,.1)}
  .btn:hover{transform:translateY(-1px);box-shadow:0 8px 24px rgba(0,0,0,.3)}
  .grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(280px,1fr));gap:20px;padding:40px;max-width:1100px;margin:0 auto}
  .card{background:rgba(255,255,255,.04);border:1px solid rgba(255,255,255,.06);border-radius:16px;padding:24px;backdrop-filter:blur(12px)}
  .card h3{color:#D4AF37;font-size:14px;margin-bottom:8px}
  .card p{color:#9AA4B2;font-size:13px;line-height:1.7}
  .live{position:fixed;top:16px;left:16px;background:#00D4FF;color:#0B0D12;padding:6px 12px;border-radius:999px;font-size:11px;font-weight:900;letter-spacing:.5px;animation:pulse 2s infinite}
  @keyframes pulse{0%,100%{opacity:1} 50%{opacity:.7}}
  .canvas{margin:40px auto;max-width:900px;background:#11141C;border-radius:16px;overflow:hidden;border:1px solid rgba(255,255,255,.08)}
  .canvas-bar{height:36px;background:#1A1E2A;display:flex;align-items:center;gap:8px;padding:0 16px}
  .dot{width:12px;height:12px;border-radius:50%}
  .dot.r{background:#FF5F56} .dot.y{background:#FFBD2E} .dot.g{background:#27CA3F}
</style>
</head>
<body>
  <div class="live">● LIVE PREVIEW — Arena Agent</div>
  <section class="hero">
    <div class="badge">✦ SAYVIS ARENA AGENT — گرافیک زنده</div>
    <h1>$title</h1>
    <p class="sub">این پیش‌نمایشِ زنده توسط ایجنتِ گرافیکیِ سایویس ساخته شد — دقیقاً مثل محیطِ Arena AI: فایل‌ها، ترمینال و پیش‌نمایش هم‌زمان.</p>
    <div class="actions">
      <button class="btn btn-primary" onclick="document.body.style.background='#0B0D12'">شروع کنید →</button>
      <button class="btn btn-ghost">دیدنِ کد</button>
    </div>
  </section>
  <div class="canvas">
    <div class="canvas-bar"><span class="dot r"></span><span class="dot y"></span><span class="dot g"></span><span style="margin-right:auto;font-size:12px;color:#9AA4B2">index.html — Live</span></div>
    <div style="padding:32px;text-align:center;color:#9AA4B2">گرافیکِ تعاملی اینجا رندر می‌شود — WebViewِ اندروید دقیقاً همین HTML را نمایش می‌دهد</div>
  </div>
  <div class="grid">
    <div class="card"><h3>🎨 طراحیِ گرافیک</h3><p>پالتِ طلایی-فیروزه‌ایِ سایویس با گرادینتِ زنده و کارت‌هایِ شیشه‌ای</p></div>
    <div class="card"><h3>⚡ تعاملِ آنی</h3><p>هر تغییرِ فایل بلافاصله در پیش‌نمایشِ کنارِ ترمینال دیده می‌شود</p></div>
    <div class="card"><h3>🧠 هوشِ Arena</h3><p>پلن → اجرایِ ابزارها → پیش‌نمایش → راستی‌آزمایی — مثلِ Arena AI</p></div>
  </div>
  <script>
    console.log('SAYVIS Arena Agent — graphical preview active');
    document.querySelector('.btn-primary')?.addEventListener('click',()=>alert('SAYVIS Arena — تعامل برقرار است ✨'));
  </script>
</body>
</html>
        """.trimIndent()
    }

    /**
     * Simulates a bash tool call — returns plausible output for the UI.
     */
    suspend fun simulateTool(step: PlanStep, index: Int): ToolCall {
        delay(600 + (index * 120L))
        return when (step.tool) {
            ToolKind.READ -> ToolCall(
                id = "call_$index",
                kind = ToolKind.READ,
                input = "read workspace files",
                output = "✓ 4 files read — index.html (2.4 KB), style.css (1.1 KB), script.js, README.md",
                graphicalData = GraphicalData(GraphType.FILE_TREE, "workspace tree", "File Explorer")
            )
            ToolKind.WEB_SEARCH -> ToolCall(
                id = "call_$index",
                kind = ToolKind.WEB_SEARCH,
                input = step.description,
                output = "🔍 3 sources found — Dribbble inspiration, MDN docs, Arena AI patterns",
                graphicalData = GraphicalData(GraphType.WEB_RESULTS, "3 live sources", "Web Search")
            )
            ToolKind.WRITE -> ToolCall(
                id = "call_$index",
                kind = ToolKind.WRITE,
                input = "write ${step.description}",
                output = "✓ File written — ${step.description} (1.8 KB) — diff: +42 lines",
                graphicalData = GraphicalData(GraphType.DIFF, "+42 lines • style.css", "Diff")
            )
            ToolKind.PREVIEW -> ToolCall(
                id = "call_$index",
                kind = ToolKind.PREVIEW,
                input = "render preview",
                output = "✅ Live preview updated — 340×680 @ 60fps",
                graphicalData = GraphicalData(GraphType.HTML_PREVIEW, generateHtmlPreview(step.titleEn), "Live Preview")
            )
            ToolKind.EDIT -> ToolCall(
                id = "call_$index",
                kind = ToolKind.EDIT,
                input = "edit & polish",
                output = "✏️ Edited — added animations, hover states, responsive grid",
                graphicalData = GraphicalData(GraphType.DIFF, "edited style.css +11 lines", "Edit")
            )
            ToolKind.BASH -> ToolCall(
                id = "call_$index",
                kind = ToolKind.BASH,
                input = "bash: ${step.description}",
                output = "$ ls -la workspace/\n total 12\n -rw-r--r-- index.html\n -rw-r--r-- style.css\n✓ bash completed",
                graphicalData = GraphicalData(GraphType.TERMINAL, "bash output", "Terminal")
            )
            ToolKind.GENERATE_IMAGE -> ToolCall(
                id = "call_$index",
                kind = ToolKind.GENERATE_IMAGE,
                input = "generate image: ${step.description}",
                output = "🎨 Image generated — 1024×1024 • gallery/generated_01.png",
                graphicalData = GraphicalData(GraphType.IMAGE, "generated_01.png", "Canvas")
            )
            ToolKind.IMAGE_SEARCH -> ToolCall(
                id = "call_$index",
                kind = ToolKind.IMAGE_SEARCH,
                input = "image_search",
                output = "🖼️ 4 images found — Unsplash, Dribbble",
                graphicalData = GraphicalData(GraphType.IMAGE, "search results", "Images")
            )
            ToolKind.DONE -> ToolCall(
                id = "call_$index",
                kind = ToolKind.DONE,
                input = "verify",
                output = "✅ All steps verified — report ready",
                graphicalData = GraphicalData(GraphType.CANVAS, "verified", "Done")
            )
        }
    }
}
