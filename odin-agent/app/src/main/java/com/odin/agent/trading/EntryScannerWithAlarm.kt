package com.odin.agent.trading

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import com.odin.agent.indicators.TradingViewIndicators
import com.odin.agent.models.QuantStrategyType
import com.odin.agent.models.SignalSide
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * ODIN v1.0.21 - Entry Scanner REAL ONLY - NO FAKE - Meta Fix
 * اسکنر نقاط ورود واقعی - بدون Random - فقط با اندیکاتور واقعی
 * تمام سیگنال‌ها از TradingViewIndicators واقعی + قیمت واقعی
 */

data class EntrySignal(
    val id: String,
    val symbol: String,
    val side: SignalSide,
    val price: Double,
    val bid: Double,
    val ask: Double,
    val strategy: QuantStrategyType,
    val confidence: Double,
    val rr: Double,
    val confluence: Int,
    val reason: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isAlarm: Boolean = true,
    val source: String = "REAL"
)

data class AutoTradeConfig(
    val enabled: Boolean = false,
    val maxTrades: Int = 5,
    val maxTradesPerDay: Int = 10,
    val currentTrades: Int = 0,
    val tradesToday: Int = 0,
    val minConfidence: Double = 80.0,
    val minRR: Double = 2.0,
    val minConfluence: Int = 5
)

data class ScannerState(
    val isScanning: Boolean = false,
    val lastSignals: List<EntrySignal> = emptyList(),
    val totalAlarms: Int = 0,
    val autoTradeConfig: AutoTradeConfig = AutoTradeConfig(),
    val lastAlarmTime: Long = 0,
    val alarmEnabled: Boolean = true,
    val scannedSymbols: Int = 0
)

class EntryScannerWithAlarm(private val context: Context? = null) {

    private val _state = MutableStateFlow(ScannerState())
    val state: StateFlow<ScannerState> = _state

    private var toneGenerator: ToneGenerator? = null
    private val indicators = TradingViewIndicators()

