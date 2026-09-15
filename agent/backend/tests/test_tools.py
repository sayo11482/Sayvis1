"""
Test all professional tools
"""
import pytest
from unittest.mock import Mock

@pytest.mark.asyncio
async def test_all_tools_basic():
    from app.agents.tools.calculator import CalculatorTool
    from app.agents.tools.datetime_tool import DateTimeTool
    from app.agents.tools.trading_tool import TradingAnalysisTool
    from app.agents.tools.file_tool import FileManagerTool
    from app.agents.tools.web_search import WebSearchTool

    tools = [
        CalculatorTool(),
        DateTimeTool(),
        TradingAnalysisTool(),
        FileManagerTool(base_path="/tmp/sayvis_test"),
        WebSearchTool()
    ]

    for tool in tools:
        assert hasattr(tool, 'name')
        assert hasattr(tool, 'description')
        assert hasattr(tool, 'execute')
        print(f"✅ Tool {tool.name} has correct interface")

@pytest.mark.asyncio
async def test_file_manager_security():
    from app.agents.tools.file_tool import FileManagerTool
    import tempfile
    import os

    with tempfile.TemporaryDirectory() as tmpdir:
        tool = FileManagerTool(base_path=tmpdir)
        
        # Test path traversal blocked
        result = await tool.execute({
            "operation": "read",
            "path": "../../../etc/passwd"
        })
        assert result["success"] == False
        assert "traversal" in result["error"].lower()

        # Test write and read
        write_result = await tool.execute({
            "operation": "write",
            "path": "test.txt",
            "content": "Hello SAYVIS"
        })
        assert write_result["success"] == True

        read_result = await tool.execute({
            "operation": "read",
            "path": "test.txt"
        })
        assert read_result["success"] == True
        assert "Hello SAYVIS" in read_result["content"]

        # Test list
        list_result = await tool.execute({
            "operation": "list",
            "path": ""
        })
        assert list_result["success"] == True
        assert "test.txt" in list_result["files"]

@pytest.mark.asyncio
async def test_web_search_offline():
    from app.agents.tools.web_search import WebSearchTool
    
    tool = WebSearchTool()  # No API keys - should use offline mock
    
    result = await tool.execute({
        "query": "SAYVIS AI agent",
        "max_results": 2
    })
    
    assert result["success"] == True
    assert "query" in result
    assert len(result["results"]) >= 1

def test_tool_schemas():
    from app.agents.tools.registry import ToolRegistry
    from app.agents.tools.calculator import CalculatorTool
    from app.agents.tools.trading_tool import TradingAnalysisTool
    from app.agents.tools.n8n_tool import N8nWorkflowTool

    calc = CalculatorTool()
    schema = calc.get_schema()
    
    assert schema["name"] == "calculator"
    assert schema["risk_level"] == "LOW"
    assert "description" in schema

    trading = TradingAnalysisTool()
    schema = trading.get_schema()
    assert schema["risk_level"] == "MEDIUM"

    n8n_tool = N8nWorkflowTool()
    schema = n8n_tool.get_schema()
    assert schema["risk_level"] == "HIGH"
    assert schema["requires_confirmation"] == True
