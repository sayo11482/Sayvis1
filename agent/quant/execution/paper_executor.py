"""
ODIN QUANT - Paper Executor: Simulated live trading
"""
import pandas as pd
from typing import Dict, Any, List
import logging
import time
import uuid
from datetime import datetime

try:
    from ..risk.risk_manager import RiskManager
except ImportError:
    from risk.risk_manager import RiskManager

logger = logging.getLogger(__name__)

class PaperExecutor:
    """
    Paper trading executor - Simulates live trading without real money
    
    Features:
    - Real-time data simulation
    - Risk manager integration
    - Slippage & commission
    - Position tracking
    """
    
    def __init__(self, config: Dict[str, Any], initial_capital: float = 10000.0):
        self.config = config
        self.risk_manager = RiskManager(config, initial_capital)
        self.open_positions: Dict[str, Dict] = {}
        self.pending_orders: List[Dict] = []
        
        self.commission = config.get("risk", {}).get("commission", 0.001)
        self.slippage = config.get("risk", {}).get("slippage", 0.0005)
        
        logger.info(f"PaperExecutor initialized with ${initial_capital:.2f}")
    
    def place_order(
        self,
        symbol: str,
        side: int,  # 1=long, -1=short
        entry_price: float,
        sl_price: float,
        tp_price: float,
        size: float = None,
        reason: str = "",
        strategy: str = ""
    ) -> Dict[str, Any]:
        """
        Place paper order
        
        Returns:
            Order result
        """
        # Risk check
        can_open, risk_reason = self.risk_manager.can_open_position(symbol)
        if not can_open:
            return {
                "success": False,
                "error": f"Risk blocked: {risk_reason}",
                "symbol": symbol
            }
        
        # Calculate size if not provided
        if size is None:
            size_info = self.risk_manager.calculate_position_size(symbol, entry_price, sl_price)
            size = size_info["size"]
        
        # Apply slippage
        if side == 1:
            exec_price = entry_price * (1 + self.slippage)
        else:
            exec_price = entry_price * (1 - self.slippage)
        
        order_id = str(uuid.uuid4())
        position = {
            "id": order_id,
            "symbol": symbol,
            "side": side,
            "entry_price": exec_price,
            "sl_price": sl_price,
            "tp_price": tp_price,
            "size": size,
            "entry_time": datetime.now(),
            "strategy": strategy,
            "reason": reason,
            "commission": self.commission,
            "status": "open"
        }
        
        self.risk_manager.register_position(position)
        self.open_positions[order_id] = position
        
        logger.info(f"📝 PAPER ORDER: {symbol} {'LONG' if side==1 else 'SHORT'} {size:.4f} @ {exec_price:.2f} SL {sl_price:.2f} TP {tp_price:.2f}")
        
        return {
            "success": True,
            "order_id": order_id,
            "symbol": symbol,
            "side": side,
            "entry_price": exec_price,
            "size": size,
            "position": position
        }
    
    def check_exits(self, symbol: str, current_price: float, high: float, low: float) -> List[Dict]:
        """Check if any positions should exit"""
        closed = []
        
        for pos_id in list(self.open_positions.keys()):
            pos = self.open_positions[pos_id]
            if pos["symbol"] != symbol:
                continue
            
            side = pos["side"]
            should_close = False
            reason = ""
            
            # SL
            if side == 1 and low <= pos["sl_price"]:
                should_close = True
                reason = "stop_loss"
                exit_price = pos["sl_price"]
            elif side == -1 and high >= pos["sl_price"]:
                should_close = True
                reason = "stop_loss"
                exit_price = pos["sl_price"]
            
            # TP
            elif side == 1 and high >= pos["tp_price"]:
                should_close = True
                reason = "take_profit"
                exit_price = pos["tp_price"]
            elif side == -1 and low <= pos["tp_price"]:
                should_close = True
                reason = "take_profit"
                exit_price = pos["tp_price"]
            
            if should_close:
                # Apply slippage on exit
                if side == 1:
                    exit_price = exit_price * (1 - self.slippage)
                else:
                    exit_price = exit_price * (1 + self.slippage)
                
                closed_pos = self.risk_manager.close_position(pos_id, exit_price, reason)
                if closed_pos:
                    closed.append(closed_pos)
                    del self.open_positions[pos_id]
                    logger.info(f"📕 PAPER CLOSE: {symbol} PnL ${closed_pos['pnl']:.2f} reason {reason}")
        
        return closed
    
    def get_open_positions(self) -> List[Dict]:
        return list(self.open_positions.values())
    
    def get_status(self) -> Dict[str, Any]:
        return {
            **self.risk_manager.get_status(),
            "open_positions_detail": self.get_open_positions()
        }
