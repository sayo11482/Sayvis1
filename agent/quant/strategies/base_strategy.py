"""
ODIN QUANT - Base Strategy Class
All strategies inherit from this
"""
from abc import ABC, abstractmethod
import pandas as pd
import numpy as np
from typing import Dict, Any, Optional, Tuple
import logging

logger = logging.getLogger(__name__)

class BaseStrategy(ABC):
    """
    Base class for all quantitative trading strategies
    
    Each strategy must implement:
    - generate_signals: Entry/exit signals
    - calculate_position_size: Risk-based sizing
    - get_params: Current parameters
    
    Risk management is enforced outside strategy via RiskManager
    """
    
    def __init__(self, name: str, params: Dict[str, Any] = None):
        self.name = name
        self.params = params or {}
        self.positions = []  # Track open positions
        
    @abstractmethod
    def generate_signals(self, data: pd.DataFrame) -> pd.DataFrame:
        """
        Generate trading signals
        
        Args:
            data: OHLCV DataFrame with timestamp, open, high, low, close, volume
            
        Returns:
            DataFrame with additional columns:
            - signal: 1 (buy), -1 (sell), 0 (hold)
            - entry_price, sl_price, tp_price
            - reason: text explanation
            - confidence: 0-1
        """
        pass
    
    def calculate_indicators(self, data: pd.DataFrame) -> pd.DataFrame:
        """Calculate technical indicators - override in subclass"""
        return data
    
    def filter_signals(self, data: pd.DataFrame) -> pd.DataFrame:
        """
        Apply filters (trading hours, news, spread, volume)
        Override for custom filters
        """
        # Default: no filtering
        return data
    
    def calculate_entry(
        self,
        data: pd.DataFrame,
        idx: int,
        signal: int
    ) -> Dict[str, Any]:
        """
        Calculate entry details at index
        
        Returns:
            Dict with entry_price, sl_price, tp_price, size, etc.
        """
        row = data.iloc[idx]
        
        # Default ATR-based SL/TP if available
        atr = row.get("atr", row["close"] * 0.01)  # 1% fallback
        
        sl_atr = self.params.get("sl_atr", 2.0)
        tp_atr = self.params.get("tp_atr", 3.0)
        
        if signal == 1:  # Long
            entry = row["close"]
            sl = entry - atr * sl_atr
            tp = entry + atr * tp_atr
        elif signal == -1:  # Short
            entry = row["close"]
            sl = entry + atr * sl_atr
            tp = entry - atr * tp_atr
        else:
            return {}
        
        return {
            "entry_price": entry,
            "sl_price": sl,
            "tp_price": tp,
            "atr": atr,
            "timestamp": row["timestamp"]
        }
    
    def calculate_exit(
        self,
        data: pd.DataFrame,
        idx: int,
        position: Dict[str, Any]
    ) -> Tuple[bool, str]:
        """
        Check if should exit position
        
        Args:
            data: OHLCV data
            idx: current index
            position: open position dict
            
        Returns:
            (should_exit: bool, reason: str)
        """
        row = data.iloc[idx]
        entry = position["entry_price"]
        side = position["side"]
        
        # Check SL
        if side == 1 and row["low"] <= position["sl_price"]:
            return True, "stop_loss"
        if side == -1 and row["high"] >= position["sl_price"]:
            return True, "stop_loss"
        
        # Check TP
        if side == 1 and row["high"] >= position["tp_price"]:
            return True, "take_profit"
        if side == -1 and row["low"] <= position["tp_price"]:
            return True, "take_profit"
        
        # Time-based exit
        time_exit_bars = self.params.get("time_exit_bars", 20)
        if idx - position["entry_idx"] >= time_exit_bars:
            return True, "time_exit"
        
        return False, ""
    
    def get_params(self) -> Dict[str, Any]:
        """Return current parameters"""
        return self.params.copy()
    
    def set_params(self, params: Dict[str, Any]):
        """Update parameters"""
        self.params.update(params)
        logger.info(f"Strategy {self.name} params updated: {params}")
    
    def __str__(self):
        return f"{self.name}({self.params})"
