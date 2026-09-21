package com.odin.agent.risk

import com.odin.agent.models.QuantRiskStatus
import kotlin.math.abs
import kotlin.math.min

/**
 * ODIN QUANT - Risk Manager - Android version
 * Non-negotiable limits - The guardian of capital
 */

class RiskManager(
    var initialCapital: Double = 10000.0,
    var maxRiskPerTrade: Double = 0.01,  // 1%
    var maxDailyDd: Double = 0.03,       // 3%
    var maxTotalDd: Double = 0.15,       // 15%
    var maxOpenPositions: Int = 5
) {
    var currentCapital: Double = initialCapital
    var peakCapital: Double = initialCapital
    var dailyPnl: Double = 0.0
    var dailyStartCapital: Double = initialCapital
    var openPositions: Int = 0

    var killSwitchActive: Boolean = false
    var totalStopActive: Boolean = false

    fun canOpenPosition(symbol: String): Pair<Boolean, String> {
        if (totalStopActive) return false to "Total DD stop ${maxTotalDd*100}%"
        if (killSwitchActive) return false to "Kill-switch active ${maxDailyDd*100}% daily DD"

        val dailyDd = if (dailyPnl < 0) -dailyPnl / dailyStartCapital else 0.0
        if (dailyDd >= maxDailyDd) {
            killSwitchActive = true
            return false to "Daily DD ${dailyDd*100}% >= ${maxDailyDd*100}%"
        }

        val totalDd = (peakCapital - currentCapital) / peakCapital
        if (totalDd >= maxTotalDd) {
            totalStopActive = true
            return false to "Total DD ${totalDd*100}% >= ${maxTotalDd*100}%"
        }

        if (openPositions >= maxOpenPositions) {
            return false to "Max positions $maxOpenPositions reached"
        }

        return true to "OK"
    }

    fun calculatePositionSize(entry: Double, sl: Double, atr: Double? = null): Double {
        val riskAmount = currentCapital * maxRiskPerTrade
        var riskPerUnit = abs(entry - sl)
        if (riskPerUnit == 0.0) riskPerUnit = (atr ?: entry*0.01) * 2.0
        var size = riskAmount / riskPerUnit
        val maxSize = currentCapital * 0.2 / entry
        size = min(size, maxSize)
        return maxOf(size, 0.001)
    }

    fun getStatus(): QuantRiskStatus {
        val totalDd = (peakCapital - currentCapital) / peakCapital
        val dailyDd = if (dailyPnl < 0) -dailyPnl / dailyStartCapital else 0.0

        if (currentCapital > peakCapital) peakCapital = currentCapital

        return QuantRiskStatus(
            currentCapital = currentCapital,
            initialCapital = initialCapital,
            totalPnl = currentCapital - initialCapital,
            totalPnlPercent = (currentCapital - initialCapital) / initialCapital * 100,
            totalDrawdown = totalDd * 100,
            dailyPnl = dailyPnl,
            dailyDrawdown = dailyDd * 100,
            openPositions = openPositions,
            killSwitchActive = killSwitchActive,
            totalStopActive = totalStopActive,
            maxRiskPerTrade = maxRiskPerTrade*100,
            maxDailyDd = maxDailyDd*100,
            maxTotalDd = maxTotalDd*100
        )
    }

    fun resetDaily() {
        dailyPnl = 0.0
        dailyStartCapital = currentCapital
        killSwitchActive = false
    }
}
