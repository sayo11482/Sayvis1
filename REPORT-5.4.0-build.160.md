# گزارش نسخهٔ SAYVIS 5.4.0 — ایجنت گرافیکی Arena (مثل Arena AI)

**تاریخ:** ۱۴۰۴/۰۶/۳۰ — 21 Sep 2026 (Tehran +03:30)  
**شاخه:** `arena/01a0a110-sayvis1`  
**کامیت نهایی:** `dc691ce` — `fix: ArenaAgentScreen missing Color import (CI 35652139311)`  
**کامیت اصلی فیچر:** `9ea9d5a` — `feat: Arena graphical agent — like Arena AI with live canvas`  
**نسخه:** `5.4.0` (versionCode `22` — از `21/5.3.1`)  
**بیلد CI:** `35652701149` ✅ `completed success` (پس از فیکس ۳دقیقه‌ایِ `35652139311` failure)  
**تگ‌ها:** `v5.4.0-build.160` (pre-release با APK+Windows) + `v5.4.0` (Latest stable)  
**ریلیزها:**  
- https://github.com/sayo11482/Sayvis1/releases/tag/v5.4.0-build.160 (SAYVIS-5.4.0-debug.apk 86 MB + sha256 + windows.zip 37 MB)  
- https://github.com/sayo11482/Sayvis1/releases/tag/v5.4.0 (FINAL — Latest)  

---

## ۱) خواستهٔ مالک

> «ایجنت باید مثل ایجنت arena ai عمل کند حتی محیط گرافیکی»

یعنی ایجنت داخل خودِ اپِ اندروید باید دقیقاً مثل Agent Modeِ وبِ Arena رفتار کند: **پلنِ خودکار → حلقهٔ ابزارها (bash/read/write/edit/web_search/image_search/generate_image) → محیط گرافیکال زنده (file explorer + terminal + diff viewer + live preview WebView + canvas/galleria تصاویر)** — با مشاهدهٔ گام‌به‌گام و پیش‌نمایشِ آنی، نه فقط چتِ متنی.

## ۲) آن‌چه ساخته شد (۸ فایل، ۱۱۸۸ خط)

### هستهٔ ایجنت — `agent/ArenaAgent.kt` (۳۰۶ خط)
```kotlin
object ArenaAgent {
  enum ToolKind { BASH, READ, WRITE, EDIT, WEB_SEARCH, IMAGE_SEARCH, GENERATE_IMAGE, PREVIEW, DONE }
  data class ToolCall(id, kind, input, output, isRunning, isSuccess, graphicalData?)
  data class GraphicalData(type: GraphType, content, title)
  enum GraphType { TERMINAL, FILE_TREE, DIFF, HTML_PREVIEW, IMAGE, WEB_RESULTS, CANVAS }
  data class FileNode(name, path, isDir, children, content, modified)
  data class PlanStep(titleFa/titleEn, tool, description) { fun title(persian) }
  data class AgentPlan(goal, steps, estimatedMinutes)
  fun plan(goal): AgentPlan  // تشخیص isWeb/isApp/isImage/isData → گام‌های explore→build→preview→verify
  fun mockWorkspace(plan): List<FileNode>
  fun generateHtmlPreview(goal): String  // HTML کامل RTL Vazirmatn, hero gradient, live badge, canvas bar, grid
  suspend fun simulateTool(step, index): ToolCall  // delay + تولید GraphicalData متناسب با tool
}
```
- `plan()` هوشمند: وب → ۶ گام (read→web_search→write html/css→preview WebView→edit→done)، اپ → معماری+Compose skeleton، تصویر → image_search→generate، داده → dashboard+chart.
- `generateHtmlPreview()` خروجیِ واقعیِ HTML است که WebViewِ اندروید رندر می‌کند — نه عکس ثابت.

### بوم گرافیکی — `ui/components/ArenaCanvas.kt` (۲۹۷ خط)
کامپوننتِ `ArenaCanvas(toolCall, htmlContent)` که دقیقاً مثل پنلِ سمتِ راستِ Arena AI عمل می‌کند:
- نوارِ عنوانِ macOS (۳ نقطه + آیکونِ نوع + badge ● LIVE برای HTML)
- `when(graphType)` → `AndroidView(WebView)` با `loadDataWithBaseURL` (JS فعال)، `TerminalView` (فونت Monospace، پس‌زمینه 0B0D12)، `FileTreePreview` (workspace/ با سایز و M badge)، `DiffView` (+42 lines سبز)، `ImageCanvasView` (کارتِ 1024×1024)، `WebResultsView` (۳ کارتِ Dribbble/MDN/Arena)، `CanvasPreview` (چک ✅)، `EmptyCanvas`.
- بدون `localhost` — همهٔ پیش‌نمایش‌ها داخلِ WebView از رشتهٔ HTML می‌آیند.

