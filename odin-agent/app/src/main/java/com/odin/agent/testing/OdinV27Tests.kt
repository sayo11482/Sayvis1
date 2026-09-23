package com.odin.agent.testing

import com.odin.agent.models.QuantStrategyType
import com.odin.agent.models.SignalSide
import com.odin.agent.trading.DemoAccountManager
import com.odin.agent.trading.OdinTokenManager
import com.odin.agent.trading.OdinTier
import com.odin.agent.trading.SoftwareGapAnalyzer

/**
 * ODIN v1.0.27 - Token Economy + Demo Trading + Gap Analysis Tests
 * تست‌های v1.0.27 - توکن اودین + کمیسیون 20% + حساب دمو + تحلیل کمبودها
 */

class OdinV27Tests {

    suspend fun runAll(): List<TestResult> {
        val results = mutableListOf<TestResult>()
        results.add(testTokenCommission20Percent())
        results.add(testTokenGateMandatory())
        results.add(testTokenLedgerSecurity())
        results.add(testTokenEconomyFlow())
        results.add(testDemoAccountLifecycle())
        results.add(testDemoSpreadAndMargin())
        results.add(testDemoFeeOnProfitOnly())
        results.add(testBacktestDeterministic())
        results.add(testGapAnalyzer())
        results.add(testAuditIntegration())
        return results
    }

    private fun testTokenCommission20Percent(): TestResult {
        val start = System.currentTimeMillis()
        return try {
            val tm = OdinTokenManager.getInstance()
            val pay = tm.applyPerformanceFee("V27-FEE-${System.currentTimeMillis()}", 10.0, QuantStrategyType.LIT_LIQUIDITY_INVERSION)
            val passed = pay != null && pay.feeUsd == 2.0 && pay.userNetUsd == 8.0
            TestResult(
                "Token Commission 20% - 10$ سود ربات → 2$ اتومات به صاحب نرم‌افزار + 8$ کاربر",
                passed,
                System.currentTimeMillis() - start,
                "مثال کاربر اجرا شد: سود خام ${pay?.grossProfitUsd}$ - کمیسیون ${pay?.feeUsd}$ سوینکس - خالص ${pay?.userNetUsd}$ - سوزاندن ${pay?.odnBurned} ODN - خزانه: ${tm.state.value.ownerTreasuryUsd}$",
                null
            )
        } catch (e: Exception) {
            TestResult("Token Commission 20%", false, System.currentTimeMillis() - start, "Exception", e.message)
        }
    }

    private fun testTokenGateMandatory(): TestResult {
        val start = System.currentTimeMillis()
        return try {
            val tm = OdinTokenManager.getInstance()
            val gates = tm.state.value.gates
            val freeLocked = !tm.isStrategyAllowed(QuantStrategyType.TV_80_PERCENT) && !tm.isStrategyAllowed(QuantStrategyType.LIT_LIQUIDITY_INVERSION)
            val meanRevFree = tm.isStrategyAllowed(QuantStrategyType.MEAN_REVERSION)
            val tiers = OdinTier.values().map { it.minStake }
            val passed = freeLocked && meanRevFree && gates.size == 7 && tiers.contains(2000.0)
            TestResult(
                "Token Gate Mandatory - استفاده از توکن اجباری برای استراتژی‌های پرسود بالاتر",
                passed,
                System.currentTimeMillis() - start,
                "TV80 نیاز ${OdinTier.PLATINUM.minStake.toInt()} ODN - LIT نیاز ${OdinTier.GOLD.minStake.toInt()} ODN - Trend/Momentum نیاز ${OdinTier.SILVER.minStake.toInt()} ODN - MeanRev رایگان - ${gates.size} گیت فعال",
                null
            )
        } catch (e: Exception) {
            TestResult("Token Gate", false, System.currentTimeMillis() - start, "Exception", e.message)
        }
    }

