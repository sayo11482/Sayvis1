package com.example.sayvis.model

enum class LitSignalType(val labelEn: String, val labelFa: String) {
    BULLISH_ORDER_BLOCK("Bullish Liquidity Inversion", "اینورژن نقدینگی صعودی"),
    BEARISH_DISPLACEMENT("Bearish Displacement", "جایجایی نزولی ساختار"),
    ACCUMULATION_RANGE("Range Accumulation", "تراکم در محدوده رنج"),
    BREAK_OF_STRUCTURE("Break of Structure (BOS)", "شکست ساختار روند")
}

data class LitAnalysisSignal(
    val id: String,
    val assetSymbol: String,
    val timeframe: String,
    val signalType: LitSignalType,
    val entryPrice: Double,
    val invalidationStop: Double,
    val takeProfitTarget: Double,
    val riskRewardRatio: Double,
    val notes: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class TradingGateState(
    val liveTradingBlocked: Boolean = true, // paper simulation by default (owner switches to LIVE in Gateway)
    val paperTradingMode: Boolean = true,
    val killSwitchEngaged: Boolean = false,
    val maxDailyDrawdownLimitUsd: Double = 50.0,
    val activePositionsCount: Int = 0,
    val executionMode: com.example.sayvis.settings.TradingExecutionMode = com.example.sayvis.settings.TradingExecutionMode.PAPER_SIMULATION
)