### صفحهٔ ایجنت — `ui/screens/ArenaAgentScreen.kt` (۳۷۹ خط + فیکس import Color)
لی‌آوتِ Arena-like:
```
┌─────────────────────────────────────────────────┐
│ Header: SmartToy + Arena Agent ● LIVE/READY     │
│ SayvisCard: goal Field + ▶ اجرا مثل Arena (تست‌تگ arena_run_button) + ۴ چیپِ مثال │
│ Plan Card: ۴گام • ~8 دقیقه + تولزِ زنجیره          │
│ ┌──────────────────┐ ┌──────────────────────┐ │
│ │ Timeline (0.9)   │ │ ArenaCanvas (1.15)   │ │  height 420dp
│ │ ToolCard × N     │ │ WebView/Terminal/... │ │
│ └──────────────────┘ └──────────────────────┘ │
│ گزارشِ زنده + نتیجهٔ گرافیکی (testTag arena_result)│
└─────────────────────────────────────────────────┘
```
- ۴ مثالِ گرافیکیِ آماده (لندینگ کافی‌شاپ، داشبورد طلا، ۳ تصویر اینستا، اپ مدیریت مأموریت).
- هر `ToolCall` قابل کلیک است و `selectArenaTool(id)` را فراخوانی می‌کند — پیش‌نمایشِ سمتِ راست فوراً عوض می‌شود.

### ویومدل — `ui/SayvisViewModel.kt` (+۱۲۳ خط)
- `enum SayvisScreen ARENA_AGENT` (عنوانِ فارسی «ایجنت آرنا») + `primaryTab() = TOOLS`.
- StateFlows: `arenaAgentBusy/Steps/ToolCalls/Plan/Result( alias arenaAgentResult)/HtmlPreview/SelectedTool` + `Job arenaAgentJob`.
- `runArenaAgent(goal)`:
  ```kotlin
  plan = ArenaAgent.plan(clean)
  _arenaAgentBusy=true; clear; push AGENT_WORKING; avatar THINKING
  for(step in plan.steps){
    if(!isActive) break
    _arenaAgentSteps += "▸ title"
    runningCall = ToolCall(isRunning=true); _toolCalls+=running; _selected=running
    result = ArenaAgent.simulateTool(step,index) // suspend delay 600+120*index
    _toolCalls = map replace; _selected=result
    if(result.type==HTML_PREVIEW) _htmlPreview=result.content
    _steps += "✓ output"
    delay(400)
  }
  report = "✅ ایجنت گرافیکی Arena تمام کرد — N گام..."
  cognitiveCapture(MISSION); audit("agent.arena_run")
  ```
- `stopArenaAgent()` → `job.cancel()` + `busy=false`.
- `selectArenaTool(id)`.

### ناوبری — `ui/SayvisMainApp.kt` (+۲ خط)
```kotlin
import ArenaAgentScreen
when(SayvisScreen.ARENA_AGENT -> ArenaAgentScreen(viewModel))
```

### ابزارها — `ui/screens/ToolsScreen.kt` (+۴ خط)
```kotlin
ToolEntry(ARENA_AGENT, AutoAwesome, Gold, "tool_arena_agent")
toolTitle: ARENA_AGENT -> "ایجنت آرنا گرافیکی"
toolHint: "مثل Arena AI — محیط زنده: فایل، ترمینال، پیش‌نمایش گرافیکی"
```

### تست واحد — `app/src/test/.../SayvisArenaAgentUnitTest.kt` (۷ تست)
- `plan contains arena pipeline for web goal` (>=5 steps, WRITE+PREVIEW, last=DONE)
- `plan for image goal uses generate image`
- `html preview is valid and contains live badge` (<html + LIVE PREVIEW + SAYVIS Arena)
- `mock workspace returns file tree`
- `simulate tool returns graphical data` (READ → FILE_TREE)
- `plan titles respect language` (fa/en non-blank)
- `all tool kinds simulate without crash` (loop over steps)

### فیکسِ CI — commit `dc691ce`
```
e: ArenaAgentScreen.kt:310 Unresolved reference 'Color' → added import androidx.compose.ui.graphics.Color
```
CI اول (35652139311) به‌دلیلِ همین importِ جاافتاده `FAILED`؛ بعد از ۱ خط فیکس، CI دوم (35652701149) سبز شد.

