# SAYVIS / SAYO — Sovereign Personal AI Platform

**SAYVIS** (Codename: **SAYO** / **سایویس**) is a Sovereign Personal AI Operating System and Cognitive Platform for Android and multi-device companion environments.

## Core Philosophical Tenets
- **Human Sovereignty**: The human owner maintains irrevocable supreme authority over all actions and data.
- **Zero Trust**: No AI model, remote server, background agent, or external hardware peripheral is implicitly trusted.
- **Explicit Consent**: Actions categorized above passive observation require cryptographically logged human authorization.
- **Offline-First & Graceful Degradation**: Core cognition, mission tracking, UIC epistemic review, and local decision simulation operate completely offline without active internet connectivity.
- **Tamper-Evident Security**: All audit events are hashed into a cryptographic SHA-256 event chain.

---

## Architectural Modules
1. **User Cognitive Model (UIC)**: Epistemic knowledge tracking (`OBSERVED`, `INFERRED`, `CONFIRMED`, `REVOKED`) with confidence metrics (0.0 to 1.0) and provenance auditing.
2. **AWARE Engine**: Proactive contextual synthesis across battery, network, missions, and cognitive rhythms.
3. **AI Orchestrator**: Multi-provider bridge (Gemini, OpenRouter, Groq, any OpenAI-compatible endpoint) with local cognitive reasoning fallback. **Providers and keys are configured at runtime from Settings.**
4. **Zero-Trust Permission Engine**: Risk classification (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`) with hardware/software killswitch.
5. **Missions Engine**: Hierarchical breakdown of macro goals into objectives, tasks, and subtasks.
6. **Trading Terminal Gateway**: MetaTrader 4/5 connection through a bridge (MetaApi, a self-hosted Expert Advisor service, or a local paper simulator), with multi-layer order gating.
7. **LIT (Liquidity Intelligence & Trading)**: Analytical order-flow and market-structure engine, paper-trading by default.
8. **Scripting & Automation**: A small line-based rule language the owner writes in-app; scripts emit effects that pass through the zero-trust gate and never perform I/O themselves.
9. **Life Simulation Engine**: Probabilistic scenario analysis for career, financial, and strategic life decisions.
10. **Sovereign Avatar**: Real-time cybernetic canvas state visualizer displaying cognitive, communicative, and security posture.
11. **Floating Avatar & Ambient Listening**: A small draggable chat-head avatar (foreground service with `microphone` type + `SYSTEM_ALERT_WINDOW`) that stays on screen over the launcher after Home. It listens on-device with an adaptive VAD, builds a 26-dimensional voice-print (log-mel + F0 + centroid) from three enrollment samples, and recognises the owner's voice offline — tap opens SAYVIS, drag moves, long-press stops. Nothing is recorded, stored as audio, or uploaded.

---

## Navigation Model
The bottom bar carries **four general categories**. Every concrete capability lives in
Tools or Settings, where there is room to label it properly.

| Tab | Contains |
|---|---|
| **Home** | Live status, active mission, pending AWARE proposals, quick ask |
| **Assistant** | Conversation with the AI orchestrator |
| **Tools** | Work & Planning · Cognition & Context · Finance & Trading · System & Development |
| **Settings** | Language & Appearance · AI & API · Trading Gateway · Automation · Security · Data · About |

Secondary screens (Missions, Cognitive Profile, Smart Suggestions, Trading, Decision
Simulator, Security, Gateway, Scripts) report the primary tab they belong to, so the bar
stays highlighted and a back affordance appears in the top bar.

---

## Settings
`settings/SettingsStore.kt` persists owner configuration as a reactive `StateFlow`.

- **Language & Appearance** — interface language (Persian / English / device), Persian
  digits, AI translation of free text, machine-translation marker, RTL mirroring, theme,
  haptics.
- **AI & API** — provider selection, API key entry, model id, custom base URL, persona,
  temperature, max tokens, timeout, forced offline mode, and a live connection test that
  reports latency and the real error.
- **Trading Gateway** — bridge type, terminal version, account type, broker, server,
  login, password, token, execution mode, daily loss cap, max lot, auto-flatten.
- **Automation** — enable/disable automatic script execution, jump to the editor.
- **Security** — emergency lock, high-risk confirmation, on-device audit retention.
- **Data** — translation cache inspection and clearing, full settings reset.

Secrets never touch the settings JSON: they are encrypted with an AES-256/GCM key inside
the Android Keystore (`settings/SecureVault.kt`). When secure hardware is unavailable the
UI says so instead of pretending to be safe.

---

## Localization
Nothing in the interface is left in English when the owner picks Persian.

- `i18n/SayvisStrings.kt` — the complete built-in dictionary (offline, deterministic).
- `i18n/PersianFormat.kt` — Persian digits, currency word order, relative and Jalali dates.
- `i18n/ContextLocalization.kt` — renders the language-neutral `ContextSnapshot`.
- `ai/TranslationService.kt` — hybrid resolution for free-form text: exact dictionary →
  numeric/quoted templates (with recursive translation of nested fragments) → persisted
  cache → online AI translation. Machine identifiers and tickers are detected and left
  untouched.
- `SayvisText` — drop-in `Text` replacement that runs the pipeline and can mark
  machine-translated output.

---

## Brand & Launcher Icon
The launcher identity is the SAYVIS power-core artwork (violet chrome head, neon
conduits, hexagonal plasma core):

- `art/sayvis-icon-master.png` — the square master artwork (source of truth).
- `art/sayvis-icon-512.png` — store-listing size.
- `tools/make-icons.sh [source-image]` — regenerates **every** launcher asset from any
  source image: centre-crops to square, then emits the five `mipmap` densities
  (`ic_launcher.webp`), circular `ic_launcher_round.webp` variants, the 432px adaptive
  foreground (`drawable-nodpi/ic_launcher_foreground.png`) and the 512px store asset.
  Hand the owner's original artwork to this script and the whole icon set follows.
- Adaptive layers (API 26+): `drawable/ic_launcher_background.xml` (edge-sampled
  gradient) and `drawable/ic_launcher_monochrome.xml` (themed-icon glyph).

---

## Technology Stack
- **Language**: Kotlin 2.0+
- **UI Framework**: Jetpack Compose with Material Design 3
- **Local Persistence**: Room Database (SQLite with Flow reactivity) + JSON stores for settings and scripts
- **Networking**: OkHttp (AI providers, MetaApi / self-hosted trading bridge, script webhooks)
- **Concurrency**: Kotlin Coroutines & Reactive StateFlow
- **Cryptography**: SHA-256 tamper-evident audit chaining; AES-256/GCM via Android Keystore
- **Localization**: Native bidirectional support (Persian RTL / English LTR)

## حساب مالک و جفت‌سازی دستگاه‌ها (Owner account & device pairing)

- حساب مالک با **ایمیل + رمز عبور** روی خود گوشی ساخته می‌شود؛ رمز ذخیره نمی‌شود، فقط اثر `PBKDF2-HMAC-SHA256` (۱۲۰٬۰۰۰ تکرار) داخل Keystore نگه داشته می‌شود.
- **جفت‌سازی PC/دستگاه دیگر**: گوشی یک کد ۵ دقیقه‌ای می‌سازد، دستگاه مقابل با همان ایمیل/رمز وارد می‌شود و `proof = HMAC(secret, code|fingerprint|accountId)` می‌سازد، مالک آن را روی گوشی تأیید می‌کند. دستگاه جدید همیشه «محدود» شروع می‌شود تا مالک اعتماد بدهد. همهٔ مراحل در سیاههٔ ممیزی ثبت می‌شوند و در قفل اضطراری مسدودند.
- وضعیت طبق Dossier: **Account/Auth = IMPLEMENTED (on-device)**، **Pairing handshake = IMPLEMENTED (crypto contract + UI)**، **Gateway شبکه‌ای / همگام‌سازی واقعی بین دستگاه‌ها = PLANNED** — سمت PC هنوز باید همین قرارداد (`identity/DevicePairing.kt`) را پیاده کند.
