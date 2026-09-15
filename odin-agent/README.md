# 🔥 ODIN AGENT - Professional Quantitative Trading AI Agent

> **اودین ایجنت - عامل ترید کوانت و هوش مصنوعی حرفه‌ای**
> **کاملاً جدا از پروژه سایویز**

## 🎯 فلسفه: بقا > سود رویایی

- ✅ 1% ریسک هر معامله (غیرقابل مذاکره)
- ✅ 3% DD روزانه → Kill-switch خودکار
- ✅ 15% DD کلی → توقف کامل
- ✅ هیچ سود تضمینی نیست!

---

## 📦 این پروژه چیست؟

**ODIN AGENT** یک سیستم ترید کوانت و AI Agent حرفه‌ای، ماژولار و production-ready است که **کاملاً جدا از سایویز** ساخته شده.

### معماری:

```
odin-agent/
├── app/                    # اپ اندروید مستقل (Kotlin + Compose)
│   ├── models/             # QuantModels, TradingModels
│   ├── strategies/         # 5 استراتژی جدا
│   ├── risk/               # RiskManager
│   ├── backtest/           # Backtester
│   ├── data/               # DataFetcher
│   └── ui/screens/         # Dashboard, Strategies, Backtest, PaperTrade
│
├── backend/                # بک‌اند FastAPI (جدا)
│   ├── quant/              # سیستم کوانت پایتون
│   └── api/                # API های ترید
│
├── quant/                  # هسته کوانت پایتون (مستقل)
│   ├── config/config.yaml
│   ├── strategies/ (5)
│   ├── risk/
│   └── backtest/
│
└── docker-compose.yml      # n8n + Ollama + Qdrant + Postgres
```

---

## 🧠 5 استراتژی اولویت‌دار

### 1. Trend Following (Multi-Timeframe) - Priority 1
- **Entry:** EMA20/50 crossover + ADX>25 + Volume>MA + HTF filter
- **Exit:** SL ATR*2, TP ATR*3, Trailing ATR*1.5, Time 20 bars
- **Best Regime:** Trending
- **Why:** پایدارترین در بلندمدت - 30% روندها کل ضررها را جبران می‌کند

### 2. Mean Reversion (BB + RSI + Z-Score) - Priority 2
- **Entry:** Price < BB lower + RSI<30 + Z<-2
- **Exit:** Mid BB, RSI 50
- **Best Regime:** Ranging

### 3. Momentum Breakout (ATR-based) - Priority 3
- **Entry:** Breakout 20-bar high/low + ATR filter + Volume spike
- **Exit:** ATR trailing
- **Best Regime:** High Vol

### 4. Pairs Trading / Stat Arb - Priority 4
- **Entry:** Z-Score spread >2 + cointegration
- **Exit:** Z→0
- **Best Regime:** All (Market neutral)

### 5. Volatility Regime Detection - Priority 5
- **Regimes:** Trending (ADX>25), Ranging (ADX<20+BB squeeze), High Vol (ATR>2*MA)
- **Action:** تغییر استراتژی فعال

---

## 🚀 نصب سریع

### اپ اندروید (APK)

**دانلود مستقیم:**
```
https://github.com/sayo11482/Sayvis1/releases/download/v4.0.0-odin-quant-build.91/SAYVIS-4.0.0-odin-quant-debug.apk
```

بعد از این نسخه، نسخه جدید ODIN مستقل ساخته می‌شود.

### بک‌اند + کوانت (جدا از اپ)

```bash
cd odin-agent
cp .env.example .env
docker compose --profile cpu up -d

# بک‌تست
cd quant
python backtest_script.py --strategy trend_following --symbol BTC/USDT

# پیپر ترید
python paper_trade.py --symbol BTC/USDT
```

---

## 📊 بک‌تست

```bash
python quant/backtest_script.py --config quant/config/config.yaml --strategy trend_following --symbol BTC/USDT --start 2023-01-01 --end 2024-01-01

# خروجی:
# Total Trades: 124 | Winrate: 48% | Sharpe: 1.45 | Max DD: -8.2% | PF: 1.65
```

---

## 🛡️ مدیریت ریسک

```yaml
risk:
  max_risk_per_trade: 0.01  # 1%
  max_daily_drawdown: 0.03  # 3% → Kill-switch
  max_total_drawdown: 0.15  # 15% → Stop
```

---

## 📱 اپ اندروید - ویژگی‌ها

- **Dashboard:** سرمایه، PnL، Drawdown، Kill-switch
- **Strategies:** لیست 5 استراتژی + فعال/غیرفعال + پارامترها
- **Backtest:** اجرای بک‌تست + گزارش Sharpe, Sortino, Winrate
- **Paper Trade:** شروع/توقف پیپر ترید + پوزیشن‌های باز
- **Risk Monitor:** وضعیت ریسک لحظه‌ای
- **AI Agent Chat:** چت با اودین برای تحلیل بازار

---

## ➕ افزودن استراتژی جدید

1. فایل جدید در `app/src/main/java/com/odin/agent/strategies/`
2. از `BaseStrategy` ارث ببر
3. `generateSignals()` رو پیاده کن
4. در `config.yaml` اضافه کن

---

## ⚠️ امنیت لایو

- پیپر ترید 1 ماه قبل از لایو
- API keys در `.env`
- IP whitelist
- فقط سرمایه قابل از دست دادن
- فیلتر اخبار NFP/CPI
- Walk-forward برای جلوگیری از overfit

---

## 📄 جدا از سایویز؟

**بله، کاملاً جدا!**

- Package: `com.odin.agent` (جدا از `com.example` سایویز)
- پروژه مستقل: `odin-agent/` (جدا از `app/`)
- می‌تونی فقط کوانت رو جدا استفاده کنی بدون اندروید
- APK جدا: ODIN AGENT

---

**ODIN AGENT v4.0.0 - اودین ایجنت - بقا > سود رویایی**
