"""
SAYVIS Professional AI Agent - FastAPI Main Application
Self-hosted AI Starter Kit Professional Edition

Integrates:
- n8n (workflow automation)
- Ollama (local LLM)
- Qdrant (vector DB)
- Postgres (main DB)
- Redis (cache)
"""

from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from contextlib import asynccontextmanager
import logging
import time
from datetime import datetime

from .config import settings
from .api.routes import health, chat, missions, workflows, rag, quant

# Configure logging
logging.basicConfig(
    level=getattr(logging, settings.log_level.upper(), logging.INFO),
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s"
)
logger = logging.getLogger(__name__)

# Global services container
_services = {}

def get_services():
    return _services

@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan - startup and shutdown"""
    logger.info("🚀 SAYVIS Professional AI Agent starting...")
    logger.info(f"Environment: {settings.env}")
    logger.info(f"Ollama: {settings.ollama_base_url} / Model: {settings.ollama_model}")
    logger.info(f"Qdrant: {settings.qdrant_url}")
    logger.info(f"n8n: {settings.n8n_api_url}")

    try:
        # Initialize services
        from .services.ollama_service import OllamaService
        from .services.qdrant_service import QdrantService
        from .services.n8n_service import N8nService
        from .memory.manager import MemoryManager
        from .rag.engine import RAGEngine
        from .agents.tools.registry import ToolRegistry
        from .agents.tools.web_search import WebSearchTool
        from .agents.tools.calculator import CalculatorTool
        from .agents.tools.datetime_tool import DateTimeTool
        from .agents.tools.qdrant_tools import QdrantMemoryTool, QdrantKnowledgeTool
        from .agents.tools.trading_tool import TradingAnalysisTool
        from .agents.tools.n8n_tool import N8nWorkflowTool
        from .agents.tools.file_tool import FileManagerTool
        from .security.zero_trust import ZeroTrustEngine
        from .orchestrator.orchestrator import AgentOrchestrator

        # Core services
        logger.info("Initializing Ollama service...")
        ollama_service = OllamaService()
        _services["ollama"] = ollama_service

        logger.info("Initializing Qdrant service...")
        try:
            qdrant_service = QdrantService()
            qdrant_service.ensure_collections()
            _services["qdrant"] = qdrant_service
            logger.info("✅ Qdrant collections ensured")
        except Exception as e:
            logger.warning(f"⚠️ Qdrant init failed (will run offline): {e}")
            _services["qdrant"] = None

        logger.info("Initializing n8n service...")
        n8n_service = N8nService()
        _services["n8n"] = n8n_service

        # Security
        logger.info("Initializing Zero-Trust engine...")
        zero_trust = ZeroTrustEngine(emergency_lock=settings.emergency_lock)
        _services["zero_trust"] = zero_trust

        # Memory & RAG
        logger.info("Initializing Memory Manager...")
        memory_manager = MemoryManager(
            qdrant_service=_services.get("qdrant"),
            ollama_service=ollama_service
        )
        _services["memory"] = memory_manager

        logger.info("Initializing RAG Engine...")
        rag_engine = RAGEngine(
            qdrant_service=_services.get("qdrant"),
            ollama_service=ollama_service
        )
        _services["rag"] = rag_engine

        # Tools
        logger.info("Initializing Tool Registry...")
        tool_registry = ToolRegistry()
        
        # Register all tools
        tool_registry.register(CalculatorTool())
        tool_registry.register(DateTimeTool())
        tool_registry.register(FileManagerTool(base_path="./data"))
        tool_registry.register(TradingAnalysisTool())
        
        if settings.enable_web_search:
            tool_registry.register(WebSearchTool())
        
        tool_registry.register(QdrantMemoryTool(
            qdrant_service=_services.get("qdrant"),
            ollama_service=ollama_service
        ))
        tool_registry.register(QdrantKnowledgeTool(
            qdrant_service=_services.get("qdrant"),
            ollama_service=ollama_service
        ))
        
        if settings.enable_n8n_tools:
            tool_registry.register(N8nWorkflowTool(n8n_service=n8n_service))
        
        _services["tools"] = tool_registry
        logger.info(f"✅ Registered {len(tool_registry.tools)} tools")

        # Orchestrator
        logger.info("Initializing Agent Orchestrator...")
        orchestrator = AgentOrchestrator(
            ollama_service=ollama_service,
            memory_manager=memory_manager,
            rag_engine=rag_engine,
            tool_registry=tool_registry,
            zero_trust_engine=zero_trust,
            n8n_service=n8n_service
        )
        _services["orchestrator"] = orchestrator

        logger.info("🎉 SAYVIS Agent fully initialized and ready!")
        logger.info(f"📊 Tools: {list(tool_registry.tools.keys())}")

    except Exception as e:
        logger.error(f"❌ Startup failed: {e}", exc_info=True)
        # Don't crash - run in degraded mode
        _services["orchestrator"] = None

    yield

    # Shutdown
    logger.info("🛑 SAYVIS Agent shutting down...")
    try:
        if _services.get("ollama"):
            await _services["ollama"].close()
        if _services.get("n8n"):
            await _services["n8n"].close()
    except Exception as e:
        logger.warning(f"Shutdown cleanup failed: {e}")
    
    logger.info("👋 Goodbye!")

# Create FastAPI app
app = FastAPI(
    title="ODIN AGENT - Professional AI Agent",
    description="""
## ODIN AGENT - Sovereign Professional AI Agent Platform

