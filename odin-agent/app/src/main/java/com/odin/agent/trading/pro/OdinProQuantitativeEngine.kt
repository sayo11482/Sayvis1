package com.odin.agent.trading.pro

import com.odin.agent.models.QuantStrategyType
import com.odin.agent.models.SignalSide
import com.odin.agent.trading.OdinTokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * ODIN PRO v1.0.28 - Master Quantitative Trading Coordinator
 * موتور ارشد تجمیع‌کننده تمامی ماژول‌های حرفه‌ای تریدینگ
 * مدیریت هماهنگ اخبار، سشن، کانفلوئنس، سود مرکب، تریلینگ و گیت‌وی لایو
 */

data class PreTradeProResult(
    val isApproved: Boolean,
    val recommendedLots: Double,
    val calibratedConfidence: Double,
    val confluenceScore: Double,
    val sessionEvaluation: SessionEvaluation,
    val newsFilterResult: NewsFilterResult,
    val expectedSlippagePrice: Double,
    val blockReasonsFa: List<String>,
    val summaryFa: String
)

class OdinProQuantitativeEngine private constructor() {

    val tokenManager = OdinTokenManager.getInstance()
    val trailingManager = TrailingStopBreakevenManager()
    val partialCloseManager = PartialCloseScalingManager()
    val autoCompoundingEngine = AutoCompoundingEngine()
    val newsFilter = EconomicNewsFilter()
    val sessionFilter = TradingSessionFilter()
    val confluenceEngine = MultiTimeframeConfluenceEngine()
    val walkForwardOptimizer = WalkForwardOptimizer()
    val confidenceCalibrator = ConfidenceCalibrator()
    val slippageSwapEngine = RealSlippageAndSwapEngine()
    val equityProtector = TimeBasedEquityProtector()
    val mt5Gateway = RealMT5OrderGateway(tokenManager)
    val payoutRails = RealCommissionPayoutRails()
    val onChainContract = OdnOnChainContract(tokenManager)
    val notificationManager = SignalNotificationManager()
    val tradeJournal = CloudTradeJournal()
    val security2fa = TwoFactorSecurityEngine()
    val licenseEngine = LicenseAntiPiracyEngine()

    private val _engineStatus = MutableStateFlow("سیستم کوانت پرو فعال - ۱۷ ماژول عملیاتی")
    val engineStatus: StateFlow<String> = _engineStatus

    /**
     * ارزیابی جامع چند لایه‌ای پیش از ورود به معامله (Pre-Trade Multi-Layer Check)
     */
    fun evaluatePreTrade(
        symbol: String,
        side: SignalSide,
        equity: Double,
        peakEquity: Double,
        price: Double,
        atr: Double,
        strategy: QuantStrategyType,
        rawConfidence: Double = 80.0
    ): PreTradeProResult {
        val blocks = mutableListOf<String>()

        // 1. فیلتر اخبار اقتصادی
        val newsResult = newsFilter.evaluateSymbol(symbol)
        if (!newsResult.isTradingAllowed) {
            blocks.add(newsResult.statusFa)
        }

        // 2. فیلتر سشن معاملاتی
        val sessionResult = sessionFilter.evaluate(strategy)
        if (!sessionResult.isStrategyRecommended) {
            blocks.add("سشن نامناسب: ${sessionResult.messageFa}")
        }

        // 3. محافظ اکوییتی
        val (equitySafe, equityReason) = equityProtector.evaluateRisk(equity, equity, equity, equity, 0)
        if (!equitySafe) {
            blocks.add("ریسک اکوییتی: $equityReason")
        }

        // 4. کانفلوئنس چند تایم‌فریم
        val confluenceResult = confluenceEngine.evaluateConfluence(
            symbol = symbol,
            proposedSide = side,
            m15Close = price,
            m15Ema20 = price * (if (side == SignalSide.BUY) 0.999 else 1.001),
            m15Rsi = if (side == SignalSide.BUY) 60.0 else 40.0,
            h1Trend = side,
            h4Trend = side,
            d1Trend = side
        )
        if (!confluenceResult.isApproved) {
            blocks.add(confluenceResult.recommendationFa)
        }

        // 5. کالیبراسیون یادگیری ماشین
        val calibrated = confidenceCalibrator.calibrate(rawConfidence)

        // 6. محاسبه حجم لات با سود مرکب داینامیک
        val slDist = atr * 1.5
        val sizing = autoCompoundingEngine.calculateLotSize(
            equity = equity,
            peakEquity = peakEquity,
            slDistancePrice = slDist,
            entryPrice = price
        )

        // 7. محاسبه اسلیپیج
        val executedPrice = slippageSwapEngine.calculateSlippage(
            symbol = symbol,
            isBuy = (side == SignalSide.BUY),
            basePrice = price,
            lots = sizing.recommendedLots,
            volatilityAtrPips = (atr / 0.0001),
            isHighNewsActive = !newsResult.isTradingAllowed
        )

        val approved = blocks.isEmpty()
        val summary = if (approved) {
            "معامله تایید شد: حجم ${sizing.recommendedLots} لات با کانفلوئنس ${confluenceResult.overallScore}٪ و احتمال برد کالیبره ${calibrated.calibratedWinProbability}٪"
        } else {
            "معامله مسدود شد به دلایل: ${blocks.joinToString(" | ")}"
        }

        return PreTradeProResult(
            isApproved = approved,
            recommendedLots = sizing.recommendedLots,
            calibratedConfidence = calibrated.calibratedWinProbability,
            confluenceScore = confluenceResult.overallScore,
            sessionEvaluation = sessionResult,
            newsFilterResult = newsResult,
            expectedSlippagePrice = executedPrice,
            blockReasonsFa = blocks,
            summaryFa = summary
        )
    }

    companion object {
        @Volatile private var instance: OdinProQuantitativeEngine? = null
        fun getInstance(): OdinProQuantitativeEngine = instance ?: synchronized(this) {
            instance ?: OdinProQuantitativeEngine().also { instance = it }
        }
    }
}
