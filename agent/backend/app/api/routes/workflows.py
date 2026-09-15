from fastapi import APIRouter, HTTPException
from ...models.schemas import WorkflowTriggerRequest, WorkflowResponse
from typing import List, Dict, Any
import logging

router = APIRouter()
logger = logging.getLogger(__name__)

@router.get("/workflows", tags=["Workflows"])
async def list_workflows():
    from ...main import get_services
    
    services = get_services()
    n8n = services.get("n8n")
    
    if not n8n:
        return {"workflows": [], "count": 0, "offline": True}
    
    try:
        workflows = await n8n.list_workflows()
        return {"workflows": workflows, "count": len(workflows)}
    except Exception as e:
        logger.error(f"Failed to list workflows: {e}")
        return {"workflows": [], "count": 0, "error": str(e)}

@router.post("/workflows/trigger", response_model=WorkflowResponse, tags=["Workflows"])
async def trigger_workflow(request: WorkflowTriggerRequest):
    from ...main import get_services
    
    services = get_services()
    n8n = services.get("n8n")
    
    if not n8n:
        return WorkflowResponse(
            workflow_id=request.workflow_id,
            status="queued_offline",
            result={"message": "n8n not available, queued for later", "payload": request.payload}
        )
    
    try:
        result = await n8n.execute_workflow(request.workflow_id, request.payload)
        
        return WorkflowResponse(
            workflow_id=request.workflow_id,
            execution_id=result.get("data", {}).get("executionId") if isinstance(result.get("data"), dict) else None,
            status="success" if result.get("success") else "failed",
            result=result
        )
    except Exception as e:
        logger.error(f"Workflow trigger failed: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@router.post("/workflows/webhook/{webhook_path}", tags=["Workflows"])
async def trigger_webhook(webhook_path: str, payload: Dict[str, Any] = None):
    from ...main import get_services
    
    services = get_services()
    n8n = services.get("n8n")
    
    if not n8n:
        return {"success": False, "error": "n8n not available", "offline": True}
    
    try:
        result = await n8n.trigger_webhook(webhook_path, payload or {})
        return result
    except Exception as e:
        logger.error(f"Webhook trigger failed: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@router.get("/workflows/sayvis", tags=["Workflows"])
async def list_sayvis_workflows():
    from ...services.n8n_service import SAYVIS_N8N_WORKFLOWS
    
    return {
        "workflows": SAYVIS_N8N_WORKFLOWS,
        "count": len(SAYVIS_N8N_WORKFLOWS),
        "description": "Predefined SAYVIS workflows for n8n"
    }
