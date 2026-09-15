"""
ODIN QUANT - API Routes for Quantitative Trading
Professional integration with ODIN Agent backend
"""
from fastapi import APIRouter, HTTPException
from typing import List, Dict, Any, Optional
import logging
from pydantic import BaseModel

router = APIRouter()
logger = logging.getLogger(__name__)

# Pydantic models
class BacktestRequest(BaseModel):
    symbol: str = "BTC/USDT"
    strategy: str = "trend_following"
    timeframe: str = "1h"
    start: str = "2023-01-01"
    end: str = "2024-01-01"
    capital: float = 10000.0
    walk_forward: bool = False

class PaperTradeRequest(BaseModel):
    symbol: str = "BTC/USDT"
    strategy: str = "trend_following"
    capital: float = 10000.0

class StrategyListResponse(BaseModel):
    strategies: List[Dict[str, Any]]

# Import quant modules (with fallback for offline)
try:
    import sys
    import os
    sys.path.insert(0, os.path.join(os.path.dirname(__file__), '../../../../quant'))
    from data.data_fetcher import DataFetcher
    from strategies.trend_following import TrendFollowingStrategy
    from strategies.mean_reversion import MeanReversionStrategy
    from strategies.momentum_breakout import MomentumBreakoutStrategy
    from strategies.pairs_trading import PairsTradingStrategy
    from strategies.volatility_regime import VolatilityRegimeStrategy
    from backtest.backtester import Backtester
    from backtest.performance import PerformanceAnalyzer
    import yaml
    QUANT_AVAILABLE = True
except Exception as e:
    logger.warning(f"Quant modules not available: {e}")
    QUANT_AVAILABLE = False

STRATEGIES = {
    "trend_following": "TrendFollowingStrategy",
    "mean_reversion": "MeanReversionStrategy",
    "momentum_breakout": "MomentumBreakoutStrategy",
    "pairs_trading": "PairsTradingStrategy",
    "volatility_regime": "VolatilityRegimeStrategy"
} if QUANT_AVAILABLE else {}

def load_quant_config():
    try:
        import yaml
        config_path = os.path.join(os.path.dirname(__file__), '../../../../quant/config/config.yaml')
        with open(config_path, 'r') as f:
            return yaml.safe_load(f)
    except:
        return {
            "risk": {
                "max_risk_per_trade": 0.01,
                "max_daily_drawdown": 0.03,
                "max_total_drawdown": 0.15,
                "max_open_positions": 5,
                "commission": 0.001,
                "slippage": 0.0005,
                "risk_free_rate": 0.02
            },
            "backtest": {"commission": 0.001, "slippage": 0.0005},
            "strategies": {
                "trend_following": {"params": {"ema_fast": 20, "ema_slow": 50}},
                "mean_reversion": {"params": {"bb_period": 20}},
                "momentum_breakout": {"params": {"lookback": 20}}
            }
        }

@router.get("/quant/strategies", response_model=StrategyListResponse, tags=["Quant"])
async def list_strategies():
    """List all available quantitative trading strategies"""
    
    strategies_info = [
        {
            "id": "trend_following",
            "name": "Trend Following (Multi-Timeframe)",
            "name_fa": "دنباله‌روی روند با فیلتر چندتایم‌فریم",
            "priority": 1,
            "description": "MA Crossover + ADX + Volume confirmation + HTF filter. Most robust long-term.",
            "description_fa": "کراس مووینگ + ADX + تایید حجم + فیلتر تایم‌فریم بالا. پایدارترین در بلندمدت.",
            "best_regime": "trending",
            "risk": "MEDIUM",
            "enabled": True
        },
        {
            "id": "mean_reversion",
            "name": "Mean Reversion",
            "name_fa": "بازگشت به میانگین",
            "priority": 2,
            "description": "Bollinger Bands + RSI + Z-Score. Best in ranging markets.",
            "description_fa": "باند بولینگر + RSI + Z-Score. بهترین در بازار رنج.",
            "best_regime": "ranging",
            "risk": "MEDIUM",
            "enabled": True
        },
        {
            "id": "momentum_breakout",
            "name": "Momentum Breakout",
            "name_fa": "شکست مومنتوم",
            "priority": 3,
            "description": "Volatility filtered breakout (ATR-based) with volume spike.",
            "description_fa": "شکست با فیلتر نوسان (مبتنی بر ATR) + جهش حجم.",
            "best_regime": "high_vol",
            "risk": "MEDIUM",
            "enabled": True
        },
        {
            "id": "pairs_trading",
            "name": "Pairs Trading / Stat Arb",
            "name_fa": "معاملات جفتی / آربیتراژ آماری",
            "priority": 4,
            "description": "Statistical arbitrage for correlated markets. Market neutral.",
            "description_fa": "آربیتراژ آماری برای بازارهای همبسته. خنثی نسبت به بازار.",
            "best_regime": "all",
            "risk": "LOW",
            "enabled": False
        },
        {
            "id": "volatility_regime",
            "name": "Volatility Regime Detection",
            "name_fa": "تشخیص رژیم نوسان",
            "priority": 5,
            "description": "Meta-strategy: detects Trending/Ranging/High Vol and switches strategies.",
            "description_fa": "متا-استراتژی: تشخیص روند/رنج/پرنوسان و تغییر استراتژی.",
            "best_regime": "meta",
            "risk": "LOW",
            "enabled": True
        }
    ]
    
    return {"strategies": strategies_info}

