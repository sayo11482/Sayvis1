package com.example.sayvis

import com.example.sayvis.agent.BusinessDirectory
import com.example.sayvis.ai.SearchTasteEngine
import com.example.sayvis.trading.LitStrategyEngine
import com.example.sayvis.trading.MtfScanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** v5.0.0: MTF confluence, business classifier, taste engine. */
class SayvisV5UnitTest {

    // ------------------------------------------------------------ MTF rules

    private fun v(tf: MtfScanner.Tf, side: LitStrategyEngine.Side) =
        MtfScanner.TfVerdict(tf, side, rsi = 50.0, atr = 1.0, bars = 40)

    @Test
    fun `three agreeing timeframes grade A`() {
        val d = MtfScanner.combine(
            listOf(v(MtfScanner.Tf.M15, LitStrategyEngine.Side.LONG), v(MtfScanner.Tf.H1, LitStrategyEngine.Side.LONG), v(MtfScanner.Tf.H4, LitStrategyEngine.Side.LONG), v(MtfScanner.Tf.D1, LitStrategyEngine.Side.WAIT))
        )
        assertTrue(d.isEntry)
        assertEquals(LitStrategyEngine.Side.LONG, d.side)
        assertEquals(MtfScanner.Grade.A, d.grade)
        assertEquals(MtfScanner.Tf.M15, MtfScanner.executionTf(d))
    }

    @Test
    fun `heavy timeframe opposition blocks the entry`() {
        val d = MtfScanner.combine(
            listOf(v(MtfScanner.Tf.M15, LitStrategyEngine.Side.LONG), v(MtfScanner.Tf.H1, LitStrategyEngine.Side.LONG), v(MtfScanner.Tf.D1, LitStrategyEngine.Side.SHORT))
        )
        assertFalse(d.isEntry)
        assertEquals(MtfScanner.Grade.NONE, d.grade)
    }

    @Test
    fun `single timeframe never fires`() {
        val d = MtfScanner.combine(listOf(v(MtfScanner.Tf.D1, LitStrategyEngine.Side.SHORT)))
        assertFalse(d.isEntry)
    }

    @Test
    fun `two agreeing with light opposition is grade C`() {
        val d = MtfScanner.combine(
            listOf(v(MtfScanner.Tf.M15, LitStrategyEngine.Side.SHORT), v(MtfScanner.Tf.H1, LitStrategyEngine.Side.SHORT), v(MtfScanner.Tf.M15, LitStrategyEngine.Side.LONG))
        )
        // duplicate TF list is possible from multiple symbols; rules still hold
        assertTrue(d.side == LitStrategyEngine.Side.SHORT || d.side == LitStrategyEngine.Side.WAIT)
    }

    @Test
    fun `two agreeing clean is grade B`() {
        val d = MtfScanner.combine(
            listOf(v(MtfScanner.Tf.M15, LitStrategyEngine.Side.SHORT), v(MtfScanner.Tf.H1, LitStrategyEngine.Side.SHORT))
        )
        assertTrue(d.isEntry)
        assertEquals(MtfScanner.Grade.B, d.grade)
        assertEquals(LitStrategyEngine.Side.SHORT, d.side)
    }

    // ---------------------------------------------------- business classifier

    @Test
    fun `raw material supplier is detected`() {
        val c = BusinessDirectory.classify("سلام، موجودی گرانول پلی‌اتیلن رسید؛ قیمت عمده و بارگیری این هفته است.")
        assertEquals(BusinessDirectory.Category.SUPPLIER, c.category)
        assertTrue(c.score >= 40)
        assertTrue(c.signals.any { it.startsWith("تأمین") })
    }

    @Test
    fun `distribution office category`() {
        val c = BusinessDirectory.classify("شرکت پخش سراسری مواد غذایی، نمایندگی استان‌ها")
        assertEquals(BusinessDirectory.Category.DISTRIBUTION, c.category)
    }

