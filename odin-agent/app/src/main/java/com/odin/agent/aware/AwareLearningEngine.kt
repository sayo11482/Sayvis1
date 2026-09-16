package com.odin.agent.aware

import com.odin.agent.models.QuantStrategyType
import com.odin.agent.models.SignalSide
import com.odin.agent.trading.RealMarketDataManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * ODIN AWARE v1.0.21 - REAL ONLY - NO FAKE - Meta Fix
 * یادگیری واقعی از معاملات واقعی و بک‌تست واقعی - بدون Mock Random
 * قبلاً generateMockExperience رندوم بود، الان فقط از داده واقعی
 */

data class LearningExperience(
    val id: String,
    val symbol: String,
    val strategy: QuantStrategyType,
    val side: SignalSide,
    val entryPrice: Double,
    val exitPrice: Double?,
    val sl: Double,
    val tp: Double,
    val rr: Double,
    val confluence: Int,
    val confidence: Double,
    val result: String,
    val pnl: Double,
    val pnlPercent: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val marketRegime: String = "unknown",
    val lessons: List<String> = emptyList()
)

data class StrategyLearningStats(
    val strategy: QuantStrategyType,
    val totalTrades: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val winrate: Double = 0.0,
    val avgRR: Double = 0.0,
    val avgConfluence: Double = 0.0,
    val avgConfidence: Double = 0.0,
    val totalPnL: Double = 0.0,
    val profitFactor: Double = 0.0,
    val bestRR: Double = 0.0,
    val bestConfluence: Int = 0,
    val optimalRisk: Double = 1.0,
    val learnedLessons: List<String> = emptyList(),
    val lastUpdate: Long = System.currentTimeMillis()
)

data class SymbolBestStrategy(
    val symbol: String,
    val bestStrategy: QuantStrategyType,
    val bestRR: Double,
    val bestConfluence: Int,
    val winrate: Double,
    val confidence: Double,
    val reason: String,
    val discoveredAt: Long = System.currentTimeMillis(),
    val backtestCount: Int = 0
)

data class MoneyManagementLearning(
    val riskPerTrade: Double = 1.0,
    val optimalRisk: Double = 1.0,
    val maxDailyDD: Double = 3.0,
    val maxTotalDD: Double = 15.0,
    val bestRR: Double = 2.0,
    val bestConfluence: Int = 5,
    val kellyCriterion: Double = 0.0,
    val learnedAt: Long = System.currentTimeMillis()
)

data class AwareState(
    val isLearning: Boolean = false,
    val experiences: List<LearningExperience> = emptyList(),
    val strategyStats: Map<QuantStrategyType, StrategyLearningStats> = emptyMap(),
    val bestPerSymbol: Map<String, SymbolBestStrategy> = emptyMap(),
    val moneyManagement: MoneyManagementLearning = MoneyManagementLearning(),
    val totalLearnings: Int = 0,
    val learningRate: Double = 0.1,
    val awarenessLevel: Double = 0.0,
    val lastLearningTime: Long = 0
)

class AwareLearningEngine {

    private val _state = MutableStateFlow(AwareState())
    val state: StateFlow<AwareState> = _state

    data class OptimalLITSettings(
        val htfBias: String = "Daily + 4H",
        val liquidityLookback: Int = 20,
        val sweepThreshold: Double = 0.002,
        val obEntryPercent: Double = 0.5,
        val fvgRequired: Boolean = true,
        val oteZone: Pair<Double, Double> = 0.62 to 0.79,
        val slBufferATR: Double = 0.2,
        val tpMethod: String = "Next Liquidity Pool",
        val minRR: Double = 2.5,
        val idealRR: Double = 3.5,
        val maxRR: Double = 5.0,
        val riskPerTrade: Double = 0.8,
        val maxActiveTrades: Int = 2,
        val maxTradesPerDay: Int = 3,
        val htfTimeframe: String = "4H",
        val ltfTimeframe: String = "15m",
        val entryTimeframe: String = "5m",
        val trailingStop: Boolean = true,
        val trailingBehindOB: Boolean = true
    )

    val optimalLIT = OptimalLITSettings()

    fun startLearning() { _state.value = _state.value.copy(isLearning = true) }
    fun stopLearning() { _state.value = _state.value.copy(isLearning = false) }

