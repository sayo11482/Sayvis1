# گزارش تک ۱۴ — SAYVIS 3.1.0: گیت‌هاب موتور LIT را واقعاً رتیون می‌کند

## ✅ تحویل‌شده

**Release:** [v3.1.0-build.64](https://github.com/sayo11482/Sayvis1/releases/tag/v3.1.0-build.64)
**APK:** [SAYVIS-3.1.0-debug.apk](https://github.com/sayo11482/Sayvis1/releases/download/v3.1.0-build.64/SAYVIS-3.1.0-debug.apk) (61 مگابایت، + [SHA256](https://github.com/sayo11482/Sayvis1/releases/download/v3.1.0-build.64/SAYVIS-3.1.0-debug.apk.sha256))
**کامیت سبز:** `d930adb` — CI: **completed / success** (14/14 مرحله سبز، versionCode 14 / versionName 3.1.0)

> نام build بر اساس شمارهٔ run است (قرارداد ثابت پروژه): دورهای قرمز ۵۸ تا ۶۳ شماره را مصرف کردند؛ سبز نهایی run #64 شد.

## 🔄 حلقهٔ ارتقای گیت‌هاب → موتور (کاملاً واقعی، نه تصویری)

1. **اسکن:** `scanTradingTuning` با GitHub Search API (بدون کلید، ستارگان نزولی، ۱۰ ریپو برتر `trading bot strategy`).
2. **استخراج سیگنال:** از description + topics هر ریپو → `deriveTuning` (instance method):
   - scalp/scalping/m1/m5/intraday → atrFactor=1.2
   - swing/daily/long-term → atrFactor=2.0
   - mean reversion/bollinger/oversold → گیت RSI 72/28
   - breakout/liquidity/smc/order block/smart money/risk-reward → اهداف [3, 5, 8]
   - فقط ریپوهای ⭐≥100 «منبع» حساب می‌شوند (provenance)؛ بدون سیگنال = پیش‌فرض خانه (1.5، 75/25، [3, 4.5, 6])
3. **اعمال روی خودِ موتور:** `LitStrategyEngine.Tuning` (ATR factor، گیت‌های RSI، نردبان اهداف) → persist در تنظیمات (کلید `tradeTuning`) → `refreshMarkets` فوراً با تیونینگ جدید.
4. **حاکمیت:** audit event با ریسبک MEDIUM_RISK / OWNER_CONFIRMED و digest شامل پارامترها و تعداد ریپو؛ UI (MarketsScreen) خط تیونینگ فعال + ریپوهای منبع با ⭐ + دکمهٔ «ارتقای خودکار از گیت‌هاب» را نشان می‌دهد.

## 🔒 کف سخت 1:3 — شکستنی حتی در برابر اسکن

`Tuning.safe()` همیشه: atr∈[1, 2.5]، rsiHigh∈[60, 85]، rsiLow∈[15, 40]، اهداف clamp به ≥3 و اکیداً صعودی (+0.5 پلکانی). تست صریح: پیشنهاد `[1.5, 2.0]` → clamp به ≥3.0. وتوی RSI تیون‌شده هم تست شد (RSI74: سیگنال LONG خانه → WAIT تیون‌شده).

## 🧪 تست — ۱۲ تست LIT سبز در CI

۸ تست قبلی + ۴ تست جدید: derivation از ریپوهای سیگنال‌دار، defaults بدون سیگنال، وتوی گیت RSI + clamp کف 1:3، و round-trip JSON با provenance (+ null روی ورودی خراب).

## 🧯 مسیر ۷ دور CI تا سبز (شفاف)

| دور | کامیت | خطا | درس |
|---|---|---|---|
| 58 | 479d962 | `_tradeTuning must be initialized` | init block قبل از اعلان StateFlow ممنوع؛ مقداردهی در محل اعلان |
| 59 | 221d174 | `deriveTuning` unresolved | instance method است نه static |
| 60 | 243e8f6 | تست JSON | org.json روی JVM stub است → JSON دستی با `\u0022` |
| 61 | 6d1262e | syntax خط 62 | `\s`/`\.` تک‌بک‌اسلشی در رشتهٔ Kotlin نامعتبر است |
| 62 | c08d38d | تست NPE | کلاس کاراکتری regex باز می‌ماند (`[^\"\\]` ناقص) |
| 63 | 18abf10 | type mismatch خط 76 | `\\"` در ابتدای literal رشته را می‌بندد؛ باید `\\\"` باشد |
| **64** | **d930adb** | — | **سبز؛ الگو با دیکدر سخت‌گیرانهٔ Kotlin + شبیه‌سازی رفتاری round-trip اثبات شد** |

الگوی نهایی اثبات‌شده برای همهٔ ۵ regex هلپر (بایت‌محور، مرجع آینده): `\\\"` برای کوتیشن داخل pattern، `\\\\.` برای escape-pair، اعضای کلاس `\"` و `\\\\` — دقیقاً هم‌خانوادهٔ `pairRegex` اثبات‌شدهٔ WebSearchService.

## 🧾 جمع‌بندی معیارها

- CI سبز ✅ — run [35013025884](https://github.com/sayo11482/Sayvis1/actions/runs/35013025884)
- Release + APK ✅ — v3.1.0-build.64 با APK و sha256
- تست ✅ — ۱۲ تست LIT + کل مجموعهٔ unit در CI پاس
- عملکرد واقعی نه تصویری ✅ — اسکن → derive → apply → persist → re-rank همه در کد اجرایی و پوشش‌داده‌شده با تست
- گزارش فارسی ✅ — همین سند
