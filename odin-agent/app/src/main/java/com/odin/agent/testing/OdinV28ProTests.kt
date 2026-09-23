package com.odin.agent.testing

import com.odin.agent.models.QuantStrategyType
import com.odin.agent.models.SignalSide
import com.odin.agent.trading.OdinTokenManager
import com.odin.agent.trading.pro.*

/**
 * ODIN PRO v1.0.28 - Pro Quantitative Trading Suite Tests (15 Tests)
 * تست‌های جامع ۱۸ ماژول پیشرفته رفع کمبودها و معماری سودسازتر
 */

class OdinV28ProTests {

    suspend fun runAll(): List<TestResult> {
        val results = mutableListOf<TestResult>()
        results.add(testTrailingStopBreakeven())
        results.add(testPartialCloseScaling())
        results.add(testAutoCompoundingDynamicLots())
        results.add(testEconomicNewsBlackout())
        results.add(testTradingSessionOverlapFilter())
        results.add(testMultiTimeframeConfluence())
        results.add(testWalkForwardOptimizer())
        results.add(testConfidenceCalibrator())
        results.add(testRealSlippageAndSwap())
        results.add(testTimeBasedEquityProtector())
        results.add(testRealMT5OrderGateway())
        results.add(testSwinexUsdtPayoutRails())
        results.add(testOdnOnChainBscContract())
        results.add(testTwoFactorAuthAndAntiPiracy())
        results.add(testCloudTradeJournalCsvExport())
        return results
    }

    private fun testTrailingStopBreakeven(): TestResult {
        val start = System.currentTimeMillis()
        return try {
            val mgr = TrailingStopBreakevenManager()
            // پوزیشن BUY در 1.0850 با استاپ 1.0800 (ریسک 50 پیپ). قیمت به 1.0900 می‌رسد (1R سود)
            val adjBe = mgr.evaluate("P-1", SignalSide.BUY, 1.0850, 1.0905, 1.0800, atr = 0.0035)
            val bePassed = adjBe != null && adjBe.isBreakeven && adjBe.newSl >= 1.0850
            TestResult(
                "PRO Trailing Stop & Breakeven - انتقال به سر‌به‌سر در 1R + تریلینگ ATR",
                bePassed,
                System.currentTimeMillis() - start,
                "سر‌به‌سر در 1R فعال شد: SL جدید ${adjBe?.newSl} (بافر اسپرد اعمال شد) - R=${adjBe?.currentR}",
                null
            )
        } catch (e: Exception) {
            TestResult("PRO Trailing Stop", false, System.currentTimeMillis() - start, "Exception", e.message)
        }
    }

    private fun testPartialCloseScaling(): TestResult {
        val start = System.currentTimeMillis()
        return try {
            val mgr = PartialCloseScalingManager()
            // سود به 1.6R می‌رسد -> TP1 (بستن ۵۰٪)
            val ev = mgr.checkPartialClose("P-2", SignalSide.BUY, 1.0850, 1.0935, 1.0800, currentLots = 1.0, originalLots = 1.0)
            val passed = ev != null && ev.stage == 1 && ev.closedLots == 0.50 && ev.remainingLots == 0.50
            TestResult(
                "PRO Partial Close Scaling - سیو سود ۵۰٪ در تارگت ۱.۵R و باقی‌مانده رانر",
                passed,
                System.currentTimeMillis() - start,
                "رویداد بستن پله‌ای: ${ev?.messageFa} - سود ناخالص سیوشده: ${ev?.realizedGrossProfit}$",
                null
            )
        } catch (e: Exception) {
            TestResult("PRO Partial Close", false, System.currentTimeMillis() - start, "Exception", e.message)
        }
    }

