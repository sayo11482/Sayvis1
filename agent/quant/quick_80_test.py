"""
Quick 80% WR Test - Simplified for fast execution
"""

import pandas as pd
import numpy as np
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import os, sys
from datetime import datetime, timedelta

sys.path.append(os.path.dirname(__file__))

from indicators.tradingview_indicators import ConfluenceValidator

def quick_test():
    print("="*90)
    print("🔥 ODIN - تست سریع 80% WR + تمام اندیکاتورهای TradingView + RR 1:2+")
    print("="*90)
    print()
    print("📋 الزامات:")
    print("   - بررسی تمام اندیکاتورهای TradingView (20+ اندیکاتور)")
    print("   - حداقل RR 1:2")
    print("   - تاییدیه‌های معتبر (Confluence >=5)")
    print("   - موفقیت زیر 80% = عدم ورود")
    print()
    
    # Simulate 50 trades history with varying WR
    validator = ConfluenceValidator(min_rr=2.0, min_winrate=80.0, min_confluence=5)
    
    # Simulate building history
    np.random.seed(42)
    # First 20 trades with 70% WR (below threshold)
    for i in range(20):
        is_win = np.random.random() < 0.70
        validator.add_trade_result({
            "pnl": 0.2 if is_win else -0.1,
            "strategy": "TV_80Percent_LIT",
            "rr": 2.0
        })
    
    wr = validator.calculate_historical_winrate("TV_80Percent_LIT")
    print(f"📊 تاریخچه فعلی: {len(validator.history)} ترید, WR {wr:.1f}%")
    print(f"   → {'✅ بالای 80% - مجاز به ترید' if wr>=80 else '❌ زیر 80% - نباید ترید کند'}")
    print()
    
    # Test 5 potential new signals
    print("🧪 تست 5 سیگنال جدید با تاییدیه‌های مختلف:")
    print("-"*90)
    
    test_cases = [
        {"name": "LIT کامل + 7 اندیکاتور", "indicators": {"lit_bull": True, "bos_bull": True, "ob_bull": True, "sweep_bull": True, "ema_bull": True, "rsi_bull": True, "macd_bull": True, "bb_bull": False}, "rr": 2.5, "expected": "بالا"},
        {"name": "فقط EMA + RSI", "indicators": {"ema_bull": True, "rsi_bull": True}, "rr": 2.0, "expected": "کم"},
        {"name": "LIT بدون تاییدیه کافی", "indicators": {"lit_bull": True}, "rr": 2.0, "expected": "کم"},
        {"name": "7 تاییدیه + RR 1:1.5", "indicators": {"ema_bull": True, "sma_bull": True, "rsi_bull": True, "macd_bull": True, "bb_bull": True, "ob_bull": True, "bos_bull": True}, "rr": 1.5, "expected": "RR کم"},
        {"name": "LIT + 8 تاییدیه + RR 1:3", "indicators": {"lit_bull": True, "bos_bull": True, "ob_bull": True, "sweep_bull": True, "fvg_bull": True, "ema_bull": True, "supertrend_bull": True, "vwap_bull": True}, "rr": 3.0, "expected": "عالی"},
    ]
    
    for i, case in enumerate(test_cases, 1):
        entry, sl, tp = 100, 98, 100 + (100-98)*case["rr"]
        result = validator.validate_trade(entry, sl, tp, 1, case["indicators"], "TV_80Percent_LIT")
        
        print(f"\n{i}. {case['name']} - RR 1:{case['rr']}")
        print(f"   تاییدیه‌ها: {result['confluence_score']} - {', '.join(result['confirmations'][:4])}")
        print(f"   RR: {result['rr']:.2f} ({'✅' if result['rr_ok'] else '❌'}), WR تاریخی: {result['winrate']:.1f}% ({'✅' if result['wr_ok'] else '❌'}), Confluence: {result['confluence_score']} ({'✅' if result['confluence_ok'] else '❌'})")
        print(f"   اعتماد: {result['confidence']:.0f}%")
        print(f"   نتیجه: {'✅ ورود مجاز' if result['valid'] else '❌ رد شد - ' + result['reason'][-50:]}")
    
    print("\n" + "="*90)
    print("📊 تحلیل 80% WR - چرا سخت است؟")
    print("="*90)
    print()
    print("فرمول Breakeven:")
    print("   برای RR 1:2 → WR لازم = 1/(1+2) = 33% برای سر به سر")
    print("   برای RR 1:3 → WR لازم = 1/(1+3) = 25%")
    print("   برای WR 80% → RR سر به سر = (1-0.8)/0.8 = 0.25 → RR 1:0.25")
    print()
    print("یعنی:")
    print("   - WR 80% با RR 1:2 = بسیار سودده تئوری (اما نادر)")
    print("   - در بازار واقعی، 80% WR فقط با فیلتر بسیار سختگیرانه ممکن است")
    print("   - نتیجه: معاملات خیلی کم (2-5 در 90 روز) اما با کیفیت بالا")
    print("   - LIT با 7+ تاییدیه می‌تواند به 75-80% برسد")
    print()
    print("💡 پیشنهاد حرفه‌ای:")
    print("   - به جای WR تاریخی 80%، از Confluence Confidence 80% استفاده کن")
    print("   - یعنی اگر 5+ تاییدیه از 20+ اندیکاتور + RR 1:2 → اعتماد 80%+ → ورود")
    print("   - این منطقی‌تر از WR تاریخی است")
    print()
    
    # Now simulate building WR to 80%+
    print("🔄 شبیه‌سازی رسیدن به 80% WR:")
    print("-"*90)
    
    # Add more winning trades to reach 80%
    for i in range(10):
        validator.add_trade_result({"pnl": 0.25, "strategy": "TV_80Percent_LIT", "rr": 2.2})
    
    wr = validator.calculate_historical_winrate("TV_80Percent_LIT")
    print(f"بعد از 10 ترید سودده دیگر: WR {wr:.1f}% از {len(validator.history)} ترید")
    
    # Test again
    test_indicators = {"lit_bull": True, "bos_bull": True, "ob_bull": True, "sweep_bull": True, "ema_bull": True, "rsi_bull": True, "macd_bull": True}
    result = validator.validate_trade(100, 98, 104, 1, test_indicators, "TV_80Percent_LIT")
    print(f"تست مجدد سیگنال LIT کامل: Valid={result['valid']}, WR={result['winrate']:.1f}%, Conf={result['confidence']:.0f}%")
    print(f"   → {'✅ حالا مجاز است چون WR >=80%' if result['valid'] else '❌ هنوز رد'}")
    
    # Generate charts
    os.makedirs("charts", exist_ok=True)
    
    fig, axes = plt.subplots(2, 2, figsize=(15, 10))
    fig.suptitle('ODIN 80% WR + TradingView Indicators + RR 1:2+ - Analysis', fontsize=14, fontweight='bold')
    
    # Chart 1: WR vs RR breakeven
    ax = axes[0, 0]
    wrs = np.arange(10, 95, 5)
    breakeven_rr = [(1-w/100)/(w/100) for w in wrs]
    ax.plot(wrs, breakeven_rr, label='Breakeven RR', color='green', linewidth=2)
    ax.axhline(y=2, color='red', linestyle='--', label='Min RR 1:2')
    ax.axvline(x=80, color='orange', linestyle='--', label='80% WR threshold')
    ax.scatter([60], [2], color='blue', s=100, label='LIT Realistic 60% WR + RR 1:2')
    ax.scatter([80], [2], color='gold', s=150, marker='*', label='Target 80% WR + RR 1:2 (Rare)')
    ax.set_xlabel('Winrate %')
    ax.set_ylabel('RR')
    ax.set_title('WR vs RR - 80% WR with RR 1:2 is very profitable')
    ax.legend()
    ax.grid(True, alpha=0.3)
    
    # Chart 2: Confluence vs WR
    ax = axes[0, 1]
    conf = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
    wr_est = [35, 42, 50, 58, 65, 72, 78, 82, 85, 87]
    ax.plot(conf, wr_est, marker='o', color='#00D4FF', linewidth=2)
    ax.set_xlabel('Confluence Score')
    ax.set_ylabel('Estimated WR%')
    ax.set_title('Confluence vs WR')
    ax.axhline(y=80, color='orange', linestyle='--', label='80% target')
    ax.axvline(x=5, color='red', linestyle=':', label='Min 5 required')
    ax.legend()
    ax.grid(True, alpha=0.3)
    
    # Chart 3: Indicator coverage
    ax = axes[1, 0]
    indicators = ['Trend\n(5)', 'Momentum\n(6)', 'Volatility\n(4)', 'Volume\n(4)', 'SMC/LIT\n(5+)']
    coverage = [5, 6, 4, 4, 6]
    ax.bar(indicators, coverage, color=['#00D4FF', '#F59E0B', '#22C55E', '#EF4444', '#8B5CF6'], alpha=0.7)
    ax.set_title('TradingView Indicators Coverage (20+)')
    ax.set_ylabel('Number of Indicators')
    
    # Chart 4: Trade frequency vs WR
    ax = axes[1, 1]
    wr_levels = ['30% WR\n100 trades', '50% WR\n30 trades', '60% WR\n15 trades', '70% WR\n8 trades', '80% WR\n3 trades']
    freq = [100, 30, 15, 8, 3]
    colors = ['red', 'orange', 'yellow', 'lightgreen', 'green']
    ax.bar(wr_levels, freq, color=colors, alpha=0.7)
    ax.set_title('Trade Frequency vs WR Filter')
    ax.set_ylabel('Trades per 90 days')
    ax.set_xlabel('WR Filter')
    
    plt.tight_layout()
    plt.savefig('charts/tradingview_80_percent.png', dpi=150, bbox_inches='tight')
    plt.close()
    print(f"\n✅ چارت: charts/tradingview_80_percent.png")
    
    print("\n" + "="*90)
    print("✅ تست 80% کامل شد")
    print("="*90)

if __name__ == "__main__":
    quick_test()
