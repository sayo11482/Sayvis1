package com.odin.agent.trading.pro

import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

/**
 * ODIN PRO v1.0.28 - ML Confidence Calibrator (Platt Scaling)
 * کالیبراسیون یادگیری ماشین برای تخمین احتمال برد واقعی
 * تبدیل کانفیدنس خام اندیکاتورها به احتمال واقعی برد جهت جلوگیری از خطای سایزینگ پوزیشن
 */

data class CalibrationOutput(
    val rawScore: Double,          // 0 to 100
    val calibratedWinProbability: Double, // 0 to 100%
    val confidenceTierFa: String,
    val recommendedRiskWeight: Double // 0.5x to 1.25x
)

class ConfidenceCalibrator(
    private val paramA: Double = -0.075,
    private val paramB: Double = 3.8
) {
    /**
     * کالیبره کردن امتیاز خام با تابع سیگموئید پلات (Platt Sigmoid Calibration)
     */
    fun calibrate(rawScore: Double): CalibrationOutput {
        val score = max(10.0, min(99.0, rawScore))
        // P(y=1 | f) = 1 / (1 + exp(A * f + B))
        val z = paramA * score + paramB
        val prob = 1.0 / (1.0 + exp(z))
        val probPercent = round2(prob * 100.0)

        val (tierFa, weight) = when {
            probPercent >= 80.0 -> Pair("بسیار بالا (کانفیدنس طلایی)", 1.20)
            probPercent >= 68.0 -> Pair("بالا (سیگنال استاندارد)", 1.00)
            probPercent >= 55.0 -> Pair("متوسط (نیازمند احتیاط)", 0.75)
            else -> Pair("ضعیف (حجم باید نصف شود)", 0.50)
        }

        return CalibrationOutput(
            rawScore = round2(rawScore),
            calibratedWinProbability = probPercent,
            confidenceTierFa = tierFa,
            recommendedRiskWeight = weight
        )
    }

    private fun round2(v: Double): Double = kotlin.math.round(v * 100.0) / 100.0
}
