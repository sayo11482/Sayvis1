package com.example.sayvis.i18n

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Locale-aware numeric, currency and date rendering.
 *
 * Persian users expect Persian digits and the Jalali-friendly "day/month name" style;
 * everything in the app funnels through these helpers so a single settings toggle
 * (`persianDigits`) changes the whole interface consistently.
 */
object PersianFormat {

    private val FA_DIGITS = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    private val EN_DIGITS = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')

    /** Converts ASCII digits to Persian digits (and the decimal separator to «٫»). */
    fun toPersianDigits(input: String): String = buildString(input.length) {
        for (ch in input) {
            when {
                ch in '0'..'9' -> append(FA_DIGITS[ch - '0'])
                ch == '.' -> append('٫')
                ch == ',' -> append('٬')
                ch == '%' -> append('٪')
                else -> append(ch)
            }
        }
    }

    /**
     * Digit-only conversion, intended for **prose**.
     *
     * [toPersianDigits] also rewrites `.` to «٫» and `,` to «٬», which is correct for a
     * formatted number but wrong inside a sentence: there a full stop is punctuation, not
     * a decimal separator, and a comma is not a thousands separator. Translated sentences
     * therefore go through this function instead, so «سطح باتری ۱۷٪ است.» keeps its period.
     */
    fun toPersianNumerals(input: String): String = buildString(input.length) {
        for (ch in input) {
            when (ch) {
                in '0'..'9' -> append(FA_DIGITS[ch - '0'])
                '%' -> append('٪')
                else -> append(ch)
            }
        }
    }

    fun toLatinDigits(input: String): String = buildString(input.length) {
        for (ch in input) {
            val idx = FA_DIGITS.indexOf(ch)
            when {
                idx >= 0 -> append(EN_DIGITS[idx])
                ch == '٫' -> append('.')
                ch == '٬' -> append(',')
                ch == '٪' -> append('%')
                else -> append(ch)
            }
        }
    }

    /** Applies the owner's digit preference to any already-formatted string. */
    fun digits(value: String, persianDigits: Boolean): String =
        if (persianDigits) toPersianDigits(value) else value

    fun number(value: Int, persianDigits: Boolean): String =
        digits(String.format(Locale.US, "%,d", value), persianDigits)

    fun number(value: Long, persianDigits: Boolean): String =
        digits(String.format(Locale.US, "%,d", value), persianDigits)

    fun number(value: Double, decimals: Int, persianDigits: Boolean): String =
        digits(String.format(Locale.US, "%,.${decimals}f", value), persianDigits)

    fun percent(ratio: Double, decimals: Int = 0, persianDigits: Boolean = true): String =
        digits(String.format(Locale.US, "%.${decimals}f%%", ratio * 100.0), persianDigits)

    fun percent(percentValue: Int, persianDigits: Boolean = true): String =
        digits("$percentValue%", persianDigits)

    /**
     * Money rendering. In Persian the currency word follows the amount («۱٬۲۵۰ دلار»)
     * and the Latin symbol is dropped, because a bare «$» reads as noise to a Persian
     * reader. In English the conventional prefix is kept.
     */
    fun money(amount: Double, currency: String = "USD", persianDigits: Boolean = true): String {
        val body = number(amount, 2, persianDigits)
        return if (persianDigits) "$body ${currencyWord(currency, true)}" else "${currencySymbol(currency)}$body"
    }

    fun currencySymbol(currency: String): String = when (currency.uppercase(Locale.ROOT)) {
        "USD" -> "$"
        "EUR" -> "€"
        "GBP" -> "£"
        "IRR", "TMN", "IRT" -> ""
        else -> "$"
    }

    fun currencyWord(currency: String, persianDigits: Boolean): String {
        if (!persianDigits) return currency.uppercase(Locale.ROOT)
        return when (currency.uppercase(Locale.ROOT)) {
            "USD" -> "دلار"
            "EUR" -> "یورو"
            "GBP" -> "پوند"
            "IRR" -> "ریال"
            "TMN", "IRT" -> "تومان"
            "XAU" -> "دلار (طلا)"
            else -> currency.uppercase(Locale.ROOT)
        }
    }

