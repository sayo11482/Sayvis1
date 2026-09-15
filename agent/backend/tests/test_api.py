"""
Test FastAPI endpoints
"""
import pytest
from fastapi.testclient import TestClient
import sys
import os

# Add app to path
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

# Mock services to avoid needing real DBs
import unittest.mock as mock

@pytest.fixture
def mock_services():
    with mock.patch('app.main.get_services') as mock_get:
        # Mock orchestrator
        mock_orchestrator = mock.AsyncMock()
        mock_orchestrator.process_message = mock.AsyncMock(return_value={
            "response": "Test response from SAYVIS",
            "session_id": "test-session",
            "provider_used": "test",
            "model": "test-model",
            "tools_used": [],
            "memory_hits": [],
            "rag_sources": [],
            "processing_time_ms": 100,
            "language": "en",
            "is_persian": False
        })
        
        mock_memory = mock.Mock()
        mock_memory.get_short_term = mock.Mock(return_value=[])
        mock_memory.clear_session = mock.Mock()
        mock_memory.search_long_term = mock.AsyncMock(return_value=[])
        mock_memory.short_term = {}
        
        mock_get.return_value = {
            "orchestrator": mock_orchestrator,
            "memory": mock_memory,
            "qdrant": None,
            "ollama": None,
            "n8n": None,
            "rag": None,
            "tools": None,
            "zero_trust": None
        }
        yield mock_get

def test_root_endpoint():
    from app.main import app
    client = TestClient(app)
    
    response = client.get("/")
    assert response.status_code == 200
    data = response.json()
    assert "name" in data
    assert "ODIN" in data["name"] or "SAYVIS" in data["name"]

def test_health_endpoint():
    from app.main import app
    client = TestClient(app)
    
    response = client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert "status" in data
    assert "services" in data

def test_openai_compat_endpoint_structure():
    from app.main import app
    client = TestClient(app)
    
    # Test with mock
    with mock.patch('app.main.get_services') as mock_get:
        mock_orch = mock.AsyncMock()
        mock_orch.process_message = mock.AsyncMock(return_value={
            "response": "Hello from SAYVIS",
            "session_id": "test",
            "provider_used": "local",
            "model": "test",
            "tools_used": [],
            "memory_hits": [],
            "rag_sources": [],
            "processing_time_ms": 50,
            "language": "en",
            "is_persian": False
        })
        mock_get.return_value = {"orchestrator": mock_orch}
        
        response = client.post("/v1/chat/completions", json={
            "model": "sayvis-agent",
            "messages": [{"role": "user", "content": "Hello"}]
        })
        
        assert response.status_code == 200
        data = response.json()
        assert "choices" in data
        assert data["choices"][0]["message"]["role"] == "assistant"

def test_mission_endpoints():
    from app.main import app
    client = TestClient(app)
    
    # Create mission
    with mock.patch('app.main.get_services') as mock_get:
        mock_orch = mock.AsyncMock()
        mock_orch.process_message = mock.AsyncMock(return_value={
            "response": '[{"title": "Task 1", "priority": "high"}]',
            "session_id": "test",
            "provider_used": "local",
            "model": "test",
            "tools_used": [],
            "memory_hits": [],
            "rag_sources": [],
            "processing_time_ms": 50,
            "language": "en",
            "is_persian": False
        })
        mock_get.return_value = {"orchestrator": mock_orch, "n8n": None}
        
        response = client.post("/api/v1/missions", json={
            "title": "Learn Rust",
            "description": "Become proficient in Rust",
            "priority": "high"
        })
        
        assert response.status_code == 200
        data = response.json()
        assert data["title"] == "Learn Rust"
        assert "id" in data
        mission_id = data["id"]
        
        # List missions
        response = client.get("/api/v1/missions")
        assert response.status_code == 200
        assert isinstance(response.json(), list)
        
        # Get specific mission
        response = client.get(f"/api/v1/missions/{mission_id}")
        assert response.status_code == 200
        
        # Delete
        response = client.delete(f"/api/v1/missions/{mission_id}")
        assert response.status_code == 200
