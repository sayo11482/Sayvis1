from fastapi import APIRouter, HTTPException, Depends
from ...models.schemas import ChatRequest, ChatResponse
from datetime import datetime
import logging
import time

router = APIRouter()
logger = logging.getLogger(__name__)

@router.post("/chat", response_model=ChatResponse, tags=["Chat"])
async def chat(request: ChatRequest):
    """
    Main chat endpoint - Professional AI Agent conversation
    """
    from ...main import get_services
    
    try:
        services = get_services()
        orchestrator = services.get("orchestrator")
        
        if not orchestrator:
            raise HTTPException(status_code=503, detail="Agent orchestrator not ready")

        result = await orchestrator.process_message(
            message=request.message,
            session_id=request.session_id,
            language=request.language,
            use_tools=request.use_tools,
            use_memory=request.use_memory,
            use_rag=request.use_rag,
            context=request.context
        )

        return ChatResponse(
            response=result["response"],
            session_id=result["session_id"],
            provider_used=result["provider_used"],
            model=result["model"],
            tools_used=result.get("tools_used", []),
            memory_hits=result.get("memory_hits", []),
            rag_sources=result.get("rag_sources", []),
            processing_time_ms=result["processing_time_ms"],
            timestamp=datetime.utcnow()
        )

    except Exception as e:
        logger.error(f"Chat failed: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"Chat processing failed: {str(e)}")

@router.post("/chat/stream", tags=["Chat"])
async def chat_stream(request: ChatRequest):
    """
    Streaming chat endpoint
    """
    from fastapi.responses import StreamingResponse
    from ...main import get_services
    import json

    async def generate():
        try:
            services = get_services()
            ollama = services.get("ollama")
            
            if not ollama:
                yield f"data: {json.dumps({'error': 'Ollama not available'})}\n\n"
                return

            # Simple streaming
            async for chunk in ollama.generate_stream(
                prompt=request.message,
                temperature=0.7
            ):
                yield f"data: {json.dumps({'token': chunk})}\n\n"
            
            yield f"data: {json.dumps({'done': True})}\n\n"

        except Exception as e:
            yield f"data: {json.dumps({'error': str(e)})}\n\n"

    return StreamingResponse(generate(), media_type="text/event-stream")

@router.get("/chat/sessions/{session_id}/history", tags=["Chat"])
async def get_chat_history(session_id: str, limit: int = 20):
    from ...main import get_services
    
    services = get_services()
    memory = services.get("memory")
    
    if not memory:
        return {"session_id": session_id, "messages": [], "count": 0}
    
    messages = memory.get_short_term(session_id, limit=limit)
    return {
        "session_id": session_id,
        "messages": messages,
        "count": len(messages)
    }

@router.delete("/chat/sessions/{session_id}", tags=["Chat"])
async def clear_session(session_id: str):
    from ...main import get_services
    
    services = get_services()
    memory = services.get("memory")
    
    if memory:
        memory.clear_session(session_id)
    
    return {"session_id": session_id, "cleared": True, "timestamp": datetime.utcnow().isoformat()}

@router.post("/chat/memory/search", tags=["Chat"])
async def search_memory(query: str, top_k: int = 5, session_id: str = None):
    from ...main import get_services
    
    services = get_services()
    memory = services.get("memory")
    
    if not memory:
        return {"query": query, "results": [], "count": 0}
    
    results = await memory.search_long_term(query, top_k=top_k, session_id=session_id)
    return {"query": query, "results": results, "count": len(results)}
