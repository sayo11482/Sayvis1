package com.odin.agent.trading.pro

import com.odin.agent.models.SignalSide
import kotlin.math.abs

/**
 * ODIN PRO v1.0.28 - Partial Close & Scale In/Out Manager
 * بستن پله‌ای تارگت‌ها (50% در TP1، 30% در TP2 و 20% رانر) و ورود پله‌ای در پولبک
 * بهینه‌سازی نسبت سود به ریسک واقعی (Real Risk/Reward Maximizer)
 */

data class PartialCloseStage(
    val stage: Int,             // 1, 2, 3
    val targetR: Double,        // 1.5R, 2.5R, 3.5R
    val closeFraction: Double,  // 0.50, 0.30, 0.20
    val labelFa: String
)

data class PartialCloseEvent(
    val positionId: String,
    val stage: Int,
    val closedLots: Double,
    val remainingLots: Double,
    val realizedGrossProfit: Double,
    val price: Double,
    val messageFa: String,
    val timestamp: Long = System.currentTimeMillis()
)

class PartialCloseScalingManager {

    private val stages = listOf(
        PartialCloseStage(1, 1.5, 0.50, "سیو سود پله اول (۵۰٪) در تارگت ۱.۵R"),
        PartialCloseStage(2, 2.5, 0.30, "سیو سود پله دوم (۳۰٪) در تارگت ۲.۵R")
    )

    private val executedStages = mutableMapOf<String, MutableSet<Int>>()

    /**
     * بررسی آیا زمان بستن پله‌ای یک موقعیت فرا رسیده است
     */
    fun checkPartialClose(
        positionId: String,
        side: SignalSide,
        entryPrice: Double,
        currentPrice: Double,
        slPrice: Double,
        currentLots: Double,
        originalLots: Double
    ): PartialCloseEvent? {
        val initialRisk = abs(entryPrice - slPrice)
        if (initialRisk <= 0.0 || currentLots <= 0.001) return null

        val isBuy = side == SignalSide.BUY
        val floatingMove = if (isBuy) currentPrice - entryPrice else entryPrice - currentPrice
        val currentR = floatingMove / initialRisk

        val completed = executedStages.getOrPut(positionId) { mutableSetOf() }

        for (stage in stages) {
            if (!completed.contains(stage.stage) && currentR >= stage.targetR) {
                val lotsToClose = kotlin.math.max(0.01, kotlin.math.round(originalLots * stage.closeFraction * 100.0) / 100.0)
                val effectiveLotsToClose = kotlin.math.min(lotsToClose, currentLots)
                val remaining = kotlin.math.max(0.0, kotlin.math.round((currentLots - effectiveLotsToClose) * 100.0) / 100.0)

                completed.add(stage.stage)
                val realizedPnl = (floatingMove / entryPrice) * (effectiveLotsToClose * 100000.0)

                return PartialCloseEvent(
                    positionId = positionId,
                    stage = stage.stage,
                    closedLots = effectiveLotsToClose,
                    remainingLots = remaining,
                    realizedGrossProfit = kotlin.math.round(realizedPnl * 100.0) / 100.0,
                    price = currentPrice,
                    messageFa = "${stage.labelFa} - حجم بسته شده: $effectiveLotsToClose لات، باقیمانده: $remaining لات"
                )
            }
        }
        return null
    }

    fun reset(positionId: String) {
        executedStages.remove(positionId)
    }
}
