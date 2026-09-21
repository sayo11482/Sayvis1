# SAYVIS 5.6.1 — build 26 — نسخهٔ تمیزِ قابلِ نصب (Clean & Installable)

**تاریخ:** 2026-09-21 — **شاخه:** `arena/01a0a110-sayvis1` → `v5.6.1`  
**VersionCode:** 26 · **VersionName:** 5.6.1  
**وضعیت:** ✅ **نسخهٔ تمیز — تمام تست‌ها سبز، بدونِ اضافه، سرعتِ بهینه، قابلِ نصب**

---

## تستِ کامل
- `gradle testDebugUnitTest assembleDebug` — **۲۲۰ تستِ واحد سبز** (Arena ۳۷ + Sovereign ۷ + بقیه ۱۷۶) — CI `35662365420` سبز ۵m۵۰s
- `gradle :desktop:packageUberJarForCurrentOS` — Windows jar سبز
- هیچ `TODO/println/runBlocking` در main — فقط `Log.w`ِ failover + `Thread.sleep`ِ daemonِ صدا

## پاکسازیِ نهاییِ بدونِ عجله (۱۱۱+۲۹)
- ۱۱ گزارشِ قدیمی حذف (۱۳ → ۲) + این گزارش
- ۲۹ importِ مرده حذف در ۱۷ فایل (OkHttpClient/TimeUnit/…)
- ۰ فایلِ untracked، ۰ trailing در Kotlin، `git clean -nd` خالی

## نصب
- **APKِ Debugِ امضاشده** (`debug.keystore`ِ auto-generated) — برایِ نصب «نصب از منابع ناشناس» را فعال کنید
- **Windows:** `SAYVIS-5.6.1-windows.zip` — `SAYVIS.bat` را دوبار کلیک کنید (Java 17+)
- لینکِ دانلودِ مستقیم در Releaseِ `v5.6.1` و `v5.6.1-build.<run>` — بدونِ نیاز به لاگین (release) یا با لاگین (artifact)

## سرعتِ تائیدشده
- Arena ۶ گام ۲.۵s زنده، Sovereign ۱۸۰ms، liveness ۵s — هیچ لگی رویِ Main
