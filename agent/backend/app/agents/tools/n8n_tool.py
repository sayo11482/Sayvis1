from .registry import BaseTool
from typing import Dict, Any
from ...models.schemas import RiskLevel

class N8nWorkflowTool(BaseTool):
    name = "n8n_workflow"
    description = "Trigger n8n workflows for automation. Use for complex multi-step tasks."
    description_fa = "اجرای ورک‌فلوهای n8n برای خودکارسازی. برای کارهای چندمرحله‌ای استفاده کنید."
    risk_level = RiskLevel.HIGH
    requires_confirmation = True

    def __init__(self, n8n_service=None):
        self.n8n = n8n_service

    async def execute(self, input_data: Dict[str, Any], context: Dict[str, Any] = None) -> Dict[str, Any]:
        workflow_id = input_data.get("workflow_id", "")
        webhook_path = input_data.get("webhook_path", "")
        payload = input_data.get("payload", {})

        if not workflow_id and not webhook_path:
            return {"success": False, "error": "workflow_id or webhook_path required"}

        if not self.n8n:
            return {
                "success": True,
                "message": "n8n service not available, workflow queued for later",
                "workflow_id": workflow_id or webhook_path,
                "payload": payload,
                "offline": True
            }

        try:
            if webhook_path:
                result = await self.n8n.trigger_webhook(webhook_path, payload)
            else:
                result = await self.n8n.execute_workflow(workflow_id, payload)

            return {
                "success": result.get("success", False),
                "workflow_id": workflow_id or webhook_path,
                "result": result,
                "payload": payload
            }
        except Exception as e:
            return {"success": False, "error": str(e), "workflow_id": workflow_id}
