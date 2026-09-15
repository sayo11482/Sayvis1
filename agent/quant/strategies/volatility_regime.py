"""
ODIN QUANT - Strategy #5: Volatility Regime Detection
Meta-strategy that switches between Trending/Ranging/High Vol

Regimes:
- Trending: ADX > 25
- Ranging: ADX < 20 + BB squeeze
- High Vol: ATR > 2*MA

This is not a standalone trading strategy but a filter that tells which strategy to use.
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

class VolatilityRegimeStrategy(BaseStrategy):
    def __init__(self, params: Dict[str, Any] = None):
        default_params = {
            "adx_trending": 25,
            "adx_ranging": 20,
            "bb_squeeze_threshold": 0.5,
            "atr_high_vol_multiplier": 2.0,
            "atr_period": 14,
            "adx_period": 14,
            "bb_period": 20
        }
        if params:
            default_params.update(params)
        super().__init__("VolatilityRegime", default_params)
    
    def calculate_indicators(self, data: pd.DataFrame) -> pd.DataFrame:
        df = data.copy()
        
        # ADX
        high, low, close = df["high"], df["low"], df["close"]
        tr1 = high - low
        tr2 = (high - close.shift()).abs()
        tr3 = (low - close.shift()).abs()
        tr = pd.concat([tr1, tr2, tr3], axis=1).max(axis=1)
        
        up_move = high - high.shift()
        down_move = low.shift() - low
        plus_dm = np.where((up_move > down_move) & (up_move > 0), up_move, 0)
        minus_dm = np.where((down_move > up_move) & (down_move > 0), down_move, 0)
        
        period = self.params["adx_period"]
        tr_smooth = tr.rolling(period).mean()
        plus_dm_smooth = pd.Series(plus_dm).rolling(period).mean()
        minus_dm_smooth = pd.Series(minus_dm).rolling(period).mean()
        
        plus_di = 100 * (plus_dm_smooth / tr_smooth)
        minus_di = 100 * (minus_dm_smooth / tr_smooth)
        dx = 100 * (abs(plus_di - minus_di) / (plus_di + minus_di)).fillna(0)
        df["adx"] = dx.rolling(period).mean()
        
        # BB
        bb_period = self.params["bb_period"]
        df["bb_ma"] = df["close"].rolling(bb_period).mean()
        df["bb_std"] = df["close"].rolling(bb_period).std()
        df["bb_upper"] = df["bb_ma"] + df["bb_std"] * 2
        df["bb_lower"] = df["bb_ma"] - df["bb_std"] * 2
        df["bb_width"] = (df["bb_upper"] - df["bb_lower"]) / df["bb_ma"]
        df["bb_squeeze"] = df["bb_width"] < self.params["bb_squeeze_threshold"]
        
        # ATR
        df["atr"] = tr.rolling(self.params["atr_period"]).mean()
        df["atr_ma"] = df["atr"].rolling(bb_period).mean()
        df["high_vol"] = df["atr"] > df["atr_ma"] * self.params["atr_high_vol_multiplier"]
        
        # Regime
        df["regime"] = "unknown"
        df.loc[df["adx"] > self.params["adx_trending"], "regime"] = "trending"
        df.loc[(df["adx"] < self.params["adx_ranging"]) & (df["bb_squeeze"]), "regime"] = "ranging"
        df.loc[df["high_vol"], "regime"] = "high_vol"
        # Default to ranging if ADX low but not squeeze
        df.loc[(df["adx"] < self.params["adx_ranging"]) & (~df["bb_squeeze"]), "regime"] = "ranging"
        # If ADX between 20-25, keep previous or trending/ranging based on BB
        df["regime"] = df["regime"].replace("unknown", "trending")
        
        return df
    
    def generate_signals(self, data: pd.DataFrame) -> pd.DataFrame:
        """
        This strategy doesn't generate buy/sell signals,
        it generates regime signals for other strategies to use
        """
        df = self.calculate_indicators(data)
        df["signal"] = 0
        df["reason"] = df["regime"]
        df["confidence"] = 1.0
        
        # Map regime to recommended strategy
        regime_to_strategy = {
            "trending": "trend_following",
            "ranging": "mean_reversion",
            "high_vol": "momentum_breakout"
        }
        df["recommended_strategy"] = df["regime"].map(regime_to_strategy)
        
        logger.info(f"Regime distribution: {df['regime'].value_counts().to_dict()}")
        return df
    
    def get_current_regime(self, data: pd.DataFrame) -> str:
        """Get current market regime"""
        df = self.calculate_indicators(data)
        if df.empty:
            return "unknown"
        return df.iloc[-1]["regime"]
    
    def should_use_strategy(self, regime: str, strategy_name: str) -> bool:
        """Check if strategy should be used in current regime"""
        mapping = {
            "trending": ["trend_following", "momentum_breakout"],
            "ranging": ["mean_reversion", "pairs_trading"],
            "high_vol": ["momentum_breakout", "volatility_regime"]
        }
        return strategy_name in mapping.get(regime, [])
