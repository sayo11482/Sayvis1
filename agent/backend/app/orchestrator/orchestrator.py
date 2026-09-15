"""
Agent Orchestrator - Main AI Agent Logic
Professional multi-step reasoning with tools, memory, RAG
"""
from typing import List, Dict, Any, Optional
import time
import logging
import json
import re

logger = logging.getLogger(__name__)

class AgentOrchestrator:
    def __init__(
        self,
        ollama_service=None,
        memory_manager=None,
        rag_engine=None,
        tool_registry=None,
        zero_trust_engine=None,
        n8n_service=None
    ):
        self.ollama = ollama_service
        self.memory = memory_manager
        self.rag = rag_engine
        self.tools = tool_registry
        self.zero_trust = zero_trust_engine
        self.n8n = n8n_service

    def _detect_language(self, text: str) -> bool:
        """Detect if text is Persian"""
        persian_chars = sum(1 for c in text if '\u0600' <= c <= '\u06FF')
        return persian_chars > len(text) * 0.1

    def _build_system_prompt(self, language_fa: bool, context_text: str = "", rag_context: str = "") -> str:
        base_fa = """شما سایویس (SAYVIS) هستید - یک عامل هوش مصنوعی حاکمیتی، حرفه‌ای و امن برای مالک انسانی خود.

اصول شما:
- حاکمیت انسان: مالک کنترل کامل دارد
- Zero-Trust: هیچ خروجی AI بدون بررسی اعتماد نیست
- شفافیت: همیشه بگویید از چه ابزاری استفاده کردید
- فارسی روان: وقتی کاربر فارسی می‌نویسد، کاملاً فارسی پاسخ دهید

ابزارهای در دسترس:
{tools}

قوانین ابزار:
- فقط وقتی نیاز است از ابزار استفاده کن
- برای هر استفاده، دلیل بیاور
- ابزارهای پرخطر نیاز به تایید دارند
- همیشه نتیجه ابزار را در پاسخ نهایی خلاصه کن

کانتکست:
{context}

دانش مرتبط:
{rag}
"""

        base_en = """You are SAYVIS - a sovereign, professional, secure AI agent for your human owner.

Principles:
- Human Sovereignty: Owner has full control
- Zero-Trust: No AI output is trusted without verification
- Transparency: Always state which tools you used
- Precision: Calm, competent, security-minded

Available tools:
{tools}

Tool rules:
- Use tools only when needed
- Justify each tool use
- High-risk tools require confirmation
- Summarize tool results in final answer

Context:
{context}

Relevant knowledge:
{rag}
"""

        tools_desc = ""
        if self.tools:
            tools_desc = self.tools.get_tools_for_prompt(language_fa)

        template = base_fa if language_fa else base_en
        return template.format(
            tools=tools_desc or "No tools available (offline mode)",
            context=context_text or "No additional context",
            rag=rag_context or "No relevant knowledge found"
        )

    def _parse_tool_calls(self, text: str) -> List[Dict[str, Any]]:
        """Parse tool calls from LLM output - supports multiple formats"""
        tool_calls = []
        
        # Format 1: JSON tool calls
        json_pattern = r'```json\s*(\{[^`]*"tool"[^`]*\})\s*```'
        for match in re.finditer(json_pattern, text, re.DOTALL):
            try:
                data = json.loads(match.group(1))
                if "tool" in data:
                    tool_calls.append(data)
            except:
                pass

        # Format 2: TOOL: name {json}
        tool_pattern = r'TOOL:\s*(\w+)\s*(\{.*?\})'
        for match in re.finditer(tool_pattern, text, re.DOTALL):
            try:
                tool_name = match.group(1)
                params = json.loads(match.group(2))
                tool_calls.append({"tool": tool_name, "input": params})
            except:
                pass

        # Format 3: <tool>name</tool><input>{}</input>
        xml_pattern = r'<tool>(\w+)</tool>\s*<input>(.*?)</input>'
        for match in re.finditer(xml_pattern, text, re.DOTALL):
            try:
                tool_name = match.group(1)
                params = json.loads(match.group(2))
                tool_calls.append({"tool": tool_name, "input": params})
            except:
                pass

        return tool_calls

    async def process_message(
        self,
        message: str,
        session_id: str = "default",
        language: str = "auto",
        use_tools: bool = True,
        use_memory: bool = True,
        use_rag: bool = True,
        context: Dict[str, Any] = None
    ) -> Dict[str, Any]:
        start_time = time.time()
        
        # Detect language
        if language == "auto":
            is_persian = self._detect_language(message)
        else:
            is_persian = language == "fa"

        # Get memory context
        memory_context = ""
        memory_hits = []
        if use_memory and self.memory:
            mem_ctx = await self.memory.get_context_for_query(
                session_id=session_id,
                query=message,
                include_short=True,
                include_long=True
            )
            memory_context = mem_ctx.get("combined_text", "")
            memory_hits = mem_ctx.get("long_term", [])

        # Get RAG context
        rag_context = ""
        rag_sources = []
        if use_rag and self.rag:
            rag_context = await self.rag.get_context_for_prompt(message, top_k=3)
            rag_sources = await self.rag.search(message, top_k=3)

        # Build system prompt
        system_prompt = self._build_system_prompt(
            language_fa=is_persian,
            context_text=memory_context,
            rag_context=rag_context
        )

        # Get conversation history
        history = []
        if self.memory:
            short_term = self.memory.get_short_term(session_id, limit=6)
            for msg in short_term[-4:]:  # Last 4 messages
                history.append({
                    "role": msg["role"] if msg["role"] in ["user", "assistant"] else "user",
                    "content": msg["content"][:500]
                })

        # Generate initial response
        if not self.ollama:
            response_text = self._offline_response(message, is_persian)
            tools_used = []
        else:
            llm_result = await self.ollama.generate(
                prompt=message,
                system=system_prompt,
                temperature=0.7,
                history=history
            )
            response_text = llm_result.get("text", "")
            tools_used = []

            # Tool calling loop (max 3 iterations)
            if use_tools and self.tools:
                for iteration in range(3):
                    parsed_calls = self._parse_tool_calls(response_text)
                    if not parsed_calls:
                        break

                    logger.info(f"Tool calling iteration {iteration+1}: {len(parsed_calls)} calls")

                    for call in parsed_calls:
                        tool_name = call.get("tool", "")
                        tool_input = call.get("input", call.get("parameters", {}))

                        # Zero-trust check
                        if self.zero_trust:
                            risk = self.zero_trust.classify_tool(tool_name, tool_input)
                            decision = self.zero_trust.check_permission(
                                action=f"tool.{tool_name}",
                                risk_level=risk,
                                context={"input": tool_input, "session": session_id}
                            )
                            
                            if not decision.allowed:
                                tools_used.append({
                                    "tool": tool_name,
                                    "input": tool_input,
                                    "output": f"Blocked by zero-trust: {decision.reason_fa if is_persian else decision.reason_en}",
                                    "risk_level": risk.value,
                                    "success": False,
                                    "blocked": True
                                })
                                continue
                            
                            if decision.requires_confirmation:
                                # In API mode, we still execute but mark as requiring confirmation
                                logger.warning(f"High-risk tool {tool_name} requires confirmation")

                        # Execute tool
                        result = await self.tools.execute(tool_name, tool_input, context={
                            "language_fa": is_persian,
                            "session_id": session_id,
                            **(context or {})
                        })
                        tools_used.append(result)

                    # If tools were used, generate follow-up response with tool results
                    if tools_used:
                        tool_results_text = "\n".join([
                            f"Tool {t['tool']} result: {t.get('output', t.get('result', str(t)))}"
                            for t in tools_used[-3:]
                        ])
                        
                        follow_up_prompt = f"""Original query: {message}

Tool results:
{tool_results_text}

Now provide final answer in {'Persian' if is_persian else 'English'}, summarizing tool results and answering original query.
"""
                        
                        follow_up = await self.ollama.generate(
                            prompt=follow_up_prompt,
                            system=system_prompt,
                            temperature=0.5,
                            history=history
                        )
                        response_text = follow_up.get("text", response_text)
                        break

        # Save to memory
        if self.memory:
            await self.memory.add_to_short_term(session_id, "user", message)
            await self.memory.add_to_short_term(session_id, "assistant", response_text)
            
            # Add to long-term if important
            if len(message) > 20:  # Only substantial messages
                await self.memory.add_to_long_term(
                    content=f"User: {message}\nAssistant: {response_text[:500]}",
                    category="conversation",
                    importance=0.6,
                    session_id=session_id
                )

        # Trigger n8n workflows
        if self.n8n:
            try:
                await self.n8n.trigger_sayvis_workflows("new_message", {
                    "session_id": session_id,
                    "message": message,
                    "response": response_text[:500],
                    "language": "fa" if is_persian else "en",
                    "tools_used": len(tools_used)
                })
            except Exception as e:
                logger.warning(f"n8n trigger failed: {e}")

        elapsed_ms = int((time.time() - start_time) * 1000)

        return {
            "response": response_text,
            "session_id": session_id,
            "provider_used": "ollama" if self.ollama else "local_core",
            "model": getattr(self.ollama, 'model', 'sayvis-local-core') if self.ollama else "sayvis-local-core",
            "tools_used": tools_used,
            "memory_hits": memory_hits,
            "rag_sources": rag_sources,
            "processing_time_ms": elapsed_ms,
            "language": "fa" if is_persian else "en",
            "is_persian": is_persian
        }

    def _offline_response(self, message: str, is_persian: bool) -> str:
        msg_lower = message.lower()
        
        if is_persian:
            if any(word in message for word in ["سلام", "درود", "صبح بخیر"]):
                return "سلام! من سایویس هستم 👋\n\nدر حال حاضر در حالت آفلاین (هسته محلی) کار می‌کنم. با این حال می‌توانم:\n- ماموریت‌ها را مدیریت کنم\n- یادداشت‌ها را ذخیره کنم\n- محاسبات انجام دهم\n- تاریخ و زمان را بگویم\n\nبرای قابلیت‌های پیشرفته‌تر (جستجوی وب، RAG، تحلیل بازار)، لطفاً سرویس‌های Ollama و Qdrant را اجرا کنید:\n`docker compose --profile cpu up`"
            
            elif "ماموریت" in message:
                return "برای ساخت ماموریت جدید، لطفاً عنوان و توضیحات را مشخص کنید. مثال:\n\n`عنوان: یادگیری Rust`\n`توضیح: در 2 ماه به سطح متوسط برسم`\n\nمن آن را به تسک‌های کوچک‌تر تقسیم می‌کنم."
            
            elif "ابزار" in message:
                tools = self.tools.list_tools() if self.tools else []
                tool_list = "\n".join([f"- {t['name']}: {t['description_fa']}" for t in tools]) if tools else "ابزاری در دسترس نیست"
                return f"ابزارهای در دسترس:\n{tool_list}"
            
            else:
                return f"پیام شما دریافت شد: «{message[:100]}»\n\nمن سایویس در حالت آفلاین هستم. پاسخ کامل‌تری می‌خواهید؟ لطفاً Ollama را اجرا کنید یا سوال خود را دقیق‌تر بپرسید.\n\nمی‌توانم کمک کنم با:\n- مدیریت ماموریت‌ها\n- ذخیره در حافظه\n- محاسبات\n- تاریخ جلالی"
        
        else:
            if any(word in msg_lower for word in ["hello", "hi", "hey"]):
                return "Hello! I'm SAYVIS 👋\n\nCurrently running in offline local core mode. I can still:\n- Manage missions\n- Store memories\n- Perform calculations\n- Tell date/time\n\nFor advanced features (web search, RAG, market analysis), please start Ollama & Qdrant:\n`docker compose --profile cpu up`"
            
            elif "mission" in msg_lower:
                return "To create a new mission, please provide title and description. Example:\n\nTitle: Learn Rust\nDescription: Reach intermediate level in 2 months\n\nI'll break it down into actionable tasks."
            
            elif "tool" in msg_lower:
                tools = self.tools.list_tools() if self.tools else []
                tool_list = "\n".join([f"- {t['name']}: {t['description']}" for t in tools]) if tools else "No tools available"
                return f"Available tools:\n{tool_list}"
            
            else:
                return f"Received: \"{message[:100]}\"\n\nI'm SAYVIS in offline mode. For a more complete answer, please start Ollama or ask more specifically.\n\nI can help with:\n- Mission management\n- Memory storage\n- Calculations\n- Date/time"
