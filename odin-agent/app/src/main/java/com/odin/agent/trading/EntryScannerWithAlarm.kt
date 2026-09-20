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
 * ODIN v1.0.24 - Entry Scanner REAL ONLY - VITTAVERSE ONLY - با انتخاب نماد و نمایش استراتژی و اسپرد
 * اسکنر نقاط ورود واقعی - با قابلیت انتخاب نماد - نمایش استراتژی بررسی شده - محاسبه اسپرد
 * تمام سیگنال‌ها از TradingViewIndicators واقعی + قیمت واقعی ویتاورس
 */

data class EntrySignal(
    val id: String,
    val symbol: String,
    val side: SignalSide,
    val price: Double,
    val bid: Double,
    val ask: Double,
    val spread: Double,
    val spreadCostToman: Double,
    val spreadCostUSDT: Double,
    val unit: String,
    val unitFa: String,
    val priceToman: Double,
    val priceUSDT: Double,
    val strategy: QuantStrategyType,
    val strategyDetails: String, // جزئیات استراتژی بررسی شده
    val allStrategiesChecked: List<String>, // تمام استراتژی‌های بررسی شده
    val confidence: Double,
    val rr: Double,
    val confluence: Int,
    val sl: Double = 0.0,
    val tp: Double = 0.0,
    val reason: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isAlarm: Boolean = true,
    val source: String = "REAL Vittaverse",
    val broker: String = "Vittaverse"
)

data class AutoTradeConfig(
    val enabled: Boolean = false,
    val maxTrades: Int = 5,
    val maxTradesPerDay: Int = 10,
    val currentTrades: Int = 0,
    val tradesToday: Int = 0,
    val minConfidence: Double = 80.0,
    val minRR: Double = 2.0,
    val minConfluence: Int = 5,
    val capital: Double = 100.0 // سرمایه ورودی - قبلاً 10$ ثابت بود، الان قابل تنظیم
)

