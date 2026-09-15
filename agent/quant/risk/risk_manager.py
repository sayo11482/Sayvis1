"""
ODIN QUANT - Risk Manager: The most important module!
Enforces non-negotiable risk limits

- Max risk per trade: 1%
- Max daily DD: 3% -> Kill-switch
- Max total DD: 15% -> Stop all
- Correlation filter
- Position limits
"""
import pandas as pd
import numpy as np
from typing import Dict, List, Any, Optional
import logging
from datetime import datetime, date
try:
    from .position_sizing import PositionSizer
except ImportError:
    from risk.position_sizing import PositionSizer

logger = logging.getLogger(__name__)

class RiskManager:
    """
    Central Risk Manager - Non-negotiable limits
    
    This is the guardian of capital. No trade passes without its approval.
    """
    
    def __init__(self, config: Dict[str, Any], initial_capital: float = 10000.0):
        risk_cfg = config.get("risk", {})
        
        self.max_risk_per_trade = risk_cfg.get("max_risk_per_trade", 0.01)
        self.max_daily_dd = risk_cfg.get("max_daily_drawdown", 0.03)
        self.max_total_dd = risk_cfg.get("max_total_drawdown", 0.15)
        self.max_open_positions = risk_cfg.get("max_open_positions", 5)
        self.max_correlation = risk_cfg.get("max_correlation", 0.7)
        
        self.initial_capital = initial_capital
        self.current_capital = initial_capital
        self.peak_capital = initial_capital
        
        self.daily_pnl = 0.0
        self.daily_start_capital = initial_capital
        self.last_daily_reset = date.today()
        
        self.open_positions: List[Dict] = []
        self.closed_trades: List[Dict] = []
        
        self.kill_switch_active = False
        self.total_stop_active = False
        
        self.sizer = PositionSizer(
            method=risk_cfg.get("position_sizing_method", "atr"),
            risk_per_trade=self.max_risk_per_trade
        )
        
        logger.info(f"RiskManager initialized: {self.max_risk_per_trade*100}% per trade, {self.max_daily_dd*100}% daily DD, {self.max_total_dd*100}% total DD")
    
    def reset_daily(self):
        """Reset daily PnL at new day"""
        today = date.today()
        if today != self.last_daily_reset:
            logger.info(f"Daily reset: {self.daily_pnl:.2f} PnL yesterday, starting capital today: {self.current_capital:.2f}")
            self.daily_pnl = 0.0
            self.daily_start_capital = self.current_capital
            self.last_daily_reset = today
            self.kill_switch_active = False  # Reset kill-switch daily (but keep total DD check)
    
    def check_daily_drawdown(self) -> bool:
        """Check if daily DD exceeded -> kill-switch"""
        self.reset_daily()
        
        daily_dd = -self.daily_pnl / self.daily_start_capital if self.daily_start_capital > 0 else 0
        
        if daily_dd >= self.max_daily_dd:
            if not self.kill_switch_active:
                logger.warning(f"🔴 KILL-SWITCH ACTIVATED! Daily DD {daily_dd*100:.2f}% >= {self.max_daily_dd*100}%")
                self.kill_switch_active = True
            return False
        
        return True
    
    def check_total_drawdown(self) -> bool:
        """Check if total DD exceeded -> stop all"""
        total_dd = (self.peak_capital - self.current_capital) / self.peak_capital if self.peak_capital > 0 else 0
        
        if total_dd >= self.max_total_dd:
            if not self.total_stop_active:
                logger.critical(f"💀 TOTAL STOP! Total DD {total_dd*100:.2f}% >= {self.max_total_dd*100}% - STOP ALL TRADING")
                self.total_stop_active = True
            return False
        
        # Update peak
        if self.current_capital > self.peak_capital:
            self.peak_capital = self.current_capital
        
        return True
    
    def check_position_limits(self) -> bool:
        """Check max open positions"""
        if len(self.open_positions) >= self.max_open_positions:
            logger.warning(f"Max open positions reached: {len(self.open_positions)}/{self.max_open_positions}")
            return False
        return True
    
    def check_correlation(self, symbol: str, data: pd.DataFrame) -> bool:
        """Check correlation with open positions (simplified)"""
        # In production, calculate real correlation between symbols
        # For now, check if same symbol already open
        for pos in self.open_positions:
            if pos["symbol"] == symbol:
                logger.warning(f"Correlation filter: {symbol} already has open position")
                return False
        
        # TODO: Add real correlation matrix check
        return True
    
    def can_open_position(self, symbol: str, data: pd.DataFrame = None) -> tuple[bool, str]:
        """
        Master check: can we open new position?
        
        Returns:
            (can_open: bool, reason: str)
        """
        if self.total_stop_active:
            return False, f"Total DD stop active ({self.max_total_dd*100}%)"
        
        if self.kill_switch_active:
            return False, f"Daily kill-switch active ({self.max_daily_dd*100}% daily DD)"
        
        if not self.check_daily_drawdown():
            return False, "Daily DD exceeded"
        
        if not self.check_total_drawdown():
            return False, "Total DD exceeded"
        
        if not self.check_position_limits():
            return False, f"Max positions {self.max_open_positions} reached"
        
        if not self.check_correlation(symbol, data):
            return False, f"Correlation filter blocked {symbol}"
        
        return True, "OK"
    
    def calculate_position_size(
        self,
        symbol: str,
        entry_price: float,
        sl_price: float,
        atr: float = None,
        volatility: float = None
    ) -> Dict[str, Any]:
        """Calculate risk-based position size"""
        return self.sizer.calculate_size(
            capital=self.current_capital,
            entry_price=entry_price,
            sl_price=sl_price,
            atr=atr,
            volatility=volatility
        )
    
    def register_position(self, position: Dict[str, Any]):
        """Register new open position"""
        self.open_positions.append(position)
        logger.info(f"Position opened: {position['symbol']} {position['side']} size={position['size']:.4f} capital={self.current_capital:.2f}")
    
    def close_position(self, position_id: str, exit_price: float, reason: str) -> Dict[str, Any]:
        """Close position and update PnL"""
        for i, pos in enumerate(self.open_positions):
            if pos["id"] == position_id:
                # Calculate PnL
                entry = pos["entry_price"]
                size = pos["size"]
                side = pos["side"]
                
                if side == 1:  # Long
                    pnl = (exit_price - entry) * size
                else:  # Short
                    pnl = (entry - exit_price) * size
                
                # Apply commission
                commission = pos.get("commission", 0.001)
                pnl -= abs(entry * size * commission) + abs(exit_price * size * commission)
                
                # Update capital
                self.current_capital += pnl
                self.daily_pnl += pnl
                
                # Move to closed
                closed = {
                    **pos,
                    "exit_price": exit_price,
                    "exit_reason": reason,
                    "pnl": pnl,
                    "exit_time": datetime.now(),
                    "duration_bars": pos.get("duration", 0)
                }
                self.closed_trades.append(closed)
                self.open_positions.pop(i)
                
                logger.info(f"Position closed: {pos['symbol']} PnL={pnl:.2f} reason={reason} capital={self.current_capital:.2f} daily={self.daily_pnl:.2f}")
                
                return closed
        
        logger.warning(f"Position {position_id} not found for closing")
        return {}
    
    def get_status(self) -> Dict[str, Any]:
        """Get risk status"""
        total_dd = (self.peak_capital - self.current_capital) / self.peak_capital if self.peak_capital > 0 else 0
        daily_dd = -self.daily_pnl / self.daily_start_capital if self.daily_start_capital > 0 and self.daily_pnl < 0 else 0
        
        return {
            "current_capital": self.current_capital,
            "initial_capital": self.initial_capital,
            "peak_capital": self.peak_capital,
            "total_pnl": self.current_capital - self.initial_capital,
            "total_pnl_percent": (self.current_capital - self.initial_capital) / self.initial_capital * 100,
            "total_drawdown": total_dd * 100,
            "daily_pnl": self.daily_pnl,
            "daily_drawdown": daily_dd * 100,
            "open_positions": len(self.open_positions),
            "closed_trades": len(self.closed_trades),
            "kill_switch": self.kill_switch_active,
            "total_stop": self.total_stop_active,
            "max_risk_per_trade": self.max_risk_per_trade * 100,
            "max_daily_dd": self.max_daily_dd * 100,
            "max_total_dd": self.max_total_dd * 100
        }
