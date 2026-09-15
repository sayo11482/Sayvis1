package com.example.sayvis

import com.example.sayvis.ai.TranslationSource
import com.example.sayvis.screentranslate.MemoryKeyValueStore
import com.example.sayvis.screentranslate.PixelSampler
import com.example.sayvis.screentranslate.RecognizedFragment
import com.example.sayvis.screentranslate.ScreenColorMath
import com.example.sayvis.screentranslate.ScreenLexicon
import com.example.sayvis.screentranslate.ScreenPlateStyle
import com.example.sayvis.screentranslate.ScreenRect
import com.example.sayvis.screentranslate.ScreenTextColorMode
import com.example.sayvis.screentranslate.ScreenTextPlanner
import com.example.sayvis.screentranslate.ScreenTranslationGranularity
import com.example.sayvis.screentranslate.ScreenTranslationCache
import com.example.sayvis.screentranslate.ScreenTranslationEngine
import com.example.sayvis.screentranslate.ScreenTranslationMode
import com.example.sayvis.screentranslate.ScreenTranslationSettings
import com.example.sayvis.screentranslate.TranslatedSegment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the live screen translator (PUR).
 *
 * Everything that decides *what* is written on the screen and *where* is pure Kotlin, and is
 * covered here: the geometry of the replacement plates, the prose filter that keeps tickers
 * and numbers from being mangled, the offline glossary, the three-tier engine and the
 * persistent cache that stops the same sentence from being translated twice.
 */
class SayvisScreenTranslateUnitTest {

    // ------------------------------------------------------------- glossary tier

    @Test
    fun lexicon_resolves_interface_phrases_and_words_offline() {
        assertEquals("ذخیرهٔ تغییرها", ScreenLexicon.lookup("Save changes"))
        assertEquals("تنظیمات", ScreenLexicon.lookup("  Settings.  "))
        assertEquals("لغو", ScreenLexicon.lookup("cancel"))
        assertEquals("افزودن به سبد خرید", ScreenLexicon.lookup("Add to Cart"))
        assertTrue(ScreenLexicon.size > 400)
    }

    @Test
    fun lexicon_normalises_case_punctuation_and_spacing() {
        assertEquals("save changes", ScreenLexicon.normalize("  Save   CHANGES!  "))
        assertEquals("don't", ScreenLexicon.normalize("Don\u2019t"))
        assertEquals(ScreenLexicon.lookup("Sign in"), ScreenLexicon.lookup("sign   in"))
    }

    @Test
    fun lexicon_composes_lines_but_refuses_gibberish() {
        val composed = ScreenLexicon.glossLine("Save file")
        assertNotNull(composed)
        assertTrue(composed!!.contains("ذخیره"))

        // Two of three words unknown → below the coverage threshold → no half-Persian salad.
        assertNull(ScreenLexicon.glossLine("Zorblex quux save"))
        assertNull(ScreenLexicon.glossLine("wibble wobble"))
    }

    @Test
    fun lexicon_handles_plurals_and_possessives() {
        // "buttons" has no own entry: the plural fallback must find "button".
        assertEquals("دکمه", ScreenLexicon.glossToken("buttons"))
        assertEquals("کاربر", ScreenLexicon.glossToken("user's"))
        assertEquals("سند", ScreenLexicon.glossToken("document"))
    }

    // ------------------------------------------------------------- prose filter

    @Test
    fun planner_accepts_english_prose() {
        assertTrue(ScreenTextPlanner.isTranslatableLine("Save your changes before leaving"))
        assertTrue(ScreenTextPlanner.isTranslatableLine("Battery level at 17%"))
        assertTrue(ScreenTextPlanner.isTranslatableToken("Battery", 2))
    }

