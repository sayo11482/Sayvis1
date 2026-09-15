"""
Tool Registry - Professional Tool Management
"""
from abc import ABC, abstractmethod
from typing import Dict, Any, List, Optional
import logging
from ...models.schemas import RiskLevel

logger = logging.getLogger(__name__)

class BaseTool(ABC):
    name: str = "base_tool"
    description: str = "Base tool"
    description_fa: str = "ابزار پایه"
    risk_level: RiskLevel = RiskLevel.LOW
    requires_confirmation: bool = False

    @abstractmethod
    async def execute(self, input_data: Dict[str, Any], context: Dict[str, Any] = None) -> Dict[str, Any]:
        pass

    def get_schema(self) -> Dict[str, Any]:
        return {
            "name": self.name,
            "description": self.description,
            "description_fa": self.description_fa,
            "risk_level": self.risk_level.value,
            "requires_confirmation": self.requires_confirmation
        }

class ToolRegistry:
    def __init__(self):
        self.tools: Dict[str, BaseTool] = {}
        self.execution_history: List[Dict[str, Any]] = []

    def register(self, tool: BaseTool):
        self.tools[tool.name] = tool
        logger.info(f"Registered tool: {tool.name} [{tool.risk_level}]")

    def get(self, name: str) -> Optional[BaseTool]:
        return self.tools.get(name)

    def list_tools(self) -> List[Dict[str, Any]]:
        return [tool.get_schema() for tool in self.tools.values()]

    def get_tools_for_prompt(self, language_fa: bool = False) -> str:
        """Generate tools description for LLM prompt"""
        lines = []
        for tool in self.tools.values():
            desc = tool.description_fa if language_fa else tool.description
            lines.append(f"- {tool.name}: {desc} [Risk: {tool.risk_level.value}]")
        return "\n".join(lines)

    async def execute(
        self,
        tool_name: str,
        input_data: Dict[str, Any],
        context: Dict[str, Any] = None
    ) -> Dict[str, Any]:
        tool = self.get(tool_name)
        if not tool:
            return {
                "success": False,
                "error": f"Tool '{tool_name}' not found",
                "tool": tool_name
            }

        try:
            import time
            start = time.time()
            result = await tool.execute(input_data, context)
            elapsed = int((time.time() - start) * 1000)
            
            execution_record = {
                "tool": tool_name,
                "input": input_data,
                "output": str(result)[:500],
                "risk_level": tool.risk_level.value,
                "execution_time_ms": elapsed,
                "success": result.get("success", True),
                "timestamp": __import__("datetime").datetime.utcnow().isoformat()
            }
            self.execution_history.append(execution_record)
            
            return {
                **result,
                "tool": tool_name,
                "risk_level": tool.risk_level.value,
                "execution_time_ms": elapsed
            }
        except Exception as e:
            logger.error(f"Tool {tool_name} execution failed: {e}")
            return {
                "success": False,
                "error": str(e),
                "tool": tool_name,
                "risk_level": tool.risk_level.value
            }

    def get_history(self, limit: int = 50) -> List[Dict[str, Any]]:
        return self.execution_history[-limit:]
