package com.example.sayvis.screentranslate

import com.example.sayvis.ai.TranslationSource
import com.example.sayvis.i18n.PersianFormat
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Reads a frame's worth of recognised words and produces the **overlay plan**: for every
 * piece of English on the screen, the exact rectangle that must be covered and the Persian
 * string that must be written in its place.
 *
 * Deliberately free of Android types. Everything here — line assembly, the "is this even
 * prose" filter, plate geometry, font fitting, contrast decisions and the perceptual frame
 * fingerprint — is exercised by `SayvisScreenTranslateUnitTest` on the JVM, because a
 * translator that misplaces its text is worse than one that does not run at all.
 */
object ScreenTextPlanner {

    /** Average glyph advance as a fraction of the font size, Persian script. */
    const val PERSIAN_ADVANCE = 0.55f

    /** Average glyph advance as a fraction of the font size, Latin script. */
    const val LATIN_ADVANCE = 0.52f

    /** Hard ceiling on a single translatable segment (protects the model context). */
    const val MAX_SEGMENT_CHARS = 240

    // ------------------------------------------------------------ text filtering

    /** True when the string is mostly Persian/Arabic script. */
    fun isPersianText(text: String): Boolean {
        var rtl = 0
        var letters = 0
        for (ch in text) {
            if (ch.isLetter()) {
                letters++
                if (ch.code in 0x0600..0x06FF || ch.code in 0xFB50..0xFDFF || ch.code in 0xFE70..0xFEFF) rtl++
            }
        }
        return letters > 0 && rtl.toFloat() / letters.toFloat() > 0.5f
    }

    /** True when the string is mostly Latin script. */
    fun isLatinText(text: String): Boolean {
        var latin = 0
        var letters = 0
        for (ch in text) {
            if (ch.isLetter()) {
                letters++
                if (ch.code in 0x0041..0x024F) latin++
            }
        }
        return letters > 0 && latin.toFloat() / letters.toFloat() > 0.45f
    }

    /**
     * A single word is worth translating when it is prose rather than an identifier:
     * long enough, alphabetic, and containing a vowel (tickers and code tokens such as
     * "XAUUSD", "MT5", "HTTP2" are deliberately left untouched).
     */
    fun isTranslatableToken(token: String, minWordLength: Int = 2): Boolean {
        val trimmed = token.trim()
        if (trimmed.length < minWordLength) return false
        if (!isLatinText(trimmed)) return false
        if (trimmed.length <= 3) return true // "ok", "yes", "no", "on", "off"
        if (TECHNICAL.matches(trimmed)) return false
        val letters = trimmed.filter { it.isLetter() }
        if (letters.length < 3) return false
        // All-capitals words of four letters or more are identifiers, not prose:
        // EURUSD, XAUUSD, MT5, JSON, UTF8. Nothing readable is lost by skipping them.
        if (letters.length >= 4 && letters.none { it.isLowerCase() }) return false
        // A word with no vowel at all ("http", "www", "nn") is a code fragment.
        if (letters.none { it.lowercaseChar() in "aeiouy" }) return false
        return true
    }