    @Test
    fun planner_rejects_numbers_identifiers_and_already_persian_text() {
        assertTrue(!ScreenTextPlanner.isTranslatableLine("1,234.56"))
        assertTrue(!ScreenTextPlanner.isTranslatableLine("XAUUSD 1.0842"))
        assertTrue(!ScreenTextPlanner.isTranslatableLine("https://sayvis.example.com/app"))
        assertTrue(!ScreenTextPlanner.isTranslatableLine("owner@sayvis.example"))
        assertTrue(!ScreenTextPlanner.isTranslatableLine("@sayo_channel"))
        assertTrue(!ScreenTextPlanner.isTranslatableLine("سطح باتری ۱۷٪ است"))
        assertTrue(!ScreenTextPlanner.isTranslatableToken("EURUSD", 2))
        assertTrue(!ScreenTextPlanner.isTranslatableToken("http", 4))
    }

    @Test
    fun planner_detects_script_direction() {
        assertTrue(ScreenTextPlanner.isPersianText("سلام دنیا"))
        assertTrue(!ScreenTextPlanner.isPersianText("Hello world"))
        assertTrue(ScreenTextPlanner.isLatinText("Hello world"))
        assertTrue(!ScreenTextPlanner.isLatinText("سلام"))
    }

    // ------------------------------------------------------------ line assembly

    @Test
    fun planner_groups_fragments_of_the_same_line_into_one_sentence() {
        val lines = ScreenTextPlanner.groupIntoLines(
            listOf(
                word("Save", 40f, 300f, 90f, 330f),
                word("your", 96f, 300f, 140f, 330f),
                word("changes", 146f, 300f, 220f, 330f),
                word("Cancel", 40f, 400f, 110f, 428f)
            )
        )
        assertEquals(2, lines.size)
        assertEquals("Save your changes", lines[0].text)
        assertEquals("Cancel", lines[1].text)
        assertEquals(40f, lines[0].bounds.left, 0.01f)
        assertEquals(220f, lines[0].bounds.right, 0.01f)
    }

    @Test
    fun planner_splits_distant_columns_on_the_same_band() {
        val lines = ScreenTextPlanner.groupIntoLines(
            listOf(
                word("Price", 30f, 200f, 90f, 230f),
                word("Quantity", 600f, 200f, 680f, 230f)
            )
        )
        assertEquals(2, lines.size)
    }

    @Test
    fun planner_joins_hyphenated_word_breaks() {
        val lines = ScreenTextPlanner.groupIntoLines(
            listOf(
                word("trans-", 20f, 100f, 70f, 128f),
                word("lation", 74f, 100f, 126f, 128f)
            )
        )
        assertEquals(1, lines.size)
        assertEquals("translation", lines[0].text)
    }

    // ------------------------------------------------------------ overlay planning

    @Test
    fun planner_writes_persian_in_place_of_the_original_rect() {
        val settings = ScreenTranslationSettings(
            mode = ScreenTranslationMode.REPLACE,
            granularity = ScreenTranslationGranularity.LINE,
            persianNumbers = true,
            plateStyle = ScreenPlateStyle.DARK,
            plateOpacityPercent = 90
        )
        val line = ScreenTextPlanner.groupIntoLines(
            listOf(word("Battery", 40f, 500f, 120f, 532f), word("low", 128f, 500f, 168f, 532f))
        ).first()

        val plan = ScreenTextPlanner.plan(
            lines = listOf(line),
            translations = mapOf(
                line.text to TranslatedSegment(line.text, "شارژ باتری کم است", TranslationSource.DICTIONARY)
            ),
            settings = settings,
            displayWidth = 1080,
            displayHeight = 2400
        )

        assertEquals(1, plan.regions.size)
        val region = plan.regions.first()
        assertEquals("شارژ باتری کم است", region.persianText)
        assertEquals(line.text, region.sourceText)
        assertEquals(TranslationSource.DICTIONARY, region.provenance)
        // The plate covers the original text…
        assertTrue(region.bounds.left <= line.bounds.left)
        assertTrue(region.bounds.top <= line.bounds.top)
        assertTrue(region.bounds.right >= line.bounds.right)
        assertTrue(region.bounds.bottom >= line.bounds.bottom)
        // …and stays on the display.
        assertTrue(region.bounds.right <= 1080f)
        assertTrue(region.bounds.bottom <= 2400f)
        // 90% of 255.
        assertEquals(229, (region.plateColor ushr 24) and 0xFF)
        assertTrue(region.fontSizePx > 8f)
    }

