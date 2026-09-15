"""
ODIN QUANT - Strategy #3: Momentum Breakout with Volatility Filter (ATR-based)
Captures explosive moves

Entry: Breakout of 20-bar high/low + ATR + Volume spike
Exit: ATR trailing + time
"""
import pandas as pd
import numpy as np
try:
    from .base_strategy import BaseStrategy
except ImportError:
    from strategies.base_strategy import BaseStrategy
from typing import Dict, Any
import logging

logger = logging.getLogger(__name__)

class MomentumBreakoutStrategy(BaseStrategy):
    def __init__(self, params: Dict[str, Any] = None):
        default_params = {
            "lookback": 20,
            "atr_period": 14,
            "atr_threshold": 1.0,
            "volume_spike": 2.0,
            "sl_atr": 1.5,
            "trailing_atr": 1.0,
            "time_exit_bars": 10
        }
        if params:
            default_params.update(params)
        super().__init__("MomentumBreakout", default_params)
    
    def calculate_indicators(self, data: pd.DataFrame) -> pd.DataFrame:
        df = data.copy()
        lookback = self.params["lookback"]
        
        df["high_lookback"] = df["high"].rolling(lookback).max()
        df["low_lookback"] = df["low"].rolling(lookback).min()
        df["close_ma"] = df["close"].rolling(lookback).mean()
        
        # ATR
        high, low, close = df["high"], df["low"], df["close"]
        tr1 = high - low
        tr2 = (high - close.shift()).abs()
        tr3 = (low - close.shift()).abs()
        tr = pd.concat([tr1, tr2, tr3], axis=1).max(axis=1)
        df["atr"] = tr.rolling(self.params["atr_period"]).mean()
        df["atr_ma"] = df["atr"].rolling(lookback).mean()
        
        # Volume
        df["volume_ma"] = df["volume"].rolling(lookback).mean()
        
        # Volatility filter
        df["volatility_ok"] = df["atr"] > df["atr_ma"] * self.params["atr_threshold"]
        
        return df
    
    def generate_signals(self, data: pd.DataFrame) -> pd.DataFrame:
        df = self.calculate_indicators(data)
        df["signal"] = 0
        df["reason"] = ""
        df["confidence"] = 0.0
        
        for i in range(1, len(df)):
            row = df.iloc[i]
            prev = df.iloc[i-1]
            
            if pd.isna(row["high_lookback"]) or pd.isna(row["atr"]):
                continue
            
            # Filters
            vol_ok = row["volatility_ok"]
            volume_spike = row["volume"] > row["volume_ma"] * self.params["volume_spike"]
            
            # Long breakout: close breaks above lookback high
            if (row["close"] > prev["high_lookback"] and
                vol_ok and volume_spike):
                
                df.loc[df.index[i], "signal"] = 1
                df.loc[df.index[i], "reason"] = f"Breakout Long: Close>{prev['high_lookback']:.2f} ATR OK Vol spike"
                df.loc[df.index[i], "confidence"] = 0.7
            
            # Short breakout: close breaks below lookback low
            elif (row["close"] < prev["low_lookback"] and
                  vol_ok and volume_spike):
                
                df.loc[df.index[i], "signal"] = -1
                df.loc[df.index[i], "reason"] = f"Breakout Short: Close<{prev['low_lookback']:.2f} ATR OK Vol spike"
                df.loc[df.index[i], "confidence"] = 0.7
        
        # SL/TP
        df["sl_price"] = np.nan
        df["tp_price"] = np.nan
        for i in range(len(df)):
            if df.iloc[i]["signal"] != 0:
                atr = df.iloc[i]["atr"]
                close = df.iloc[i]["close"]
                if df.iloc[i]["signal"] == 1:
                    sl = close - atr * self.params["sl_atr"]
                    tp = close + atr * 3.0
                else:
                    sl = close + atr * self.params["sl_atr"]
                    tp = close - atr * 3.0
                df.loc[df.index[i], "sl_price"] = sl
                df.loc[df.index[i], "tp_price"] = tp
        
        logger.info(f"MomentumBreakout: {len(df[df['signal']!=0])} signals")
        return df
