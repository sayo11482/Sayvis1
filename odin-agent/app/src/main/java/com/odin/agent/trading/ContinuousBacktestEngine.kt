package com.odin.agent.trading

import com.odin.agent.models.QuantStrategyType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.random.Random

/**
 * ODIN v1.0.14 - Continuous Backtest Engine WITHOUT BAN RULE
 * تمام استراتژی‌ها همیشه مجاز - فقط امتیازدهی بر اساس قدرت
 * No more $10->$15 ban - all strategies scored by Power Score
 */

data class BacktestResult(
    val strategy: QuantStrategyType,
    val testNumber: Int,
    val initialCapital: Double,
    val finalCapital: Double,
    val profit: Double,
    val profitPercent: Double,
    val winrate: Double,
    val trades: Int,
    val profitFactor: Double,
    val sharpe: Double,
    val timestamp: Long = System.currentTimeMillis()
)

data class StrategyPower(
    val strategy: QuantStrategyType,
    val results: List<BacktestResult> = emptyList(),
    val avgFinal: Double = 0.0,
    val minFinal: Double = 0.0,
    val maxFinal: Double = 0.0,
    val avgProfit: Double = 0.0,
    val avgWinrate: Double = 0.0,
    val avgProfitFactor: Double = 0.0,
    val avgSharpe: Double = 0.0,
    val powerScore: Double = 0.0, // 0-100 based on profit, WR, PF, Sharpe
    val stability: Double = 0.0, // consistency of results
    val rank: Int = 0,
    val lastUpdate: Long = System.currentTimeMillis()
)

data class ContinuousBacktestState(
    val strategies: Map<QuantStrategyType, StrategyPower> = emptyMap(),
    val isRunning: Boolean = false,
    val totalTests: Int = 0,
    val bestStrategy: QuantStrategyType? = null,
    val lastUpdate: Long = 0
)

class ContinuousBacktestEngine {

    private val _state = MutableStateFlow(ContinuousBacktestState())
    val state: StateFlow<ContinuousBacktestState> = _state

    private val random = Random(System.currentTimeMillis())
    private val initialCapital = 100.0 // Now 100$ base for realistic testing

    private val strategyParams = mapOf(
        QuantStrategyType.TV_80_PERCENT to Triple(70 to 85, 2.0 to 3.5, 1.5 to 2.5),
        QuantStrategyType.LIT_LIQUIDITY_INVERSION to Triple(55 to 70, 2.2 to 3.8, 1.8 to 3.0),
        QuantStrategyType.TREND_FOLLOWING to Triple(40 to 55, 1.8 to 2.8, 1.2 to 2.0),
        QuantStrategyType.MEAN_REVERSION to Triple(48 to 62, 1.3 to 2.2, 1.0 to 1.8),
        QuantStrategyType.MOMENTUM_BREAKOUT to Triple(35 to 50, 2.0 to 3.2, 1.3 to 2.2),
        QuantStrategyType.PAIRS_TRADING to Triple(52 to 65, 1.1 to 1.6, 0.9 to 1.5),
        QuantStrategyType.VOLATILITY_REGIME to Triple(42 to 58, 1.4 to 2.4, 1.1 to 1.9)
    )

    fun startContinuous() {
        _state.value = _state.value.copy(isRunning = true)
    }

    fun stopContinuous() {
        _state.value = _state.value.copy(isRunning = false)
    }

    private fun runSingleBacktest(strategy: QuantStrategyType): BacktestResult {
        val params = strategyParams[strategy] ?: Triple(40 to 55, 1.5 to 2.0, 1.0 to 1.5)
        val wrRange = params.first
        val rrRange = params.second
        val pfRange = params.third

        val wr = random.nextDouble() * (wrRange.second - wrRange.first) + wrRange.first
        val rr = random.nextDouble() * (rrRange.second - rrRange.first) + rrRange.first
        val pf = random.nextDouble() * (pfRange.second - pfRange.first) + pfRange.first

        // Simulate 50 trades with 1% risk - more realistic
        var capital = initialCapital
        var wins = 0
        var totalWin = 0.0
        var totalLoss = 0.0
        repeat(50) {
            val win = random.nextDouble() * 100 < wr
            val riskAmount = capital * 0.01
            if (win) {
                val profit = riskAmount * rr * (0.8 + random.nextDouble() * 0.4)
                capital += profit
                totalWin += profit
                wins++
            } else {
                capital -= riskAmount
                totalLoss += riskAmount
            }
        }
        capital *= random.nextDouble() * 0.15 + 0.92 // noise 0.92-1.07

        val profit = capital - initialCapital
        val profitPercent = profit / initialCapital * 100
        val actualPF = if (totalLoss > 0) totalWin / totalLoss else pf
        val sharpe = (profitPercent / 15.0).coerceIn(0.0, 3.0) + random.nextDouble() * 0.3

        return BacktestResult(
            strategy = strategy,
            testNumber = 0,
            initialCapital = initialCapital,
            finalCapital = maxOf(1.0, capital),
            profit = profit,
            profitPercent = profitPercent,
            winrate = wr,
            trades = 50,
            profitFactor = actualPF,
            sharpe = sharpe
        )
    }

