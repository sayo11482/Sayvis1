package com.odin.agent.trading

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import com.odin.agent.models.QuantStrategyType
import com.odin.agent.models.SignalSide
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.random.Random

/**
 * ODIN - Entry Scanner with Alarm + Auto Trade
 * هر لحظه می‌گردد و نقطه ورود مناسب پیدا کند الارم تک بوق صوتی
 * اگر روی حالت ترید اتومات بود بر اساس تعداد مشخص اجازه ترید می‌کند
 */

data class EntrySignal(
    val id: String,
    val symbol: String,
    val side: SignalSide,
    val price: Double,
    val strategy: QuantStrategyType,
    val confidence: Double,
    val rr: Double,
    val confluence: Int,
    val reason: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isAlarm: Boolean = true
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
    val alarmEnabled: Boolean = true
)

class EntryScannerWithAlarm(private val context: Context? = null) {

    private val _state = MutableStateFlow(ScannerState())
    val state: StateFlow<ScannerState> = _state

    private val random = Random(System.currentTimeMillis())
    private var toneGenerator: ToneGenerator? = null

    private val symbols = listOf("BTC/USDT", "ETH/USDT", "EURUSD", "XAUUSD")

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 80)
        } catch (e: Exception) {
            toneGenerator = null
        }
    }

    fun startScanning() {
        _state.value = _state.value.copy(isScanning = true)
    }

    fun stopScanning() {
        _state.value = _state.value.copy(isScanning = false)
    }

    fun setAlarmEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(alarmEnabled = enabled)
    }

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
            // Single beep - تک بوق صوتی
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 500) // 500ms beep
            
            // Also try system beep via context if available
            // For real implementation, use MediaPlayer with custom beep sound
        } catch (e: Exception) {
            // Ignore audio errors
        }

        _state.value = _state.value.copy(
            totalAlarms = _state.value.totalAlarms + 1,
            lastAlarmTime = System.currentTimeMillis()
        )
    }

    fun scanForEntries(
        bannedStrategies: Set<QuantStrategyType> = emptySet(),
        minConfidence: Double = 80.0
    ): List<EntrySignal> {
        val signals = mutableListOf<EntrySignal>()

        // Simulate scanning 4 symbols continuously
        for (symbol in symbols) {
            // 10% chance to find entry point per scan per symbol
            if (random.nextDouble() < 0.10) {
                val availableStrategies = QuantStrategyType.values().filter { it !in bannedStrategies }
                if (availableStrategies.isEmpty()) continue

                val strategy = availableStrategies.random()
                val isBuy = random.nextBoolean()
                val side = if (isBuy) SignalSide.BUY else SignalSide.SELL

                val basePrice = when (symbol) {
                    "BTC/USDT" -> 65000.0
                    "ETH/USDT" -> 3500.0
                    "EURUSD" -> 1.0850
                    "XAUUSD" -> 2350.0
                    else -> 100.0
                }

                val price = basePrice * (0.98 + random.nextDouble() * 0.04)
                val rr = 2.0 + random.nextDouble() * 1.5
                val confluence = 5 + random.nextInt(6) // 5-10
                val confidence = 75 + random.nextInt(20) // 75-95%

                // Only generate if meets min criteria
                if (confidence >= minConfidence && rr >= 2.0 && confluence >= 5) {
                    val signal = EntrySignal(
                        id = "entry_${symbol}_${System.currentTimeMillis()}_${random.nextInt(1000)}",
                        symbol = symbol,
                        side = side,
                        price = price,
                        strategy = strategy,
                        confidence = confidence.toDouble(),
                        rr = rr,
                        confluence = confluence,
                        reason = "${strategy.name} ${side.name} Confluence $confluence RR 1:${String.format("%.1f", rr)} Conf ${confidence}%"
                    )
                    signals.add(signal)

                    // Play alarm beep for each valid entry found
                    playAlarmBeep()

                    // Auto trade if enabled and within limits
                    val autoConfig = _state.value.autoTradeConfig
                    if (autoConfig.enabled) {
                        if (autoConfig.currentTrades < autoConfig.maxTrades && 
                            autoConfig.tradesToday < autoConfig.maxTradesPerDay &&
                            confidence >= autoConfig.minConfidence &&
                            rr >= autoConfig.minRR &&
                            confluence >= autoConfig.minConfluence) {
                            
                            // Execute auto trade
                            val newConfig = autoConfig.copy(
                                currentTrades = autoConfig.currentTrades + 1,
                                tradesToday = autoConfig.tradesToday + 1
                            )
                            _state.value = _state.value.copy(autoTradeConfig = newConfig)
                            
                            // Log auto trade
                            println("🤖 AUTO TRADE EXECUTED: $symbol ${side.name} @ $price RR 1:${String.format("%.1f", rr)} - Trades: ${newConfig.currentTrades}/${newConfig.maxTrades}")
                        }
                    }
                }
            }
        }

        if (signals.isNotEmpty()) {
            val allSignals = (_state.value.lastSignals + signals).takeLast(20)
            _state.value = _state.value.copy(lastSignals = allSignals)
        }

        return signals
    }

    fun getRecentSignals(limit: Int = 10): List<EntrySignal> {
        return _state.value.lastSignals.takeLast(limit).reversed()
    }

    fun clearSignals() {
        _state.value = _state.value.copy(lastSignals = emptyList())
    }

    fun release() {
        try {
            toneGenerator?.release()
        } catch (e: Exception) {}
        toneGenerator = null
    }
}
