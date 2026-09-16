package com.odin.agent.trading

import com.odin.agent.models.QuantStrategyType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.random.Random

/**
 * ODIN - Continuous Backtest Engine
 * تمام استراتژی‌ها هر لحظه دائم در حال بک‌تست برای تحلیل قدرت
 * اگر بعد از 5 بک‌تست 10 دلار زیر 15 دلار → ممنوع
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
    val passed: Boolean, // >= $15 ?
    val timestamp: Long = System.currentTimeMillis()
)

data class StrategyPower(
    val strategy: QuantStrategyType,
    val results: List<BacktestResult> = emptyList(),
    val avgFinal: Double = 0.0,
    val minFinal: Double = 0.0,
    val maxFinal: Double = 0.0,
    val winrateTests: Double = 0.0, // % of tests that passed >=$15
    val isBanned: Boolean = false,
    val banReason: String = "",
    val powerScore: Double = 0.0, // 0-100
    val stability: Double = 0.0, // 0-100
    val lastUpdate: Long = System.currentTimeMillis()
)

data class ContinuousBacktestState(
    val strategies: Map<QuantStrategyType, StrategyPower> = emptyMap(),
    val isRunning: Boolean = false,
    val totalTests: Int = 0,
    val bannedCount: Int = 0,
    val validCount: Int = 0,
    val lastUpdate: Long = 0
)

class ContinuousBacktestEngine {

    private val _state = MutableStateFlow(ContinuousBacktestState())
    val state: StateFlow<ContinuousBacktestState> = _state

    private val random = Random(System.currentTimeMillis())
    private val minFinal = 15.0
    private val initialCapital = 10.0
    private val numTestsRequired = 5

    private val strategyParams = mapOf(
        QuantStrategyType.TV_80_PERCENT to Pair(70 to 85, 2.0 to 3.0),
        QuantStrategyType.LIT_LIQUIDITY_INVERSION to Pair(50 to 65, 2.0 to 2.8),
        QuantStrategyType.TREND_FOLLOWING to Pair(35 to 50, 1.5 to 2.5),
        QuantStrategyType.MEAN_REVERSION to Pair(45 to 60, 1.2 to 2.0),
        QuantStrategyType.MOMENTUM_BREAKOUT to Pair(30 to 45, 1.8 to 3.0),
        QuantStrategyType.PAIRS_TRADING to Pair(50 to 65, 1.0 to 1.5),
        QuantStrategyType.VOLATILITY_REGIME to Pair(40 to 55, 1.3 to 2.2)
    )

    fun startContinuous() {
        _state.value = _state.value.copy(isRunning = true)
    }

    fun stopContinuous() {
        _state.value = _state.value.copy(isRunning = false)
    }

    private fun runSingleBacktest(strategy: QuantStrategyType): BacktestResult {
        val params = strategyParams[strategy] ?: Pair(40 to 55, 1.5 to 2.0)
        val wrRange = params.first
        val rrRange = params.second

        val wr = random.nextDouble() * (wrRange.second - wrRange.first) + wrRange.first
        val rr = random.nextDouble() * (rrRange.second - rrRange.first) + rrRange.first

        // Simulate 20 trades with 1% risk
        var capital = initialCapital
        var wins = 0
        repeat(20) {
            val win = random.nextDouble() * 100 < wr
            val riskAmount = capital * 0.01
            if (win) {
                capital += riskAmount * rr
                wins++
            } else {
                capital -= riskAmount
            }
        }
        capital *= random.nextDouble() * 0.2 + 0.9 // noise 0.9-1.1

        val profit = capital - initialCapital
        val profitPercent = profit / initialCapital * 100
        val passed = capital >= minFinal

        return BacktestResult(
            strategy = strategy,
            testNumber = 0,
            initialCapital = initialCapital,
            finalCapital = maxOf(0.1, capital),
            profit = profit,
            profitPercent = profitPercent,
            winrate = wr,
            trades = 20,
            passed = passed
        )
    }

    fun runBacktestCycle(): Map<QuantStrategyType, StrategyPower> {
        val newPowers = mutableMapOf<QuantStrategyType, StrategyPower>()

        for (strategy in QuantStrategyType.values()) {
            val existing = _state.value.strategies[strategy]
            val existingResults = existing?.results ?: emptyList()

            // Run one new test
            val newResult = runSingleBacktest(strategy).copy(testNumber = existingResults.size + 1)
            val allResults = (existingResults + newResult).takeLast(numTestsRequired)

            val avgFinal = if (allResults.isNotEmpty()) allResults.map { it.finalCapital }.average() else 0.0
            val minFinalVal = if (allResults.isNotEmpty()) allResults.minOf { it.finalCapital } else 0.0
            val maxFinalVal = if (allResults.isNotEmpty()) allResults.maxOf { it.finalCapital } else 0.0
            val winrateTests = if (allResults.isNotEmpty()) allResults.count { it.passed }.toDouble() / allResults.size * 100 else 0.0

            // Ban logic: if after 5 tests, avg < $15 OR min < $15 OR winrate <60% → banned
            val isBanned = if (allResults.size >= numTestsRequired) {
                avgFinal < minFinal || minFinalVal < minFinal || winrateTests < 60
            } else {
                false // Not enough tests yet
            }

            val banReason = if (isBanned) {
                "Avg $${String.format("%.2f", avgFinal)} < $$minFinal or Min $${String.format("%.2f", minFinalVal)} < $$minFinal or WR ${String.format("%.0f", winrateTests)}% <60%"
            } else if (allResults.size < numTestsRequired) {
                "Testing ${allResults.size}/$numTestsRequired"
            } else {
                "Avg $${String.format("%.2f", avgFinal)} >= $$minFinal and WR ${String.format("%.0f", winrateTests)}% >=60%"
            }

            // Power score: based on avg final and winrate
            val powerScore = ((avgFinal / minFinal * 50).coerceAtMost(50.0) + (winrateTests * 0.5)).coerceAtMost(100.0)
            val stability = winrateTests // stability = % of tests passed

            val power = StrategyPower(
                strategy = strategy,
                results = allResults,
                avgFinal = avgFinal,
                minFinal = minFinalVal,
                maxFinal = maxFinalVal,
                winrateTests = winrateTests,
                isBanned = isBanned,
                banReason = banReason,
                powerScore = powerScore,
                stability = stability,
                lastUpdate = System.currentTimeMillis()
            )

            newPowers[strategy] = power
        }

        val bannedCount = newPowers.values.count { it.isBanned }
        val validCount = newPowers.values.count { !it.isBanned && it.results.size >= numTestsRequired }

        _state.value = ContinuousBacktestState(
            strategies = newPowers,
            isRunning = _state.value.isRunning,
            totalTests = _state.value.totalTests + QuantStrategyType.values().size,
            bannedCount = bannedCount,
            validCount = validCount,
            lastUpdate = System.currentTimeMillis()
        )

        return newPowers
    }

    fun getBannedStrategies(): List<StrategyPower> {
        return _state.value.strategies.values.filter { it.isBanned }
    }

    fun getValidStrategies(): List<StrategyPower> {
        return _state.value.strategies.values.filter { !it.isBanned && it.results.size >= numTestsRequired }
    }

    fun isStrategyBanned(strategy: QuantStrategyType): Boolean {
        return _state.value.strategies[strategy]?.isBanned ?: false
    }
}
