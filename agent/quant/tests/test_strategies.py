"""
ODIN QUANT - Tests for all strategies
"""
import pytest
import pandas as pd
import numpy as np
import sys
import os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

from data.data_fetcher import DataFetcher
from strategies.trend_following import TrendFollowingStrategy
from strategies.mean_reversion import MeanReversionStrategy
from strategies.momentum_breakout import MomentumBreakoutStrategy
from strategies.pairs_trading import PairsTradingStrategy
from strategies.volatility_regime import VolatilityRegimeStrategy
from risk.risk_manager import RiskManager
from risk.position_sizing import PositionSizer
from backtest.backtester import Backtester
from backtest.performance import PerformanceAnalyzer

@pytest.fixture
def sample_data():
    fetcher = DataFetcher()
    return fetcher.fetch_ohlcv("BTC/USDT", "1h", limit=200)

@pytest.fixture
def config():
    return {
        "risk": {
            "max_risk_per_trade": 0.01,
            "max_daily_drawdown": 0.03,
            "max_total_drawdown": 0.15,
            "max_open_positions": 5,
            "max_correlation": 0.7,
            "position_sizing_method": "atr",
            "risk_free_rate": 0.02,
            "commission": 0.001,
            "slippage": 0.0005
        },
        "backtest": {
            "commission": 0.001,
            "slippage": 0.0005
        }
    }

def test_data_fetcher():
    fetcher = DataFetcher()
    df = fetcher.fetch_ohlcv("BTC/USDT", "1h", limit=100)
    assert len(df) >= 100  # May be 500 in synthetic fallback
    assert all(col in df.columns for col in ["timestamp", "open", "high", "low", "close", "volume"])
    assert df["high"].min() >= df["low"].min()

def test_trend_following(sample_data):
    strategy = TrendFollowingStrategy()
    signals = strategy.generate_signals(sample_data)
    assert "signal" in signals.columns
    assert "ema_fast" in signals.columns
    assert "adx" in signals.columns
    assert signals["signal"].isin([-1, 0, 1]).all()

def test_mean_reversion(sample_data):
    strategy = MeanReversionStrategy()
    signals = strategy.generate_signals(sample_data)
    assert "signal" in signals.columns
    assert "bb_upper" in signals.columns
    assert "rsi" in signals.columns

def test_momentum_breakout(sample_data):
    strategy = MomentumBreakoutStrategy()
    signals = strategy.generate_signals(sample_data)
    assert "signal" in signals.columns
    assert "high_lookback" in signals.columns

def test_pairs_trading(sample_data):
    strategy = PairsTradingStrategy()
    signals = strategy.generate_signals(sample_data)
    assert "signal" in signals.columns
    assert "zscore" in signals.columns

def test_volatility_regime(sample_data):
    strategy = VolatilityRegimeStrategy()
    signals = strategy.generate_signals(sample_data)
    assert "regime" in signals.columns
    assert signals["regime"].isin(["trending", "ranging", "high_vol"]).all()

def test_position_sizing():
    sizer = PositionSizer(method="atr", risk_per_trade=0.01)
    result = sizer.calculate_size(
        capital=10000,
        entry_price=100,
        sl_price=98,
        atr=1.0
    )
    assert result["size"] > 0
    assert result["risk_amount"] == 100  # 1% of 10000
    assert result["risk_percent"] == 1.0

def test_risk_manager(config):
    rm = RiskManager(config, initial_capital=10000)
    
    # Should allow first position
    can_open, reason = rm.can_open_position("BTC/USDT")
    assert can_open == True
    
    # Test daily DD kill-switch
    rm.daily_pnl = -400  # -4% daily
    rm.daily_start_capital = 10000
    can_open, reason = rm.can_open_position("BTC/USDT")
    assert can_open == False
    assert "Daily" in reason or "kill" in reason.lower()

def test_backtester(config, sample_data):
    strategy = TrendFollowingStrategy()
    backtester = Backtester(config, initial_capital=10000)
    result = backtester.run(sample_data, strategy, symbol="BTC/USDT")
    
    assert "trades" in result
    assert "equity_curve" in result
    assert "metrics" in result
    assert result["metrics"]["total_trades"] >= 0

def test_performance_analyzer():
    # Create dummy trades
    trades = [
        {"pnl": 100}, {"pnl": -50}, {"pnl": 200}, {"pnl": -30}, {"pnl": 150}
    ]
    equity = pd.Series([10000, 10100, 10050, 10250, 10220, 10370])
    
    metrics = PerformanceAnalyzer.calculate_metrics(trades, equity)
    
    assert metrics["total_trades"] == 5
    assert metrics["wins"] == 3
    assert metrics["losses"] == 2
    assert metrics["winrate"] == 60.0
    assert metrics["total_pnl"] == 370

def test_all_strategies_have_required_methods():
    strategies = [
        TrendFollowingStrategy(),
        MeanReversionStrategy(),
        MomentumBreakoutStrategy(),
        PairsTradingStrategy(),
        VolatilityRegimeStrategy()
    ]
    
    for strat in strategies:
        assert hasattr(strat, 'generate_signals')
        assert hasattr(strat, 'calculate_entry')
        assert hasattr(strat, 'calculate_exit')
        assert hasattr(strat, 'get_params')
        print(f"✅ {strat.name} has all required methods")