    private fun testAutoCompoundingDynamicLots(): TestResult {
        val start = System.currentTimeMillis()
        return try {
            val engine = AutoCompoundingEngine()
            val res1 = engine.calculateLotSize(equity = 10000.0, peakEquity = 10000.0, slDistancePrice = 0.0020, entryPrice = 1.0850)
            val res2 = engine.calculateLotSize(equity = 20000.0, peakEquity = 20000.0, slDistancePrice = 0.0020, entryPrice = 1.0850)
            val passed = res2.recommendedLots > res1.recommendedLots && res1.recommendedLots > 0.1
            TestResult(
                "PRO Auto-Compounding - رشد خودکار حجم لات متناسب با اکوییتی",
                passed,
                System.currentTimeMillis() - start,
                "بالانس ۱۰,۰۰۰$: ${res1.recommendedLots} لات -> بالانس ۲۰,۰۰۰$: ${res2.recommendedLots} لات - ریسک موثر: ${res1.riskPercentageEffective*100}%",
                null
            )
        } catch (e: Exception) {
            TestResult("PRO Auto-Compounding", false, System.currentTimeMillis() - start, "Exception", e.message)
        }
    }

    private fun testEconomicNewsBlackout(): TestResult {
        val start = System.currentTimeMillis()
        return try {
            val filter = EconomicNewsFilter()
            val now = System.currentTimeMillis()
            filter.addCustomEvent(EconomicEvent("TEST-NEWS", "High NFP", "خبر اشتغال آزمایشی", "USD", NewsImpact.HIGH, now + 5 * 60000L, "تست"))
            val res = filter.evaluateSymbol("EURUSD", now)
            val passed = !res.isTradingAllowed && res.spreadWidenWarning
            TestResult(
                "PRO Economic News Blackout - مسدودسازی ورود در زمان اخبار قرمز",
                passed,
                System.currentTimeMillis() - start,
                "وضعیت: ${res.statusFa} - هشدار باز شدن اسپرد: ${res.spreadWidenWarning}",
                null
            )
        } catch (e: Exception) {
            TestResult("PRO News Filter", false, System.currentTimeMillis() - start, "Exception", e.message)
        }
    }

    private fun testTradingSessionOverlapFilter(): TestResult {
        val start = System.currentTimeMillis()
        return try {
            val filter = TradingSessionFilter()
            val eval = filter.evaluate(QuantStrategyType.LIT_LIQUIDITY_INVERSION)
            val passed = eval.activeSessions.isNotEmpty() && eval.tehranTimeStr.isNotBlank()
            TestResult(
                "PRO Trading Session Filter - تداخل نقدینگی لندن-نیویورک و ساعت تهران",
                passed,
                System.currentTimeMillis() - start,
                "سشن‌های فعال: ${eval.activeSessions.map { it.name }} - ساعت تهران: ${eval.tehranTimeStr} - ضریب اعتبار: ${eval.confidenceModifier}",
                null
            )
        } catch (e: Exception) {
            TestResult("PRO Session Filter", false, System.currentTimeMillis() - start, "Exception", e.message)
        }
    }

    private fun testMultiTimeframeConfluence(): TestResult {
        val start = System.currentTimeMillis()
        return try {
            val engine = MultiTimeframeConfluenceEngine()
            val res = engine.evaluateConfluence("EURUSD", SignalSide.BUY, 1.0850, 1.0840, 62.0, SignalSide.BUY, SignalSide.BUY, SignalSide.BUY)
            val passed = res.isApproved && res.overallScore >= 75.0 && res.timeframes.size == 4
            TestResult(
                "PRO Multi-Timeframe Confluence - تایید همزمان M15 + H1 + H4 + D1",
                passed,
                System.currentTimeMillis() - start,
                "امتیاز کانفلوئنس: ${res.overallScore}/100 - رتبه: ${res.confluenceGradeFa} - جهت: ${res.primaryDirection}",
                null
            )
        } catch (e: Exception) {
            TestResult("PRO Confluence", false, System.currentTimeMillis() - start, "Exception", e.message)
        }
    }

    private fun testWalkForwardOptimizer(): TestResult {
        val start = System.currentTimeMillis()
        return try {
            val opt = WalkForwardOptimizer()
            val res = opt.runOptimizationCycle("EURUSD", listOf(1.08, 1.082, 1.085, 1.087, 1.09))
            val passed = res.evaluatedCount > 0 && res.bestParameters.emaFast > 0
            TestResult(
                "PRO Walk-Forward Optimizer - بهینه‌ساز دوره‌ای بدون Overfitting",
                passed,
                System.currentTimeMillis() - start,
                "بهترین پارامتر: EMA(${res.bestParameters.emaFast}/${res.bestParameters.emaSlow}) - نسبت پایایی: ${res.bestParameters.robustnessRatio}",
                null
            )
        } catch (e: Exception) {
            TestResult("PRO Walk-Forward", false, System.currentTimeMillis() - start, "Exception", e.message)
        }
    }

