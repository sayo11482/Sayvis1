#!/usr/bin/env python3
"""
TradeSense AI - Deep Multi-Disciplinary Professional Audit Suite
Exhaustively testing:
1. PWA & Mobile Installation Manifest & Service Worker
2. Stale Data Lifecycle & Connection Recovery
3. Independent Mathematical Quant Calculation Verification
4. 9-Field Explainability & Financial Setup Consistency
5. Walk-Forward Backtesting Cost & Bias Verification
6. Cybersecurity, XSS Sanitization & DoS Payload Guards
7. Practice Journal Validation & Client Feedback
"""

import unittest
import urllib.request
import urllib.error
import json
import math
import os
import sys

BASE_URL = "http://127.0.0.1:8080"

# Import server quant functions for direct unit testing
sys.path.insert(0, os.path.abspath("web_dashboard"))
from server import calculate_ema, calculate_rsi, calculate_atr, detect_market_structure

class TestPWAAndMobileReadiness(unittest.TestCase):
    """PWA, Service Worker & Mobile Shell Verification"""

    def test_pwa_manifest_json(self):
        req = urllib.request.urlopen(f"{BASE_URL}/manifest.json", timeout=5)
        self.assertEqual(req.status, 200)
        self.assertIn("application/manifest+json", req.headers.get("Content-Type", ""))
        data = json.loads(req.read().decode('utf-8'))
        self.assertEqual(data["name"], "TradeSense AI — Market Research Terminal")
        self.assertEqual(data["short_name"], "TradeSense AI")
        self.assertEqual(data["display"], "standalone")
        self.assertEqual(data["dir"], "rtl")
        self.assertEqual(data["lang"], "fa")
        self.assertTrue(len(data["icons"]) > 0)

    def test_pwa_service_worker(self):
        req = urllib.request.urlopen(f"{BASE_URL}/sw.js", timeout=5)
        self.assertEqual(req.status, 200)
        self.assertIn("application/javascript", req.headers.get("Content-Type", ""))
        content = req.read().decode('utf-8')
        self.assertIn("tradesense-cache-v1", content)
        self.assertIn("addEventListener('install'", content)
        self.assertIn("addEventListener('fetch'", content)

    def test_pwa_icon_svg(self):
        req = urllib.request.urlopen(f"{BASE_URL}/icon.svg", timeout=5)
        self.assertEqual(req.status, 200)
        self.assertIn("image/svg+xml", req.headers.get("Content-Type", ""))
        content = req.read().decode('utf-8')
        self.assertIn("<svg", content)
        self.assertIn("TRADESENSE", content)


class TestStaleDataAndConnectivityLifecycle(unittest.TestCase):
    """Market Data Freshness, Staleness Simulation & Connectivity Transparency"""

    def test_stale_data_simulation_and_recovery(self):
        # 1. Enable stale simulation
        req = urllib.request.urlopen(f"{BASE_URL}/api/test/stale?enable=1", timeout=5)
        self.assertEqual(req.status, 200)

        # 2. Query markets endpoint
        req_m = urllib.request.urlopen(f"{BASE_URL}/api/markets", timeout=5)
        data_stale = json.loads(req_m.read().decode('utf-8'))
        self.assertTrue(data_stale["XAUUSD"]["is_stale"])
        self.assertEqual(data_stale["XAUUSD"]["data_freshness"], "منقضی (Stale)")
        self.assertTrue(data_stale["XAUUSD"]["age_seconds"] >= 30.0)

        # 3. Disable stale simulation (recovery)
        req_rec = urllib.request.urlopen(f"{BASE_URL}/api/test/stale?enable=0", timeout=5)
        self.assertEqual(req_rec.status, 200)

        # 4. Query markets endpoint again
        req_fresh = urllib.request.urlopen(f"{BASE_URL}/api/markets", timeout=5)
        data_fresh = json.loads(req_fresh.read().decode('utf-8'))
        self.assertFalse(data_fresh["XAUUSD"]["is_stale"])
        self.assertEqual(data_fresh["XAUUSD"]["data_freshness"], "تازه (Fresh)")
        self.assertTrue(data_fresh["XAUUSD"]["age_seconds"] < 10.0)

    def test_connectivity_audit_transparency(self):
        req = urllib.request.urlopen(f"{BASE_URL}/api/connectivity", timeout=5)
        self.assertEqual(req.status, 200)
        audit = json.loads(req.read().decode('utf-8'))
        self.assertFalse(audit["is_real_broker_connected"])
        self.assertIn("BLOCKED", audit["live_broker_socket"])
        self.assertIn("TradeSense Calibrated ECN Research Adapter", audit["active_data_adapter"])
        self.assertTrue(audit["is_model_calibrated"])


