# ODIN QUANT - Professional Quantitative Trading System

> **اودین کوانت - سیستم ترید کوانت حرفه‌ای**
> Production-ready, modular, risk-first trading system

## 🎯 فلسفه: بقا > سود رویایی

این سیستم برای **بقای بلندمدت** طراحی شده، نه سودهای نجومی کوتاه‌مدت.

### اصول غیرقابل مذاکره:
- ✅ ریسک هر معامله: **حداکثر 1%**
- ✅ Drawdown روزانه: **حداکثر 3%** → Kill-switch
- ✅ Drawdown کلی: **حداکثر 15%** → توقف کامل
- ✅ Position sizing بر اساس ATR/Volatility
- ✅ هیچ سود تضمینی وجود ندارد!

---

## 📦 ساختار پروژه

```
quant/
├── config/
│   └── config.yaml          # تمام پارامترها بدون تغییر کد
├── data/
│   ├── data_fetcher.py      # yfinance, ccxt, polygon
│   ├── data_cleaner.py      # clean, resample, multi-timeframe
│   └── market_hours.py      # فیلتر ساعات معاملاتی
├── strategies/
│   ├── base_strategy.py     # کلاس پایه
│   ├── trend_following.py   # #1: MA Crossover + ADX + Volume
│   ├── mean_reversion.py    # #2: BB + RSI + Z-Score
│   ├── momentum_breakout.py # #3: ATR-based breakout
│   ├── pairs_trading.py     # #4: Statistical arbitrage
│   └── volatility_regime.py # #5: Regime detection
├── risk/
│   ├── risk_manager.py      # مدیر ریسک مرکزی
│   └── position_sizing.py   # ATR, volatility-based sizing
├── execution/
│   ├── paper_executor.py    # پیپر ترید
│   └── live_executor.py     # آماده برای لایو (ccxt)
├── backtest/
│   ├── backtester.py        # بک‌تست دقیق با کمیسیون و اسلیپیج
│   └── performance.py       # Sharpe, Sortino, MaxDD, Winrate, PF
├── alerts/
│   └── telegram_alerter.py  # اطلاع‌رسانی
├── main.py                  # اجرای اصلی
├── paper_trade.py           # اجرای پیپر ترید
├── backtest_script.py       # اسکریپت بک‌تست کامل
└── tests/
    └── test_strategies.py
```

---

## 🧠 استراتژی‌ها (به ترتیب اولویت)

### 1. Trend Following با فیلتر چندتایم‌فریم
- **Entry:** MA Crossover (EMA 20/50) + ADX > 25 + Volume > MA20
- **Exit:** SL: ATR*2, TP: ATR*3, Trailing: ATR*1.5, Time: 20 bars
- **Multi-timeframe:** H1 trend + M15 entry
- **Filter:** فقط در Trending regime

### 2. Mean Reversion
- **Entry:** BB (2σ) + RSI <30/>70 + Z-Score >2
- **Exit:** Mid BB, RSI 50, SL: BB width *1.5
- **Filter:** فقط در Ranging regime

### 3. Momentum Breakout
- **Entry:** Breakout of 20-bar high/low + ATR filter + Volume spike
- **Exit:** ATR trailing, time-based 10 bars

### 4. Pairs Trading
- **Entry:** Z-Score of spread >2, cointegration test
- **Exit:** Z-Score reverts to 0, stop if cointegration breaks

### 5. Volatility Regime Detection
- **Regimes:** Trending (ADX>25), Ranging (ADX<20, BB squeeze), High Vol (ATR > 2*MA)
- **Action:** تغییر استراتژی فعال بر اساس رژیم

---

## 🚀 نصب و اجرا

```bash
cd agent/quant
pip install -r requirements.txt

# بک‌تست
python backtest_script.py --config config/config.yaml --strategy trend_following --symbol BTC-USD --start 2023-01-01 --end 2024-01-01

# پیپر ترید
python paper_trade.py --config config/config.yaml --symbol BTC-USD

# تست‌ها
pytest tests/ -v
```

---

## 📊 خروجی بک‌تست

```
Sharpe: 1.45
Sortino: 2.10
Max DD: -8.2%
Winrate: 48%
Profit Factor: 1.65
Total Trades: 124
Avg Win: $120, Avg Loss: $-75
```

---

## ➕ افزودن استراتژی جدید

1. فایل جدید در `strategies/` بساز
2. از `BaseStrategy` ارث‌بری کن
3. متدهای `generate_signals`, `calculate_entry`, `calculate_exit` را پیاده کن
4. در `config.yaml` اضافه کن
5. تست بنویس

مثال:
```python
class MyStrategy(BaseStrategy):
    def generate_signals(self, data: pd.DataFrame) -> pd.DataFrame:
        # Entry/exit logic
        return data
```

---

## ⚠️ نکات امنیتی لایو

- همیشه با پیپر ترید شروع کن (حداقل 1 ماه)
- API keys را در `.env` بگذار، نه در کد
- از IP whitelist برای exchange استفاده کن
- فقط با سرمایه‌ای ترید کن که از دست دادنش مشکلی ندارد
- اخبار مهم (NFP, CPI) را فیلتر کن
- اسپرد و کمیسیون را در بک‌تست لحاظ کن
- Walk-forward analysis برای جلوگیری از overfit

---

## 📄 لایسنس

MIT - برای آموزش و پژوهش. مسئولیت ترید با خودتان!
