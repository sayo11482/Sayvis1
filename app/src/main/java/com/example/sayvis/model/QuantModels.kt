package com.example.sayvis.model

/**
 * ODIN QUANT - Quantitative Trading Models for Android
 * Professional trading system models
 */

enum class QuantStrategyType(
    val id: String,
    val labelEn: String,
    val labelFa: String,
    val priority: Int,
    val bestRegime: String
) {
    TREND_FOLLOWING("trend_following", "Trend Following (Multi-TF)", "دنباله‌روی روند چندتایم‌فریم", 1, "trending"),
    MEAN_REVERSION("mean_reversion", "Mean Reversion", "بازگشت به میانگین", 2, "ranging"),
    MOMENTUM_BREAKOUT("momentum_breakout", "Momentum Breakout", "شکست مومنتوم", 3, "high_vol"),
    PAIRS_TRADING("pairs_trading", "Pairs Trading", "معاملات جفتی", 4, "all"),
    VOLATILITY_REGIME("volatility_regime", "Volatility Regime", "تشخیص رژیم نوسان", 5, "meta");

    fun label(isPersian: Boolean): String = if (isPersian) labelFa else labelEn
}

enum class MarketRegime(val labelEn: String, val labelFa: String) {
    TRENDING("Trending", "روندی"),
    RANGING("Ranging", "رنج"),
    HIGH_VOL("High Volatility", "پرنوسان"),
    UNKNOWN("Unknown", "نامشخص");

    fun label(isPersian: Boolean): String = if (isPersian) labelFa else labelEn
}

data class QuantSignal(
    val id: String,
    val symbol: String,
    val timeframe: String,
    val strategy: QuantStrategyType,
    val side: Int, // 1=long, -1=short
    val signalType: String, // entry, exit
    val entryPrice: Double,
    val slPrice: Double,
    val tpPrice: Double,
    val confidence: Double,
    val reason: String,
    val reasonFa: String,
    val regime: MarketRegime,
    val timestamp: Long = System.currentTimeMillis()
)

data class QuantPosition(
    val id: String,
    val symbol: String,
    val strategy: QuantStrategyType,
    val side: Int,
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
    val currentCapital: Double,
    val initialCapital: Double,
    val totalPnl: Double,
    val totalPnlPercent: Double,
    val totalDrawdown: Double,
    val dailyPnl: Double,
    val dailyDrawdown: Double,
    val openPositions: Int,
    val killSwitchActive: Boolean,
    val totalStopActive: Boolean,
    val maxRiskPerTrade: Double = 1.0,
    val maxDailyDd: Double = 3.0,
    val maxTotalDd: Double = 15.0
) {
    fun isSafeToTrade(): Boolean = !killSwitchActive && !totalStopActive
}

data class QuantConfig(
    val maxRiskPerTrade: Double = 1.0,
    val maxDailyDd: Double = 3.0,
    val maxTotalDd: Double = 15.0,
    val enabledStrategies: List<QuantStrategyType> = listOf(
        QuantStrategyType.TREND_FOLLOWING,
        QuantStrategyType.MEAN_REVERSION,
        QuantStrategyType.MOMENTUM_BREAKOUT
    ),
    val defaultSymbol: String = "BTC/USDT",
    val defaultTimeframe: String = "1h"
)
