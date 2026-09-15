"""
Qdrant Service - Vector DB for Memory & RAG
Professional integration with collection management, hybrid search
"""
from qdrant_client import QdrantClient
from qdrant_client.models import (
    Distance, VectorParams, PointStruct, 
    Filter, FieldCondition, MatchValue,
    SearchParams
)
from typing import List, Dict, Any, Optional
import uuid
import logging
import time
from ..config import settings, QDRANT_COLLECTIONS

logger = logging.getLogger(__name__)

class QdrantService:
    def __init__(self, url: str = None, api_key: str = None):
        self.url = url or settings.qdrant_url
        self.api_key = api_key or settings.qdrant_api_key
        self.client = QdrantClient(url=self.url, api_key=self.api_key, timeout=30)
        self.collections = QDRANT_COLLECTIONS
        self.vector_size = 768  # nomic-embed-text default

    async def health_check(self) -> bool:
        try:
            # Sync call in async context - ok for health check
            collections = self.client.get_collections()
            return True
        except Exception as e:
            logger.warning(f"Qdrant health check failed: {e}")
            return False

    def ensure_collections(self):
        """Ensure all required collections exist"""
        existing = [c.name for c in self.client.get_collections().collections]
        
        for key, name in self.collections.items():
            if name not in existing:
                logger.info(f"Creating Qdrant collection: {name}")
                self.client.create_collection(
                    collection_name=name,
                    vectors_config=VectorParams(
                        size=self.vector_size,
                        distance=Distance.COSINE
                    )
                )
                # Create payload indexes
                try:
                    self.client.create_payload_index(
                        collection_name=name,
                        field_name="category",
                        field_schema="keyword"
                    )
                    self.client.create_payload_index(
                        collection_name=name,
                        field_name="session_id",
                        field_schema="keyword"
                    )
                except Exception as e:
                    logger.warning(f"Failed to create index for {name}: {e}")

    def upsert_memory(
        self,
        content: str,
        embedding: List[float],
        category: str = "general",
        metadata: Dict[str, Any] = None,
        collection: str = None
    ) -> str:
        collection_name = collection or self.collections["memory"]
        point_id = str(uuid.uuid4())
        
        payload = {
            "content": content,
            "category": category,
            "timestamp": int(time.time() * 1000),
            "importance": metadata.get("importance", 0.5) if metadata else 0.5,
            **(metadata or {})
        }

        point = PointStruct(
            id=point_id,
            vector=embedding,
            payload=payload
        )

        self.client.upsert(
            collection_name=collection_name,
            points=[point]
        )
        return point_id

    def search(
        self,
        query_vector: List[float],
        top_k: int = 5,
        collection: str = None,
        category: str = None,
        filter_dict: Dict[str, Any] = None
    ) -> List[Dict[str, Any]]:
        collection_name = collection or self.collections["memory"]
        
        query_filter = None
        conditions = []
        
        if category:
            conditions.append(FieldCondition(key="category", match=MatchValue(value=category)))
        
        if filter_dict:
            for k, v in filter_dict.items():
                conditions.append(FieldCondition(key=k, match=MatchValue(value=v)))
        
        if conditions:
            query_filter = Filter(must=conditions)

        try:
            results = self.client.search(
                collection_name=collection_name,
                query_vector=query_vector,
                query_filter=query_filter,
                limit=top_k,
                search_params=SearchParams(hnsw_ef=128, exact=False)
            )
            
            return [
                {
                    "id": str(r.id),
                    "content": r.payload.get("content", ""),
                    "score": r.score,
                    "category": r.payload.get("category", "general"),
                    "metadata": {k: v for k, v in r.payload.items() if k not in ["content", "category"]},
                    "payload": r.payload
                }
                for r in results
            ]
        except Exception as e:
            logger.error(f"Qdrant search failed: {e}")
            return []

    def search_by_text(self, query: str, top_k: int = 5, collection: str = None) -> List[Dict[str, Any]]:
        """Fallback text search when embeddings unavailable"""
        collection_name = collection or self.collections["memory"]
        try:
            # Scroll and filter by text containment
            results, _ = self.client.scroll(
                collection_name=collection_name,
                scroll_filter=Filter(
                    must=[]
                ),
                limit=100,
                with_payload=True,
                with_vectors=False
            )
            
            # Simple keyword matching
            query_lower = query.lower()
            scored = []
            for point in results:
                content = point.payload.get("content", "").lower()
                if query_lower in content:
                    score = content.count(query_lower) / max(len(content.split()), 1)
                    scored.append({
                        "id": str(point.id),
                        "content": point.payload.get("content", ""),
                        "score": score,
                        "category": point.payload.get("category", "general"),
                        "metadata": point.payload,
                        "payload": point.payload
                    })
            
            scored.sort(key=lambda x: x["score"], reverse=True)
            return scored[:top_k]
        except Exception as e:
            logger.error(f"Text search failed: {e}")
            return []

    def delete(self, point_id: str, collection: str = None):
        collection_name = collection or self.collections["memory"]
        self.client.delete(
            collection_name=collection_name,
            points_selector=[point_id]
        )

    def count(self, collection: str = None) -> int:
        collection_name = collection or self.collections["memory"]
        try:
            result = self.client.count(collection_name=collection_name)
            return result.count
        except:
            return 0

    def ingest_documents(
        self,
        documents: List[Dict[str, Any]],
        embeddings: List[List[float]],
        collection: str = None,
        batch_size: int = 100
    ) -> int:
        collection_name = collection or self.collections["knowledge"]
        count = 0
        
        for i in range(0, len(documents), batch_size):
            batch_docs = documents[i:i+batch_size]
            batch_embs = embeddings[i:i+batch_size]
            
            points = []
            for doc, emb in zip(batch_docs, batch_embs):
                points.append(PointStruct(
                    id=str(uuid.uuid4()),
                    vector=emb,
                    payload={
                        "content": doc.get("content", ""),
                        "source": doc.get("source", "unknown"),
                        "timestamp": int(time.time() * 1000),
                        **doc.get("metadata", {})
                    }
                ))
            
            self.client.upsert(
                collection_name=collection_name,
                points=points
            )
            count += len(points)
        
        return count
