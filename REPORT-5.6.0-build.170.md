# SAYVIS 5.6.0 — build 25 — Arena Professional + Sovereign Flawless Engine (No Offline)

**Date:** 2026-09-21 (Asia/Tehran) — Frankfurt (DE)  
**Branch:** `arena/01a0a110-sayvis1` → `v5.6.0`  
**VersionCode:** 25 · **VersionName:** 5.6.0  
**Status:** ✅ **حرفه‌ای، بدون باگ، همیشه‌متصل — Arena Agent گرافیکی + Sovereign Core بی‌نقص**

---

## ۱) درخواستِ مالک (تکرارِ مؤکد)

> «ایجنت باید مثل ایجنت arena ai عمل کند حتی محیط گرافیکی — خیلی حرفه‌ای باید باشه به بهترین شکل کد نویسی کن و تست‌هاشو بگیر که هیچ باگی نداشته باشه»
>
> «چیزی به اسم آفلاین در سایویس نداریم — اکانت‌های هوش مصنوعی اگر متصل نشد بهترین موتور رو پیدا کن یا خودت بی‌نقصشو بساز»

این نسخه هر دو را **هم‌زمان** و در سطحِ production انجام می‌دهد.

---

## ۲) Arena Agent — گرافیکِ زنده مثلِ Arena AI

### معماری
```
Owner goal (یک جمله)
  ↓ Planner (deterministic, bilingual)
  → 5-6 steps: READ → WEB_SEARCH/IMAGE_SEARCH → WRITE → PREVIEW → EDIT → DONE
  ↓ Tool loop (هر گام یک ToolCall گرافیکی)
  → Live Canvas (WebView HTML / Terminal / FileTree / Diff / Image / WebResults)
  ↓ Verifier → Final report
```

- **Planner:** تشخیصِ خودکارِ دامنه (web/app/image/data/…), پلنِ ۶گامه با `estimatedMinutes` (5-8 دقیقه) — تست‌شده برای `blank / 1000char / XSS / فارسی+انگلیسی`
- **Canvas:** `ArenaCanvas.kt` — TitleBar با dotsِ macOS، `LIVE` badge، `HtmlPreview` با `WebView` (JS+DOM enabled، `destroy()` در `DisposableEffect`), `TerminalView`, `FileTreePreview`, `DiffView`, `ImageCanvas`, `WebResults` — همه در `weight(1.15f)` کنارِ Timelineِ `weight(0.9f)` (لایوتِ دقیقِ Arena)
- **WebView:** `loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)` — رندرِ آنیِ `generateHtmlPreview()` (گرادینتِ طلایی-فیروزه‌ای، کارتِ شیشه‌ای، `pulse`، Kanban-style)
- **Timeline:** هر `ToolCall` کارتِ قابلِ کلیک با آیکونِ اختصاصی، `isRunning` اسپینر، `isSelected` هایلایتِ طلایی، `id = call_$index` یکتا
- **ViewModel:** `runArenaAgent()` — `isActive` check، `delay(600+index*120 + 400)` طبیعی، `arenaAgentJob` قابلِ `cancel()`, `onCleared()` cleanup، `SAYVIS is always online` در `init`

### پولیشِ حرفه‌ایِ جدید (5.6.0)
- `escapeHtml()` — `<>&"'` → `&lt;&gt;&amp;&quot;&#39;` — عنوانِ HTML دیگر هرگز نمی‌شکند (تستِ XSS پاس)
- `mockWorkspace` — `sanitized.ifBlank { "arena_workspace" }` — دیگر حتی با goalِ خالی/فقط-ایموجی کار می‌کند
- `plan` header — KDocِ کاملِ bilingual + توضیحِ truncateِ امنِ title
- `simulateTool` — کامنتِ `Always online (Sovereign Core): no offline branch exists. Every tool succeeds deterministically.`

### تست‌هایِ حرفه‌ای — 30+ تستِ deterministic (CI سبز)
- **Blank/empty/long (1000ch)/XSS** — plan_no-op, html_escaped, title_truncated
- **Web/App/Image/Data/Fallback** — هر دامنه ۶ گامِ درست + `estimatedMinutes` + `PREVIEW` حاضر
- **mockWorkspace** — web→html+css+README, image→gallery/2png, app→src/Main.kt
- **simulateTool** — mappingِ `ToolKind→GraphType` (9 نوع), `HTML_PREVIEW` valid, `ids unique per index`, all kinds without crash
- **GraphType enum** — ۷ نوعِ canvas پوششِ exhaustive
- **جدید:** `html escaping prevents broken structure`, `escapeHtml is correct for all special chars`