    /** Price rendering that keeps the precision a trader needs. */
    fun price(value: Double, currency: String = "USD", persianDigits: Boolean = true): String {
        val decimals = if (value >= 1000) 2 else if (value >= 1) 3 else 5
        val body = number(value, decimals, persianDigits)
        return if (persianDigits) body else "${currencySymbol(currency)}$body"
    }

    private val FA_MONTHS = arrayOf(
        "ژانویه", "فوریه", "مارس", "آوریل", "مه", "ژوئن",
        "ژوئیه", "اوت", "سپتامبر", "اکتبر", "نوفمبر", "دسامبر"
    )

    /**
     * Human-readable absolute timestamp. Persian uses «امروز/دیروز» plus a time,
     * which is far more natural than a transliterated English date.
     */
    fun dateTime(epochMillis: Long, persianDigits: Boolean): String {
        val cal = Calendar.getInstance().apply { timeInMillis = epochMillis }
        val hour = String.format(Locale.US, "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
        val time = digits(hour, persianDigits)

        if (!persianDigits) {
            return SimpleDateFormat("MMM d, HH:mm", Locale.US).format(Date(epochMillis))
        }

        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val dayLabel = when {
            isSameDay(cal, today) -> "امروز"
            isSameDay(cal, yesterday) -> "دیروز"
            else -> "${digits(cal.get(Calendar.DAY_OF_MONTH).toString(), true)} ${FA_MONTHS[cal.get(Calendar.MONTH)]}"
        }
        return "$dayLabel، $time"
    }

    /** Relative «چند لحظه پیش» style label. */
    fun relative(epochMillis: Long, persianDigits: Boolean): String {
        val diff = System.currentTimeMillis() - epochMillis
        val minutes = diff / 60_000L
        val hours = diff / 3_600_000L
        val days = diff / 86_400_000L

        if (!persianDigits) {
            return when {
                minutes < 1 -> "just now"
                minutes < 60 -> "${minutes}m ago"
                hours < 24 -> "${hours}h ago"
                else -> "${days}d ago"
            }
        }
        return when {
            minutes < 1 -> "همین حالا"
            minutes < 60 -> "${digits(minutes.toString(), true)} دقیقه پیش"
            hours < 24 -> "${digits(hours.toString(), true)} ساعت پیش"
            days < 30 -> "${digits(days.toString(), true)} روز پیش"
            else -> dateTime(epochMillis, true)
        }
    }

    /** Converts an ISO-ish duration window such as "09:00 - 11:30" to Persian digits. */
    fun timeRange(value: String, persianDigits: Boolean): String = digits(value, persianDigits)

    private fun isSameDay(a: Calendar, b: Calendar): Boolean =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

    /**
     * Rough Gregorian→Jalali conversion used only for display labels.
     * Keeps dates meaningful for Persian users without pulling in a calendar library.
     */
    fun jalaliDate(epochMillis: Long): String {
        val gc = Calendar.getInstance().apply { timeInMillis = epochMillis }
        val gy = gc.get(Calendar.YEAR)
        val gm = gc.get(Calendar.MONTH) + 1
        val gd = gc.get(Calendar.DAY_OF_MONTH)

        val gDaysInMonth = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        val jDaysInMonth = intArrayOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)
        val monthNames = arrayOf(
            "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
            "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
        )

        val gy2 = if (gm > 2) gy + 1 else gy
        var days = 355666L + (365L * gy) + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) +
            ((gy2 + 399) / 400) + gd + gDaysInMonth.take(gm - 1).sum()
        if (isGregorianLeap(gy) && gm > 2) days += 1

        var jy = -1595 + (33 * (days / 12053).toInt())
        days %= 12053
        jy += 4 * (days / 1461).toInt()
        days %= 1461
        if (days > 365) {
            jy += ((days - 1) / 365).toInt()
            days = (days - 1) % 365
        }
        var jm = 0
        var jd = days.toInt()
        while (jm < 12 && jd >= jDaysInMonth[jm]) {
            jd -= jDaysInMonth[jm]
            jm++
        }
        return "${toPersianDigits((jd + 1).toString())} ${monthNames[jm]} ${toPersianDigits((jy + 1).toString())}"
    }

    private fun isGregorianLeap(year: Int): Boolean =
        (year % 4 == 0 && year % 100 != 0) || year % 400 == 0
}
