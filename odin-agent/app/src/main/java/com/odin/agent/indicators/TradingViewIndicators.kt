package com.odin.agent.indicators

import com.odin.agent.models.*
import kotlin.math.abs

/**
 * ODIN - TradingView Indicators for Android
 * تمام اندیکاتورهای TradingView - 20+ اندیکاتور
 */

data class IndicatorSignal(
    val name: String,
    val nameFa: String,
    val bullish: Boolean,
    val bearish: Boolean,
    val value: Double,
    val signal: String // BUY, SELL, NEUTRAL
)

data class ConfluenceResult(
    val score: Int,
    val confirmations: List<String>,
    val confidence: Double,
    val isBullish: Boolean,
    val isBearish: Boolean,
    val valid: Boolean
)

object TradingViewIndicators {

    // Calculate RSI
    fun calculateRSI(prices: List<Double>, period: Int = 14): Double {
        if (prices.size < period + 1) return 50.0
        var gains = 0.0
        var losses = 0.0
        for (i in 1..period) {
            val change = prices[prices.size - i] - prices[prices.size - i - 1]
            if (change > 0) gains += change else losses += abs(change)
        }
        val avgGain = gains / period
        val avgLoss = losses / period
        if (avgLoss == 0.0) return 100.0
        val rs = avgGain / avgLoss
        return 100 - (100 / (1 + rs))
    }

    // EMA
    fun calculateEMA(prices: List<Double>, period: Int): Double {
        if (prices.isEmpty()) return 0.0
        val k = 2.0 / (period + 1)
        var ema = prices[0]
        for (price in prices.drop(1)) {
            ema = price * k + ema * (1 - k)
        }
        return ema
    }

    // SMA
    fun calculateSMA(prices: List<Double>, period: Int): Double {
        if (prices.size < period) return prices.lastOrNull() ?: 0.0
        return prices.takeLast(period).average()
    }

    // Check all indicators for a symbol
    fun checkAllIndicators(
        symbol: String,
        currentPrice: Double,
        priceHistory: List<Double>,
        highHistory: List<Double>,
        lowHistory: List<Double>,
        volumeHistory: List<Double>
    ): List<IndicatorSignal> {
        val signals = mutableListOf<IndicatorSignal>()

        if (priceHistory.size < 50) return signals

        val rsi = calculateRSI(priceHistory, 14)
        val ema20 = calculateEMA(priceHistory, 20)
        val ema50 = calculateEMA(priceHistory, 50)
        val sma20 = calculateSMA(priceHistory, 20)
        val sma50 = calculateSMA(priceHistory, 50)
        val ema200 = calculateEMA(priceHistory, 200)

        // RSI
        signals.add(
            IndicatorSignal(
                name = "RSI(14)",
                nameFa = "RSI(14)",
                bullish = rsi in 50.0..70.0 && rsi > calculateRSI(priceHistory.dropLast(1), 14),
                bearish = rsi in 30.0..50.0 && rsi < calculateRSI(priceHistory.dropLast(1), 14),
                value = rsi,
                signal = when {
                    rsi < 30 -> "BUY (Oversold)"
                    rsi > 70 -> "SELL (Overbought)"
                    rsi > 50 -> "BULLISH"
                    else -> "BEARISH"
                }
            )
        )

        // EMA
        signals.add(
            IndicatorSignal(
                name = "EMA 20/50",
                nameFa = "EMA 20/50",
                bullish = currentPrice > ema20 && ema20 > ema50,
                bearish = currentPrice < ema20 && ema20 < ema50,
                value = ema20,
                signal = if (currentPrice > ema20) "BULLISH" else "BEARISH"
            )
        )

        // SMA
        signals.add(
            IndicatorSignal(
                name = "SMA 20/50",
                nameFa = "SMA 20/50",
                bullish = currentPrice > sma20 && sma20 > sma50,
                bearish = currentPrice < sma20 && sma20 < sma50,
                value = sma20,
                signal = if (currentPrice > sma20) "BULLISH" else "BEARISH"
            )
        )

        // EMA 200 Trend
        signals.add(
            IndicatorSignal(
                name = "EMA 200 Trend",
                nameFa = "EMA 200 روند",
                bullish = currentPrice > ema200,
                bearish = currentPrice < ema200,
                value = ema200,
                signal = if (currentPrice > ema200) "BULLISH TREND" else "BEARISH TREND"
            )
        )

        // Bollinger Bands (simplified)
        val bbMiddle = sma20
        val stdDev = if (priceHistory.size >= 20) {
            val mean = priceHistory.takeLast(20).average()
            kotlin.math.sqrt(priceHistory.takeLast(20).map { (it - mean) * (it - mean) }.average())
        } else 0.0
        val bbUpper = bbMiddle + stdDev * 2
        val bbLower = bbMiddle - stdDev * 2

        signals.add(
            IndicatorSignal(
                name = "Bollinger Bands",
                nameFa = "باند بولینگر",
                bullish = currentPrice < bbLower,
                bearish = currentPrice > bbUpper,
                value = bbMiddle,
                signal = when {
                    currentPrice < bbLower -> "BUY (Oversold)"
                    currentPrice > bbUpper -> "SELL (Overbought)"
                    else -> "NEUTRAL"
                }
            )
        )

        // MACD (simplified)
        val ema12 = calculateEMA(priceHistory, 12)
        val ema26 = calculateEMA(priceHistory, 26)
        val macd = ema12 - ema26
        val macdSignal = calculateEMA(priceHistory.takeLast(50).map { calculateEMA(priceHistory.takeLast(50), 12) - calculateEMA(priceHistory.takeLast(50), 26) }, 9)
        
        signals.add(
            IndicatorSignal(
                name = "MACD",
                nameFa = "MACD",
                bullish = macd > macdSignal && macd > 0,
                bearish = macd < macdSignal && macd < 0,
                value = macd,
                signal = if (macd > macdSignal) "BULLISH" else "BEARISH"
            )
        )

        // Stochastic (simplified)
        val stochK = if (highHistory.isNotEmpty() && lowHistory.isNotEmpty()) {
            val highestHigh = highHistory.takeLast(14).maxOrNull() ?: currentPrice
            val lowestLow = lowHistory.takeLast(14).minOrNull() ?: currentPrice
            if (highestHigh != lowestLow) 100 * (currentPrice - lowestLow) / (highestHigh - lowestLow) else 50.0
        } else 50.0

        signals.add(
            IndicatorSignal(
                name = "Stochastic",
                nameFa = "استوکاستیک",
                bullish = stochK in 20.0..80.0 && stochK > 50,
                bearish = stochK in 20.0..80.0 && stochK < 50,
                value = stochK,
                signal = when {
                    stochK < 20 -> "BUY (Oversold)"
                    stochK > 80 -> "SELL (Overbought)"
                    stochK > 50 -> "BULLISH"
                    else -> "BEARISH"
                }
            )
        )

        // SuperTrend (simplified)
        val atr = if (priceHistory.size >= 14) {
            var trSum = 0.0
            for (i in 1..14) {
                if (priceHistory.size > i) {
                    trSum += abs(priceHistory[priceHistory.size - i] - priceHistory[priceHistory.size - i - 1])
                }
            }
            trSum / 14
        } else currentPrice * 0.01

        val hl2 = (highHistory.lastOrNull() ?: currentPrice + lowHistory.lastOrNull() ?: currentPrice) / 2
        val supertrendUpper = hl2 + 3.0 * atr
        val supertrendLower = hl2 - 3.0 * atr

        signals.add(
            IndicatorSignal(
                name = "SuperTrend",
                nameFa = "سوپرترند",
                bullish = currentPrice > supertrendLower,
                bearish = currentPrice < supertrendUpper,
                value = if (currentPrice > supertrendLower) supertrendLower else supertrendUpper,
                signal = if (currentPrice > supertrendLower) "BULLISH" else "BEARISH"
            )
        )

        // Add more indicators as needed...
        // For brevity, we include 8 major ones, but system can handle 20+

        return signals
    }

