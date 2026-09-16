package com.odin.agent.trading

import com.odin.agent.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.random.Random

/**
 * ODIN QUANT - Multi-Symbol Monitor
 * Continuously monitors 4 symbols for LIT entry/exit
 * 
 * Symbols: BTC/USDT, ETH/USDT, EURUSD, XAUUSD
 * Each symbol analyzed for:
 * - Liquidity pools (equal highs/lows)
 * - Liquidity sweep + rejection
 * - BOS (Break of Structure)
 * - Order Block retest
 * - FVG mitigation
 * - Premium/Discount zone
 */

data class SymbolAnalysis(
    val symbol: String,
    val currentPrice: Double,
    val prevPrice: Double,
    val trend: String, // bullish, bearish, ranging
    val regime: MarketRegime,
    val liquidityHigh: Double?,
    val liquidityLow: Double?,
    val lastSweep: String?, // bullish_sweep, bearish_sweep, none
    val hasBOS: Boolean,
    val bosDirection: String?, // bullish, bearish
    val orderBlockHigh: Double?,
    val orderBlockLow: Double?,
    val fvgDetected: Boolean,
    val premiumDiscount: Double, // 0=discount, 1=premium
    val signal: QuantSignal?,
    val confidence: Double,
    val lastUpdate: Long = System.currentTimeMillis()
)

data class MultiSymbolState(
    val symbols: List<SymbolAnalysis> = emptyList(),
    val isScanning: Boolean = false,
    val totalSignalsToday: Int = 0,
    val lastScanTime: Long = 0
)

class MultiSymbolMonitor {

    private val _state = MutableStateFlow(MultiSymbolState())
    val state: StateFlow<MultiSymbolState> = _state

    private val symbols = listOf("BTC/USDT", "ETH/USDT", "EURUSD", "XAUUSD")
    
    // Base prices
    private val basePrices = mapOf(
        "BTC/USDT" to 65000.0,
        "ETH/USDT" to 3500.0,
        "EURUSD" to 1.0850,
        "XAUUSD" to 2350.0
    )

    private val currentPrices = mutableMapOf<String, Double>().apply {
        basePrices.forEach { (k, v) -> put(k, v) }
    }

    private val random = Random(System.currentTimeMillis())

    fun startMonitoring() {
        _state.value = _state.value.copy(isScanning = true)
    }

    fun stopMonitoring() {
        _state.value = _state.value.copy(isScanning = false)
    }

