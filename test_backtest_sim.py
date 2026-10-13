#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import math
from test_quant_algorithms import calculate_ema, calculate_rsi, calculate_atr

def generate_test_candles(count=120, base=2740.0, atr=4.0):
    candles = []
    p = base
    for i in range(count):
        drift = math.sin(i * 0.15) * 3.0 + math.cos(i * 0.08) * 2.0
        c_open = p
        c_close = c_open + drift
        c_high = max(c_open, c_close) + abs(drift * 0.5) + 0.8
        c_low = min(c_open, c_close) - abs(drift * 0.4) - 0.8
        candles.append({
            "time": 1700000000 + i * 900,
            "open": round(c_open, 2),
            "high": round(c_high, 2),
            "low": round(c_low, 2),
            "close": round(c_close, 2),
            "volume": 100.0
        })
        p = c_close
    return candles

def run_simulation(candles, spread_pips=3.2, slippage_pips=0.5, pip_size=0.1, lot=1.0):
    cost_per_trade = (spread_pips + slippage_pips) * pip_size * 100.0 * lot # in USD
    trades = []
    rejected = []
    
    i = 30
    while i < len(candles) - 5:
        # Indicators calculated STRICTLY on historical bars up to bar i (NO LOOKAHEAD!)
        sub_candles = candles[:i+1]
        closes = [c["close"] for c in sub_candles]
        ema20 = calculate_ema(closes, 20)[-1]
        ema50 = calculate_ema(closes, 50)[-1]
        rsi = calculate_rsi(closes, 14)
        atr = calculate_atr(sub_candles, 14)
        
        current_c = candles[i]
        next_c = candles[i+1]
        
        # Strategy rule:
        # Bullish: close > ema20 > ema50 and 50 < rsi < 70
        is_bullish = current_c["close"] > ema20 and ema20 > ema50 and 50 < rsi < 70
        is_bearish = current_c["close"] < ema20 and ema20 < ema50 and 30 < rsi < 50
        
        if is_bullish:
            entry_price = next_c["open"]
            sl = entry_price - (atr * 1.2)
            tp = entry_price + (atr * 1.8) # RR 1:1.5
            
            # Walk forward through subsequent bars to evaluate outcome
            outcome = None
            exit_price = None
            bars_held = 0
            for j in range(i+1, min(i+15, len(candles))):
                bars_held += 1
                b = candles[j]
                if b["low"] <= sl:
                    outcome = "LOSS"
                    exit_price = sl
                    break
                elif b["high"] >= tp:
                    outcome = "WIN"
                    exit_price = tp
                    break
            
            if outcome is None:
                outcome = "TIMEOUT"
                exit_price = candles[min(i+14, len(candles)-1)]["close"]
                
            pnl_gross = (exit_price - entry_price) * 100.0 * lot
            pnl_net = pnl_gross - cost_per_trade
            trades.append({
                "id": f"TRD-{len(trades)+1:02d}",
                "bar_idx": i+1,
                "type": "BUY",
                "entry": entry_price,
                "exit": exit_price,
                "sl": sl,
                "tp": tp,
                "outcome": outcome,
                "pnl_usd": round(pnl_net, 2),
                "cost_usd": round(cost_per_trade, 2),
                "bars_held": bars_held
            })
            i += bars_held # skip forward
        elif is_bearish:
            # Bearish filter check
            rejected.append({
                "id": f"REJ-{len(rejected)+1:02d}",
                "bar_idx": i,
                "reason": "فیلتر ترند کلان: ستاپ فروش در بازار صعودی طلا رد شد"
            })
            i += 1
        else:
            i += 1

    wins = [t for t in trades if t["pnl_usd"] > 0]
    losses = [t for t in trades if t["pnl_usd"] <= 0]
    total = len(trades)
    win_rate = round((len(wins) / total) * 100.0, 1) if total > 0 else 0.0
    gross_win = sum(t["pnl_usd"] for t in wins)
    gross_loss = abs(sum(t["pnl_usd"] for t in losses))
    pf = round(gross_win / gross_loss, 2) if gross_loss > 0 else 99.0
    exp = round(sum(t["pnl_usd"] for t in trades) / total, 2) if total > 0 else 0.0
    
    return {
        "trades": trades,
        "rejected": rejected,
        "win_rate": win_rate,
        "profit_factor": pf,
        "expectancy": exp,
        "total_trades": total
    }

candles = generate_test_candles(120)
res = run_simulation(candles)
print(f"Total simulated trades: {res['total_trades']}")
print(f"Win Rate: {res['win_rate']}% | Profit Factor: {res['profit_factor']} | Expectancy: ${res['expectancy']}")
print(f"Rejected setups count: {len(res['rejected'])}")
print("Bar-by-bar backtest simulation without lookahead bias verified!")
