# SAYVIS Professional AI Agent - Architecture

## Overview

```
                    ┌─────────────────────────────────┐
                    │     SAYVIS Android App          │
                    │  (Kotlin + Jetpack Compose)     │
                    │  New Provider: SAYVIS_AGENT     │
                    └──────────────┬──────────────────┘
                                   │ OpenAI-compatible /api/v1/chat
                                   ▼
                    ┌─────────────────────────────────┐
                    │   SAYVIS Agent Backend (FastAPI)│
                    │   Port 8000 - Main Orchestrator │
                    └──────────────┬──────────────────┘
                                   │
        ┌──────────────┬───────────┼───────────┬──────────────┐
        ▼              ▼           ▼           ▼              ▼
   ┌─────────┐   ┌─────────┐  ┌─────────┐ ┌─────────┐  ┌─────────┐
   │ Ollama  │   │ Qdrant  │  │Postgres │ │   n8n   │  │  Redis  │
   │ LLM     │   │ Vector  │  │   DB    │ │Workflow │  │  Cache  │
   │ :11434  │   │ :6333   │  │ :5432   │ │ :5678   │  │ :6379   │
   └─────────┘   └─────────┘  └─────────┘ └─────────┘  └─────────┘
        │              │           │           │
        └──────────────┴───────────┴───────────┘
                       ▼
              ┌─────────────────┐
              │  Zero-Trust     │
              │  Audit Chain    │
              │  SHA256         │
              └─────────────────┘
```

## Core Components

### 1. Agent Orchestrator (`orchestrator/orchestrator.py`)
- Multi-step reasoning loop
- Tool calling with parsing (JSON, TOOL:, XML)
- Memory & RAG integration
- Language detection (FA/EN)
- n8n workflow triggering

### 2. Tool Registry (`agents/tools/`)
Professional tools with risk classification:

| Tool | Risk | Description |
|------|------|-------------|
| calculator | LOW | Safe math |
| datetime_tool | LOW | Jalali/Gregorian |
| qdrant_memory_search | LOW | Long-term memory |
| qdrant_knowledge_search | LOW | RAG |
| web_search | MEDIUM | Tavily/DuckDuckGo |
| trading_analysis | MEDIUM | Market analysis |
| file_manager | MEDIUM/HIGH | Safe file ops |
| n8n_workflow | HIGH | Workflow trigger |

### 3. Memory System (`memory/manager.py`)
- **Short-term**: In-memory dict per session (20 messages max)
- **Long-term**: Qdrant vector DB with embeddings
- **Episodic**: Postgres (future)

### 4. RAG Engine (`rag/engine.py`)
- Smart chunking (sentence boundaries)
- Overlap handling
- Embedding via Ollama
- Reranking by keyword overlap
- Source tracking

### 5. Zero-Trust Security (`security/zero_trust.py`)
Mirrors Android app's engine:
- Risk classification
- Emergency lock blocks HIGH/CRITICAL
- SHA256 audit chain
- Tamper-evident verification

### 6. Services
- **OllamaService**: LLM + embeddings, streaming, fallback
- **QdrantService**: Vector ops, collection management
- **N8nService**: Workflow listing, webhook triggering

### 7. API Routes
- `/api/v1/chat` - Main chat
- `/api/v1/missions` - Mission CRUD
- `/api/v1/rag/*` - Knowledge base
- `/api/v1/workflows/*` - n8n integration
- `/v1/chat/completions` - OpenAI-compatible (for Android)
- `/health`, `/status` - Monitoring

## Data Flow

1. **User Message** -> FastAPI `/api/v1/chat`
2. **Language Detection** -> FA/EN
3. **Memory Context** -> Short + Long term from Qdrant
4. **RAG Context** -> Knowledge base search
5. **System Prompt** -> Built with tools, context, RAG
6. **LLM Generation** -> Ollama (or fallback)
7. **Tool Parsing** -> Extract TOOL calls
8. **Zero-Trust Check** -> Risk classification
9. **Tool Execution** -> Via registry
10. **Follow-up Generation** -> With tool results
11. **Memory Save** -> Short + Long term
12. **n8n Trigger** -> Workflows
13. **Response** -> With metadata

## Self-Hosted Stack

### docker-compose.yml Services
- **postgres**: Main DB, n8n DB
- **qdrant**: Vector DB (6333/6334)
- **ollama**: LLM server (11434)
- **n8n**: Workflow automation (5678)
- **redis**: Cache (6379)
- **agent**: FastAPI backend (8000)

### Profiles
- `cpu`: CPU-only Ollama
- `gpu`: NVIDIA GPU
- `gpu-nvidia`: Explicit NVIDIA

## Android Integration

New provider `SAYVIS_AGENT` added:

```kotlin
enum class AiProviderKind {
    SAYVIS_AGENT("عامل حرفه‌ای سایویس", "SAYVIS Professional Agent")
}

data class AiSettings(
    val sayvisAgentBaseUrl: String = "http://localhost:8000",
    val sayvisAgentApiKey: String = "",
    val sayvisAgentModel: String = "sayvis-agent"
)
```

Provider implementation:
- `SayvisAgentProvider.kt` - Handles both `/api/v1/chat` and OpenAI-compatible `/v1/chat/completions`
- Health check via `/health`
- Supports API key auth
- Persian/English error messages
- Fallback between endpoints

## Security Model

### Zero-Trust Levels
- **LOW**: Auto-approved (calculator, datetime, memory search)
- **MEDIUM**: Auto with notification (web_search, trading_analysis)
- **HIGH**: Requires confirmation (n8n_workflow, file write)
- **CRITICAL**: Blocked until explicit (trading_order, delete)

### Emergency Lock
When engaged:
- Blocks HIGH/CRITICAL
- Only LOW/MEDIUM allowed
- Audit logged
- UI shows warning

### Audit Chain
- SHA256 chained hashes
- Prev hash linking
- Tamper-evident
- Actor, action, risk, timestamp

## Testing

- **Unit**: Tools, security, memory
- **Integration**: API endpoints with mocks
- **Coverage**: 46% overall, 97% for security
- **Offline**: All tests pass without external services

## Future Enhancements

- [ ] Postgres persistence for missions
- [ ] Redis for short-term memory
- [ ] Real MT4/MT5 bridge integration
- [ ] WebSocket streaming
- [ ] Multi-agent collaboration
- [ ] Voice I/O
- [ ] Mobile push notifications via n8n
