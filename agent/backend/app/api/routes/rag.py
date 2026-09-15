from fastapi import APIRouter, HTTPException
from ...models.schemas import RAGIngestRequest, RAGSearchRequest
from typing import List
import logging

router = APIRouter()
logger = logging.getLogger(__name__)

@router.post("/rag/ingest", tags=["RAG"])
async def ingest_document(request: RAGIngestRequest):
    from ...main import get_services
    
    services = get_services()
    rag = services.get("rag")
    
    if not rag:
        raise HTTPException(status_code=503, detail="RAG engine not available")
    
    try:
        result = await rag.ingest(
            content=request.content,
            source=request.source,
            metadata=request.metadata,
            chunk_size=request.chunk_size,
            chunk_overlap=request.chunk_overlap
        )
        return result
    except Exception as e:
        logger.error(f"RAG ingest failed: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@router.post("/rag/search", tags=["RAG"])
async def search_knowledge(request: RAGSearchRequest):
    from ...main import get_services
    
    services = get_services()
    rag = services.get("rag")
    
    if not rag:
        return {"query": request.query, "results": [], "count": 0, "offline": True}
    
    try:
        results = await rag.search(
            query=request.query,
            top_k=request.top_k,
            filter_source=request.filter_source
        )
        return {
            "query": request.query,
            "results": results,
            "count": len(results)
        }
    except Exception as e:
        logger.error(f"RAG search failed: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@router.get("/rag/stats", tags=["RAG"])
async def rag_stats():
    from ...main import get_services
    
    services = get_services()
    qdrant = services.get("qdrant")
    
    if not qdrant:
        return {"collections": {}, "offline": True}
    
    try:
        from ...config import QDRANT_COLLECTIONS
        stats = {}
        for key, name in QDRANT_COLLECTIONS.items():
            try:
                count = qdrant.count(collection=name)
                stats[key] = {"name": name, "count": count}
            except Exception as e:
                stats[key] = {"name": name, "count": 0, "error": str(e)}
        
        return {"collections": stats}
    except Exception as e:
        logger.error(f"RAG stats failed: {e}")
        return {"collections": {}, "error": str(e)}
