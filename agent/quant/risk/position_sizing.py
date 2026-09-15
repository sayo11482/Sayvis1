"""
ODIN QUANT - Position Sizing: ATR, Volatility-based
Never risk more than 1% per trade!
"""
import pandas as pd
import numpy as np
from typing import Dict, Any
import logging

logger = logging.getLogger(__name__)

class PositionSizer:
    """
    Professional position sizing
    
    Methods:
    - ATR-based: risk / (ATR * multiplier)
    - Volatility-based: risk / (volatility * price)
    - Fixed fractional
    """
    
    def __init__(self, method: str = "atr", risk_per_trade: float = 0.01):
        self.method = method
        self.risk_per_trade = risk_per_trade  # 1%
    
    def calculate_size(
        self,
        capital: float,
        entry_price: float,
        sl_price: float,
        atr: float = None,
        volatility: float = None
    ) -> Dict[str, Any]:
        """
        Calculate position size based on risk
        
        Args:
            capital: total capital
            entry_price: entry price
            sl_price: stop loss price
            atr: Average True Range
            volatility: price volatility
        
        Returns:
            Dict with size, risk_amount, etc.
        """
        risk_amount = capital * self.risk_per_trade
        
        if self.method == "atr" and atr:
            # Size = Risk / (ATR * multiplier)
            # For simplicity, use distance to SL
            risk_per_unit = abs(entry_price - sl_price)
            if risk_per_unit == 0:
                risk_per_unit = atr * 2.0
            
            size = risk_amount / risk_per_unit
            
        elif self.method == "volatility" and volatility:
            # Size = Risk / (Volatility * Price)
            vol = volatility if volatility > 0 else 0.01
            size = risk_amount / (vol * entry_price)
            
        else:
            # Fixed risk: use SL distance
            risk_per_unit = abs(entry_price - sl_price)
            if risk_per_unit == 0:
                risk_per_unit = entry_price * 0.02  # 2% fallback
            
            size = risk_amount / risk_per_unit
        
        # For crypto/forex, size is in units, for stocks it's shares
        # Ensure size is reasonable
        max_size = capital * 0.2 / entry_price  # Max 20% capital per position
        size = min(size, max_size)
        
        # Minimum size
        size = max(size, 0.001)
        
        return {
            "size": size,
            "risk_amount": risk_amount,
            "risk_percent": self.risk_per_trade * 100,
            "entry_price": entry_price,
            "sl_price": sl_price,
            "position_value": size * entry_price,
            "method": self.method
        }
    
    def calculate_atr_size(
        self,
        capital: float,
        atr: float,
        atr_multiplier: float = 2.0
    ) -> float:
        """Simple ATR sizing"""
        risk_amount = capital * self.risk_per_trade
        size = risk_amount / (atr * atr_multiplier)
        return size

class VolatilityCalculator:
    @staticmethod
    def calculate_atr(data: pd.DataFrame, period: int = 14) -> pd.Series:
        high, low, close = data["high"], data["low"], data["close"]
        tr1 = high - low
        tr2 = (high - close.shift()).abs()
        tr3 = (low - close.shift()).abs()
        tr = pd.concat([tr1, tr2, tr3], axis=1).max(axis=1)
        return tr.rolling(period).mean()
    
    @staticmethod
    def calculate_volatility(data: pd.DataFrame, period: int = 20) -> pd.Series:
        returns = data["close"].pct_change()
        return returns.rolling(period).std() * np.sqrt(252)  # Annualized