    /**
     * A whole line is worth translating when it carries English prose. Pure numbers,
     * timestamps, chat handles, URLs, base64 blobs and already-Persian text are rejected
     * so the overlay never scribbles over something it cannot improve.
     */
    fun isTranslatableLine(text: String, minWordLength: Int = 2): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || trimmed.length > MAX_SEGMENT_CHARS) return false
        if (isPersianText(trimmed)) return false
        if (!isLatinText(trimmed)) return false
        if (URL.matches(trimmed) || EMAIL.containsMatchIn(trimmed)) return false
        if (HANDLE.matches(trimmed) || HASHTAG_ONLY.matches(trimmed)) return false
        if (DIGIT_HEAVY.matches(trimmed)) return false

        val tokens = trimmed.split(' ', '\t', '|', '·').filter { it.isNotBlank() }
        val translatableTokens = tokens.count { isTranslatableToken(it, minWordLength) }
        if (translatableTokens == 0) return false

        val letters = trimmed.count { it.isLetter() }
        val digits = trimmed.count { it.isDigit() }
        if (letters < 2) return false
        // "Total 1,234.56" → mostly digits, nothing to translate.
        if (digits > 0 && letters.toFloat() / (letters + digits).toFloat() < 0.35f) return false
        return true
    }

    // --------------------------------------------------------------- line assembly

    /**
     * Stitches ML Kit word fragments back into readable lines.
     *
     * Two passes: first cluster fragments that share a vertical band (so a two-column
     * screen produces two separate lines, not one merged salad), then split each cluster
     * where the horizontal gap is too wide to be a space between words.
     */
    fun groupIntoLines(
        fragments: List<RecognizedFragment>,
        maximumFragments: Int = 600
    ): List<ScreenTextLine> {
        val usable = fragments
            .filter { it.bounds.isValid && it.text.isNotBlank() }
            .take(maximumFragments)
        if (usable.isEmpty()) return emptyList()

        val clusters = mutableListOf<MutableList<RecognizedFragment>>()
        for (fragment in usable.sortedWith(compareBy({ it.bounds.top }, { it.bounds.left }))) {
            val target = clusters.firstOrNull { cluster ->
                val band = cluster[0].bounds
                val overlap = band.verticalOverlap(fragment.bounds)
                val reference = min(band.height, fragment.bounds.height).coerceAtLeast(1f)
                overlap / reference >= 0.45f
            }
            if (target != null) target.add(fragment) else clusters.add(mutableListOf(fragment))
        }

        val lines = mutableListOf<ScreenTextLine>()
        for (cluster in clusters) {
            val ordered = cluster.sortedBy { it.bounds.left }
            val averageCharWidth = ordered
                .map { fragment -> fragment.bounds.width / fragment.text.length.coerceAtLeast(1) }
                .average()
                .toFloat()
                .coerceAtLeast(1f)
            val gapLimit = max(14f, averageCharWidth * 2.4f)

            var run = mutableListOf<RecognizedFragment>()
            var previous: RecognizedFragment? = null
            fun flush() {
                if (run.isEmpty()) return
                lines.add(buildLine(run))
                run = mutableListOf()
            }
            for (fragment in ordered) {
                val prior = previous
                if (prior != null && prior.bounds.horizontalGap(fragment.bounds) > gapLimit) flush()
                run.add(fragment)
                previous = fragment
            }
            flush()
        }
        return lines.sortedWith(compareBy({ it.bounds.top }, { it.bounds.left }))
    }

    private fun buildLine(fragments: List<RecognizedFragment>): ScreenTextLine {
        val builder = StringBuilder()
        for (fragment in fragments) {
            val text = fragment.text.trim()
            if (text.isEmpty()) continue
            when {
                builder.isEmpty() -> builder.append(text)
                builder.endsWith("-") -> {
                    builder.setLength(builder.length - 1)
                    builder.append(text)
                }
                else -> builder.append(' ').append(text)
            }
        }
        val bounds = fragments
            .map { it.bounds }
            .reduce { acc, rect -> acc.union(rect) }
        return ScreenTextLine(builder.toString().trim(), bounds, fragments.toList())
    }

    // -------------------------------------------------------------------- planning

    /**
     * Turns recognised lines plus their translations into drawable regions.
     *
     * @param translations keyed by the exact segment text that was submitted, for lines
     *        and — when the granularity allows it — for individual words.
     * @param sampler reads the colour underneath a rectangle so the plate can be matched
     *        to the app that is being translated (null → fall back to the chosen style).
     */
    fun plan(
        lines: List<ScreenTextLine>,
        translations: Map<String, TranslatedSegment>,
        settings: ScreenTranslationSettings,
        displayWidth: Int,
        displayHeight: Int,
        sampler: PixelSampler? = null
    ): FrameTranslationPlan {
        val width = displayWidth.toFloat().coerceAtLeast(1f)
        val height = displayHeight.toFloat().coerceAtLeast(1f)
        val scale = settings.textScale()
        val alpha = settings.plateAlpha()
        val regions = mutableListOf<PlannedRegion>()
        var candidates = 0
        var skipped = 0
        var cached = 0
        var dictionary = 0
        var machine = 0

        for (line in lines) {
            if (!line.bounds.isValid) {
                skipped++
                continue
            }
            if (!isTranslatableLine(line.text, settings.minWordLength)) {
                skipped++
                continue
            }
            candidates++

            var handled = 0
            if (settings.granularity == ScreenTranslationGranularity.WORD) {
                handled = appendWordRegions(
                    line = line,
                    translations = translations,
                    regions = regions,
                    settings = settings,
                    scale = scale,
                    alpha = alpha,
                    displayWidth = width,
                    displayHeight = height,
                    sampler = sampler
                )
            }

            if (handled == 0) {
                val lineTranslation = translations[line.text]
                if (lineTranslation != null && lineTranslation.persianText.isNotBlank()) {
                    when (lineTranslation.provenance) {
                        TranslationSource.DICTIONARY -> dictionary++
                        TranslationSource.CACHE -> cached++
                        TranslationSource.MACHINE -> machine++
                        else -> Unit
                    }
                    regions += buildRegion(
                        sourceText = line.text,
                        persianText = lineTranslation.persianText,
                        provenance = lineTranslation.provenance,
                        sourceBounds = line.bounds,
                        settings = settings,
                        scale = scale,
                        alpha = alpha,
                        displayWidth = width,
                        displayHeight = height,
                        sampler = sampler
                    )
                    handled = 1
                }
            }

            // Last resort in AUTO mode: the line itself was not resolvable, but some of its
            // words were — cover those, so the screen is never left half English.
            if (handled == 0 && settings.granularity != ScreenTranslationGranularity.LINE) {
                handled = appendWordRegions(
                    line = line,
                    translations = translations,
                    regions = regions,
                    settings = settings,
                    scale = scale,
                    alpha = alpha,
                    displayWidth = width,
                    displayHeight = height,
                    sampler = sampler
                )
            }

            if (handled == 0) skipped++
        }

        val limited = if (regions.size > settings.maxSegmentsPerFrame) {
            regions
                .sortedByDescending { it.sourceText.length }
                .take(settings.maxSegmentsPerFrame)
                .sortedWith(compareBy({ it.bounds.top }, { it.bounds.left }))
        } else {
            regions.sortedWith(compareBy({ it.bounds.top }, { it.bounds.left }))
        }

        return FrameTranslationPlan(
            regions = limited,
            detectedFragments = lines.sumOf { it.fragments.size },
            candidateSegments = candidates,
            skippedFragments = skipped,
            cachedHits = cached,
            dictionaryHits = dictionary,
            machineHits = machine,
            capturedAt = System.currentTimeMillis(),
            displayWidth = displayWidth,
            displayHeight = displayHeight
        )
    }

    /**
     * Adds one region per word that has its own translation. Returns how many regions were
     * produced, so the caller can decide whether the line as a whole still needs handling.
     */
    private fun appendWordRegions(
        line: ScreenTextLine,
        translations: Map<String, TranslatedSegment>,
        regions: MutableList<PlannedRegion>,
        settings: ScreenTranslationSettings,
        scale: Float,
        alpha: Int,
        displayWidth: Float,
        displayHeight: Float,
        sampler: PixelSampler?
    ): Int {
        var produced = 0
        for (fragment in line.fragments) {
            val word = fragment.text.trim()
            if (word.isEmpty()) continue
            if (!isTranslatableToken(word, settings.minWordLength)) continue
            val translation = translations[word] ?: continue
            if (translation.persianText.isBlank()) continue
            regions += buildRegion(
                sourceText = word,
                persianText = translation.persianText,
                provenance = translation.provenance,
                sourceBounds = fragment.bounds,
                settings = settings,
                scale = scale,
                alpha = alpha,
                displayWidth = displayWidth,
                displayHeight = displayHeight,
                sampler = sampler
            )
            produced++
        }
        return produced
    }

    /** Geometry + colours for a single translated segment. */
    fun buildRegion(
        sourceText: String,
        persianText: String,
        provenance: TranslationSource,
        sourceBounds: ScreenRect,
        settings: ScreenTranslationSettings,
        scale: Float,
        alpha: Int,
        displayWidth: Float,
        displayHeight: Float,
        sampler: PixelSampler? = null
    ): PlannedRegion {
        val rendered = if (settings.persianNumbers) PersianFormat.toPersianNumerals(persianText) else persianText
        val baseFont = (sourceBounds.height * 0.80f * scale).coerceIn(9f, 96f)
        val padded = sourceBounds.expanded(
            dx = max(2f, sourceBounds.height * 0.14f),
            dy = max(1.5f, sourceBounds.height * 0.10f)
        )

        val plateRect: ScreenRect
        val textRect: ScreenRect
        when (settings.mode) {
            ScreenTranslationMode.REPLACE -> {
                plateRect = padded
                textRect = padded.expanded(dx = -3f, dy = -2f)
            }
            ScreenTranslationMode.CAPTION -> {
                val captionHeight = sourceBounds.height * 0.95f
                plateRect = ScreenRect(
                    left = sourceBounds.left,
                    top = sourceBounds.bottom + 2f,
                    right = sourceBounds.right,
                    bottom = sourceBounds.bottom + 2f + captionHeight
                ).expanded(dx = 2f, dy = 1f)
                textRect = plateRect.expanded(dx = -3f, dy = -2f)
            }
            ScreenTranslationMode.HIGHLIGHT -> {
                plateRect = sourceBounds.expanded(dx = 2f, dy = 2f)
                textRect = sourceBounds
            }
        }

        // A Persian rendering is usually wider than the English original; widen the plate
        // symmetrically (never asymmetrically, or the text would drift off its anchor)
        // so a single-line replacement keeps looking like the interface it replaced.
        val needed = estimateTextWidth(rendered, baseFont)
        val widened = widenToFit(plateRect, needed + 6f, displayWidth)
            .clampTo(displayWidth, displayHeight)

        val finalTextRect = ScreenRect(
            left = widened.left + 3f,
            top = widened.top + 2f,
            right = widened.right - 3f,
            bottom = widened.bottom - 2f
        )

        val sampled = if (settings.plateStyle == ScreenPlateStyle.SAMPLED) {
            ScreenColorMath.averageColor(sampler, plateRect)
        } else {
            null
        }
        val plate = ScreenColorMath.plateColor(settings.plateStyle, settings.customPlateColor, sampled, alpha)
        val ink = ScreenColorMath.textColor(settings.textColorMode, plate, settings.customTextColor)

        return PlannedRegion(
            sourceText = sourceText,
            persianText = rendered,
            provenance = provenance,
            bounds = widened,
            textBounds = finalTextRect,
            fontSizePx = baseFont,
            plateColor = plate,
            textColor = ink,
            lines = max(1, estimateLineCount(rendered, baseFont, widened.width))
        )
    }

    /** Estimated rendered width of [text] at [fontSizePx]. */
    fun estimateTextWidth(text: String, fontSizePx: Float, persian: Boolean = true): Float {
        val advance = if (persian) PERSIAN_ADVANCE else LATIN_ADVANCE
        var width = 0f
        for (ch in text) {
            width += when {
                ch == ' ' -> fontSizePx * 0.30f
                ch == '\t' -> fontSizePx * 1.2f
                ch.isDigit() -> fontSizePx * 0.55f
                ch == '.' || ch == ',' || ch == '\'' || ch == ':' || ch == ';' || ch == '|' || ch == '!' -> fontSizePx * 0.26f
                ch == 'ی' || ch == 'ي' || ch == 'ب' || ch == 'ت' || ch == 'ن' -> fontSizePx * advance
                else -> fontSizePx * advance
            }
        }
        return width
    }

    /** How many wrapped lines [text] occupies inside [availableWidth]. */
    fun estimateLineCount(text: String, fontSizePx: Float, availableWidth: Float): Int {
        if (availableWidth <= 0f) return 1
        val total = estimateTextWidth(text, fontSizePx)
        return max(1, (total / availableWidth).toInt() + 1)
    }

    /**
     * Grows a rectangle left and right — keeping its centre — until [neededWidth] fits,
     * stopping at the screen edges. The overlay may therefore cover a little more than the
     * original text, which is exactly what keeps a single-line translation on the same line
     * as the word it replaces.
     */
    fun widenToFit(rect: ScreenRect, neededWidth: Float, displayWidth: Float): ScreenRect {
        if (rect.width >= neededWidth || rect.width <= 0f) return rect
        val half = (neededWidth - rect.width) / 2f
        var left = rect.left - half
        var right = rect.right + half
        if (left < 0f) {
            right = min(displayWidth, right - left)
            left = 0f
        }
        if (right > displayWidth) {
            left = max(0f, left - (right - displayWidth))
            right = displayWidth
        }
        return ScreenRect(left, rect.top, right, rect.bottom)
    }

    // --------------------------------------------------------- frame level helpers

    /**
     * Perceptual fingerprint of a luminance grid (row-major, [gridSize] x [gridSize]).
     * Two consecutive frames with the same fingerprint are skipped before OCR, which is
     * what keeps a *permanent* translator from eating the battery.
     */
    fun fingerprint(grid: IntArray): Long {
        var hash = 0xcbf29ce484222325UL
        for (value in grid) {
            hash = hash xor (value.toLong().toULong() and 0xFFUL)
            hash *= 0x100000001b3UL
        }
        return hash.toLong()
    }

    /**
     * A screen showing protected content (`FLAG_SECURE`) or a powered-off display comes
     * back as a uniformly black frame. Detecting it lets SAYVIS report "nothing to read"
     * instead of pretending the screen has no text.
     */
    fun looksLikeBlankFrame(grid: IntArray): Boolean {
        if (grid.isEmpty()) return true
        var dark = 0
        var total = 0
        for (value in grid) {
            total++
            if (value <= 6) dark++
        }
        return total > 0 && dark.toFloat() / total.toFloat() >= 0.985f
    }

    /** Average luminance of a grid, 0..255 (used for the "very dark app" heuristic). */
    fun averageLuminance(grid: IntArray): Int {
        if (grid.isEmpty()) return 0
        var sum = 0L
        for (value in grid) sum += value
        return (sum / grid.size).toInt().coerceIn(0, 255)
    }

    /** Difference between two grids, 0 (identical) .. 255 (opposite). */
    fun gridDistance(a: IntArray, b: IntArray): Int {
        if (a.isEmpty() || a.size != b.size) return 255
        var sum = 0
        for (index in a.indices) sum += abs(a[index] - b[index])
        return (sum / a.size).coerceIn(0, 255)
    }

    private val TECHNICAL = Regex("^[A-Za-z0-9]+(?:[_.=/:#-][A-Za-z0-9]+)+$")
    private val URL = Regex("(?i)^(https?://|www\\.)\\S+$")
    private val EMAIL = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
    private val HANDLE = Regex("^@[A-Za-z0-9_.]+$")
    private val HASHTAG_ONLY = Regex("^#[A-Za-z0-9_]+$")
    private val DIGIT_HEAVY = Regex("^[\\d\\s.,:;%/+\\-×*()\\[\\]]+$")
}