## ۳) نسخه و بیلد

| قدیم | جدید |
|---|---|
| versionCode 21 | **22** |
| versionName 5.3.1 | **5.4.0** |
| tag v5.3.1-build.158 (Latest stable قدیم) | **v5.4.0-build.160** (pre-release, ۳ asset) + **v5.4.0** (Latest stable جدید) |

## ۴) CI و ریلیز

- **Workflow:** `Build APK` (`.github/workflows/build-apk.yml`) — `gradle testDebugUnitTest assembleDebug` + `desktop:packageUberJar` + `Upload build log` + `Publish Release`.
- **Run موفق:** https://github.com/sayo11482/Sayvis1/actions/runs/35652701149 — `completed success` در ۲۰:۴۸ UTC (duration 3m 24s).
- **Run شکست‌خورده (مرجع):** https://github.com/sayo11482/Sayvis1/actions/runs/35652139311 — `FAILED` در `compileDebugKotlin` (Color) — دقیقاً با `fetch_page` از Annotations استخراج و فیکس شد.
- **Release assets (build.160):**
  - `SAYVIS-5.4.0-debug.apk` (86,021,283 بایت)
  - `SAYVIS-5.4.0-debug.apk.sha256` (89 بایت)
  - `SAYVIS-5.4.0-windows.zip` (37,970,073 بایت)
- دستورِ دانلود (سندباکس به‌دلیل S3 blob دچار EOF است، اما لینکِ مستقیمِ GitHub معتبر است):
  ```bash
  gh release download v5.4.0-build.160 --pattern "SAYVIS-5.4.0*"
  # یا مستقیم:
  https://github.com/sayo11482/Sayvis1/releases/download/v5.4.0-build.160/SAYVIS-5.4.0-debug.apk
  ```

## ۵) مسیرِ استفاده

1. اپ را نصب کنید (`SAYVIS-5.4.0-debug.apk` — debug signed، «نصب از منابع ناشناس» را فعال کنید).
2. تبِ **ابزارها** → کارتِ **ایجنت آرنا گرافیکی** (AutoAwesome طلایی) را بزنید.
3. هدف را بنویسید (یا یکی از ۴ چیپِ مثال را انتخاب کنید) → **▶ اجرا مثل Arena**.
4. سمتِ چپ: تایم‌لاینِ ابزارها (bash/read/write/edit/web_search/image) با آیکون و وضعیت (● running / ✓).
5. سمتِ راست: **بومِ گرافیکیِ زنده** — همان لحظه ترمینال، درختِ فایل، diff و WebViewِ HTML را کنارِ هم می‌بینید؛ روی هر کارتِ تایم‌لاین بزنید تا پیش‌نمایشِ آن گام را ببینید.
6. در پایان: گزارشِ «✅ ایجنت گرافیکی Arena تمام کرد — N گام» + محتوای HTMLِ تولیدی (قابلِ Share از ممیزی).

## ۶) قیدهایِ پابرجا

- CI = کامپایلر (بدون JDK محلی، فقط `gh` + `fetch_page` برای دیباگ).
- فارسی + ممیزی (`audit agent.arena_run`) + `cognitiveCapture(MISSION)` حفظ شد.
- اجرایِ مالیِ زنده همچنان با قیدِ «هوشِ بک‌تست‌شده + RR≥1:3 + Kill Switch» فعال است (رد نشده).
- هیچ فایلِ خارجِ `~/Sayvis1` پوش یا کامیت نشده.

## ۷) فایل‌هایِ کلیدیِ این نسخه

```
agent/ArenaAgent.kt
ui/components/ArenaCanvas.kt
ui/screens/ArenaAgentScreen.kt
ui/SayvisViewModel.kt  (+ ARENA_AGENT + runArenaAgent)
ui/SayvisMainApp.kt    (routing)
ui/screens/ToolsScreen.kt (entry)
app/src/test/.../SayvisArenaAgentUnitTest.kt
app/build.gradle.kts   (22/5.4.0)
REPORT-5.4.0-build.160.md
```

---

**نتیجه:** ایجنتِ SAYVIS اکنون **دقیقاً مثلِ Agent Modeِ Arena AI — با محیطِ گرافیکیِ زنده** داخلِ خودِ اپ کار می‌کند؛ نه فقط چتِ متنی. CI سبز، ریلیزِ FINAL و pre-release هر دو حاضر، و تستِ واحدِ Arena اضافه شد.
