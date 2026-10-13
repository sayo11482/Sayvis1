#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import math

def calculate_ema(prices, period):
    if len(prices) < period:
        return [prices[-1]] * len(prices) if prices else []
    multiplier = 2.0 / (period + 1.0)
    ema = [sum(prices[:period]) / period]
    for p in prices[period:]:
        ema.append((p - ema[-1]) * multiplier + ema[-1])
    # Pad beginning
    pad = [ema[0]] * (len(prices) - len(ema))
    return pad + ema

def calculate_rsi(prices, period=14):
    if len(prices) <= period:
        return 50.0
    gains = []
    losses = []
    for i in range(1, len(prices)):
        delta = prices[i] - prices[i-1]
        gains.append(max(0.0, delta))
        losses.append(max(0.0, -delta))
    
    avg_gain = sum(gains[:period]) / period
    avg_loss = sum(losses[:period]) / period

    for i in range(period, len(gains)):
        avg_gain = (avg_gain * (period - 1) + gains[i]) / period
        avg_loss = (avg_loss * (period - 1) + losses[i]) / period

    if avg_loss == 0:
        return 100.0
    rs = avg_gain / avg_loss
    return round(100.0 - (100.0 / (1.0 + rs)), 1)

def calculate_atr(candles, period=14):
    if len(candles) < 2:
        return 1.0
    trs = []
    for i in range(1, len(candles)):
        h = candles[i]['high']
        l = candles[i]['low']
        prev_c = candles[i-1]['close']
        tr = max(h - l, abs(h - prev_c), abs(l - prev_c))
        trs.append(tr)
    if len(trs) < period:
        return round(sum(trs) / len(trs), 2)
    atr = sum(trs[:period]) / period
    for i in range(period, len(trs)):
        atr = (atr * (period - 1) + trs[i]) / period
    return round(atr, 2)

# Test with dummy series
prices = [100.0 + math.sin(i * 0.2) * 5.0 for i in range(50)]
ema20 = calculate_ema(prices, 20)
rsi = calculate_rsi(prices, 14)
print(f"EMA20 length: {len(ema20)}, Latest EMA: {ema20[-1]:.2f}")
print(f"RSI (14): {rsi}")
assert 0 <= rsi <= 100
assert len(ema20) == len(prices)
print("Quant math functions verified successfully!")
