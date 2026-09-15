"""
RAG Engine - Retrieval Augmented Generation
Professional RAG with chunking, embedding, reranking
"""
from typing import List, Dict, Any, Optional
import re
import logging

logger = logging.getLogger(__name__)

class RAGEngine:
    def __init__(self, qdrant_service=None, ollama_service=None):
        self.qdrant = qdrant_service
        self.ollama = ollama_service

    def chunk_text(self, text: str, chunk_size: int = 500, overlap: int = 50) -> List[str]:
        """Smart chunking with sentence boundaries"""
        if len(text) <= chunk_size:
            return [text]

        # Split by sentences first
        sentences = re.split(r'(?<=[.!?؟])\s+', text)
        chunks = []
        current = ""
        
        for sentence in sentences:
            if len(current) + len(sentence) <= chunk_size:
                current += " " + sentence if current else sentence
            else:
                if current:
                    chunks.append(current.strip())
                    # Overlap: keep last part
                    if overlap > 0 and len(current) > overlap:
                        current = current[-overlap:] + " " + sentence
                    else:
                        current = sentence
                else:
                    # Single long sentence - force split
                    chunks.append(sentence[:chunk_size])
                    current = sentence[chunk_size:]

        if current:
            chunks.append(current.strip())

        return [c for c in chunks if len(c.strip()) > 20]

    async def ingest(
        self,
        content: str,
        source: str = "manual",
        metadata: Dict[str, Any] = None,
        chunk_size: int = 500,
        chunk_overlap: int = 50
    ) -> Dict[str, Any]:
        if not self.qdrant or not self.ollama:
            return {
                "success": False,
                "error": "RAG services not available",
                "offline": True
            }

        try:
            chunks = self.chunk_text(content, chunk_size, chunk_overlap)
            logger.info(f"Ingesting {len(chunks)} chunks from source: {source}")

            # Generate embeddings
            embeddings = await self.ollama.embed_batch(chunks)

            # Prepare documents
            documents = []
            for i, chunk in enumerate(chunks):
                documents.append({
                    "content": chunk,
                    "source": source,
                    "metadata": {
                        "chunk_index": i,
                        "total_chunks": len(chunks),
                        "source": source,
                        **(metadata or {})
                    }
                })

            from ..config import QDRANT_COLLECTIONS
            count = self.qdrant.ingest_documents(
                documents=documents,
                embeddings=embeddings,
                collection=QDRANT_COLLECTIONS["knowledge"]
            )

            return {
                "success": True,
                "chunks_created": len(chunks),
                "points_ingested": count,
                "source": source
            }

        except Exception as e:
            logger.error(f"RAG ingest failed: {e}")
            return {"success": False, "error": str(e)}

    async def search(
        self,
        query: str,
        top_k: int = 5,
        filter_source: str = None
    ) -> List[Dict[str, Any]]:
        if not self.qdrant:
            return []

        try:
            from ..config import QDRANT_COLLECTIONS
            
            if self.ollama:
                embedding = await self.ollama.embed(query)
                if embedding and any(embedding):
                    filter_dict = {}
                    if filter_source:
                        filter_dict["source"] = filter_source
                    
                    results = self.qdrant.search(
                        query_vector=embedding,
                        top_k=top_k,
                        collection=QDRANT_COLLECTIONS["knowledge"],
                        filter_dict=filter_dict if filter_dict else None
                    )
                    return self._rerank(query, results)

            # Fallback text search
            results = self.qdrant.search_by_text(
                query, top_k=top_k,
                collection=QDRANT_COLLECTIONS["knowledge"]
            )
            return self._rerank(query, results)

        except Exception as e:
            logger.error(f"RAG search failed: {e}")
            return []

    def _rerank(self, query: str, results: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """Simple reranking based on keyword overlap"""
        query_words = set(query.lower().split())
        
        for result in results:
            content = result.get("content", "").lower()
            content_words = set(content.split())
            overlap = len(query_words & content_words)
            # Boost score by overlap
            result["rerank_score"] = result.get("score", 0) * (1 + overlap * 0.1)
            result["keyword_overlap"] = overlap

        results.sort(key=lambda x: x.get("rerank_score", x.get("score", 0)), reverse=True)
        return results

    async def get_context_for_prompt(self, query: str, top_k: int = 3) -> str:
        results = await self.search(query, top_k=top_k)
        
        if not results:
            return ""

        context_parts = ["=== Knowledge Base Context ==="]
        for i, result in enumerate(results[:top_k], 1):
            source = result.get("payload", {}).get("source", "unknown")
            content = result.get("content", "")[:400]
            score = result.get("score", 0)
            context_parts.append(f"[{i}] (Source: {source}, Score: {score:.2f})\n{content}")

        return "\n\n".join(context_parts)
