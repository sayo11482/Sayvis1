package com.example.sayvis

import com.example.sayvis.trading.LitStrategyEngine
import com.example.sayvis.trading.MarketDataService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the LIT trading brain and the live-market parsers against the real
 * response shapes observed in the live audit (Frankfurter ECB, CoinGecko,
 * exchangerate-api). The engine's hard owner constraint — every executed plan
 * carries risk:reward ≥ 1:3 — is asserted on synthetic trends with known
 * answers.
 */
class SayvisLitTradingUnitTest {

    /** Realistic uptrend: +1.0,+0.8,+1.2,−1.4,+0.6 repeated — RSI lands ≈ 68. */
    private fun rising(n: Int = 80, start: Double = 100.0): List<Double> {
        val steps = listOf(1.0, 0.8, 1.2, -1.4, 0.6)
        val out = ArrayList<Double>(n)
        var v = start
        for (i in 0 until n) {
            out.add(v)
            v += steps[i % steps.size]
        }
        return out
    }

    /** Realistic downtrend: mirrored pattern — RSI lands ≈ 31. */
    private fun falling(n: Int = 80, start: Double = 400.0): List<Double> {
        val steps = listOf(-1.0, -0.8, -1.2, 1.4, -0.6)
        val out = ArrayList<Double>(n)
        var v = start
        for (i in 0 until n) {
            out.add(v)
            v += steps[i % steps.size]
        }
        return out
    }

    @Test
    fun `uptrend yields a long plan with rr floor 1 to 3`() {
        val analysis = LitStrategyEngine.analyse(rising())!!
        assertEquals(LitStrategyEngine.Side.LONG, analysis.plan.side)
        assertTrue(analysis.plan.rr >= 3.0)
        assertTrue(analysis.plan.stop < analysis.plan.entry)
        val targets = analysis.plan.targets
        assertEquals(3, targets.size)
        assertEquals(3.0, targets[0].rr, 1e-9)
        assertEquals(4.5, targets[1].rr, 1e-9)
        assertEquals(6.0, targets[2].rr, 1e-9)
        // Geometric consistency: each target sits beyond the previous one.
        assertTrue(targets[0].price < targets[1].price && targets[1].price < targets[2].price)
        // The achieved RR of target 1 honours the floor.
        val r = analysis.plan.entry - analysis.plan.stop
        assertTrue((targets[0].price - analysis.plan.entry) / r >= 3.0 - 1e-9)
    }

    @Test
    fun `downtrend yields a short plan with rr floor 1 to 3`() {
        val analysis = LitStrategyEngine.analyse(falling())!!
        assertEquals(LitStrategyEngine.Side.SHORT, analysis.plan.side)
        assertTrue(analysis.plan.stop > analysis.plan.entry)
        val targets = analysis.plan.targets
        // Short targets DESCEND away from the entry.
        assertTrue(targets[0].price > targets[1].price && targets[1].price > targets[2].price)
        val r = analysis.plan.stop - analysis.plan.entry
        assertTrue((analysis.plan.entry - targets[0].price) / r >= 3.0 - 1e-9)
    }

    @Test
    fun `insufficient history and exhaustion wait instead of trade`() {
        assertNull(LitStrategyEngine.analyse(List(20) { 100.0 + it }))
        // Flat market: no atr edge.
        assertNull(LitStrategyEngine.analyse(List(60) { 100.0 }))
        // Exhausted high: RSI pinned at 100 on a moon-candle series -> WAIT.
        val moon = List(60) { 100.0 + it * it * 0.01 }
        val analysis = LitStrategyEngine.analyse(moon)!!
        // With RSI saturated, building the plan directly must WAIT.
        val wait = LitStrategyEngine.buildPlan(
            price = moon.last(), trendUp = true, rsi = 80.0, atr = 1.0,
            swingHigh = null, swingLow = null
        )
        assertEquals(LitStrategyEngine.Side.WAIT, wait.side)
        assertNotNull(analysis)
    }

    @Test
    fun `indicators compute expected values on known series`() {
        // EMA of a constant series is the constant.
        assertEquals(42.0, LitStrategyEngine.ema(List(60) { 42.0 }, 20)!!, 1e-9)
        // RSI of strictly rising closes is 100; strictly falling is 0.
        assertEquals(100.0, LitStrategyEngine.rsi14(List(30) { it.toDouble() })!!, 1e-9)
        assertEquals(0.0, LitStrategyEngine.rsi14(List(30) { -it.toDouble() })!!, 1e-9)
        // ATR of unit-range candles is ~1.
        val candles = List(30) { LitStrategyEngine.Candle(0.0, 1.0, 0.0, 0.5) }
        assertEquals(1.0, LitStrategyEngine.atr14(candles)!!, 1e-9)
    }

    @Test
    fun `frankfurter parser reads the real ecb payload sorted by date`() {
        val payload = """{"amount":1.0,"base":"EUR","start_date":"2026-09-14",
            "rates":{"2026-09-14":{"USD":1.1551},"2026-08-20":{"USD":1.1681},"2026-09-15":{"USD":1.1539}}}"""
        val closes = MarketDataService().parseFrankfurterSeries(payload)
        assertEquals(listOf(1.1681, 1.1551, 1.1539), closes)
    }

    @Test
    fun `coingecko ohlc parser extracts closes from candle arrays`() {
        val payload = """[[1788000000000,4200.5,4310.2,4190.1,4305.45],[1788086400000,4305.0,4320.0,4250.0,4299.9]]"""
        val candles = MarketDataService().parseCoinGeckoOhlc(payload)
        assertEquals(2, candles.size)
        // [ts, o, h, l, close] — the close is the last element.
        assertEquals(4305.45, candles[0][4], 1e-9)
        assertEquals(4299.9, candles[1][4], 1e-9)
    }

    @Test
    fun `rate extractors read exchangerate-api and nobitex shapes`() {
        val erApi = """{"result":"success","rates":{"EUR":0.865688,"IRR":1466919.759851}}"""
        assertEquals(1466919.759851, MarketDataService().number(erApi, "IRR")!!, 1e-6)
        val nobitex = """{"status":"ok","stats":{"usdt-rls":{"latest":1495000,"dayChange":-0.2}}}"""
        assertEquals(1495000.0, MarketDataService().number(nobitex, "latest")!!, 1e-6)
        assertNull(MarketDataService().number(erApi, "missing"))
    }

    @Test
    fun `scenario ladder anchors on live structure`() {
        val scenarios = LitStrategyEngine.scenariosFor(
            price = 2000.0, trendUp = true, swingHigh = 2050.0, swingLow = 1950.0
        )
        assertEquals(3, scenarios.size)
        assertEquals(2050.0, scenarios[0].trigger, 1e-9)
        assertEquals(2000.0, scenarios[1].trigger, 1e-9)
        assertEquals(1950.0, scenarios[2].trigger, 1e-9)
    }
}