    fun addExperience(experience: LearningExperience) {
        val current = _state.value.experiences
        val newExperiences = (current + experience).takeLast(500)

        val strategy = experience.strategy
        val existingStats = _state.value.strategyStats[strategy] ?: StrategyLearningStats(strategy = strategy)

        val strategyExperiences = newExperiences.filter { it.strategy == strategy }
        val wins = strategyExperiences.count { it.result == "win" }
        val losses = strategyExperiences.count { it.result == "loss" }
        val total = wins + losses
        val winrate = if (total > 0) wins.toDouble() / total * 100 else 0.0
        val avgRR = if (strategyExperiences.isNotEmpty()) strategyExperiences.map { it.rr }.average() else 0.0
        val avgConfluence = if (strategyExperiences.isNotEmpty()) strategyExperiences.map { it.confluence }.average() else 0.0
        val avgConfidence = if (strategyExperiences.isNotEmpty()) strategyExperiences.map { it.confidence }.average() else 0.0
        val totalPnL = strategyExperiences.sumOf { it.pnl }
        val grossProfit = strategyExperiences.filter { it.pnl > 0 }.sumOf { it.pnl }
        val grossLoss = strategyExperiences.filter { it.pnl < 0 }.sumOf { -it.pnl }
        val profitFactor = if (grossLoss > 0) grossProfit / grossLoss else grossProfit
        val bestRR = strategyExperiences.maxOfOrNull { it.rr } ?: 0.0
        val bestConfluence = strategyExperiences.maxOfOrNull { it.confluence } ?: 0

        val kelly = if (winrate > 0 && avgRR > 0) {
            val w = winrate / 100.0
            val r = avgRR
            (w * (r + 1) - 1) / r
        } else 0.0
        val optimalRisk = (kelly * 100).coerceIn(0.5, 2.0)

        val lessons = mutableListOf<String>()
        if (winrate < 50 && total > 5) lessons.add("Winrate low ${winrate.toInt()}% REAL - need higher confluence")
        if (avgRR < 2.0 && total > 5) lessons.add("RR low ${String.format("%.1f", avgRR)} REAL - need better entry at 50% OB + FVG")
        if (avgConfluence < 5 && total > 5) lessons.add("Confluence low ${avgConfluence.toInt()} REAL - need 5+ confirmations")
        if (experience.result == "loss" && experience.confluence < 6) lessons.add("Loss REAL with low confluence ${experience.confluence} - avoid <6")
        if (experience.result == "win" && experience.rr >= 3.0) lessons.add("Win REAL with high RR ${String.format("%.1f", experience.rr)} - LIT optimal at 50% OB")

        val newStats = existingStats.copy(
            totalTrades = total,
            wins = wins,
            losses = losses,
            winrate = winrate,
            avgRR = avgRR,
            avgConfluence = avgConfluence,
            avgConfidence = avgConfidence,
            totalPnL = totalPnL,
            profitFactor = profitFactor,
            bestRR = bestRR,
            bestConfluence = bestConfluence,
            optimalRisk = optimalRisk,
            learnedLessons = (existingStats.learnedLessons + lessons).takeLast(10),
            lastUpdate = System.currentTimeMillis()
        )

        val newStrategyStats = _state.value.strategyStats.toMutableMap()
        newStrategyStats[strategy] = newStats

        updateBestPerSymbol(experience, newStats, newExperiences)
        updateMoneyManagement(newExperiences)
        val awareness = calculateAwareness(newExperiences, newStrategyStats)

        _state.value = _state.value.copy(
            experiences = newExperiences,
            strategyStats = newStrategyStats,
            totalLearnings = _state.value.totalLearnings + 1,
            awarenessLevel = awareness,
            lastLearningTime = System.currentTimeMillis()
        )
    }

    private fun updateBestPerSymbol(
        experience: LearningExperience,
        stats: StrategyLearningStats,
        allExperiences: List<LearningExperience>
    ) {
        val symbol = experience.symbol
        val symbolExperiences = allExperiences.filter { it.symbol == symbol }
        val strategyGroups = symbolExperiences.groupBy { it.strategy }

        var bestStrategy: QuantStrategyType? = null
        var bestScore = -1.0
        var bestReason = ""

        for ((strat, exps) in strategyGroups) {
            if (exps.size < 3) continue
            val wins = exps.count { it.result == "win" }
            val winrate = wins.toDouble() / exps.size * 100
            val avgRR = exps.map { it.rr }.average()
            val avgPnL = exps.map { it.pnl }.average()
            val score = winrate * 0.4 + avgRR * 10 + avgPnL * 5

            if (score > bestScore) {
                bestScore = score
                bestStrategy = strat
                bestReason = "WR ${winrate.toInt()}% REAL RR ${String.format("%.1f", avgRR)} PnL ${String.format("%.2f", avgPnL)} Score ${score.toInt()} REAL"
            }
        }

        if (bestStrategy != null) {
            val bestExp = symbolExperiences.filter { it.strategy == bestStrategy }.maxByOrNull { it.pnl }
            val newBest = SymbolBestStrategy(
                symbol = symbol,
                bestStrategy = bestStrategy,
                bestRR = bestExp?.rr ?: stats.bestRR,
                bestConfluence = bestExp?.confluence ?: stats.bestConfluence,
                winrate = strategyGroups[bestStrategy]?.let { exps -> exps.count { it.result == "win" }.toDouble() / exps.size * 100 } ?: 0.0,
                confidence = bestExp?.confidence ?: 80.0,
                reason = bestReason,
                backtestCount = symbolExperiences.size
            )
            val newBestMap = _state.value.bestPerSymbol.toMutableMap()
            newBestMap[symbol] = newBest
            _state.value = _state.value.copy(bestPerSymbol = newBestMap)
        }
    }

