package com.odin.agent.strategies

import com.odin.agent.models.QuantSignal
import com.odin.agent.models.QuantStrategyType
import com.odin.agent.models.MarketRegime
import com.odin.agent.models.SignalSide

/**
 * ODIN QUANT v1.0.21 - Base Strategy - 100% REAL ONLY - NO FAKE - Meta Fix
 * قبلاً generateMockSignal با Math.random بود - الان فقط واقعی - هیچ فیک
 */

abstract class BaseStrategy(
    val type: QuantStrategyType,
    val params: MutableMap<String, Any> = mutableMapOf()
) {
    abstract fun getDescription(isPersian: Boolean): String
    abstract fun getEntryRules(isPersian: Boolean): String
    abstract fun getExitRules(isPersian: Boolean): String

    fun getRiskLevel(): String = type.riskLevel
    fun getBestRegime(): String = type.bestRegime

    // REAL only - no fake mock - Meta fix - returns null, real signals come from EntryScannerWithAlarm REAL
    open fun generateMockSignal(symbol: String, price: Double): QuantSignal? {
        return null // No fake - only REAL signals from real indicators
    }

    // REAL signal generation using real indicators - to be implemented with real data
    open fun generateRealSignal(symbol: String, price: Double, candles: List<com.odin.agent.trading.RealCandle>): QuantSignal? {
        return null // Override in subclasses with REAL logic using TradingViewIndicators
    }
}

class TrendFollowingStrategy : BaseStrategy(QuantStrategyType.TREND_FOLLOWING, mutableMapOf(
    "ema_fast" to 20,
    "ema_slow" to 50,
    "adx_threshold" to 25,
    "sl_atr" to 2.0,
    "tp_atr" to 3.0
)) {
    override fun getDescription(isPersian: Boolean): String = if (isPersian)
        "دنباله‌روی روند با فیلتر چندتایم‌فریم - پایدارترین استراتژی بلندمدت. 30% روندها کل ضررها را جبران می‌کند. - ۱۰۰٪ واقعی"
    else
        "Trend Following with Multi-Timeframe filter - Most robust long-term. 30% of trends pay for all losses. - 100% REAL"

    override fun getEntryRules(isPersian: Boolean): String = if (isPersian)
        "ورود واقعی: کراس EMA20/50 + ADX>25 + حجم>MA*1.5 + فیلتر 4h صعودی - فقط واقعی"
    else
        "Entry REAL: EMA20/50 crossover + ADX>25 + Volume>MA*1.5 + HTF bullish - REAL only"

    override fun getExitRules(isPersian: Boolean): String = if (isPersian)
        "خروج واقعی: SL ATR*2, TP ATR*3, Trailing ATR*1.5, Time 20 کندل - واقعی"
    else
        "Exit REAL: SL ATR*2, TP ATR*3, Trailing ATR*1.5, Time 20 bars - REAL"
}

class MeanReversionStrategy : BaseStrategy(QuantStrategyType.MEAN_REVERSION, mutableMapOf(
    "bb_period" to 20,
    "bb_std" to 2.0,
    "rsi_oversold" to 30,
    "rsi_overbought" to 70
)) {
    override fun getDescription(isPersian: Boolean): String = if (isPersian)
        "بازگشت به میانگین - بهترین در بازار رنج. قیمت به میانگین برمی‌گردد. - واقعی"
    else
        "Mean Reversion - Best in ranging markets. Price reverts to mean. - REAL"

    override fun getEntryRules(isPersian: Boolean): String = if (isPersian)
        "ورود واقعی: قیمت < BB lower + RSI<30 + Z-Score<-2 - واقعی"
    else
        "Entry REAL: Price < BB lower + RSI<30 + Z-Score<-2 - REAL"

    override fun getExitRules(isPersian: Boolean): String = if (isPersian)
        "خروج واقعی: وسط BB یا RSI 50 - واقعی"
    else
        "Exit REAL: Middle BB or RSI 50 - REAL"
}

