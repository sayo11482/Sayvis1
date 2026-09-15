"""
ODIN QUANT - Performance Analyzer
Calculates Sharpe, Sortino, Max DD, Winrate, Profit Factor, etc.
"""
import pandas as pd
import numpy as np
from typing import Dict, List, Any
import logging

logger = logging.getLogger(__name__)

class PerformanceAnalyzer:
    """
    Professional performance metrics
    
    No fake promises - just real stats
    """
    
    @staticmethod
    def calculate_metrics(
        trades: List[Dict[str, Any]],
        equity_curve: pd.Series,
        risk_free_rate: float = 0.02
    ) -> Dict[str, Any]:
        """
        Calculate all performance metrics
        
        Args:
            trades: List of closed trades with pnl
            equity_curve: Series of equity over time
            risk_free_rate: Annual risk-free rate
        
        Returns:
            Dict with all metrics
        """
        if not trades or equity_curve.empty:
            return PerformanceAnalyzer._empty_metrics()
        
        # Basic
        total_trades = len(trades)
        pnls = [t["pnl"] for t in trades]
        wins = [p for p in pnls if p > 0]
        losses = [p for p in pnls if p <= 0]
        
        total_pnl = sum(pnls)
        winrate = len(wins) / total_trades * 100 if total_trades > 0 else 0
        
        avg_win = np.mean(wins) if wins else 0
        avg_loss = np.mean(losses) if losses else 0
        
        profit_factor = abs(sum(wins) / sum(losses)) if losses and sum(losses) != 0 else float('inf') if wins else 0
        
        # Equity curve metrics
        returns = equity_curve.pct_change().dropna()
        
        # Sharpe
        sharpe = PerformanceAnalyzer._sharpe_ratio(returns, risk_free_rate)
        
        # Sortino
        sortino = PerformanceAnalyzer._sortino_ratio(returns, risk_free_rate)
        
        # Max Drawdown
        max_dd, max_dd_duration = PerformanceAnalyzer._max_drawdown(equity_curve)
        
        # Calmar
        annual_return = PerformanceAnalyzer._annualized_return(equity_curve)
        calmar = annual_return / abs(max_dd) if max_dd != 0 else 0
        
        # Other
        avg_trade = np.mean(pnls) if pnls else 0
        best_trade = max(pnls) if pnls else 0
        worst_trade = min(pnls) if pnls else 0
        
        # Consecutive wins/losses
        max_consec_wins, max_consec_losses = PerformanceAnalyzer._consecutive_wins_losses(pnls)
        
        # Expectancy
        expectancy = (winrate/100 * avg_win) + ((1-winrate/100) * avg_loss) if total_trades > 0 else 0
        
        return {
            "total_trades": total_trades,
            "total_pnl": total_pnl,
            "total_pnl_percent": (equity_curve.iloc[-1] / equity_curve.iloc[0] - 1) * 100 if len(equity_curve) > 0 else 0,
            "winrate": winrate,
            "wins": len(wins),
            "losses": len(losses),
            "avg_win": avg_win,
            "avg_loss": avg_loss,
            "avg_trade": avg_trade,
            "best_trade": best_trade,
            "worst_trade": worst_trade,
            "profit_factor": profit_factor,
            "expectancy": expectancy,
            "sharpe_ratio": sharpe,
            "sortino_ratio": sortino,
            "calmar_ratio": calmar,
            "annual_return": annual_return * 100,
            "max_drawdown": max_dd * 100,
            "max_dd_duration": max_dd_duration,
            "max_consec_wins": max_consec_wins,
            "max_consec_losses": max_consec_losses,
            "final_equity": equity_curve.iloc[-1] if len(equity_curve) > 0 else 0,
            "initial_equity": equity_curve.iloc[0] if len(equity_curve) > 0 else 0
        }
    
    @staticmethod
    def _sharpe_ratio(returns: pd.Series, risk_free_rate: float, periods_per_year: int = 252) -> float:
        """Sharpe ratio"""
        if returns.empty or returns.std() == 0:
            return 0.0
        
        excess_returns = returns - risk_free_rate / periods_per_year
        return np.sqrt(periods_per_year) * excess_returns.mean() / excess_returns.std()
    
    @staticmethod
    def _sortino_ratio(returns: pd.Series, risk_free_rate: float, periods_per_year: int = 252) -> float:
        """Sortino ratio - only downside deviation"""
        if returns.empty:
            return 0.0
        
        excess_returns = returns - risk_free_rate / periods_per_year
        downside_returns = excess_returns[excess_returns < 0]
        
        if downside_returns.empty or downside_returns.std() == 0:
            return float('inf') if excess_returns.mean() > 0 else 0.0
        
        return np.sqrt(periods_per_year) * excess_returns.mean() / downside_returns.std()
    
    @staticmethod
    def _max_drawdown(equity: pd.Series) -> tuple[float, int]:
        """Max drawdown and duration"""
        if equity.empty:
            return 0.0, 0
        
        peak = equity.expanding().max()
        dd = (equity - peak) / peak
        
        max_dd = dd.min()
        
        # Duration: longest time from peak to recovery
        # Simplified: count bars in DD
        in_dd = dd < 0
        max_duration = 0
        current_duration = 0
        
        for is_dd in in_dd:
            if is_dd:
                current_duration += 1
                max_duration = max(max_duration, current_duration)
            else:
                current_duration = 0
        
        return max_dd, max_duration
    
    @staticmethod
    def _annualized_return(equity: pd.Series, periods_per_year: int = 252) -> float:
        """Annualized return"""
        if equity.empty or len(equity) < 2:
            return 0.0
        
        total_return = equity.iloc[-1] / equity.iloc[0] - 1
        years = len(equity) / periods_per_year
        
        if years == 0:
            return 0.0
        
        return (1 + total_return) ** (1 / years) - 1
    
    @staticmethod
    def _consecutive_wins_losses(pnls: List[float]) -> tuple[int, int]:
        """Max consecutive wins/losses"""
        max_wins = 0
        max_losses = 0
        current_wins = 0
        current_losses = 0
        
        for pnl in pnls:
            if pnl > 0:
                current_wins += 1
                current_losses = 0
                max_wins = max(max_wins, current_wins)
            else:
                current_losses += 1
                current_wins = 0
                max_losses = max(max_losses, current_losses)
        
        return max_wins, max_losses
    
    @staticmethod
    def _empty_metrics() -> Dict[str, Any]:
        return {
            "total_trades": 0,
            "total_pnl": 0,
            "total_pnl_percent": 0,
            "winrate": 0,
            "wins": 0,
            "losses": 0,
            "avg_win": 0,
            "avg_loss": 0,
            "avg_trade": 0,
            "best_trade": 0,
            "worst_trade": 0,
            "profit_factor": 0,
            "expectancy": 0,
            "sharpe_ratio": 0,
            "sortino_ratio": 0,
            "calmar_ratio": 0,
            "annual_return": 0,
            "max_drawdown": 0,
            "max_dd_duration": 0,
            "max_consec_wins": 0,
            "max_consec_losses": 0,
            "final_equity": 0,
            "initial_equity": 0
        }
    
    @staticmethod
    def print_report(metrics: Dict[str, Any]):
        """Print professional report"""
        print("\n" + "="*60)
        print("📊 ODIN QUANT - PERFORMANCE REPORT")
        print("="*60)
        print(f"Total Trades: {metrics['total_trades']} | Wins: {metrics['wins']} | Losses: {metrics['losses']}")
        print(f"Winrate: {metrics['winrate']:.2f}% | Profit Factor: {metrics['profit_factor']:.2f}")
        print(f"Total PnL: ${metrics['total_pnl']:.2f} ({metrics['total_pnl_percent']:.2f}%)")
        print(f"Avg Win: ${metrics['avg_win']:.2f} | Avg Loss: ${metrics['avg_loss']:.2f} | Expectancy: ${metrics['expectancy']:.2f}")
        print(f"Best: ${metrics['best_trade']:.2f} | Worst: ${metrics['worst_trade']:.2f}")
        print("-"*60)
        print(f"Sharpe: {metrics['sharpe_ratio']:.2f} | Sortino: {metrics['sortino_ratio']:.2f} | Calmar: {metrics['calmar_ratio']:.2f}")
        print(f"Annual Return: {metrics['annual_return']:.2f}% | Max DD: {metrics['max_drawdown']:.2f}% (Duration: {metrics['max_dd_duration']} bars)")
        print(f"Max Consec Wins: {metrics['max_consec_wins']} | Max Consec Losses: {metrics['max_consec_losses']}")
        print(f"Final Equity: ${metrics['final_equity']:.2f} (from ${metrics['initial_equity']:.2f})")
        print("="*60)
        print("⚠️ No guaranteed profit - Risk management is key to survival!")
        print("="*60 + "\n")
