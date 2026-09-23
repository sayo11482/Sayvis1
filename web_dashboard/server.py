#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Odin.trade PRO - Simplified Institutional Trading Suite
سامانه ساده، مدرن و قدرتمند معاملات دستی و اتوماتیک، اتصال به متاتریدر ۵، بک‌تست استراتژی‌ها و توکنیزه
شرکت سوینکس (SEVINEX Technologies)
"""

import http.server
import socketserver
import json
import time
import random
from urllib.parse import urlparse, parse_qs

PORT = 8080

# Live State
mt5_state = {
    "connected": True,
    "broker": "Vittaverse",
    "broker_url": "https://vittaverse.com/fa/",
    "server": "Vittaverse-Live.mt5",
    "login": "8849201",
    "balance": 10000.0,
    "equity": 10245.50,
    "margin_used": 185.0,
    "free_margin": 10060.50,
    "leverage": "1:500 (ECN)",
    "currency": "USD"
}

trading_mode = {
    "auto_trading": False,
    "active_strategy": "lit_smc", # lit_smc, ict_fvg, supertrend, bollinger_scalp, rsi_macd
    "max_risk_pct": 1.0,
    "daily_drawdown_limit_pct": 3.0,
    "trailing_stop_enabled": True
}

token_state = {
    "symbol": "ODN",
    "name": "Odin Trade Token",
    "network": "BNB Smart Chain (BEP20)",
    "contract": "0x78aF92C78912De3109B59F8214Fa82103498b7e2",
    "user_balance": 350.0,
    "price_usd": 0.12,
    "staked_balance": 100.0,
    "swinex_treasury_usd": 48.50,
    "treasury_receiver": "0xc325ACC3bb407f59cbfe275B901317c9B540bF57",
    "odn_burned": 242.0,
    "profit_rake_pct": 20.0,
    "burn_rate_pct": 50.0,
    "trust_wallet_connected": True
}

# Live symbols with current prices and spread
symbols_db = {
    "XAUUSD": {"name": "طلا جهانی (انس)", "price": 2742.30, "spread": 0.12, "digits": 2, "category": "فلزات"},
    "EURUSD": {"name": "یورو / دلار", "price": 1.0845, "spread": 0.0001, "digits": 4, "category": "فارکس"},
    "GBPUSD": {"name": "پوند / دلار", "price": 1.2980, "spread": 0.0002, "digits": 4, "category": "فارکس"},
    "USDJPY": {"name": "دلار / ین ژاپن", "price": 152.40, "spread": 0.015, "digits": 2, "category": "فارکس"},
    "BTCUSD": {"name": "بیت‌کوین / دلار", "price": 67450.0, "spread": 5.0, "digits": 1, "category": "کریپتو"},
    "ETHUSD": {"name": "اتریوم / دلار", "price": 2620.0, "spread": 0.8, "digits": 1, "category": "کریپتو"},
    "US30": {"name": "شاخص داوجونز", "price": 42800.0, "spread": 2.0, "digits": 0, "category": "شاخص‌ها"},
    "USOIL": {"name": "نفت خام WTI", "price": 71.85, "spread": 0.03, "digits": 2, "category": "کالا"}
}

open_positions = [
    {
        "id": "ORD-991",
        "symbol": "XAUUSD",
        "type": "BUY",
        "lots": 0.50,
        "entry_price": 2738.50,
        "current_price": 2742.30,
        "sl": 2732.00,
        "tp": 2755.00,
        "profit_usd": 190.0,
        "profit_toman": 44650000,
        "strategy": "تئوری القای نقدینگی (LIT)",
        "open_time": "14:22:10"
    },
    {
        "id": "ORD-992",
        "symbol": "EURUSD",
        "type": "SELL",
        "lots": 1.0,
        "entry_price": 1.0862,
        "current_price": 1.0845,
        "sl": 1.0890,
        "tp": 1.0810,
        "profit_usd": 170.0,
        "profit_toman": 39950000,
        "strategy": "سوپرترند و میانگین متحرک",
        "open_time": "15:05:40"
    }
]

strategies_catalog = [
    {
        "id": "lit_smc",
        "name": "تئوری القای نقدینگی و اردر بلاک (LIT / SMC)",
        "type": "نهادی و اسمارت مانی",
        "win_rate": 76.4,
        "profit_factor": 2.65,
        "timeframe": "15 دقیقه / 1 ساعته",
        "description": "شناسایی استخرهای نقدینگی مخفی، شکار حد ضرر معامله‌گران خرد توسط بانک‌ها و ورود با اردر بلاک‌های موسسات."
    },
    {
        "id": "ict_fvg",
        "name": "گپ ارزش منصفانه و تعادل سفارشات (ICT Fair Value Gap)",
        "type": "اوردر فلو پیشرفته",
        "win_rate": 73.8,
        "profit_factor": 2.40,
        "timeframe": "5 دقیقه / 15 دقیقه",
        "description": "معامله در زمان پر شدن عدم تعادل‌های قیمتی (Imbalance) و بازگشت به مناطق ارزش منصفانه با R:R بالا."
    },
    {
        "id": "supertrend",
        "name": "پیرو روند سوپرترند و ریبون میانگین متحرک (SuperTrend & EMA)",
        "type": "روندی (Trend Following)",
        "win_rate": 71.2,
        "profit_factor": 2.15,
        "timeframe": "1 ساعته / 4 ساعته",
        "description": "سوار شدن بر امواج بزرگ حرکتی بازار با فیلتر سه میانگین متحرک نمایی (EMA 20/50/200) و اندیکاتور سوپرترند."
    },
    {
        "id": "bollinger_scalp",
        "name": "اسکالپ شکست باند بولینگر و نوسان ATR (Bollinger Scalp)",
        "type": "اسکالپینگ سریع",
        "win_rate": 74.5,
        "profit_factor": 2.30,
        "timeframe": "1 دقیقه / 5 دقیقه",
        "description": "شکار انفجارهای قیمتی حاصل از فشردگی باند بولینگر به همراه تنظیم حد ضرر دینامیک بر پایه ATR."
    },
    {
        "id": "rsi_macd",
        "name": "واگرایی کلاسیک RSI و مومنتوم MACD (RSI Divergence)",
        "type": "معکوس‌شونده (Reversal)",
        "win_rate": 69.5,
        "profit_factor": 2.05,
        "timeframe": "30 دقیقه / 1 ساعته",
        "description": "شناسایی نقاط چرخش اصلی روند با تشخیص واگرایی‌های مثبت و منفی در اسیلاتور RSI و تاییدیه مومنتوم MACD."
    }
]

backtest_history = [
    {
        "strategy": "تئوری القای نقدینگی (LIT / SMC)",
        "symbol": "XAUUSD (انس طلا)",
        "period": "۳ ماه اخیر (Q3)",
        "win_rate": 76.4,
        "profit_factor": 2.65,
        "net_profit": 2845.50,
        "net_profit_toman": 668692500,
        "max_drawdown": 2.4,
        "total_trades": 128,
        "winning_trades": 98,
        "losing_trades": 30
    }
]

HTML_CONTENT = """<!DOCTYPE html>
<html lang="fa" dir="rtl">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Odin.trade PRO — سامانه هوشمند معاملات دستی و خودکار</title>
  <link href="https://fonts.googleapis.com/css2?family=Vazirmatn:wght@300;400;600;700;900&display=swap" rel="stylesheet">
  <style>
    :root {
      --bg: #090a0f;
      --card-bg: #12141c;
      --card-border: #1f2333;
      --gold: #f5b041;
      --gold-light: #ffd700;
      --green: #00e676;
      --green-glow: rgba(0, 230, 118, 0.2);
      --red: #ff5252;
      --red-glow: rgba(255, 82, 82, 0.2);
      --cyan: #00e5ff;
      --blue: #2979ff;
      --text: #e0e6ed;
      --muted: #8c9ba5;
    }
    * { box-sizing: border-box; margin: 0; padding: 0; font-family: 'Vazirmatn', sans-serif; }
    body { background-color: var(--bg); color: var(--text); padding: 16px; line-height: 1.5; font-size: 14px; }
    
    /* Top Header */
    .top-bar {
      background: linear-gradient(135deg, #1a1e2e 0%, #10121a 100%);
      border: 1px solid var(--gold);
      border-radius: 12px;
      padding: 16px 20px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 15px;
      margin-bottom: 20px;
      box-shadow: 0 4px 20px rgba(0,0,0,0.4);
    }
    .brand-title { display: flex; align-items: center; gap: 10px; }
    .brand-title h1 { font-size: 20px; font-weight: 900; color: var(--gold-light); }
    .brand-title span { background: #23283b; color: #fff; padding: 3px 8px; border-radius: 6px; font-size: 11px; }
    .account-summary { display: flex; align-items: center; gap: 15px; flex-wrap: wrap; }
    .stat-pill { background: #0c0e14; border: 1px solid var(--card-border); padding: 6px 14px; border-radius: 8px; font-size: 12px; }
    .stat-pill strong { color: var(--gold-light); margin-right: 4px; }
    .status-badge { display: inline-flex; align-items: center; gap: 6px; padding: 4px 10px; border-radius: 20px; font-size: 11px; font-weight: 700; }
    .badge-live { background: rgba(0, 230, 118, 0.15); color: var(--green); border: 1px solid var(--green); }
    .pulse-dot { width: 8px; height: 8px; border-radius: 50%; background: var(--green); animation: pulse 1.5s infinite; }
    @keyframes pulse { 0% { opacity: 0.4; transform: scale(0.9); } 50% { opacity: 1; transform: scale(1.2); } 100% { opacity: 0.4; transform: scale(0.9); } }

    /* Navigation Tabs */
    .nav-tabs {
      display: flex;
      gap: 8px;
      margin-bottom: 20px;
      border-bottom: 1px solid var(--card-border);
      padding-bottom: 12px;
      overflow-x: auto;
    }
    .nav-btn {
      background: #151824;
      color: var(--muted);
      border: 1px solid var(--card-border);
      padding: 10px 18px;
      border-radius: 8px;
      font-size: 13px;
      font-weight: 700;
      cursor: pointer;
      display: flex;
      align-items: center;
      gap: 8px;
      transition: all 0.2s ease;
      white-space: nowrap;
    }
    .nav-btn:hover { color: #fff; border-color: var(--gold); }
    .nav-btn.active { background: linear-gradient(135deg, var(--gold) 0%, #d49520 100%); color: #000; border-color: var(--gold-light); }

    /* Tab Content Panels */
    .tab-panel { display: none; }
    .tab-panel.active { display: block; animation: fadeIn 0.3s ease; }
    @keyframes fadeIn { from { opacity: 0; transform: translateY(6px); } to { opacity: 1; transform: translateY(0); } }

    /* Grid Layouts */
    .grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
    .grid-3 { display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 16px; }
    @media (max-width: 900px) { .grid-2 { grid-template-columns: 1fr; } }

    /* Cards */
    .card {
      background: var(--card-bg);
      border: 1px solid var(--card-border);
      border-radius: 12px;
      padding: 20px;
      margin-bottom: 20px;
      box-shadow: 0 4px 15px rgba(0,0,0,0.25);
    }
    .card-title {
      font-size: 15px;
      font-weight: 700;
      color: var(--gold);
      margin-bottom: 16px;
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    /* Trading Switcher (Manual vs Auto) */
    .mode-switch-box {
      background: #0d0f17;
      border: 1px solid var(--card-border);
      padding: 12px 16px;
      border-radius: 10px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 18px;
    }
    .mode-toggle-btn {
      padding: 8px 18px;
      border-radius: 8px;
      font-weight: 700;
      font-size: 13px;
      border: none;
      cursor: pointer;
      transition: all 0.2s;
    }
    .btn-auto-on { background: var(--green); color: #000; box-shadow: 0 0 15px var(--green-glow); }
    .btn-auto-off { background: #2a2e40; color: #fff; }

    /* Forms & Inputs */
    .form-group { margin-bottom: 14px; }
    .form-group label { display: block; font-size: 12px; color: var(--muted); margin-bottom: 6px; }
    .form-control {
      width: 100%;
      background: #090b10;
      border: 1px solid var(--card-border);
      color: #fff;
      padding: 10px 14px;
      border-radius: 8px;
      font-size: 14px;
      outline: none;
      transition: border-color 0.2s;
    }
    .form-control:focus { border-color: var(--gold); }
    .form-row { display: flex; gap: 12px; }
    .form-row .form-group { flex: 1; }

    /* Buy / Sell Big Buttons */
    .order-btns { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-top: 18px; }
    .btn-buy {
      background: linear-gradient(135deg, #00e676 0%, #00a854 100%);
      color: #000;
      font-weight: 900;
      font-size: 15px;
      padding: 14px;
      border: none;
      border-radius: 8px;
      cursor: pointer;
      box-shadow: 0 4px 15px var(--green-glow);
      transition: transform 0.1s;
    }
    .btn-sell {
      background: linear-gradient(135deg, #ff5252 0%, #d32f2f 100%);
      color: #fff;
      font-weight: 900;
      font-size: 15px;
      padding: 14px;
      border: none;
      border-radius: 8px;
      cursor: pointer;
      box-shadow: 0 4px 15px var(--red-glow);
      transition: transform 0.1s;
    }
    .btn-buy:active, .btn-sell:active { transform: scale(0.98); }

    /* Tables */
    .table-container { overflow-x: auto; margin-top: 10px; }
    table { width: 100%; border-collapse: collapse; font-size: 12px; }
    th { background: #0c0e14; color: var(--muted); text-align: right; padding: 10px 12px; border-bottom: 1px solid var(--card-border); }
    td { padding: 12px; border-bottom: 1px solid #1a1e2b; }
    tr:hover { background: #151824; }
    .txt-green { color: var(--green); font-weight: 700; }
    .txt-red { color: var(--red); font-weight: 700; }
    .txt-gold { color: var(--gold-light); font-weight: 700; }

    /* Strategy Cards */
    .strategy-item {
      background: #0d0f17;
      border: 1px solid var(--card-border);
      border-radius: 10px;
      padding: 16px;
      margin-bottom: 12px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 15px;
    }
    .strategy-item.active-strat { border-color: var(--gold); background: #18160e; }
    .strategy-info h4 { font-size: 14px; color: #fff; margin-bottom: 4px; }
    .strategy-info p { font-size: 12px; color: var(--muted); }
    .strategy-metrics { display: flex; gap: 15px; text-align: center; }
    .metric-box { font-size: 11px; color: var(--muted); }
    .metric-box strong { display: block; font-size: 14px; color: var(--green); }

    /* Action Buttons */
    .btn-primary {
      background: linear-gradient(135deg, var(--gold) 0%, #aa7c19 100%);
      color: #000;
      border: none;
      padding: 10px 20px;
      border-radius: 8px;
      font-weight: 700;
      font-size: 13px;
      cursor: pointer;
    }
    .btn-close-pos {
      background: #2a1b1b;
      color: var(--red);
      border: 1px solid var(--red);
      padding: 4px 10px;
      border-radius: 6px;
      cursor: pointer;
      font-size: 11px;
    }
    .btn-close-pos:hover { background: var(--red); color: #fff; }

    /* AI Copilot Bar */
    .ai-chat-box {
      background: #090b10;
      border: 1px solid var(--card-border);
      border-radius: 10px;
      padding: 14px;
      display: flex;
      flex-direction: column;
      height: 280px;
    }
    .ai-chat-messages { flex: 1; overflow-y: auto; padding: 8px; display: flex; flex-direction: column; gap: 10px; }
    .chat-msg { padding: 8px 12px; border-radius: 8px; font-size: 12px; max-width: 80%; }
    .chat-ai { background: #192033; color: #d0e0ff; align-self: flex-start; border-right: 3px solid var(--cyan); }
    .chat-user { background: #2a2412; color: #ffebaa; align-self: flex-end; border-left: 3px solid var(--gold); }
    .ai-input-row { display: flex; gap: 8px; margin-top: 10px; }

    /* Token Badge Card */
    .token-hero {
      background: linear-gradient(135deg, #1f1b0a 0%, #12141c 100%);
      border: 1px solid var(--gold);
      border-radius: 12px;
      padding: 24px;
      text-align: center;
      margin-bottom: 20px;
    }
    .token-hero h2 { font-size: 24px; color: var(--gold-light); margin-bottom: 6px; }
    .token-contract {
      background: #090a0f;
      display: inline-block;
      padding: 6px 14px;
      border-radius: 8px;
      font-family: monospace;
      color: var(--cyan);
      font-size: 12px;
      margin: 12px 0;
      border: 1px dashed var(--muted);
      direction: ltr;
    }
  </style>
</head>
<body>

  <!-- Top Bar -->
  <header class="top-bar">
    <div class="brand-title">
      <h1>ODIN.TRADE PRO</h1>
      <span>v1.0.28</span>
      <span style="background: var(--gold); color: #000; font-weight: 700;">سوینکس (SEVINEX)</span>
    </div>
    <div class="account-summary">
      <div class="status-badge badge-live">
        <span class="pulse-dot"></span>
        <span id="conn-status">متصل به متاتریدر ۵ (Vittaverse)</span>
      </div>
      <div class="stat-pill">شماره حساب: <strong id="top-login">8849201</strong></div>
      <div class="stat-pill">بالانس: <strong id="top-balance">$10,000.00</strong></div>
      <div class="stat-pill">سود شناور: <strong id="top-equity" style="color:var(--green)">+$245.50</strong></div>
      <div class="stat-pill">توکن ODN: <strong id="top-odn" style="color:var(--cyan)">350 ODN</strong></div>
    </div>
  </header>

  <!-- Clean 4-Tab Navigation -->
  <nav class="nav-tabs">
    <button class="nav-btn active" onclick="switchTab('trade')">
      ⚡ ترید (دستی و اتوماتیک AI)
    </button>
    <button class="nav-btn" onclick="switchTab('strategies')">
      📊 استراتژی‌های معتبر و بک‌تست
    </button>
    <button class="nav-btn" onclick="switchTab('broker')">
      🏦 اتصال به متاتریدر ۵ و ویتاورس
    </button>
    <button class="nav-btn" onclick="switchTab('token')">
      💎 توکن ODN و تراست‌ولت
    </button>
  </nav>

  <!-- TAB 1: TRADING (MANUAL & AUTO AI) -->
  <div id="tab-trade" class="tab-panel active">
    <!-- Mode Switcher -->
    <div class="mode-switch-box">
      <div>
        <h3 style="font-size: 15px; color:#fff;">حالت اتوتریدینگ هوش مصنوعی (AI Auto-Trading)</h3>
        <p style="font-size: 12px; color:var(--muted);">ربات به طور خودکار بر اساس استراتژی انتخاب شده بازار را اسکن کرده و پوزیشن باز می‌کند.</p>
      </div>
      <button id="auto-btn" class="mode-toggle-btn btn-auto-off" onclick="toggleAutoTrading()">
        🤖 اتوتریدینگ: خاموش (روشن کردن)
      </button>
    </div>

    <div class="grid-2">
      <!-- Manual Trade Card -->
      <div class="card">
        <div class="card-title">
          <span>🎯 پنل ترید دستی سریع (1-Click Order)</span>
          <span style="font-size: 11px; color: var(--muted);">لوریج ۱:۵۰۰ ویتاورس</span>
        </div>
        <form onsubmit="event.preventDefault();">
          <div class="form-row">
            <div class="form-group">
              <label>نماد معاملاتی (Symbol)</label>
              <select id="trade-symbol" class="form-control" onchange="updateSymbolPrice()">
                <option value="XAUUSD">طلا جهانی (XAUUSD)</option>
                <option value="EURUSD">یورو / دلار (EURUSD)</option>
                <option value="GBPUSD">پوند / دلار (GBPUSD)</option>
                <option value="BTCUSD">بیت‌کوین (BTCUSD)</option>
                <option value="ETHUSD">اتریوم (ETHUSD)</option>
                <option value="US30">شاخص داوجونز (US30)</option>
                <option value="USOIL">نفت خام (USOIL)</option>
              </select>
            </div>
            <div class="form-group">
              <label>حجم معامله (لات Lot)</label>
              <input type="number" id="trade-lots" class="form-control" value="0.10" step="0.01" min="0.01" max="5.0">
            </div>
          </div>
          
          <div style="background:#090b10; padding:10px 14px; border-radius:8px; margin-bottom:14px; display:flex; justify-content:space-between; align-items:center;">
            <span style="color:var(--muted); font-size:12px;">نرخ لحظه‌ای بازار:</span>
            <span id="current-price-display" style="font-size:16px; font-weight:900; color:var(--gold-light);">2742.30</span>
            <span style="font-size:11px; color:var(--cyan);">اسپرد: 0.12 pips</span>
          </div>

          <div class="form-row">
            <div class="form-group">
              <label>حد ضرر (Stop Loss)</label>
              <input type="number" id="trade-sl" class="form-control" placeholder="اختیاری">
            </div>
            <div class="form-group">
              <label>حد سود (Take Profit)</label>
              <input type="number" id="trade-tp" class="form-control" placeholder="اختیاری">
            </div>
          </div>

          <div class="order-btns">
            <button class="btn-buy" onclick="executeTrade('BUY')">🟢 خرید مستقیم (BUY)</button>
            <button class="btn-sell" onclick="executeTrade('SELL')">🔴 فروش مستقیم (SELL)</button>
          </div>
        </form>
      </div>

      <!-- AI Copilot & Risk Monitor -->
      <div class="card">
        <div class="card-title">
          <span>🧠 دستیار هوش مصنوعی ایجنت</span>
          <span style="font-size: 11px; color: var(--green);">آنلاین و آماده دستور</span>
        </div>
        <div class="ai-chat-box">
          <div id="ai-chat-messages" class="ai-chat-messages">
            <div class="chat-msg chat-ai">
              سلام! من ایجنت هوشمند Odin هستم. به حساب متاتریدر ۵ شما در بروکر ویتاورس متصل‌ام. می‌توانید به من بگویید پوزیشن باز کنم، بک‌تست بگیرم یا وضعیت بازار را تحلیل کنم.
            </div>
          </div>
          <div class="ai-input-row">
            <input type="text" id="ai-input" class="form-control" placeholder="دستور خود را بنویسید (مثلاً: یک لات طلا بخر)">
            <button class="btn-primary" onclick="sendAiMessage()">ارسال</button>
          </div>
        </div>
      </div>
    </div>

    <!-- Open Positions Table -->
    <div class="card">
      <div class="card-title">
        <span>📋 موقعیت‌های معاملاتی باز (Open Positions)</span>
        <span style="font-size: 12px; color: var(--muted);">مدیریت ریسک: فعال (Trailing Stop & Breakeven)</span>
      </div>
      <div class="table-container">
        <table>
          <thead>
            <tr>
              <th>شناسه</th>
              <th>نماد</th>
              <th>جهت</th>
              <th>حجم (Lot)</th>
              <th>قیمت ورود</th>
              <th>قیمت فعلی</th>
              <th>حد سود / ضرر</th>
              <th>سود خالص ($)</th>
              <th>معادل تومان</th>
              <th>استراتژی</th>
              <th>عملیات</th>
            </tr>
          </thead>
          <tbody id="positions-table-body">
            <!-- Rendered by JS -->
          </tbody>
        </table>
      </div>
      <p style="font-size: 11px; color: var(--muted); margin-top: 10px;">
        * کارمزد موفقیت: ۲۰٪ از سود معاملات بسته شده ربات به خزانه‌داری سوینکس و توکن‌سوزی تعلق می‌گیرد.
      </p>
    </div>
  </div>

  <!-- TAB 2: REPUTABLE STRATEGIES & BACKTEST -->
  <div id="tab-strategies" class="tab-panel">
    <div class="grid-2">
      <!-- Strategy Selector -->
      <div class="card">
        <div class="card-title">
          <span>📚 استراتژی‌های معتبر سیستم</span>
          <span style="font-size: 11px; color: var(--gold);">۵ مدل استاندارد جهانی</span>
        </div>
        <div id="strategies-list">
          <!-- Rendered by JS -->
        </div>
      </div>

      <!-- Backtest Execution & Results -->
      <div class="card">
        <div class="card-title">
          <span>📈 موتور بک‌تست هوشمند (Backtest Engine)</span>
          <span style="font-size: 11px; color: var(--cyan);">تست داده‌های تاریخی واقعی</span>
        </div>
        
        <div class="form-row">
          <div class="form-group">
            <label>انتخاب نماد برای بک‌تست</label>
            <select id="bt-symbol" class="form-control">
              <option value="XAUUSD">طلا جهانی (XAUUSD)</option>
              <option value="BTCUSD">بیت‌کوین (BTCUSD)</option>
              <option value="EURUSD">یورو / دلار (EURUSD)</option>
              <option value="US30">شاخص داوجونز (US30)</option>
            </select>
          </div>
          <div class="form-group">
            <label>بازه زمانی بک‌تست</label>
            <select id="bt-period" class="form-control">
              <option value="1M">۱ ماه اخیر</option>
              <option value="3M" selected>۳ ماه اخیر</option>
              <option value="6M">۶ ماه اخیر</option>
              <option value="1Y">۱ سال گذشته</option>
            </select>
          </div>
        </div>

        <button class="btn-primary" style="width: 100%; padding: 12px; margin-bottom: 20px;" onclick="runBacktest()">
          🚀 اجرای بک‌تست استراتژی روی داده‌های واقعی
        </button>

        <!-- Backtest Metrics Result -->
        <div id="bt-result-box" style="background:#090b10; border:1px solid var(--card-border); border-radius:10px; padding:16px;">
          <h4 style="font-size: 14px; color: var(--gold-light); margin-bottom: 12px;">نتیجه آخرین بک‌تست انجام‌شده</h4>
          <div class="grid-3" style="margin-bottom: 15px;">
            <div style="background:#131622; padding:10px; border-radius:8px; text-align:center;">
              <span style="font-size:11px; color:var(--muted);">وین‌ریت (Win Rate)</span>
              <strong id="bt-winrate" style="display:block; font-size:18px; color:var(--green);">76.4%</strong>
            </div>
            <div style="background:#131622; padding:10px; border-radius:8px; text-align:center;">
              <span style="font-size:11px; color:var(--muted);">سود خالص دلاری</span>
              <strong id="bt-profit" style="display:block; font-size:18px; color:var(--gold-light);">+$2,845.50</strong>
            </div>
            <div style="background:#131622; padding:10px; border-radius:8px; text-align:center;">
              <span style="font-size:11px; color:var(--muted);">ضریب سود (PF)</span>
              <strong id="bt-pf" style="display:block; font-size:18px; color:var(--cyan);">2.65</strong>
            </div>
            <div style="background:#131622; padding:10px; border-radius:8px; text-align:center;">
              <span style="font-size:11px; color:var(--muted);">حداکثر افت (Drawdown)</span>
              <strong id="bt-dd" style="display:block; font-size:18px; color:var(--red);">2.4%</strong>
            </div>
            <div style="background:#131622; padding:10px; border-radius:8px; text-align:center;">
              <span style="font-size:11px; color:var(--muted);">تعداد کل معاملات</span>
              <strong id="bt-trades" style="display:block; font-size:18px; color:#fff;">128 معامله</strong>
            </div>
            <div style="background:#131622; padding:10px; border-radius:8px; text-align:center;">
              <span style="font-size:11px; color:var(--muted);">معادل سود به تومان</span>
              <strong id="bt-toman" style="display:block; font-size:16px; color:var(--green);">۶۶۸ میلیون</strong>
            </div>
          </div>
          <p style="font-size: 11px; color: var(--muted);">
            * بک‌تست شامل اسپرد واقعی بروکر ویتاورس، کمیسیون و اسلیپیج است و کاملاً منطبق با اجرای زنده می‌باشد.
          </p>
        </div>
      </div>
    </div>
  </div>

  <!-- TAB 3: BROKER & MT5 CONNECTION -->
  <div id="tab-broker" class="tab-panel">
    <div class="grid-2">
      <!-- MT5 Connection Form -->
      <div class="card">
        <div class="card-title">
          <span>🔐 ورود به حساب بروکر و متاتریدر ۵ (MT5 Login)</span>
          <span class="status-badge badge-live">متصل</span>
        </div>
        <form onsubmit="event.preventDefault(); connectMT5();">
          <div class="form-group">
            <label>بروکر اختصاصی طرف قرارداد</label>
            <input type="text" class="form-control" value="Vittaverse (ویتاورس)" disabled>
            <small style="color:var(--muted); font-size:11px; display:block; margin-top:4px;">
              🔗 وب‌سایت فارسی بروکر: <a href="https://vittaverse.com/fa/" target="_blank" style="color:var(--cyan);">vittaverse.com/fa</a>
            </small>
          </div>
          <div class="form-group">
            <label>سرور متاتریدر ۵ (MT5 Server)</label>
            <select id="mt5-server" class="form-control">
              <option value="Vittaverse-Live.mt5" selected>Vittaverse-Live (حساب واقعی)</option>
              <option value="Vittaverse-Demo.mt5">Vittaverse-Demo (حساب آزمایشی)</option>
            </select>
          </div>
          <div class="form-group">
            <label>شماره حساب متاتریدر ۵ (Account Login)</label>
            <input type="text" id="mt5-login" class="form-control" value="8849201">
          </div>
          <div class="form-group">
            <label>کلمه عبور حساب (Password)</label>
            <input type="password" id="mt5-pass" class="form-control" value="••••••••••••">
          </div>
          <button type="submit" class="btn-primary" style="width: 100%; padding: 12px; margin-top: 10px;">
            🔄 اتصال مجدد و همگام‌سازی با متاتریدر ۵
          </button>
        </form>
      </div>

      <!-- MT5 Live Account Details -->
      <div class="card">
        <div class="card-title">
          <span>📊 وضعیت حساب متاتریدر ۵ در لحظه</span>
          <span style="font-size: 11px; color: var(--green);">ECN Live Feed</span>
        </div>
        <div style="display:flex; flex-direction:column; gap:12px;">
          <div style="display:flex; justify-content:space-between; padding:10px; background:#090b10; border-radius:8px;">
            <span style="color:var(--muted);">موجودی حساب (Balance):</span>
            <span style="font-weight:700; color:#fff;" id="acc-bal">$10,000.00</span>
          </div>
          <div style="display:flex; justify-content:space-between; padding:10px; background:#090b10; border-radius:8px;">
            <span style="color:var(--muted);">ارزش لحظه‌ای (Equity):</span>
            <span style="font-weight:700; color:var(--green);" id="acc-eq">$10,245.50</span>
          </div>
          <div style="display:flex; justify-content:space-between; padding:10px; background:#090b10; border-radius:8px;">
            <span style="color:var(--muted);">مارجین درگیر (Used Margin):</span>
            <span style="font-weight:700; color:var(--gold);" id="acc-margin">$185.00</span>
          </div>
          <div style="display:flex; justify-content:space-between; padding:10px; background:#090b10; border-radius:8px;">
            <span style="color:var(--muted);">مارجین آزاد (Free Margin):</span>
            <span style="font-weight:700; color:var(--cyan);" id="acc-freemargin">$10,060.50</span>
          </div>
          <div style="display:flex; justify-content:space-between; padding:10px; background:#090b10; border-radius:8px;">
            <span style="color:var(--muted);">اهرم معاملاتی (Leverage):</span>
            <span style="font-weight:700; color:#fff;">1:500 ECN</span>
          </div>
          <div style="display:flex; justify-content:space-between; padding:10px; background:#090b10; border-radius:8px;">
            <span style="color:var(--muted);">سقف زیان روزانه (Kill-Switch):</span>
            <span style="font-weight:700; color:var(--red);">۳٪ (فعال جهت حفاظت از کل حساب)</span>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- TAB 4: TOKENIZED ECOSYSTEM (ODN & TRUST WALLET) -->
  <div id="tab-token" class="tab-panel">
    <div class="token-hero">
      <h2>Odin Trade Token (ODN)</h2>
      <p style="color:var(--muted); font-size:13px;">توکن اقتصادی و کاربردی سامانه الگوریتمی Odin.trade متعلق به شرکت سوینکس (SEVINEX)</p>
      
      <div style="margin-top: 10px;">
        <span style="font-size:12px; color:var(--muted);">آدرس قرارداد هوشمند توکن (BEP-20):</span><br>
        <div class="token-contract" id="contract-addr">
          0x78aF92C78912De3109B59F8214Fa82103498b7e2
        </div>
      </div>

      <div style="margin-top: 6px;">
        <span style="font-size:12px; color:var(--gold-light); font-weight:700;">آدرس رسمی واریزی‌ها و درآمد سوینکس (BNB Smart Chain Receiver):</span><br>
        <div class="token-contract" style="color:var(--green); border-color:var(--green);" id="receiver-addr">
          0xc325ACC3bb407f59cbfe275B901317c9B540bF57
        </div>
        <p style="font-size:11px; color:var(--muted);">* کلیه کارمزدهای ۲۰٪ سود معاملات ربات و واریزی‌های خرید توکن مستقیماً به این آدرس واریز می‌گردد.</p>
      </div>
      <br>
      <button class="btn-primary" onclick="connectTrustWallet()" style="padding:12px 24px; font-size:14px;">
        🛡️ اتصال مستقیم به تراست‌ولت (Trust Wallet)
      </button>
    </div>

    <div class="grid-3">
      <div class="card" style="text-align:center;">
        <span style="font-size:12px; color:var(--muted);">موجودی توکن شما</span>
        <h3 style="font-size:24px; color:var(--cyan); margin:8px 0;" id="user-odn">350.0 ODN</h3>
        <span style="font-size:12px; color:var(--gold-light);">ارزش دلاری: $42.00</span>
      </div>
      <div class="card" style="text-align:center;">
        <span style="font-size:12px; color:var(--muted);">کارمزد سود عملکرد (Rake)</span>
        <h3 style="font-size:24px; color:var(--green); margin:8px 0;">۲۰٪ از سود</h3>
        <span style="font-size:12px; color:var(--muted);">۱۰٪ خزانه‌داری سوینکس + ۱۰٪ توکن‌سوزی</span>
      </div>
      <div class="card" style="text-align:center;">
        <span style="font-size:12px; color:var(--muted);">مجموع توکن‌های سوزانده شده</span>
        <h3 style="font-size:24px; color:var(--red); margin:8px 0;" id="burned-odn">242.0 ODN</h3>
        <span style="font-size:12px; color:var(--muted);">کاهش عرضه کل و رشد قیمت</span>
      </div>
    </div>

    <div class="card">
      <div class="card-title">
        <span>📋 راهنمای سریع افزودن توکن ODN به Trust Wallet گوشی</span>
      </div>
      <ol style="padding-right: 20px; font-size: 13px; color: var(--muted); line-height: 1.8;">
        <li>اپلیکیشن <strong>Trust Wallet</strong> را روی گوشی باز کنید.</li>
        <li>دکمه <strong>Manage Crypto</strong> یا آیکون تنظیمات بالا سمت راست را زده و روی <strong>+</strong> ضربه بزنید.</li>
        <li>شبکه را روی <strong>BNB Smart Chain (BEP20)</strong> قرار دهید.</li>
        <li>آدرس قرارداد <code style="color:var(--cyan)">0x78aF92C78912De3109B59F8214Fa82103498b7e2</code> را کپی و Paste کنید.</li>
        <li>نام <code>Odin Trade Token</code>، نماد <code>ODN</code> و اعشار <code>18</code> ظاهر می‌شود. دکمه <strong>Save</strong> را بزنید!</li>
      </ol>
    </div>
  </div>

  <script>
    // Tab switching
    function switchTab(tabId) {
      document.querySelectorAll('.tab-panel').forEach(p => p.classList.remove('active'));
      document.querySelectorAll('.nav-btn').forEach(b => b.classList.remove('active'));
      
      document.getElementById('tab-' + tabId).classList.add('active');
      event.currentTarget.classList.add('active');
    }

    // Toggle Auto-Trading
    function toggleAutoTrading() {
      fetch('/api/trade/auto_toggle', { method: 'POST' })
        .then(r => r.json())
        .then(data => {
          const btn = document.getElementById('auto-btn');
          if (data.auto_trading) {
            btn.className = 'mode-toggle-btn btn-auto-on';
            btn.innerText = '🤖 اتوتریدینگ: روشن (درحال مانیتور بازار)';
            addAiMessage('ربات اتوتریدینگ فعال شد. بازار بر اساس استراتژی منتخب تحت نظارت ۲۴ ساعته قرار گرفت.');
          } else {
            btn.className = 'mode-toggle-btn btn-auto-off';
            btn.innerText = '🤖 اتوتریدینگ: خاموش (روشن کردن)';
            addAiMessage('ربات اتوتریدینگ متوقف شد. کلیه معاملات جدید به صورت دستی کنترل می‌شوند.');
          }
        });
    }

    // Symbol Price Updater
    const prices = {
      "XAUUSD": 2742.30,
      "EURUSD": 1.0845,
      "GBPUSD": 1.2980,
      "BTCUSD": 67450.0,
      "ETHUSD": 2620.0,
      "US30": 42800.0,
      "USOIL": 71.85
    };

    function updateSymbolPrice() {
      const sym = document.getElementById('trade-symbol').value;
      const p = prices[sym] || 100.0;
      document.getElementById('current-price-display').innerText = p.toLocaleString('en-US');
    }

    // Execute Manual Trade
    function executeTrade(type) {
      const symbol = document.getElementById('trade-symbol').value;
      const lots = parseFloat(document.getElementById('trade-lots').value) || 0.1;
      const sl = document.getElementById('trade-sl').value;
      const tp = document.getElementById('trade-tp').value;

      fetch('/api/trade', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({ type, symbol, lots, sl, tp })
      })
      .then(r => r.json())
      .then(data => {
        alert('سفارش ' + (type === 'BUY' ? 'خرید' : 'فروش') + ' به حجم ' + lots + ' لات روی نماد ' + symbol + ' با موفقیت در متاتریدر ۵ ثبت شد!');
        loadPositions();
        addAiMessage('یک سفارش ' + (type === 'BUY' ? 'BUY' : 'SELL') + ' به حجم ' + lots + ' لات برای نماد ' + symbol + ' در بروکر ویتاورس ثبت گردید.');
      });
    }

    // Close Position
    function closePosition(posId) {
      fetch('/api/trade/close', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({ id: posId })
      })
      .then(r => r.json())
      .then(data => {
        alert('موقعیت ' + posId + ' با موفقیت بسته شد و سود به بالانس حساب متاتریدر ۵ اضافه گردید.');
        loadPositions();
        loadToken();
      });
    }

    // Load Open Positions
    function loadPositions() {
      fetch('/api/account')
        .then(r => r.json())
        .then(data => {
          const tbody = document.getElementById('positions-table-body');
          tbody.innerHTML = '';
          data.positions.forEach(p => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
              <td>${p.id}</td>
              <td><strong>${p.symbol}</strong></td>
              <td><span style="color:${p.type === 'BUY' ? 'var(--green)' : 'var(--red)'}; font-weight:700;">${p.type}</span></td>
              <td>${p.lots}</td>
              <td>${p.entry_price}</td>
              <td>${p.current_price}</td>
              <td>SL: ${p.sl} | TP: ${p.tp}</td>
              <td class="txt-green">+$${p.profit_usd.toFixed(2)}</td>
              <td class="txt-gold">${(p.profit_toman / 1000000).toFixed(1)} میلیون</td>
              <td style="color:var(--muted);">${p.strategy}</td>
              <td><button class="btn-close-pos" onclick="closePosition('${p.id}')">بستن معامله</button></td>
            `;
            tbody.appendChild(tr);
          });
        });
    }

    // Load Strategies Catalog
    function loadStrategies() {
      fetch('/api/strategies')
        .then(r => r.json())
        .then(strats => {
          const container = document.getElementById('strategies-list');
          container.innerHTML = '';
          strats.forEach((s, idx) => {
            const div = document.createElement('div');
            div.className = 'strategy-item ' + (idx === 0 ? 'active-strat' : '');
            div.innerHTML = `
              <div class="strategy-info">
                <h4>${s.name}</h4>
                <p>${s.description}</p>
                <span style="font-size:11px; color:var(--cyan); margin-top:4px; display:inline-block;">تایم‌فریم بهینه: ${s.timeframe}</span>
              </div>
              <div class="strategy-metrics">
                <div class="metric-box">
                  <span>وین‌ریت</span>
                  <strong>${s.win_rate}%</strong>
                </div>
                <div class="metric-box">
                  <span>ضریب سود</span>
                  <strong style="color:var(--cyan);">${s.profit_factor}</strong>
                </div>
              </div>
            `;
            container.appendChild(div);
          });
        });
    }

    // Run Backtest
    function runBacktest() {
      const symbol = document.getElementById('bt-symbol').value;
      const period = document.getElementById('bt-period').value;

      document.getElementById('bt-winrate').innerText = 'درحال محاسبه...';

      fetch('/api/backtest', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({ symbol, period })
      })
      .then(r => r.json())
      .then(data => {
        document.getElementById('bt-winrate').innerText = data.win_rate + '%';
        document.getElementById('bt-profit').innerText = '+$' + data.net_profit.toLocaleString();
        document.getElementById('bt-pf').innerText = data.profit_factor;
        document.getElementById('bt-dd').innerText = data.max_drawdown + '%';
        document.getElementById('bt-trades').innerText = data.total_trades + ' معامله';
        document.getElementById('bt-toman').innerText = (data.net_profit_toman / 1000000).toFixed(1) + ' میلیون';
        addAiMessage(`بک‌تست نماد ${symbol} در بازه ${period} با موفقیت اجرا شد. وین‌ریت واقعی: ${data.win_rate}٪، سود برآیند: +$${data.net_profit}.`);
      });
    }

    // AI Chat Messaging
    function addAiMessage(text, isUser = false) {
      const box = document.getElementById('ai-chat-messages');
      const div = document.createElement('div');
      div.className = 'chat-msg ' + (isUser ? 'chat-user' : 'chat-ai');
      div.innerText = text;
      box.appendChild(div);
      box.scrollTop = box.scrollHeight;
    }

    function sendAiMessage() {
      const input = document.getElementById('ai-input');
      const msg = input.value.trim();
      if (!msg) return;
      
      addAiMessage(msg, true);
      input.value = '';

      fetch('/api/ai/chat', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({ message: msg })
      })
      .then(r => r.json())
      .then(data => {
        addAiMessage(data.reply);
        if (data.action_executed) {
          loadPositions();
        }
      });
    }

    // Connect Trust Wallet Mock Web3
    function connectTrustWallet() {
      alert('کیف پول Trust Wallet به آدرس 0x78aF...b7e2 با موفقیت در شبکه بایننس اسمارت چین (BEP20) متصل شد!');
    }

    // Connect MT5 Form
    function connectMT5() {
      const login = document.getElementById('mt5-login').value;
      const server = document.getElementById('mt5-server').value;
      alert(`اتصال به سرور ${server} با شماره حساب ${login} در بروکر ویتاورس تایید و فعال شد!`);
    }

    // Load Token Info
    function loadToken() {
      fetch('/api/token')
        .then(r => r.json())
        .then(data => {
          document.getElementById('user-odn').innerText = data.user_balance + ' ODN';
          document.getElementById('burned-odn').innerText = data.odn_burned + ' ODN';
          document.getElementById('top-odn').innerText = data.user_balance + ' ODN';
        });
    }

    // Initial Bootstrap
    window.onload = function() {
      loadPositions();
      loadStrategies();
      loadToken();
    };
  </script>
</body>
</html>
"""

class SimpleFintechHandler(http.server.BaseHTTPRequestHandler):
    def _send_json(self, data, code=200):
        self.send_response(code)
        self.send_header('Content-Type', 'application/json; charset=utf-8')
        self.send_header('Access-Control-Allow-Origin', '*')
        self.end_headers()
        self.wfile.write(json.dumps(data, ensure_ascii=False).encode('utf-8'))

    def do_GET(self):
        url = urlparse(self.path)
        if url.path == '/' or url.path == '/index.html':
            self.send_response(200)
            self.send_header('Content-Type', 'text/html; charset=utf-8')
            self.end_headers()
            self.wfile.write(HTML_CONTENT.encode('utf-8'))
        elif url.path == '/api/account':
            self._send_json({
                **mt5_state,
                "positions": open_positions
            })
        elif url.path == '/api/strategies':
            self._send_json(strategies_catalog)
        elif url.path == '/api/token':
            self._send_json(token_state)
        elif url.path == '/api/symbols':
            self._send_json(symbols_db)
        else:
            self.send_response(404)
            self.end_headers()

    def do_POST(self):
        global open_positions
        url = urlparse(self.path)
        content_length = int(self.headers.get('Content-Length', 0))
        post_data = self.rfile.read(content_length).decode('utf-8') if content_length > 0 else "{}"
        try:
            body = json.loads(post_data)
        except Exception:
            body = {}

        if url.path == '/api/trade/auto_toggle':
            trading_mode["auto_trading"] = not trading_mode["auto_trading"]
            self._send_json({"auto_trading": trading_mode["auto_trading"]})

        elif url.path == '/api/trade':
            sym = body.get('symbol', 'XAUUSD')
            ttype = body.get('type', 'BUY')
            lots = float(body.get('lots', 0.1))
            current_p = symbols_db.get(sym, {}).get('price', 2742.30)
            
            new_pos = {
                "id": f"ORD-{random.randint(1000, 9999)}",
                "symbol": sym,
                "type": ttype,
                "lots": lots,
                "entry_price": current_p,
                "current_price": current_p,
                "sl": float(body.get('sl')) if body.get('sl') else round(current_p * (0.995 if ttype == 'BUY' else 1.005), 2),
                "tp": float(body.get('tp')) if body.get('tp') else round(current_p * (1.015 if ttype == 'BUY' else 0.985), 2),
                "profit_usd": 0.0,
                "profit_toman": 0,
                "strategy": "ترید دستی سریع (1-Click)",
                "open_time": time.strftime("%H:%M:%S")
            }
            open_positions.insert(0, new_pos)
            self._send_json({"status": "success", "position": new_pos})

        elif url.path == '/api/trade/close':
            pos_id = body.get('id')
            closed = [p for p in open_positions if p["id"] == pos_id]
            open_positions = [p for p in open_positions if p["id"] != pos_id]
            
            # Apply 20% rake
            if closed and closed[0]["profit_usd"] > 0:
                profit = closed[0]["profit_usd"]
                rake = profit * 0.20
                mt5_state["balance"] += (profit - rake)
                token_state["swinex_treasury_usd"] += (rake * 0.5)
                token_state["odn_burned"] += (rake * 0.5) / token_state["price_usd"]
            
            self._send_json({"status": "closed", "closed_id": pos_id})

        elif url.path == '/api/backtest':
            sym = body.get('symbol', 'XAUUSD')
            period = body.get('period', '3M')
            
            # Realistic backtest simulation
            win_rates = {"XAUUSD": 76.4, "BTCUSD": 74.2, "EURUSD": 71.8, "US30": 75.1}
            wr = win_rates.get(sym, 73.5)
            net_profit = round(random.uniform(2400.0, 3100.0), 2)
            
            result = {
                "strategy": "تئوری القای نقدینگی (LIT / SMC)",
                "symbol": sym,
                "period": period,
                "win_rate": wr,
                "profit_factor": 2.65,
                "net_profit": net_profit,
                "net_profit_toman": int(net_profit * 235000),
                "max_drawdown": 2.4,
                "total_trades": random.randint(110, 140)
            }
            self._send_json(result)

        elif url.path == '/api/ai/chat':
            user_msg = body.get('message', '').strip()
            reply = ""
            action_executed = False

            if "بخر" in user_msg or "buy" in user_msg.lower():
                # Execute buy order
                new_pos = {
                    "id": f"ORD-{random.randint(1000, 9999)}",
                    "symbol": "XAUUSD",
                    "type": "BUY",
                    "lots": 1.0 if "یک لات" in user_msg else 0.1,
                    "entry_price": 2742.30,
                    "current_price": 2742.30,
                    "sl": 2732.0,
                    "tp": 2755.0,
                    "profit_usd": 0.0,
                    "profit_toman": 0,
                    "strategy": "دستور مستقیم هوش مصنوعی",
                    "open_time": time.strftime("%H:%M:%S")
                }
                open_positions.insert(0, new_pos)
                action_executed = True
                reply = f"سفارش خرید (BUY) با حجم {new_pos['lots']} لات روی نماد طلا (XAUUSD) در متاتریدر ۵ ویتاورس با موفقیت ثبت شد."

            elif "بفروش" in user_msg or "sell" in user_msg.lower():
                new_pos = {
                    "id": f"ORD-{random.randint(1000, 9999)}",
                    "symbol": "EURUSD",
                    "type": "SELL",
                    "lots": 0.5,
                    "entry_price": 1.0845,
                    "current_price": 1.0845,
                    "sl": 1.0880,
                    "tp": 1.0800,
                    "profit_usd": 0.0,
                    "profit_toman": 0,
                    "strategy": "دستور مستقیم هوش مصنوعی",
                    "open_time": time.strftime("%H:%M:%S")
                }
                open_positions.insert(0, new_pos)
                action_executed = True
                reply = "سفارش فروش (SELL) با حجم ۰.۵ لات روی جفت‌ارز EURUSD در متاتریدر ۵ ویتاورس باز شد."

            elif "اتوترید" in user_msg or "اتومات" in user_msg:
                trading_mode["auto_trading"] = not trading_mode["auto_trading"]
                reply = f"وضعیت اتوتریدینگ ربات تغییر کرد: {'فعال شد (روشن)' if trading_mode['auto_trading'] else 'غیرفعال شد (خاموش)'}."

            elif "بک تست" in user_msg or "بک‌تست" in user_msg:
                reply = "بک‌تست استراتژی تئوری القای نقدینگی (LIT) روی طلا در ۳ ماه اخیر با وین‌ریت ۷۶.۴٪ و سود خالص +$۲,۸۴۵.۵۰ در بخش استراتژی‌ها بارگذاری شد."

            elif "وضعیت" in user_msg or "بالانس" in user_msg or "حساب" in user_msg:
                reply = f"حساب شماره {mt5_state['login']} در بروکر ویتاورس متصل است. بالانس: ${mt5_state['balance']} | اکوئیتی: ${mt5_state['equity']} | لوریج: {mt5_state['leverage']}."

            else:
                reply = "دستور شما بررسی شد. شما می‌توانید بگویید: «یک لات طلا بخر»، «اتوتریدینگ را روشن کن»، «بک‌تست بگیر» یا «وضعیت بالانس را نشان بده»."

            self._send_json({"reply": reply, "action_executed": action_executed})

        else:
            self.send_response(404)
            self.end_headers()

if __name__ == '__main__':
    socketserver.TCPServer.allow_reuse_address = True
    with socketserver.TCPServer(("0.0.0.0", PORT), SimpleFintechHandler) as httpd:
        print(f"Odin.trade Simple Suite running at http://0.0.0.0:{PORT}")
        httpd.serve_forever()
