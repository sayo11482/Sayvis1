# راهنمای راه‌اندازی سایویس - نسخه فارسی

## نصب سریع (5 دقیقه)

### 1. پیش‌نیازها
- Docker Desktop (یا Docker Engine)
- Git
- 8GB RAM حداقل

### 2. کلون و اجرا

```bash
# کلون پروژه
git clone https://github.com/sayo11482/Sayvis1.git
cd Sayvis1/agent

# کپی تنظیمات
cp .env.example .env

# اجرا - پروفایل CPU
docker compose --profile cpu up -d

# مشاهده لاگ‌ها
docker compose logs -f
```

### 3. نصب مدل‌های Ollama

```bash
# مدل سبک برای شروع (4GB)
docker exec -it sayvis-ollama ollama pull qwen2.5:7b

# مدل امبدینگ
docker exec -it sayvis-ollama ollama pull nomic-embed-text

# مدل قدرتمند (اختیاری - 40GB)
docker exec -it sayvis-ollama ollama pull llama3.3:70b
```

### 4. بررسی سلامت

```bash
curl http://localhost:8000/health
curl http://localhost:8000/api/v1/status
```

باید ببینید:
```json
{"status": "healthy", "services": {"qdrant": true, "ollama": true, "n8n": true}}
```

## سرویس‌ها

| سرویس | آدرس | توضیح |
|-------|------|-------|
| SAYVIS Agent API | http://localhost:8000 | API اصلی عامل |
| API Docs | http://localhost:8000/docs | مستندات Swagger |
| n8n | http://localhost:5678 | اتوماسیون ورک‌فلو |
| Qdrant | http://localhost:6333/dashboard | داشبورد برداری |
| Ollama | http://localhost:11434 | سرور LLM |
| Frontend | http://localhost:8000/static/ | داشبورد وب |

## تست چت

```bash
curl -X POST http://localhost:8000/api/v1/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "سلام سایویس! یک ماموریت برای یادگیری Rust بساز",
    "language": "fa",
    "session_id": "test-123"
  }'
```

## اتصال اپ اندروید

1. اپ SAYVIS را نصب کنید
2. بروید به: تنظیمات → هوش مصنوعی و API
3. انتخاب کنید: **عامل حرفه‌ای سایویس (Self-hosted)**
4. نشانی را وارد کنید:
   - اگر روی همان کامپیوتر تست می‌کنید: `http://10.0.2.2:8000` (برای امولاتور)
   - اگر روی گوشی واقعی: `http://192.168.1.100:8000` (IP کامپیوتر خود را بگذارید)
5. تست اتصال را بزنید
6. باید ببینید: ✅ عامل حرفه‌ای سایویس متصل است!

### پیدا کردن IP کامپیوتر

```bash
# ویندوز
ipconfig

# مک/لینوکس
ifconfig
# یا
hostname -I
```

## ورک‌فلوهای n8n

4 ورک‌فلو آماده در `n8n/workflows/`:

1. **sayvis_main_agent.json** - ارکستریتور اصلی
2. **rag_ingestion.json** - ingestion خودکار اسناد
3. **trading_analysis.json** - تحلیل بازار
4. **auto_mission.json** - تجزیه خودکار ماموریت‌ها

برای import:
1. بروید به http://localhost:5678
2. Workflows → Import from File
3. فایل‌های JSON را import کنید

## عیب‌یابی

### پورت‌ها اشغال هستند

```bash
docker compose down
# پورت‌های مورد استفاده را بررسی کنید
lsof -i :8000
lsof -i :5678
```

### Ollama مدل ندارد

```bash
docker exec -it sayvis-ollama ollama list
docker exec -it sayvis-ollama ollama pull qwen2.5:7b
```

### Qdrant وصل نمی‌شود

```bash
docker compose logs qdrant
docker compose restart qdrant
```

### حافظه کم است

```bash
# فقط سرویس‌های ضروری
docker compose up postgres qdrant agent -d
```

### لاگ‌ها را ببینید

```bash
docker compose logs -f agent
docker compose logs -f ollama
docker compose logs -f n8n
```

## تست‌ها

```bash
cd backend
python -m venv venv
source venv/bin/activate  # ویندوز: venv\Scripts\activate
pip install -r requirements.txt
pytest tests/ -v
```

## حذف

```bash
docker compose down -v  # با حذف دیتا
docker compose down      # بدون حذف دیتا
```

## نکات حرفه‌ای

- برای GPU: `docker compose --profile gpu up`
- برای دیدن داشبورد وب: `frontend/index.html` را باز کنید
- برای RAG: اسناد را به `http://localhost:8000/api/v1/rag/ingest` بفرستید
- برای امنیت: `ZERO_TRUST_ENABLED=true` در `.env`

## پشتیبانی

- GitHub Issues: https://github.com/sayo11482/Sayvis1/issues
- مستندات: `agent/README.md`
- معماری: `agent/docs/ARCHITECTURE.md`
