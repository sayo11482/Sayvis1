# 📱 SAYVIS Professional AI Agent - APK Download

## نسخه نصبی حرفه‌ای با ایجنت هوش مصنوعی

### 🎉 Build موفق!

**Tag:** `v2.0.0-build.87`
**Branch:** `arena/01a0a74a-sayvis1`
**Commit:** `ad12008` - fix: remove duplicate ProbeOutcome
**Size:** 23.5 MB (23,503,039 bytes)
**Build Time:** 2026-09-15 23:09 UTC
**Status:** ✅ Success

---

## 📥 لینک دانلود مستقیم

### APK اصلی (نصبی)
```
https://github.com/sayo11482/Sayvis1/releases/download/v2.0.0-build.87/SAYVIS-2.0.0-debug.apk
```

### SHA256 Checksum
```
https://github.com/sayo11482/Sayvis1/releases/download/v2.0.0-build.87/SAYVIS-2.0.0-debug.apk.sha256
```

### صفحه Release
```
https://github.com/sayo11482/Sayvis1/releases/tag/v2.0.0-build.87
```

---

## 📲 نحوه نصب

1. **دانلود APK** از لینک بالا (روی گوشی یا انتقال از کامپیوتر)

2. **فعال‌سازی نصب از منابع ناشناس:**
   - Settings → Apps → Special access → Install unknown apps
   - یا هنگام نصب، گزینه Allow را بزنید

3. **نصب:**
   - فایل APK را باز کنید
   - Install را بزنید

4. **اجرا:**
   - اپ SAYVIS را باز کنید
   - زبان فارسی را انتخاب کنید

---

## 🚀 ویژگی‌های نسخه حرفه‌ای

### ✅ جدید در این نسخه (Professional Agent Edition)

- **عامل حرفه‌ای سایویس (Self-hosted)** - Provider جدید در Settings → AI & API
  - اتصال به بک‌اند FastAPI حرفه‌ای
  - پشتیبانی از n8n, Ollama, Qdrant, Postgres
  - آدرس: `http://YOUR_PC_IP:8000`
  - تست اتصال با پیام فارسی

- **بک‌اند کامل AI Agent**
  - `agent/docker-compose.yml` - 6 سرویس
  - FastAPI + 7 ابزار با Zero-Trust
  - حافظه سلسله‌مراتبی + RAG
  - 27 تست پاس شده

- **n8n Workflows**
  - 4 ورک‌فلو آماده

- **رفع باگ‌ها**
  - Duplicate ProbeOutcome fix

### 📋 ویژگی‌های قبلی

- هسته محلی آفلاین
- UIC, AWARE, Missions, Trading, Scripts
- Zero-Trust Security
- Persian/English full support
- SecureVault با Keystore

---

## 🔧 اتصال به Agent حرفه‌ای

بعد از نصب APK:

### روی کامپیوتر (سرور Agent):
```bash
cd Sayvis1/agent
docker compose --profile cpu up -d
docker exec -it sayvis-ollama ollama pull qwen2.5:7b
```

### روی گوشی (اپ SAYVIS):
1. Settings → AI & API
2. Provider: **عامل حرفه‌ای سایویس (Self-hosted)**
3. URL: `http://192.168.1.100:8000` (IP کامپیوتر)
4. Test Connection → ✅

**نکته:** IP کامپیوتر را از `ipconfig` (Windows) یا `ifconfig` (Mac/Linux) بگیرید.

---

## 🛡️ امنیت

- امضای Debug (برای نصب نیاز به فعال‌سازی منابع ناشناس دارد)
- SHA256: در فایل `.sha256` موجود است
- Build توسط GitHub Actions (ubuntu-latest, JDK 17, Gradle 9.3.1)

---

## 📊 Build Log

- Run ID: 35034259141
- Workflow: Build APK
- Duration: 1m31s
- Status: Success ✅
- Artifact: sayvis-debug-apk (22.8 MB)

لینک Build:
```
https://github.com/sayo11482/Sayvis1/actions/runs/35034259141
```

---

## 🆘 عیب‌یابی

**نصب نمی‌شود؟**
- مطمئن شوید نصب از منابع ناشناس فعال است
- نسخه قبلی را حذف کنید

**Agent وصل نمی‌شود؟**
- Docker اجرا است؟ `docker compose ps`
- IP درست است؟ نه localhost
- فایروال پورت 8000 را باز کند
- گوشی و کامپیوتر در یک WiFi باشند

---

**ساخته شده با ❤️ - SAYVIS Professional Agent v2.0.0**
