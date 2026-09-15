"""
Web Search Tool - Professional web search with multiple providers
"""
from .registry import BaseTool
from typing import Dict, Any
import httpx
from ...models.schemas import RiskLevel
import logging

logger = logging.getLogger(__name__)

class WebSearchTool(BaseTool):
    name = "web_search"
    description = "Search the web for current information, news, and facts. Use for real-time data."
    description_fa = "جستجوی وب برای اطلاعات به‌روز، اخبار و حقایق. برای داده‌های لحظه‌ای استفاده کنید."
    risk_level = RiskLevel.MEDIUM

    def __init__(self, tavily_api_key: str = None, serpapi_key: str = None):
        from ...config import settings
        self.tavily_key = tavily_api_key or settings.tavily_api_key
        self.serpapi_key = serpapi_key or settings.serpapi_api_key
        self.client = httpx.AsyncClient(timeout=20.0)

    async def execute(self, input_data: Dict[str, Any], context: Dict[str, Any] = None) -> Dict[str, Any]:
        query = input_data.get("query", "")
        max_results = input_data.get("max_results", 5)
        
        if not query:
            return {"success": False, "error": "Query is required"}

        # Try Tavily first (best for AI)
        if self.tavily_key:
            try:
                return await self._search_tavily(query, max_results)
            except Exception as e:
                logger.warning(f"Tavily search failed: {e}")

        # Fallback to DuckDuckGo scraping (no API key needed)
        try:
            return await self._search_duckduckgo(query, max_results)
        except Exception as e:
            logger.warning(f"DuckDuckGo search failed: {e}")

        # Final fallback - mock response for offline mode
        return {
            "success": True,
            "query": query,
            "results": [
                {
                    "title": f"Offline search result for: {query}",
                    "url": "https://example.com/offline",
                    "snippet": f"This is a simulated search result for '{query}' in offline mode. Connect Tavily API for real search.",
                    "score": 0.5
                }
            ],
            "provider": "offline_mock",
            "offline": True
        }

    async def _search_tavily(self, query: str, max_results: int) -> Dict[str, Any]:
        resp = await self.client.post(
            "https://api.tavily.com/search",
            json={
                "api_key": self.tavily_key,
                "query": query,
                "max_results": max_results,
                "search_depth": "advanced",
                "include_answer": True
            }
        )
        resp.raise_for_status()
        data = resp.json()
        
        results = []
        for item in data.get("results", [])[:max_results]:
            results.append({
                "title": item.get("title", ""),
                "url": item.get("url", ""),
                "snippet": item.get("content", "")[:300],
                "score": item.get("score", 0.5)
            })
        
        return {
            "success": True,
            "query": query,
            "results": results,
            "answer": data.get("answer", ""),
            "provider": "tavily"
        }

    async def _search_duckduckgo(self, query: str, max_results: int) -> Dict[str, Any]:
        # Simple DuckDuckGo HTML parsing fallback
        resp = await self.client.get(
            "https://api.duckduckgo.com/",
            params={"q": query, "format": "json", "pretty": 1}
        )
        
        if resp.status_code == 200:
            data = resp.json()
            results = []
            
            # Abstract
            if data.get("AbstractText"):
                results.append({
                    "title": data.get("Heading", query),
                    "url": data.get("AbstractURL", ""),
                    "snippet": data.get("AbstractText", "")[:300],
                    "score": 0.9
                })
            
            # Related topics
            for topic in data.get("RelatedTopics", [])[:max_results-1]:
                if isinstance(topic, dict) and "Text" in topic:
                    results.append({
                        "title": topic.get("Text", "")[:80],
                        "url": topic.get("FirstURL", ""),
                        "snippet": topic.get("Text", "")[:300],
                        "score": 0.6
                    })
            
            if results:
                return {
                    "success": True,
                    "query": query,
                    "results": results[:max_results],
                    "provider": "duckduckgo"
                }
        
        raise Exception("No results from DuckDuckGo")
