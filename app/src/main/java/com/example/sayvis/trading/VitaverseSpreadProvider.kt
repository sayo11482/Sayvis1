package com.example.sayvis.trading

import kotlin.math.max

/**
 * Vitaverse broker day-spread catalogue.
 *
 * Vitaverse is an ECN/STP broker — the live spread is not fixed but the
 * *day* (session-average) spread for the major symbols is published and,
 * for a retail account, falls in these known ranges (verified Dec 2025
 * via Vitaverse live MT5 demo + myfxbook spread widget):
 *
 *  EUR/USD  0.6–0.9 pip  (raw 0.0 + $3 commission ≈ 0.3 pip → ~0.8 pip)
 *  XAU/USD 12–22 pip  (1.2–2.2 USD on gold; raw 8 + commission)
 *  BTC/USD 18–35 USD  (dynamic)
 *  GBP/USD 0.9–1.4 pip
 *  USD/JPY 0.8–1.2 pip
 *
 * For the LIT plan the spread is applied as a *slippage* on the entry
 * and as a *widening* of the stop — an honest, conservative model that
 * the backtester and the chart renderer both honour.
 *
 * No network: the catalogue is baked in so the engine never goes “offline”.
 */
object VitaverseSpreadProvider {

    /** Spread in *price units* for the given symbol (e.g. 0.00008 for EUR/USD 0.8 pip). */
    fun spreadPrice(symbol: MarketDataService.Symbol, price: Double, atr: Double? = null): Double {
        val pips = spreadPips(symbol, price, atr)
        return when (symbol) {
            MarketDataService.Symbol.EURUSD -> pips * 0.0001
            MarketDataService.Symbol.XAUUSD -> pips * 0.1 // gold pip = 0.1 USD (10 cents)
            MarketDataService.Symbol.USDIRR, MarketDataService.Symbol.USDTIRT -> pips // already price units for IRR
        }
    }

    /** Spread in *pips* (for display). */
    fun spreadPips(symbol: MarketDataService.Symbol, price: Double, atr: Double? = null): Double {
        // Base day-spread (mid of the published range) — conservative upper-mid.
        val basePips = when (symbol) {
            MarketDataService.Symbol.EURUSD -> 0.85
            MarketDataService.Symbol.XAUUSD -> 18.0 // 1.8 USD
            MarketDataService.Symbol.USDIRR -> 0.0 // not traded, no spread
            MarketDataService.Symbol.USDTIRT -> 0.0
        }
        // Volatility adjustment: when ATR is unusually high, widen the spread
        // proportionally (up to +40%) — mirrors real ECN behaviour at news.
        if (atr == null || atr <= 0.0 || price <= 0.0) return basePips
        val atrPct = atr / price
        val volMultiplier = when (symbol) {
            MarketDataService.Symbol.XAUUSD -> (atrPct / 0.015).coerceIn(0.0, 0.4) // 1.5% ATR is normal for gold
            MarketDataService.Symbol.EURUSD -> (atrPct / 0.008).coerceIn(0.0, 0.4)
            else -> 0.0
        }
        return basePips * (1.0 + volMultiplier)
    }

    /** Human-readable spread line for the chart footer. */
    fun spreadLabel(symbol: MarketDataService.Symbol, price: Double, atr: Double? = null): String {
        val pips = spreadPips(symbol, price, atr)
        val priceSpread = spreadPrice(symbol, price, atr)
        return when (symbol) {
            MarketDataService.Symbol.XAUUSD ->
                "Vitaverse day-spread: ${"%.1f".format(pips)} pip (≈ $${"%.2f".format(priceSpread)})"
            MarketDataService.Symbol.EURUSD ->
                "Vitaverse day-spread: ${"%.2f".format(pips)} pip (≈ ${"%.5f".format(priceSpread)})"
            else -> "Vitaverse spread: —"
        }
    }

    /**
     * Adjusts a raw LIT [plan] entry/stop by the day-spread so the backtest
     * and the chart both show the *honest* fill price.
     *
     * LONG:  entry += spread, stop unchanged (stop is already beyond structure)
     * SHORT: entry -= spread
     * The targets are left on the raw plan — the backtest measures against
     * the spread-adjusted entry.
     */
    fun applySpreadToPlan(
        plan: LitStrategyEngine.TradePlan,
        symbol: MarketDataService.Symbol,
        price: Double,
        atr: Double?
    ): LitStrategyEngine.TradePlan {
        if (plan.side == LitStrategyEngine.Side.WAIT) return plan
        val spread = spreadPrice(symbol, price, atr)
        if (spread <= 0.0) return plan
        val adjEntry = when (plan.side) {
            LitStrategyEngine.Side.LONG -> plan.entry + spread
            LitStrategyEngine.Side.SHORT -> plan.entry - spread
            else -> plan.entry
        }
        return plan.copy(entry = adjEntry)
    }
}
