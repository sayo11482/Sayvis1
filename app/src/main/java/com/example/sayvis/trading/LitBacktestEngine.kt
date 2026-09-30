package com.example.sayvis.trading

import kotlin.math.max
import kotlin.math.min

/**
 * LIT backtester — the “۱۰ الی ۵۶ بار” honesty check the owner demanded.
 *
 * Takes a close series and slides the LIT engine over it, collecting every
 * *tradeable* plan (side != WAIT, RR ≥ 3) as a hypothetical trade that is
 * settled against the *next* bars. No look-ahead: the plan at bar `i` is
 * judged only by bars `i+1 … i+20`.
 *
 * Vitaverse day-spread is applied to the entry (via [VitaverseSpreadProvider])
 * so the win rate is the *honest* one, not the raw-plan illusion.
 *
 * The engine returns a [Result] with win rate, profit factor and a Persian
 * summary — the ViewModel shows it on the chart card and the strategy is
 * only beeped when the backtest is *convincing* (see [isConvincing]).
 */
object LitBacktestEngine {

    data class Trade(
        val index: Int,
        val side: LitStrategyEngine.Side,
        val entry: Double,
        val stop: Double,
        val targets: List<LitStrategyEngine.Target>,
        val rr: Double,
        val won: Boolean?, // null = not settled within 20 bars
        val exitPrice: Double?,
        val barsToExit: Int?
    )

    data class Result(
        val symbol: MarketDataService.Symbol,
        val tuning: LitStrategyEngine.Tuning,
        val spreadPrice: Double,
        val totalTrades: Int,
        val settledTrades: Int,
        val wins: Int,
        val losses: Int,
        val winRate: Double, // 0..1, settled only
        val avgWinR: Double,
        val avgLossR: Double,
        val profitFactor: Double, // gross wins / gross losses
        val maxConsecWins: Int,
        val maxConsecLosses: Int,
        val trades: List<Trade>,
        val verdictFa: String,
        val verdictEn: String
    ) {
        /** Convincing = enough samples (≥10) and either winRate≥55% with PF≥1.4 or winRate≥45% with PF≥1.8. */
        val isConvincing: Boolean
            get() = settledTrades in 10..56 && (
                (winRate >= 0.55 && profitFactor >= 1.4) ||
                    (winRate >= 0.45 && profitFactor >= 1.8) ||
                    (winRate >= 0.60)
                )

        /** Short Persian summary for the chart footer. */
        fun summaryFa(): String = buildString {
            append("بک‌تست LIT (${settledTrades} معامله): ")
            append("برد ${"%.0f".format(winRate * 100)}٪")
            append(" · PF ${"%.2f".format(profitFactor)}")
            append(" · RR ${"%.1f".format(avgWinR)}:۱")
            if (isConvincing) append(" ✅ قابلِ پیش‌بینی") else append(" ⚠️ احتیاط")
        }

        fun summaryEn(): String = buildString {
            append("LIT backtest (${settledTrades} trades): ")
            append("${"%.0f".format(winRate * 100)}% win")
            append(" · PF ${"%.2f".format(profitFactor)}")
            append(" · RR ${"%.1f".format(avgWinR)}:1")
            if (isConvincing) append(" ✅ convincing") else append(" ⚠️ weak")
        }
    }

