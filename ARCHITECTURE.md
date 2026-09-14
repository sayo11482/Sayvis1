# SAYVIS System Architecture

```text
                    ┌────────────────────────┐
                    │       SAYVIS UI        │
                    │   (Jetpack Compose)    │
                    │  4 tabs: Home /        │
                    │  Assistant / Tools /   │
                    │        Settings        │
                    └───────────┬────────────┘
                                │  SayvisText → TranslationBridge
                                ▼
                    ┌────────────────────────┐
                    │   Localisation Layer   │
                    │ SayvisStrings (offline │
                    │ dictionary) + AI tier  │
                    └───────────┬────────────┘
                                ▼
                    ┌────────────────────────┐
                    │     SayvisViewModel    │
                    │   (StateFlow / Flows)  │
                    └───────────┬────────────┘
                                │
        ┌───────────┬───────────┼───────────┬───────────────┐
        ▼           ▼           ▼           ▼               ▼
 ┌────────────┐┌────────────┐┌────────────┐┌──────────────┐┌───────────────┐
 │ UIC Engine ││Aware Engine││Memory System││ Script Engine││Settings Store │
 └─────┬──────┘└─────┬──────┘└─────┬──────┘└──────┬───────┘└──────┬────────┘
       │             │             │              │ effects only  │
       └─────────────┴─────────────┴──────────────┘               ▼
                                │                        ┌───────────────┐
                                ▼                        │  Secure Vault │
                    ┌────────────────────────┐           │(Android       │
                    │    AI Orchestrator     │           │ Keystore AES) │
                    │ Gemini / OpenRouter /  │           └───────────────┘
                    │ Groq / Custom / Local  │
                    └───────────┬────────────┘
                                │ (Proposed Action)
                                ▼
                    ┌────────────────────────┐
                    │ Zero-Trust Permission  │
                    │  (Risk / Kill Switch)  │
                    └───────────┬────────────┘
                                │ (Explicit Consent)
              ┌─────────────────┼─────────────────┐
              ▼                 ▼                 ▼
     ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐
     │Repository/DAO│  │   MT4/MT5    │  │ Automation router│
     │ (Room/Cache) │  │   Gateway    │  │ (notify/webhook) │
     └──────┬───────┘  └──────┬───────┘  └────────┬─────────┘
            │                 │                   │
            └─────────────────┴───────────────────┘
                                ▼
                    ┌────────────────────────┐
                    │   Audit Trail (SHA256) │
                    └────────────────────────┘
```

## Data Isolation & Decoupling
AI outputs are strictly proposals (`ProposedAction`) and never possess direct hardware, execution, or persistent write capabilities without moving through the `ZeroTrustPermissionEngine`.

## Trust Boundaries

| Boundary | Rule |
|---|---|
| AI → app | An answer is text. Any action it suggests becomes a `ProposedAction` shown to the owner. |
| Script → app | `ScriptEngine` performs **no I/O**. It returns `ScriptEffect` values; the ViewModel routes each one through the permission gate exactly like an AWARE proposal. |
| App → broker | `MetaTraderGateway.placeOrder()` re-checks connection, emergency lock, execution mode, live-confirmation, lot cap and drawdown cap on *every* call, not once at connect time. |
| Live trading | `LIVE_EXECUTION` on a real account requires an explicit in-session confirmation; dismissing the dialog drops the profile back to `DEMO_EXECUTION`. LIT analysis stays paper-only at the architecture level. |
| Secrets | Keys, broker password and bridge tokens live in `SecureVault` (AES-256/GCM, key inside the Android Keystore) and never enter the JSON settings blob. If secure hardware is unavailable the vault degrades to obfuscation and the UI says so. |
| Everything | Settings resets, gateway connections, execution-mode changes, orders, blocks and script runs are appended to the SHA-256 chained audit trail. |

## Presentation Layer
The bottom bar carries four general categories — **Home**, **Assistant**, **Tools**,
**Settings**. Every concrete capability lives inside Tools (grouped as Work & Planning,
Cognition & Context, Finance & Trading, System & Development) or inside Settings.
`SayvisScreen.primaryTab()` maps every secondary destination back to the tab that owns it,
so the bar stays highlighted and the top bar shows a back affordance.

`SayvisMainApp` provides three CompositionLocals that make localisation automatic:
`LocalLayoutDirection` (RTL for Persian), `LocalStrings` (the offline dictionary) and
`LocalTranslation` (the hybrid bridge). Screens use `SayvisText` instead of `Text` for
anything free-form, so no string can silently fall through to English.

## Degradation Paths
- **No network / offline mode**: the AI orchestrator falls back to `LocalCognitiveProvider`
  and *says so*; translation stops at the offline dictionary and cache; the gateway uses
  the local paper simulator.
- **No API key configured**: the app is fully usable; Settings shows what is missing and
  the probe reports the exact reason instead of a generic failure.
- **No secure hardware**: `SecureVault` reports `isHardwareBacked = false` and the Settings
  screen surfaces it rather than implying protection that does not exist.