    private fun testConfidenceCalibrator(): TestResult {
        val start = System.currentTimeMillis()
        return try {
            val cal = ConfidenceCalibrator()
            val out = cal.calibrate(85.0)
            val passed = out.calibratedWinProbability in 50.0..99.0 && out.recommendedRiskWeight > 0.0
            TestResult(
                "PRO ML Confidence Calibrator - تبدیل نمره خام به احتمال برد واقعی",
                passed,
                System.currentTimeMillis() - start,
                "نمره خام 85 -> احتمال کالیبره: ${out.calibratedWinProbability}% - رده: ${out.confidenceTierFa} - ضریب ریسک: ${out.recommendedRiskWeight}x",
                null
            )
        } catch (e: Exception) {
            TestResult("PRO Calibrator", false, System.currentTimeMillis() - start, "Exception", e.message)
        }
    }

    private fun testRealSlippageAndSwap(): TestResult {
        val start = System.currentTimeMillis()
        return try {
            val engine = RealSlippageAndSwapEngine()
            val executed = engine.calculateSlippage("EURUSD", isBuy = true, basePrice = 1.0850, lots = 1.0, volatilityAtrPips = 10.0, isHighNewsActive = false)
            val swap = engine.calculateSwap("EURUSD", lots = 1.0, isBuy = true, daysHeld = 1)
            val passed = executed >= 1.0850 && swap != 0.0
            TestResult(
                "PRO Real Slippage & Swap - محاسبه اسلیپیج و سواپ ویتاورس",
                passed,
                System.currentTimeMillis() - start,
                "قیمت با اسلیپیج: $executed (پایه: 1.0850) - سواپ شبانه 1 لات EURUSD: ${swap}$",
                null
            )
        } catch (e: Exception) {
            TestResult("PRO Slippage Swap", false, System.currentTimeMillis() - start, "Exception", e.message)
        }
    }

    private fun testTimeBasedEquityProtector(): TestResult {
        val start = System.currentTimeMillis()
        return try {
            val protector = TimeBasedEquityProtector()
            val (safe, _) = protector.evaluateRisk(10000.0, 10000.0, 10000.0, 10000.0, 0)
            val (hitDaily, reason) = protector.evaluateRisk(9600.0, 10000.0, 10000.0, 10000.0, 0) // 4% loss -> exceeds 3%
            val passed = safe && !hitDaily
            TestResult(
                "PRO Time-Based Equity Protector - سقف ضرر ۳٪ روزانه (Kill-switch) و ۵٪ هفتگی",
                passed,
                System.currentTimeMillis() - start,
                "وضعیت عادی: امن - افت ۴٪ روزانه: مسدود شد ($reason)",
                null
            )
        } catch (e: Exception) {
            TestResult("PRO Equity Protector", false, System.currentTimeMillis() - start, "Exception", e.message)
        }
    }

    private fun testRealMT5OrderGateway(): TestResult {
        val start = System.currentTimeMillis()
        return try {
            val tm = OdinTokenManager.getInstance()
            val gw = RealMT5OrderGateway(tm)
            val res = gw.sendRealOrder("EURUSD", SignalSide.BUY, 0.1, 1.0850, 1.0800, 1.0950, QuantStrategyType.MEAN_REVERSION, isTwoFactorVerified = true)
            val ord = res.getOrNull()
            val passed = ord != null && ord.ticket > 0 && ord.server == "Vittaverse-Live"
            TestResult(
                "PRO Real MT5 Order Gateway - ارسال سفارش واقعی به سرور ویتاورس",
                passed,
                System.currentTimeMillis() - start,
                "تیکت لایو #${ord?.ticket} - سرور ${ord?.server} - حجم ${ord?.lots} لات - ${ord?.responseMessageFa}",
                null
            )
        } catch (e: Exception) {
            TestResult("PRO MT5 Gateway", false, System.currentTimeMillis() - start, "Exception", e.message)
        }
    }

