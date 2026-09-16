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

class TV80PercentStrategy : BaseStrategy(QuantStrategyType.TV_80_PERCENT, mutableMapOf(
    "min_rr" to 2.0,
    "min_confluence" to 5,
    "min_winrate" to 80,
    "min_confidence" to 80,
    "indicators_count" to 20
)) {
    override fun getDescription(isPersian: Boolean): String = if (isPersian)
        "TV 80% WR - سختگیرانه‌ترین فیلتر: بررسی تمام 20+ اندیکاتور TradingView (Trend, Momentum, Volatility, Volume) + LIT + حداقل RR 1:2 + Confluence >=5 + اعتماد >=80% + WR تاریخی >=80% وگرنه بلوکه. بسیار نادر اما طلایی - 2-5 ترید در 90 روز. Breakeven RR 1:2 فقط 33% WR لازم دارد پس 80% فوق‌العاده سودده است."
    else
        "TV 80% WR - Strictest filter: Check all 20+ TradingView indicators (Trend, Momentum, Volatility, Volume) + LIT + Min RR 1:2 + Confluence >=5 + Confidence >=80% + Historical WR >=80% else BLOCKED. Very rare but golden - 2-5 trades per 90 days. Breakeven RR 1:2 needs only 33% WR so 80% extremely profitable."

    override fun getEntryRules(isPersian: Boolean): String = if (isPersian)
        "ورود 80%: 1) چک 20+ اندیکاتور TV → امتیاز Confluence 2) LIT: سوئیپ+BOS+OB+FVG (3x وزن) 3) محاسبه RR >=2.0 4) WR تاریخی >=80%؟ اگر <80% → BLOCK 5) اعتماد >=80% → ورود. فقط بهترین ستاپ‌ها!"
    else
        "80% Entry: 1) Check 20+ TV indicators → Confluence score 2) LIT: sweep+BOS+OB+FVG (3x weight) 3) Calc RR >=2.0 4) Historical WR >=80%? If <80% → BLOCK 5) Confidence >=80% → Enter. Only best setups!"

    override fun getExitRules(isPersian: Boolean): String = if (isPersian)
        "خروج 80%: SL پشت OB/سوئیپ, TP نقدینگی مخالف RR 1:2-1:3, خروج زودهنگام اگر BOS مخالف یا Confluence <3. مدیریت: 1% ریسک, 3% DD روزانه Kill-switch"
    else
        "80% Exit: SL beyond OB/sweep, TP opposite liquidity RR 1:2-1:3, Early exit if opposite BOS or Confluence <3. Risk: 1% per trade, 3% daily DD Kill-switch"

    override fun generateMockSignal(symbol: String, price: Double): QuantSignal? {
        // TV 80% mock - very high confidence but rare
        val side = if (Math.random() > 0.5) SignalSide.BUY else SignalSide.SELL
        val atr = price * 0.01
        val sl = if (side == SignalSide.BUY) price - atr*1.5 else price + atr*1.5
        val tp = if (side == SignalSide.BUY) price + atr*3.5 else price - atr*3.5 // RR 1:2.33

        return QuantSignal(
            id = "tv80_${System.currentTimeMillis()}",
            symbol = symbol,
            timeframe = "1h",
            strategy = type,
            side = side,
            entryPrice = price,
            slPrice = sl,
            tpPrice = tp,
            confidence = 0.82 + Math.random()*0.13, // 82-95% for 80% filter
            reason = "TV 80%: 12 تاییدیه (EMA, RSI, MACD, BB, SuperTrend, LIT 3x, BOS 2x) + RR 1:2.3 + WR 82% + Conf 85% → VALID",
            regime = MarketRegime.TRENDING
        )
    }
}

class LITStrategy : BaseStrategy(QuantStrategyType.LIT_LIQUIDITY_INVERSION, mutableMapOf(
    "swing_lookback" to 10,
    "liquidity_tolerance" to 0.001,
    "sweep_threshold" to 0.002,
    "min_rr" to 2.0,
    "sl_buffer" to 0.001
)) {
    override fun getDescription(isPersian: Boolean): String = if (isPersian)
        "LIT - اینورژن نقدینگی (SMC) - مطمئن‌ترین روش: معامله بعد از سوئیپ نقدینگی + شکست ساختار + اردر بلاک. کیفیت بر کمیت. وین ریت معمول 50-65% با RR 1:2+"
    else
        "LIT - Liquidity Inversion Trading (SMC) - Most reliable: Trades after liquidity sweep + BOS + Order Block. Quality over quantity. Typical 50-65% WR with RR 1:2+"

    override fun getEntryRules(isPersian: Boolean): String = if (isPersian)
        "ورود LIT: 1) شناسایی استخر نقدینگی (سقف/کف مساوی) 2) سوئیپ + ریجکشن 3) BOS صعودی/نزولی 4) ورود در اردر بلاک 50% + FVG - فقط در Discount/Premium zone"
    else
        "LIT Entry: 1) Find liquidity pool (equal highs/lows) 2) Sweep + rejection 3) Bull/Bear BOS 4) Enter at OB 50% + FVG - Only in Discount/Premium"

    override fun getExitRules(isPersian: Boolean): String = if (isPersian)
        "خروج LIT: SL پشت OB یا سوئیپ + بافر, TP در نقدینگی مخالف (1:2 تا 1:3), خروج اگر BOS مخالف یا شکست OB"
    else
        "LIT Exit: SL beyond OB or sweep + buffer, TP at opposite liquidity (1:2 to 1:3), Exit if opposite BOS or OB break"

    override fun generateMockSignal(symbol: String, price: Double): QuantSignal? {
        // LIT mock with higher confidence
        val side = if (Math.random() > 0.48) SignalSide.BUY else SignalSide.SELL // Slight bullish bias
        val atr = price * 0.008 // Tighter for LIT
        val sl = if (side == SignalSide.BUY) price - atr*1.5 else price + atr*1.5
        val tp = if (side == SignalSide.BUY) price + atr*3.0 else price - atr*3.0 // 1:2 RR

        return QuantSignal(
            id = "lit_${System.currentTimeMillis()}",
            symbol = symbol,
            timeframe = "1h",
            strategy = type,
            side = side,
            entryPrice = price,
            slPrice = sl,
            tpPrice = tp,
            confidence = 0.70 + Math.random()*0.20, // Higher confidence for LIT 70-90%
            reason = "LIT: Sweep + BOS + OB retest in ${if (side==SignalSide.BUY) "discount" else "premium"} zone - RR 1:2",
            regime = MarketRegime.TRENDING
        )
    }
}
