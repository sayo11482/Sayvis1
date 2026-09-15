"""
ODIN QUANT - Backtester: Accurate backtest with commission, slippage, walk-forward
"""
import pandas as pd
import numpy as np
from typing import Dict, List, Any, Optional
import logging
from datetime import datetime
import uuid

try:
    from ..strategies.base_strategy import BaseStrategy
    from ..risk.risk_manager import RiskManager
    from .performance import PerformanceAnalyzer
except ImportError:
    # Fallback for direct execution
    from strategies.base_strategy import BaseStrategy
    from risk.risk_manager import RiskManager
    from backtest.performance import PerformanceAnalyzer

logger = logging.getLogger(__name__)

class Backtester:
    """
    Professional backtester
    
    Features:
    - Commission & slippage
    - Risk manager integration
    - Multi-strategy
    - Walk-forward analysis
    - Realistic order execution
    """
    
    def __init__(self, config: Dict[str, Any], initial_capital: float = 10000.0):
        self.config = config
        self.initial_capital = initial_capital
        self.commission = config.get("backtest", {}).get("commission", 0.001)
        self.slippage = config.get("backtest", {}).get("slippage", 0.0005)
        
        self.risk_manager = RiskManager(config, initial_capital)
        
        self.trades: List[Dict] = []
        self.equity_curve: List[float] = [initial_capital]
        self.equity_timestamps: List[pd.Timestamp] = []
    
    def run(
        self,
        data: pd.DataFrame,
        strategy: BaseStrategy,
        symbol: str = "BTC/USDT"
    ) -> Dict[str, Any]:
        """
        Run backtest for single strategy
        
        Args:
            data: OHLCV DataFrame
            strategy: Strategy instance
            symbol: Symbol name
        
        Returns:
            Dict with trades, equity, metrics
        """
        logger.info(f"Starting backtest for {strategy.name} on {symbol} with {len(data)} bars")
        
        # Generate signals
        data_with_signals = strategy.generate_signals(data)
        
        # Reset
        self.risk_manager = RiskManager(self.config, self.initial_capital)
        self.trades = []
        self.equity_curve = [self.initial_capital]
        self.equity_timestamps = [data.iloc[0]["timestamp"] if "timestamp" in data.columns else datetime.now()]
        
        open_positions: Dict[str, Dict] = {}
        
        for idx in range(len(data_with_signals)):
            row = data_with_signals.iloc[idx]
            timestamp = row.get("timestamp", datetime.now())
            
            # Check exits first for open positions
            for pos_id in list(open_positions.keys()):
                pos = open_positions[pos_id]
                should_exit, reason = strategy.calculate_exit(data_with_signals, idx, pos)
                
                if should_exit:
                    # Calculate exit price with slippage
                    exit_price = self._apply_slippage(row, pos["side"], is_entry=False)
                    
                    closed = self.risk_manager.close_position(pos_id, exit_price, reason)
                    if closed:
                        self.trades.append(closed)
                        self.equity_curve.append(self.risk_manager.current_capital)
                        self.equity_timestamps.append(timestamp)
                        del open_positions[pos_id]
            
            # Check entries
            signal = row.get("signal", 0)
            if signal != 0:
                # Risk check
                can_open, reason = self.risk_manager.can_open_position(symbol, data_with_signals)
                if not can_open:
                    logger.debug(f"Entry blocked at {timestamp}: {reason}")
                    continue
                
                # Calculate position size
                entry_price = self._apply_slippage(row, signal, is_entry=True)
                sl_price = row.get("sl_price", entry_price * 0.98 if signal == 1 else entry_price * 1.02)
                atr = row.get("atr", entry_price * 0.01)
                
                size_info = self.risk_manager.calculate_position_size(
                    symbol, entry_price, sl_price, atr
                )
                
                # Create position
                pos_id = str(uuid.uuid4())
                position = {
                    "id": pos_id,
                    "symbol": symbol,
                    "side": signal,
                    "entry_price": entry_price,
                    "sl_price": sl_price,
                    "tp_price": row.get("tp_price", entry_price * 1.03 if signal == 1 else entry_price * 0.97),
                    "size": size_info["size"],
                    "entry_idx": idx,
                    "entry_time": timestamp,
                    "strategy": strategy.name,
                    "confidence": row.get("confidence", 0.5),
                    "reason": row.get("reason", ""),
                    "commission": self.commission,
                    "atr": atr
                }
                
                self.risk_manager.register_position(position)
                open_positions[pos_id] = position
                
                logger.debug(f"Opened {symbol} {signal} at {entry_price:.2f} size {size_info['size']:.4f}")
        
        # Close any remaining open positions at last price
        if open_positions:
            last_row = data_with_signals.iloc[-1]
            last_price = last_row["close"]
            last_timestamp = last_row.get("timestamp", datetime.now())
            
            for pos_id in list(open_positions.keys()):
                pos = open_positions[pos_id]
                exit_price = last_price
                closed = self.risk_manager.close_position(pos_id, exit_price, "end_of_backtest")
                if closed:
                    self.trades.append(closed)
                    self.equity_curve.append(self.risk_manager.current_capital)
                    self.equity_timestamps.append(last_timestamp)
        
        # Calculate metrics
        equity_series = pd.Series(self.equity_curve, index=self.equity_timestamps)
        metrics = PerformanceAnalyzer.calculate_metrics(
            self.trades,
            equity_series,
            risk_free_rate=self.config.get("risk", {}).get("risk_free_rate", 0.02)
        )
        
        logger.info(f"Backtest completed: {len(self.trades)} trades, PnL ${metrics['total_pnl']:.2f}, Sharpe {metrics['sharpe_ratio']:.2f}")
        
        return {
            "trades": self.trades,
            "equity_curve": equity_series,
            "metrics": metrics,
            "risk_status": self.risk_manager.get_status(),
            "signals": data_with_signals
        }
    
    def _apply_slippage(self, row: pd.Series, side: int, is_entry: bool) -> float:
        """Apply slippage to price"""
        base_price = row["close"]
        
        # Slippage: entry at slightly worse price
        if is_entry:
            if side == 1:  # Long entry: pay higher
                return base_price * (1 + self.slippage)
            else:  # Short entry: get lower
                return base_price * (1 - self.slippage)
        else:
            # Exit at slightly worse
            if side == 1:  # Long exit: get lower
                return base_price * (1 - self.slippage)
            else:  # Short exit: pay higher
                return base_price * (1 + self.slippage)
    
    def walk_forward(
        self,
        data: pd.DataFrame,
        strategy: BaseStrategy,
        train_days: int = 180,
        test_days: int = 30,
        step_days: int = 30,
        symbol: str = "BTC/USDT"
    ) -> Dict[str, Any]:
        """
        Walk-forward analysis to avoid overfitting
        
        Train on train_days, test on test_days, step by step_days
        """
        logger.info(f"Walk-forward: train {train_days}d, test {test_days}d, step {step_days}d")
        
        if "timestamp" not in data.columns:
            logger.warning("No timestamp column for walk-forward, using simple split")
            return self.run(data, strategy, symbol)
        
        data = data.sort_values("timestamp")
        data["timestamp"] = pd.to_datetime(data["timestamp"])
        
        results = []
        start_idx = 0
        
        # Convert days to bars (assuming 1h timeframe: 24 bars per day)
        train_bars = train_days * 24
        test_bars = test_days * 24
        step_bars = step_days * 24
        
        while start_idx + train_bars + test_bars < len(data):
            train_data = data.iloc[start_idx:start_idx+train_bars]
            test_data = data.iloc[start_idx+train_bars:start_idx+train_bars+test_bars]
            
            logger.info(f"WF: Train {train_data.iloc[0]['timestamp']} to {train_data.iloc[-1]['timestamp']} ({len(train_data)} bars)")
            logger.info(f"WF: Test {test_data.iloc[0]['timestamp']} to {test_data.iloc[-1]['timestamp']} ({len(test_data)} bars)")
            
            # In real WF, you would optimize params on train, then test on test
            # For simplicity, we just run strategy on test data
            result = self.run(test_data, strategy, symbol)
            results.append(result)
            
            start_idx += step_bars
        
        # Aggregate results
        if not results:
            return {}
        
        all_trades = []
        for r in results:
            all_trades.extend(r["trades"])
        
        # Combine equity curves (simplified)
        combined_equity = [self.initial_capital]
        for r in results:
            # Skip first equity point (duplicate)
            combined_equity.extend(r["equity_curve"].iloc[1:].tolist())
        
        combined_series = pd.Series(combined_equity)
        metrics = PerformanceAnalyzer.calculate_metrics(
            all_trades,
            combined_series,
            risk_free_rate=self.config.get("risk", {}).get("risk_free_rate", 0.02)
        )
        
        return {
            "walk_forward_results": results,
            "combined_trades": all_trades,
            "combined_equity": combined_series,
            "metrics": metrics,
            "num_windows": len(results)
        }
