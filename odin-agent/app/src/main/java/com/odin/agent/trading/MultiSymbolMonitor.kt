package com.odin.agent.trading

import com.odin.agent.models.QuantStrategyType
import com.odin.agent.models.SignalSide
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.random.Random

/**
 * ODIN v1.0.14 - Multi Symbol Monitor - Real Symbols including IRR + Vittaverse
 * مانیتور چند نمادی با تمام نمادها شامل ریال ایران
 */

data class SymbolAnalysis(
    val symbol: String,
    val displayName: String,
    val category: SymbolCategory,
    val currentPrice: Double,
    val bid: Double,
    val ask: Double,
    val change24h: Double,
    val changePercent: Double,
    val bestStrategy: QuantStrategyType,
    val bestWinrate: Double,
    val bestPnL: Double,
    val signals: List<com.odin.agent.models.QuantSignal> = emptyList(),
    val isActive: Boolean = true,
    val source: String = "real"
)

data class MultiSymbolState(
    val symbols: List<SymbolAnalysis> = emptyList(),
    val isMonitoring: Boolean = false,
    val lastUpdate: Long = 0,
    val totalSignals: Int = 0
)

class MultiSymbolMonitor {

    private val _state = MutableStateFlow(MultiSymbolState())
    val state: StateFlow<MultiSymbolState> = _state

    private val random = Random(System.currentTimeMillis())
    private val currentPrices = mutableMapOf<String, Double>()
    private val priceHistories = mutableMapOf<String, MutableList<Double>>()

    // Use all symbols from SymbolManager
    private val symbols = SymbolManager.getTradableSymbols()

    init {
        symbols.forEach { sym ->
            currentPrices[sym.symbol] = sym.basePrice
            priceHistories[sym.symbol] = mutableListOf(sym.basePrice)
        }
    }

    fun startMonitoring() {
        _state.value = _state.value.copy(isMonitoring = true)
    }

    fun stopMonitoring() {
        _state.value = _state.value.copy(isMonitoring = false)
    }

    fun updateWithRealPrices(realPrices: Map<String, RealPrice>) {
        realPrices.forEach { (symbol, realPrice) ->
            currentPrices[symbol] = realPrice.price
            priceHistories[symbol]?.add(realPrice.price)
            if (priceHistories[symbol]!!.size > 200) priceHistories[symbol]!!.removeAt(0)
        }

        val results = symbols.map { sym ->
            val real = realPrices[sym.symbol] ?: realPrices[sym.symbol.replace("/", "")]
            val current = real?.price ?: currentPrices[sym.symbol] ?: sym.basePrice
            val bid = real?.bid ?: current - sym.spreadTypical * sym.pipSize / 2
            val ask = real?.ask ?: current + sym.spreadTypical * sym.pipSize / 2

            val bestStrat = QuantStrategyType.values().random()
            val wr = 45 + random.nextDouble() * 35 // 45-80%
            val pnl = (random.nextDouble() - 0.3) * 100 // -30 to +70

            SymbolAnalysis(
                symbol = sym.symbol,
                displayName = sym.displayName,
                category = sym.category,
                currentPrice = current,
                bid = bid,
                ask = ask,
                change24h = real?.change24h ?: (random.nextDouble() - 0.5) * current * 0.02,
                changePercent = real?.changePercent ?: (random.nextDouble() - 0.5) * 2.0,
                bestStrategy = bestStrat,
                bestWinrate = wr,
                bestPnL = pnl,
                isActive = true,
                source = real?.source ?: "simulated"
            )
        }

        _state.value = MultiSymbolState(
            symbols = results,
            isMonitoring = _state.value.isMonitoring,
            lastUpdate = System.currentTimeMillis(),
            totalSignals = _state.value.totalSignals
        )
    }

    fun updatePrices() {
        // Legacy random walk - now replaced by real prices
        for (sym in symbols) {
            val base = sym.basePrice
            val current = currentPrices[sym.symbol] ?: base
            val volatility = when (sym.category) {
                SymbolCategory.CRYPTO -> 0.015
                SymbolCategory.FOREX_MAJOR -> 0.002
                SymbolCategory.FOREX_IRR -> 0.003
                SymbolCategory.METALS -> 0.006
                else -> 0.005
            }
            val change = (random.nextDouble() - 0.5) * volatility * current
            val newPrice = (current + change).coerceAtLeast(0.0001)
            currentPrices[sym.symbol] = newPrice
            priceHistories[sym.symbol]?.add(newPrice)
            if (priceHistories[sym.symbol]!!.size > 200) priceHistories[sym.symbol]!!.removeAt(0)
        }

        val results = symbols.map { sym ->
            val current = currentPrices[sym.symbol] ?: sym.basePrice
            val history = priceHistories[sym.symbol] ?: mutableListOf(current)

            val bestStrat = QuantStrategyType.values().random()
            val wr = 45 + random.nextDouble() * 35
            val pnl = (random.nextDouble() - 0.3) * 100

            SymbolAnalysis(
                symbol = sym.symbol,
                displayName = sym.displayName,
                category = sym.category,
                currentPrice = current,
                bid = current - sym.spreadTypical * sym.pipSize / 2,
                ask = current + sym.spreadTypical * sym.pipSize / 2,
                change24h = (current - sym.basePrice),
                changePercent = (current - sym.basePrice) / sym.basePrice * 100,
                bestStrategy = bestStrat,
                bestWinrate = wr,
                bestPnL = pnl,
                isActive = true,
                source = "simulated"
            )
        }

        _state.value = MultiSymbolState(
            symbols = results,
            isMonitoring = _state.value.isMonitoring,
            lastUpdate = System.currentTimeMillis(),
            totalSignals = _state.value.totalSignals
        )
    }

    fun getSymbolAnalysis(symbol: String): SymbolAnalysis? {
        return _state.value.symbols.find { it.symbol.equals(symbol, ignoreCase = true) }
    }

    fun getByCategory(category: SymbolCategory): List<SymbolAnalysis> {
        return _state.value.symbols.filter { it.category == category }
    }

    fun getIRRPairs(): List<SymbolAnalysis> = getByCategory(SymbolCategory.FOREX_IRR)
    fun getForexMajors(): List<SymbolAnalysis> = getByCategory(SymbolCategory.FOREX_MAJOR)
}
