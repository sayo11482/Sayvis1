# SAYVIS Professional AI Agent - Quick Start (2 minutes)

## For the User Who Sent n8n Instructions

You sent:
```
git clone https://github.com/n8n-io/self-hosted-ai-starter-kit.git
cd self-hosted-ai-starter-kit
docker compose --profile cpu up
# n8n at localhost:5678
```

**We built the PROFESSIONAL version for SAYVIS!**

### Our Professional Agent - Same Commands, More Power:

```bash
git clone https://github.com/sayo11482/Sayvis1.git
cd Sayvis1/agent
cp .env.example .env
docker compose --profile cpu up -d

# Pull models
docker exec -it sayvis-ollama ollama pull qwen2.5:7b
docker exec -it sayvis-ollama ollama pull nomic-embed-text
```

**You get:**
- ✅ n8n at http://localhost:5678 (same as starter kit)
- ✅ PLUS SAYVIS Agent API at http://localhost:8000
- ✅ PLUS Qdrant at http://localhost:6333/dashboard
- ✅ PLUS Professional tools, memory, RAG, zero-trust security
- ✅ PLUS Android app integration
- ✅ PLUS 4 ready n8n workflows
- ✅ PLUS 27 passing tests

### Test It

```bash
curl http://localhost:8000/health
curl -X POST http://localhost:8000/api/v1/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "سلام سایویس!", "language": "fa"}'
```

### Android App

1. Settings → AI & API → **SAYVIS Professional Agent**
2. URL: `http://YOUR_PC_IP:8000` (not localhost from phone!)
3. Test connection → ✅

### What We Built Professionally

**Backend (FastAPI):**
- `app/main.py` - Main API with OpenAI-compatible endpoint
- `app/orchestrator/` - Multi-step reasoning with tools
- `app/agents/tools/` - 7 professional tools with risk levels
- `app/memory/` - Hierarchical memory (short + long via Qdrant)
- `app/rag/` - RAG with smart chunking
- `app/security/` - Zero-trust engine mirroring Android app
- `app/services/` - Ollama, Qdrant, n8n integrations

**n8n Workflows (4 ready):**
- Main agent orchestrator
- RAG auto-ingestion
- Trading analysis
- Mission auto-breakdown

**Android Integration:**
- New provider `SAYVIS_AGENT`
- `SayvisAgentProvider.kt` - Full implementation
- Settings UI with Persian support
- Health check & error handling

**Tests (27 passing):**
- Core agent logic
- Security (97% coverage)
- Tools (safe execution)
- API (mocked)
- Offline resilience

**Docs:**
- `README.md` - Full guide
- `docs/ARCHITECTURE.md` - Architecture
- `docs/SETUP_FA.md` - Persian setup
- `docs/TEST_RESULTS.md` - Test report
- `frontend/index.html` - Web dashboard

### Professional Features vs Starter Kit

| Feature | Starter Kit | SAYVIS Professional |
|---------|-------------|---------------------|
| n8n | ✅ | ✅ + 4 workflows |
| Ollama | ✅ | ✅ + fallback core |
| Qdrant | ✅ | ✅ + memory + RAG |
| Postgres | ✅ | ✅ + missions |
| Agent API | ❌ | ✅ FastAPI + tools |
| Zero-Trust | ❌ | ✅ Full engine |
| Memory | ❌ | ✅ Hierarchical |
| RAG | ❌ | ✅ Smart chunking |
| Android | ❌ | ✅ Integrated |
| Tests | ❌ | ✅ 27 tests |
| Persian | ❌ | ✅ Full support |

### One-Liner Demo

```bash
# After docker compose up
curl -s http://localhost:8000/ | jq
# Should return SAYVIS info

# Chat in Persian
curl -s -X POST http://localhost:8000/api/v1/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "یک ماموریت برای یادگیری معامله بساز", "language": "fa"}' | jq .response
```

Enjoy! 🚀
