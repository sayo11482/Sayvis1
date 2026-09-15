#!/usr/bin/env python3
"""
ODIN QUANT - Backtest Script
Professional backtest with full report

Usage:
    python backtest_script.py --config config/config.yaml --strategy trend_following --symbol BTC/USDT --start 2023-01-01 --end 2024-01-01
"""
import argparse
import yaml
import pandas as pd
import logging
from datetime import datetime

from data.data_fetcher import DataFetcher
from strategies.trend_following import TrendFollowingStrategy
from strategies.mean_reversion import MeanReversionStrategy
from strategies.momentum_breakout import MomentumBreakoutStrategy
from strategies.pairs_trading import PairsTradingStrategy
from strategies.volatility_regime import VolatilityRegimeStrategy
from backtest.backtester import Backtester
from backtest.performance import PerformanceAnalyzer

logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(name)s - %(levelname)s - %(message)s")
logger = logging.getLogger(__name__)

STRATEGIES = {
    "trend_following": TrendFollowingStrategy,
    "mean_reversion": MeanReversionStrategy,
    "momentum_breakout": MomentumBreakoutStrategy,
    "pairs_trading": PairsTradingStrategy,
    "volatility_regime": VolatilityRegimeStrategy
}

def load_config(path: str) -> dict:
    with open(path, 'r') as f:
        return yaml.safe_load(f)

def main():
    parser = argparse.ArgumentParser(description="ODIN QUANT Backtest")
    parser.add_argument("--config", default="config/config.yaml", help="Config file")
    parser.add_argument("--strategy", default="trend_following", choices=list(STRATEGIES.keys()), help="Strategy")
    parser.add_argument("--symbol", default="BTC/USDT", help="Symbol")
    parser.add_argument("--timeframe", default="1h", help="Timeframe")
    parser.add_argument("--start", default="2023-01-01", help="Start date")
    parser.add_argument("--end", default="2024-01-01", help="End date")
    parser.add_argument("--capital", type=float, default=10000.0, help="Initial capital")
    parser.add_argument("--walk-forward", action="store_true", help="Walk-forward analysis")
    
    args = parser.parse_args()
    
    print(f"\n{'='*70}")
    print(f"🔥 ODIN QUANT - BACKTEST")
    print(f"Strategy: {args.strategy} | Symbol: {args.symbol} | TF: {args.timeframe}")
    print(f"Period: {args.start} to {args.end} | Capital: ${args.capital}")
    print(f"{'='*70}\n")
    
    # Load config
    config = load_config(args.config)
    
    # Fetch data
    print("📥 Fetching data...")
    fetcher = DataFetcher(source="yfinance")
    data = fetcher.fetch_ohlcv(args.symbol, args.timeframe, args.start, args.end)
    print(f"✅ Fetched {len(data)} candles")
    print(f"   From {data.iloc[0]['timestamp']} to {data.iloc[-1]['timestamp']}")
    print(f"   Price range: ${data['low'].min():.2f} - ${data['high'].max():.2f}\n")
    
    # Strategy
    strategy_cfg = config.get("strategies", {}).get(args.strategy, {})
    params = strategy_cfg.get("params", {})
    strategy = STRATEGIES[args.strategy](params=params)
    print(f"🧠 Strategy: {strategy.name} with params: {params}\n")
    
    # Backtest
    backtester = Backtester(config, initial_capital=args.capital)
    
    if args.walk_forward:
        print("🔄 Running Walk-Forward Analysis...")
        wf_cfg = config.get("backtest", {}).get("walk_forward", {})
        result = backtester.walk_forward(
            data, strategy,
            train_days=wf_cfg.get("train_days", 180),
            test_days=wf_cfg.get("test_days", 30),
            step_days=wf_cfg.get("step_days", 30),
            symbol=args.symbol
        )
        print(f"\n✅ Walk-Forward completed with {result.get('num_windows', 0)} windows")
        metrics = result.get("metrics", {})
    else:
        print("📊 Running Backtest...")
        result = backtester.run(data, strategy, symbol=args.symbol)
        metrics = result.get("metrics", {})
    
    # Report
    PerformanceAnalyzer.print_report(metrics)
    
    # Risk status
    risk_status = result.get("risk_status", {})
    print("\n🛡️ Risk Status:")
    for k, v in risk_status.items():
        if k != "open_positions_detail":
            print(f"   {k}: {v}")
    
    # Save trades
    trades = result.get("trades", []) or result.get("combined_trades", [])
    if trades:
        df_trades = pd.DataFrame(trades)
        out_file = f"backtest_{args.strategy}_{args.symbol.replace('/', '_')}_{args.start}_{args.end}.csv"
        df_trades.to_csv(out_file, index=False)
        print(f"\n💾 Trades saved to {out_file} ({len(trades)} trades)")
    
    print("\n⚠️ Disclaimer: Past performance does not guarantee future results!")
    print("   Always paper trade first, risk management is key to survival.\n")

if __name__ == "__main__":
    main()