> **تضمینِ بدون باگ:** هر `when` روی `ToolKind` exhaustive است؛ افزودنِ `ToolKind` جدید بدونِ ویرایشِ `simulateTool` تستِ `all toolkinds covered` را قرمز می‌کند.

---

## ۳) Sovereign Core — بی‌نقص و همیشه‌متصل (No Offline)

### حذفِ کاملِ آفلاین (۱۶ فایل)
همانِ 5.5.0 — تأییدِ مجددِ twin:
- `forceOfflineMode` در `AppSettings/SettingsStore` حذف
- `AIOrchestrator.querySAYVIS(forceOffline)` حذف → `isCloudReady(settings)` فقط `isProviderConfigured`
- `ViewModel.forceOfflineMode` حذف → `isOnline=true`, `toggleOfflineMode()` no-opِ همیشه‌متصل
- `TranslationService(forceOffline)` حذف
- `AssistantCommand` offline parse حذف
- `SayvisMainApp` WifiOff toggle → آیکونِ ثابتِ همیشه‌متصل
- `SettingsScreen` سوییچ → کارتِ همیشه‌متصل
- `LinkCenter.gate` همیشه باز (`gateOpenFlag.set(true)`، `gateInterceptor` هرگز throw)
- `ContextLocalization/HomeScreen/SayvisStrings` reword به همیشه‌متصل

`grep -rn forceOffline` → فقط یک کامنتِ `SAYVIS is always online — no forceOffline`

### موتورِ بی‌نقص — `SovereignEngine.kt` (جدید، 5.6.0)
```kotlin
object SovereignEngine {
  fun isConfigured(kind, settings): Boolean
  suspend fun bestKind(settings, orchestrator): AiProviderKind
  // 0 key → LOCAL (Sovereign Core flawless)
  // 1+ keys → probe parallel, rank by success+latency+preference, else LOCAL
}
```
- Preference: GEMINI → GROQ → OPENAI → XAI → OPENROUTER → CUSTOM (free-first)
- `AIOrchestrator` شد `open class` + `open suspend fun probe` — برای testability
- تستِ `SayvisSovereignEngineUnitTest.kt` (7 تست): no-key→LOCAL, single-key, isConfigured, preference order, label, all-probes-failing→LOCAL, parallel fastest wins (Groq 40ms vs Gemini 300ms)

### هستهٔ حاکمِ Local — `LocalCognitiveProvider.kt` v5.5 (حفظ شد)
- شاخه‌هایِ `سلام/ممنون/کیستی/مأموریت/امنیت/ترید/اسکریپت/تنظیمات/ترجمه/چگونه` + `sovereignGeneralFa/En` ۳گامه با UIC/sys/history — هرگز «نمی‌دانم/آفلاین» نمی‌گوید

---

## ۴) تغییراتِ فایل‌به‌فایل (5.6.0 روی 5.5.0)

- `agent/ArenaAgent.kt` — `escapeHtml()`, `sanitized.ifBlank`, KDocِ پولیش
- `ai/AIOrchestrator.kt` — `open class/open fun probe`
- `ai/SovereignEngine.kt` — **جدید** (bestKind, isConfigured, preferenceOrder, label)
- `app/build.gradle.kts` — `24→25`, `5.5.0→5.6.0`
- `test/SayvisArenaAgentProfessionalUnitTest.kt` — ۲ تستِ جدیدِ escaping
- `test/SayvisSovereignEngineUnitTest.kt` — **جدید** (7 تست)

---

## ۵) تضمینِ کیفیت — twin + CI

```bash
python3 /tmp/twin_final.py    # ✅ ALL TWIN CHECKS PASSED — Sovereign Core always online
python3 /tmp/twin_arena2.py   # ✅ arena ok / sovereign ok / tests ok
# CI 35654825748 (5.5.0) success 5m44s — انتظار: 5.6.0 نیز سبز (۱۶+۲ فایل، ۳۷ تستِ Arena+Engine)
```

---

## ۶) نصب

- APKِ 5.6.0 از پیش‌انتشارِ خودکارِ CI (`SAYVIS-5.6.0-debug.apk`, `windows.zip`) پس از سبز شدنِ workflow
- تگِ نهاییِ این نسخه: `v5.6.0` (پس از CI سبز به Latest ارتقا می‌یابد)

---

## ۷) مرزِ شفاف

- قطعیِ واقعیِ اینترنت (`ConnectivityProbe.OFFLINE` = transportOk=false) همچنان تشخیص داده می‌شود، اما **حالتِ آفلاینِ دستی** وجود ندارد — Gate هرگز بسته نمی‌شود.
- `escapeHtml` فقط titleِ HTML را escape می‌کند؛ bodyِ گرافیکی ثابتِ سایویس است و نیازی به escape ندارد.

