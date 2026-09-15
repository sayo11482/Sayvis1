"""
ODIN QUANT - Strategy #2: Mean Reversion (Bollinger Bands + RSI + Z-Score)
Best in ranging markets

Entry: Price touches BB outer + RSI extreme + Z-Score
Exit: Middle BB, RSI 50, or time
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

class MeanReversionStrategy(BaseStrategy):
    def __init__(self, params: Dict[str, Any] = None):
        default_params = {
            "bb_period": 20,
            "bb_std": 2.0,
            "rsi_period": 14,
            "rsi_oversold": 30,
            "rsi_overbought": 70,
            "zscore_period": 20,
            "zscore_threshold": 2.0,
            "sl_bb_multiplier": 1.5,
            "time_exit_bars": 10
        }
        if params:
            default_params.update(params)
        super().__init__("MeanReversion", default_params)
    
    def calculate_indicators(self, data: pd.DataFrame) -> pd.DataFrame:
        df = data.copy()
        
        # Bollinger Bands
        period = self.params["bb_period"]
        std_mult = self.params["bb_std"]
        
        df["bb_ma"] = df["close"].rolling(period).mean()
        df["bb_std"] = df["close"].rolling(period).std()
        df["bb_upper"] = df["bb_ma"] + df["bb_std"] * std_mult
        df["bb_lower"] = df["bb_ma"] - df["bb_std"] * std_mult
        df["bb_width"] = (df["bb_upper"] - df["bb_lower"]) / df["bb_ma"]
        
        # RSI
        df = self._calculate_rsi(df, self.params["rsi_period"])
        
        # Z-Score
        df["zscore"] = (df["close"] - df["bb_ma"]) / df["bb_std"]
        
        # ATR for SL
        df = self._calculate_atr(df, 14)
        
        return df
    
    def _calculate_rsi(self, df: pd.DataFrame, period: int) -> pd.DataFrame:
        delta = df["close"].diff()
        gain = (delta.where(delta > 0, 0)).rolling(window=period).mean()
        loss = (-delta.where(delta < 0, 0)).rolling(window=period).mean()
        
        rs = gain / loss
        df["rsi"] = 100 - (100 / (1 + rs))
        return df
    
    def _calculate_atr(self, df: pd.DataFrame, period: int) -> pd.DataFrame:
        high, low, close = df["high"], df["low"], df["close"]
        tr1 = high - low
        tr2 = (high - close.shift()).abs()
        tr3 = (low - close.shift()).abs()
        tr = pd.concat([tr1, tr2, tr3], axis=1).max(axis=1)
        df["atr"] = tr.rolling(period).mean()
        return df
    
    def generate_signals(self, data: pd.DataFrame) -> pd.DataFrame:
        df = self.calculate_indicators(data)
        df["signal"] = 0
        df["reason"] = ""
        df["confidence"] = 0.0
        
        for i in range(len(df)):
            row = df.iloc[i]
            if pd.isna(row["bb_upper"]) or pd.isna(row["rsi"]):
                continue
            
            # Avoid low volatility (squeeze)
            if row["bb_width"] < 0.02:  # Too tight
                continue
            
            # Long: price below lower BB + RSI oversold + Z-Score
            if (row["close"] < row["bb_lower"] and
                row["rsi"] < self.params["rsi_oversold"] and
                row["zscore"] < -self.params["zscore_threshold"]):
                
                df.loc[df.index[i], "signal"] = 1
                df.loc[df.index[i], "reason"] = f"MeanRev Long: Close<BB lower RSI={row['rsi']:.1f} Z={row['zscore']:.2f}"
                df.loc[df.index[i], "confidence"] = min(0.8, abs(row["zscore"]) / 3.0)
            
            # Short: price above upper BB + RSI overbought + Z-Score
            elif (row["close"] > row["bb_upper"] and
                  row["rsi"] > self.params["rsi_overbought"] and
                  row["zscore"] > self.params["zscore_threshold"]):
                
                df.loc[df.index[i], "signal"] = -1
                df.loc[df.index[i], "reason"] = f"MeanRev Short: Close>BB upper RSI={row['rsi']:.1f} Z={row['zscore']:.2f}"
                df.loc[df.index[i], "confidence"] = min(0.8, abs(row["zscore"]) / 3.0)
        
        # SL/TP: BB-based
        df["sl_price"] = np.nan
        df["tp_price"] = np.nan
        for i in range(len(df)):
            if df.iloc[i]["signal"] == 1:
                # SL below BB lower by multiplier
                sl = df.iloc[i]["close"] - df.iloc[i]["bb_std"] * self.params["sl_bb_multiplier"]
                tp = df.iloc[i]["bb_ma"]  # Middle BB
                df.loc[df.index[i], "sl_price"] = sl
                df.loc[df.index[i], "tp_price"] = tp
            elif df.iloc[i]["signal"] == -1:
                sl = df.iloc[i]["close"] + df.iloc[i]["bb_std"] * self.params["sl_bb_multiplier"]
                tp = df.iloc[i]["bb_ma"]
                df.loc[df.index[i], "sl_price"] = sl
                df.loc[df.index[i], "tp_price"] = tp
        
        logger.info(f"MeanReversion: {len(df[df['signal']!=0])} signals")
        return df
