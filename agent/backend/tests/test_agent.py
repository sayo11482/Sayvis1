"""
Professional tests for SAYVIS AI Agent
"""
import pytest
import asyncio
from unittest.mock import Mock, AsyncMock

# Test core agent logic without external dependencies

def test_language_detection():
    from app.orchestrator.orchestrator import AgentOrchestrator
    
    orchestrator = AgentOrchestrator()
    
    # Persian detection
    assert orchestrator._detect_language("سلام خوبی؟") == True
    assert orchestrator._detect_language("این یک تست فارسی است") == True
    
    # English detection
    assert orchestrator._detect_language("Hello how are you?") == False
    assert orchestrator._detect_language("This is English") == False

def test_system_prompt_building():
    from app.orchestrator.orchestrator import AgentOrchestrator
    
    orchestrator = AgentOrchestrator()
    
    # Persian prompt
    fa_prompt = orchestrator._build_system_prompt(
        language_fa=True,
        context_text="Test context",
        rag_context="Test RAG"
    )
    assert "سایویس" in fa_prompt
    assert "Test context" in fa_prompt
    
    # English prompt
    en_prompt = orchestrator._build_system_prompt(
        language_fa=False,
        context_text="Test context"
    )
    assert "SAYVIS" in en_prompt

def test_tool_parsing():
    from app.orchestrator.orchestrator import AgentOrchestrator
    
    orchestrator = AgentOrchestrator()
    
    # Test JSON tool call parsing
    text_with_tool = '''
    I need to calculate something.
    TOOL: calculator {"expression": "2+2"}
    '''
    
    calls = orchestrator._parse_tool_calls(text_with_tool)
    assert len(calls) == 1
    assert calls[0]["tool"] == "calculator"
    assert calls[0]["input"]["expression"] == "2+2"

@pytest.mark.asyncio
async def test_calculator_tool():
    from app.agents.tools.calculator import CalculatorTool
    
    tool = CalculatorTool()
    
    # Valid calculation
    result = await tool.execute({"expression": "2+2"})
    assert result["success"] == True
    assert result["result"] == 4
    
    # Complex calculation
    result = await tool.execute({"expression": "sqrt(16) + pow(2, 3)"})
    assert result["success"] == True
    assert result["result"] == 12.0
    
    # Dangerous should be blocked
    result = await tool.execute({"expression": "__import__('os')"})
    assert result["success"] == False

@pytest.mark.asyncio
async def test_datetime_tool():
    from app.agents.tools.datetime_tool import DateTimeTool
    
    tool = DateTimeTool()
    
    result = await tool.execute({"operation": "now"})
    assert result["success"] == True
    assert "gregorian" in result
    assert "jalali" in result
    assert "weekday_fa" in result

@pytest.mark.asyncio
async def test_trading_tool():
    from app.agents.tools.trading_tool import TradingAnalysisTool
    
    tool = TradingAnalysisTool()
    
    result = await tool.execute({"symbol": "EURUSD", "timeframe": "H1"})
    assert result["success"] == True
    assert result["symbol"] == "EURUSD"
    assert "signal" in result
    assert result["signal"] in ["BUY", "SELL", "HOLD"]
    assert result["is_paper"] == True

def test_zero_trust_classification():
    from app.security.zero_trust import ZeroTrustEngine, RiskLevel
    
    engine = ZeroTrustEngine()
    
    # Low risk tools
    assert engine.classify_tool("calculator") == RiskLevel.LOW
    assert engine.classify_tool("datetime_tool") == RiskLevel.LOW
    
    # Medium risk
    assert engine.classify_tool("web_search") == RiskLevel.MEDIUM
    assert engine.classify_tool("trading_analysis") == RiskLevel.MEDIUM
    
    # High risk
    assert engine.classify_tool("n8n_workflow") == RiskLevel.HIGH
    assert engine.classify_tool("file_manager_write") == RiskLevel.HIGH
    
    # Critical
    assert engine.classify_tool("trading_order") == RiskLevel.CRITICAL

