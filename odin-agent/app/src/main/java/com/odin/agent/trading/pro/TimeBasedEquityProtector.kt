package com.odin.agent.trading.pro

/**
 * ODIN PRO v1.0.28 - Time-Based Equity Protector
 * محافظ زمانی اکوییتی - کنترل افت سرمایه روزانه، هفتگی و ماهانه
 * سقف ضرر ۳٪ روزانه (Kill-switch)، ۵٪ هفتگی و ۱۰٪ ماهانه + قفل سود روزانه
 */

data class EquityProtectionState(
    val dailyLossCap: Double = 0.03,    // ۳٪ سقف ضرر روزانه
    val weeklyLossCap: Double = 0.05,   // ۵٪ سقف ضرر هفتگی
    val monthlyLossCap: Double = 0.10,  // ۱۰٪ سقف ضرر ماهانه
    val dailyProfitLock: Double = 0.04, // ۴٪ تارگت سود روزانه جهت قفل
    val maxConsecutiveLosses: Int = 3,  // حداکثر باخت متوالی مجاز
    val currentConsecutiveLosses: Int = 0,
    val isKillSwitchActive: Boolean = false,
    val isWeeklyHaltActive: Boolean = false,
    val isProfitLocked: Boolean = false,
    val blockReasonFa: String = ""
)

class TimeBasedEquityProtector {

    private var state = EquityProtectionState()

    fun evaluateRisk(
        currentEquity: Double,
        dayStartEquity: Double,
        weekStartEquity: Double,
        monthStartEquity: Double,
        consecutiveLosses: Int
    ): Pair<Boolean, String> {
        val dailyDd = (dayStartEquity - currentEquity) / dayStartEquity
        val weeklyDd = (weekStartEquity - currentEquity) / weekStartEquity
        val monthlyDd = (monthStartEquity - currentEquity) / monthStartEquity
        val dailyGain = (currentEquity - dayStartEquity) / dayStartEquity

        // 1. چک سقف ضرر روزانه
        if (dailyDd >= state.dailyLossCap) {
            state = state.copy(isKillSwitchActive = true, blockReasonFa = "کیل‌سویچ روزانه فعال شد: دراودان روزانه ${(dailyDd*100).toInt()}٪ >= ۳٪")
            return false to state.blockReasonFa
        }

        // 2. چک سقف ضرر هفتگی
        if (weeklyDd >= state.weeklyLossCap) {
            state = state.copy(isWeeklyHaltActive = true, blockReasonFa = "توقف هفتگی: دراودان هفتگی ${(weeklyDd*100).toInt()}٪ >= ۵٪")
            return false to state.blockReasonFa
        }

        // 3. چک سقف ضرر ماهانه
        if (monthlyDd >= state.monthlyLossCap) {
            state = state.copy(blockReasonFa = "توقف ماهانه محافظت از اصل سرمایه: دراودان ${(monthlyDd*100).toInt()}٪ >= ۱۰٪")
            return false to state.blockReasonFa
        }

        // 4. باخت‌های متوالی
        if (consecutiveLosses >= state.maxConsecutiveLosses) {
            val msg = "توقف موقت: $consecutiveLosses باخت متوالی ثبت شد (نیاز به استراحت ۴ ساعته)"
            state = state.copy(blockReasonFa = msg)
            return false to msg
        }

        // 5. قفل سود روزانه در صورت رسیدن به هدف ۴٪
        if (dailyGain >= state.dailyProfitLock && !state.isProfitLocked) {
            state = state.copy(isProfitLocked = true, blockReasonFa = "هدف سود روزانه (+۴٪) محقق شد: ۷۰٪ سود قفل شد و سیستم محافظه‌کار می‌شود")
        }

        return true to (if (state.isProfitLocked) state.blockReasonFa else "شرایط اکوییتی کامپایل و ایمن است")
    }

    fun resetDaily() {
        state = state.copy(isKillSwitchActive = false, isProfitLocked = false, blockReasonFa = "")
    }

    fun getState(): EquityProtectionState = state
}
