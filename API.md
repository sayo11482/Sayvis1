# SAYVIS — AI Orchestration, Trading Gateway, Scripting & Deployment Guide

## 1. AI Orchestrator
- Routes between the owner-selected cloud provider and the deterministic local fallback
  (`LocalCognitiveProvider`).
- **Providers are configured at runtime**, not only at build time. The owner enters the
  key in *Settings → Artificial Intelligence & API*; the compile-time `GEMINI_API_KEY`
  secret is still honoured as a fallback for packaged builds.

| Provider | Settings fields used | Endpoint |
|---|---|---|
| `LOCAL` | — | on-device, always available |
| `GEMINI` | `geminiApiKey`, `geminiModel` | `POST https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent` |
| `OPENROUTER` | `openRouterApiKey`, `openRouterModel` | `POST https://openrouter.ai/api/v1/chat/completions` |
| `GROQ` | `groqApiKey`, `groqModel` | `POST https://api.groq.com/openai/v1/chat/completions` |
| `CUSTOM` | `customBaseUrl`, `customModel`, `customApiKey` | `POST {baseUrl}/v1/chat/completions` (OpenAI-compatible) |

- Any provider failure degrades to the local core and **reports the reason to the owner**
  in the chat instead of failing silently.
- `Settings → Test connection` runs a live probe and reports latency plus the real error.

### Secret storage
Keys, the broker password and bridge tokens are encrypted with an AES-256/GCM key held in
the **Android Keystore** (`settings/SecureVault.kt`) and are never written to the JSON
settings blob. If secure hardware is unavailable the vault falls back to obfuscation and
the Settings screen says so explicitly.

## 2. Trading Terminal Gateway (MetaTrader 4/5)
Android cannot speak the MT4/MT5 wire protocol — MetaQuotes ships no mobile broker API —
so a connection always goes through a **bridge**. `trading/MetaTraderGateway.kt` supports
three transports, selectable in *Settings → Trading Gateway*:

### 2.1 `METAAPI` (hosted gateway)
Base URL defaults to `https://mt-client-api-v1.agiliumtrade.ai`; the account token is sent
in the `auth-token` header.

```
GET  /users/current/accounts/{token}/account
GET  /users/current/accounts/{token}/positions
GET  /users/current/accounts/{token}/symbol-info/{SYMBOL}
POST /users/current/accounts/{token}/trade
     { "actionType": "ORDER_TYPE_BUY" | "ORDER_TYPE_SELL",
       "symbol": "EURUSD", "volume": 0.10, "slippage": 20,
       "stopLoss": 1.0800, "takeProfit": 1.0950, "comment": "SAYVIS" }
```

### 2.2 `SELF_HOSTED` (owner's Expert Advisor / Manager-API bridge)
The owner runs a small REST service next to their terminal (typically on a VPS). Base URL
and an optional `Authorization: Bearer <token>` come from the profile; the login is sent
as `X-MT-Login`.

```
GET  {baseUrl}/account     -> { login, server, broker, currency, balance, equity,
                               margin, freeMargin, marginLevel, leverage }
GET  {baseUrl}/positions   -> { positions: [ { id, symbol, type, volume, openPrice,
                               currentPrice, stopLoss, takeProfit, profit, comment } ] }
GET  {baseUrl}/symbol-info/{SYMBOL} -> { bid, ask, digits }
POST {baseUrl}/trade       -> { side: "BUY"|"SELL", symbol, volume, slippage,
                               stopLoss?, takeProfit?, comment }  => { id }
```

### 2.3 `OFFLINE_SIM` (local paper simulator)
No network call is made. A bounded random-walk market produces plausible quotes, an
account and open positions so the whole surface stays usable and testable offline, and
every value is labelled *simulated*.

### Execution gating
`placeOrder()` re-checks, on every call, in this order:

1. gateway connected
2. emergency lock disengaged
3. `executionMode != PAPER_SIMULATION`
4. if `LIVE_EXECUTION` on a `REAL` account → an explicit in-session confirmation exists
5. `volume <= maxLotSize`
6. daily loss cap not breached (when `autoCloseOnDrawdown`)
7. symbol present

