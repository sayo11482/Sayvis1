#!/usr/bin/env python3
"""
ODIN QUANT - Paper Trading Script
Simulates live trading

Usage:
    python paper_trade.py --config config/config.yaml --symbol BTC/USDT
"""
import argparse
import yaml
import time
import logging
from datetime import datetime

from data.data_fetcher import DataFetcher
from strategies.trend_following import TrendFollowingStrategy
from strategies.mean_reversion import MeanReversionStrategy
from strategies.momentum_breakout import MomentumBreakoutStrategy
from execution.paper_executor import PaperExecutor
from alerts.telegram_alerter import TelegramAlerter

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

STRATEGIES = {
    "trend_following": TrendFollowingStrategy,
    "mean_reversion": MeanReversionStrategy,
    "momentum_breakout": MomentumBreakoutStrategy
}

def load_config(path: str) -> dict:
    with open(path, 'r') as f:
        return yaml.safe_load(f)

def main():
    parser = argparse.ArgumentParser(description="ODIN QUANT Paper Trading")
    parser.add_argument("--config", default="config/config.yaml")
    parser.add_argument("--symbol", default="BTC/USDT")
    parser.add_argument("--strategy", default="trend_following", choices=list(STRATEGIES.keys()))
    parser.add_argument("--capital", type=float, default=10000.0)
    parser.add_argument("--interval", type=int, default=60, help="Check interval seconds")
    
    args = parser.parse_args()
    
    print(f"\n{'='*70}")
    print(f"🔥 ODIN QUANT - PAPER TRADING")
    print(f"Symbol: {args.symbol} | Strategy: {args.strategy} | Capital: ${args.capital}")
    print(f"{'='*70}\n")
    
    config = load_config(args.config)
    fetcher = DataFetcher(source="yfinance")
    
    # Strategy
    strategy_cfg = config.get("strategies", {}).get(args.strategy, {})
    strategy = STRATEGIES[args.strategy](params=strategy_cfg.get("params", {}))
    
    # Executor
    executor = PaperExecutor(config, initial_capital=args.capital)
    
    # Alerter
    alerts_cfg = config.get("alerts", {}).get("telegram", {})
    alerter = TelegramAlerter(
        bot_token=alerts_cfg.get("bot_token"),
        chat_id=alerts_cfg.get("chat_id"),
        enabled=alerts_cfg.get("enabled", False)
    )
    
    print("📝 Starting paper trading loop (Ctrl+C to stop)...\n")
    
    try:
        while True:
            # Fetch latest data
            data = fetcher.fetch_ohlcv(args.symbol, "1h", limit=100)
            
            if data.empty:
                print("❌ No data, waiting...")
                time.sleep(args.interval)
                continue
            
            # Generate signals
            signals = strategy.generate_signals(data)
            latest = signals.iloc[-1]
            
            current_price = latest["close"]
            high = latest["high"]
            low = latest["low"]
            
            # Check exits
            closed = executor.check_exits(args.symbol, current_price, high, low)
            for trade in closed:
                alerter.alert_exit(args.symbol, trade["pnl"], trade["exit_reason"])
            
            # Check entries
            signal = latest.get("signal", 0)
            if signal != 0:
                sl = latest.get("sl_price", current_price * 0.98)
                tp = latest.get("tp_price", current_price * 1.03)
                reason = latest.get("reason", "")
                
                result = executor.place_order(
                    symbol=args.symbol,
                    side=signal,
                    entry_price=current_price,
                    sl_price=sl,
                    tp_price=tp,
                    reason=reason,
                    strategy=strategy.name
                )
                
                if result["success"]:
                    alerter.alert_entry(args.symbol, signal, current_price, result["size"], reason)
                    print(f"✅ New position: {args.symbol} {'LONG' if signal==1 else 'SHORT'} @ {current_price:.2f}")
                else:
                    print(f"❌ Order blocked: {result['error']}")
            
            # Status
            status = executor.get_status()
            print(f"[{datetime.now().strftime('%H:%M:%S')}] {args.symbol} ${current_price:.2f} | "
                  f"Capital ${status['current_capital']:.2f} | Daily ${status['daily_pnl']:.2f} | "
                  f"Open {status['open_positions']} | Signal {signal}")
            
            # Check kill-switch
            if status["kill_switch"]:
                print("🔴 KILL-SWITCH ACTIVE - Daily DD exceeded, no new trades!")
                alerter.alert_kill_switch(status["daily_pnl"], status["daily_drawdown"])
            
            if status["total_stop"]:
                print("💀 TOTAL STOP - Max DD exceeded, stopping!")
                break
            
            time.sleep(args.interval)
            
    except KeyboardInterrupt:
        print("\n\n🛑 Paper trading stopped by user")
        status = executor.get_status()
        print(f"\nFinal Status:")
        for k, v in status.items():
            if k != "open_positions_detail":
                print(f"  {k}: {v}")
        
        alerter.alert_daily_summary(status)

if __name__ == "__main__":
    main()