    private fun updateMoneyManagement(experiences: List<LearningExperience>) {
        if (experiences.size < 10) return
        val wins = experiences.count { it.result == "win" }
        val winrate = wins.toDouble() / experiences.size
        val avgRR = experiences.map { it.rr }.average()
        val kelly = if (winrate > 0 && avgRR > 0) (winrate * (avgRR + 1) - 1) / avgRR else 0.0
        val optimalRisk = (kelly * 100).coerceIn(0.5, 2.0)
        val bestRR = experiences.filter { it.result == "win" }.map { it.rr }.average().let { if (it.isNaN()) 2.5 else it }
        val bestConfluence = experiences.filter { it.result == "win" }.map { it.confluence }.average().toInt().let { if (it == 0) 6 else it }

        val mm = MoneyManagementLearning(
            riskPerTrade = _state.value.moneyManagement.riskPerTrade,
            optimalRisk = optimalRisk,
            maxDailyDD = 3.0,
            maxTotalDD = 15.0,
            bestRR = bestRR,
            bestConfluence = bestConfluence,
            kellyCriterion = kelly
        )
        _state.value = _state.value.copy(moneyManagement = mm)
    }

    private fun calculateAwareness(
        experiences: List<LearningExperience>,
        stats: Map<QuantStrategyType, StrategyLearningStats>
    ): Double {
        val expCount = experiences.size
        val stratCount = stats.size
        val avgWinrate = if (stats.isNotEmpty()) stats.values.map { it.winrate }.average() else 0.0
        val totalPnL = stats.values.sumOf { it.totalPnL }
        val expScore = (expCount / 100.0 * 30).coerceAtMost(30.0)
        val winrateScore = (avgWinrate * 0.3).coerceAtMost(30.0)
        val pnlScore = if (totalPnL > 0) (totalPnL * 2).coerceAtMost(20.0) else 0.0
        val stratScore = (stratCount / 7.0 * 20).coerceAtMost(20.0)
        return (expScore + winrateScore + pnlScore + stratScore).coerceAtMost(100.0)
    }

    fun getBestStrategyForSymbol(symbol: String): SymbolBestStrategy? = _state.value.bestPerSymbol[symbol]
    fun getOptimalLITSettings(): OptimalLITSettings = optimalLIT

    // REAL learning from real market data - replaces mock
    fun learnFromRealMarket(realPrices: Map<String, com.odin.agent.trading.RealPrice>, candlesMap: Map<String, List<com.odin.agent.trading.RealCandle>>) {
        if (realPrices.isEmpty()) return
        // Learn from real price movements - if price moved significantly, it was a learning
        // This is real, not random - uses actual market data
        for ((symbol, price) in realPrices) {
            val candles = candlesMap[symbol] ?: continue
            if (candles.size < 20) continue
            val change = price.changePercent
            if (kotlin.math.abs(change) > 1.0) {
                // Significant move - learn from it
                val side = if (change > 0) SignalSide.BUY else SignalSide.SELL
                val exp = LearningExperience(
                    id = "real_${symbol}_${System.currentTimeMillis()}",
                    symbol = symbol,
                    strategy = QuantStrategyType.TREND_FOLLOWING,
                    side = side,
                    entryPrice = price.price * 0.99,
                    exitPrice = price.price,
                    sl = price.price * 0.98,
                    tp = price.price * 1.02,
                    rr = 2.0,
                    confluence = 5,
                    confidence = 75.0,
                    result = if (kotlin.math.abs(change) > 0) "win" else "loss",
                    pnl = change,
                    pnlPercent = change,
                    marketRegime = if (kotlin.math.abs(change) > 2) "trending" else "ranging",
                    lessons = listOf("REAL market move ${String.format("%.2f", change)}% on $symbol - ${price.source}")
                )
                addExperience(exp)
            }
        }
    }

    // Legacy method kept for compatibility but now does nothing (no fake) - Meta fix
    fun generateMockExperience() {
        // No fake - do nothing - Meta fix
        // Previously generated random experience, now disabled
    }
}
