"""
Pydantic schemas for SAYVIS Agent API
"""
from pydantic import BaseModel, Field
from typing import Optional, List, Dict, Any, Literal
from datetime import datetime
from enum import Enum

class RiskLevel(str, Enum):
    LOW = "LOW"
    MEDIUM = "MEDIUM"
    HIGH = "HIGH"
    CRITICAL = "CRITICAL"

class MissionStatus(str, Enum):
    DRAFT = "DRAFT"
    ACTIVE = "ACTIVE"
    PAUSED = "PAUSED"
    COMPLETED = "COMPLETED"
    CANCELLED = "CANCELLED"

class ToolCall(BaseModel):
    tool: str
    input: Dict[str, Any]
    output: Optional[str] = None
    risk_level: RiskLevel = RiskLevel.LOW
    execution_time_ms: Optional[int] = None
    success: bool = True
    error: Optional[str] = None

class ChatRequest(BaseModel):
    message: str = Field(..., min_length=1, max_length=10000, description="User message")
    session_id: str = Field(default="default", description="Conversation session ID")
    language: Literal["fa", "en", "auto"] = Field(default="auto")
    context: Optional[Dict[str, Any]] = Field(default=None, description="Additional context")
    use_tools: bool = Field(default=True)
    use_memory: bool = Field(default=True)
    use_rag: bool = Field(default=True)
    stream: bool = Field(default=False)

class ChatResponse(BaseModel):
    response: str
    session_id: str
    provider_used: str
    model: str
    tools_used: List[ToolCall] = []
    memory_hits: List[Dict[str, Any]] = []
    rag_sources: List[Dict[str, Any]] = []
    processing_time_ms: int
    timestamp: datetime = Field(default_factory=datetime.utcnow)
    risk_assessment: Optional[RiskLevel] = None

class MissionCreate(BaseModel):
    title: str = Field(..., min_length=3, max_length=200)
    description: str = Field(..., max_length=2000)
    priority: Literal["low", "medium", "high", "critical"] = "medium"
    category: str = Field(default="general")
    deadline: Optional[datetime] = None
    tags: List[str] = Field(default_factory=list)

class MissionTask(BaseModel):
    id: str
    title: str
    completed: bool = False
    priority: str = "medium"

class MissionResponse(BaseModel):
    id: str
    title: str
    description: str
    status: MissionStatus
    priority: str
    tasks: List[MissionTask] = []
    progress: float = 0.0
    created_at: datetime
    updated_at: datetime

class AgentStatus(BaseModel):
    status: str
    version: str = "1.0.0"
    ollama_connected: bool
    qdrant_connected: bool
    postgres_connected: bool
    n8n_connected: bool
    current_model: str
    embed_model: str
    active_sessions: int
    total_missions: int
    total_memories: int
    emergency_lock: bool
    uptime_seconds: int

class MemoryItem(BaseModel):
    id: Optional[str] = None
    content: str
    category: str = "general"
    importance: float = Field(default=0.5, ge=0.0, le=1.0)
    metadata: Dict[str, Any] = Field(default_factory=dict)

class MemorySearchRequest(BaseModel):
    query: str
    top_k: int = 5
    category: Optional[str] = None
    min_importance: float = 0.0

class RAGIngestRequest(BaseModel):
    content: str
    source: str = "manual"
    metadata: Dict[str, Any] = Field(default_factory=dict)
    chunk_size: int = 500
    chunk_overlap: int = 50

class RAGSearchRequest(BaseModel):
    query: str
    top_k: int = 5
    filter_source: Optional[str] = None

class HealthResponse(BaseModel):
    status: str
    timestamp: datetime
    services: Dict[str, bool]
    version: str

class WorkflowTriggerRequest(BaseModel):
    workflow_id: str
    payload: Dict[str, Any] = Field(default_factory=dict)

class WorkflowResponse(BaseModel):
    workflow_id: str
    execution_id: Optional[str] = None
    status: str
    result: Optional[Dict[str, Any]] = None

class ZeroTrustCheckRequest(BaseModel):
    action: str
    risk_level: RiskLevel
    context: Dict[str, Any] = Field(default_factory=dict)

class ZeroTrustCheckResponse(BaseModel):
    allowed: bool
    requires_confirmation: bool
    reason: str
    risk_level: RiskLevel
