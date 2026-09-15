package com.odin.agent.strategies

import com.odin.agent.models.QuantSignal
import com.odin.agent.models.QuantStrategyType
import com.odin.agent.models.MarketRegime
import com.odin.agent.models.SignalSide

/**
 * ODIN QUANT - Base Strategy for Android
 * Mirrors Python quant/strategies/base_strategy.py
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

    open fun generateMockSignal(symbol: String, price: Double): QuantSignal? {
        // Mock signal for demo - real logic in Python backend
        val side = if (Math.random() > 0.5) SignalSide.BUY else SignalSide.SELL
        val atr = price * 0.01
        val sl = if (side == SignalSide.BUY) price - atr*2 else price + atr*2
        val tp = if (side == SignalSide.BUY) price + atr*3 else price - atr*3

        return QuantSignal(
            id = "sig_${System.currentTimeMillis()}",
            symbol = symbol,
            timeframe = "1h",
            strategy = type,
            side = side,
            entryPrice = price,
            slPrice = sl,
            tpPrice = tp,
            confidence = 0.65 + Math.random()*0.25,
            reason = "${type.labelEn}: Mock signal for demo",
            regime = MarketRegime.TRENDING
        )
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
        "دنباله‌روی روند با فیلتر چندتایم‌فریم - پایدارترین استراتژی بلندمدت. 30% روندها کل ضررها را جبران می‌کند."
    else
        "Trend Following with Multi-Timeframe filter - Most robust long-term. 30% of trends pay for all losses."

    override fun getEntryRules(isPersian: Boolean): String = if (isPersian)
        "ورود: کراس EMA20/50 + ADX>25 + حجم>MA*1.5 + فیلتر 4h صعودی"
    else
        "Entry: EMA20/50 crossover + ADX>25 + Volume>MA*1.5 + HTF bullish"

    override fun getExitRules(isPersian: Boolean): String = if (isPersian)
        "خروج: SL ATR*2, TP ATR*3, Trailing ATR*1.5, Time 20 کندل"
    else
        "Exit: SL ATR*2, TP ATR*3, Trailing ATR*1.5, Time 20 bars"
}

class MeanReversionStrategy : BaseStrategy(QuantStrategyType.MEAN_REVERSION, mutableMapOf(
    "bb_period" to 20,
    "bb_std" to 2.0,
    "rsi_oversold" to 30,
    "rsi_overbought" to 70
)) {
    override fun getDescription(isPersian: Boolean): String = if (isPersian)
        "بازگشت به میانگین - بهترین در بازار رنج. قیمت به میانگین برمی‌گردد."
    else
        "Mean Reversion - Best in ranging markets. Price reverts to mean."

    override fun getEntryRules(isPersian: Boolean): String = if (isPersian)
        "ورود: قیمت < BB lower + RSI<30 + Z-Score<-2"
    else
        "Entry: Price < BB lower + RSI<30 + Z-Score<-2"

    override fun getExitRules(isPersian: Boolean): String = if (isPersian)
        "خروج: وسط BB یا RSI 50"
    else
        "Exit: Middle BB or RSI 50"
}

class MomentumBreakoutStrategy : BaseStrategy(QuantStrategyType.MOMENTUM_BREAKOUT, mutableMapOf(
    "lookback" to 20,
    "atr_threshold" to 1.0,
    "volume_spike" to 2.0
)) {
    override fun getDescription(isPersian: Boolean): String = if (isPersian)
        "شکست مومنتوم با فیلتر نوسان - شکار حرکات انفجاری"
    else
        "Momentum Breakout with Volatility filter - Captures explosive moves"

    override fun getEntryRules(isPersian: Boolean): String = if (isPersian)
        "ورود: شکست سقف 20 کندلی + ATR>MA + جهش حجم"
    else
        "Entry: Breakout 20-bar high + ATR>MA + Volume spike"

    override fun getExitRules(isPersian: Boolean): String = if (isPersian)
        "خروج: Trailing ATR*1.0 + Time 10 کندل"
    else
        "Exit: Trailing ATR*1.0 + Time 10 bars"
}

class PairsTradingStrategy : BaseStrategy(QuantStrategyType.PAIRS_TRADING, mutableMapOf(
    "zscore_entry" to 2.0,
    "zscore_exit" to 0.0
)) {
    override fun getDescription(isPersian: Boolean): String = if (isPersian)
        "معاملات جفتی / آربیتراژ آماری - خنثی نسبت به بازار، برای BTC/ETH"
    else
        "Pairs Trading / Stat Arb - Market neutral for BTC/ETH"

    override fun getEntryRules(isPersian: Boolean): String = if (isPersian)
        "ورود: Z-Score spread >2 + cointegration"
    else
        "Entry: Z-Score spread >2 + cointegration"

    override fun getExitRules(isPersian: Boolean): String = if (isPersian)
        "خروج: Z→0 یا شکست cointegration"
    else
        "Exit: Z→0 or cointegration break"
}

class VolatilityRegimeStrategy : BaseStrategy(QuantStrategyType.VOLATILITY_REGIME, mutableMapOf(
    "adx_trending" to 25,
    "adx_ranging" to 20
)) {
    override fun getDescription(isPersian: Boolean): String = if (isPersian)
        "تشخیص رژیم نوسان - متا-استراتژی که استراتژی مناسب را انتخاب می‌کند"
    else
        "Volatility Regime Detection - Meta-strategy that switches strategies"

    override fun getEntryRules(isPersian: Boolean): String = if (isPersian)
        "رژیم‌ها: روندی (ADX>25), رنج (ADX<20+BB squeeze), پرنوسان (ATR>2*MA)"
    else
        "Regimes: Trending (ADX>25), Ranging (ADX<20+BB squeeze), High Vol (ATR>2*MA)"

    override fun getExitRules(isPersian: Boolean): String = if (isPersian)
        "عمل: تغییر استراتژی فعال بر اساس رژیم"
    else
        "Action: Switch active strategy based on regime"
}
