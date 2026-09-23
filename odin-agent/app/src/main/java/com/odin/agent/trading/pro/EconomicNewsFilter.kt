package com.odin.agent.trading.pro

import kotlin.math.abs

/**
 * ODIN PRO v1.0.28 - Economic News Filter & Blackout Engine
 * فیلتر اخبار سنگین اقتصادی - جلوگیری از ورود در لحظات NFP، CPI، FOMC و نرخ بهره
 * جلوگیری از قتل‌عام استاپ‌ها و باز شدن اسپرد بروکر در زمان اخبار قرمز
 */

enum class NewsImpact(val labelFa: String, val level: Int) {
    HIGH("قرمز - بحرانی", 3),
    MEDIUM("نارنجی - متوسط", 2),
    LOW("زرد - کم‌اثر", 1)
}

data class EconomicEvent(
    val id: String,
    val title: String,
    val titleFa: String,
    val currency: String,
    val impact: NewsImpact,
    val scheduledTimeMillis: Long,
    val descriptionFa: String
)

data class NewsFilterResult(
    val isTradingAllowed: Boolean,
    val activeEvent: EconomicEvent?,
    val minutesDifference: Int,
    val statusEn: String,
    val statusFa: String,
    val spreadWidenWarning: Boolean
)

class EconomicNewsFilter(
    private val blackoutMinutesBefore: Int = 15,
    private val blackoutMinutesAfter: Int = 15
) {
    // رویدادهای سنگین اقتصادی پیش‌بینی‌شده برای سنجش
    private val scheduledEvents = mutableListOf<EconomicEvent>()

    init {
        val now = System.currentTimeMillis()
        val hour = 3600_000L
        scheduledEvents.addAll(listOf(
            EconomicEvent("EV-1", "US Non-Farm Payrolls (NFP)", "گزارش اشتغال بخش غیرکشاورزی آمریکا", "USD", NewsImpact.HIGH, now + 2 * hour, "تاثیر مستقیم روی جفت‌ارزهای USD و طلا"),
            EconomicEvent("EV-2", "US CPI Inflation MoM/YoY", "شاخص تورم مصرف‌کننده آمریکا CPI", "USD", NewsImpact.HIGH, now + 14 * hour, "نوسان شدید نرخ بهره فدرال رزرو"),
            EconomicEvent("EV-3", "FOMC Rate Decision & Press Conference", "بیانیه نرخ بهره فدرال رزرو FOMC", "USD", NewsImpact.HIGH, now + 26 * hour, "سنگین‌ترین رویداد مارکت"),
            EconomicEvent("EV-4", "ECB Monetary Policy Statement", "بیانیه سیاست پولی بانک مرکزی اروپا ECB", "EUR", NewsImpact.HIGH, now + 8 * hour, "تاثیر مستقیم روی EURUSD"),
            EconomicEvent("EV-5", "US Core PPI MoM", "شاخص بهای تولیدکننده PPI", "USD", NewsImpact.MEDIUM, now + 5 * hour, "اثر میان‌مدت روی دلار")
        ))
    }

    /**
     * بررسی مجاز بودن ترید روی یک نماد با توجه به وضعیت اخبار
     */
    fun evaluateSymbol(symbol: String, currentTimeMillis: Long = System.currentTimeMillis()): NewsFilterResult {
        val currencyToCheck = when {
            symbol.contains("USD") || symbol.contains("XAU") || symbol.contains("BTC") -> "USD"
            symbol.contains("EUR") -> "EUR"
            symbol.contains("GBP") -> "GBP"
            symbol.contains("JPY") -> "JPY"
            else -> "USD"
        }

        for (event in scheduledEvents) {
            if (event.currency == currencyToCheck && event.impact == NewsImpact.HIGH) {
                val diffMillis = event.scheduledTimeMillis - currentTimeMillis
                val diffMinutes = (diffMillis / 60000L).toInt()

                // در بازه ۱۵ دقیقه قبل تا ۱۵ دقیقه بعد از خبر قرمز
                if (diffMinutes in -blackoutMinutesAfter..blackoutMinutesBefore) {
                    val statusText = if (diffMinutes > 0)
                        "بلاک معاملاتی: $diffMinutes دقیقه تا شروع خبر قرمز ${event.titleFa}"
                    else
                        "بلاک معاملاتی: ${abs(diffMinutes)} دقیقه پس از انتشار ${event.titleFa}"

                    return NewsFilterResult(
                        isTradingAllowed = false,
                        activeEvent = event,
                        minutesDifference = diffMinutes,
                        statusEn = "NEWS_BLACKOUT_ACTIVE",
                        statusFa = statusText,
                        spreadWidenWarning = true
                    )
                }
            }
        }

        return NewsFilterResult(
            isTradingAllowed = true,
            activeEvent = null,
            minutesDifference = 999,
            statusEn = "CLEAR_NO_HIGH_IMPACT_NEWS",
            statusFa = "آسمان معاملاتی صاف - هیچ خبر قرمزی در ۳۰ دقیقه آینده نیست",
            spreadWidenWarning = false
        )
    }

    fun getAllEvents(): List<EconomicEvent> = scheduledEvents.toList()

    fun addCustomEvent(event: EconomicEvent) {
        scheduledEvents.add(event)
    }
}
