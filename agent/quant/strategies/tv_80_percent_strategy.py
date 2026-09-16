"""
ODIN QUANT - TradingView 80% Winrate Strategy
استراتژی 80% وین ریت با تمام اندیکاتورهای TradingView + R:R 1:2+

Requirements from user:
- Check all TradingView indicators
- Test them
- Minimum R:R 1:2
- Valid confirmations required
- Success rate <80% => NO TRADE

This strategy implements 80% WR filter with confluence validation.
Realistically, 80% WR with RR 1:2 is extremely rare, but achievable with:
- Very strict filtering (only best setups)
- High confluence (4+ indicators)
- LIT + SMC confirmation
- Premium/Discount zones only
- High timeframe trend filter

Expected: Very few trades (2-5 per 90 days) but high quality.
"""

import pandas as pd
import numpy as np
from typing import Dict, Any
import logging
import sys
import os

logger = logging.getLogger(__name__)

try:
    from .base_strategy import BaseStrategy
    from ..indicators.tradingview_indicators import TradingViewIndicators, ConfluenceValidator, apply_all_indicators, get_confluence_signals
except ImportError:
    try:
        from base_strategy import BaseStrategy
        from indicators.tradingview_indicators import TradingViewIndicators, ConfluenceValidator, apply_all_indicators, get_confluence_signals
    except ImportError:
        # Fallback for direct execution
        sys.path.append(os.path.dirname(os.path.dirname(__file__)))
        from strategies.base_strategy import BaseStrategy
        from indicators.tradingview_indicators import TradingViewIndicators, ConfluenceValidator, apply_all_indicators, get_confluence_signals

