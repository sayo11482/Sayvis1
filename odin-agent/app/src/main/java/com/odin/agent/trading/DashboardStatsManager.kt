package com.odin.agent.trading

import com.odin.agent.aware.AwareLearningEngine
import com.odin.agent.models.QuantStrategyType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.random.Random

/**
 * ODIN - Dashboard Stats Manager
 * 1. فعال‌ترین و موفق‌ترین استراتژی
 * 2. تعداد معاملات امروز
 * 3. سود/زیان شناور از اول سشن
 * 4. بیشترین سود از کدام نماد
 * 5. AWARE در حال یادگیری کدام استراتژی
 */

data class MostActiveStrategy(
    val strategy: QuantStrategyType,
    val trades: Int,
    val winrate: Double,
    val avgRR: Double,
    val totalPnL: Double,
    val powerScore: Double,
    val reason: String
)

data class MostProfitableSymbol(
    val symbol: String,
    val totalPnL: Double,
    val trades: Int,
    val winrate: Double,
    val bestTrade: Double,
    val strategy: QuantStrategyType
)

data class FloatingPnL(
    val sessionStartCapital: Double,
    val currentCapital: Double,
    val floatingPnL: Double,
    val floatingPnLPercent: Double,
    val openPositions: Int,
    val openPositionsPnL: Double,
    val closedTodayPnL: Double,
    val sessionStartTime: Long
)

data class DashboardStats(
    val mostActiveStrategy: MostActiveStrategy? = null,
    val mostSuccessfulStrategy: MostActiveStrategy? = null,
    val tradesToday: Int = 0,
    val floatingPnL: FloatingPnL = FloatingPnL(10000.0, 10000.0, 0.0, 0.0, 0, 0.0, 0.0, System.currentTimeMillis()),
    val mostProfitableSymbol: MostProfitableSymbol? = null,
    val awareLearningStrategy: QuantStrategyType? = null,
    val awareProgress: Double = 0.0,
    val lastUpdate: Long = 0
)

class DashboardStatsManager {

    private val _state = MutableStateFlow(DashboardStats())
    val state: StateFlow<DashboardStats> = _state

    private val random = Random(System.currentTimeMillis())
    private var sessionStartCapital = 10000.0
    private var sessionStartTime = System.currentTimeMillis()
    private var tradesTodayCount = 0
    private var floatingPnLValue = 0.0

    // Mock data for symbols
    private val symbolPnL = mutableMapOf(
        "BTC/USDT" to 0.0,
        "ETH/USDT" to 0.0,
        "EURUSD" to 0.0,
        "XAUUSD" to 0.0
    )

    private val symbolTrades = mutableMapOf(
        "BTC/USDT" to 0,
        "ETH/USDT" to 0,
        "EURUSD" to 0,
        "XAUUSD" to 0
    )

    fun startNewSession() {
        sessionStartCapital = 10000.0 + random.nextDouble() * 500 - 250
        sessionStartTime = System.currentTimeMillis()
        tradesTodayCount = 0
        floatingPnLValue = 0.0
        symbolPnL.forEach { (k, _) -> symbolPnL[k] = 0.0 }
        symbolTrades.forEach { (k, _) -> symbolTrades[k] = 0 }
    }

    fun updateFromAware(awareEngine: AwareLearningEngine) {
        val awareState = awareEngine.state.value
        val strategyStats = awareState.strategyStats

        // Most active: most trades
        val mostActive = strategyStats.values.maxByOrNull { it.totalTrades }?.let { stats ->
            MostActiveStrategy(
                strategy = stats.strategy,
                trades = stats.totalTrades,
                winrate = stats.winrate,
                avgRR = stats.avgRR,
                totalPnL = stats.totalPnL,
                powerScore = stats.winrate * 0.5 + stats.avgRR * 10,
                reason = "Most trades: ${stats.totalTrades} - Active"
            )
        }

        // Most successful: highest winrate + profit factor + PnL
        val mostSuccessful = strategyStats.values.maxByOrNull { it.winrate * 0.4 + it.profitFactor * 10 + it.totalPnL * 0.1 }?.let { stats ->
            MostActiveStrategy(
                strategy = stats.strategy,
                trades = stats.totalTrades,
                winrate = stats.winrate,
                avgRR = stats.avgRR,
                totalPnL = stats.totalPnL,
                powerScore = stats.winrate * 0.5 + stats.profitFactor * 10,
                reason = "Best WR ${stats.winrate.toInt()}% PF ${String.format("%.2f", stats.profitFactor)} PnL ${String.format("%.1f", stats.totalPnL)}"
            )
        }

        // Most profitable symbol
        val mostProfitable = symbolPnL.maxByOrNull { it.value }?.let { (symbol, pnl) ->
            val trades = symbolTrades[symbol] ?: 0
            val bestStrat = awareState.bestPerSymbol[symbol]?.bestStrategy ?: QuantStrategyType.LIT_LIQUIDITY_INVERSION
            MostProfitableSymbol(
                symbol = symbol,
                totalPnL = pnl,
                trades = trades,
                winrate = random.nextDouble() * 30 + 50,
                bestTrade = pnl * 0.4,
                strategy = bestStrat
            )
        }

        // AWARE learning which strategy - the one with most recent update or lowest trades (needs learning)
        val awareLearning = strategyStats.values.minByOrNull { it.totalTrades }?.strategy
            ?: strategyStats.values.maxByOrNull { it.lastUpdate }?.strategy
            ?: QuantStrategyType.TV_80_PERCENT

        val awareProgress = awareState.awarenessLevel

        // Update floating PnL - simulate
        floatingPnLValue += random.nextDouble() * 20 - 8 // -8 to +12
        tradesTodayCount += if (random.nextDouble() < 0.1) 1 else 0

        // Update symbol PnL randomly
        val randomSymbol = symbolPnL.keys.random()
        symbolPnL[randomSymbol] = (symbolPnL[randomSymbol] ?: 0.0) + random.nextDouble() * 10 - 3
        symbolTrades[randomSymbol] = (symbolTrades[randomSymbol] ?: 0) + if (random.nextDouble() < 0.2) 1 else 0

        val currentCapital = sessionStartCapital + floatingPnLValue
        val floating = FloatingPnL(
            sessionStartCapital = sessionStartCapital,
            currentCapital = currentCapital,
            floatingPnL = floatingPnLValue,
            floatingPnLPercent = floatingPnLValue / sessionStartCapital * 100,
            openPositions = random.nextInt(0, 3),
            openPositionsPnL = random.nextDouble() * 20 - 5,
            closedTodayPnL = floatingPnLValue - (random.nextDouble() * 20 - 5),
            sessionStartTime = sessionStartTime
        )

        _state.value = DashboardStats(
            mostActiveStrategy = mostActive,
            mostSuccessfulStrategy = mostSuccessful,
            tradesToday = tradesTodayCount,
            floatingPnL = floating,
            mostProfitableSymbol = mostProfitable,
            awareLearningStrategy = awareLearning,
            awareProgress = awareProgress,
            lastUpdate = System.currentTimeMillis()
        )
    }

    fun addTradeResult(symbol: String, pnl: Double, strategy: QuantStrategyType) {
        symbolPnL[symbol] = (symbolPnL[symbol] ?: 0.0) + pnl
        symbolTrades[symbol] = (symbolTrades[symbol] ?: 0) + 1
        tradesTodayCount++
        floatingPnLValue += pnl
    }
}
