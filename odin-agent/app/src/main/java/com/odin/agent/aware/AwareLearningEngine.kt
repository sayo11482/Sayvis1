package com.odin.agent.aware

import com.odin.agent.models.QuantStrategyType
import com.odin.agent.models.SignalSide
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.random.Random

/**
 * ODIN AWARE - Precise Learning Engine
 * آنلاین دائم در حال یادگیری و بررسی و تست استراتژی‌ها و مدیریت مالی
 * 
 * Features:
 * - Online continuous learning
 * - Tests strategies and money management methods
 * - Learns from market, backtests, and live trades
 * - Adapts risk, RR, confluence thresholds
 * - Memory of best strategy per symbol
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
    val result: String, // win, loss, pending
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
    val awarenessLevel: Double = 0.0, // 0-100
    val lastLearningTime: Long = 0
)

class AwareLearningEngine {

    private val _state = MutableStateFlow(AwareState())
    val state: StateFlow<AwareState> = _state

    private val random = Random(System.currentTimeMillis())

    // Best LIT settings researched for max RR
    data class OptimalLITSettings(
        val htfBias: String = "Daily + 4H",
        val liquidityLookback: Int = 20,
        val sweepThreshold: Double = 0.002, // 0.2%
        val obEntryPercent: Double = 0.5, // 50% OB
        val fvgRequired: Boolean = true,
        val oteZone: Pair<Double, Double> = 0.62 to 0.79, // Fibonacci 62-79%
        val slBufferATR: Double = 0.2, // 0.2 ATR beyond sweep
        val tpMethod: String = "Next Liquidity Pool",
        val minRR: Double = 2.5, // Minimum 1:2.5 for LIT max RR
        val idealRR: Double = 3.5, // Ideal 1:3.5
        val maxRR: Double = 5.0, // Max 1:5 in strong trends
        val riskPerTrade: Double = 0.8, // 0.8% for LIT
        val maxActiveTrades: Int = 2,
        val maxTradesPerDay: Int = 3,
        val htfTimeframe: String = "4H",
        val ltfTimeframe: String = "15m",
        val entryTimeframe: String = "5m",
        val trailingStop: Boolean = true,
        val trailingBehindOB: Boolean = true
    )

    val optimalLIT = OptimalLITSettings()

    fun startLearning() {
        _state.value = _state.value.copy(isLearning = true)
    }

    fun stopLearning() {
        _state.value = _state.value.copy(isLearning = false)
    }

    /**
     * Add new experience from live trade or backtest
     */
    fun addExperience(experience: LearningExperience) {
        val current = _state.value.experiences
        val newExperiences = (current + experience).takeLast(500) // Keep last 500

        // Update strategy stats
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

        // Learn optimal risk via Kelly Criterion
        val kelly = if (winrate > 0 && avgRR > 0) {
            val w = winrate / 100.0
            val r = avgRR
            (w * (r + 1) - 1) / r // Kelly formula
        } else 0.0
        val optimalRisk = (kelly * 100).coerceIn(0.5, 2.0) // Cap between 0.5% and 2%

        // Generate lessons
        val lessons = mutableListOf<String>()
        if (winrate < 50) lessons.add("Winrate low ${winrate.toInt()}% - need higher confluence")
        if (avgRR < 2.0) lessons.add("RR low ${String.format("%.1f", avgRR)} - need better entry at 50% OB + FVG")
        if (avgConfluence < 5) lessons.add("Confluence low ${avgConfluence.toInt()} - need 5+ confirmations")
        if (experience.result == "loss" && experience.confluence < 6) lessons.add("Loss with low confluence ${experience.confluence} - avoid <6")
        if (experience.result == "win" && experience.rr >= 3.0) lessons.add("Win with high RR ${String.format("%.1f", experience.rr)} - LIT optimal at 50% OB")

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

        // Update best per symbol
        updateBestPerSymbol(experience, newStats, newExperiences)

        // Update money management learning
        updateMoneyManagement(newExperiences)

        // Update awareness level
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

        // Group by strategy for this symbol
        val strategyGroups = symbolExperiences.groupBy { it.strategy }

        var bestStrategy: QuantStrategyType? = null
        var bestScore = -1.0
        var bestReason = ""

        for ((strat, exps) in strategyGroups) {
            if (exps.size < 3) continue // Need at least 3 trades

            val wins = exps.count { it.result == "win" }
            val winrate = wins.toDouble() / exps.size * 100
            val avgRR = exps.map { it.rr }.average()
            val avgPnL = exps.map { it.pnl }.average()
            val score = winrate * 0.4 + avgRR * 10 + avgPnL * 5 // Weighted score

            if (score > bestScore) {
                bestScore = score
                bestStrategy = strat
                bestReason = "WR ${winrate.toInt()}% RR ${String.format("%.1f", avgRR)} PnL ${String.format("%.2f", avgPnL)} Score ${score.toInt()}"
            }
        }

        if (bestStrategy != null) {
            val bestExp = symbolExperiences.filter { it.strategy == bestStrategy }.maxByOrNull { it.pnl }
            val newBest = SymbolBestStrategy(
                symbol = symbol,
                bestStrategy = bestStrategy,
                bestRR = bestExp?.rr ?: stats.bestRR,
                bestConfluence = bestExp?.confluence ?: stats.bestConfluence,
                winrate = strategyGroups[bestStrategy]?.let { exps ->
                    exps.count { it.result == "win" }.toDouble() / exps.size * 100
                } ?: 0.0,
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
        val avgConfluence = experiences.map { it.confluence }.average()

        // Kelly Criterion for optimal risk
        val kelly = if (winrate > 0 && avgRR > 0) {
            val w = winrate
            val r = avgRR
            (w * (r + 1) - 1) / r
        } else 0.0

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

        // Awareness based on experience, winrate, and PnL
        val expScore = (expCount / 100.0 * 30).coerceAtMost(30.0)
        val winrateScore = (avgWinrate * 0.3).coerceAtMost(30.0)
        val pnlScore = if (totalPnL > 0) (totalPnL * 2).coerceAtMost(20.0) else 0.0
        val stratScore = (stratCount / 7.0 * 20).coerceAtMost(20.0)

        return (expScore + winrateScore + pnlScore + stratScore).coerceAtMost(100.0)
    }

    fun getBestStrategyForSymbol(symbol: String): SymbolBestStrategy? {
        return _state.value.bestPerSymbol[symbol]
    }

    fun getOptimalLITSettings(): OptimalLITSettings = optimalLIT

    fun generateMockExperience() {
        val symbols = listOf("BTC/USDT", "ETH/USDT", "EURUSD", "XAUUSD")
        val strategies = QuantStrategyType.values().toList()
        val symbol = symbols.random()
        val strategy = strategies.random()
        val isWin = random.nextDouble() < 0.6
        val rr = if (strategy == QuantStrategyType.LIT_LIQUIDITY_INVERSION) {
            random.nextDouble() * 2.0 + 2.0 // 2.0-4.0 for LIT
        } else {
            random.nextDouble() * 1.5 + 1.5
        }
        val confluence = random.nextInt(6) + 5 // 5-10
        val confidence = random.nextDouble() * 20 + 75

        val exp = LearningExperience(
            id = "exp_${System.currentTimeMillis()}",
            symbol = symbol,
            strategy = strategy,
            side = if (random.nextBoolean()) SignalSide.BUY else SignalSide.SELL,
            entryPrice = random.nextDouble() * 10000 + 60000,
            exitPrice = random.nextDouble() * 10000 + 60000,
            sl = random.nextDouble() * 1000 + 64000,
            tp = random.nextDouble() * 2000 + 66000,
            rr = rr,
            confluence = confluence,
            confidence = confidence,
            result = if (isWin) "win" else "loss",
            pnl = if (isWin) rr * 10 else -10.0,
            pnlPercent = if (isWin) rr else -1.0,
            marketRegime = listOf("trending", "ranging", "high_vol").random(),
            lessons = listOf("LIT 50% OB entry", "FVG required", "RR 1:3 optimal")
        )

        addExperience(exp)
    }
}