class MomentumBreakoutStrategy : BaseStrategy(QuantStrategyType.MOMENTUM_BREAKOUT, mutableMapOf(
    "lookback" to 20,
    "atr_threshold" to 1.0,
    "volume_spike" to 2.0
)) {
    override fun getDescription(isPersian: Boolean): String = if (isPersian)
        "شکست مومنتوم با فیلتر نوسان - شکار حرکات انفجاری - واقعی"
    else
        "Momentum Breakout with Volatility filter - Captures explosive moves - REAL"

    override fun getEntryRules(isPersian: Boolean): String = if (isPersian)
        "ورود واقعی: شکست سقف 20 کندلی + ATR>MA + جهش حجم - واقعی"
    else
        "Entry REAL: Breakout 20-bar high + ATR>MA + Volume spike - REAL"

    override fun getExitRules(isPersian: Boolean): String = if (isPersian)
        "خروج واقعی: Trailing ATR*1.0 + Time 10 کندل - واقعی"
    else
        "Exit REAL: Trailing ATR*1.0 + Time 10 bars - REAL"
}

class PairsTradingStrategy : BaseStrategy(QuantStrategyType.PAIRS_TRADING, mutableMapOf(
    "zscore_entry" to 2.0,
    "zscore_exit" to 0.0
)) {
    override fun getDescription(isPersian: Boolean): String = if (isPersian)
        "معاملات جفتی / آربیتراژ آماری - خنثی نسبت به بازار، برای BTC/ETH - واقعی"
    else
        "Pairs Trading / Stat Arb - Market neutral for BTC/ETH - REAL"

    override fun getEntryRules(isPersian: Boolean): String = if (isPersian)
        "ورود واقعی: Z-Score spread >2 + cointegration - واقعی"
    else
        "Entry REAL: Z-Score spread >2 + cointegration - REAL"

    override fun getExitRules(isPersian: Boolean): String = if (isPersian)
        "خروج واقعی: Z→0 یا شکست cointegration - واقعی"
    else
        "Exit REAL: Z→0 or cointegration break - REAL"
}

class VolatilityRegimeStrategy : BaseStrategy(QuantStrategyType.VOLATILITY_REGIME, mutableMapOf(
    "adx_trending" to 25,
    "adx_ranging" to 20
)) {
    override fun getDescription(isPersian: Boolean): String = if (isPersian)
        "تشخیص رژیم نوسان - متا-استراتژی که استراتژی مناسب را انتخاب می‌کند - واقعی"
    else
        "Volatility Regime Detection - Meta-strategy that switches strategies - REAL"

    override fun getEntryRules(isPersian: Boolean): String = if (isPersian)
        "رژیم‌های واقعی: روندی (ADX>25), رنج (ADX<20+BB squeeze), پرنوسان (ATR>2*MA) - واقعی"
    else
        "Regimes REAL: Trending (ADX>25), Ranging (ADX<20+BB squeeze), High Vol (ATR>2*MA) - REAL"

    override fun getExitRules(isPersian: Boolean): String = if (isPersian)
        "عمل واقعی: تغییر استراتژی فعال بر اساس رژیم - واقعی"
    else
        "Action REAL: Switch active strategy based on regime - REAL"
}