/** Reads the colour of the screen underneath a rectangle; implemented by the capture layer. */
fun interface PixelSampler {
    /** ARGB colour at a **display-space** point, or null when it cannot be read. */
    fun colorAt(x: Float, y: Float): Int?
}

/**
 * Colour decisions for the plates and the Persian glyphs.
 *
 * Matching the plate to the pixels it covers is what makes the translation look like the
 * app's own text rather than a sticker pasted over it, while the ink is always chosen for
 * contrast against that plate.
 */
object ScreenColorMath {

    const val LIGHT_INK = 0xFFF4F7FB.toInt()
    const val DARK_INK = 0xFF10151C.toInt()

    /** Adds an alpha channel to a packed RGB colour. */
    fun withAlpha(rgb: Int, alpha: Int): Int =
        (rgb and 0x00FFFFFF) or ((alpha.coerceIn(0, 255)) shl 24)

    /** Relative luminance (0..1) of a packed RGB colour. */
    fun luminance(rgb: Int): Double {
        val r = ((rgb shr 16) and 0xFF) / 255.0
        val g = ((rgb shr 8) and 0xFF) / 255.0
        val b = (rgb and 0xFF) / 255.0
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    /** Colour of the text drawn on [rgb]; returns a dark or light ink. */
    fun inkFor(rgb: Int): Int = if (luminance(rgb) > 0.58) DARK_INK else LIGHT_INK

    /**
     * Average colour of the pixels behind [rect], sampled on a small grid. Returns null
     * when no sampler is available or every sample failed.
     */
    fun averageColor(sampler: PixelSampler?, rect: ScreenRect, grid: Int = 5): Int? {
        if (sampler == null || !rect.isValid) return null
        var r = 0L
        var g = 0L
        var b = 0L
        var hits = 0
        for (row in 0 until grid) {
            for (column in 0 until grid) {
                val x = rect.left + rect.width * (column + 0.5f) / grid
                val y = rect.top + rect.height * (row + 0.5f) / grid
                val color = runCatching { sampler.colorAt(x, y) }.getOrNull() ?: continue
                r += (color shr 16) and 0xFF
                g += (color shr 8) and 0xFF
                b += color and 0xFF
                hits++
            }
        }
        if (hits == 0) return null
        return (((r / hits).toInt() and 0xFF) shl 16) or
            (((g / hits).toInt() and 0xFF) shl 8) or
            ((b / hits).toInt() and 0xFF)
    }

    /** Final ARGB plate colour for the chosen style. */
    fun plateColor(
        style: ScreenPlateStyle,
        customColor: Int,
        sampled: Int?,
        alpha: Int
    ): Int {
        val base = when (style) {
            ScreenPlateStyle.SAMPLED -> sampled ?: 0x0B0F14
            ScreenPlateStyle.DARK -> 0x0B0F14
            ScreenPlateStyle.LIGHT -> 0xF2F4F8
            ScreenPlateStyle.CUSTOM -> customColor
        }
        return withAlpha(base, alpha)
    }

    /** Final ARGB ink colour for the chosen mode. */
    fun textColor(mode: ScreenTextColorMode, plateArgb: Int, customColor: Int): Int = when (mode) {
        ScreenTextColorMode.AUTO -> inkFor(plateArgb)
        ScreenTextColorMode.LIGHT -> withAlpha(LIGHT_INK, 255)
        ScreenTextColorMode.DARK -> withAlpha(DARK_INK, 255)
        ScreenTextColorMode.CUSTOM -> withAlpha(customColor, 255)
    }
}
