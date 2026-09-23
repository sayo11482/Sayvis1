package com.odin.agent.trading.pro

import com.odin.agent.models.QuantStrategyType
import java.util.Calendar
import java.util.TimeZone

/**
 * ODIN PRO v1.0.28 - Trading Session & Liquidity Filter
 * فیلتر سشن معاملاتی و تداخل لندن-نیویورک (London/New York Overlap)
 * بیش از ۶۰٪ حجم روزانه در تداخل لندن-نیویورک انجام می‌شود
 */

enum class MarketSession(val labelFa: String, val startUtcHour: Int, val endUtcHour: Int) {
    ASIAN("سشن آسیا و توکیو", 0, 8),
    LONDON("سشن لندن و اروپا", 7, 16),
    NEW_YORK("سشن نیویورک و آمریکا", 12, 21),
    LONDON_NY_OVERLAP("تداخل طلایی لندن و نیویورک", 12, 16); // بالاترین نقدینگی مارکت
}

data class SessionEvaluation(
    val activeSessions: List<MarketSession>,
    val isOverlapActive: Boolean,
    val confidenceModifier: Double, // ضریب اثر روی کانفیدنس سیگنال (-15% تا +25%)
    val isStrategyRecommended: Boolean,
    val messageFa: String,
    val tehranTimeStr: String,
    val utcTimeStr: String
)

class TradingSessionFilter {

    /**
     * ارزیابی سشن فعلی و تطابق آن با استراتژی انتخابی
     */
    fun evaluate(
        strategy: QuantStrategyType,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): SessionEvaluation {
        val calUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = currentTimeMillis }
        val calTehran = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tehran")).apply { timeInMillis = currentTimeMillis }

        val utcHour = calUtc.get(Calendar.HOUR_OF_DAY)
        val utcMin = calUtc.get(Calendar.MINUTE)
        val tehranHour = calTehran.get(Calendar.HOUR_OF_DAY)
        val tehranMin = calTehran.get(Calendar.MINUTE)

        val active = mutableListOf<MarketSession>()
        if (utcHour in 0..8) active.add(MarketSession.ASIAN)
        if (utcHour in 7..16) active.add(MarketSession.LONDON)
        if (utcHour in 12..21) active.add(MarketSession.NEW_YORK)
        if (utcHour in 12..16) active.add(MarketSession.LONDON_NY_OVERLAP)

        val isOverlap = active.contains(MarketSession.LONDON_NY_OVERLAP)

        // ارزیابی تطابق استراتژی
        val (recommended, modifier, reasonFa) = when (strategy) {
            QuantStrategyType.LIT_LIQUIDITY_INVERSION, QuantStrategyType.MOMENTUM_BREAKOUT, QuantStrategyType.TV_80_PERCENT -> {
                if (isOverlap) {
                    Triple(true, 1.25, "تداخل طلایی لندن-نیویورک فعال است: حجم و نقدینگی اسمارت مانی در اوج (+۲۵٪ اعتبار)")
                } else if (active.contains(MarketSession.LONDON) || active.contains(MarketSession.NEW_YORK)) {
                    Triple(true, 1.10, "سشن با نقدینگی بالا فعال است (+۱۰٪ اعتبار)")
                } else {
                    Triple(false, 0.75, "سشن آسیا (کم‌حجم): استراتژی بریک‌اوت و نقدینگی توصیه نمی‌شود (-۲۵٪ اعتبار)")
                }
            }
            QuantStrategyType.MEAN_REVERSION, QuantStrategyType.PAIRS_TRADING -> {
                if (active.contains(MarketSession.ASIAN) && !isOverlap) {
                    Triple(true, 1.20, "سشن رِنج آسیا: استراتژی بازگشت به میانگین در بهترین شرایط (+۲۰٪ اعتبار)")
                } else if (isOverlap) {
                    Triple(false, 0.80, "تداخل لندن-نیویورک نوسان شدید دارد: احتمال شکست سقف/کف و استاپ رِنج (-۲۰٪ اعتبار)")
                } else {
                    Triple(true, 1.0, "سشن نرمال معاملاتی")
                }
            }
            QuantStrategyType.TREND_FOLLOWING, QuantStrategyType.VOLATILITY_REGIME -> {
                if (isOverlap || active.contains(MarketSession.LONDON)) {
                    Triple(true, 1.15, "مومنتوم روندی لندن-نیویورک مطلوب (+۱۵٪ اعتبار)")
                } else {
                    Triple(true, 0.90, "حجم روند ضعیف")
                }
            }
        }

        val tehranStr = "%02d:%02d تهران".format(tehranHour, tehranMin)
        val utcStr = "%02d:%02d UTC".format(utcHour, utcMin)

        return SessionEvaluation(
            activeSessions = active,
            isOverlapActive = isOverlap,
            confidenceModifier = modifier,
            isStrategyRecommended = recommended,
            messageFa = "$reasonFa | ساعت: $tehranStr ($utcStr)",
            tehranTimeStr = tehranStr,
            utcTimeStr = utcStr
        )
    }
}
