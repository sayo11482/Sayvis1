# SAYVIS System Architecture

```text
                    ┌────────────────────────┐
                    │       SAYVIS UI        │
                    │   (Jetpack Compose)    │
                    └───────────┬────────────┘
                                │
                                ▼
                    ┌────────────────────────┐
                    │     SayvisViewModel    │
                    │   (StateFlow / Flows)  │
                    └───────────┬────────────┘
                                │
             ┌──────────────────┼──────────────────┐
             ▼                  ▼                  ▼
      ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
      │  UIC Engine │    │ Aware Engine│    │Memory System│
      └──────┬──────┘    └──────┬──────┘    └──────┬──────┘
             │                  │                  │
             └──────────────────┼──────────────────┘
                                ▼
                    ┌────────────────────────┐
                    │    AI Orchestrator     │
                    │  (Gemini + Local AI)   │
                    └───────────┬────────────┘
                                │ (Proposed Action)
                                ▼
                    ┌────────────────────────┐
                    │ Zero-Trust Permission  │
                    │  (Risk / Kill Switch)  │
                    └───────────┬────────────┘
                                │ (Explicit Consent)
                                ▼
                    ┌────────────────────────┐
                    │   Repository & DAOs    │
                    │ (Room Database / Cache)│
                    └───────────┬────────────┘
                                │
                                ▼
                    ┌────────────────────────┐
                    │   Audit Trail (SHA256) │
                    └────────────────────────┘
```

## Data Isolation & Decoupling
AI outputs are strictly proposals (`ProposedAction`) and never possess direct hardware, execution, or persistent write capabilities without moving through the `ZeroTrustPermissionEngine`.