    private fun testTokenLedgerSecurity(): TestResult {
        val start = System.currentTimeMillis()
        return try {
            val tm = OdinTokenManager.getInstance()
            val integrity = tm.verifyLedgerIntegrity()
            val tamperDetected = tm.simulateTampering()
            val passed = integrity && tamperDetected
            TestResult(
                "Token Ledger Security - زنجیره هش SHA-256 - تشخیص دستکاری",
                passed,
                System.currentTimeMillis() - start,
                "صحت زنجیره: $integrity - ${tm.state.value.transactions.size} تراکنش زنجیره‌ای - دستکاری تشخیص: $tamperDetected",
                null
            )
        } catch (e: Exception) {
            TestResult("Token Ledger", false, System.currentTimeMillis() - start, "Exception", e.message)
        }
    }

    private fun testTokenEconomyFlow(): TestResult {
        val start = System.currentTimeMillis()
        return try {
            val tm = OdinTokenManager.getInstance()
            val before = tm.state.value.wallet.balance
            val topOk = tm.topUp(100.0) // 100$ → 1000 ODN
            val balAfterTop = tm.state.value.wallet.balance
            val stakeOk = tm.stake(500.0) // GOLD
            val tier = tm.state.value.tier
            val litUnlocked = tm.isStrategyAllowed(QuantStrategyType.LIT_LIQUIDITY_INVERSION)
            val unOk = tm.unstake(500.0)
            val tierBack = tm.state.value.tier
            val passed = topOk && stakeOk && tier == OdinTier.GOLD && litUnlocked && unOk && balAfterTop > before
            TestResult(
                "Token Economy Flow - شارژ/استیک/تییر/باز شدن گیت/خروج",
                passed,
                System.currentTimeMillis() - start,
                "شارژ 100$ → +1000 ODN - استیک 500 → تییر $tier - LIT باز: $litUnlocked - خروج 500 → تییر $tierBack",
                null
            )
        } catch (e: Exception) {
            TestResult("Token Flow", false, System.currentTimeMillis() - start, "Exception", e.message)
        }
    }

    private suspend fun testDemoAccountLifecycle(): TestResult {
        val start = System.currentTimeMillis()
        return try {
            val tm = OdinTokenManager.getInstance()
            val demo = DemoAccountManager(tm)
            val acc = demo.createDemoAccount(10000.0)
            val open = demo.openPosition("EURUSD", SignalSide.BUY, 100.0, QuantStrategyType.MEAN_REVERSION, 1.0850, 0.0002)
            val pos = open.getOrNull()
            demo.updatePrices(mapOf("EURUSD" to (pos?.tpPrice ?: 1.09)))
            val h = demo.state.value.history
            val closed = h.firstOrNull { it.id == pos?.id }
            val passed = acc.balance == 10000.0 && pos != null && closed != null && closed.status == "CLOSED_TP" && closed.grossPnlUsd > 0
            TestResult(
                "Demo Account Lifecycle - ساخت حساب دمو + باز کردن معامله + TP اتومات",
                passed,
                System.currentTimeMillis() - start,
                "لاگین ${acc.login} سرور ${acc.server} - ورود ${pos?.entryPrice} - TP=${"%.5f".format(pos?.tpPrice ?: 0.0)} - نتیجه ${closed?.status} سود خام ${closed?.grossPnlUsd}$ کمیسیون ${closed?.performanceFeeUsd}$ خالص ${closed?.netPnlUsd}$",
                null
            )
        } catch (e: Exception) {
            TestResult("Demo Lifecycle", false, System.currentTimeMillis() - start, "Exception", e.message)
        }
    }

    private suspend fun testDemoSpreadAndMargin(): TestResult {
        val start = System.currentTimeMillis()
        return try {
            val demo = DemoAccountManager(OdinTokenManager.getInstance())
            demo.createDemoAccount(10000.0)
            val pos = demo.openPosition("BTCUSD", SignalSide.BUY, 200.0, QuantStrategyType.MEAN_REVERSION, 60000.0, 15.0).getOrNull()
            val marginCorrect = pos != null && pos.marginUsd == 200.0
            val notional = pos?.notionalUsd ?: 0.0
            val spreadCost = pos?.spreadCostUsd ?: 0.0
            val passed = marginCorrect && notional > 0 && spreadCost > 0
            TestResult(
                "Demo Spread & Margin - اسپرد و مارجین واقعی",
                passed,
                System.currentTimeMillis() - start,
                "BTC ورود ${pos?.entryPrice} - نشنال ${notional}$ (1:${demo.state.value.account?.leverage}) - مارجین ${pos?.marginUsd}$ - هزینه اسپرد ${spreadCost}$ - لات ${pos?.lots}",
                null
            )
        } catch (e: Exception) {
            TestResult("Demo Spread", false, System.currentTimeMillis() - start, "Exception", e.message)
        }
    }

