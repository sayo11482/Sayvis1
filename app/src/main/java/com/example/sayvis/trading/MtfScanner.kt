package com.example.sayvis.trading

/**
 * Multi-timeframe entry scanner (v5.0.0).
 *
 * The owner asked the market chart to be examined across several timeframes
 * hunting for an entry point. The engine:
 *
 *  1. runs the LIT analysis on each timeframe's closes (M15 / H1 / H4 / D1);
 *  2. combines the per-TF verdicts with a confluence score — an entry is
 *     only proposed when at least two timeframes agree and none of the
 *     higher timeframes opposes it;
 *  3. returns the concrete LIT plan (entry / stop / 3R–4.5R–6R targets,
 *     the hard RR≥1:3 floor is inside LitStrategyEngine.safe) with a
 *     grade A/B/C so the UI can rank entries.
 *
 * The combination rules are pure and unit-tested; the network fetching of
 * the timeframe series lives in [MarketDataService].
 */
object MtfScanner {

    enum class Tf(val labelFa: String, val labelEn: String, val minutes: Int) {
        M15("۱۵دقیقه", "15m", 15),
        H1("۱ساعته", "1h", 60),
        H4("۴ساعته", "4h", 240),
        D1("روزانه", "1d", 1440)
    }

    data class TfVerdict(
        val tf: Tf,
        val side: LitStrategyEngine.Side,
        val rsi: Double,
        val atr: Double,
        val bars: Int
    )

    enum class Grade { A, B, C, NONE }

    data class Decision(
        val side: LitStrategyEngine.Side,
        val grade: Grade,
        val agreeing: List<Tf>,
        val opposing: List<Tf>,
        val checked: List<Tf>
    ) {
        val isEntry: Boolean get() = side != LitStrategyEngine.Side.WAIT

        fun headline(persian: Boolean): String = when {
            !isEntry -> if (persian) "بدون ورود — تایم‌فریم‌ها هم‌نظر نیستند" else "No entry — timeframes disagree"
            grade == Grade.A -> if (persian) "ورود قوی (همگرایی کامل)" else "Strong entry (full confluence)"
            grade == Grade.B -> if (persian) "ورود معتبر (همگرایی حداقلی)" else "Valid entry (minimal confluence)"
            else -> if (persian) "ورود ضعیف — فقط با مدیریت ریسک" else "Weak entry — risk-managed only"
        }
    }

    /**
     * Pure confluence rules:
     *  - count non-WAIT verdicts per side;
     *  - LONG needs ≥2 LONGs and no D1/H4 SHORT; SHORT mirrored;
     *  - grade A: ≥3 agreeing and zero opposing; B: exactly 2 agreeing, zero opposing;
     *    C: agreeing with one opposing below the H4 layer; otherwise WAIT.
     */
    fun combine(verdicts: List<TfVerdict>): Decision {
        if (verdicts.isEmpty()) {
            return Decision(LitStrategyEngine.Side.WAIT, Grade.NONE, emptyList(), emptyList(), emptyList())
        }
        val checked = verdicts.map { it.tf }
        val longs = verdicts.filter { it.side == LitStrategyEngine.Side.LONG }
        val shorts = verdicts.filter { it.side == LitStrategyEngine.Side.SHORT }
        val side = when {
            longs.size >= 2 && longs.size >= shorts.size -> LitStrategyEngine.Side.LONG
            shorts.size >= 2 && shorts.size > longs.size -> LitStrategyEngine.Side.SHORT
            else -> LitStrategyEngine.Side.WAIT
        }
        if (side == LitStrategyEngine.Side.WAIT) {
            return Decision(side, Grade.NONE, emptyList(), emptyList(), checked)
        }
        val agreeingTfs = verdicts.filter { it.side == side }.map { it.tf }
        val opposing = verdicts.filter {
            it.side != LitStrategyEngine.Side.WAIT && it.side != side
        }.map { it.tf }
        val heavyOpposition = opposing.any { it == Tf.H4 || it == Tf.D1 }
        if (heavyOpposition) {
            return Decision(LitStrategyEngine.Side.WAIT, Grade.NONE, agreeingTfs, opposing, checked)
        }
        val grade = when {
            agreeingTfs.size >= 3 && opposing.isEmpty() -> Grade.A
            agreeingTfs.size == 2 && opposing.isEmpty() -> Grade.B
            opposing.isNotEmpty() -> Grade.C
            else -> Grade.NONE
        }
        return if (grade == Grade.NONE) {
            Decision(LitStrategyEngine.Side.WAIT, Grade.NONE, agreeingTfs, opposing, checked)
        } else {
            Decision(side, grade, agreeingTfs, opposing, checked)
        }
    }

    /**
     * Picks the execution timeframe for the plan: the smallest agreed TF so
     * the entry is the freshest one the confluence supports.
     */
    fun executionTf(decision: Decision): Tf? =
        decision.agreeing.minByOrNull { it.minutes }
}