    /**
     * Backtests [closes] (earliest → latest) with [tuning] and the Vitaverse
     * day-spread for [symbol]. Slides with step 1, collects up to 56 trades
     * or until the series ends — whichever is smaller. Enforces 10-trade
     * minimum for a verdict; fewer than 10 → unconving result.
     */
    fun backtest(
        closes: List<Double>,
        symbol: MarketDataService.Symbol,
        tuning: LitStrategyEngine.Tuning = LitStrategyEngine.Tuning(),
        minTrades: Int = 10,
        maxTrades: Int = 56
    ): Result {
        require(minTrades in 3..20) { "minTrades $minTrades" }
        require(maxTrades in minTrades..80) { "maxTrades $maxTrades" }
        if (closes.size < 60) {
            return emptyResult(symbol, tuning, 0.0, "داده کم (<۶۰ کندل) — بک‌تست نامعتبر", "too few candles (<60) — backtest invalid")
        }

        val trades = ArrayList<Trade>()
        val maxStart = closes.size - 20 // need 20 for settlement; analyse() itself needs 30 but we start at 60 for EMA50

        var i = 60
        while (i <= maxStart && trades.size < maxTrades) {
            val window = closes.subList(0, i) // 0..i-1 unseen future
            val analysis = LitStrategyEngine.analyse(window, tuning) ?: run { i++; continue }
            val plan = analysis.plan
            if (plan.side == LitStrategyEngine.Side.WAIT) { i++; continue }

            val spread = VitaverseSpreadProvider.spreadPrice(symbol, window.last(), analysis.view.atr14)
            val adjPlan = VitaverseSpreadProvider.applySpreadToPlan(plan, symbol, window.last(), analysis.view.atr14)

            // Settle against the next 20 closes
            val future = closes.subList(i, min(closes.size, i + 20))
            val (won, exit, bars) = settle(adjPlan, future)

            trades.add(
                Trade(
                    index = i,
                    side = adjPlan.side,
                    entry = adjPlan.entry,
                    stop = adjPlan.stop,
                    targets = adjPlan.targets,
                    rr = adjPlan.rr,
                    won = won,
                    exitPrice = exit,
                    barsToExit = bars
                )
            )
            // Non-overlapping: jump 5 bars so trades are independent
            i += 5
        }

        val settled = trades.filter { it.won != null }
        if (settled.size < minTrades) {
            return Result(
                symbol = symbol, tuning = tuning, spreadPrice = 0.0,
                totalTrades = trades.size, settledTrades = settled.size,
                wins = 0, losses = 0, winRate = 0.0, avgWinR = 0.0, avgLossR = 0.0,
                profitFactor = 0.0, maxConsecWins = 0, maxConsecLosses = 0,
                trades = trades,
                verdictFa = "بک‌تست ناکافی (${settled.size} معامله) — حداقل $minTrades لازم است",
                verdictEn = "insufficient backtest (${settled.size} trades) — need $minTrades"
            )
        }

        val wins = settled.count { it.won == true }
        val losses = settled.size - wins
        val winRate = if (settled.isNotEmpty()) wins.toDouble() / settled.size else 0.0

        // Average R: win = target1 RR, loss = -1R (stop = 1R)
        val avgWinR = settled.filter { it.won == true }.map { it.rr }.average().let { if (it.isNaN()) 0.0 else it }
        val avgLossR = 1.0 // by construction stop = 1R
        val grossWin = wins * avgWinR
        val grossLoss = losses * avgLossR
        val pf = if (grossLoss > 0) grossWin / grossLoss else if (grossWin > 0) 99.0 else 0.0

        var maxW = 0; var maxL = 0; var curW = 0; var curL = 0
        for (t in settled) {
            if (t.won == true) { curW++; curL = 0; maxW = max(maxW, curW) } else { curL++; curW = 0; maxL = max(maxL, curL) }
        }

        val verdictFa = when {
            settled.size in 10..56 && winRate >= 0.55 && pf >= 1.4 ->
                "✅ قابلِ پیش‌بینی — ${settled.size} معامله، برد ${"%.0f".format(winRate*100)}٪، PF ${"%.2f".format(pf)}"
            settled.size in 10..56 && winRate >= 0.45 && pf >= 1.5 ->
                "✅ قابلِ قبول — PF ${"%.2f".format(pf)} با وجود برد متوسط"
            else ->
                "⚠️ احتیاط — ${settled.size} معامله، برد ${"%.0f".format(winRate*100)}٪، PF ${"%.2f".format(pf)} — ورود با احتیاط"
        }
        val verdictEn = when {
            settled.size in 10..56 && winRate >= 0.55 && pf >= 1.4 ->
                "convincing — ${settled.size} trades, ${"%.0f".format(winRate*100)}% win, PF ${"%.2f".format(pf)}"
            settled.size in 10..56 && winRate >= 0.45 && pf >= 1.5 ->
                "acceptable — PF ${"%.2f".format(pf)} despite middling win rate"
            else ->
                "weak — ${settled.size} trades, ${"%.0f".format(winRate*100)}% win, PF ${"%.2f".format(pf)} — enter cautiously"
        }

        val spread = VitaverseSpreadProvider.spreadPrice(symbol, closes.last(), null)
        return Result(
            symbol = symbol, tuning = tuning, spreadPrice = spread,
            totalTrades = trades.size, settledTrades = settled.size,
            wins = wins, losses = losses, winRate = winRate,
            avgWinR = avgWinR, avgLossR = avgLossR, profitFactor = pf,
            maxConsecWins = maxW, maxConsecLosses = maxL,
            trades = trades, verdictFa = verdictFa, verdictEn = verdictEn
        )
    }

    private fun settle(plan: LitStrategyEngine.TradePlan, futureCloses: List<Double>): Triple<Boolean?, Double?, Int?> {
        if (futureCloses.isEmpty()) return Triple(null, null, null)
        val tp1 = plan.targets.firstOrNull()?.price
        val isLong = plan.side == LitStrategyEngine.Side.LONG
        for ((idx, price) in futureCloses.withIndex()) {
            val hitStop = if (isLong) price <= plan.stop else price >= plan.stop
            val hitTp = tp1?.let { if (isLong) price >= it else price <= it } ?: false
            if (hitStop && hitTp) {
                // Both on same bar — conservative: stop first
                return Triple(false, plan.stop, idx + 1)
            }
            if (hitStop) return Triple(false, plan.stop, idx + 1)
            if (hitTp) return Triple(true, tp1, idx + 1)
        }
        return Triple(null, null, null) // not settled within 20 bars
    }

    private fun emptyResult(symbol: MarketDataService.Symbol, tuning: LitStrategyEngine.Tuning, spread: Double, fa: String, en: String) =
        Result(symbol, tuning, spread, 0, 0, 0, 0, 0.0, 0.0, 0.0, 0.0, 0, 0, emptyList(), fa, en)
}