`LIVE_EXECUTION` can only be selected through a re-confirmation dialog; dismissing it
drops the profile back to `DEMO_EXECUTION`. Every mode change, order and block is written
to the tamper-evident audit chain.

## 3. Scripting & Automation
`scripts/ScriptEngine.kt` implements a small line-based rule language. It performs **no
I/O**: it returns `ScriptEffect` values that the ViewModel pushes through the zero-trust
gate.

```
SET $cap = 50
WHEN account.profit < 0 - $cap THEN block "Daily loss cap reached"
WHEN battery < 20 THEN notify "Battery is at {battery}%"
ON MARKET THEN log "Market tick evaluated"
WHEN network.online == false THEN webhook "https://example.com/hook" "{\"state\":\"offline\"}"
```

- Variables: `battery`, `battery.charging`, `network.online`, `lock.active`,
  `focus.active`, `load`, `mission.active`, `mission.blocked`, `time.hour`, `time.minute`,
  `account.balance`, `account.equity`, `account.profit`, `quote.SYMBOL.bid|ask`
- Operators: `< > <= >= == !=` and `+ - * /` with parentheses; unary minus is only unary
  in operand position, so `0 - $cap` is subtraction.
- Unknown variables resolve to `null` and comparisons **fail closed** (the rule does not
  fire) rather than throwing.
- Actions: `notify`, `log`, `propose`, `block`, `webhook`, `set`, `set_mode`.
- `set_mode` only *requests* a change; execution mode is never altered by a script.
- Webhooks require automation enabled **and** the emergency lock disengaged, and are
  audited.
- Scripts persist as JSON in `ScriptStore` (deliberately outside Room so the language can
  evolve without a migration).

## 4. Localisation
Two layers, because a single approach cannot cover both cases:

1. **Built-in dictionary** (`i18n/SayvisStrings.kt`) — every piece of interface chrome in
   Persian and English, resolved offline. `i18n/ContextLocalization.kt` renders the
   language-neutral `ContextSnapshot`, and `i18n/PersianFormat.kt` handles Persian digits,
   currency word order (`۱٬۲۵۰ دلار`) and Jalali dates.
2. **Hybrid free-text translation** (`ai/TranslationService.kt`) — for text that cannot be
   enumerated (AI answers, broker notes, owner-entered titles):
   - exact dictionary match (covers seed data and every string the AWARE engine emits),
   - numeric / quoted templates, so `"Battery level at 17%"` resolves offline and nested
     fragments such as an embedded task title are translated recursively,
   - a persisted cache,
   - online AI translation as the last tier, disabled entirely in offline mode.

Machine identifiers, tickers and fingerprints are detected and left untouched rather than
being sent to a model. `SayvisText` is the drop-in `Text` replacement that runs this
pipeline and can mark machine-translated output.

## 5. Memory, Missions, Devices & Audit
- **Memory**: Working / Episodic / Semantic / Preference / Long-Term layers in
  `memory_items` with provenance and priority.
- **Missions**: `Goal → Mission → Objectives → Tasks` with completion percentages and
  `PLANNED / ACTIVE / AT_RISK / BLOCKED / COMPLETED / CANCELLED`.
- **Devices**: public-key authenticated pairing with suspend, verify and revoke.
- **Audit**: SHA-256 chained events covering settings resets, gateway connections,
  execution-mode changes, orders, blocks and script runs.

## 6. Testing & Verification
- JVM unit tests for zero-trust gates, SHA-256 chaining, UIC transitions and AWARE rules.
- Roborazzi visual regression.
- The scripting grammar and the translation dictionary are additionally verified against
  the real strings the app emits, so coverage regressions are caught.

## 7. Deployment & Privacy
- Offline-first standalone APK; zero telemetry.
- All biometric, cognitive, audit, credential and trading data remain on-device under
  owner sovereign control.