class TestIndependentQuantMathVerification(unittest.TestCase):
    """Independent Mathematical Verification of Technical Algorithms"""

    def test_ema_mathematical_precision(self):
        # Independent manual calculation for values [10, 11, 12, 13, 14], period = 3
        # SMA(3) for [10, 11, 12] = 11.0
        # Multiplier = 2 / (3 + 1) = 0.5
        # EMA_4 (val 13) = 13 * 0.5 + 11.0 * 0.5 = 12.0
        # EMA_5 (val 14) = 14 * 0.5 + 12.0 * 0.5 = 13.0
        series = [10.0, 11.0, 12.0, 13.0, 14.0]
        ema_result = calculate_ema(series, period=3)
        self.assertEqual(len(ema_result), len(series))
        self.assertAlmostEqual(ema_result[-1], 13.0, places=4)
        self.assertAlmostEqual(ema_result[-2], 12.0, places=4)

    def test_rsi_boundary_cases(self):
        # Monotonically increasing series -> All gains, zero losses -> RSI = 100.0
        increasing = [float(i) for i in range(1, 30)]
        rsi_bull = calculate_rsi(increasing, period=14)
        self.assertEqual(rsi_bull, 100.0)

        # Monotonically decreasing series -> All losses, zero gains -> RSI = 0.0
        decreasing = [float(30 - i) for i in range(1, 30)]
        rsi_bear = calculate_rsi(decreasing, period=14)
        self.assertEqual(rsi_bear, 0.0)

    def test_atr_volatility_calculation(self):
        candles = [
            {"high": 105.0, "low": 95.0, "close": 100.0}, # TR = 10.0
            {"high": 110.0, "low": 98.0, "close": 108.0}, # TR = max(12, |110-100|, |98-100|) = 12.0
            {"high": 112.0, "low": 104.0, "close": 106.0} # TR = max(8, |112-108|, |104-108|) = 8.0
        ]
        atr = calculate_atr(candles, period=3)
        expected_atr = (10.0 + 12.0 + 8.0) / 3.0 # 10.0
        self.assertAlmostEqual(atr, expected_atr, places=3)


class TestFinancialSetupAndExplainability(unittest.TestCase):
    """9-Field Candidate Setup Explainability & Order Geometry Verification"""

    def test_xauusd_buy_setup_geometry(self):
        req = urllib.request.urlopen(f"{BASE_URL}/api/analysis?symbol=XAUUSD", timeout=5)
        self.assertEqual(req.status, 200)
        data = json.loads(req.read().decode('utf-8'))
        setup = data["setup"]
        
        # Verify 9 fields exist
        for i in range(1, 10):
            self.assertTrue(any(k.startswith(f"{i}_") for k in setup.keys()), f"Missing field {i}")

        if setup["direction"] == "BUY":
            # Extract numbers from potential entry and invalidation SL
            # Invalidation SL must be strictly less than current price / entry zone
            price = data["quote"]["bid"]
            sl_text = setup["5_structural_invalidation_and_sl"]
            sl_val = float(sl_text.split()[0])
            self.assertTrue(sl_val < price, f"SL ({sl_val}) must be below price ({price}) for BUY setup")

    def test_eurusd_no_trade_outcome(self):
        req = urllib.request.urlopen(f"{BASE_URL}/api/analysis?symbol=EURUSD", timeout=5)
        self.assertEqual(req.status, 200)
        data = json.loads(req.read().decode('utf-8'))
        setup = data["setup"]
        self.assertEqual(setup["direction"], "NO_TRADE")
        self.assertIn("no_trade_reason", setup)
        self.assertTrue(len(setup["no_trade_reason"]) > 10)


