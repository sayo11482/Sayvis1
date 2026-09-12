# SAYVIS Threat Model

| Threat ID | Threat Vector | Impact | Likelihood | Mitigation Strategy | Detection & Recovery |
|-----------|---------------|--------|------------|---------------------|----------------------|
| TM-01 | Prompt Injection via External Document/Chat | Critical | Medium | Strict decoupling of instructions from data. LLM output treated strictly as untrusted proposed action. | AI cannot execute commands directly; must pass schema validator and Zero-Trust gate. |
| TM-02 | Compromised Device Node | High | Low | Ephemeral cryptographic device pairing. Immediate capability to `REVOKE` or `SUSPEND` device trust. | Audit log hash deviation alerts user; remote pairing keys invalidated. |
| TM-03 | Replay & Tampering of Audit Logs | High | Low | SHA-256 linear hash chaining ($H_n = \text{Hash}(Data_n + H_{n-1})$). | Broken hash chain indicates immediate tampering; emergency lock auto-engages. |
| TM-04 | Unauthorized Financial Execution (LIT Engine) | Critical | Very Low | Hard prohibition of live trading. LIT engine operates strictly in `Paper Trading` simulation mode. | Trading gate enforces double-confirmation modal; orders cannot route to real brokers. |
| TM-05 | Inferred Attribute Hallucination in UIC | Medium | High | Epistemic states ensure inferences start as `INFERRED` with low confidence until confirmed by owner. | Owner can inspect, override, or revoke any cognitive attribute at any time. |
