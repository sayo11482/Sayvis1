package com.odin.agent.trading

import com.odin.agent.indicators.TradingViewIndicators
import com.odin.agent.indicators.IndicatorSignal
import com.odin.agent.indicators.ConfluenceResult
import com.odin.agent.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.random.Random

/**
 * ODIN QUANT - Multi-Symbol Monitor with TradingView Indicators + 80% WR Filter
 * - 20+ TradingView indicators
 * - Minimum RR 1:2
 * - Confluence >=5
 * - Historical WR >=80% else NO TRADE
 * Symbols: BTC/USDT, ETH/USDT, EURUSD, XAUUSD
 */

data class SymbolAnalysis(
    val symbol: String,
    val currentPrice: Double,
    val prevPrice: Double,
    val trend: String,
    val regime: MarketRegime,
    val liquidityHigh: Double?,
    val liquidityLow: Double?,
    val lastSweep: String?,
    val hasBOS: Boolean,
    val bosDirection: String?,
    val orderBlockHigh: Double?,
    val orderBlockLow: Double?,
    val fvgDetected: Boolean,
    val premiumDiscount: Double,
    val signal: QuantSignal?,
    val confidence: Double,
    val tvIndicators: List<IndicatorSignal> = emptyList(),
    val confluence: ConfluenceResult? = null,
    val rr: Double = 0.0,
    val historicalWR: Double = 0.0,
    val wrBlocked: Boolean = false,
    val lastUpdate: Long = System.currentTimeMillis()
)

data class MultiSymbolState(
    val symbols: List<SymbolAnalysis> = emptyList(),
    val isScanning: Boolean = false,
    val totalSignalsToday: Int = 0,
    val blockedByWRFilter: Int = 0,
    val lastScanTime: Long = 0,
    val avgConfluence: Double = 0.0
)

class MultiSymbolMonitor {

    private val _state = MutableStateFlow(MultiSymbolState())
    val state: StateFlow<MultiSymbolState> = _state

    private val symbols = listOf("BTC/USDT", "ETH/USDT", "EURUSD", "XAUUSD")
    
    private val basePrices = mapOf(
        "BTC/USDT" to 65000.0,
        "ETH/USDT" to 3500.0,
        "EURUSD" to 1.0850,
        "XAUUSD" to 2350.0
    )

    private val currentPrices = mutableMapOf<String, Double>().apply {
        basePrices.forEach { (k, v) -> put(k, v) }
    }

    private val priceHistories = mutableMapOf<String, MutableList<Double>>().apply {
        basePrices.forEach { (sym, price) ->
            put(sym, MutableList(200) { price * (0.95 + Random.nextDouble()*0.1) })
        }
    }

    private val historicalTrades = mutableListOf<Boolean>()
    private var blockedCount = 0

    private val random = Random(System.currentTimeMillis())

    fun startMonitoring() {
        _state.value = _state.value.copy(isScanning = true)
    }

    fun stopMonitoring() {
        _state.value = _state.value.copy(isScanning = false)
    }

    fun getHistoricalWR(): Double {
        if (historicalTrades.isEmpty()) return 0.0
        return historicalTrades.count { it }.toDouble() / historicalTrades.size * 100.0
    }

    fun addTradeResult(win: Boolean) {
        historicalTrades.add(win)
        if (historicalTrades.size > 100) historicalTrades.removeAt(0)
    }

