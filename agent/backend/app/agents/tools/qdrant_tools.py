from .registry import BaseTool
from typing import Dict, Any, List
from ...models.schemas import RiskLevel
import logging

logger = logging.getLogger(__name__)

class QdrantMemoryTool(BaseTool):
    name = "qdrant_memory_search"
    description = "Search long-term memory and past conversations in Qdrant vector DB."
    description_fa = "جستجو در حافظه بلندمدت و مکالمات گذشته در پایگاه برداری Qdrant."
    risk_level = RiskLevel.LOW

    def __init__(self, qdrant_service=None, ollama_service=None):
        self.qdrant = qdrant_service
        self.ollama = ollama_service

    async def execute(self, input_data: Dict[str, Any], context: Dict[str, Any] = None) -> Dict[str, Any]:
        query = input_data.get("query", "")
        top_k = input_data.get("top_k", 5)
        category = input_data.get("category")

        if not query:
            return {"success": False, "error": "Query required"}

        if not self.qdrant:
            return {
                "success": True,
                "query": query,
                "results": [],
                "message": "Qdrant service not available, using offline mode",
                "offline": True
            }

        try:
            # Generate embedding for query
            if self.ollama:
                embedding = await self.ollama.embed(query)
                if embedding and any(embedding):
                    results = self.qdrant.search(
                        query_vector=embedding,
                        top_k=top_k,
                        category=category
                    )
                    return {
                        "success": True,
                        "query": query,
                        "results": results,
                        "count": len(results),
                        "provider": "qdrant_vector"
                    }

            # Fallback to text search
            results = self.qdrant.search_by_text(query, top_k=top_k)
            return {
                "success": True,
                "query": query,
                "results": results,
                "count": len(results),
                "provider": "qdrant_text_fallback"
            }

        except Exception as e:
            logger.error(f"Memory search failed: {e}")
            return {"success": False, "error": str(e), "query": query}


class QdrantKnowledgeTool(BaseTool):
    name = "qdrant_knowledge_search"
    description = "Search knowledge base and ingested documents using RAG."
    description_fa = "جستجو در پایگاه دانش و اسناد با استفاده از RAG."
    risk_level = RiskLevel.LOW

    def __init__(self, qdrant_service=None, ollama_service=None):
        self.qdrant = qdrant_service
        self.ollama = ollama_service

    async def execute(self, input_data: Dict[str, Any], context: Dict[str, Any] = None) -> Dict[str, Any]:
        query = input_data.get("query", "")
        top_k = input_data.get("top_k", 5)

        if not query:
            return {"success": False, "error": "Query required"}

        if not self.qdrant:
            return {
                "success": True,
                "query": query,
                "results": [],
                "message": "Knowledge base not available offline",
                "offline": True
            }

        try:
            from ...config import QDRANT_COLLECTIONS
            
            if self.ollama:
                embedding = await self.ollama.embed(query)
                if embedding and any(embedding):
                    results = self.qdrant.search(
                        query_vector=embedding,
                        top_k=top_k,
                        collection=QDRANT_COLLECTIONS["knowledge"]
                    )
                    return {
                        "success": True,
                        "query": query,
                        "results": results,
                        "count": len(results),
                        "provider": "qdrant_knowledge_vector"
                    }

            results = self.qdrant.search_by_text(
                query, top_k=top_k, 
                collection=QDRANT_COLLECTIONS["knowledge"]
            )
            return {
                "success": True,
                "query": query,
                "results": results,
                "count": len(results),
                "provider": "qdrant_knowledge_text"
            }

        except Exception as e:
            logger.error(f"Knowledge search failed: {e}")
            return {"success": False, "error": str(e)}
