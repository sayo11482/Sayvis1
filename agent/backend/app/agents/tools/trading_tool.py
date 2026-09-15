from .registry import BaseTool
from typing import Dict, Any
from ...models.schemas import RiskLevel
import random

class TradingAnalysisTool(BaseTool):
    name = "trading_analysis"
    description = "Analyze market symbols, generate trading signals, and assess risk. Paper trading by default."
    description_fa = "تحلیل نمادهای بازار، تولید سیگنال معاملاتی و ارزیابی ریسک. به صورت پیش‌فرض کاغذی."
    risk_level = RiskLevel.MEDIUM

    async def execute(self, input_data: Dict[str, Any], context: Dict[str, Any] = None) -> Dict[str, Any]:
        symbol = input_data.get("symbol", "EURUSD").upper()
        timeframe = input_data.get("timeframe", "H1")
        analysis_type = input_data.get("type", "technical")

        # Simulated market analysis (in production, connect to MT4/MT5 bridge)
        base_prices = {
            "EURUSD": 1.0850,
            "GBPUSD": 1.2700,
            "USDJPY": 151.50,
            "XAUUSD": 2580.0,
            "BTCUSD": 65000.0
        }

        base = base_prices.get(symbol, 100.0)
        # Simulate price movement
        change_pct = random.uniform(-1.5, 1.5)
        current = base * (1 + change_pct / 100)

        # Simple technical indicators simulation
        rsi = random.uniform(30, 70)
        signal = "BUY" if rsi < 40 else "SELL" if rsi > 60 else "HOLD"
        
        support = current * 0.995
        resistance = current * 1.005

        is_persian = context.get("language_fa", False) if context else False

        if is_persian:
            analysis_text = f"""
تحلیل {symbol} در تایم‌فریم {timeframe}:
- قیمت فعلی: {current:.5f} ({change_pct:+.2f}%)
- سیگنال: {signal}
- RSI: {rsi:.1f}
- حمایت: {support:.5f}
- مقاومت: {resistance:.5f}
- ریسک: متوسط
- پیشنهاد: {'خرید در اصلاح' if signal == 'BUY' else 'فروش در رشد' if signal == 'SELL' else 'انتظار برای شکست'}
            """.strip()
        else:
            analysis_text = f"""
Analysis for {symbol} on {timeframe}:
- Current: {current:.5f} ({change_pct:+.2f}%)
- Signal: {signal}
- RSI: {rsi:.1f}
- Support: {support:.5f}
- Resistance: {resistance:.5f}
- Risk: Medium
- Suggestion: {'Buy on dip' if signal == 'BUY' else 'Sell on rally' if signal == 'SELL' else 'Wait for breakout'}
            """.strip()

        return {
            "success": True,
            "symbol": symbol,
            "timeframe": timeframe,
            "current_price": current,
            "change_percent": change_pct,
            "signal": signal,
            "rsi": rsi,
            "support": support,
            "resistance": resistance,
            "analysis": analysis_text,
            "risk_level": "MEDIUM",
            "is_paper": True,
            "disclaimer": "This is paper analysis only. Not financial advice."
        }
