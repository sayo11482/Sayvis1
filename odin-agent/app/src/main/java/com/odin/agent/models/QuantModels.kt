package com.odin.agent.models

/**
 * ODIN QUANT - Quantitative Trading Models
 * Completely independent from SAYVIS
 */

enum class QuantStrategyType(
    val id: String,
    val labelEn: String,
    val labelFa: String,
    val priority: Int,
    val bestRegime: String,
    val riskLevel: String
) {
    // LIT is most reliable - Priority 0
    LIT_LIQUIDITY_INVERSION("lit_liquidity_inversion", "LIT - Liquidity Inversion (SMC)", "LIT - اینورژن نقدینگی (SMC)", 0, "all", "LOW"),
    TREND_FOLLOWING("trend_following", "Trend Following (Multi-TF)", "دنباله‌روی روند چندتایم‌فریم", 1, "trending", "MEDIUM"),
    MEAN_REVERSION("mean_reversion", "Mean Reversion", "بازگشت به میانگین", 2, "ranging", "MEDIUM"),
    MOMENTUM_BREAKOUT("momentum_breakout", "Momentum Breakout", "شکست مومنتوم", 3, "high_vol", "MEDIUM"),
    PAIRS_TRADING("pairs_trading", "Pairs Trading", "معاملات جفتی", 4, "all", "LOW"),
    VOLATILITY_REGIME("volatility_regime", "Volatility Regime", "تشخیص رژیم نوسان", 5, "meta", "LOW");

    fun label(isPersian: Boolean): String = if (isPersian) labelFa else labelEn
    
    fun isLIT(): Boolean = this == LIT_LIQUIDITY_INVERSION
}

enum class LitSignalType(val labelEn: String, val labelFa: String) {
    BULLISH_SWEEP_OB("Bullish Sweep + OB", "سوئیپ صعودی + اردر بلاک"),
    BEARISH_SWEEP_OB("Bearish Sweep + OB", "سوئیپ نزولی + اردر بلاک"),
    BULLISH_BOS("Bullish BOS", "شکست ساختار صعودی"),
    BEARISH_BOS("Bearish BOS", "شکست ساختار نزولی"),
    FVG_MITIGATION("FVG Mitigation", "اصلاح FVG"),
    LIQUIDITY_POOL("Liquidity Pool", "استخر نقدینگی");

    fun label(isPersian: Boolean): String = if (isPersian) labelFa else labelEn
}

enum class MarketRegime(val labelEn: String, val labelFa: String) {
    TRENDING("Trending", "روندی"),
    RANGING("Ranging", "رنج"),
    HIGH_VOL("High Volatility", "پرنوسان"),
    UNKNOWN("Unknown", "نامشخص");

    fun label(isPersian: Boolean): String = if (isPersian) labelFa else labelEn
}

enum class SignalSide(val value: Int, val labelEn: String, val labelFa: String) {
    BUY(1, "BUY", "خرید"),
    SELL(-1, "SELL", "فروش"),
    HOLD(0, "HOLD", "انتظار");

    fun label(isPersian: Boolean): String = if (isPersian) labelFa else labelEn
}

data class QuantSignal(
    val id: String,
    val symbol: String,
    val timeframe: String,
    val strategy: QuantStrategyType,
    val side: SignalSide,
    val entryPrice: Double,
    val slPrice: Double,
    val tpPrice: Double,
    val confidence: Double,
    val reason: String,
    val regime: MarketRegime,
    val timestamp: Long = System.currentTimeMillis()
)

data class QuantPosition(
    val id: String,
    val symbol: String,
    val strategy: QuantStrategyType,
    val side: SignalSide,
    val entryPrice: Double,
    val currentPrice: Double,
    val slPrice: Double,
    val tpPrice: Double,
    val size: Double,
    val pnl: Double,
    val pnlPercent: Double,
    val entryTime: Long,
    val confidence: Double
)

data class QuantBacktestResult(
    val id: String,
    val symbol: String,
    val strategy: QuantStrategyType,
    val totalTrades: Int,
    val winrate: Double,
    val totalPnl: Double,
    val totalPnlPercent: Double,
    val sharpe: Double,
    val sortino: Double,
    val maxDd: Double,
    val profitFactor: Double,
    val startDate: String,
    val endDate: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class QuantRiskStatus(
    val currentCapital: Double = 10000.0,
    val initialCapital: Double = 10000.0,
    val totalPnl: Double = 0.0,
    val totalPnlPercent: Double = 0.0,
    val totalDrawdown: Double = 0.0,
    val dailyPnl: Double = 0.0,
    val dailyDrawdown: Double = 0.0,
    val openPositions: Int = 0,
    val killSwitchActive: Boolean = false,
    val totalStopActive: Boolean = false,
    val maxRiskPerTrade: Double = 1.0,
    val maxDailyDd: Double = 3.0,
    val maxTotalDd: Double = 15.0
) {
    fun isSafeToTrade(): Boolean = !killSwitchActive && !totalStopActive
    fun dailyDdPercent(): Double = if (dailyPnl < 0) ( -dailyPnl / initialCapital * 100) else 0.0
}

data class OdinConfig(
    val maxRiskPerTrade: Double = 1.0,
    val maxDailyDd: Double = 3.0,
    val maxTotalDd: Double = 15.0,
    val enabledStrategies: List<QuantStrategyType> = listOf(
        QuantStrategyType.TREND_FOLLOWING,
        QuantStrategyType.MEAN_REVERSION,
        QuantStrategyType.MOMENTUM_BREAKOUT
    ),
    val defaultSymbol: String = "BTC/USDT",
    val defaultTimeframe: String = "1h",
    val backendUrl: String = "http://192.168.1.100:8000",
    val isPersian: Boolean = true
)
