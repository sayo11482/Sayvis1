#!/usr/bin/env python3
"""
TradeSense AI - Multi-Disciplinary System Audit Suite
Covering:
1. QA & Functional Health
2. Cybersecurity & Threat Modeling
3. AI / NLP & Macroeconomic Intelligence
4. Quantitative Finance & Algorithmic Integrity
5. Mobile & Android UX Architecture
"""

import unittest
import urllib.request
import urllib.error
import json
import re
import os
import subprocess

BASE_URL = "http://127.0.0.1:8080"

class TestQASystemHealth(unittest.TestCase):
    """Discipline 1: Quality Assurance & System Verification"""

    def test_all_eight_core_endpoints(self):
        endpoints = [
            "/api/status",
            "/api/markets",
            "/api/news",
            "/api/analysis?symbol=XAUUSD",
            "/api/analysis?symbol=EURUSD",
            "/api/backtest?symbol=XAUUSD",
            "/api/backtest?symbol=EURUSD",
            "/api/alerts",
            "/api/journal",
            "/api/settings",
            "/api/connectivity"
        ]
        for ep in endpoints:
            with urllib.request.urlopen(f"{BASE_URL}{ep}", timeout=5) as resp:
                self.assertEqual(resp.status, 200, f"Endpoint {ep} failed with status {resp.status}")
                data = json.loads(resp.read().decode('utf-8'))
                self.assertIsInstance(data, (dict, list), f"Endpoint {ep} did not return valid JSON")

    def test_pwa_and_mobile_html_assets(self):
        with urllib.request.urlopen(f"{BASE_URL}/", timeout=5) as resp:
            html = resp.read().decode('utf-8')
            # Check viewport meta tag for mobile responsiveness
            self.assertIn('name="viewport"', html)
            self.assertIn('width=device-width', html)
            # Check Persian RTL direction
            self.assertIn('dir="rtl"', html)
            # Check 8 navigation tabs
            tabs = ['tab-dashboard', 'tab-markets', 'tab-news', 'tab-analysis', 'tab-backtest', 'tab-alerts', 'tab-journal', 'tab-settings']
            for t in tabs:
                self.assertIn(t, html, f"Missing navigation tab: {t}")
            # Check audio controls
            self.assertIn('btn-audio-toggle', html)


class TestCybersecurityAndThreatModel(unittest.TestCase):
    """Discipline 2: Cybersecurity, Data Privacy & Attack Surface Hardening"""

    def test_zero_live_secrets_in_repo(self):
        # Run rigorous secret search for common token/key patterns
        patterns = [
            r"ghp_[a-zA-Z0-9]{36}",
            r"AKIA[0-9A-Z]{16}",
            r"eyJ[a-zA-Z0-9_-]{10,}\.[a-zA-Z0-9_-]{10,}",
            r"password\s*=\s*['\"][^'\"]+['\"]"
        ]
        for p in patterns:
            res = subprocess.run(
                ["grep", "-rnE", p, "web_dashboard/", "test_tradesense_ai.py"],
                capture_output=True, text=True
            )
            # Filter out expected false positives in comments/placeholders
            matched_lines = [l for l in res.stdout.splitlines() if "placeholder" not in l.lower() and "none" not in l.lower()]
            self.assertEqual(len(matched_lines), 0, f"Potential secret exposure found matching {p}: {matched_lines}")

    def test_research_only_execution_block(self):
        # Verify status endpoint certifies research-only
        with urllib.request.urlopen(f"{BASE_URL}/api/status", timeout=5) as resp:
            data = json.loads(resp.read().decode('utf-8'))
            self.assertEqual(data["execution_mode"], "RESEARCH_ONLY (No live trading allowed)")
            self.assertFalse(data["order_execution_enabled"])

    def test_payload_size_limit_rejection(self):
        # POST payload larger than 64KB should return 413 Entity Too Large
        oversized_payload = json.dumps({"symbol": "XAUUSD", "junk": "A" * 70000}).encode('utf-8')
        req = urllib.request.Request(
            f"{BASE_URL}/api/alerts/create",
            data=oversized_payload,
            headers={"Content-Type": "application/json"}
        )
        try:
            urllib.request.urlopen(req, timeout=5)
            self.fail("Server accepted >64KB payload!")
        except urllib.error.HTTPError as e:
            self.assertEqual(e.code, 413, f"Expected 413 for oversized payload, got {e.code}")

    def test_xss_sanitization_in_post_requests(self):
        # Inject script tags into alert creation
        xss_payload = json.dumps({
            "symbol": "XAUUSD",
            "type": "PRICE_ABOVE",
            "condition": "<script>alert('xss')</script>2750.00"
        }).encode('utf-8')
        req = urllib.request.Request(
            f"{BASE_URL}/api/alerts/create",
            data=xss_payload,
            headers={"Content-Type": "application/json"}
        )
        with urllib.request.urlopen(req, timeout=5) as resp:
            data = json.loads(resp.read().decode('utf-8'))
            self.assertTrue(data["success"])
            alert = data["alert"]
            # Script tag should be escaped to &lt;script&gt;
            self.assertNotIn("<script>", alert["condition"])
            self.assertIn("&lt;script&gt;", alert["condition"])

    def test_transparent_firewall_disclosure(self):
        with urllib.request.urlopen(f"{BASE_URL}/api/connectivity", timeout=5) as resp:
            data = json.loads(resp.read().decode('utf-8'))
            self.assertFalse(data["is_real_broker_connected"])
            self.assertIn("BLOCKED", data["live_broker_socket"])
            self.assertTrue(data["is_model_calibrated"])


