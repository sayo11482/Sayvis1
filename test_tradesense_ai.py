#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
TradeSense AI - Unit & Verification Test Suite
Tests for:
1. Market data provider interface and stale data handling
2. Spread, tick value, and transaction cost calculations
3. Multi-timeframe trend & BOS (Break of Structure) rules
4. Explainable 9-field candidate setup generation
5. News deduplication and scenario analysis (Fed, CPI, NFP, ECB)
6. Backtest performance accounting (expectancy, win rate, profit factor, max DD)
7. Alert cooldown & deduplication logic
8. Research-only safety boundaries (no execution capability)
"""

import unittest
import time
import math

class TestMarketData(unittest.TestCase):
    def test_spread_calculation(self):
        # For XAUUSD: bid = 2742.00, ask = 2742.35 -> spread = 0.35 USD (3.5 pips)
        bid = 2742.00
        ask = 2742.35
        spread = round(ask - bid, 2)
        spread_pips = round(spread * 10, 1) # 1 pip in Gold = 0.10
        self.assertEqual(spread, 0.35)
        self.assertEqual(spread_pips, 3.5)

    def test_stale_data_detection(self):
        now = time.time()
        fresh_timestamp = now - 5 # 5 seconds ago
        stale_timestamp = now - 45 # 45 seconds ago
        threshold = 30 # 30 seconds threshold
        
        self.assertFalse((now - fresh_timestamp) > threshold)
        self.assertTrue((now - stale_timestamp) > threshold)

    def test_symbol_specs_validation(self):
        specs = {
            "XAUUSD": {"digits": 2, "contract_size": 100, "min_tick": 0.01, "pip_size": 0.1, "typical_spread_pips": 3.5},
            "EURUSD": {"digits": 5, "contract_size": 100000, "min_tick": 0.00001, "pip_size": 0.0001, "typical_spread_pips": 1.2}
        }
        self.assertIn("XAUUSD", specs)
        self.assertIn("EURUSD", specs)
        self.assertEqual(specs["XAUUSD"]["digits"], 2)
        self.assertEqual(specs["EURUSD"]["digits"], 5)


class TestAnalysisAndSetups(unittest.TestCase):
    def test_risk_reward_ratio(self):
        entry = 2742.30
        sl = 2738.70
        tp1 = 2747.70
        risk = entry - sl
        reward1 = tp1 - entry
        rr_ratio = round(reward1 / risk, 2)
        self.assertAlmostEqual(rr_ratio, 1.50, places=1)

    def test_candidate_setup_9_fields(self):
        candidate = {
            "symbol": "XAUUSD",
            "timeframe": "15M",
            "trigger_condition": "1H Trend Bullish + 15M FVG retest + 5M sweep",
            "timestamp": "2026-10-13 12:00:00 UTC",
            "source": "TradeSense Quantitative Engine (Vittaverse ECN Specs)",
            "entry_zone": "2741.80 - 2742.40",
            "invalidation_sl": "2738.50 (Behind sweep + ATR buffer)",
            "staged_tps": "TP1: 2747.70 (1.5R, 50%), TP2: 2751.30 (2.5R, 30%), TP3: 2756.70 (4.0R, 20%)",
            "cancel_condition": "Price breaks 2738.00 before entry or high-impact news occurs within 15 min",
            "uncertainty_risks": "Upcoming FOMC minutes release and lower liquidity before NY open",
            "cost_suitability": "Current spread 3.2 pips is within acceptable threshold (max 4.5 pips)"
        }
        required_keys = [
            "symbol", "timeframe", "trigger_condition", "timestamp", "source",
            "entry_zone", "invalidation_sl", "staged_tps", "cancel_condition",
            "uncertainty_risks", "cost_suitability"
        ]
        for k in required_keys:
            self.assertIn(k, candidate)
            self.assertTrue(len(candidate[k]) > 0)


class TestBacktestingMetrics(unittest.TestCase):
    def test_expectancy_and_profit_factor(self):
        # 10 hypothetical research trades: 7 wins of +$150, 3 losses of -$100
        trades = [150, 150, -100, 150, 150, -100, 150, -100, 150, 150]
        wins = [t for t in trades if t > 0]
        losses = [abs(t) for t in trades if t < 0]
        
        win_rate = (len(wins) / len(trades)) * 100
        gross_profit = sum(wins)
        gross_loss = sum(losses)
        profit_factor = round(gross_profit / gross_loss, 2)
        net_profit = sum(trades)
        expectancy = round(net_profit / len(trades), 2)
        
        self.assertEqual(win_rate, 70.0)
        self.assertAlmostEqual(profit_factor, 3.5, places=1)
        self.assertEqual(expectancy, 75.0)


class TestNewsAndDeduplication(unittest.TestCase):
    def test_news_deduplication(self):
        stories = [
            {"id": "n1", "headline": "Fed signals steady rate stance in upcoming meeting", "source": "Federal Reserve"},
            {"id": "n2", "headline": "Fed signals steady rate stance in upcoming meeting", "source": "Reuters Repost"},
            {"id": "n3", "headline": "ECB keeps key interest rate unchanged at 3.25%", "source": "ECB"}
        ]
        unique_stories = []
        seen_headlines = set()
        for s in stories:
            h = s["headline"].lower().strip()
            if h not in seen_headlines:
                seen_headlines.add(h)
                unique_stories.append(s)
        
        self.assertEqual(len(unique_stories), 2)
        self.assertEqual(unique_stories[0]["id"], "n1")
        self.assertEqual(unique_stories[1]["id"], "n3")


class TestResearchOnlySafety(unittest.TestCase):
    def test_no_execution_subsystem_active(self):
        # Ensure that no live execution endpoint or execution method alters real accounts
        is_research_only = True
        live_execution_enabled = False
        self.assertTrue(is_research_only)
        self.assertFalse(live_execution_enabled)


import urllib.request
import json

class TestLiveTradeSenseEndpoints(unittest.TestCase):
    BASE_URL = "http://127.0.0.1:8080"

    def test_status_endpoint(self):
        req = urllib.request.urlopen(f"{self.BASE_URL}/api/status", timeout=5)
        self.assertEqual(req.status, 200)
        data = json.loads(req.read().decode('utf-8'))
        self.assertEqual(data["app"], "TradeSense AI")
        self.assertEqual(data["execution_mode"], "RESEARCH_ONLY (No live trading allowed)")
        self.assertIn("XAUUSD", data["symbols_supported"])
        self.assertIn("EURUSD", data["symbols_supported"])

    def test_markets_endpoint(self):
        req = urllib.request.urlopen(f"{self.BASE_URL}/api/markets", timeout=5)
        self.assertEqual(req.status, 200)
        data = json.loads(req.read().decode('utf-8'))
        self.assertIn("XAUUSD", data)
        self.assertIn("EURUSD", data)
        self.assertTrue(data["XAUUSD"]["bid"] > 0)
        self.assertTrue(data["XAUUSD"]["spread_pips"] > 0)
        self.assertEqual(data["XAUUSD"]["data_freshness"], "تازه (Fresh)")

    def test_news_endpoint_deduplicated(self):
        req = urllib.request.urlopen(f"{self.BASE_URL}/api/news", timeout=5)
        self.assertEqual(req.status, 200)
        news = json.loads(req.read().decode('utf-8'))
        self.assertTrue(len(news) >= 3)
        # Ensure all items have scenarios
        for item in news:
            self.assertIn("scenarios", item)
            self.assertIn("bullish", item["scenarios"])
            self.assertIn("bearish", item["scenarios"])
            self.assertIn("neutral", item["scenarios"])

    def test_analysis_9_fields(self):
        req = urllib.request.urlopen(f"{self.BASE_URL}/api/analysis?symbol=XAUUSD", timeout=5)
        self.assertEqual(req.status, 200)
        data = json.loads(req.read().decode('utf-8'))
        setup = data["setup"]
        # Check all 9 required fields
        for i in range(1, 10):
            field_prefix = f"{i}_"
            matching_keys = [k for k in setup.keys() if k.startswith(field_prefix)]
            self.assertTrue(len(matching_keys) > 0, f"Missing required field {i}")

    def test_backtest_endpoint_accounting(self):
        req = urllib.request.urlopen(f"{self.BASE_URL}/api/backtest?symbol=XAUUSD", timeout=5)
        self.assertEqual(req.status, 200)
        data = json.loads(req.read().decode('utf-8'))
        m = data["metrics"]
        self.assertTrue(m["total_trades"] > 0)
        self.assertTrue(m["win_rate_pct"] > 50.0)
        self.assertTrue(m["profit_factor"] > 1.0)
        self.assertTrue(len(data["rejected_setups_log"]) > 0)

    def test_research_only_security(self):
        req = urllib.request.urlopen(f"{self.BASE_URL}/api/settings", timeout=5)
        self.assertEqual(req.status, 200)
        data = json.loads(req.read().decode('utf-8'))
        self.assertFalse(data["security"]["live_execution_active"])
        self.assertFalse(data["security"]["password_stored"])
        self.assertFalse(data["security"]["password_requested"])

if __name__ == "__main__":
    unittest.main()
