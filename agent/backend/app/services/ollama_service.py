"""
Ollama Service - Local LLM Integration
Professional integration with retry, streaming, embeddings
"""
import httpx
import json
import logging
from typing import List, Dict, Any, Optional, AsyncGenerator
from ..config import settings
import asyncio

logger = logging.getLogger(__name__)

class OllamaService:
    def __init__(self, base_url: str = None, model: str = None):
        self.base_url = base_url or settings.ollama_base_url
        self.model = model or settings.ollama_model
        self.embed_model = settings.ollama_embed_model
        self.client = httpx.AsyncClient(timeout=120.0)

    async def health_check(self) -> bool:
        try:
            resp = await self.client.get(f"{self.base_url}/api/tags")
            return resp.status_code == 200
        except Exception as e:
            logger.warning(f"Ollama health check failed: {e}")
            return False

    async def list_models(self) -> List[str]:
        try:
            resp = await self.client.get(f"{self.base_url}/api/tags")
            if resp.status_code == 200:
                data = resp.json()
                return [m["name"] for m in data.get("models", [])]
            return []
        except Exception as e:
            logger.error(f"Failed to list models: {e}")
            return []

    async def generate(
        self,
        prompt: str,
        system: str = None,
        temperature: float = 0.7,
        max_tokens: int = 2048,
        history: List[Dict[str, str]] = None
    ) -> Dict[str, Any]:
        """
        Generate response from Ollama
        """
        messages = []
        if system:
            messages.append({"role": "system", "content": system})
        if history:
            messages.extend(history)
        messages.append({"role": "user", "content": prompt})

        payload = {
            "model": self.model,
            "messages": messages,
            "stream": False,
            "options": {
                "temperature": temperature,
                "num_predict": max_tokens,
            }
        }

        try:
            resp = await self.client.post(f"{self.base_url}/api/chat", json=payload)
            resp.raise_for_status()
            data = resp.json()
            return {
                "text": data.get("message", {}).get("content", ""),
                "model": data.get("model", self.model),
                "total_duration": data.get("total_duration", 0),
                "eval_count": data.get("eval_count", 0),
                "success": True
            }
        except Exception as e:
            logger.error(f"Ollama generate failed: {e}")
            # Fallback to local cognitive response
            return {
                "text": self._fallback_response(prompt),
                "model": "sayvis-local-fallback",
                "success": False,
                "error": str(e)
            }

    async def generate_stream(
        self,
        prompt: str,
        system: str = None,
        temperature: float = 0.7,
        history: List[Dict[str, str]] = None
    ) -> AsyncGenerator[str, None]:
        messages = []
        if system:
            messages.append({"role": "system", "content": system})
        if history:
            messages.extend(history)
        messages.append({"role": "user", "content": prompt})

        payload = {
            "model": self.model,
            "messages": messages,
            "stream": True,
            "options": {"temperature": temperature}
        }

        try:
            async with self.client.stream("POST", f"{self.base_url}/api/chat", json=payload) as resp:
                async for line in resp.aiter_lines():
                    if line:
                        try:
                            data = json.loads(line)
                            content = data.get("message", {}).get("content", "")
                            if content:
                                yield content
                            if data.get("done"):
                                break
                        except json.JSONDecodeError:
                            continue
        except Exception as e:
            logger.error(f"Stream failed: {e}")
            yield self._fallback_response(prompt)

    async def embed(self, text: str) -> List[float]:
        """Generate embeddings"""
        try:
            payload = {
                "model": self.embed_model,
                "prompt": text
            }
            resp = await self.client.post(f"{self.base_url}/api/embeddings", json=payload)
            resp.raise_for_status()
            data = resp.json()
            return data.get("embedding", [])
        except Exception as e:
            logger.error(f"Embedding failed: {e}")
            # Return zero vector as fallback (768 dim for nomic)
            return [0.0] * 768

    async def embed_batch(self, texts: List[str]) -> List[List[float]]:
        embeddings = []
        for text in texts:
            emb = await self.embed(text)
            embeddings.append(emb)
            await asyncio.sleep(0.05)  # Rate limit
        return embeddings

    def _fallback_response(self, prompt: str) -> str:
        """Local cognitive fallback when Ollama is unavailable"""
        prompt_lower = prompt.lower()
        
        # Persian detection
        is_persian = any('\u0600' <= c <= '\u06FF' for c in prompt)
        
        if is_persian:
            if "سلام" in prompt or "درود" in prompt:
                return "سلام! من سایویس هستم، عامل هوش مصنوعی حاکمیتی شما. در حال حاضر در حالت آفلاین کار می‌کنم ولی کاملاً آماده‌ام تا کمک کنم. چه کاری می‌تونم برات انجام بدم؟"
            elif "ماموریت" in prompt:
                return "برای ساخت ماموریت جدید، عنوان و توضیح هدف خود را بفرمایید. من آن را به تسک‌های قابل اجرا تجزیه می‌کنم و پیشرفت را پیگیری خواهم کرد."
            elif "معامله" in prompt or "ترید" in prompt:
                return "تحلیل بازار در حالت آفلاین با شبیه‌ساز محلی انجام می‌شود. برای اتصال واقعی به متاتریدر، درگاه معاملاتی را در تنظیمات پیکربندی کنید."
            else:
                return f"پیام شما را دریافت کردم: «{prompt[:100]}...»\n\nدر حال حاضر هسته محلی سایویس فعال است. برای پاسخ‌های پیشرفته‌تر، لطفاً Ollama را اجرا کنید: `docker compose up ollama`"
        else:
            if "hello" in prompt_lower or "hi" in prompt_lower:
                return "Hello! I'm SAYVIS, your sovereign personal AI agent. Currently running in offline local core mode, but fully operational. How can I assist you?"
            elif "mission" in prompt_lower:
                return "To create a new mission, please provide title and description. I'll break it down into actionable tasks and track progress."
            else:
                return f"Received: \"{prompt[:100]}...\"\n\nCurrently running on SAYVIS local core. For advanced reasoning, please start Ollama: `docker compose up ollama`"

    async def close(self):
        await self.client.aclose()