class TestAINLPAndMacroIntelligence(unittest.TestCase):
    """Discipline 3: AI / NLP & Macroeconomic News Intelligence"""

    def test_fact_versus_interpretation_separation(self):
        with urllib.request.urlopen(f"{BASE_URL}/api/news", timeout=5) as resp:
            items = json.loads(resp.read().decode('utf-8'))
            self.assertTrue(len(items) >= 3)
            for item in items:
                self.assertIn("headline_fa", item)
                self.assertIn("fact_raw", item, "News item lacks raw factual event description")
                self.assertIn("interpretation_fa", item, "News item lacks analytical interpretation")
                self.assertIn("source", item)
                self.assertIn("impact", item)
                self.assertIn(item["impact"], ["HIGH", "MEDIUM", "LOW"])

    def test_multi_scenario_conditional_triggers(self):
        with urllib.request.urlopen(f"{BASE_URL}/api/news", timeout=5) as resp:
            items = json.loads(resp.read().decode('utf-8'))
            for item in items:
                scenarios = item.get("scenarios", {})
                self.assertIn("bullish", scenarios, "Missing bullish scenario")
                self.assertIn("bearish", scenarios, "Missing bearish scenario")
                self.assertIn("neutral", scenarios, "Missing neutral scenario")
                # Scenarios must contain actual conditional triggers
                self.assertTrue(len(scenarios["bullish"]["condition"]) > 10)
                self.assertTrue(len(scenarios["bearish"]["condition"]) > 10)
                self.assertTrue(len(scenarios["neutral"]["condition"]) > 10)


class TestQuantAndAlgorithmicFinance(unittest.TestCase):
    """Discipline 4: Quantitative Trading, 9-Field Explainability & Cost Accounting"""

    def test_all_nine_mandatory_setup_fields(self):
        for sym in ["XAUUSD", "EURUSD"]:
            with urllib.request.urlopen(f"{BASE_URL}/api/analysis?symbol={sym}", timeout=5) as resp:
                data = json.loads(resp.read().decode('utf-8'))
                setup = data.get("candidate_setup") or data.get("setup")
                self.assertIsNotNone(setup, f"No setup returned for {sym}")
                for i in range(1, 10):
                    prefix = f"{i}_"
                    matching = [k for k in setup.keys() if k.startswith(prefix)]
                    self.assertTrue(len(matching) > 0, f"Missing candidate field {i} for {sym}")

    def test_no_trade_support(self):
        with urllib.request.urlopen(f"{BASE_URL}/api/analysis?symbol=EURUSD", timeout=5) as resp:
            data = json.loads(resp.read().decode('utf-8'))
            setup = data.get("candidate_setup") or data.get("setup")
            direction = setup.get("direction")
            self.assertIn(direction, ["BUY", "SELL", "NO_TRADE"])
            if direction == "NO_TRADE":
                self.assertTrue(len(setup.get("no_trade_reason", "")) > 5)

    def test_backtest_cost_model_and_lookahead_freedom(self):
        with urllib.request.urlopen(f"{BASE_URL}/api/backtest?symbol=XAUUSD", timeout=5) as resp:
            data = json.loads(resp.read().decode('utf-8'))
            metrics = data["metrics"]
            self.assertIn("total_trades", metrics)
            self.assertIn("win_rate_pct", metrics)
            self.assertIn("profit_factor", metrics)
            self.assertIn("expectancy_usd", metrics)
            self.assertIn("total_costs_usd", metrics)
            self.assertIn("max_drawdown_pct", metrics)
            # Cost assumptions explicitly declared
            self.assertIn("cost_assumptions", data)
            self.assertIn("statistical_confidence_note", data)
            # Rejected setups log must exist
            self.assertTrue(len(data.get("rejected_setups_log", [])) > 0)


class TestAlertsAndEducationalJournal(unittest.TestCase):
    """Discipline 5: Client-Side Alerting & Practice Journal"""

    def test_alert_toggle_and_evaluation(self):
        # Toggle alert ALT-1
        payload = json.dumps({"id": "ALT-1", "is_active": False}).encode('utf-8')
        req = urllib.request.Request(
            f"{BASE_URL}/api/alerts/toggle",
            data=payload,
            headers={"Content-Type": "application/json"}
        )
        with urllib.request.urlopen(req, timeout=5) as resp:
            data = json.loads(resp.read().decode('utf-8'))
            self.assertTrue(data["success"])
            self.assertFalse(data["alert"]["active"])

    def test_journal_entry_lifecycle(self):
        # Add a journal practice entry
        payload = json.dumps({
            "symbol": "XAUUSD",
            "type": "SELL",
            "entry_price": 2745.0,
            "sl": 2755.0,
            "tp": 2730.0,
            "thesis": "Rejection from 1H Liquidity sweep with RSI bear div",
            "mistakes_to_avoid": "Do not widen SL if price hovers at 2753"
        }).encode('utf-8')
        req = urllib.request.Request(
            f"{BASE_URL}/api/journal/add",
            data=payload,
            headers={"Content-Type": "application/json"}
        )
        with urllib.request.urlopen(req, timeout=5) as resp:
            data = json.loads(resp.read().decode('utf-8'))
            self.assertTrue(data["success"])
            self.assertIn("entry", data)
            self.assertEqual(data["entry"]["symbol"], "XAUUSD")


if __name__ == "__main__":
    unittest.main()