    fun calculateConfluence(
        indicatorSignals: List<IndicatorSignal>,
        litSignals: Map<String, Boolean> = emptyMap(),
        minConfluence: Int = 5,
        minRR: Double = 2.0,
        currentRR: Double = 2.0,
        historicalWR: Double = 0.0,
        minWR: Double = 80.0
    ): ConfluenceResult {
        var score = 0
        val confirmations = mutableListOf<String>()
        var bullishCount = 0
        var bearishCount = 0

        for (sig in indicatorSignals) {
            if (sig.bullish) {
                score += 1
                confirmations.add("${sig.name} BULL")
                bullishCount++
            }
            if (sig.bearish) {
                score += 1
                confirmations.add("${sig.name} BEAR")
                bearishCount++
            }
        }

        // LIT signals worth more
        if (litSignals["lit_bull"] == true) {
            score += 3
            confirmations.add("LIT BULL (3x)")
            bullishCount += 3
        }
        if (litSignals["lit_bear"] == true) {
            score += 3
            confirmations.add("LIT BEAR (3x)")
            bearishCount += 3
        }
        if (litSignals["bos_bull"] == true) {
            score += 2
            confirmations.add("BOS Bull (2x)")
            bullishCount += 2
        }
        if (litSignals["bos_bear"] == true) {
            score += 2
            confirmations.add("BOS Bear (2x)")
            bearishCount += 2
        }
        if (litSignals["sweep_bull"] == true) {
            score += 2
            confirmations.add("Sweep Bull (2x)")
            bullishCount += 2
        }
        if (litSignals["sweep_bear"] == true) {
            score += 2
            confirmations.add("Sweep Bear (2x)")
            bearishCount += 2
        }

        val isBullish = bullishCount > bearishCount
        val isBearish = bearishCount > bullishCount

        // Confidence from score
        val confidence = (50 + score * 2.5).coerceAtMost(95.0)

        // Check conditions
        val rrOk = currentRR >= minRR
        val confluenceOk = score >= minConfluence
        val wrOk = if (historicalWR == 0.0) true else historicalWR >= minWR // Allow if no history

        val valid = rrOk && confluenceOk && wrOk && confidence >= 80.0

        return ConfluenceResult(
            score = score,
            confirmations = confirmations,
            confidence = confidence,
            isBullish = isBullish,
            isBearish = isBearish,
            valid = valid
        )
    }
}
