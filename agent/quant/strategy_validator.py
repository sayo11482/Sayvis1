"""
ODIN - Strategy Validator: Ban if 5 backtests $10 < $15
- هر استراتژی 5 بار بک‌تست با سرمایه 10 دلار
- اگر میانگین یا هر 5 تا زیر 15 دلار → ممنوع
- تمام استراتژی‌ها دائم در حال بک‌تست برای تحلیل قدرت
"""

import random
import statistics
from typing import Dict, List

class MockStrategy:
    def __init__(self, name, cls_name, wr_range, rr_range):
        self.name = name
        self.cls_name = cls_name
        self.wr_range = wr_range
        self.rr_range = rr_range

class StrategyValidator:
    def __init__(self, initial_capital: float = 10.0, min_final: float = 15.0, num_tests: int = 5):
        self.initial_capital = initial_capital
        self.min_final = min_final
        self.num_tests = num_tests
        self.banned_strategies: Dict[str, Dict] = {}
        self.valid_strategies: Dict[str, Dict] = {}
        
    def run_single_backtest(self, strategy: MockStrategy) -> float:
        """Simulate backtest based on strategy realistic WR and RR"""
        wr = random.uniform(*strategy.wr_range) / 100.0
        rr = random.uniform(*strategy.rr_range)
        # Simulate 20 trades
        trades = []
        for _ in range(20):
            win = random.random() < wr
            if win:
                trades.append(rr)  # +RR
            else:
                trades.append(-1.0)  # -1
        # Apply risk 1% per trade, compound
        capital = self.initial_capital
        for r in trades:
            risk_amount = capital * 0.01
            if r > 0:
                capital += risk_amount * r
            else:
                capital += risk_amount * r
        # Add some noise
        capital *= random.uniform(0.9, 1.1)
        return max(0.1, capital)
    
    def validate_strategy(self, strategy: MockStrategy) -> Dict:
        print(f"\n🔍 تست {strategy.name} - {strategy.cls_name} - {self.num_tests} بک‌تست با ${self.initial_capital}")
        
        finals = []
        for i in range(self.num_tests):
            final = self.run_single_backtest(strategy)
            finals.append(final)
            status = "✅" if final >= self.min_final else "❌"
            print(f"  تست {i+1}: ${final:.2f} {status} (حداقل ${self.min_final})")
        
        avg_final = statistics.mean(finals)
        min_final = min(finals)
        max_final = max(finals)
        winrate_tests = sum(1 for f in finals if f >= self.min_final) / len(finals) * 100
        
        is_banned = avg_final < self.min_final or min_final < self.min_final or winrate_tests < 60
        
        result = {
            'strategy': strategy.name,
            'class': strategy.cls_name,
            'finals': finals,
            'avg_final': avg_final,
            'min_final': min_final,
            'max_final': max_final,
            'winrate_tests': winrate_tests,
            'is_banned': is_banned,
            'reason': f"Avg ${avg_final:.2f} < ${self.min_final} or Min ${min_final:.2f} < ${self.min_final} or WR {winrate_tests:.0f}% <60%" if is_banned else f"Avg ${avg_final:.2f} >= ${self.min_final} and WR {winrate_tests:.0f}% >=60%"
        }
        
        if is_banned:
            self.banned_strategies[strategy.name] = result
            print(f"  🚫 ممنوع: {result['reason']}")
        else:
            self.valid_strategies[strategy.name] = result
            print(f"  ✅ مجاز: {result['reason']}")
        
        return result
    
    def validate_all(self) -> Dict:
        strategies = [
            MockStrategy("tv_80_percent", "TV80PercentStrategy", (70, 85), (2.0, 3.0)),
            MockStrategy("lit_liquidity_inversion", "LITStrategy", (50, 65), (2.0, 2.8)),
            MockStrategy("trend_following", "TrendFollowingStrategy", (35, 50), (1.5, 2.5)),
            MockStrategy("mean_reversion", "MeanReversionStrategy", (45, 60), (1.2, 2.0)),
            MockStrategy("momentum_breakout", "MomentumBreakoutStrategy", (30, 45), (1.8, 3.0)),
            MockStrategy("pairs_trading", "PairsTradingStrategy", (50, 65), (1.0, 1.5)),
            MockStrategy("volatility_regime", "VolatilityRegimeStrategy", (40, 55), (1.3, 2.2)),
        ]
        
        print("="*80)
        print(f"🔥 ODIN - اعتبارسنجی: 5 بک‌تست ${self.initial_capital} → حداقل ${self.min_final} وگرنه ممنوع")
        print("="*80)
        
        results = []
        for strat in strategies:
            res = self.validate_strategy(strat)
            results.append(res)
        
        print("\n" + "="*80)
        print("📊 خلاصه:")
        print(f"  ✅ مجاز: {len(self.valid_strategies)} - {list(self.valid_strategies.keys())}")
        print(f"  🚫 ممنوع: {len(self.banned_strategies)} - {list(self.banned_strategies.keys())}")
        print("="*80)
        
        return {
            'results': results,
            'valid': self.valid_strategies,
            'banned': self.banned_strategies
        }

class ContinuousBacktester:
    def __init__(self, validator: StrategyValidator):
        self.validator = validator
        self.history: List[Dict] = []
        
    def run_continuous(self, iterations: int = 5):
        print("\n🔄 بک‌تست دائمی - تحلیل قدرت استراتژی‌ها")
        print("-"*80)
        
        for iter in range(iterations):
            print(f"\n🔄 دور {iter+1}/{iterations} - بک‌تست دائمی")
            result = self.validator.validate_all()
            self.history.append({
                'iteration': iter+1,
                'valid_count': len(result['valid']),
                'banned_count': len(result['banned']),
                'valid': list(result['valid'].keys()),
                'banned': list(result['banned'].keys())
            })
        
        print("\n📈 تحلیل پایداری:")
        all_names = ["tv_80_percent", "lit_liquidity_inversion", "trend_following", "mean_reversion", "momentum_breakout", "pairs_trading", "volatility_regime"]
        for strat_name in all_names:
            valid_times = sum(1 for h in self.history if strat_name in h['valid'])
            total = len(self.history)
            stability = valid_times / total * 100 if total > 0 else 0
            status = "✅ پایدار" if stability >= 80 else "⚠️ ناپایدار" if stability >= 50 else "🚫 ضعیف"
            print(f"  {strat_name}: {valid_times}/{total} = {stability:.0f}% {status}")

if __name__ == "__main__":
    validator = StrategyValidator(initial_capital=10.0, min_final=15.0, num_tests=5)
    results = validator.validate_all()
    cont = ContinuousBacktester(validator)
    cont.run_continuous(iterations=3)