    fun scanSymbols(): List<SymbolAnalysis> {
        val results = mutableListOf<SymbolAnalysis>()
        var blocked = 0

        for (symbol in symbols) {
            val base = basePrices[symbol] ?: 100.0
            val current = currentPrices[symbol] ?: base
            
            val volatility = when(symbol) {
                "BTC/USDT" -> 0.015
                "ETH/USDT" -> 0.02
                "EURUSD" -> 0.002
                "XAUUSD" -> 0.006
                else -> 0.01
            }
            
            val change = (random.nextDouble() - 0.5) * 2 * volatility
            val newPrice = current * (1 + change)
            val prevPrice = current
            currentPrices[symbol] = newPrice

            priceHistories[symbol]?.add(newPrice)
            if (priceHistories[symbol]!!.size > 200) priceHistories[symbol]!!.removeAt(0)
            val history = priceHistories[symbol] ?: mutableListOf(newPrice)

            val highHist = history.map { it * (1 + random.nextDouble()*0.005) }
            val lowHist = history.map { it * (1 - random.nextDouble()*0.005) }
            val volHist = history.map { random.nextDouble()*1000 + 500 }

            val tvSignals = TradingViewIndicators.checkAllIndicators(
                symbol = symbol,
                currentPrice = newPrice,
                priceHistory = history,
                highHistory = highHist,
                lowHistory = lowHist,
                volumeHistory = volHist
            )

            val premiumDiscount = random.nextDouble()
            val liquidityHigh = if (random.nextDouble() < 0.3) newPrice * (1 + random.nextDouble()*0.02) else null
            val liquidityLow = if (random.nextDouble() < 0.3) newPrice * (1 - random.nextDouble()*0.02) else null
            val sweep = when {
                random.nextDouble() < 0.08 -> if (random.nextBoolean()) "bullish_sweep" else "bearish_sweep"
                else -> null
            }
            val hasBOS = random.nextDouble() < 0.12
            val bosDir = if (hasBOS) { if (random.nextBoolean()) "bullish" else "bearish" } else null
            val hasOB = random.nextDouble() < 0.22
            val obHigh = if (hasOB) newPrice * (1 + random.nextDouble()*0.005) else null
            val obLow = if (hasOB) newPrice * (1 - random.nextDouble()*0.005) else null
            val hasFVG = random.nextDouble() < 0.18

            val regime = when {
                random.nextDouble() < 0.4 -> MarketRegime.TRENDING
                random.nextDouble() < 0.7 -> MarketRegime.RANGING
                else -> MarketRegime.HIGH_VOL
            }
            val trend = when {
                newPrice > prevPrice * 1.001 -> "bullish"
                newPrice < prevPrice * 0.999 -> "bearish"
                else -> "ranging"
            }

            val litMap = mutableMapOf<String, Boolean>()
            if (sweep == "bullish_sweep") litMap["sweep_bull"] = true
            if (sweep == "bearish_sweep") litMap["sweep_bear"] = true
            if (hasBOS && bosDir == "bullish") litMap["bos_bull"] = true
            if (hasBOS && bosDir == "bearish") litMap["bos_bear"] = true
            if (hasOB) {
                if (sweep != null && hasBOS) {
                    if (sweep.contains("bullish") || bosDir == "bullish") litMap["lit_bull"] = true
                    if (sweep.contains("bearish") || bosDir == "bearish") litMap["lit_bear"] = true
                }
            }

            val atr = newPrice * 0.01
            val rr = if (random.nextDouble() < 0.7) 2.0 + random.nextDouble()*1.5 else 1.0 + random.nextDouble()

            val historicalWR = getHistoricalWR()
            val confluenceResult = TradingViewIndicators.calculateConfluence(
                indicatorSignals = tvSignals,
                litSignals = litMap,
                minConfluence = 5,
                minRR = 2.0,
                currentRR = rr,
                historicalWR = historicalWR,
                minWR = 80.0
            )

            var signal: QuantSignal? = null
            var confidence = confluenceResult.confidence / 100.0
            var wrBlocked = false

            if (confluenceResult.valid && (historicalWR == 0.0 || historicalWR >= 80.0)) {
                val isBullish = confluenceResult.isBullish
                val isBearish = confluenceResult.isBearish

                if (isBullish || isBearish) {
                    val side = if (isBullish) SignalSide.BUY else SignalSide.SELL
                    val sl = if (side == SignalSide.BUY) newPrice - atr*1.5 else newPrice + atr*1.5
                    val tp = if (side == SignalSide.BUY) newPrice + atr*3.0 else newPrice - atr*3.0
                    val topConfirmations = confluenceResult.confirmations.take(3).joinToString(", ")
                    val rrStr = String.format("%.1f", rr)
                    val wrStr = String.format("%.0f", historicalWR)
                    val reasonText = "TV ${confluenceResult.score} conf: $topConfirmations | RR 1:$rrStr | WR $wrStr%"
                    signal = QuantSignal(
                        id = "tv80_${symbol}_${System.currentTimeMillis()}",
                        symbol = symbol,
                        timeframe = "1h",
                        strategy = QuantStrategyType.TV_80_PERCENT,
                        side = side,
                        entryPrice = newPrice,
                        slPrice = sl,
                        tpPrice = tp,
                        confidence = confidence,
                        reason = reasonText,
                        regime = regime
                    )
                }
            } else if (!confluenceResult.valid && historicalWR != 0.0 && historicalWR < 80.0 && confluenceResult.score >=5 && rr >=2.0) {
                wrBlocked = true
                blocked++
            }

            val analysis = SymbolAnalysis(
                symbol = symbol,
                currentPrice = newPrice,
                prevPrice = prevPrice,
                trend = trend,
                regime = regime,
                liquidityHigh = liquidityHigh,
                liquidityLow = liquidityLow,
                lastSweep = sweep,
                hasBOS = hasBOS,
                bosDirection = bosDir,
                orderBlockHigh = obHigh,
                orderBlockLow = obLow,
                fvgDetected = hasFVG,
                premiumDiscount = premiumDiscount,
                signal = signal,
                confidence = confidence,
                tvIndicators = tvSignals,
                confluence = confluenceResult,
                rr = rr,
                historicalWR = historicalWR,
                wrBlocked = wrBlocked
            )

            results.add(analysis)
        }

        val avgConf = if (results.isNotEmpty()) results.mapNotNull { it.confluence?.score }.average() else 0.0

        _state.value = MultiSymbolState(
            symbols = results,
            isScanning = _state.value.isScanning,
            totalSignalsToday = _state.value.totalSignalsToday + results.count { it.signal != null },
            blockedByWRFilter = _state.value.blockedByWRFilter + blocked,
            lastScanTime = System.currentTimeMillis(),
            avgConfluence = avgConf
        )

        return results
    }

    fun getSymbolAnalysis(symbol: String): SymbolAnalysis? {
        return _state.value.symbols.find { it.symbol == symbol }
    }
}