    /**
     * Simulate scanning all 4 symbols for LIT setups
     * In real app, this would call Python backend or MT5 bridge
     */
    fun scanSymbols(): List<SymbolAnalysis> {
        val results = mutableListOf<SymbolAnalysis>()

        for (symbol in symbols) {
            val base = basePrices[symbol] ?: 100.0
            val current = currentPrices[symbol] ?: base
            
            // Simulate price movement with some volatility
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

            // Simulate LIT analysis
            val premiumDiscount = random.nextDouble() // 0=discount, 1=premium
            
            // Randomly generate liquidity levels
            val liquidityHigh = if (random.nextDouble() < 0.3) newPrice * (1 + random.nextDouble()*0.02) else null
            val liquidityLow = if (random.nextDouble() < 0.3) newPrice * (1 - random.nextDouble()*0.02) else null

            // Sweep detection (5% chance)
            val sweep = when {
                random.nextDouble() < 0.05 -> if (random.nextBoolean()) "bullish_sweep" else "bearish_sweep"
                else -> null
            }

            // BOS detection (10% chance)
            val hasBOS = random.nextDouble() < 0.10
            val bosDir = if (hasBOS) {
                if (random.nextBoolean()) "bullish" else "bearish"
            } else null

            // Order Block (20% chance)
            val hasOB = random.nextDouble() < 0.20
            val obHigh = if (hasOB) newPrice * (1 + random.nextDouble()*0.005) else null
            val obLow = if (hasOB) newPrice * (1 - random.nextDouble()*0.005) else null

            // FVG (15% chance)
            val hasFVG = random.nextDouble() < 0.15

            // Regime
            val regime = when {
                random.nextDouble() < 0.4 -> MarketRegime.TRENDING
                random.nextDouble() < 0.7 -> MarketRegime.RANGING
                else -> MarketRegime.HIGH_VOL
            }

            // Trend
            val trend = when {
                newPrice > prevPrice * 1.001 -> "bullish"
                newPrice < prevPrice * 0.999 -> "bearish"
                else -> "ranging"
            }

            // Generate signal if conditions met (LIT: sweep + BOS + OB + discount/premium)
            var signal: QuantSignal? = null
            var confidence = 0.0

            // LIT logic: sweep + BOS + OB + premium/discount = high confidence
            if (sweep != null && hasBOS && hasOB) {
                val isBullish = sweep == "bullish_sweep" && bosDir == "bullish" && premiumDiscount < 0.5
                val isBearish = sweep == "bearish_sweep" && bosDir == "bearish" && premiumDiscount > 0.5
                
                if (isBullish || isBearish) {
                    val side = if (isBullish) SignalSide.BUY else SignalSide.SELL
                    val atr = newPrice * 0.01
                    val sl = if (side == SignalSide.BUY) newPrice - atr*1.5 else newPrice + atr*1.5
                    val tp = if (side == SignalSide.BUY) newPrice + atr*3.0 else newPrice - atr*3.0
                    
                    confidence = 0.75 + random.nextDouble()*0.15 // 75-90% for LIT
                    
                    signal = QuantSignal(
                        id = "lit_${symbol}_${System.currentTimeMillis()}",
                        symbol = symbol,
                        timeframe = "1h",
                        strategy = QuantStrategyType.LIT_LIQUIDITY_INVERSION,
                        side = side,
                        entryPrice = newPrice,
                        slPrice = sl,
                        tpPrice = tp,
                        confidence = confidence,
                        reason = "LIT: ${sweep} + ${bosDir} BOS + OB retest in ${if (isBullish) "discount" else "premium"} zone",
                        regime = regime
                    )
                }
            }
            // Also weaker signals: BOS + OB without sweep (lower confidence)
            else if (hasBOS && hasOB && random.nextDouble() < 0.3) {
                val isBullish = bosDir == "bullish" && premiumDiscount < 0.5
                val isBearish = bosDir == "bearish" && premiumDiscount > 0.5
                
                if (isBullish || isBearish) {
                    val side = if (isBullish) SignalSide.BUY else SignalSide.SELL
                    val atr = newPrice * 0.01
                    val sl = if (side == SignalSide.BUY) newPrice - atr*2.0 else newPrice + atr*2.0
                    val tp = if (side == SignalSide.BUY) newPrice + atr*3.0 else newPrice - atr*3.0
                    
                    confidence = 0.55 + random.nextDouble()*0.15
                    
                    signal = QuantSignal(
                        id = "lit_${symbol}_${System.currentTimeMillis()}",
                        symbol = symbol,
                        timeframe = "1h",
                        strategy = QuantStrategyType.LIT_LIQUIDITY_INVERSION,
                        side = side,
                        entryPrice = newPrice,
                        slPrice = sl,
                        tpPrice = tp,
                        confidence = confidence,
                        reason = "LIT (no sweep): ${bosDir} BOS + OB in ${if (isBullish) "discount" else "premium"}",
                        regime = regime
                    )
                }
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
                confidence = confidence
            )

            results.add(analysis)
        }

        _state.value = MultiSymbolState(
            symbols = results,
            isScanning = _state.value.isScanning,
            totalSignalsToday = _state.value.totalSignalsToday + results.count { it.signal != null },
            lastScanTime = System.currentTimeMillis()
        )

        return results
    }

    fun getSymbolAnalysis(symbol: String): SymbolAnalysis? {
        return _state.value.symbols.find { it.symbol == symbol }
    }
}