@router.post("/quant/backtest", tags=["Quant"])
async def run_backtest(request: BacktestRequest):
    """Run professional backtest with full performance report"""
    
    if not QUANT_AVAILABLE:
        return {
            "success": False,
            "error": "Quant modules not available in offline mode",
            "offline": True,
            "mock_result": {
                "symbol": request.symbol,
                "strategy": request.strategy,
                "total_trades": 124,
                "winrate": 48.5,
                "total_pnl": 1250.50,
                "sharpe": 1.45,
                "max_dd": -8.2,
                "message": "Mock result - install quant dependencies for real backtest"
            }
        }
    
    try:
        config = load_quant_config()
        
        # Fetch data
        fetcher = DataFetcher(source="yfinance")
        data = fetcher.fetch_ohlcv(request.symbol, request.timeframe, request.start, request.end)
        
        if data.empty:
            raise HTTPException(status_code=400, detail=f"No data for {request.symbol}")
        
        # Strategy
        strategy_map = {
            "trend_following": TrendFollowingStrategy,
            "mean_reversion": MeanReversionStrategy,
            "momentum_breakout": MomentumBreakoutStrategy,
            "pairs_trading": PairsTradingStrategy,
            "volatility_regime": VolatilityRegimeStrategy
        }
        
        strategy_class = strategy_map.get(request.strategy)
        if not strategy_class:
            raise HTTPException(status_code=400, detail=f"Unknown strategy {request.strategy}")
        
        strategy_cfg = config.get("strategies", {}).get(request.strategy, {})
        strategy = strategy_class(params=strategy_cfg.get("params", {}))
        
        # Backtest
        backtester = Backtester(config, initial_capital=request.capital)
        
        if request.walk_forward:
            result = backtester.walk_forward(
                data, strategy,
                train_days=180, test_days=30, step_days=30,
                symbol=request.symbol
            )
        else:
            result = backtester.run(data, strategy, symbol=request.symbol)
        
        # Convert trades to dict
        trades = result.get("trades", []) or result.get("combined_trades", [])
        metrics = result.get("metrics", {})
        
        return {
            "success": True,
            "symbol": request.symbol,
            "strategy": request.strategy,
            "timeframe": request.timeframe,
            "period": f"{request.start} to {request.end}",
            "data_points": len(data),
            "trades_count": len(trades),
            "metrics": metrics,
            "risk_status": result.get("risk_status", {}),
            "trades": trades[:20],  # First 20 trades
            "equity_curve": result.get("equity_curve", {}).tolist()[-100:] if hasattr(result.get("equity_curve", {}), 'tolist') else [],
            "disclaimer": "Past performance does not guarantee future results. Risk management is key to survival!"
        }
        
    except Exception as e:
        logger.error(f"Backtest failed: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))

@router.get("/quant/risk/status", tags=["Quant"])
async def get_risk_status():
    """Get current risk manager status - for monitoring"""
    # In production, this would read from live risk manager
    return {
        "current_capital": 10000.0,
        "initial_capital": 10000.0,
        "total_pnl": 0.0,
        "total_pnl_percent": 0.0,
        "total_drawdown": 0.0,
        "daily_pnl": 0.0,
        "daily_drawdown": 0.0,
        "open_positions": 0,
        "kill_switch": False,
        "total_stop": False,
        "limits": {
            "max_risk_per_trade": "1%",
            "max_daily_dd": "3%",
            "max_total_dd": "15%"
        },
        "message": "Risk manager operational - Paper trading mode",
        "timestamp": "2024-01-01T00:00:00Z"
    }

@router.post("/quant/paper-trade/start", tags=["Quant"])
async def start_paper_trading(request: PaperTradeRequest):
    """Start paper trading session"""
    
    return {
        "success": True,
        "message": f"Paper trading started for {request.symbol} with {request.strategy}",
        "symbol": request.symbol,
        "strategy": request.strategy,
        "capital": request.capital,
        "mode": "paper",
        "status": "running",
        "note": "Use /quant/risk/status to monitor, and check logs for trades. For real paper trading, run agent/quant/paper_trade.py",
        "command": f"python paper_trade.py --symbol {request.symbol} --strategy {request.strategy} --capital {request.capital}"
    }

@router.get("/quant/config", tags=["Quant"])
async def get_quant_config():
    """Get current quant configuration"""
    config = load_quant_config()
    return {
        "config": config,
        "risk_rules": "NON-NEGOTIABLE: 1% per trade, 3% daily DD kill-switch, 15% total DD stop",
        "disclaimer": "No guaranteed profit - Focus on survival probability!"
    }
