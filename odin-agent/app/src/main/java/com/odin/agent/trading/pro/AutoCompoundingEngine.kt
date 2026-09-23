package com.odin.agent.trading.pro

import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

/**
 * ODIN PRO v1.0.28 - Auto-Compounding Money Management Engine
 * مدیریت سرمایه با سود مرکب خودکار - قوی‌ترین موتور رشد بالانس در بازارهای مالی
 * محاسبه حجم لات داینامیک بر اساس اکوییتی لحظه‌ای، دراودان و فاصله استاپ
 */

data class CompoundingConfig(
    val baseRiskPercentage: Double = 0.01,  // ریسک پیش‌فرض ۱٪ از اکوییتی
    val maxRiskPercentage: Double = 0.02,   // سقف ریسک ۲٪
    val minLotSize: Double = 0.01,          // حداقل لات
    val maxLotSize: Double = 10.0,          // حداکثر لات مجاز
    val enabled: Boolean = true
)

data class PositionSizeCalculation(
    val equity: Double,
    val riskAmountUsd: Double,
    val riskPercentageEffective: Double,
    val slDistancePips: Double,
    val recommendedLots: Double,
    val notionalUsd: Double,
    val drawdownAdjustment: String,
    val drawdownAdjustmentFa: String
)

class AutoCompoundingEngine(
    private val config: CompoundingConfig = CompoundingConfig()
) {
    /**
     * محاسبه حجم دقیق ورود بر اساس سرمایه مرکب و دراودان حساب
     */
    fun calculateLotSize(
        equity: Double,
        peakEquity: Double,
        slDistancePrice: Double,
        entryPrice: Double,
        pipSize: Double = 0.0001,
        lotSizeContract: Double = 100000.0
    ): PositionSizeCalculation {
        if (!config.enabled || equity <= 0.0) {
            return PositionSizeCalculation(equity, 10.0, 0.01, 20.0, config.minLotSize, 1000.0, "DISABLED", "مرکب غیرفعال")
        }

        // محاسبه دراودان لحظه‌ای اکوییتی
        val currentDd = if (peakEquity > 0.0 && equity < peakEquity) {
            (peakEquity - equity) / peakEquity
        } else 0.0

        // تعدیل خودکار ریسک در زمان دراودان (Drawdown Throttle)
        val (effectiveRisk, ddReasonEn, ddReasonFa) = when {
            currentDd >= 0.06 -> Triple(config.baseRiskPercentage * 0.25, "DD_THROTTLE_HEAVY", "کاهش ریسک به ۰.۲۵٪ به دلیل دراودان بالای ۶٪")
            currentDd >= 0.03 -> Triple(config.baseRiskPercentage * 0.50, "DD_THROTTLE_MODERATE", "کاهش ریسک به ۰.۵۰٪ به دلیل دراودان بالای ۳٪")
            currentDd >= 0.015 -> Triple(config.baseRiskPercentage * 0.75, "DD_THROTTLE_LIGHT", "کاهش ریسک به ۰.۷۵٪ به دلیل دراودان بالای ۱.۵٪")
            else -> Triple(config.baseRiskPercentage, "NORMAL_COMPOUND", "سود مرکب فعال - ریسک استاندارد ۱٪")
        }

        val riskAmountUsd = equity * effectiveRisk
        val slPips = max(5.0, slDistancePrice / pipSize)

        // فرمول استاندارد متاتریدر: لات = ریسک دلاری / (فاصله پیپ * ارزش هر پیپ در یک لات استاندارد)
        // برای جفت‌ارزهای با پایه USD ارزش هر پیپ در ۱ لات تقریباً ۱۰ دلار است
        val pipValuePerLot = (lotSizeContract * pipSize)
        val rawLots = riskAmountUsd / (slPips * pipValuePerLot)

        val boundedLots = max(config.minLotSize, min(config.maxLotSize, round(rawLots * 100.0) / 100.0))
        val notional = boundedLots * lotSizeContract

        return PositionSizeCalculation(
            equity = round2(equity),
            riskAmountUsd = round2(riskAmountUsd),
            riskPercentageEffective = round4(effectiveRisk),
            slDistancePips = round2(slPips),
            recommendedLots = boundedLots,
            notionalUsd = round2(notional),
            drawdownAdjustment = ddReasonEn,
            drawdownAdjustmentFa = ddReasonFa
        )
    }

    private fun round2(v: Double): Double = round(v * 100.0) / 100.0
    private fun round4(v: Double): Double = round(v * 10000.0) / 10000.0
}
