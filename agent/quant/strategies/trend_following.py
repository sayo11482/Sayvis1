"""
ODIN QUANT - Strategy #1: Trend Following with Multi-Timeframe Filter
Highest priority - Most robust in trending markets

Entry: EMA crossover + ADX + Volume confirmation + HTF filter
Exit: ATR-based SL/TP + Trailing + Time
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

class TrendFollowingStrategy(BaseStrategy):
    """
    Trend Following - The most proven strategy long-term
    
    Logic:
    - Fast EMA crosses above Slow EMA = Uptrend
    - ADX > threshold = Strong trend (not ranging)
    - Volume > MA = Confirmation (real move, not fake)
    - HTF EMA filter = Trade only in HTF direction
    
    Why it works:
    - Markets trend ~30% of time but those trends pay for all losses
    - Multi-filter reduces whipsaws
    - ATR-based exits adapt to volatility
    """
    
    def __init__(self, params: Dict[str, Any] = None):
        default_params = {
            "ema_fast": 20,
            "ema_slow": 50,
            "adx_period": 14,
            "adx_threshold": 25,
            "volume_ma": 20,
            "volume_multiplier": 1.5,
            "sl_atr": 2.0,
            "tp_atr": 3.0,
            "trailing_atr": 1.5,
            "time_exit_bars": 20,
            "htf_timeframe": "4h",
            "htf_ema": 50
        }
        if params:
            default_params.update(params)
        
        super().__init__("TrendFollowing", default_params)
    
    def calculate_indicators(self, data: pd.DataFrame) -> pd.DataFrame:
        """Calculate all needed indicators"""
        df = data.copy()
        
        # EMA
        df["ema_fast"] = df["close"].ewm(span=self.params["ema_fast"]).mean()
        df["ema_slow"] = df["close"].ewm(span=self.params["ema_slow"]).mean()
        
        # ADX - manual calculation (no TA-Lib needed)
        df = self._calculate_adx(df, self.params["adx_period"])
        
        # Volume MA
        df["volume_ma"] = df["volume"].rolling(self.params["volume_ma"]).mean()
        
        # ATR
        df = self._calculate_atr(df, 14)
        
        # HTF EMA (if HTF data provided, else use same timeframe)
        # For simplicity, we calculate on same TF but could be replaced with real HTF
        df["htf_ema"] = df["close"].ewm(span=self.params["htf_ema"]).mean()
        
        # Crossover signals
        df["ema_cross"] = 0
        df["ema_cross"] = np.where(
            (df["ema_fast"] > df["ema_slow"]) & (df["ema_fast"].shift(1) <= df["ema_slow"].shift(1)),
            1,  # Bullish cross
            np.where(
                (df["ema_fast"] < df["ema_slow"]) & (df["ema_fast"].shift(1) >= df["ema_slow"].shift(1)),
                -1,  # Bearish cross
                0
            )
        )
        
        return df
    
    def _calculate_adx(self, df: pd.DataFrame, period: int) -> pd.DataFrame:
        """Calculate ADX without TA-Lib"""
        high = df["high"]
        low = df["low"]
        close = df["close"]
        
        # True Range
        tr1 = high - low
        tr2 = (high - close.shift()).abs()
        tr3 = (low - close.shift()).abs()
        tr = pd.concat([tr1, tr2, tr3], axis=1).max(axis=1)
        
        # Directional Movement
        up_move = high - high.shift()
        down_move = low.shift() - low
        
        plus_dm = np.where((up_move > down_move) & (up_move > 0), up_move, 0)
        minus_dm = np.where((down_move > up_move) & (down_move > 0), down_move, 0)
        
        # Smoothed
        tr_smooth = tr.rolling(period).mean()
        plus_dm_smooth = pd.Series(plus_dm).rolling(period).mean()
        minus_dm_smooth = pd.Series(minus_dm).rolling(period).mean()
        
        # DI
        plus_di = 100 * (plus_dm_smooth / tr_smooth)
        minus_di = 100 * (minus_dm_smooth / tr_smooth)
        
        # DX and ADX
        dx = 100 * (abs(plus_di - minus_di) / (plus_di + minus_di)).fillna(0)
        adx = dx.rolling(period).mean()
        
        df["adx"] = adx
        df["plus_di"] = plus_di
        df["minus_di"] = minus_di
        
        return df
    
    def _calculate_atr(self, df: pd.DataFrame, period: int) -> pd.DataFrame:
        """Calculate ATR"""
        high = df["high"]
        low = df["low"]
        close = df["close"]
        
        tr1 = high - low
        tr2 = (high - close.shift()).abs()
        tr3 = (low - close.shift()).abs()
        tr = pd.concat([tr1, tr2, tr3], axis=1).max(axis=1)
        
        df["atr"] = tr.rolling(period).mean()
        return df
    
    def generate_signals(self, data: pd.DataFrame) -> pd.DataFrame:
        """Generate trend following signals"""
        df = self.calculate_indicators(data)
        
        df["signal"] = 0
        df["reason"] = ""
        df["confidence"] = 0.0
        
        for i in range(1, len(df)):
            row = df.iloc[i]
            prev = df.iloc[i-1]
            
            # Skip if indicators not ready
            if pd.isna(row["adx"]) or pd.isna(row["atr"]):
                continue
            
            # Filters
            strong_trend = row["adx"] > self.params["adx_threshold"]
            volume_ok = row["volume"] > row["volume_ma"] * self.params["volume_multiplier"]
            
            # HTF filter: price above/below HTF EMA
            htf_bull = row["close"] > row["htf_ema"]
            htf_bear = row["close"] < row["htf_ema"]
            
            # Long entry: bullish cross + strong trend + volume + HTF bullish
            if (row["ema_cross"] == 1 and 
                strong_trend and 
                volume_ok and 
                htf_bull and
                row["plus_di"] > row["minus_di"]):
                
                df.loc[df.index[i], "signal"] = 1
                df.loc[df.index[i], "reason"] = f"Bull cross EMA{self.params['ema_fast']}/{self.params['ema_slow']} ADX={row['adx']:.1f} Vol OK HTF bull"
                df.loc[df.index[i], "confidence"] = min(0.9, row["adx"] / 50.0)
            
            # Short entry: bearish cross + strong trend + volume + HTF bearish
            elif (row["ema_cross"] == -1 and 
                  strong_trend and 
                  volume_ok and 
                  htf_bear and
                  row["minus_di"] > row["plus_di"]):
                
                df.loc[df.index[i], "signal"] = -1
                df.loc[df.index[i], "reason"] = f"Bear cross EMA{self.params['ema_fast']}/{self.params['ema_slow']} ADX={row['adx']:.1f} Vol OK HTF bear"
                df.loc[df.index[i], "confidence"] = min(0.9, row["adx"] / 50.0)
        
        # Add SL/TP
        df["sl_price"] = np.nan
        df["tp_price"] = np.nan
        
        for i in range(len(df)):
            if df.iloc[i]["signal"] != 0:
                entry_info = self.calculate_entry(df, i, df.iloc[i]["signal"])
                df.loc[df.index[i], "sl_price"] = entry_info.get("sl_price", np.nan)
                df.loc[df.index[i], "tp_price"] = entry_info.get("tp_price", np.nan)
        
        logger.info(f"TrendFollowing: Generated {len(df[df['signal']!=0])} signals from {len(df)} bars")
        return df
    
    def calculate_exit(self, data: pd.DataFrame, idx: int, position: dict):
        """Enhanced exit with trailing stop"""
        # First check base exits (SL, TP, time)
        should_exit, reason = super().calculate_exit(data, idx, position)
        if should_exit:
            return True, reason
        
        # Trailing stop logic
        row = data.iloc[idx]
        entry = position["entry_price"]
        side = position["side"]
        atr = row.get("atr", entry * 0.01)
        
        # Update trailing stop
        if "trailing_sl" not in position:
            position["trailing_sl"] = position["sl_price"]
        
        if side == 1:  # Long
            # Trail up: new SL = close - ATR*trailing
            new_sl = row["close"] - atr * self.params["trailing_atr"]
            if new_sl > position["trailing_sl"]:
                position["trailing_sl"] = new_sl
            
            if row["low"] <= position["trailing_sl"]:
                return True, "trailing_stop"
        
        else:  # Short
            new_sl = row["close"] + atr * self.params["trailing_atr"]
            if new_sl < position["trailing_sl"]:
                position["trailing_sl"] = new_sl
            
            if row["high"] >= position["trailing_sl"]:
                return True, "trailing_stop"
        
        # Exit if EMA crosses back
        if side == 1 and row["ema_fast"] < row["ema_slow"]:
            return True, "ema_cross_exit"
        if side == -1 and row["ema_fast"] > row["ema_slow"]:
            return True, "ema_cross_exit"
        
        return False, ""
