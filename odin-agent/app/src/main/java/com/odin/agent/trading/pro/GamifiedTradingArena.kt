package com.odin.agent.trading.pro

import com.odin.agent.trading.JalaliCalendar
import com.odin.agent.trading.OdinTokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * ODIN PRO v1.0.28 - Gamified Trading Arena & Monetization Engine
 * ماژول بازی معاملاتی، مسابقات تورنمنتی و کسب درآمد پایدار برای مالک اپلیکیشن (سوینکس)
 * الگوبرداری‌شده از غول‌های فین‌تک (Binance Launchpool، Robinhood، Telegram Gamified Finance)
 */

data class PredictionGame(
    val id: String,
    val symbol: String, // BTCUSD, XAUUSD
    val targetTimeUtc: String,
    val entryOdn: Double = 10.0,
    val totalUpPoolOdn: Double = 1200.0,
    val totalDownPoolOdn: Double = 980.0,
    val swinexHouseRakeRate: Double = 0.15, // ۱۵٪ سهم سود مستقیم سوینکس
    val status: String = "ACTIVE"
)

data class TournamentBattle(
    val id: String,
    val titleFa: String,
    val entryFeeOdn: Double,
    val participantsCount: Int,
    val totalPrizePoolOdn: Double,
    val swinexRakeOdn: Double,     // سهم سود خالص سوینکس از مسابقه (۲۰٪)
    val winnersPoolOdn: Double,    // جوایز کاربران برتر (۸۰٪)
    val endsInHours: Int,
    val isJoined: Boolean = false
)

data class QuizQuestion(
    val id: Int,
    val questionFa: String,
    val optionsFa: List<String>,
    val correctIndex: Int,
    val rewardOdn: Double = 5.0
)

data class ArenaState(
    val activeGames: List<PredictionGame> = emptyList(),
    val tournaments: List<TournamentBattle> = emptyList(),
    val totalSwinexGameRevenueUsd: Double = 345.0, // مجموع سود دلاری سوینکس از بازی‌ها
    val userGameOdnEarned: Double = 0.0,
    val dailyStreakDays: Int = 4,
    val lastActionFa: String = "آماده شرکت در چالش‌های کوانت"
)

class GamifiedTradingArena(
    private val tokenManager: OdinTokenManager
) {
    private val _state = MutableStateFlow(initialArenaState())
    val state: StateFlow<ArenaState> = _state

    private fun initialArenaState(): ArenaState {
        return ArenaState(
            activeGames = listOf(
                PredictionGame("PRED-BTC-1", "BTCUSD", "20:00 UTC", 10.0, 1450.0, 1120.0),
                PredictionGame("PRED-GOLD-1", "XAUUSD", "21:00 UTC", 10.0, 890.0, 940.0)
            ),
            tournaments = listOf(
                TournamentBattle(
                    id = "TOURN-WEEKLY-1",
                    titleFa = "لیگ هفتگی قهرمانان کوانت سوینکس (EURUSD & Gold)",
                    entryFeeOdn = 50.0,
                    participantsCount = 142,
                    totalPrizePoolOdn = 7100.0,
                    swinexRakeOdn = 1420.0, // ۲۰٪ سود سوینکس = ۱۴۲ دلار
                    winnersPoolOdn = 5680.0,
                    endsInHours = 48
                ),
                TournamentBattle(
                    id = "TOURN-FLASH-1",
                    titleFa = "کاپ بریک‌اوت سریع سشن نیویورک",
                    entryFeeOdn = 20.0,
                    participantsCount = 85,
                    totalPrizePoolOdn = 1700.0,
                    swinexRakeOdn = 340.0, // ۲۰٪ سود سوینکس = ۳۴ دلار
                    winnersPoolOdn = 1360.0,
                    endsInHours = 6
                )
            )
        )
    }

    /**
     * شرکت در مسابقه تورنمنت - کسر ۵۰ ODN، افزودن به استخر، کسر ۲۰٪ سود برای سوینکس
     */
    fun joinTournament(tournamentId: String): Result<String> {
        val s = _state.value
        val tour = s.tournaments.find { it.id == tournamentId }
            ?: return Result.failure(IllegalStateException("تورنمنت یافت نشد"))

        val userBal = tokenManager.state.value.wallet.balance
        if (userBal < tour.entryFeeOdn) {
            return Result.failure(IllegalStateException("موجودی ناکافی: به ${tour.entryFeeOdn} ODN نیاز دارید"))
        }

        // کسر ورودی و واریز سهم Rake به خزانه سوینکس
        tokenManager.stake(tour.entryFeeOdn) // قفل در استخر
        val swinexRevenue = tour.entryFeeOdn * 0.20 * OdinTokenManager.TOKEN_PRICE_USD

        _state.value = s.copy(
            totalSwinexGameRevenueUsd = s.totalSwinexGameRevenueUsd + swinexRevenue,
            lastActionFa = "شما با موفقیت در '${tour.titleFa}' ثبت‌نام کردید. سهم استخر منظور شد."
        )

        return Result.success("ثبت‌نام با موفقیت انجام شد. ۲۰٪ سهم پلتفرم به خزانه واریز گردید.")
    }

    /**
     * ثبت پیش‌بینی صعود یا نزول در بازی روزانه
     */
    fun placePrediction(gameId: String, isBullish: Boolean): Result<String> {
        val userBal = tokenManager.state.value.wallet.balance
        if (userBal < 10.0) {
            return Result.failure(IllegalStateException("برای شرکت در پیش‌بینی به حداقل ۱۰ ODN نیاز است"))
        }

        val s = _state.value
        val houseProfitUsd = 10.0 * 0.15 * OdinTokenManager.TOKEN_PRICE_USD // 15% Rake = $0.15

        _state.value = s.copy(
            totalSwinexGameRevenueUsd = s.totalSwinexGameRevenueUsd + houseProfitUsd,
            lastActionFa = "پیش‌بینی شما (${if (isBullish) "صعودی 🟢" else "نزولی 🔴"}) ثبت شد."
        )

        return Result.success("پیش‌بینی ثبت شد. ۱۵٪ کارمزد بازی به خزانه سوینکس منظور گردید.")
    }

    /**
     * سوالات آموزشی Learn-to-Earn با پاداش خرد
     */
    fun getDailyQuizzes(): List<QuizQuestion> = listOf(
        QuizQuestion(1, "شاخص RSI بالای عدد ۷۰ معمولاً بیانگر چه وضعیتی است؟", listOf("اشباع خرید (Overbought)", "اشباع فروش", "روند خنثی", "افزایش حجم"), 0),
        QuizQuestion(2, "تداخل طلایی سشن‌های معاملاتی لندن و نیویورک چه مزیتی دارد؟", listOf("بیشترین نقدینگی و اسلیپیج کم", "بسته بودن بازار", "کاهش حرکات قیمتی", "اسپرد بالا"), 0),
        QuizQuestion(3, "در صورت سود ۱۰ دلاری در Odin.trade، چند دلار به حساب سازنده (سوینکس) می‌رود؟", listOf("۲ دلار (۲۰٪)", "۵ دلار", "صفر", "۱ دلار"), 0)
    )
}
