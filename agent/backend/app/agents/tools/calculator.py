"""
Calculator Tool - Safe mathematical calculations
"""
from .registry import BaseTool
from typing import Dict, Any
import math
import re
from ...models.schemas import RiskLevel

class CalculatorTool(BaseTool):
    name = "calculator"
    description = "Perform safe mathematical calculations. Supports +, -, *, /, **, sqrt, sin, cos, log, etc."
    description_fa = "انجام محاسبات ریاضی امن. از عملگرهای +, -, *, /, توان، جذر، سینوس و ... پشتیبانی می‌کند."
    risk_level = RiskLevel.LOW

    SAFE_FUNCTIONS = {
        'sqrt': math.sqrt,
        'sin': math.sin,
        'cos': math.cos,
        'tan': math.tan,
        'log': math.log,
        'log10': math.log10,
        'exp': math.exp,
        'abs': abs,
        'round': round,
        'ceil': math.ceil,
        'floor': math.floor,
        'pow': pow,
        'pi': math.pi,
        'e': math.e,
    }

    async def execute(self, input_data: Dict[str, Any], context: Dict[str, Any] = None) -> Dict[str, Any]:
        expression = input_data.get("expression", "")
        
        if not expression:
            return {"success": False, "error": "Expression is required"}

        # Security: only allow safe characters
        if not re.match(r'^[\d\s\+\-\*\/\(\)\.\,\%\^\!\=\<\>\&\|\~\w]+$', expression):
            return {"success": False, "error": "Invalid characters in expression"}

        # Block dangerous patterns
        dangerous = ['import', 'exec', 'eval', '__', 'open', 'file', 'os', 'sys', 'subprocess']
        if any(d in expression.lower() for d in dangerous):
            return {"success": False, "error": "Potentially dangerous expression blocked"}

        try:
            # Safe eval with limited namespace
            result = eval(expression, {"__builtins__": {}}, self.SAFE_FUNCTIONS)
            
            return {
                "success": True,
                "expression": expression,
                "result": result,
                "result_str": str(result)
            }
        except Exception as e:
            return {
                "success": False,
                "error": f"Calculation error: {str(e)}",
                "expression": expression
            }
