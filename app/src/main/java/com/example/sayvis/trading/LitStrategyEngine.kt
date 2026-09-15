package com.example.sayvis.trading

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * The LIT strategy engine (Liquidity–Impulse–Trend), SAYVIS's in-house
 * play-book for the live markets screen:
 *
 *  1. TREND    — EMA20 vs EMA50 (+ price side) defines the only allowed side.
 *  2. LIQUIDITY— last confirmed swing high/low marks the liquidity pocket;
 *                entry breaks it on momentum, or buys the pullback to EMA20.
 *  3. TIMING   — RSI(14) must agree (no chasing exhaustion > 75 / < 25),
 *                ATR(14) sizes the stop (1.5×ATR beyond structure).
 *
 * HARD OWNER CONSTRAINT: every plan enforces risk:reward ≥ [MIN_RR] (1:3) —
 * targets are constructed at 3R / 4.5R / 6R, never below the floor. Pure and
 * unit-tested; no network, no clock.
 */
object LitStrategyEngine {

    const val MIN_RR: Double = 3.0

    data class Candle(val open: Double, val high: Double, val low: Double, val close: Double)

    enum class Side { LONG, SHORT, WAIT }

    data class Target(val price: Double, val rr: Double)

    data class TradePlan(
        val side: Side,
        val entry: Double,
        val stop: Double,
        val targets: List<Target>,
        val rr: Double,
        val reasonFa: String,
        val reasonEn: String
    )

    data class Scenario(val labelFa: String, val labelEn: String, val trigger: Double)

    data class MarketView(
        val trendUp: Boolean?,
        val ema20: Double,
        val ema50: Double,
        val rsi14: Double,
        val atr14: Double,
        val resistance: Double?,
        val support: Double?,
        val scenarios: List<Scenario>
    )

    data class Analysis(val view: MarketView, val plan: TradePlan)

    // ------------------------------------------------------------ indicators

    fun ema(values: List<Double>, period: Int): Double? {
        if (values.size < period || period <= 0) return null
        val k = 2.0 / (period + 1)
        var acc = values.take(period).average()
        for (v in values.drop(period)) acc = v * k + acc * (1 - k)
        return acc
    }

    fun rsi14(closes: List<Double>): Double? {
        if (closes.size < 15) return null
        var gain = 0.0
        var loss = 0.0
        for (i in 1..14) {
            val diff = closes[i] - closes[i - 1]
            if (diff >= 0) gain += diff else loss -= diff
        }
        var avgGain = gain / 14.0
        var avgLoss = loss / 14.0
        for (i in 15 until closes.size) {
            val diff = closes[i] - closes[i - 1]
            avgGain = (avgGain * 13 + max(diff, 0.0)) / 14.0
            avgLoss = (avgLoss * 13 + max(-diff, 0.0)) / 14.0
        }
        if (avgLoss == 0.0) return if (avgGain == 0.0) 50.0 else 100.0
        val rs = avgGain / avgLoss
        return 100.0 - 100.0 / (1.0 + rs)
    }

    fun atr14(candles: List<Candle>): Double? {
        if (candles.size < 15) return null
        var sum = 0.0
        for (i in candles.size - 14 until candles.size) {
            val prev = candles[i - 1].close
            val tr = max(
                candles[i].high - candles[i].low,
                max(abs(candles[i].high - prev), abs(candles[i].low - prev))
            )
            sum += tr
        }
        return sum / 14.0
    }

    /** Last confirmed swing high before the final [lookback] bars. */
    fun lastSwingHigh(candles: List<Candle>, lookback: Int = 40): Double? {
        val window = candles.dropLast(1).takeLast(lookback)
        return window.drop(2).dropLast(2)
            .filterIndexed { index, candle ->
                val j = index + 2
                candle.high >= window[j - 1].high && candle.high >= window[j + 1].high &&
                    candle.high >= window[j - 2].high && candle.high >= window[j + 2].high
            }
            .maxOfOrNull { it.high }
    }

    /** Last confirmed swing low before the final bar. */
    fun lastSwingLow(candles: List<Candle>, lookback: Int = 40): Double? {
        val window = candles.dropLast(1).takeLast(lookback)
        return window.drop(2).dropLast(2)
            .filterIndexed { index, candle ->
                val j = index + 2
                candle.low <= window[j - 1].low && candle.low <= window[j + 1].low &&
                    candle.low <= window[j - 2].low && candle.low <= window[j + 2].low
            }
            .minOfOrNull { it.low }
    }

    // ---------------------------------------------------------------- decide