**اودین ایجنت - عامل هوش مصنوعی حرفه‌ای و حاکمیتی**

A professional self-hosted AI agent built on:
- **n8n**: Workflow automation & orchestration
- **Ollama**: Local LLM (Llama 3.3, Qwen, etc.)
- **Qdrant**: Vector DB for memory & RAG
- **Postgres**: Main persistence
- **Redis**: Cache & short-term memory

### Features
- 🧠 Multi-tool reasoning
- 💾 Hierarchical memory (short/long-term)
- 📚 Advanced RAG
- 🛡️ Zero-Trust security
- 🎯 Mission engine
- 📈 Trading intelligence
- 🔄 n8n workflow integration
- 🇮🇷 Persian-first

### Quick Start
```bash
docker compose --profile cpu up
```

Then visit:
- API Docs: `/docs`
- n8n: http://localhost:5678
- Qdrant: http://localhost:6333/dashboard
    """,
    version="3.0.0-odin",
    lifespan=lifespan,
    docs_url="/docs",
    redoc_url="/redoc",
    openapi_url="/openapi.json"
)

# CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Request logging middleware
@app.middleware("http")
async def log_requests(request: Request, call_next):
    start_time = time.time()
    response = await call_next(request)
    elapsed = (time.time() - start_time) * 1000
    
    logger.info(f"{request.method} {request.url.path} -> {response.status_code} ({elapsed:.1f}ms)")
    return response

# Include routers
app.include_router(health.router, prefix="", tags=["Health"])
app.include_router(health.router, prefix="/api/v1", tags=["Health"])
app.include_router(chat.router, prefix="/api/v1", tags=["Chat"])
app.include_router(missions.router, prefix="/api/v1", tags=["Missions"])
app.include_router(workflows.router, prefix="/api/v1", tags=["Workflows"])
app.include_router(rag.router, prefix="/api/v1", tags=["RAG"])
app.include_router(quant.router, prefix="/api/v1", tags=["Quant Trading"])

# OpenAI-compatible endpoint for Android app integration
@app.post("/v1/chat/completions", tags=["OpenAI Compatible"])
async def openai_chat_completions(request: Request):
    """
    OpenAI-compatible chat endpoint for SAYVIS Android app integration
    """
    from .main import get_services
    
    try:
        body = await request.json()
        messages = body.get("messages", [])
        model = body.get("model", settings.ollama_model)
        
        # Extract last user message
        user_message = ""
        for msg in reversed(messages):
            if msg.get("role") == "user":
                user_message = msg.get("content", "")
                break
        
        if not user_message:
            return JSONResponse(
                status_code=400,
                content={"error": "No user message found"}
            )

        services = get_services()
        orchestrator = services.get("orchestrator")
        
        if not orchestrator:
            # Fallback response
            return {
                "id": "chatcmpl-sayvis-offline",
                "object": "chat.completion",
                "created": int(datetime.utcnow().timestamp()),
                "model": model,
                "choices": [{
                    "index": 0,
                    "message": {
                        "role": "assistant",
                        "content": "SAYVIS offline core: I received your message but orchestrator is not ready. Please check docker services."
                    },
                    "finish_reason": "stop"
                }],
                "usage": {"prompt_tokens": 0, "completion_tokens": 0, "total_tokens": 0}
            }

        result = await orchestrator.process_message(
            message=user_message,
            session_id=body.get("session_id", "openai-compat"),
            language="auto",
            use_tools=True,
            use_memory=True,
            use_rag=True
        )

        return {
            "id": f"chatcmpl-sayvis-{int(time.time())}",
            "object": "chat.completion",
            "created": int(datetime.utcnow().timestamp()),
            "model": result.get("model", model),
            "choices": [{
                "index": 0,
                "message": {
                    "role": "assistant",
                    "content": result["response"]
                },
                "finish_reason": "stop"
            }],
            "usage": {
                "prompt_tokens": len(user_message.split()),
                "completion_tokens": len(result["response"].split()),
                "total_tokens": len(user_message.split()) + len(result["response"].split())
            },
            "sayvis_meta": {
                "tools_used": result.get("tools_used", []),
                "processing_time_ms": result.get("processing_time_ms", 0)
            }
        }

    except Exception as e:
        logger.error(f"OpenAI compat endpoint failed: {e}", exc_info=True)
        return JSONResponse(
            status_code=500,
            content={"error": str(e)}
        )

# Root
@app.get("/", tags=["Root"])
async def root():
    return {
        "name": "ODIN AGENT - Professional AI Agent",
        "name_fa": "اودین ایجنت - عامل هوش مصنوعی حرفه‌ای",
        "version": "3.0.0-odin",
        "status": "operational",
        "description": "Odin Sovereign Professional AI Agent Platform",
        "services": {
            "n8n": "http://localhost:5678",
            "qdrant": "http://localhost:6333/dashboard",
            "ollama": "http://localhost:11434",
            "api_docs": "/docs"
        },
        "quick_start": "docker compose --profile cpu up",
        "timestamp": datetime.utcnow().isoformat()
    }

# Global exception handler
@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    logger.error(f"Unhandled exception: {exc}", exc_info=True)
    return JSONResponse(
        status_code=500,
        content={
            "error": "Internal server error",
            "detail": str(exc),
            "path": str(request.url.path),
            "timestamp": datetime.utcnow().isoformat()
        }
    )

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "app.main:app",
        host=settings.host,
        port=settings.port,
        reload=True,
        log_level=settings.log_level.lower()
    )