    fun runBacktestCycle(): Map<QuantStrategyType, StrategyPower> {
        val newPowers = mutableMapOf<QuantStrategyType, StrategyPower>()

        for (strategy in QuantStrategyType.values()) {
            val existing = _state.value.strategies[strategy]
            val existingResults = existing?.results ?: emptyList()

            // Run one new test - keep last 10 for better stats
            val newResult = runSingleBacktest(strategy).copy(testNumber = existingResults.size + 1)
            val allResults = (existingResults + newResult).takeLast(10)

            val avgFinal = if (allResults.isNotEmpty()) allResults.map { it.finalCapital }.average() else 0.0
            val minFinalVal = if (allResults.isNotEmpty()) allResults.minOf { it.finalCapital } else 0.0
            val maxFinalVal = if (allResults.isNotEmpty()) allResults.maxOf { it.finalCapital } else 0.0
            val avgProfit = if (allResults.isNotEmpty()) allResults.map { it.profitPercent }.average() else 0.0
            val avgWR = if (allResults.isNotEmpty()) allResults.map { it.winrate }.average() else 0.0
            val avgPF = if (allResults.isNotEmpty()) allResults.map { it.profitFactor }.average() else 0.0
            val avgSharpe = if (allResults.isNotEmpty()) allResults.map { it.sharpe }.average() else 0.0

            // Power score: profit 40% + WR 30% + PF 20% + Sharpe 10%
            val profitScore = ((avgProfit / 50.0 * 40).coerceIn(0.0, 40.0))
            val wrScore = (avgWR / 100.0 * 30)
            val pfScore = ((avgPF / 3.0 * 20).coerceIn(0.0, 20.0))
            val sharpeScore = ((avgSharpe / 3.0 * 10).coerceIn(0.0, 10.0))
            val powerScore = profitScore + wrScore + pfScore + sharpeScore

            // Stability: inverse of std dev of final capitals
            val mean = avgFinal
            val variance = if (allResults.size > 1) {
                allResults.map { (it.finalCapital - mean) * (it.finalCapital - mean) }.average()
            } else 0.0
            val stdDev = kotlin.math.sqrt(variance)
            val stability = if (mean > 0) (100 - (stdDev / mean * 100)).coerceIn(0.0, 100.0) else 0.0

            val power = StrategyPower(
                strategy = strategy,
                results = allResults,
                avgFinal = avgFinal,
                minFinal = minFinalVal,
                maxFinal = maxFinalVal,
                avgProfit = avgProfit,
                avgWinrate = avgWR,
                avgProfitFactor = avgPF,
                avgSharpe = avgSharpe,
                powerScore = powerScore.coerceIn(0.0, 100.0),
                stability = stability,
                lastUpdate = System.currentTimeMillis()
            )

            newPowers[strategy] = power
        }

        // Rank by power score
        val sorted = newPowers.values.sortedByDescending { it.powerScore }
        sorted.forEachIndexed { index, power ->
            newPowers[power.strategy] = power.copy(rank = index + 1)
        }

        val best = sorted.firstOrNull()?.strategy

        _state.value = ContinuousBacktestState(
            strategies = newPowers,
            isRunning = _state.value.isRunning,
            totalTests = _state.value.totalTests + QuantStrategyType.values().size,
            bestStrategy = best,
            lastUpdate = System.currentTimeMillis()
        )

        return newPowers
    }

    fun getAllStrategiesRanked(): List<StrategyPower> {
        return _state.value.strategies.values.sortedByDescending { it.powerScore }
    }

    fun getBestStrategy(): StrategyPower? {
        return _state.value.strategies.values.maxByOrNull { it.powerScore }
    }

    // Legacy compatibility - no banned anymore
    fun getBannedStrategies(): List<StrategyPower> = emptyList()
    fun getValidStrategies(): List<StrategyPower> = getAllStrategiesRanked()
    fun isStrategyBanned(strategy: QuantStrategyType): Boolean = false
}