    @Test
    fun planner_skips_lines_it_cannot_translate_in_line_mode() {
        val line = ScreenTextPlanner.groupIntoLines(listOf(word("Loading", 40f, 100f, 120f, 130f))).first()
        val plan = ScreenTextPlanner.plan(
            lines = listOf(line),
            translations = emptyMap(),
            settings = ScreenTranslationSettings(granularity = ScreenTranslationGranularity.LINE),
            displayWidth = 1080,
            displayHeight = 2400
        )
        assertTrue(plan.isEmpty)
        assertEquals(1, plan.skippedFragments)
    }

    @Test
    fun planner_falls_back_to_word_regions_when_the_line_is_unknown() {
        val line = ScreenTextPlanner.groupIntoLines(
            listOf(word("Widget", 40f, 200f, 110f, 228f), word("Save", 118f, 200f, 168f, 228f))
        ).first()
        val plan = ScreenTextPlanner.plan(
            lines = listOf(line),
            translations = mapOf(
                "Save" to TranslatedSegment("Save", "ذخیره", TranslationSource.DICTIONARY)
            ),
            settings = ScreenTranslationSettings(granularity = ScreenTranslationGranularity.AUTO),
            displayWidth = 1080,
            displayHeight = 2400
        )
        assertEquals(1, plan.regions.size)
        assertEquals("Save", plan.regions.first().sourceText)
        assertEquals("ذخیره", plan.regions.first().persianText)
    }

    @Test
    fun planner_word_mode_ignores_line_translations() {
        val line = ScreenTextPlanner.groupIntoLines(
            listOf(word("Open", 40f, 200f, 90f, 228f), word("Folder", 96f, 200f, 160f, 228f))
        ).first()
        val plan = ScreenTextPlanner.plan(
            lines = listOf(line),
            translations = mapOf(
                line.text to TranslatedSegment(line.text, "پوشه را باز کن", TranslationSource.MACHINE),
                "Open" to TranslatedSegment("Open", "باز کردن", TranslationSource.DICTIONARY),
                "Folder" to TranslatedSegment("Folder", "پوشه", TranslationSource.DICTIONARY)
            ),
            settings = ScreenTranslationSettings(granularity = ScreenTranslationGranularity.WORD),
            displayWidth = 1080,
            displayHeight = 2400
        )
        assertEquals(2, plan.regions.size)
        assertTrue(plan.regions.all { it.provenance == TranslationSource.DICTIONARY })
    }

    @Test
    fun planner_caption_mode_places_text_below_the_original() {
        val line = ScreenTextPlanner.groupIntoLines(listOf(word("Hello", 40f, 300f, 120f, 330f))).first()
        val plan = ScreenTextPlanner.plan(
            lines = listOf(line),
            translations = mapOf("Hello" to TranslatedSegment("Hello", "سلام", TranslationSource.DICTIONARY)),
            settings = ScreenTranslationSettings(mode = ScreenTranslationMode.CAPTION),
            displayWidth = 1080,
            displayHeight = 2400
        )
        val region = plan.regions.first()
        assertTrue(region.bounds.top >= line.bounds.bottom)
    }

    @Test
    fun planner_widens_plates_symmetrically_and_clamps_at_the_edges() {
        val rect = ScreenRect(500f, 100f, 560f, 130f)
        val widened = ScreenTextPlanner.widenToFit(rect, 300f, 1000f)
        assertEquals(rect.centerX, widened.centerX, 0.5f)
        assertEquals(300f, widened.width, 1f)

        val touchingEdge = ScreenTextPlanner.widenToFit(ScreenRect(0f, 100f, 60f, 130f), 400f, 1000f)
        assertEquals(0f, touchingEdge.left, 0.01f)
        assertTrue(touchingEdge.right <= 1000f)

        // Already wide enough: untouched.
        assertEquals(rect, ScreenTextPlanner.widenToFit(rect, 10f, 1000f))
    }

