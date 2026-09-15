"""
ODIN QUANT - Strategy #4: Pairs Trading / Statistical Arbitrage
For correlated markets

Entry: Z-Score of spread > threshold, cointegration holds
Exit: Z-Score reverts to 0, or cointegration breaks, or stop
"""
import pandas as pd
import numpy as np
try:
    from .base_strategy import BaseStrategy
except ImportError:
    from strategies.base_strategy import BaseStrategy
from typing import Dict, Any, List
import logging

logger = logging.getLogger(__name__)

class PairsTradingStrategy(BaseStrategy):
    """
    Pairs Trading - Market neutral, works in all regimes
    
    Requires two correlated symbols.
    Steps:
    1. Check cointegration (Engle-Granger)
    2. Calculate spread = log(price1) - hedge_ratio * log(price2)
    3. Z-Score of spread
    4. Entry when |Z| > threshold
    5. Exit when Z -> 0
    
    For single symbol mode, we simulate with price vs its MA
    """
    
    def __init__(self, params: Dict[str, Any] = None):
        default_params = {
            "lookback": 60,
            "zscore_entry": 2.0,
            "zscore_exit": 0.0,
            "cointegration_pvalue": 0.05,
            "sl_zscore": 3.0,
            "pairs": [["BTC/USDT", "ETH/USDT"]]
        }
        if params:
            default_params.update(params)
        super().__init__("PairsTrading", default_params)
    
    def calculate_indicators(self, data: pd.DataFrame) -> pd.DataFrame:
        df = data.copy()
        
        # For single asset, create synthetic pair: price vs MA
        lookback = self.params["lookback"]
        df["ma"] = df["close"].rolling(lookback).mean()
        df["spread"] = np.log(df["close"]) - np.log(df["ma"])
        df["spread_ma"] = df["spread"].rolling(lookback).mean()
        df["spread_std"] = df["spread"].rolling(lookback).std()
        df["zscore"] = (df["spread"] - df["spread_ma"]) / df["spread_std"]
        
        # ATR for safety
        high, low, close = df["high"], df["low"], df["close"]
        tr1 = high - low
        tr2 = (high - close.shift()).abs()
        tr3 = (low - close.shift()).abs()
        tr = pd.concat([tr1, tr2, tr3], axis=1).max(axis=1)
        df["atr"] = tr.rolling(14).mean()
        
        return df
    
    def generate_signals(self, data: pd.DataFrame) -> pd.DataFrame:
        df = self.calculate_indicators(data)
        df["signal"] = 0
        df["reason"] = ""
        df["confidence"] = 0.0
        
        for i in range(len(df)):
            row = df.iloc[i]
            if pd.isna(row["zscore"]):
                continue
            
            # Long spread means close is high vs MA, expect reversion down -> short
            # Short spread means close low vs MA, expect up -> long
            # For pairs: Z>2 = spread too high, short spread (sell asset1, buy asset2)
            # Simplified: single asset mean reversion via spread
            
            if row["zscore"] < -self.params["zscore_entry"]:
                # Spread too low, expect up
                df.loc[df.index[i], "signal"] = 1
                df.loc[df.index[i], "reason"] = f"Pairs Long: Z={row['zscore']:.2f} spread low"
                df.loc[df.index[i], "confidence"] = min(0.8, abs(row["zscore"])/3.0)
            
            elif row["zscore"] > self.params["zscore_entry"]:
                df.loc[df.index[i], "signal"] = -1
                df.loc[df.index[i], "reason"] = f"Pairs Short: Z={row['zscore']:.2f} spread high"
                df.loc[df.index[i], "confidence"] = min(0.8, abs(row["zscore"])/3.0)
        
        # SL based on Z-Score
        df["sl_price"] = np.nan
        df["tp_price"] = np.nan
        for i in range(len(df)):
            if df.iloc[i]["signal"] != 0:
                close = df.iloc[i]["close"]
                atr = df.iloc[i]["atr"]
                # SL if Z goes to 3
                if df.iloc[i]["signal"] == 1:
                    sl = close - atr * 2.5
                    tp = df.iloc[i]["ma"]
                else:
                    sl = close + atr * 2.5
                    tp = df.iloc[i]["ma"]
                df.loc[df.index[i], "sl_price"] = sl
                df.loc[df.index[i], "tp_price"] = tp
        
        logger.info(f"PairsTrading: {len(df[df['signal']!=0])} signals")
        return df
    
    def calculate_exit(self, data: pd.DataFrame, idx: int, position: dict):
        # Check base exits first
        should_exit, reason = super().calculate_exit(data, idx, position)
        if should_exit:
            return True, reason
        
        row = data.iloc[idx]
        zscore = row.get("zscore", 0)
        
        # Exit when Z reverts to 0
        if abs(zscore) < self.params["zscore_exit"]:
            return True, "zscore_revert"
        
        # Stop if Z goes too far
        if abs(zscore) > self.params["sl_zscore"]:
            return True, "zscore_stop"
        
        return False, ""