def test_zero_trust_permissions():
    from app.security.zero_trust import ZeroTrustEngine, RiskLevel
    
    engine = ZeroTrustEngine(emergency_lock=False)
    
    # Low risk should be allowed without confirmation
    decision = engine.check_permission("tool.calculator", RiskLevel.LOW)
    assert decision.allowed == True
    assert decision.requires_confirmation == False
    
    # High risk requires confirmation
    decision = engine.check_permission("tool.n8n_workflow", RiskLevel.HIGH)
    assert decision.allowed == True
    assert decision.requires_confirmation == True
    
    # Critical blocked
    decision = engine.check_permission("tool.trading_order", RiskLevel.CRITICAL)
    assert decision.allowed == False
    assert decision.requires_confirmation == True

def test_zero_trust_emergency_lock():
    from app.security.zero_trust import ZeroTrustEngine, RiskLevel
    
    engine = ZeroTrustEngine(emergency_lock=True)
    
    # Low should still pass
    decision = engine.check_permission("tool.calculator", RiskLevel.LOW)
    assert decision.allowed == True
    
    # High should be blocked when emergency lock is on
    decision = engine.check_permission("tool.n8n_workflow", RiskLevel.HIGH)
    assert decision.allowed == False
    
    decision = engine.check_permission("tool.trading_order", RiskLevel.CRITICAL)
    assert decision.allowed == False

def test_audit_chain():
    from app.security.zero_trust import ZeroTrustEngine, RiskLevel
    
    engine = ZeroTrustEngine()
    
    # Make some decisions
    engine.check_permission("tool.calculator", RiskLevel.LOW, actor="AGENT")
    engine.check_permission("tool.web_search", RiskLevel.MEDIUM, actor="AGENT")
    
    trail = engine.get_audit_trail()
    assert len(trail) == 2
    assert trail[0]["action"] == "tool.calculator"
    assert trail[1]["action"] == "tool.web_search"
    
    # Verify chain integrity
    assert engine.verify_chain() == True

def test_qdrant_chunking():
    from app.rag.engine import RAGEngine
    
    engine = RAGEngine()
    
    # Short text
    short = "This is short"
    chunks = engine.chunk_text(short, chunk_size=500)
    assert len(chunks) == 1
    
    # Long text
    long_text = "This is a sentence. " * 100  # ~2000 chars
    chunks = engine.chunk_text(long_text, chunk_size=500, overlap=50)
    assert len(chunks) > 1
    assert all(len(c) <= 600 for c in chunks)  # Allow some overflow for sentence boundaries

@pytest.mark.asyncio
async def test_memory_manager():
    from app.memory.manager import MemoryManager
    
    manager = MemoryManager()
    
    # Short term
    await manager.add_to_short_term("test-session", "user", "Hello")
    await manager.add_to_short_term("test-session", "assistant", "Hi there")
    
    short = manager.get_short_term("test-session")
    assert len(short) == 2
    assert short[0]["content"] == "Hello"
    
    # Clear
    manager.clear_session("test-session")
    short = manager.get_short_term("test-session")
    assert len(short) == 0

def test_tool_registry():
    from app.agents.tools.registry import ToolRegistry
    from app.agents.tools.calculator import CalculatorTool
    from app.agents.tools.datetime_tool import DateTimeTool
    
    registry = ToolRegistry()
    registry.register(CalculatorTool())
    registry.register(DateTimeTool())
    
    assert len(registry.tools) == 2
    assert registry.get("calculator") is not None
    assert registry.get("nonexistent") is None
    
    tools_list = registry.list_tools()
    assert len(tools_list) == 2
    assert tools_list[0]["name"] in ["calculator", "datetime_tool"]

@pytest.mark.asyncio
async def test_offline_responses():
    from app.orchestrator.orchestrator import AgentOrchestrator
    
    orchestrator = AgentOrchestrator()
    
    # Persian offline
    fa_resp = orchestrator._offline_response("سلام", is_persian=True)
    assert "سایویس" in fa_resp
    
    # English offline
    en_resp = orchestrator._offline_response("hello", is_persian=False)
    assert "SAYVIS" in en_resp

if __name__ == "__main__":
    pytest.main([__file__, "-v"])
