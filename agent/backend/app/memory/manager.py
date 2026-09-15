"""
Memory Manager - Hierarchical Memory System
- Short-term: In-memory (session)
- Long-term: Qdrant vector DB
- Episodic: Postgres
"""
from typing import List, Dict, Any, Optional
import logging
from datetime import datetime

logger = logging.getLogger(__name__)

class MemoryManager:
    def __init__(self, qdrant_service=None, ollama_service=None, db_session=None):
        self.qdrant = qdrant_service
        self.ollama = ollama_service
        self.db = db_session
        self.short_term: Dict[str, List[Dict[str, Any]]] = {}  # session_id -> messages
        self.max_short_term = 20

    async def add_to_short_term(self, session_id: str, role: str, content: str, metadata: Dict[str, Any] = None):
        if session_id not in self.short_term:
            self.short_term[session_id] = []
        
        self.short_term[session_id].append({
            "role": role,
            "content": content,
            "timestamp": datetime.utcnow().isoformat(),
            "metadata": metadata or {}
        })
        
        # Keep only recent
        if len(self.short_term[session_id]) > self.max_short_term:
            self.short_term[session_id] = self.short_term[session_id][-self.max_short_term:]

    def get_short_term(self, session_id: str, limit: int = 10) -> List[Dict[str, Any]]:
        return self.short_term.get(session_id, [])[-limit:]

    async def add_to_long_term(
        self,
        content: str,
        category: str = "conversation",
        importance: float = 0.5,
        metadata: Dict[str, Any] = None,
        session_id: str = None
    ) -> Optional[str]:
        if not self.qdrant or not self.ollama:
            logger.warning("Long-term memory not available (qdrant/ollama missing)")
            return None

        try:
            embedding = await self.ollama.embed(content)
            if not embedding or not any(embedding):
                return None

            from ..config import QDRANT_COLLECTIONS
            collection = QDRANT_COLLECTIONS["memory"]
            
            payload = {
                "importance": importance,
                "session_id": session_id or "default",
                **(metadata or {})
            }

            point_id = self.qdrant.upsert_memory(
                content=content,
                embedding=embedding,
                category=category,
                metadata=payload,
                collection=collection
            )
            logger.info(f"Added to long-term memory: {point_id}")
            return point_id
        except Exception as e:
            logger.error(f"Failed to add to long-term memory: {e}")
            return None

    async def search_long_term(
        self,
        query: str,
        top_k: int = 5,
        category: str = None,
        session_id: str = None
    ) -> List[Dict[str, Any]]:
        if not self.qdrant:
            return []

        try:
            if self.ollama:
                embedding = await self.ollama.embed(query)
                if embedding and any(embedding):
                    filter_dict = {}
                    if session_id:
                        filter_dict["session_id"] = session_id
                    
                    results = self.qdrant.search(
                        query_vector=embedding,
                        top_k=top_k,
                        category=category,
                        filter_dict=filter_dict if filter_dict else None
                    )
                    return results

            # Fallback text search
            return self.qdrant.search_by_text(query, top_k=top_k)
        except Exception as e:
            logger.error(f"Long-term search failed: {e}")
            return []

    async def get_context_for_query(
        self,
        session_id: str,
        query: str,
        include_short: bool = True,
        include_long: bool = True,
        long_term_k: int = 3
    ) -> Dict[str, Any]:
        context = {
            "short_term": [],
            "long_term": [],
            "combined_text": ""
        }

        if include_short:
            short = self.get_short_term(session_id, limit=6)
            context["short_term"] = short

        if include_long:
            long_results = await self.search_long_term(query, top_k=long_term_k, session_id=session_id)
            context["long_term"] = long_results

        # Build combined text for prompt
        parts = []
        if context["long_term"]:
            parts.append("=== Relevant Memories ===")
            for mem in context["long_term"][:3]:
                parts.append(f"- {mem.get('content', '')[:200]} (score: {mem.get('score', 0):.2f})")
        
        if context["short_term"]:
            parts.append("\n=== Recent Conversation ===")
            for msg in context["short_term"][-4:]:
                parts.append(f"{msg['role']}: {msg['content'][:150]}")

        context["combined_text"] = "\n".join(parts)
        return context

    def clear_session(self, session_id: str):
        if session_id in self.short_term:
            del self.short_term[session_id]