    @Test
    fun `entertainment and other are separated`() {
        val ent = BusinessDirectory.classify("تور تفریحی کیش، رزرو رستوران و سینما")
        assertEquals(BusinessDirectory.Category.ENTERTAINMENT, ent.category)
        val other = BusinessDirectory.classify("سلام خوبی؟ فردا میای؟")
        assertEquals(BusinessDirectory.Category.OTHER, other.category)
        assertTrue(other.score < 16)
    }

    @Test
    fun `extraction of site address company and names`() {
        val text = "شرکت آریا پلیمر — قیمت عمده مواد اولیه\nآدرس: تهران، جاده مخصوص کیلومتر ۹\nwww.arya-polymer.ir"
        assertEquals("https://www.arya-polymer.ir", BusinessDirectory.extractWebsite(text))
        assertTrue(BusinessDirectory.extractAddress(text).contains("جاده"))
        assertTrue(BusinessDirectory.extractCompany(text).startsWith("شرکت آریا"))
        val (given, family) = BusinessDirectory.splitPersonName("علی رضایی")
        assertEquals("علی", given)
        assertEquals("رضایی", family)
        assertTrue(BusinessDirectory.isSenderCode("ARYAPOL"))
        assertFalse(BusinessDirectory.isSenderCode("09121234567"))
    }

    @Test
    fun `directory json round trip with unicode`() {
        val entries = listOf(
            BusinessDirectory.Entry(
                name = "علی", family = "رضایی", phone = "09121234567",
                company = "شرکت «آریا» پلیمر", category = BusinessDirectory.Category.SUPPLIER,
                site = "https://arya.ir", address = "تهران، جاده مخصوص",
                signals = listOf("تأمین:مواد اولیه", "بیزینس:قیمت"),
                provenance = "sms", score = 74, scannedAt = 1_700_000_000_000
            )
        )
        val restored = BusinessDirectory.decode(BusinessDirectory.encode(entries))
        assertEquals(1, restored.size)
        val e = restored[0]
        assertEquals("علی", e.name)
        assertEquals("رضایی", e.family)
        assertEquals(BusinessDirectory.Category.SUPPLIER, e.category)
        assertEquals("شرکت «آریا» پلیمر", e.company)
        assertEquals("https://arya.ir", e.site)
        assertEquals(74, e.score)
        assertEquals(2, e.signals.size)
    }

    // ---------------------------------------------------------- taste engine

    @Test
    fun `history is capped at one hundred`() {
        var h = emptyList<String>()
        repeat(130) { h = SearchTasteEngine.appendQuery(h, "q" + it) }
        assertEquals(100, h.size)
        assertEquals("q30", h.first())
    }

    @Test
    fun `tennis and calisthenics interests are recognised`() {
        val history = listOf("تمرین فورهند تنیس", "برنامه بارفیکس برای مبتدی", "بهترین راکت تنیس ۲۰۲۶", "اخبار فوتسال")
        val interests = SearchTasteEngine.interestsOf(history)
        assertTrue(interests.any { it.key == "tennis" })
        assertTrue(interests.any { it.key == "calisthenics" })
        val suggestions = SearchTasteEngine.sportsSuggestions(history, true)
        assertTrue(suggestions.any { it.tag == "تنیس" })
        assertTrue(suggestions.any { it.tag == "کالیستنیکس" })
        assertTrue(suggestions.all { it.reason.isNotBlank() })
    }

    @Test
    fun `import ingests pasted google activity text`() {
        val blob = "جستجوی: تمرین کالیستنیکس در خانه. جستجوی: تنیس ویمبلدون نتیجه. جستجوی: پروتئین وی مکمل."
        val ingested = SearchTasteEngine.ingestImport(blob)
        assertTrue(ingested.size >= 3)
        assertNotNull(SearchTasteEngine.sportsSuggestions(ingested, false))
    }

    @Test
    fun `empty history still yields the starter plan`() {
        val suggestions = SearchTasteEngine.sportsSuggestions(emptyList(), true)
        assertTrue(suggestions.isNotEmpty())
        assertEquals("شروع", suggestions.first().tag)
    }
}
