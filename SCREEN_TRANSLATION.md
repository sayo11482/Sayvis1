# Live Screen Translator (PUR — Permanent Universal Reader)

**Persian UI name:** مترجم زندهٔ صفحه
**Goal:** English text anywhere on the phone is replaced, in place, with Persian — readable
without copying, switching apps or waiting for a model to answer.

The owner turns it on once; it stays on until they stop it. It then reads whatever is on the
screen and writes the Persian **on top of the original characters**, so a menu item that said
`Save changes` reads `ذخیرهٔ تغییرها` in the same position, at the same size, with a plate whose
colour was sampled from the app underneath.

---

## Pipeline

```text
 MediaProjection (owner consent, per session)
        │
        ▼
 VirtualDisplay → ImageReader        ~1180 px long edge, one frame in flight
        │
        ▼
 Change detection                    16×16 luminance grid → FNV-1a fingerprint
        │                            identical frame ⇒ no OCR, no battery cost
        ▼
 ML Kit Text Recognition (bundled)   fully offline Latin OCR → word boxes
        │
        ▼
 ScreenTextPlanner                   line assembly, prose filter, plate geometry,
        │                            font fitting, contrast, plate colour sampling
        ▼
 ScreenTranslationEngine             cache → built-in dictionary → owner's AI provider
        │
        ▼
 ScreenOverlayPlanView               Persian drawn exactly where the English was
```

Budget per frame: one OCR pass, one plan, at most `maxSegmentsPerRequest` new sentences per AI
call. Everything already known is resolved from the on-device cache or the GLOSSARY with no
network at all.

---

## The three translation tiers

| Tier | Source | Cost | Works offline |
|------|--------|------|---------------|
| 1 | On-device cache (`sayvis_screen_translations`) | free, instant | yes |
| 2 | Built-in glossary — `ScreenLexicon` (400+ phrases, 490+ words, plus word-by-word composition) | free, instant | yes |
| 3 | The AI provider configured in Settings (Gemini / OpenRouter / Groq / custom endpoint) | one request per batch | no |

Tier 3 is skipped entirely when **any** of these is true: `offline dictionary only` is on, the
global **force offline** mode is on, the **emergency lock** is engaged, the selected provider is
the local core, or no key is configured. Even then the overlay keeps working for everything the
glossary knows — the feature degrades, it never dies.

Every region carries its provenance (`DICTIONARY`, `CACHE`, `MACHINE`), which the interface
shows next to each translated line, so machine output is never presented as reviewed text.

---

## Display modes

| Mode | Behaviour |
|------|-----------|
| `REPLACE` (default) | The plate covers the English and the Persian is written in its place. |
| `CAPTION` | The English stays visible; the Persian is written directly underneath it. |
| `HIGHLIGHT` | Nothing is covered — only a translucent plate marks the translated line. |

## What is deliberately *not* translated

* numbers, prices, dates, times, percentages (`1,234.56`, `17%`)
* URLs, e-mail addresses, `@handles`, `#hashtags`
* machine identifiers and tickers (`XAUUSD`, `EURUSD`, `server_error`, `utf-8`)
* all-capitals code words of four letters or more, and vowel-less fragments
* anything already in Persian or Arabic script
* a line whose glossary coverage is below 60% (a half-English line is worse than none)

---

## Permissions and why each one exists

| Permission | Why |
|------------|-----|
| `SYSTEM_ALERT_WINDOW` ("display over other apps") | To draw the Persian over the app the owner is reading. Without it there is nowhere to write. |
| `MediaProjection` consent (system dialog, every session) | To read the screen. Android requires a fresh consent per capture session and does not allow caching it. |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MEDIA_PROJECTION` | A capture session must be a visible foreground service; the notification is the permanent "off" button. |
| `POST_NOTIFICATIONS` | The session is permanent, so its status must stay visible and stoppable. |
| `RECEIVE_BOOT_COMPLETED` | Optional reminder after a reboot (capture consent is still required). |

---

## Zero-trust behaviour

* A capture session cannot start without **both** the overlay permission and the on-screen
  consent dialog, and cannot start at all while the emergency lock is engaged.
* Engaging the emergency lock while running stops the session immediately, clears the overlay
  and leaves a `CRITICAL` audit event (`screen.translate.blocked`).
* `screen.translate.start`, `.stop`, `.consent` and `.permission` are written to the audit
  chain; the consent path records `OWNER_CONSENT_SCREEN_CAPTURE`.
* The captured image never leaves the device. Only recognised **text** — and only in tier 3 —
  is sent to the provider the owner configured. In `offline dictionary only` mode, nothing is
  sent anywhere, ever.
* The floating bubble is the always-available stop control: tap = pause, hold = stop.

---

## Tuning knobs (Settings → Live Screen Translator / Tools → Live Screen Translator)

| Setting | Default | Effect |
|---------|---------|--------|
| `pollIntervalMs` | 700 | Gap between frames. Lower is snappier, higher is cooler. |
| `maxSegmentsPerFrame` | 48 | Ceiling per frame; keeps a wall of text from stalling the pipeline. |
| `maxSegmentsPerRequest` | 20 | Sentences packed into one model call. |
| `plateOpacityPercent` | 92 | How strongly the plate hides the original text. |
| `textScalePercent` | 100 | Persian size relative to the line it replaces. |
| `showControlBubble` | on | The floating pause/stop bubble. |
| `dictionaryOnly` | off | Hard switch: on-device glossary only, no network tier at all. |
| `cacheTranslations` | on | Reuse sentences across sessions. |
| `preferSingleAppCapture` | off | Android 14+: capture one chosen app window instead of the whole screen. |
| `keepScreenOn` | off | Keep the display awake while translating. |
| `resumeAfterBoot` | off | Offer to re-arm the translator after a restart. |

---

## Performance notes

* Frames are mirrored at ~1180 px on the long edge, not at panel resolution: OCR accuracy is
  unaffected while the per-frame copy cost drops by an order of magnitude.
* Only one frame is ever in flight; a slow model call cannot queue stale screens.
* Unchanged frames are dropped by fingerprint before OCR runs — a static screen costs one cheap
  grid pass per interval and nothing else.
* Blank frames (protected content such as banking apps, or a powered-off display) are detected
  and reported instead of pretending the screen is empty.
* Rotation resizes the existing virtual display and swaps its surface; Android 14+ allows only
  one `createVirtualDisplay()` per projection, and this respects that.

---

## Troubleshooting

| Symptom | Cause / fix |
|---------|-------------|
| "Waiting for screen-capture consent" forever | The system dialog was dismissed. Start again and accept "Start now". |
| Nothing is drawn, but the notification says active | "Display over other apps" is not granted — grant it, then tap *Refresh* (or re-open the screen). |
| Translation stops by itself | The owner pressed "Stop sharing" in the system UI, or the emergency lock engaged. |
| A banking app is not translated | `FLAG_SECURE` content cannot be captured by design; SAYVIS reports it. |
| English text stays English | The line has no glossary coverage and the AI tier is unavailable (offline / no key / dictionary-only). Turn either on, or widen the glossary. |
| Battery drain | Raise `pollIntervalMs`, disable the bubble, or limit capture to one app (Android 14+). |
