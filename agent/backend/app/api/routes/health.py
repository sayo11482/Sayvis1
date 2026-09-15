from fastapi import APIRouter, Depends
from ...models.schemas import HealthResponse, AgentStatus
from datetime import datetime
import time
import logging

router = APIRouter()
logger = logging.getLogger(__name__)

# Global start time
_start_time = time.time()

@router.get("/health", response_model=HealthResponse, tags=["Health"])
async def health_check():
    """Basic health check"""
    # Try to check services
    services = {
        "api": True,
        "postgres": False,
        "qdrant": False,
        "ollama": False,
        "n8n": False,
        "redis": False
    }

    # Import here to avoid circular deps
    try:
        from ...main import get_services
        svc = get_services()
        
        if svc.get("qdrant"):
            services["qdrant"] = await svc["qdrant"].health_check() if hasattr(svc["qdrant"], "health_check") else True
        
        if svc.get("ollama"):
            services["ollama"] = await svc["ollama"].health_check()
        
        if svc.get("n8n"):
            services["n8n"] = await svc["n8n"].health_check()
        
        # Postgres and Redis assumed healthy if services exist
        if svc.get("memory"):
            services["postgres"] = True
            services["redis"] = True

    except Exception as e:
        logger.warning(f"Health check service probe failed: {e}")

    status = "healthy" if services["api"] else "degraded"
    
    return HealthResponse(
        status=status,
        timestamp=datetime.utcnow(),
        services=services,
        version="1.0.0"
    )

@router.get("/status", response_model=AgentStatus, tags=["Health"])
async def agent_status():
    """Detailed agent status"""
    from ...main import get_services
    from ...config import settings
    
    svc = get_services()
    
    ollama_ok = False
    qdrant_ok = False
    n8n_ok = False
    postgres_ok = False
    
    try:
        if svc.get("ollama"):
            ollama_ok = await svc["ollama"].health_check()
        if svc.get("qdrant"):
            qdrant_ok = svc["qdrant"].health_check() if hasattr(svc["qdrant"], "health_check") else False
            if callable(qdrant_ok):
                qdrant_ok = await qdrant_ok
        if svc.get("n8n"):
            n8n_ok = await svc["n8n"].health_check()
        if svc.get("memory"):
            postgres_ok = True
    except Exception as e:
        logger.warning(f"Status check failed: {e}")

    # Get counts
    total_memories = 0
    try:
        if svc.get("qdrant"):
            total_memories = svc["qdrant"].count()
    except:
        pass

    active_sessions = 0
    try:
        if svc.get("memory"):
            active_sessions = len(svc["memory"].short_term)
    except:
        pass

    return AgentStatus(
        status="operational" if ollama_ok or postgres_ok else "degraded",
        version="1.0.0",
        ollama_connected=ollama_ok,
        qdrant_connected=qdrant_ok,
        postgres_connected=postgres_ok,
        n8n_connected=n8n_ok,
        current_model=settings.ollama_model,
        embed_model=settings.ollama_embed_model,
        active_sessions=active_sessions,
        total_missions=0,  # TODO: from DB
        total_memories=total_memories,
        emergency_lock=settings.emergency_lock,
        uptime_seconds=int(time.time() - _start_time)
    )

@router.get("/ready", tags=["Health"])
async def readiness():
    """Kubernetes readiness probe"""
    return {"ready": True, "timestamp": datetime.utcnow().isoformat()}

@router.get("/live", tags=["Health"])
async def liveness():
    """Kubernetes liveness probe"""
    return {"alive": True, "timestamp": datetime.utcnow().isoformat()}
