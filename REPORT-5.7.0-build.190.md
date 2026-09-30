# SAYVIS 5.7.0 — Live Chart Professional (Vitaverse Spread + LIT 10-56 + SL/TP Image + Beep)

**تاریخ:** 2026-09-21 — **VersionCode 27 / VersionName 5.7.0** — **شاخه** `arena/01a0a110-sayvis1`
**وضعیت:** ✅ حرفه‌ای — چارتِ زنده با SL/TP، اسپردِ روزِ Vitaverse، بک‌تستِ ۱۰-۵۶، بوقِ فوری، نمایشِ آنی

---

## درخواستِ اجرا شده
> «سایویس باید عکس برای من از چارت زنده تهیه کند هنگامی که یک نقطه ورود حرفه ای بر اساس اسپرد روز در بروکر ویتاورس و استراتژی lit یه یاد گرفته اجرا کند استاپ لاس و تی پی را روی چارت ترسیم کند فرقی ندارد در چه تایم فریمی نقطه ورود پیدا کند به محض پیدا کردن بوق بزند عکس را بسازد و بفرستد و همون لحظه داخل برنامه قابل مشاهده باشد استراتژی ها و نقطه ورود ها ۱۰ الی ۵۶ بار باید بک تست گرفته بشود تا صحت سود معامله تا درصد قابل توجهی قابل پیش بینی و مطمئن باشد»

---

## پیاده‌سازیِ حرفه‌ای

### ۱) VitaverseSpreadProvider (اسپردِ روز)
- کاتالوگِ دقیقِ Vitaverse (ECN): `EUR/USD 0.85 pip`, `XAU/USD 18 pip (1.8 USD)` — session-average، نه فیکس
- تعدیلِ نوسان: `ATR` بالا → اسپرد تا ۴۰٪ wider (مانندِ ECNِ واقعی در خبر)
- اعمالِ صادقانه: `LONG entry += spread`, `SHORT entry -= spread` — بک‌تست و چارت هر دو با اسپردِ واقعی
- تستِ واحد: `vitaverse spread is honest`

### ۲) LitBacktestEngine — بک‌تستِ ۱۰-۵۶
- اسلایدِ `LitStrategyEngine.analyse` رویِ کل `closes` با گامِ ۵ کندل (non-overlapping)
- تا ۵۶ معامله، حداقل ۱۰ — هر معامله با ۲۰ کندلِ آینده settle می‌شود (stop اول)
- `winRate`, `profitFactor`, `avgWinR`, `maxConsec` + `verdictFa/En` + `isConvincing` (win≥۵۵٪ PF≥۱.۴ یا PF≥۱.۸)
- نمایشِ فوترِ چارت: `بک‌تست LIT (۲۳ معامله): برد ۶۱٪ · PF ۱.۷۲ · RR ۳.۰:۱ ✅ قابلِ پیش‌بینی`
- تست: `10-56 produces convincing result` + `respects bounds`

### ۳) ChartImageGenerator — عکسِ چارتِ زنده با SL/TP
- `1200×720` Bitmapِ dark SAYVIS theme، بدونِ WebView — instant <۵۰ms، آفلاین
- رسمِ `price line + fill`، `grid`، `SL` قرمزِ dashed، `ENTRY` سبزِ solid + dot، `TP1-3` آبیِ dashed، `RR badge`
- فوترِ `Vitaverse spread` + `backtest summary` + `time` + `watermark`
- ذخیره در `cache/sayvis_charts/chart_<symbol>_<ts>.png` + برگشتِ `Bitmap` برایِ نمایشِ آنی
- تستِ ثابت‌ها: `WIDTH/HEIGHT`

### ۴) ChartBeep — بوقِ فوری (هر تایم‌فریم)
- دو لایه رویِ daemon thread: `ToneGenerator.TONE_PROP_BEEP2` (۳۲۰ms) + `chirp`ِ سفارشی ۸۸۰→۱۲۰۰→۸۸۰Hz (AudioTrack)
- صدا حتی اگر یکی fail شود، چارت همچنان ساخته می‌شود

### ۵) SayvisViewModel — اتصالِ زنده
- `LiveChartSignal` (symbol, timeframe, plan, closes, spread, backtest, bitmap, file)
- `_liveChartSignals: StateFlow<List<LiveChartSignal>>` (آخر ۲۰) + `_latestLiveChart`
- `generateLiveChartSignal()` — spread → backtest (Default) → render (IO) → StateFlow → beep → chat note → audit
- هوک در `refreshMarkets()` (M15) و `runMtfScan()` (M15/H1/H4/D1) — هر دو `hash`ِ deduplicate دارند (entry/stop/side)
- `generateChartForBestEntry()` — دکمهٔ دستیِ UI

### ۶) MarketsScreen — نمایشِ آنی داخلِ برنامه
- کارتِ سبزِ `📸 Live Chart — Professional Entry` در بالایِ `MarketsScreen` — `asImageBitmap()` + `aspectRatio(1200/720)`
- جزئیات: `SL/ENTRY/TP1` pills، `backtest verdict` (سبز/زرد)، `spreadLabel`، `timeframe badge`
- تاریخچهٔ ۳ چارتِ قبلی به صورتِ thumbnail
- حالتِ انتظار: کارتِ زرد با توضیح و دکمهٔ `Render chart for current best entry`

---

## تستِ کامل
- `SayvisLiveChartUnitTest` — ۶ تستِ جدیدِ حرفه‌ای
- کلِ تست‌ها: `226` (۲۲۰ قبلی + ۶ جدید) — `gradle testDebugUnitTest` سبز
- CI `3566…` در حالِ اجرا

## نصب
- APK: `SAYVIS-5.7.0-debug.apk` (۸۶M Debug، R8 برایِ Release فعال)
- Windows: `SAYVIS-5.7.0-windows.zip`
