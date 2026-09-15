"""
n8n Service - Workflow Automation Integration
Professional n8n API integration for triggering workflows as tools
"""
import httpx
from typing import Dict, Any, List, Optional
import logging

logger = logging.getLogger(__name__)

class N8nService:
    def __init__(self, base_url: str = None, api_key: str = None):
        from ..config import settings
        self.base_url = base_url or settings.n8n_api_url
        self.api_key = api_key or settings.n8n_api_key
        self.client = httpx.AsyncClient(timeout=60.0)

    async def health_check(self) -> bool:
        try:
            resp = await self.client.get(f"{self.base_url}/healthz")
            return resp.status_code == 200
        except Exception as e:
            logger.warning(f"n8n health check failed: {e}")
            return False

    def _headers(self) -> Dict[str, str]:
        headers = {"Content-Type": "application/json"}
        if self.api_key:
            headers["X-N8N-API-KEY"] = self.api_key
        return headers

    async def list_workflows(self) -> List[Dict[str, Any]]:
        try:
            resp = await self.client.get(
                f"{self.base_url}/api/v1/workflows",
                headers=self._headers()
            )
            if resp.status_code == 200:
                data = resp.json()
                return data.get("data", [])
            return []
        except Exception as e:
            logger.error(f"Failed to list n8n workflows: {e}")
            return []

    async def get_workflow(self, workflow_id: str) -> Optional[Dict[str, Any]]:
        try:
            resp = await self.client.get(
                f"{self.base_url}/api/v1/workflows/{workflow_id}",
                headers=self._headers()
            )
            if resp.status_code == 200:
                return resp.json()
            return None
        except Exception as e:
            logger.error(f"Failed to get workflow {workflow_id}: {e}")
            return None

    async def trigger_webhook(
        self,
        webhook_path: str,
        payload: Dict[str, Any],
        method: str = "POST"
    ) -> Dict[str, Any]:
        """
        Trigger n8n workflow via webhook
        webhook_path: e.g., 'sayvis-agent' or 'webhook/abc123'
        """
        url = f"{self.base_url}/webhook/{webhook_path.lstrip('/')}"
        
        try:
            if method.upper() == "POST":
                resp = await self.client.post(url, json=payload)
            else:
                resp = await self.client.get(url, params=payload)
            
            return {
                "status_code": resp.status_code,
                "success": 200 <= resp.status_code < 300,
                "data": resp.json() if resp.headers.get("content-type", "").startswith("application/json") else resp.text,
                "url": url
            }
        except Exception as e:
            logger.error(f"Webhook trigger failed {url}: {e}")
            return {
                "success": False,
                "error": str(e),
                "url": url
            }

    async def execute_workflow(
        self,
        workflow_id: str,
        payload: Dict[str, Any] = None
    ) -> Dict[str, Any]:
        """
        Execute n8n workflow via API (requires n8n API key)
        """
        try:
            # n8n API v1 execute endpoint
            resp = await self.client.post(
                f"{self.base_url}/api/v1/workflows/{workflow_id}/execute",
                headers=self._headers(),
                json=payload or {}
            )
            
            return {
                "status_code": resp.status_code,
                "success": 200 <= resp.status_code < 300,
                "data": resp.json() if resp.status_code == 200 else resp.text
            }
        except Exception as e:
            logger.error(f"Workflow execution failed {workflow_id}: {e}")
            return {
                "success": False,
                "error": str(e)
            }

    async def trigger_sayvis_workflows(self, event_type: str, data: Dict[str, Any]) -> List[Dict[str, Any]]:
        """
        Trigger all SAYVIS-related workflows based on event type
        """
        workflows_map = {
            "new_message": ["sayvis-new-message", "sayvis-agent"],
            "new_mission": ["sayvis-mission-created"],
            "market_alert": ["sayvis-trading-alert"],
            "memory_ingest": ["sayvis-rag-ingestion"],
            "daily_summary": ["sayvis-daily-summary"]
        }
        
        webhook_paths = workflows_map.get(event_type, [])
        results = []
        
        for path in webhook_paths:
            result = await self.trigger_webhook(path, {
                "event": event_type,
                "timestamp": __import__("time").time(),
                **data
            })
            results.append({"workflow": path, **result})
        
        return results

    async def close(self):
        await self.client.aclose()

# Predefined SAYVIS n8n workflows
SAYVIS_N8N_WORKFLOWS = {
    "main_agent": {
        "name": "SAYVIS Main Agent Orchestrator",
        "webhook": "sayvis-agent",
        "description": "Main entry for SAYVIS agent requests"
    },
    "rag_ingestion": {
        "name": "SAYVIS RAG Auto-Ingestion",
        "webhook": "sayvis-rag-ingestion",
        "description": "Auto-ingest documents to Qdrant"
    },
    "trading_analysis": {
        "name": "SAYVIS Trading Analysis",
        "webhook": "sayvis-trading-alert",
        "description": "Market analysis and alerts"
    },
    "mission_automation": {
        "name": "SAYVIS Mission Automation",
        "webhook": "sayvis-mission-created",
        "description": "Auto-breakdown missions into tasks"
    }
}
