# SAYVIS Professional AI Agent - Self-Hosted AI Starter Kit

> **سایویس - پلتفرم عامل هوش مصنوعی حاکمیتی حرفه‌ای**
> Sovereign Personal AI Agent Platform - Professional Edition

این پروژه یک **AI Agent حرفه‌ای و کامل** بر اساس Self-hosted AI Starter Kit (n8n, Ollama, Qdrant, Postgres) است که برای SAYVIS ساخته شده.

## 🚀 ویژگی‌های حرفه‌ای

### معماری اصلی
- **n8n**: ارکستریتور ورک‌فلو و اتوماسیون
- **Ollama**: اجرای LLM محلی (Llama 3.3, Qwen, Mistral, ...)
- **Qdrant**: پایگاه داده برداری برای حافظه بلندمدت و RAG
- **Postgres**: ذخیره‌سازی اصلی ماموریت‌ها، مکالمات، و لاگ امنیتی
- **SAYVIS Agent Backend**: FastAPI + Python - مغز متفکر عامل

### قابلیت‌های عامل
- ✅ **Multi-Tool Reasoning**: جستجوی وب، محاسبات، تحلیل بازار، مدیریت فایل، اجرای اسکریپت
- ✅ **حافظه سلسله‌مراتبی**: کوتاه‌مدت (Redis-like in-memory) + بلندمدت (Qdrant) + اپیزودیک (Postgres)
- ✅ **RAG پیشرفته**: ingestion خودکار، chunking هوشمند، reranking
- ✅ **Zero-Trust Security**: تمام اقدامات از دروازه امنیتی عبور می‌کنند
- ✅ **Mission Engine**: تجزیه اهداف کلان به تسک‌های قابل اجرا
- ✅ **AWARE Engine**: پیشنهادهای فعال بر اساس کانتکست
- ✅ **n8n Integration**: اجرای ورک‌فلوهای n8n به عنوان ابزار
- ✅ **Trading Intelligence**: تحلیل بازار و اتصال به MT4/MT5
- ✅ **Persian-First**: پشتیبانی کامل فارسی + انگلیسی

## 📦 راه‌اندازی سریع

### پیش‌نیازها
- Docker & Docker Compose
- Git
- 8GB RAM حداقل (16GB پیشنهادی)

### نصب

```bash
# کلون کردن
git clone https://github.com/sayo11482/Sayvis1.git
cd Sayvis1/agent

# کپی env
cp .env.example .env
# مقادیر را ویرایش کنید

# اجرا با پروفایل CPU (یا gpu برای NVIDIA)
docker compose --profile cpu up -d

# لاگ‌ها
docker compose logs -f
```

سرویس‌ها:
- n8n: http://localhost:5678
- SAYVIS Agent API: http://localhost:8000
- Qdrant Dashboard: http://localhost:6333/dashboard
- Ollama API: http://localhost:11434
- Postgres: localhost:5432

### نصب اولیه Ollama مدل

```bash
docker exec -it ollama ollama pull llama3.3:70b
# یا مدل سبک‌تر
docker exec -it ollama ollama pull qwen2.5:7b
docker exec -it ollama ollama pull nomic-embed-text
```

### تست سلامت

```bash
curl http://localhost:8000/health
curl http://localhost:8000/api/v1/agent/status
```

## 🧠 API مستندات

بعد از اجرا: http://localhost:8000/docs (Swagger)

### مثال چت

```bash
curl -X POST http://localhost:8000/api/v1/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "سلام سایویس! یک ماموریت برای یادگیری Rust بساز",
    "language": "fa",
    "session_id": "user-123"
  }'
```

### مثال ماموریت

```bash
curl -X POST http://localhost:8000/api/v1/missions \
  -H "Content-Type: application/json" \
  -d '{
    "title": "یادگیری معامله‌گری حرفه‌ای",
    "description": "3 ماهه به سود مستمر برسم",
    "priority": "high"
  }'
```

## 🔧 ساختار پروژه

```
agent/
├── docker-compose.yml      # ارکستریشن کامل
├── .env.example
├── backend/
│   ├── Dockerfile
│   ├── requirements.txt
│   ├── app/
│   │   ├── main.py         # FastAPI entry
│   │   ├── config.py
│   │   ├── agents/
│   │   │   ├── sayvis_agent.py
│   │   │   └── tools/
│   │   ├── memory/
│   │   ├── rag/
│   │   ├── orchestrator/
│   │   ├── security/
│   │   ├── api/routes/
│   │   └── services/
│   └── tests/
├── n8n/
│   └── workflows/          # ورک‌فلوهای آماده
├── frontend/               # داشبورد ساده
└── docs/
```

## 🛡️ امنیت Zero-Trust

تمام اقدامات عامل از `ZeroTrustPermissionEngine` عبور می‌کند:
- LOW: اجرای خودکار
- MEDIUM: لاگ + اطلاع
- HIGH: نیاز به تایید کاربر
- CRITICAL: مسدود در قفل اضطراری

## 📊 تست‌ها

```bash
cd backend
pip install -r requirements.txt
pytest tests/ -v --cov=app

# تست یکپارچگی
docker compose -f ../docker-compose.yml exec agent pytest tests/ -v
```

## 🔗 اتصال به اپ اندروید SAYVIS

در اپ اندروید:
1. Settings → AI & API
2. Provider = Custom OpenAI-compatible
3. Base URL = `http://YOUR_SERVER_IP:8000/v1`
4. Model = `sayvis-agent`

یا از طریق n8n webhook.

## 📄 لایسنس

MIT - همانند SAYVIS اصلی

---

**ساخته شده با ❤️ برای حاکمیت انسان بر هوش مصنوعی**