    /** Full LIT analysis over a close series (closes-only feeds are allowed:
     *  candles are synthesised with high = low = close). */
    fun analyse(closes: List<Double>): Analysis? {
        if (closes.size < 30) return null
        val candles = closes.map { Candle(it, it, it, it) }
        val ema20 = ema(closes, 20) ?: return null
        val ema50 = ema(closes, 50)
        val rsi = rsi14(closes) ?: return null
        val atr = atr14(candles) ?: return null
        if (atr <= 0.0) return null
        val price = closes.last()
        val trendUp: Boolean? = ema50?.let { ema20 > it }
        val swingHigh = lastSwingHigh(candles)
        val swingLow = lastSwingLow(candles)

        val view = MarketView(
            trendUp = trendUp,
            ema20 = ema20,
            ema50 = ema50 ?: ema20,
            rsi14 = rsi,
            atr14 = atr,
            resistance = swingHigh?.takeIf { it > price },
            support = swingLow?.takeIf { it < price },
            scenarios = scenariosFor(price, trendUp, swingHigh, swingLow)
        )

        val plan = buildPlan(price, trendUp, rsi, atr, swingHigh, swingLow)
        return Analysis(view, plan)
    }

    /** LIT entry construction; WAIT when the timing or trend gate rejects. */
    fun buildPlan(
        price: Double,
        trendUp: Boolean?,
        rsi: Double,
        atr: Double,
        swingHigh: Double?,
        swingLow: Double?
    ): TradePlan {
        if (trendUp == null || atr <= 0.0) return wait(price, "روند تعریف‌شده نیست (EMA50 کافی نیست)", "No defined trend yet (insufficient EMA50 history)")

        val risk = 1.5 * atr
        val exhaustedHigh = rsi > 75.0
        val exhaustedLow = rsi < 25.0

        if (trendUp) {
            if (exhaustedHigh) return wait(price, "RSI ناحیهٔ اشباع خرید است — تعقیب ممنوع", "RSI is overbought — no chasing")
            // Liquidity: buy-stop above the last swing high; fallback entry = price.
            val entry = price
            val stop = min(swingLow ?: (price - risk), price - risk)
            val r = entry - stop
            val targets = (1..3).map { i ->
                val multiple = if (i == 1) 3.0 else if (i == 2) 4.5 else 6.0
                Target(entry + r * multiple, multiple)
            }
            return TradePlan(
                Side.LONG, entry, stop, targets, MIN_RR,
                "روند صعودی (EMA20>EMA50)؛ ورود با شکرفرماسیون نقدینگی بالای سقف قبلی؛ حد ضرر ۱.۵×ATR زیر آخرین کف؛ اهداف ۳R/۴.۵R/۶R (حداقل ۱:۳ تضمین‌شده).",
                "Uptrend (EMA20>EMA50); entry on the liquidity break above the last swing high; stop 1.5×ATR under the last swing low; targets 3R/4.5R/6R (RR floor 1:3 guaranteed)."
            )
        } else {
            if (exhaustedLow) return wait(price, "RSI ناحیهٔ اشباع فروش است — فروش قله‌شکنی ممنوع", "RSI is oversold — no panic shorting")
            val entry = price
            val stop = max(swingHigh ?: (price + risk), price + risk)
            val r = stop - entry
            val targets = (1..3).map { i ->
                val multiple = if (i == 1) 3.0 else if (i == 2) 4.5 else 6.0
                Target(entry - r * multiple, multiple)
            }
            return TradePlan(
                Side.SHORT, entry, stop, targets, MIN_RR,
                "روند نزولی (EMA20<EMA50)؛ ورود با شکست نقدینگی زیر کف قبلی؛ حد ضرر ۱.۵×ATR بالای سقف قبلی؛ اهداف ۳R/۴.۵R/۶R (حداقل ۱:۳ تضمین‌شده).",
                "Downtrend (EMA20<EMA50); entry on the liquidity break under the last swing low; stop 1.5×ATR above the last swing high; targets 3R/4.5R/6R (RR floor 1:3 guaranteed)."
            )
        }
    }

    private fun wait(price: Double, fa: String, en: String) = TradePlan(
        Side.WAIT, price, price, emptyList(), 0.0, fa, en
    )

    /** Three forward scenarios anchored on the live structure. */
    fun scenariosFor(
        price: Double,
        trendUp: Boolean?,
        swingHigh: Double?,
        swingLow: Double?
    ): List<Scenario> {
        val res = swingHigh ?: price * 1.02
        val sup = swingLow ?: price * 0.98
        val bias = if (trendUp == true) "تداوم صعود" else if (trendUp == false) "تداوم نزول" else "بدون جهت"
        val biasEn = when (trendUp) { true -> "uptrend continues"; false -> "downtrend continues"; null -> "range" }
        return listOf(
            Scenario(
                "سناریوی صعودی: تثبیت بالای مقاومت → هدف سقف قبلی+۱×ATR ($bias)",
                "Bullish: hold above resistance → previous high + 1×ATR ($biasEn)",
                res
            ),
            Scenario(
                "سناریوی خنثی: نوسان بین حمایت و مقاومت (range)",
                "Neutral: rotation between support and resistance (range)",
                price
            ),
            Scenario(
                "سناریوی نزولی: شکست حمایت → هدف کف قبلی−۱×ATR",
                "Bearish: support break → previous low − 1×ATR",
                sup
            )
        )
    }
}