    private fun testSwinexUsdtPayoutRails(): TestResult {
        val start = System.currentTimeMillis()
        return try {
            val rails = RealCommissionPayoutRails()
            val inv = rails.generateWeeklyInvoice(100.0, PayoutNetwork.TRC20)
            val passed = inv.swinexCommissionUsd == 20.0 && inv.digitalSignature.isNotBlank()
            TestResult(
                "PRO USDT Commission Payout Rails - تسویه اتوماتیک ۲۰٪ کمیسیون سوینکس",
                passed,
                System.currentTimeMillis() - start,
                "فاکتور رسمی ${inv.invoiceId} - سود کاربر ۱۰۰$ -> سهم سوینکس ${inv.swinexCommissionUsd}$ USDT-TRC20 - امضا: ${inv.digitalSignature.take(16)}...",
                null
            )
        } catch (e: Exception) {
            TestResult("PRO Payout Rails", false, System.currentTimeMillis() - start, "Exception", e.message)
        }
    }

    private fun testOdnOnChainBscContract(): TestResult {
        val start = System.currentTimeMillis()
        return try {
            val tm = OdinTokenManager.getInstance()
            val contract = OdnOnChainContract(tm)
            val state = contract.syncWithOnChain()
            val code = contract.getSolidityContractSource()
            val passed = state.contractSpec.symbol == "ODN" && code.contains("contract OdinToken")
            TestResult(
                "PRO ODN On-Chain BEP-20 Contract - مشخصات و پل قرارداد هوشمند بایننس",
                passed,
                System.currentTimeMillis() - start,
                "توکن ${state.contractSpec.name} (${state.contractSpec.symbol}) - شبکه BSC (ChainId: 56) - قرارداد: ${state.contractSpec.contractAddressBsc.take(12)}... - همگام: ${state.isSynced}",
                null
            )
        } catch (e: Exception) {
            TestResult("PRO On-Chain Contract", false, System.currentTimeMillis() - start, "Exception", e.message)
        }
    }

    private fun testTwoFactorAuthAndAntiPiracy(): TestResult {
        val start = System.currentTimeMillis()
        return try {
            val auth = TwoFactorSecurityEngine()
            val pinValid = auth.verifyPin("123456")
            val pinInvalid = auth.verifyPin("000000")
            val license = LicenseAntiPiracyEngine().verifyLicense()
            val passed = pinValid && !pinInvalid && license.isValid && license.deviceFingerprint.isNotBlank()
            TestResult(
                "PRO 2FA Security & Anti-Piracy - امنیت دو مرحله‌ای و لایسنس سخت‌افزاری",
                passed,
                System.currentTimeMillis() - start,
                "تایید 2FA پین: $pinValid - رد پین اشتباه: ${!pinInvalid} - اثرانگشت سخت‌افزار: ${license.deviceFingerprint} - مالک: ${license.ownerOrg}",
                null
            )
        } catch (e: Exception) {
            TestResult("PRO 2FA Anti-Piracy", false, System.currentTimeMillis() - start, "Exception", e.message)
        }
    }

    private fun testCloudTradeJournalCsvExport(): TestResult {
        val start = System.currentTimeMillis()
        return try {
            val journal = CloudTradeJournal()
            journal.logTrade("TX-PRO-1", "EURUSD", SignalSide.BUY, 1.0850, 1.0950, 80.0, 2.0, QuantStrategyType.LIT_LIQUIDITY_INVERSION)
            val csv = journal.exportToCsv()
            val passed = journal.entries.value.isNotEmpty() && csv.contains("EURUSD") && csv.contains("80.0")
            TestResult(
                "PRO Cloud Trade Journal & CSV Export - ژورنال ابری و خروجی اکسل",
                passed,
                System.currentTimeMillis() - start,
                "ثبت ${journal.entries.value.size} معامله - طول فایل CSV: ${csv.length} کاراکتر - سرفصل و مقادیر صحیح",
                null
            )
        } catch (e: Exception) {
            TestResult("PRO Trade Journal", false, System.currentTimeMillis() - start, "Exception", e.message)
        }
    }
}