    @Test
    fun planner_estimates_text_geometry() {
        val short = ScreenTextPlanner.estimateTextWidth("سلام", 20f)
        val long = ScreenTextPlanner.estimateTextWidth("سلام دنیای زیبا", 20f)
        assertTrue(long > short)
        assertEquals(1, ScreenTextPlanner.estimateLineCount("کوتاه", 20f, 400f))
        assertTrue(ScreenTextPlanner.estimateLineCount("یک جملهٔ بسیار طولانی برای آزمودن شکستن خط", 20f, 60f) > 1)
    }

    @Test
    fun planner_limits_regions_per_frame() {
        val lines = (0 until 30).map { index ->
            ScreenTextPlanner.groupIntoLines(
                listOf(word("Warning$index", 20f, 100f + index * 40f, 200f, 128f + index * 40f))
            ).first()
        }
        val translations = lines.associate { it.text to TranslatedSegment(it.text, "هشدار شماره", TranslationSource.MACHINE) }
        val plan = ScreenTextPlanner.plan(
            lines = lines,
            translations = translations,
            settings = ScreenTranslationSettings(maxSegmentsPerFrame = 5),
            displayWidth = 1080,
            displayHeight = 2400
        )
        assertEquals(5, plan.regions.size)
    }

    // ------------------------------------------------------------------ colours

    @Test
    fun colour_math_matches_the_plate_to_the_pixels_underneath() {
        val sampler = PixelSampler { _, _ -> 0x00202830 }
        val rect = ScreenRect(10f, 10f, 60f, 40f)
        val sampled = ScreenColorMath.averageColor(sampler, rect)
        assertNotNull(sampled)
        // Sampling is scaled down to 5x5 and re-packed; the colour must stay identical.
        assertEquals(0x202830, sampled!! and 0xFFFFFF)

        val plate = ScreenColorMath.plateColor(ScreenPlateStyle.SAMPLED, 0x123456, sampled, 255)
        assertEquals(0x202830, plate and 0xFFFFFF)
        assertEquals(0xFF, (plate ushr 24) and 0xFF)

        assertEquals(0xF4F7FB.toInt() and 0xFFFFFF, ScreenColorMath.inkFor(0x101010) and 0xFFFFFF)
        assertEquals(0x10151C, ScreenColorMath.inkFor(0xFAFAFA) and 0xFFFFFF)
    }

    @Test
    fun colour_math_respects_explicit_ink_modes() {
        val plate = ScreenColorMath.plateColor(ScreenPlateStyle.DARK, 0x0B0F14, null, 200)
        assertEquals(200, (plate ushr 24) and 0xFF)
        val ink = ScreenColorMath.textColor(ScreenTextColorMode.DARK, plate, 0xFFFFFF)
        assertEquals(0x10151C, ink and 0xFFFFFF)
        assertEquals(255, (ink ushr 24) and 0xFF)
    }

    // --------------------------------------------------------- frame fingerprints

    @Test
    fun fingerprints_track_content_and_detect_protected_screens() {
        val bright = IntArray(256) { 200 }
        val changed = IntArray(256) { 60 }
        assertNotEquals(ScreenTextPlanner.fingerprint(bright), ScreenTextPlanner.fingerprint(changed))
        assertEquals(ScreenTextPlanner.fingerprint(bright), ScreenTextPlanner.fingerprint(IntArray(256) { 200 }))

        assertTrue(ScreenTextPlanner.looksLikeBlankFrame(IntArray(256) { 0 }))
        assertTrue(!ScreenTextPlanner.looksLikeBlankFrame(bright))
        assertEquals(0, ScreenTextPlanner.gridDistance(bright, bright))
        assertEquals(140, ScreenTextPlanner.gridDistance(bright, changed))
        assertEquals(200, ScreenTextPlanner.averageLuminance(bright))
    }

    // -------------------------------------------------------------------- cache

