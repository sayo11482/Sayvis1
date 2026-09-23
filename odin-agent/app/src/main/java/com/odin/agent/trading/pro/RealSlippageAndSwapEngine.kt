package com.odin.agent.trading.pro

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.max

/**
 * ODIN PRO v1.0.28 - Real Slippage & Swap Engine
 * مدل اسلیپیج واقعی بازار و نرخ‌های سواپ شبانه بروکر ویتاورس
 * محاسبه دقیق هزینه‌های پنهان اجرای معامله برای بک‌تست و معاملات زنده
 */

data class ExecutionCostDetails(
    val basePrice: Double,
    val executedPrice: Double,
    val slippagePips: Double,
    val slippageCostUsd: Double,
    val dailySwapLongUsd: Double,
    val dailySwapShortUsd: Double,
    val isTripleSwapNight: Boolean,
    val messageFa: String
)

class RealSlippageAndSwapEngine {

    /**
     * محاسبه اسلیپیج داینامیک بر اساس نوسان و حجم لات
     */
    fun calculateSlippage(
        symbol: String,
        isBuy: Boolean,
        basePrice: Double,
        lots: Double,
        volatilityAtrPips: Double,
        isHighNewsActive: Boolean
    ): Double {
        val pipSize = if (symbol.contains("JPY")) 0.01 else 0.0001

        // اسلیپیج پایه در بروکر ECN ویتاورس حدود ۰.۲ پیپ
        var pips = 0.2
        if (volatilityAtrPips > 15.0) pips += 0.3
        if (lots > 1.0) pips += (lots - 1.0) * 0.15
        if (isHighNewsActive) pips *= 3.5 // در زمان خبر اسلیپیج به شدت جهش می‌کند

        val effectivePips = round2(max(0.1, pips))
        return if (isBuy) basePrice + (effectivePips * pipSize) else basePrice - (effectivePips * pipSize)
    }

    /**
     * جدول سواپ شبانه ویتاورس (Vittaverse Overnight Swap Rates)
     * چهارشنبه شب‌ها سواپ ۳ برابری (Triple Swap) برای تسویه شنبه و یکشنبه محاسبه می‌شود
     */
    fun calculateSwap(
        symbol: String,
        lots: Double,
        isBuy: Boolean,
        daysHeld: Int = 1,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): Double {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = currentTimeMillis }
        val isWednesday = cal.get(Calendar.DAY_OF_WEEK) == Calendar.WEDNESDAY

        // سواپ دلاری هر لات در ویتاورس
        val (longSwap, shortSwap) = when {
            symbol.startsWith("EURUSD") -> Pair(-4.50, +1.20)
            symbol.startsWith("GBPUSD") -> Pair(-5.20, +0.80)
            symbol.startsWith("XAUUSD") -> Pair(-18.50, +9.00)
            symbol.startsWith("BTCUSD") -> Pair(-25.00, -15.00)
            else -> Pair(-3.00, 0.0)
        }

        val baseSwap = if (isBuy) longSwap else shortSwap
        val multiplier = if (isWednesday) 3 else 1
        return round2(baseSwap * lots * daysHeld * multiplier)
    }

    private fun round2(v: Double): Double = kotlin.math.round(v * 100.0) / 100.0
}
