# SAYVIS AI Orchestration, Memory, Missions, LIT & Deployment Guide

## 1. AI Orchestrator (`AI_ORCHESTRATION.md`)
- Dynamically routes between remote cloud reasoning (Gemini API) and deterministic local cognitive fallback (`LocalCognitiveProvider`).
- Automatically switches to offline cognitive rules when network connectivity drops or `Force Offline Mode` is toggled.
- Enforces strict input validation to prevent prompt injection vectors from hijacking system policies.

## 2. Memory System (`MEMORY.md`)
- Stratified into Working, Episodic, Semantic, Preference, and Long-Term layers.
- Indexed within Room Database (`memory_items`) with provenance and priority tracking.

## 3. Mission Engine (`MISSION_ENGINE.md`)
- Manages goal decomposition: `Goal -> Mission -> Objectives -> Tasks`.
- Tracks real-time completion percentages and dependency status (`PLANNED`, `ACTIVE`, `COMPLETED`, `PAUSED`).

## 4. Life Simulation & LIT Trading (`LIFE_SIMULATION.md` / `LIT.md`)
- **Life Simulation**: Probabilistic scenario analysis evaluating strategic life, career, and financial paths with explicit uncertainty disclaimers.
- **LIT (Liquidity Intelligence & Trading)**: Structural market order-flow analysis strictly quarantined to **Paper Trading Mode**. Live execution is permanently prohibited at architectural level.

## 5. Device Security & Companion Arms (`DEVICE_SECURITY.md`)
- Public-key authenticated cryptographic device pairing.
- Granular capability to suspend, verify, or permanently revoke paired companion nodes (Android, Windows, Web).

## 6. API Architecture & Gateway (`API.md`)
- Versioned REST endpoints:
  - `/api/v1/auth` - Multi-factor cryptographic authentication
  - `/api/v1/ai` - AI request gateway with secret isolation
  - `/api/v1/sync` - Multi-node event-sourced replication
  - `/api/v1/audit` - Verifiable audit log sync

## 7. Testing & Verification (`TESTING.md`)
- JVM local unit tests verifying Zero-Trust gates, SHA-256 hash chaining, UIC epistemic transitions, and AWARE opportunity rules.
- Roborazzi visual regression and screenshot validation.

## 8. Deployment & Privacy (`DEPLOYMENT.md` / `PRIVACY.md`)
- Offline-first standalone Android APK deployment.
- Zero-telemetry policy; all biometric, cognitive, and audit data remain on-device under owner sovereign control.