    @Test
    fun cache_keeps_persian_results_and_survives_a_restart() {
        val store = MemoryKeyValueStore()
        val cache = ScreenTranslationCache(store, limit = 3)
        cache.put("Save changes", "ذخیرهٔ تغییرها")
        // English output is never cached.
        cache.put("Email", "Email")
        assertEquals(1, cache.size())
        assertEquals("ذخیرهٔ تغییرها", cache.get("Save changes"))
        assertNull(cache.get("cancel"))

        // A fresh instance reads the persisted store.
        val reloaded = ScreenTranslationCache(store, limit = 3)
        assertEquals("ذخیرهٔ تغییرها", reloaded.get("  save changes "))
    }

    @Test
    fun cache_evicts_the_oldest_entries_beyond_its_limit() {
        val store = MemoryKeyValueStore()
        val cache = ScreenTranslationCache(store, limit = 2)
        cache.put("one", "یک")
        cache.put("two", "دو")
        cache.put("three", "سه")
        assertTrue(cache.size() <= 2)
        assertEquals("سه", cache.get("three"))
    }

    // -------------------------------------------------------------------- engine

    @Test
    fun engine_prefers_cache_then_dictionary_without_touching_the_network() {
        val store = MemoryKeyValueStore()
        val cache = ScreenTranslationCache(store)
        cache.put("Payment method", "روش پرداخت")
        val engine = ScreenTranslationEngine(cache = cache)

        val resolved = engine.resolveOffline(
            listOf("Payment method", "Save changes"),
            ScreenTranslationSettings()
        )
        assertEquals(TranslationSource.CACHE, resolved.getValue("Payment method").provenance)
        assertEquals(TranslationSource.DICTIONARY, resolved.getValue("Save changes").provenance)
    }

    @Test
    fun engine_reads_numbered_model_output_in_every_numbering_style() {
        val engine = ScreenTranslationEngine(cache = ScreenTranslationCache(MemoryKeyValueStore()))
        val parsed = engine.parseNumbered(
            """
            1. سلام
            2) دنیای زیبا
            3- پایان
            """.trimIndent(),
            expected = 3
        )
        assertEquals("سلام", parsed[0])
        assertEquals("دنیای زیبا", parsed[1])
        assertEquals("پایان", parsed[2])

        // Out-of-range indices are dropped rather than mismatched.
        assertTrue(engine.parseNumbered("7. هیچ‌چیز", expected = 2).isEmpty())

        // A single bare answer with no numbering is still accepted.
        assertEquals("سلام", engine.parseNumbered("سلام", expected = 1)[0])
    }

    // ------------------------------------------------------------------ settings

    @Test
    fun settings_are_sanitised_before_they_reach_the_pipeline() {
        val extreme = ScreenTranslationSettings(
            pollIntervalMs = 10,
            plateOpacityPercent = 250,
            textScalePercent = 1000,
            bubbleOpacityPercent = 5
        )
        assertEquals(ScreenTranslationSettings.MIN_POLL_MS.toLong(), extreme.boundedPollInterval())
        assertEquals(255, extreme.plateAlpha())
        assertEquals(1.7f, extreme.textScale(), 0.001f)
        assertEquals(51, extreme.bubbleAlpha())
    }

    // -------------------------------------------------------------------- rects

    @Test
    fun rect_helpers_behave() {
        val a = ScreenRect(10f, 20f, 60f, 50f)
        val b = ScreenRect(40f, 40f, 120f, 90f)
        assertEquals(50f, a.width, 0.01f)
        assertEquals(30f, a.height, 0.01f)
        assertEquals(35f, a.centerX, 0.01f)
        assertTrue(a.contains(30f, 30f))
        assertTrue(!a.contains(70f, 30f))
        assertEquals(10f, a.verticalOverlap(b), 0.01f)
        assertEquals(0f, a.horizontalGap(b), 0.01f)
        assertEquals(120f, a.union(b).right, 0.01f)
        assertEquals(120f, a.scaled(2f).right, 0.01f)
        assertTrue(!ScreenRect.EMPTY.isValid)
    }

    private fun word(text: String, left: Float, top: Float, right: Float, bottom: Float): RecognizedFragment =
        RecognizedFragment(text, ScreenRect(left, top, right, bottom), word = true)
}