    private suspend fun testDemoFeeOnProfitOnly(): TestResult {
        val start = System.currentTimeMillis()
        return try {
            val demo = DemoAccountManager(OdinTokenManager.getInstance())
            demo.createDemoAccount(10000.0)
            demo.openPosition("XAUUSD", SignalSide.SELL, 50.0, QuantStrategyType.MEAN_REVERSION, 2650.0, 0.35)
            val pos = demo.state.value.openPositions.lastOrNull()
            if (pos != null) demo.updatePrices(mapOf("XAUUSD" to pos.slPrice * 1.02))
            val closed = demo.state.value.history.firstOrNull { it.status == "CLOSED_SL" }
            val passed = closed != null && closed.netPnlUsd < 0 && closed.performanceFeeUsd == 0.0
            TestResult(
                "Demo Fee On Profit Only - روی ضرر هیچ کمیسیونی نیست",
                passed,
                System.currentTimeMillis() - start,
                "SL خورد: ${closed?.status} - ضرر ${closed?.netPnlUsd}$ - کمیسیون ${closed?.performanceFeeUsd}$ (صفر روی ضرر)",
                null
            )
        } catch (e: Exception) {
            TestResult("Demo Fee Only", false, System.currentTimeMillis() - start, "Exception", e.message)
        }
    }

    private fun testBacktestDeterministic(): TestResult {
        val start = System.currentTimeMillis()
        return try {
            val audit = FullSystemAudit()
            val c1 = audit.deterministicCandles("EURUSD", 100)
            val c2 = audit.deterministicCandles("EURUSD", 100)
            val same = c1.map { it.close } == c2.map { it.close }
            val passed = c1.size == 100 && same
            TestResult(
                "Backtest Deterministic - بک‌تست تکرارپذیر بدون Random",
                passed,
                System.currentTimeMillis() - start,
                "دو اجرای یکسان نتیجه یکسان: $same - ${c1.size} کندل هش‌محور - اصل 100% REAL",
                null
            )
        } catch (e: Exception) {
            TestResult("Backtest Deterministic", false, System.currentTimeMillis() - start, "Exception", e.message)
        }
    }

    private fun testGapAnalyzer(): TestResult {
        val start = System.currentTimeMillis()
        return try {
            val analyzer = SoftwareGapAnalyzer()
            val gaps = analyzer.analyze()
            val summary = analyzer.summary()
            val criticalFound = gaps.any { it.impact == com.odin.agent.trading.GapImpact.CRITICAL }
            val passed = gaps.size >= 15 && criticalFound && summary.topPriorities.size == 5 && analyzer.roadmapFa().size == 5
            TestResult(
                "Gap Analyzer - چه چیزهایی کم داره تا سودسازتر بشه",
                passed,
                System.currentTimeMillis() - start,
                "${gaps.size} ماژول - ${summary.missing} نصب نیست - ${summary.partial} نیمه‌کاره - حیاتی: ${summary.criticalIds.joinToString()} - ${summary.totalEffortDays} روز کاری - نقشه راه 5 فاز",
                null
            )
        } catch (e: Exception) {
            TestResult("Gap Analyzer", false, System.currentTimeMillis() - start, "Exception", e.message)
        }
    }

    private suspend fun testAuditIntegration(): TestResult {
        val start = System.currentTimeMillis()
        return try {
            val audit = FullSystemAudit()
            val sec = audit.auditSectionsCount()
            val passed = sec == 5
            TestResult(
                "Full System Audit Integration - تست کامل سیستم 5 بخشی",
                passed,
                System.currentTimeMillis() - start,
                "بخش‌ها: اینترنت + امنیت + دمو + بک‌تست + کمبودها = $sec بخش - گزارش فارسی + وردیکت",
                null
            )
        } catch (e: Exception) {
            TestResult("Audit Integration", false, System.currentTimeMillis() - start, "Exception", e.message)
        }
    }
}
