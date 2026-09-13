# 🧠 حافظهٔ پروژه SAYVIS — منبع حقیقت برای جلسات بعدی

> آخرین به‌روزرسانی: 2026-09-13 — بعد از انتشار موفق **v1.2**
> این فایل را هر عامل/جلسهٔ جدیدی که روی این پروژه کار می‌کند، **اول** بخواند.

---

## ۱. شناسنامهٔ پروژه

| مورد | مقدار |
|---|---|
| نام | SAYVIS — سیستم‌عامل هوش مصنوعی شخصی (اندروید) |
| ریپو | `sayo11482/Sayvis1` (عمومی) |
| برنچ کاری | **فقط** `arena/01a099b3-sayvis1` — هرگز main یا برنچ دیگر push نشود |
| پشته | Kotlin + Jetpack Compose + Room + Coroutines/Flow، minSdk 26، targetSdk 36 |
| بیلد | فقط GitHub Actions (در سندباکس Gradle/SDK نداریم) — workflow: `.github/workflows/build-apk.yml` |
| زبان کاربر | **فارسی** — همهٔ پاسخ‌های کاربر-رو باید فارسی باشد |

## ۲. وضعیت فعلی (2026-09-13)

- ✅ **v1.2 منتشر و عمومی است** (بیلد CI سبز، run #11 = 34749867153):
  - صفحهٔ Release: https://github.com/sayo11482/Sayvis1/releases/tag/v1.2
  - دانلود مستقیم: https://github.com/sayo11482/Sayvis1/releases/download/v1.2/SAYVIS-v1.2-debug.apk (~۲۱MB)
  - کامیت کد: `74025ff` (بالای آن کامیت APK بات: `chore(bot): publish SAYVIS v1.2`)
- ✅ تست شکست‌خورده شناسایی و اصلاح شد (جلسهٔ همان روز): `SayvisViewModelTest` عتیق به مقدار دموی اولیه (`2` مأموریت/`Online`) گره خورده بود؛ در v1.2 اسنپ‌شات زنده است → تست به انتظار مقدار واقعی نصب تازه (۰ مأموریت) با wait محدود بازنویسی شد.
- ✅ اتصال git بعد از انقضای موقت توکن خودش بازیابی شد؛ اگر دوباره 401/username دیدی: کاربر باید GitHub را در Arena از نو وصل کند. (نکته: بعد از هر شکست بیلد، کامیت بات روی remote می‌نشیند → قبل از push همیشه `git pull --rebase`.)
- نسخه‌های قبلی: v1.1 (https://github.com/sayo11482/Sayvis1/releases/tag/v1.1) و v1.0.

## ۳. قابلیت‌های v1.2 (چیز واقعی، نه دمو)

1. **هستهٔ AI آنلاین**: `ai/AIOrchestrator.kt` + سه Provider واقعی:
   - `GeminiProvider.kt` → `generativelanguage.googleapis.com/v1beta` (کلید به‌صورت query param) — مدل پیش‌فرض `gemini-3.5-flash`
   - `OpenAiCompatibleProvider.kt` → OpenRouter (`openrouter.ai/api/v1`) و Groq (`api.groq.com/openai/v1`) — Bearer + chat/completions — پیش‌فرض‌ها: `openai/gpt-4o-mini` و `llama-3.3-70b-versatile`
   - انتخاب Provider: `auto|gemini|openrouter|groq|local`
2. **تنظیمات درون‌برنامه‌ای**: تب «تنظیمات» (`ui/screens/SettingsScreen.kt`) — کلید API، مدل، انتخاب سرویس، دکمهٔ «تست اتصال» (testTag: `settings_test_connection`). کلیدها فقط در SharedPreferences دستگاه (`core/SettingsStore.kt`).
3. **حافظهٔ خودیادگیر**: هر تبادل چت → `MemoryItem` (INTERACTION_HISTORY) در Room؛ ۱۰ مورد آخر به پرامپت‌ها تزریق می‌شود (`ai/PromptFactory.kt`)؛ تب «حافظه» (`ui/screens/MemoryScreen.kt`) برای دیدن/حذف. Repository API ها: `allMemories / addMemory / deleteMemory / clearMemories`.
4. **تله‌متری واقعی**: `core/TelemetryProvider.kt` (باتری، رم، شبکه، سنسورها؛ runCatching + lazy برای سازگاری Robolectric) — هر ۳۰ ثانیه refresh و داخل پرامپت.
5. **تحلیل فایل با SAF**: دکمهٔ 📎 در `ui/screens/ChatScreen.kt` (testTag: `chat_attach_btn`) — `ActivityResultContracts.OpenDocument()` — محتوای فایل به پرامپت اضافه می‌شود. (دسترسی نامحدود فایل در اندروید جدید ممکن نیست؛ SAF همان راه قانونی است. MANAGE_EXTERNAL_STORAGE = آپشن آینده.)
6. **خودتغییرکاری ظاهر**: بلوک ```sayvis-ui``` در پاسخ AI (JSON با accent_color/language) → `SayvisViewModel.applyUiDirective` → `runtimeAccent` StateFlow → `MainActivity` + `Theme.kt` (پارامتر accentColor).

## ۴. نقشهٔ فایل‌های کلیدی v1.2

| فایل | نقش |
|---|---|
| `app/src/main/java/com/example/sayvis/core/SettingsStore.kt` | ذخیرهٔ کلیدها/تنظیمات (SharedPreferences) |
| `app/src/main/java/com/example/sayvis/core/TelemetryProvider.kt` | تله‌متری سخت‌افزار |
| `app/src/main/java/com/example/sayvis/ai/PromptFactory.kt` | ساخت پرامپت (شخصیت + حافظه + تله‌متری + فایل) |
| `app/src/main/java/com/example/sayvis/ai/{AIOrchestrator,GeminiProvider,OpenAiCompatibleProvider}.kt` | هستهٔ AI |
| `app/src/main/java/com/example/sayvis/data/local/SayvisDaos.kt` | DAO ها + `clearAll` حافظه |
| `app/src/main/java/com/example/sayvis/data/repository/SayvisRepository.kt` | API های حافظه (بخش «Memory System») |
| `app/src/main/java/com/example/sayvis/ui/SayvisViewModel.kt` | ViewModel بازنویسی‌شدهٔ کامل (صفحه‌های CHAT/AI→MEMORY/SETTINGS، تله‌متری، UI directive) |
| `app/src/main/java/com/example/sayvis/ui/SayvisMainApp.kt` | ناوبری + تب‌های جدید |
| `app/src/main/java/com/example/sayvis/ui/screens/{ChatScreen,SettingsScreen,MemoryScreen}.kt` | UI |
| `app/build.gradle.kts` | versionCode 3 / versionName "1.2" |
| `.github/workflows/build-apk.yml` | بیلد/تست/انتشار (بخش ۶ را ببین) |
| `GAP_ANALYSIS_FA.md` | سند شکاف قابلیت‌ها (بخش v1.2 اضافه شده) |

## ۵. قیدهای کاربر (قانون — هرگز نقض نشود)

1. پاسخ‌ها همیشه **فارسی**.
2. قبل از تحویل APK جدید، **تست رگرسیون خودکار** باید سبز باشد (سابقهٔ کرش v1.0).
3. دانلود باید از **لینک عمومی Release** باشد (کاربر با صفحه‌های artifact کلش داشت) — نه صفحات وابسته به لاگین.
4. فقط برنچ `arena/01a099b3-sayvis1` قابل push است.
5. کلید API باید کاربر خودش در اپ بگذارد: [aistudio.google.com/apikey](https://aistudio.google.com/apikey) (رایگان) / [openrouter.ai/keys](https://openrouter.ai/keys) / [console.groq.com/keys](https://console.groq.com/keys) — بعد دکمهٔ «تست اتصال».

## ۶. CI/CD — درس‌های سخت‌گرفته (مهم!)

- Workflow: JDK 21، SDK 36.1، `debug.keystore` کامیت‌شده، در موفقیت: کامیت APK توسط بات + ساخت Release عمومی `v$VERSION` با یادداشت فارسی/انگلیسی.
- **شل GitHub Actions = `bash -e -o pipefail`**: هر دستور fail شده (gradle، grep بدون match، grep|head با SIGPIPE) کل اسکریپت را می‌کشد. راه‌حل اعمال‌شده: اول هر بلاک `set +e`، بعد `status=$?`، بعد لاگ، بعد `exit 1`. **هرگز بدون set +e بنویس.**
- مکانیزم لاگ خطا (فعلاً فعال): در شکست بیلد/تست، `build-error.log`/`test-error.log` توسط بات با پیام `[skip ci]` کامیت و push می‌شود؛ publish step آن‌ها را پاک می‌کند.
- **نکتهٔ push:** هر شکست بیلد = یک کامیت بات روی remote → push بعدی من **رد می‌شود** (non-fast-forward) → همیشه قبل از push: `git pull --rebase origin arena/01a099b3-sayvis1`.
- خواندن خطاهای کامپایل بدون دانلود لاگ: annotations — `gh api repos/sayo11482/Sayvis1/commits/{sha}/check-runs` → `check-runs/{id}/annotations`. (خطاهای `e:` واقعاً با این آمد.)
- میزبان‌های بلاک‌شده در سندباکس: `results-receiver` (لاگ Actions) و `release-assets.githubusercontent.com` (دانلود فایل Release) → فایل APK را نمی‌توان داخل سندباکس دانلود کرد؛ تحویل = لینک عمومی به کاربر.
- identity بات: `sayvis-builder[bot] <41898282+github-actions[bot]@users.noreply.github.com>`.
- سیکل بیلد: ~۴–۵ دقیقه.

## ۷. کارهای باز (Backlog جلسهٔ بعد)

1. [x] تست fail شده اصلاح شد (SayvisViewModelTest → مقدارهای واقعی v1.2).
2. [ ] لاگ‌های خطا از history بات‌ها مانده‌اند (بی‌ضرر؛ در صورت دلخواه تمیزکاری شود).
3. [ ] راستی‌آزمایی مدل پیش‌فرض OpenRouter (`openai/gpt-4o-mini` — فرض بلندمدت، جستوجو نشده).
4. [ ] آپشن آینده: `MANAGE_EXTERNAL_STORAGE` برای دسترسی گسترده‌تر فایل (در صورت درخواست کاربر، با توجیه سیاست Play).
5. [ ] بعد از رفع تست: نسخهٔ v1.2.1 یا v1.3 طبق درخواست کاربر.

## ۸. تاریخچهٔ شمارهٔ نسخه‌ها

| نسخه | روایت |
|---|---|
| v1.0 | APK اولیه نصب‌شدنی |
| v1.1 | رفع کرش + تست رگرسیون + صفحهٔ Release عمومی |
| v1.2 | هستهٔ AI واقعی + حافظهٔ خودیادگیر + تله‌متری + SAF + خودتغییرکاری ظاهر |

---
*این فایل عمداً خودکفا نوشته شده تا هیچ جلسه‌ای به حافظهٔ گفتگو وابسته نباشد. با هر تغییر مهم پروژه، این فایل را هم به‌روز کن.*