class TestBacktestCostAccountingAndIntegrity(unittest.TestCase):
    """Walk-Forward Backtesting Cost Deduction & Sample Size Disclosure"""

    def test_backtest_costs_included(self):
        req = urllib.request.urlopen(f"{BASE_URL}/api/backtest?symbol=XAUUSD", timeout=5)
        self.assertEqual(req.status, 200)
        data = json.loads(req.read().decode('utf-8'))
        
        # Verification that spread (3.2 pips) + slippage (0.5 pips) is deducted
        self.assertTrue("3.2" in data["cost_assumptions"] or "۳.۲" in data["cost_assumptions"])
        self.assertTrue("0.5" in data["cost_assumptions"] or "۰.۵" in data["cost_assumptions"])
        
        # Sample size and confidence note
        self.assertIn("statistical_confidence_note", data)
        self.assertTrue("120" in data["statistical_confidence_note"] or "۱۲۰" in data["statistical_confidence_note"])
        
        # Trades have cost_usd recorded
        if len(data["recent_trades"]) > 0:
            trade = data["recent_trades"][0]
            self.assertTrue(trade["cost_usd"] > 0)
            self.assertTrue(trade["bars_held"] >= 1)


class TestSecurityHardeningAndJournalLifeCycle(unittest.TestCase):
    """Security Boundaries, Payload Limits & Practice Journal LifeCycle"""

    def test_oversized_payload_rejected_with_413(self):
        junk = json.dumps({"symbol": "XAUUSD", "payload": "X" * 70000}).encode('utf-8')
        req = urllib.request.Request(
            f"{BASE_URL}/api/alerts/create",
            data=junk,
            headers={"Content-Type": "application/json"}
        )
        try:
            urllib.request.urlopen(req, timeout=5)
            self.fail("Server did not reject >64KB payload!")
        except urllib.error.HTTPError as e:
            self.assertEqual(e.code, 413)

    def test_xss_prevention_in_journal(self):
        malicious = json.dumps({
            "symbol": "XAUUSD",
            "type": "BUY",
            "planned_entry": 2742.0,
            "planned_sl": 2735.0,
            "planned_tp": 2755.0,
            "strategy": "<script>alert('pwned')</script>Liquidity sweep",
            "psychology": "Disciplined",
            "notes": "<img src=x onerror=alert(1)>"
        }).encode('utf-8')
        req = urllib.request.Request(
            f"{BASE_URL}/api/journal/add",
            data=malicious,
            headers={"Content-Type": "application/json"}
        )
        with urllib.request.urlopen(req, timeout=5) as resp:
            data = json.loads(resp.read().decode('utf-8'))
            entry = data["entry"]
            self.assertNotIn("<script>", entry["strategy"])
            self.assertIn("&lt;script&gt;", entry["strategy"])
            self.assertNotIn("<img", entry["notes"])
            self.assertIn("&lt;img", entry["notes"])

    def test_research_only_execution_lock(self):
        with urllib.request.urlopen(f"{BASE_URL}/api/status", timeout=5) as resp:
            status_data = json.loads(resp.read().decode('utf-8'))
            self.assertEqual(status_data["execution_mode"], "RESEARCH_ONLY (No live trading allowed)")
            self.assertFalse(status_data["order_execution_enabled"])


if __name__ == "__main__":
    unittest.main()
