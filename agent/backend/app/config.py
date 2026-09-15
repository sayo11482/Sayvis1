"""
SAYVIS Agent Configuration - Professional Edition
"""
from pydantic_settings import BaseSettings
from pydantic import Field
from typing import Optional, List
import os

class Settings(BaseSettings):
    # Core
    env: str = Field(default="development", alias="SAYVIS_ENV")
    log_level: str = Field(default="INFO", alias="SAYVIS_LOG_LEVEL")
    secret_key: str = Field(default="dev_secret_key_change_in_prod", alias="SAYVIS_SECRET_KEY")
    api_key: str = Field(default="sayvis_dev_api_key", alias="SAYVIS_API_KEY")
    host: str = Field(default="0.0.0.0", alias="SAYVIS_AGENT_HOST")
    port: int = Field(default=8000, alias="SAYVIS_AGENT_PORT")

    # Database
    postgres_user: str = Field(default="sayvis", alias="POSTGRES_USER")
    postgres_password: str = Field(default="sayvis_secure_password_2024", alias="POSTGRES_PASSWORD")
    postgres_db: str = Field(default="sayvis_agent", alias="POSTGRES_DB")
    postgres_host: str = Field(default="localhost", alias="POSTGRES_HOST")
    postgres_port: int = Field(default=5432, alias="POSTGRES_PORT")
    database_url: Optional[str] = Field(default=None, alias="DATABASE_URL")

    @property
    def async_database_url(self) -> str:
        if self.database_url:
            url = self.database_url
            if url.startswith("postgresql://"):
                return url.replace("postgresql://", "postgresql+asyncpg://", 1)
            return url
        return f"postgresql+asyncpg://{self.postgres_user}:{self.postgres_password}@{self.postgres_host}:{self.postgres_port}/{self.postgres_db}"

    @property
    def sync_database_url(self) -> str:
        if self.database_url:
            url = self.database_url
            if "asyncpg" in url:
                return url.replace("postgresql+asyncpg://", "postgresql://")
            return url
        return f"postgresql://{self.postgres_user}:{self.postgres_password}@{self.postgres_host}:{self.postgres_port}/{self.postgres_db}"

    # Qdrant
    qdrant_host: str = Field(default="localhost", alias="QDRANT_HOST")
    qdrant_port: int = Field(default=6333, alias="QDRANT_PORT")
    qdrant_url: str = Field(default="http://localhost:6333", alias="QDRANT_URL")
    qdrant_api_key: Optional[str] = Field(default=None, alias="QDRANT_API_KEY")

    # Ollama
    ollama_host: str = Field(default="localhost", alias="OLLAMA_HOST")
    ollama_port: int = Field(default=11434, alias="OLLAMA_PORT")
    ollama_base_url: str = Field(default="http://localhost:11434", alias="OLLAMA_BASE_URL")
    ollama_model: str = Field(default="qwen2.5:7b", alias="OLLAMA_MODEL")
    ollama_embed_model: str = Field(default="nomic-embed-text", alias="OLLAMA_EMBED_MODEL")

    # n8n
    n8n_api_url: str = Field(default="http://localhost:5678", alias="N8N_API_URL")
    n8n_api_key: Optional[str] = Field(default=None, alias="N8N_API_KEY")
    n8n_host: str = Field(default="localhost", alias="N8N_HOST")

    # Redis
    redis_host: str = Field(default="localhost", alias="REDIS_HOST")
    redis_port: int = Field(default=6379, alias="REDIS_PORT")
    redis_url: str = Field(default="redis://localhost:6379/0", alias="REDIS_URL")

    # Security
    zero_trust_enabled: bool = Field(default=True, alias="ZERO_TRUST_ENABLED")
    emergency_lock: bool = Field(default=False, alias="EMERGENCY_LOCK")
    require_high_risk_confirmation: bool = Field(default=True, alias="REQUIRE_HIGH_RISK_CONFIRMATION")

    # Features
    enable_trading: bool = Field(default=False, alias="ENABLE_TRADING")
    enable_web_search: bool = Field(default=True, alias="ENABLE_WEB_SEARCH")
    enable_rag: bool = Field(default=True, alias="ENABLE_RAG")
    enable_n8n_tools: bool = Field(default=True, alias="ENABLE_N8N_TOOLS")

    # External APIs
    openai_api_key: Optional[str] = Field(default=None, alias="OPENAI_API_KEY")
    openrouter_api_key: Optional[str] = Field(default=None, alias="OPENROUTER_API_KEY")
    groq_api_key: Optional[str] = Field(default=None, alias="GROQ_API_KEY")
    gemini_api_key: Optional[str] = Field(default=None, alias="GEMINI_API_KEY")
    tavily_api_key: Optional[str] = Field(default=None, alias="TAVILY_API_KEY")
    serpapi_api_key: Optional[str] = Field(default=None, alias="SERPAPI_API_KEY")

    # Agent behavior
    max_iterations: int = 10
    temperature: float = 0.7
    max_tokens: int = 2048
    memory_top_k: int = 5
    rag_top_k: int = 5

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"
        case_sensitive = False
        extra = "ignore"

# Singleton
settings = Settings()

# Collection names for Qdrant
QDRANT_COLLECTIONS = {
    "memory": "sayvis_memory",
    "knowledge": "sayvis_knowledge",
    "missions": "sayvis_missions",
    "conversations": "sayvis_conversations",
}

# Risk levels matching Android app
RISK_LEVELS = ["LOW", "MEDIUM", "HIGH", "CRITICAL"]

# Tool registry
AVAILABLE_TOOLS = [
    "web_search",
    "calculator",
    "qdrant_memory_search",
    "qdrant_knowledge_search",
    "postgres_query",
    "n8n_workflow",
    "trading_analysis",
    "script_runner",
    "file_manager",
    "datetime_tool",
]