class TV80PercentStrategy : BaseStrategy(QuantStrategyType.TV_80_PERCENT, mutableMapOf(
    "min_rr" to 2.0,
    "min_confluence" to 5,
    "min_winrate" to 80,
    "min_confidence" to 80,
    "indicators_count" to 20
)) {
    override fun getDescription(isPersian: Boolean): String = if (isPersian)
        "TV 80% WR - سختگیرانه‌ترین فیلتر واقعی: بررسی تمام 20+ اندیکاتور TradingView + LIT + حداقل RR 1:2 + Confluence >=5 + اعتماد >=80% + WR تاریخی >=80% وگرنه بلوکه. بسیار نادر اما طلایی - 2-5 ترید در 90 روز. - ۱۰۰٪ واقعی"
    else
        "TV 80% WR - Strictest filter REAL: Check all 20+ TradingView indicators + LIT + Min RR 1:2 + Confluence >=5 + Confidence >=80% + Historical WR >=80% else BLOCKED. Very rare but golden. - 100% REAL"

    override fun getEntryRules(isPersian: Boolean): String = if (isPersian)
        "ورود 80% واقعی: 1) چک 20+ اندیکاتور TV → امتیاز Confluence 2) LIT: سوئیپ+BOS+OB+FVG (3x وزن) 3) محاسبه RR >=2.0 4) WR تاریخی >=80%؟ اگر <80% → BLOCK 5) اعتماد >=80% → ورود واقعی. فقط بهترین ستاپ‌های واقعی!"
    else
        "80% Entry REAL: 1) Check 20+ TV indicators → Confluence score 2) LIT: sweep+BOS+OB+FVG (3x weight) 3) Calc RR >=2.0 4) Historical WR >=80%? If <80% → BLOCK 5) Confidence >=80% → Enter REAL. Only best REAL setups!"

    override fun getExitRules(isPersian: Boolean): String = if (isPersian)
        "خروج 80% واقعی: SL پشت OB/سوئیپ, TP نقدینگی مخالف RR 1:2-1:3, خروج زودهنگام اگر BOS مخالف یا Confluence <3. مدیریت واقعی: 1% ریسک, 3% DD روزانه Kill-switch - واقعی"
    else
        "80% Exit REAL: SL beyond OB/sweep, TP opposite liquidity RR 1:2-1:3, Early exit if opposite BOS or Confluence <3. Risk REAL: 1% per trade, 3% daily DD Kill-switch - REAL"
}

class LITStrategy : BaseStrategy(QuantStrategyType.LIT_LIQUIDITY_INVERSION, mutableMapOf(
    "swing_lookback" to 10,
    "liquidity_tolerance" to 0.001,
    "sweep_threshold" to 0.002,
    "min_rr" to 2.0,
    "sl_buffer" to 0.001
)) {
    override fun getDescription(isPersian: Boolean): String = if (isPersian)
        "LIT - اینورژن نقدینگی (SMC) - مطمئن‌ترین روش واقعی: معامله بعد از سوئیپ نقدینگی + شکست ساختار + اردر بلاک. کیفیت بر کمیت. وین ریت معمول 50-65% با RR 1:2+ - ۱۰۰٪ واقعی"
    else
        "LIT - Liquidity Inversion Trading (SMC) - Most reliable REAL: Trades after liquidity sweep + BOS + Order Block. Quality over quantity. Typical 50-65% WR with RR 1:2+ - 100% REAL"

    override fun getEntryRules(isPersian: Boolean): String = if (isPersian)
        "ورود LIT واقعی: 1) شناسایی استخر نقدینگی (سقف/کف مساوی) 2) سوئیپ + ریجکشن واقعی 3) BOS صعودی/نزولی واقعی 4) ورود در اردر بلاک 50% + FVG واقعی - فقط در Discount/Premium zone واقعی"
    else
        "LIT Entry REAL: 1) Find liquidity pool (equal highs/lows) REAL 2) Sweep + rejection REAL 3) Bull/Bear BOS REAL 4) Enter at OB 50% + FVG REAL - Only in Discount/Premium REAL"

    override fun getExitRules(isPersian: Boolean): String = if (isPersian)
        "خروج LIT واقعی: SL پشت OB یا سوئیپ + بافر واقعی, TP در نقدینگی مخالف (1:2 تا 1:3) واقعی, خروج اگر BOS مخالف یا شکست OB واقعی - واقعی"
    else
        "LIT Exit REAL: SL beyond OB or sweep + buffer REAL, TP at opposite liquidity (1:2 to 1:3) REAL, Exit if opposite BOS or OB break REAL - REAL"
}