class TV80PercentStrategy(BaseStrategy):
    """
    TradingView 80% Winrate Strategy
    Requires:
    - RR >= 1:2
    - WR >= 80% historical
    - Confluence >= 4 indicators
    - LIT confirmation
    """
    
    def __init__(self, params: Dict[str, Any] = None):
        default_params = {
            # TradingView indicators params
            "rsi_period": 14,
            "rsi_overbought": 70,
            "rsi_oversold": 30,
            "ema_fast": 20,
            "ema_slow": 50,
            "ema_trend": 200,
            "bb_period": 20,
            "bb_std": 2.0,
            "macd_fast": 12,
            "macd_slow": 26,
            "macd_signal": 9,
            "stoch_k": 14,
            "stoch_d": 3,
            "adx_period": 14,
            "adx_threshold": 25,
            "atr_period": 14,
            "supertrend_period": 10,
            "supertrend_mult": 3.0,
            
            # LIT params
            "swing_lookback": 8,
            "sweep_threshold": 0.001,
            "min_rr": 2.0,  # Minimum 1:2
            "sl_buffer": 0.001,
            
            # 80% WR filter
            "min_winrate": 80.0,
            "min_confluence": 5,  # At least 5 confirmations
            "min_confidence": 80.0,  # 80% confidence
            
            # Additional filters
            "use_htf_filter": True,
            "htf_ema": 200,
            "use_premium_discount": True,
            "premium_discount_bull_max": 0.4,  # Only buy in discount <0.4
            "premium_discount_bear_min": 0.6,  # Only sell in premium >0.6
            
            # Time exit
            "time_exit_bars": 30,
            "sl_atr": 1.5,
            "tp_atr": 3.0,  # 1:2 RR
        }
        
        if params:
            default_params.update(params)
        
        super().__init__(name="TV_80Percent_LIT", params=default_params)
        
        # Validator for 80% WR and RR 1:2
        self.validator = ConfluenceValidator(
            min_rr=self.params["min_rr"],
            min_winrate=self.params["min_winrate"],
            min_confluence=self.params["min_confluence"]
        )
        
        # For tracking
        self.signals_tested = 0
        self.signals_passed = 0
    
    def calculate_indicators(self, data: pd.DataFrame) -> pd.DataFrame:
        """Apply all TradingView indicators + LIT"""
        df = apply_all_indicators(data)
        
        # Additional LIT indicators
        df["swing_high"] = False
        df["swing_low"] = False
        df["swing_high_price"] = np.nan
        df["swing_low_price"] = np.nan
        
        lookback = self.params["swing_lookback"]
        for i in range(lookback, len(df) - lookback):
            window_high = df["high"].iloc[i-lookback:i+lookback+1]
            if df["high"].iloc[i] == window_high.max():
                df.loc[df.index[i], "swing_high"] = True
                df.loc[df.index[i], "swing_high_price"] = df["high"].iloc[i]
            
            window_low = df["low"].iloc[i-lookback:i+lookback+1]
            if df["low"].iloc[i] == window_low.min():
                df.loc[df.index[i], "swing_low"] = True
                df.loc[df.index[i], "swing_low_price"] = df["low"].iloc[i]
        
        # Range and premium/discount
        df["range_high"] = df["high"].rolling(50).max()
        df["range_low"] = df["low"].rolling(50).min()
        df["range_size"] = df["range_high"] - df["range_low"]
        df["premium_discount"] = (df["close"] - df["range_low"]) / df["range_size"].replace(0, 1)
        
        # BOS detection
        df["bos_bull"] = False
        df["bos_bear"] = False
        for i in range(1, len(df)):
            recent_highs = df["swing_high_price"].iloc[max(0, i-20):i].dropna()
            if len(recent_highs) > 0:
                last_high = recent_highs.iloc[-1]
                if df["close"].iloc[i] > last_high and df["close"].iloc[i-1] <= last_high:
                    df.loc[df.index[i], "bos_bull"] = True
            
            recent_lows = df["swing_low_price"].iloc[max(0, i-20):i].dropna()
            if len(recent_lows) > 0:
                last_low = recent_lows.iloc[-1]
                if df["close"].iloc[i] < last_low and df["close"].iloc[i-1] >= last_low:
                    df.loc[df.index[i], "bos_bear"] = True
        
        # Liquidity sweep
        df["sweep_bull"] = False
        df["sweep_bear"] = False
        for i in range(20, len(df)):
            recent_highs = df["swing_high_price"].iloc[max(0, i-30):i].dropna()
            recent_lows = df["swing_low_price"].iloc[max(0, i-30):i].dropna()
            
            if len(recent_highs) > 0:
                last_high = recent_highs.iloc[-1]
                if df["high"].iloc[i] > last_high * 1.002 and df["close"].iloc[i] < last_high:
                    df.loc[df.index[i], "sweep_bear"] = True
            
            if len(recent_lows) > 0:
                last_low = recent_lows.iloc[-1]
                if df["low"].iloc[i] < last_low * 0.998 and df["close"].iloc[i] > last_low:
                    df.loc[df.index[i], "sweep_bull"] = True
        
        # Order Block (simplified)
        df["ob_bull"] = False
        df["ob_bear"] = False
        df["ob_high"] = np.nan
        df["ob_low"] = np.nan
        
        for i in range(10, len(df)):
            # Look for bullish OB: bearish candle before bullish displacement
            if df["close"].iloc[i-1] < df["open"].iloc[i-1]:  # Bearish
                if df["close"].iloc[i] > df["open"].iloc[i]:  # Bullish next
                    body_size = abs(df["close"].iloc[i] - df["open"].iloc[i])
                    avg_body = df["close"].rolling(20).apply(lambda x: np.mean(np.abs(x - np.roll(x,1)))).iloc[i] if i>20 else body_size
                    if body_size > avg_body * 1.5 if not pd.isna(avg_body) else True:
                        df.loc[df.index[i], "ob_bull"] = True
                        df.loc[df.index[i], "ob_high"] = df["high"].iloc[i-1]
                        df.loc[df.index[i], "ob_low"] = df["low"].iloc[i-1]
            
            if df["close"].iloc[i-1] > df["open"].iloc[i-1]:  # Bullish
                if df["close"].iloc[i] < df["open"].iloc[i]:  # Bearish next
                    body_size = abs(df["close"].iloc[i] - df["open"].iloc[i])
                    avg_body = df["close"].rolling(20).apply(lambda x: np.mean(np.abs(x - np.roll(x,1)))).iloc[i] if i>20 else body_size
                    if body_size > avg_body * 1.5 if not pd.isna(avg_body) else True:
                        df.loc[df.index[i], "ob_bear"] = True
                        df.loc[df.index[i], "ob_high"] = df["high"].iloc[i-1]
                        df.loc[df.index[i], "ob_low"] = df["low"].iloc[i-1]
        
        # FVG
        df["fvg_bull"] = False
        df["fvg_bear"] = False
        for i in range(1, len(df)-1):
            if df["low"].iloc[i+1] > df["high"].iloc[i-1]:
                df.loc[df.index[i], "fvg_bull"] = True
            if df["high"].iloc[i+1] < df["low"].iloc[i-1]:
                df.loc[df.index[i], "fvg_bear"] = True
        
        return df
    
    def generate_signals(self, data: pd.DataFrame) -> pd.DataFrame:
        """Generate signals with 80% WR filter"""
        df = self.calculate_indicators(data)
        
        df["signal"] = 0
        df["reason"] = ""
        df["confidence"] = 0.0
        df["confluence_score"] = 0
        df["rr"] = 0.0
        df["winrate"] = 0.0
        
        self.signals_tested = 0
        self.signals_passed = 0
        
        for i in range(50, len(df)):
            if pd.isna(df["atr"].iloc[i]):
                continue
            
            # Get all confluence signals at this bar
            tv_signals = get_confluence_signals(df, i)
            
            # Add LIT signals
            tv_signals["bos_bull"] = df["bos_bull"].iloc[i]
            tv_signals["bos_bear"] = df["bos_bear"].iloc[i]
            tv_signals["sweep_bull"] = df["sweep_bull"].iloc[i]
            tv_signals["sweep_bear"] = df["sweep_bear"].iloc[i]
            tv_signals["ob_bull"] = df["ob_bull"].iloc[i]
            tv_signals["ob_bear"] = df["ob_bear"].iloc[i]
            tv_signals["fvg_bull"] = df["fvg_bull"].iloc[i]
            tv_signals["fvg_bear"] = df["fvg_bear"].iloc[i]
            
            # For LIT, we need sweep + BOS + OB
            tv_signals["lit_bull"] = df["sweep_bull"].iloc[i] and df["bos_bull"].iloc[i]
            tv_signals["lit_bear"] = df["sweep_bear"].iloc[i] and df["bos_bear"].iloc[i]
            
            close = df["close"].iloc[i]
            atr = df["atr"].iloc[i] if not pd.isna(df["atr"].iloc[i]) else close * 0.01
            
            # Try bullish
            # Conditions for bullish LIT + TV confluence
            premium_discount = df["premium_discount"].iloc[i] if not pd.isna(df["premium_discount"].iloc[i]) else 0.5
            
            # Must be in discount for bullish
            if self.params["use_premium_discount"] and premium_discount > self.params["premium_discount_bull_max"]:
                # Not in discount, skip bullish
                pass
            else:
                # Check HTF filter: close > EMA200
                htf_ok = True
                if self.params["use_htf_filter"]:
                    if "sma_200" in df.columns and not pd.isna(df["sma_200"].iloc[i]):
                        htf_ok = close > df["sma_200"].iloc[i]
                
                if htf_ok:
                    # Calculate SL/TP for RR check
                    ob_low = df["ob_low"].iloc[i] if not pd.isna(df["ob_low"].iloc[i]) else close - atr*1.5
                    sl = ob_low * 0.999 if not pd.isna(ob_low) else close - atr*1.5
                    tp = close + (close - sl) * self.params["min_rr"]
                    
                    # Validate with confluence validator (RR, WR, Confluence)
                    validation = self.validator.validate_trade(
                        entry=close,
                        sl=sl,
                        tp=tp,
                        side=1,
                        indicators=tv_signals,
                        strategy_name="TV_80Percent_LIT"
                    )
                    
                    self.signals_tested += 1
                    
                    # Check if validation passes 80% WR and RR 1:2 and confluence
                    if validation["valid"] and validation["confidence"] >= self.params["min_confidence"]:
                        # Additional LIT specific: need at least sweep or BOS
                        has_lit = tv_signals.get("lit_bull", False) or tv_signals.get("bos_bull", False)
                        if has_lit or validation["confluence_score"] >= 7:  # High confluence can override
                            df.loc[df.index[i], "signal"] = 1
                            df.loc[df.index[i], "confidence"] = validation["confidence"]
                            df.loc[df.index[i], "confluence_score"] = validation["confluence_score"]
                            df.loc[df.index[i], "rr"] = validation["rr"]
                            df.loc[df.index[i], "winrate"] = validation["winrate"]
                            df.loc[df.index[i], "reason"] = f"TV80 BULL: RR {validation['rr']:.2f} WR {validation['winrate']:.1f}% Conf {validation['confidence']:.0f}% Score {validation['confluence_score']} - {', '.join(validation['confirmations'][:3])} | {validation['reason']}"
                            
                            self.signals_passed += 1
                            logger.debug(f"BULL signal at {i}: {df.loc[df.index[i], 'reason']}")
            
            # Try bearish
            if self.params["use_premium_discount"] and premium_discount < self.params["premium_discount_bear_min"]:
                pass
            else:
                htf_ok = True
                if self.params["use_htf_filter"]:
                    if "sma_200" in df.columns and not pd.isna(df["sma_200"].iloc[i]):
                        htf_ok = close < df["sma_200"].iloc[i]
                
                if htf_ok:
                    ob_high = df["ob_high"].iloc[i] if not pd.isna(df["ob_high"].iloc[i]) else close + atr*1.5
                    sl = ob_high * 1.001 if not pd.isna(ob_high) else close + atr*1.5
                    tp = close - (sl - close) * self.params["min_rr"]
                    
                    validation = self.validator.validate_trade(
                        entry=close,
                        sl=sl,
                        tp=tp,
                        side=-1,
                        indicators=tv_signals,
                        strategy_name="TV_80Percent_LIT"
                    )
                    
                    self.signals_tested += 1
                    
                    if validation["valid"] and validation["confidence"] >= self.params["min_confidence"]:
                        has_lit = tv_signals.get("lit_bear", False) or tv_signals.get("bos_bear", False)
                        if has_lit or validation["confluence_score"] >= 7:
                            df.loc[df.index[i], "signal"] = -1
                            df.loc[df.index[i], "confidence"] = validation["confidence"]
                            df.loc[df.index[i], "confluence_score"] = validation["confluence_score"]
                            df.loc[df.index[i], "rr"] = validation["rr"]
                            df.loc[df.index[i], "winrate"] = validation["winrate"]
                            df.loc[df.index[i], "reason"] = f"TV80 BEAR: RR {validation['rr']:.2f} WR {validation['winrate']:.1f}% Conf {validation['confidence']:.0f}% Score {validation['confluence_score']} - {', '.join(validation['confirmations'][:3])} | {validation['reason']}"
                            
                            self.signals_passed += 1
                            logger.debug(f"BEAR signal at {i}: {df.loc[df.index[i], 'reason']}")
        
        # Calculate SL/TP
        df["sl_price"] = np.nan
        df["tp_price"] = np.nan
        
        for i in range(len(df)):
            if df["signal"].iloc[i] != 0:
                entry_info = self.calculate_entry(df, i, df["signal"].iloc[i])
                df.loc[df.index[i], "sl_price"] = entry_info.get("sl_price", np.nan)
                df.loc[df.index[i], "tp_price"] = entry_info.get("tp_price", np.nan)
        
        logger.info(f"TV80: Tested {self.signals_tested} potential signals, Passed {self.signals_passed} (Pass rate {self.signals_passed/max(1,self.signals_tested)*100:.1f}%) - Final signals: Bull {len(df[df['signal']==1])}, Bear {len(df[df['signal']==-1])}")
        
        return df
    
    def calculate_entry(self, data: pd.DataFrame, idx: int, signal: int) -> Dict[str, Any]:
        """Calculate entry with RR 1:2 minimum"""
        row = data.iloc[idx]
        entry = row["close"]
        atr = row.get("atr", entry * 0.01)
        
        if signal == 1:  # Long
            ob_low = row.get("ob_low", np.nan)
            if not pd.isna(ob_low):
                sl = ob_low * 0.999
            else:
                sl = entry - atr * self.params["sl_atr"]
            
            risk = entry - sl
            if risk <= 0:
                risk = atr * 1.5
            
            tp = entry + risk * self.params["min_rr"]
            
            # Adjust to next liquidity if exists
            if not pd.isna(row.get("range_high", np.nan)):
                liq = row["range_high"]
                if liq > entry and (liq - entry) / risk >= self.params["min_rr"]:
                    tp = liq
        
        else:  # Short
            ob_high = row.get("ob_high", np.nan)
            if not pd.isna(ob_high):
                sl = ob_high * 1.001
            else:
                sl = entry + atr * self.params["sl_atr"]
            
            risk = sl - entry
            if risk <= 0:
                risk = atr * 1.5
            
            tp = entry - risk * self.params["min_rr"]
            
            if not pd.isna(row.get("range_low", np.nan)):
                liq = row["range_low"]
                if liq < entry and (entry - liq) / risk >= self.params["min_rr"]:
                    tp = liq
        
        return {
            "entry_price": entry,
            "sl_price": sl,
            "tp_price": tp,
            "atr": atr,
            "timestamp": row.get("timestamp", None)
        }