data class ScannerState(
    val isScanning: Boolean = false,
    val lastSignals: List<EntrySignal> = emptyList(),
    val totalAlarms: Int = 0,
    val autoTradeConfig: AutoTradeConfig = AutoTradeConfig(),
    val lastAlarmTime: Long = 0,
    val alarmEnabled: Boolean = true,
    val scannedSymbols: Int = 0,
    val selectedSymbol: String = "ALL", // نماد انتخاب شده - ALL یا یک نماد خاص
    val strategiesCheckedCount: Int = 0
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
    fun setCapital(capital: Double) {
        val current = _state.value.autoTradeConfig
        _state.value = _state.value.copy(autoTradeConfig = current.copy(capital = capital))
    }
    fun setSelectedSymbol(symbol: String) {
        _state.value = _state.value.copy(selectedSymbol = symbol)
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

    // اسکن با انتخاب نماد و نمایش تمام استراتژی‌های بررسی شده
    fun scanForEntries(
        minConfidence: Double = 80.0,
        realPrices: Map<String, RealPrice>? = null,
        candlesMap: Map<String, List<RealCandle>>? = null,
        selectedSymbolFilter: String? = null
    ): List<EntrySignal> {
        val signals = mutableListOf<EntrySignal>()
        if (realPrices == null || realPrices.isEmpty()) {
            _state.value = _state.value.copy(scannedSymbols = symbols.size, strategiesCheckedCount = 0)
            return emptyList()
        }

        val filter = selectedSymbolFilter ?: _state.value.selectedSymbol
        val symbolsToScan = if (filter == "ALL" || filter.isBlank()) symbols else listOf(filter)

        var totalStrategiesChecked = 0

        for (symbol in symbolsToScan) {
            val realPrice = realPrices[symbol] ?: realPrices[symbol.replace("/", "")] ?: continue
            val candles = candlesMap?.get(symbol) ?: candlesMap?.get(symbol.replace("/", "")) ?: continue
            if (candles.size < 20) continue

            val symbolInfo = SymbolManager.find(symbol) ?: continue
            val price = realPrice.price
            val bid = realPrice.bid
            val ask = realPrice.ask
            val spread = realPrice.spread
            val spreadCostToman = realPrice.spreadCostToman
            val spreadCostUSDT = realPrice.spreadCostUSDT

            val closes = candles.map { it.close }
            val highs = candles.map { it.high }
            val lows = candles.map { it.low }

            val ema20 = indicators.ema(closes, 20).lastOrNull() ?: continue
            val ema50 = indicators.ema(closes, 50).lastOrNull() ?: continue
            val rsi = indicators.rsi(closes, 14).lastOrNull() ?: continue
            val adx = indicators.adx(highs, lows, closes, 14).lastOrNull() ?: continue
            val atr = indicators.atr(highs, lows, closes, 14).lastOrNull() ?: (price * 0.01)

            val bb = indicators.bollingerBands(closes, 20, 2.0)
            val bbUpper = bb.upper.lastOrNull()
            val bbLower = bb.lower.lastOrNull()

            var confluence = 0
            var side: SignalSide? = null
            var strategy = QuantStrategyType.TREND_FOLLOWING
            var reason = ""
            var strategyDetails = ""
            val allChecked = mutableListOf<String>()

            // بررسی تمام استراتژی‌ها - نمایش کدام استراتژی چک شد

            // 1. Trend Following
            allChecked.add("Trend: EMA20=${String.format("%.2f", ema20)} EMA50=${String.format("%.2f", ema50)} ADX=${String.format("%.1f", adx)} RSI=${String.format("%.1f", rsi)}")
            totalStrategiesChecked++
            if (ema20 > ema50 && adx > 25 && rsi > 50 && rsi < 70) {
                confluence += 2
                if (side == null) {
                    side = SignalSide.BUY
                    strategy = QuantStrategyType.TREND_FOLLOWING
                    reason = "روند صعودی: EMA20>EMA50 + ADX>25 + RSI 50-70 - ویتاورس"
                    strategyDetails = "دنباله‌روی روند: EMA20=${String.format("%.2f", ema20)} بالای EMA50=${String.format("%.2f", ema50)} + ADX=${String.format("%.1f", adx)} قوی + RSI=${String.format("%.1f", rsi)}"
                }
            } else if (ema20 < ema50 && adx > 25 && rsi < 50 && rsi > 30) {
                confluence += 2
                if (side == null) {
                    side = SignalSide.SELL
                    strategy = QuantStrategyType.TREND_FOLLOWING
                    reason = "روند نزولی: EMA20<EMA50 + ADX>25 + RSI 30-50 - ویتاورس"
                    strategyDetails = "دنباله‌روی روند نزولی: EMA20 زیر EMA50 + ADX قوی + RSI"
                }
            }

            // 2. Mean Reversion
            allChecked.add("MeanRev: BB U=${bbUpper?.let { String.format("%.2f", it) } ?: "N/A"} L=${bbLower?.let { String.format("%.2f", it) } ?: "N/A"} RSI=${String.format("%.1f", rsi)} Price=${String.format("%.2f", price)}")
            totalStrategiesChecked++
            if (bbUpper != null && bbLower != null) {
                if (price <= bbLower && rsi < 30) {
                    confluence += 2
                    if (side == null) {
                        side = SignalSide.BUY
                        strategy = QuantStrategyType.MEAN_REVERSION
                        reason = "بازگشت به میانگین: لمس باند پایین BB + RSI<30 اشباع فروش - ویتاورس"
                        strategyDetails = "بازگشت میانگین: قیمت ${String.format("%.2f", price)} <= BB پایین ${String.format("%.2f", bbLower)} + RSI اشباع فروش ${String.format("%.1f", rsi)}"
                    }
                } else if (price >= bbUpper && rsi > 70) {
                    confluence += 2
                    if (side == null) {
                        side = SignalSide.SELL
                        strategy = QuantStrategyType.MEAN_REVERSION
                        reason = "بازگشت به میانگین: لمس باند بالا BB + RSI>70 اشباع خرید - ویتاورس"
                        strategyDetails = "بازگشت میانگین نزولی: قیمت بالای BB + RSI اشباع خرید"
                    }
                }
            }

            // 3. LIT
            val recentHigh = highs.takeLast(20).maxOrNull() ?: 0.0
            val recentLow = lows.takeLast(20).minOrNull() ?: 0.0
            allChecked.add("LIT: Recent H=${String.format("%.2f", recentHigh)} L=${String.format("%.2f", recentLow)} Price=${String.format("%.2f", price)} Sweep Check")
            totalStrategiesChecked++
            if (price > recentHigh * 1.002 && rsi < 70) {
                val lastClose = closes.lastOrNull() ?: 0.0
                if (lastClose < recentHigh) {
                    confluence += 3
                    if (side == null) {
                        side = SignalSide.SELL
                        strategy = QuantStrategyType.LIT_LIQUIDITY_INVERSION
                        reason = "LIT سوئیپ سقف + ریجکشن - اینورژن نقدینگی - RR 1:3.5 - ویتاورس"
                        strategyDetails = "LIT: قیمت ${String.format("%.2f", price)} سوئیپ سقف ${String.format("%.2f", recentHigh)} + برگشت - نقدینگی جارو شد"
                    }
                }
            } else if (price < recentLow * 0.998 && rsi > 30) {
                val lastClose = closes.lastOrNull() ?: 0.0
                if (lastClose > recentLow) {
                    confluence += 3
                    if (side == null) {
                        side = SignalSide.BUY
                        strategy = QuantStrategyType.LIT_LIQUIDITY_INVERSION
                        reason = "LIT سوئیپ کف + ریجکشن - اینورژن نقدینگی - RR 1:3.5 - ویتاورس"
                        strategyDetails = "LIT: سوئیپ کف ${String.format("%.2f", recentLow)} + برگشت صعودی"
                    }
                }
            }

            // 4. Momentum
            val atrPrev = indicators.atr(highs, lows, closes, 14).dropLast(1).lastOrNull() ?: atr
            allChecked.add("Momentum: ATR=${String.format("%.4f", atr)} Prev=${String.format("%.4f", atrPrev)} RSI=${String.format("%.1f", rsi)} Expansion=${atr > atrPrev*1.5}")
            totalStrategiesChecked++
            if (atr > atrPrev * 1.5 && rsi > 60) {
                confluence += 1
                if (side == null) {
                    side = SignalSide.BUY
                    strategy = QuantStrategyType.MOMENTUM_BREAKOUT
                    reason = "مومنتوم صعودی: گسترش ATR + RSI>60 - ویتاورس"
                    strategyDetails = "مومنتوم: ATR گسترش ${String.format("%.2f", atr/atrPrev)}x + RSI صعودی"
                }
            } else if (atr > atrPrev * 1.5 && rsi < 40) {
                confluence += 1
                if (side == null) {
                    side = SignalSide.SELL
                    strategy = QuantStrategyType.MOMENTUM_BREAKOUT
                    reason = "مومنتوم نزولی: گسترش ATR + RSI<40 - ویتاورس"
                    strategyDetails = "مومنتوم نزولی: ATR گسترش + RSI نزولی"
                }
            }

            // 5. TV80
            allChecked.add("TV80: Confluence=$confluence Need 5+ Conf=${String.format("%.1f", 50+confluence*8)} Need 80%")
            totalStrategiesChecked++

            if (side != null && confluence >= 3) {
                val rr = when (strategy) {
                    QuantStrategyType.LIT_LIQUIDITY_INVERSION -> 3.5
                    QuantStrategyType.TREND_FOLLOWING -> 2.0
                    QuantStrategyType.MEAN_REVERSION -> 1.8
                    QuantStrategyType.MOMENTUM_BREAKOUT -> 2.5
                    QuantStrategyType.TV_80_PERCENT -> 2.3
                    else -> 2.0
                }
                val confidence = (50 + confluence * 8 + adx * 0.5).coerceIn(0.0, 95.0)

                if (confidence >= minConfidence) {
                    val capital = _state.value.autoTradeConfig.capital
                    val spreadCostDesc = if (realPrice.unit == "Toman")
                        "اسپرد ${String.format("%.0f", spread)} تومان هزینه ${String.format("%.0f", spreadCostToman)} تومان"
                    else
                        "اسپرد ${String.format("%.4f", spread)} هزینه ${String.format("%.4f", spreadCostUSDT)} تتر"

                    val entryPrice = if (side == SignalSide.BUY) ask else bid
                    val slPrice = if (side == SignalSide.BUY) entryPrice - atr * 1.5 else entryPrice + atr * 1.5
                    val tpPrice = if (side == SignalSide.BUY) entryPrice + (entryPrice - slPrice) * rr else entryPrice - (slPrice - entryPrice) * rr

                    val signal = EntrySignal(
                        id = "entry_${symbol}_${System.currentTimeMillis()}",
                        symbol = symbol,
                        side = side,
                        price = entryPrice,
                        bid = bid,
                        ask = ask,
                        spread = spread,
                        spreadCostToman = spreadCostToman,
                        spreadCostUSDT = spreadCostUSDT,
                        unit = realPrice.unit,
                        unitFa = realPrice.unitFa,
                        priceToman = realPrice.priceToman,
                        priceUSDT = realPrice.priceUSDT,
                        strategy = strategy,
                        strategyDetails = strategyDetails,
                        allStrategiesChecked = allChecked,
                        confidence = confidence,
                        rr = rr,
                        confluence = confluence,
                        sl = slPrice,
                        tp = tpPrice,
                        reason = "${strategy.name} ${side.name} $symbol $reason | $spreadCostDesc | سرمایه ${capital}$ | Confluence $confluence RR 1:${String.format("%.1f", rr)} Conf ${confidence.toInt()}% - REAL ${realPrice.source} - بروکر ویتاورس",
                        source = "REAL ${realPrice.source} - Vittaverse",
                        broker = "Vittaverse"
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
            val allSignals = (_state.value.lastSignals + signals).takeLast(50)
            _state.value = _state.value.copy(lastSignals = allSignals, scannedSymbols = symbolsToScan.size, strategiesCheckedCount = totalStrategiesChecked)
        } else {
            _state.value = _state.value.copy(scannedSymbols = symbolsToScan.size, strategiesCheckedCount = totalStrategiesChecked)
        }

        return signals
    }

    fun getRecentSignals(limit: Int = 20): List<EntrySignal> = _state.value.lastSignals.takeLast(limit).reversed()
    fun clearSignals() { _state.value = _state.value.copy(lastSignals = emptyList()) }
    fun release() {
        try { toneGenerator?.release() } catch (e: Exception) {}
        toneGenerator = null
    }
}
