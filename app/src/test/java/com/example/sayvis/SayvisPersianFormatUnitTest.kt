package com.example.sayvis

import com.example.sayvis.i18n.PersianFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the numeric rendering rules.
 *
 * The owner toggles one setting (`persianDigits`) and the whole interface must follow it
 * consistently — including the trap that a full stop inside a translated *sentence* is
 * punctuation, not a decimal separator.
 */
class SayvisPersianFormatUnitTest {

    @Test
    fun toPersianDigits_formatsNumbersIncludingSeparators() {
        assertEquals("۱٬۲۵۰", PersianFormat.toPersianDigits("1,250"))
        assertEquals("۲٫۵", PersianFormat.toPersianDigits("2.5"))
        assertEquals("۴۵٪", PersianFormat.toPersianDigits("45%"))
    }

    @Test
    fun toPersianNumerals_leavesSentencePunctuationAlone() {
        val sentence = "سطح باتری 17% است. توصیه می‌شود سرویس‌ها، بهینه‌سازی شوند."
        val result = PersianFormat.toPersianNumerals(sentence)
        assertEquals("سطح باتری ۱۷٪ است. توصیه می‌شود سرویس‌ها، بهینه‌سازی شوند.", result)
        assertTrue("sentence period must survive", result.contains("است."))
        assertFalse("comma must not become a thousands separator", result.contains("٬"))
    }

    @Test
    fun digitsHelper_respectsTheOwnerPreference() {
        assertEquals("1,250", PersianFormat.digits("1,250", persianDigits = false))
        assertEquals("۱٬۲۵۰", PersianFormat.digits("1,250", persianDigits = true))
    }

    @Test
    fun numberAndPercent_useGroupingAndTheRightSign() {
        assertEquals("1,250", PersianFormat.number(1250, persianDigits = false))
        assertEquals("۱٬۲۵۰", PersianFormat.number(1250, persianDigits = true))
        assertEquals("45%", PersianFormat.percent(45, persianDigits = false))
        assertEquals("۴۵٪", PersianFormat.percent(45, persianDigits = true))
        assertEquals("12%", PersianFormat.percent(0.1234, persianDigits = false))
    }

    @Test
    fun money_putsTheCurrencyWordAfterTheAmountInPersian() {
        // Persian reads «۱٬۲۵۰ دلار»; the bare "$" prefix is noise to a Persian reader.
        assertEquals("$1,250.00", PersianFormat.money(1250.0, "USD", persianDigits = false))
        assertEquals("۱٬۲۵۰٫۰۰ دلار", PersianFormat.money(1250.0, "USD", persianDigits = true))
        assertEquals("۱٬۲۵۰٫۰۰ تومان", PersianFormat.money(1250.0, "TMN", persianDigits = true))
    }

    @Test
    fun price_keepsTraderPrecision() {
        // In English the symbol prefixes the amount; in Persian the bare number is shown
        // because money() names the currency in words instead.
        assertEquals("€1.085", PersianFormat.price(1.085, "EUR", persianDigits = false))
        assertEquals("۲٬۵۸۰٫۴۰", PersianFormat.price(2580.40, "XAU", persianDigits = true))
    }

    @Test
    fun toLatinDigits_roundTrips() {
        assertEquals("1,250.5%", PersianFormat.toLatinDigits("۱٬۲۵۰٫۵٪"))
    }

    @Test
    fun relativeTime_isPersianWhenRequested() {
        val fiveMinutesAgo = System.currentTimeMillis() - 5 * 60_000L
        val persian = PersianFormat.relative(fiveMinutesAgo, persianDigits = true)
        val english = PersianFormat.relative(fiveMinutesAgo, persianDigits = false)
        assertTrue("expected a Persian relative label, got: $persian", persian.contains("دقیقه پیش"))
        assertTrue("expected an English relative label, got: $english", english.endsWith("m ago"))
    }

    @Test
    fun jalaliDate_usesPersianMonthNamesAndDigits() {
        // 2026-09-13T12:00:00Z — a fixed instant so the assertion is deterministic.
        val millis = 1789300800000L
        val label = PersianFormat.jalaliDate(millis)
        assertTrue("expected Persian digits and a Jalali month name, got: $label", label.contains("شهریور"))
        assertTrue(label.any { it in '۰'..'۹' })
        assertFalse(label.any { it in '0'..'9' })
    }

    @Test
    fun timeRange_convertsClockDigits() {
        assertEquals("۰۹:۰۰ - ۱۱:۳۰", PersianFormat.timeRange("09:00 - 11:30", persianDigits = true))
        assertEquals("09:00 - 11:30", PersianFormat.timeRange("09:00 - 11:30", persianDigits = false))
    }
}