    private val symbols = SymbolManager.getTradableSymbols().map { it.symbol }

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 80)
        } catch (e: Exception) {
            toneGenerator = null
        }
    }

    fun startScanning() { _state.value = _state.value.copy(isScanning = true) }
    fun stopScanning() { _state.value = _state.value.copy(isScanning = false) }
    fun setAlarmEnabled(enabled: Boolean) { _state.value = _state.value.copy(alarmEnabled = enabled) }
    fun setAutoTradeEnabled(enabled: Boolean) {
        val current = _state.value.autoTradeConfig
        _state.value = _state.value.copy(autoTradeConfig = current.copy(enabled = enabled))
    }
    fun setMaxTrades(max: Int) {
        val current = _state.value.autoTradeConfig
        _state.value = _state.value.copy(autoTradeConfig = current.copy(maxTrades = max))
    }
    fun setMaxTradesPerDay(max: Int) {
        val current = _state.value.autoTradeConfig
        _state.value = _state.value.copy(autoTradeConfig = current.copy(maxTradesPerDay = max))
    }
    fun resetDailyTrades() {
        val current = _state.value.autoTradeConfig
        _state.value = _state.value.copy(autoTradeConfig = current.copy(tradesToday = 0))
    }

    fun playAlarmBeep() {
        if (!_state.value.alarmEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 500)
        } catch (e: Exception) {}
        _state.value = _state.value.copy(totalAlarms = _state.value.totalAlarms + 1, lastAlarmTime = System.currentTimeMillis())
    }

    fun scanForEntries(
        minConfidence: Double = 80.0,
        realPrices: Map<String, RealPrice>? = null,
        candlesMap: Map<String, List<RealCandle>>? = null
    ): List<EntrySignal> {
        val signals = mutableListOf<EntrySignal>()
        if (realPrices == null || realPrices.isEmpty()) {
            _state.value = _state.value.copy(scannedSymbols = symbols.size)
            return emptyList() // No fake when no real data - Meta fix
        }

        for (symbol in symbols) {
            val realPrice = realPrices[symbol] ?: realPrices[symbol.replace("/", "")] ?: continue
            val candles = candlesMap?.get(symbol) ?: candlesMap?.get(symbol.replace("/", "")) ?: continue
            if (candles.size < 20) continue // Need enough candles for real indicators

            val symbolInfo = SymbolManager.find(symbol) ?: continue
            val price = realPrice.price
            val bid = realPrice.bid
            val ask = realPrice.ask

            // REAL indicator analysis - no random
            val closes = candles.map { it.close }
            val highs = candles.map { it.high }
            val lows = candles.map { it.low }

            val ema20 = indicators.ema(closes, 20).lastOrNull() ?: continue
            val ema50 = indicators.ema(closes, 50).lastOrNull() ?: continue
            val rsi = indicators.rsi(closes, 14).lastOrNull() ?: continue
            val adx = indicators.adx(highs, lows, closes, 14).lastOrNull() ?: continue

            var confluence = 0
            var side: SignalSide? = null
            var strategy = QuantStrategyType.TREND_FOLLOWING
            var reason = ""

            // Trend Following REAL
            if (ema20 > ema50 && adx > 25 && rsi > 50 && rsi < 70) {
                confluence += 2
                if (side == null) side = SignalSide.BUY
                strategy = QuantStrategyType.TREND_FOLLOWING
                reason = "EMA20>EMA50 + ADX>25 + RSI 50-70"
            } else if (ema20 < ema50 && adx > 25 && rsi < 50 && rsi > 30) {
                confluence += 2
                if (side == null) side = SignalSide.SELL
                strategy = QuantStrategyType.TREND_FOLLOWING
                reason = "EMA20<EMA50 + ADX>25 + RSI 30-50"
            }

            // Mean Reversion REAL - BB + RSI
            val bb = indicators.bollingerBands(closes, 20, 2.0)
            val bbUpper = bb.upper.lastOrNull()
            val bbLower = bb.lower.lastOrNull()
            if (bbUpper != null && bbLower != null) {
                if (price <= bbLower && rsi < 30) {
                    confluence += 2
                    if (side == null) {
                        side = SignalSide.BUY
                        strategy = QuantStrategyType.MEAN_REVERSION
                        reason = "BB Lower touch + RSI<30 oversold"
                    }
                } else if (price >= bbUpper && rsi > 70) {
                    confluence += 2
                    if (side == null) {
                        side = SignalSide.SELL
                        strategy = QuantStrategyType.MEAN_REVERSION
                        reason = "BB Upper touch + RSI>70 overbought"
                    }
                }
            }

            // LIT REAL - Liquidity sweep detection (simplified real)
            val recentHigh = highs.takeLast(20).maxOrNull() ?: 0.0
            val recentLow = lows.takeLast(20).minOrNull() ?: 0.0
            if (price > recentHigh * 1.002 && rsi < 70) { // Sweep high then reversal
                val lastClose = closes.lastOrNull() ?: 0.0
                if (lastClose < recentHigh) {
                    confluence += 3
                    if (side == null) {
                        side = SignalSide.SELL
                        strategy = QuantStrategyType.LIT_LIQUIDITY_INVERSION
                        reason = "LIT Sweep High + Rejection - Liquidity Inversion"
                    }
                }
            } else if (price < recentLow * 0.998 && rsi > 30) {
                val lastClose = closes.lastOrNull() ?: 0.0
                if (lastClose > recentLow) {
                    confluence += 3
                    if (side == null) {
                        side = SignalSide.BUY
                        strategy = QuantStrategyType.LIT_LIQUIDITY_INVERSION
                        reason = "LIT Sweep Low + Rejection - Liquidity Inversion"
                    }
                }
            }

            // Momentum REAL - ATR expansion
            val atr = indicators.atr(highs, lows, closes, 14).lastOrNull() ?: 0.0
            val atrPrev = indicators.atr(highs, lows, closes, 14).dropLast(1).lastOrNull() ?: atr
            if (atr > atrPrev * 1.5 && rsi > 60) {
                confluence += 1
                if (side == null) {
                    side = SignalSide.BUY
                    strategy = QuantStrategyType.MOMENTUM_BREAKOUT
                    reason = "ATR Expansion + RSI>60 Momentum"
                }
            } else if (atr > atrPrev * 1.5 && rsi < 40) {
                confluence += 1
                if (side == null) {
                    side = SignalSide.SELL
                    strategy = QuantStrategyType.MOMENTUM_BREAKOUT
                    reason = "ATR Expansion + RSI<40 Momentum"
                }
            }

            if (side != null && confluence >= 3) {
                val rr = when (strategy) {
                    QuantStrategyType.LIT_LIQUIDITY_INVERSION -> 3.5
                    QuantStrategyType.TREND_FOLLOWING -> 2.0
                    QuantStrategyType.MEAN_REVERSION -> 1.8
                    QuantStrategyType.MOMENTUM_BREAKOUT -> 2.5
                    else -> 2.0
                }
                val confidence = (50 + confluence * 8 + adx * 0.5).coerceIn(0.0, 95.0)

                if (confidence >= minConfidence && rr >= 2.0 && confluence >= 5) {
                    val signal = EntrySignal(
                        id = "entry_${symbol}_${System.currentTimeMillis()}",
                        symbol = symbol,
                        side = side,
                        price = if (side == SignalSide.BUY) ask else bid,
                        bid = bid,
                        ask = ask,
                        strategy = strategy,
                        confidence = confidence,
                        rr = rr,
                        confluence = confluence,
                        reason = "${strategy.name} ${side.name} $symbol $reason Confluence $confluence RR 1:${String.format("%.1f", rr)} Conf ${confidence.toInt()}% - REAL ${realPrice.source}",
                        source = "REAL ${realPrice.source}"
                    )
                    signals.add(signal)
                    playAlarmBeep()

                    val autoConfig = _state.value.autoTradeConfig
                    if (autoConfig.enabled) {
                        if (autoConfig.currentTrades < autoConfig.maxTrades &&
                            autoConfig.tradesToday < autoConfig.maxTradesPerDay &&
                            confidence >= autoConfig.minConfidence &&
                            rr >= autoConfig.minRR &&
                            confluence >= autoConfig.minConfluence) {
                            val newConfig = autoConfig.copy(currentTrades = autoConfig.currentTrades + 1, tradesToday = autoConfig.tradesToday + 1)
                            _state.value = _state.value.copy(autoTradeConfig = newConfig)
                        }
                    }
                }
            }
        }

        if (signals.isNotEmpty()) {
            val allSignals = (_state.value.lastSignals + signals).takeLast(30)
            _state.value = _state.value.copy(lastSignals = allSignals, scannedSymbols = symbols.size)
        } else {
            _state.value = _state.value.copy(scannedSymbols = symbols.size)
        }

        return signals
    }

    fun getRecentSignals(limit: Int = 10): List<EntrySignal> = _state.value.lastSignals.takeLast(limit).reversed()
    fun clearSignals() { _state.value = _state.value.copy(lastSignals = emptyList()) }
    fun release() {
        try { toneGenerator?.release() } catch (e: Exception) {}
        toneGenerator = null
    }
}
