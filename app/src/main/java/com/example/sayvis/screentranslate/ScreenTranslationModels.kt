package com.example.sayvis.screentranslate

import com.example.sayvis.ai.TranslationSource

/**
 * PUR — Permanent Universal Reader.
 *
 * Owner-facing name: **مترجم زندهٔ صفحه** / **Live Screen Translator**.
 *
 * This file holds the *language-neutral* half of the feature: nothing here touches
 * Android, the network or the file system, so the whole planning pipeline (geometry,
 * line assembly, filter rules, colour decisions) is unit-testable on the JVM and can be
 * reasoned about without a device.
 *
 * The runtime half lives in:
 *  - [ScreenCaptureController]  – MediaProjection → frames
 *  - [ScreenTextRecognizer]     – frames → English words with bounding boxes (ML Kit, offline)
 *  - [ScreenTranslationEngine]  – words → Persian (dictionary → cache → AI provider)
 *  - [ScreenOverlayHost]        – plan → pixels drawn exactly on top of the original text
 *  - [ScreenTranslatorService]  – the permanent foreground service that drives all of it
 */

/**
 * Axis-aligned rectangle expressed in **display pixels** (the coordinate space of the
 * screen the owner is looking at), never in the coordinate space of a downscaled frame.
 * Conversion happens once, at the edge of the capture pipeline.
 */
data class ScreenRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
    val area: Float get() = (width * height).coerceAtLeast(0f)
    val isValid: Boolean get() = right > left && bottom > top

    fun scaled(factor: Float): ScreenRect = ScreenRect(
        left * factor,
        top * factor,
        right * factor,
        bottom * factor
    )

    fun translate(dx: Float, dy: Float): ScreenRect =
        ScreenRect(left + dx, top + dy, right + dx, bottom + dy)

    /** Grows the rectangle outwards on every side. */
    fun expanded(dx: Float, dy: Float = dx): ScreenRect =
        ScreenRect(left - dx, top - dy, right + dx, bottom + dy)

    fun union(other: ScreenRect): ScreenRect = ScreenRect(
        minOf(left, other.left),
        minOf(top, other.top),
        maxOf(right, other.right),
        maxOf(bottom, other.bottom)
    )

    fun intersect(other: ScreenRect): ScreenRect {
        val l = maxOf(left, other.left)
        val t = maxOf(top, other.top)
        val r = minOf(right, other.right)
        val b = minOf(bottom, other.bottom)
        return if (r <= l || b <= t) ScreenRect(l, t, l, t) else ScreenRect(l, t, r, b)
    }

    fun contains(x: Float, y: Float): Boolean = x in left..right && y in top..bottom

    /** Clamps the rectangle inside a [width] x [height] viewport. */
    fun clampTo(width: Float, height: Float): ScreenRect {
        val w = width.coerceAtMost(this.width)
        val h = height.coerceAtMost(this.height)
        val l = left.coerceIn(0f, (width - w).coerceAtLeast(0f))
        val t = top.coerceIn(0f, (height - h).coerceAtLeast(0f))
        return ScreenRect(l, t, l + w, t + h)
    }

    /** Height of the vertical overlap with [other], in pixels (0 when they do not touch). */
    fun verticalOverlap(other: ScreenRect): Float =
        (minOf(bottom, other.bottom) - maxOf(top, other.top)).coerceAtLeast(0f)

    /** Horizontal distance between two rectangles; 0 when they overlap or touch. */
    fun horizontalGap(other: ScreenRect): Float =
        (maxOf(left, other.left) - minOf(right, other.right)).coerceAtLeast(0f)

    override fun toString(): String =
        "(${left.toInt()},${top.toInt()})-(${right.toInt()},${bottom.toInt()})"

    companion object {
        val EMPTY: ScreenRect = ScreenRect(0f, 0f, 0f, 0f)

        fun of(left: Int, top: Int, right: Int, bottom: Int): ScreenRect =
            ScreenRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
    }
}

/**
 * One unit of recognised text, already mapped into display pixels.
 *
 * [word] fragments are produced by ML Kit per word; [ScreenTextPlanner.groupIntoLines]
 * stitches them back into readable lines, because a line is what a human reads and what
 * a translator must see to produce a correct sentence.
 */
data class RecognizedFragment(
    val text: String,
    val bounds: ScreenRect,
    val word: Boolean = true
)

/** A logical line of on-screen text assembled from one or more fragments. */
data class ScreenTextLine(
    val text: String,
    val bounds: ScreenRect,
    val fragments: List<RecognizedFragment>
) {
    /** Rough character height, used to seed the Persian font-size search. */
    val characterHeight: Float get() = bounds.height
}

/** How much of the screen the overlay is allowed to rewrite. */
enum class ScreenTranslationMode(val labelFa: String, val labelEn: String) {
    /** The original English is covered and the Persian is written exactly in its place. */
    REPLACE("جایگزینی دقیق سر جای متن", "Replace exactly in place"),

    /** The English stays visible and the Persian is written just underneath it. */
    CAPTION("نوشتن ترجمه زیر متن اصلی", "Caption under the original"),

    /** Nothing is covered; only a soft plate behind the translated line. */
    HIGHLIGHT("فقط برجسته‌سازی ترجمه", "Highlight the translation only");

    fun label(isPersian: Boolean): String = if (isPersian) labelFa else labelEn
}

/** Translation unit: whole lines read better, single words cover dense interfaces. */
enum class ScreenTranslationGranularity(val labelFa: String, val labelEn: String) {
    LINE("خط کامل (کیفیت بالاتر)", "Full line (best quality)"),
    WORD("کلمه به کلمه (پوشش بیشتر)", "Word by word (best coverage)"),
    AUTO("خودکار (خط، با بازگشت به کلمه)", "Automatic (line, falling back to word)");

    fun label(isPersian: Boolean): String = if (isPersian) labelFa else labelEn
}

/** Where the plate colour behind the Persian text comes from. */
enum class ScreenPlateStyle(val labelFa: String, val labelEn: String) {
    /** Average colour of the underlying pixels — the text looks native. */
    SAMPLED("هم‌رنگ با پس‌زمینهٔ خودِ برنامه", "Matched to the app background"),

    /** Fixed dark plate. */
    DARK("پلاک تیره", "Dark plate"),

    /** Fixed light plate. */
    LIGHT("پلاک روشن", "Light plate"),

    /** Owner-chosen colour. */
    CUSTOM("رنگ دلخواه", "Custom colour");

    fun label(isPersian: Boolean): String = if (isPersian) labelFa else labelEn
}

/** Colour of the Persian glyphs. */
enum class ScreenTextColorMode(val labelFa: String, val labelEn: String) {
    /** Automatically the opposite of the plate, so it is always legible. */
    AUTO("خودکار (کنتراست با پلاک)", "Automatic (contrast with plate)"),
    LIGHT("روشن", "Light"),
    DARK("تیره", "Dark"),
    CUSTOM("رنگ دلخواه", "Custom colour");

    fun label(isPersian: Boolean): String = if (isPersian) labelFa else labelEn
}

/** Lifecycle of the translation service, surfaced live in the app. */
enum class ScreenTranslatePhase(val labelFa: String, val labelEn: String) {
    IDLE("آماده به کار", "Idle"),
    AWAITING_CONSENT("در انتظار اجازهٔ ضبط صفحه", "Waiting for screen-capture consent"),
    STARTING("در حال راه‌اندازی", "Starting up"),
    RUNNING("فعال — صفحه در حال ترجمه است", "Active — translating the screen"),
    PAUSED("موقتاً متوقف", "Paused"),
    BLOCKED("مسدود (قفل اضطراری)", "Blocked (emergency lock)"),
    ERROR("خطا", "Error"),
    STOPPED("متوقف شده", "Stopped");

    fun label(isPersian: Boolean): String = if (isPersian) labelFa else labelEn
}

/**
 * Everything the owner can tune. Persisted on device by `SettingsStore` and mirrored to
 * the service through a `StateFlow`, so changing a slider is visible on the overlay the
 * very next frame.
 */
data class ScreenTranslationSettings(
    /** The owner has armed the feature (permission flow, bubble and settings surface). */
    val enabled: Boolean = false,

    val mode: ScreenTranslationMode = ScreenTranslationMode.REPLACE,
    val granularity: ScreenTranslationGranularity = ScreenTranslationGranularity.LINE,

    /** Text is written with Persian digits (۰-۹) inside the translation. */
    val persianNumbers: Boolean = true,

    val plateStyle: ScreenPlateStyle = ScreenPlateStyle.SAMPLED,
    val plateOpacityPercent: Int = 92,
    val customPlateColor: Int = 0x1B2430,
    val textColorMode: ScreenTextColorMode = ScreenTextColorMode.AUTO,
    val customTextColor: Int = 0xF4F7FB,
    val textScalePercent: Int = 100,

    /** Floating bubble: tap to pause, long-press to stop. */
    val showControlBubble: Boolean = true,
    val bubbleOpacityPercent: Int = 80,

    /** How often a frame is captured and sent to OCR (lower = snappier, hotter). */
    val pollIntervalMs: Int = 700,

    /** Hard ceiling per frame, so a wall of text can never stall the pipeline. */
    val maxSegmentsPerFrame: Int = 48,

    /** How many segments are packed into one AI request. */
    val maxSegmentsPerRequest: Int = 20,

    /** Words shorter than this are never treated as translatable prose. */
    val minWordLength: Int = 2,

    /** Offline dictionary only — the owner's text never leaves the device. */
    val dictionaryOnly: Boolean = false,

    /** Reuse translations across sessions. */
    val cacheTranslations: Boolean = true,

    /** Keep the display awake while translating. */
    val keepScreenOn: Boolean = false,

    /** Offer to resume from the notification after a reboot. */
    val resumeAfterBoot: Boolean = false,

    /** On Android 14+: let the owner pick a single app window instead of the whole screen. */
    val preferSingleAppCapture: Boolean = false,

    /** Pause automatically while the owner is inside SAYVIS itself. */
    val skipOwnApp: Boolean = true,

    val settingsRevision: Int = 1
) {
    fun boundedPollInterval(): Long = pollIntervalMs.coerceIn(MIN_POLL_MS, MAX_POLL_MS).toLong()

    fun plateAlpha(): Int = (plateOpacityPercent.coerceIn(0, 100) * 255 / 100)

    fun bubbleAlpha(): Int = (bubbleOpacityPercent.coerceIn(20, 100) * 255 / 100)

    fun textScale(): Float = textScalePercent.coerceIn(60, 170) / 100f

    companion object {
        const val MIN_POLL_MS = 250
        const val MAX_POLL_MS = 4000
    }
}

/** Outcome of translating one segment. */
data class TranslatedSegment(
    val sourceText: String,
    val persianText: String,
    val provenance: TranslationSource
) {
    val isMachine: Boolean
        get() = provenance == TranslationSource.MACHINE || provenance == TranslationSource.CACHE
}

/**
 * One rectangle of the overlay plan: what to cover, what to write, and how it should
 * look. Coordinates are display pixels; colours are ARGB.
 */
data class PlannedRegion(
    val sourceText: String,
    val persianText: String,
    val provenance: TranslationSource,
    /** Rectangle that is covered by the plate (may be wider than the English original). */
    val bounds: ScreenRect,
    /** Where the Persian baseline block starts. */
    val textBounds: ScreenRect,
    val fontSizePx: Float,
    val plateColor: Int,
    val textColor: Int,
    val lines: Int = 1
) {
    val isMachine: Boolean
        get() = provenance == TranslationSource.MACHINE || provenance == TranslationSource.CACHE
}

/** Everything the renderer needs for one captured frame. */
data class FrameTranslationPlan(
    val regions: List<PlannedRegion> = emptyList(),
    val detectedFragments: Int = 0,
    val candidateSegments: Int = 0,
    val skippedFragments: Int = 0,
    val cachedHits: Int = 0,
    val dictionaryHits: Int = 0,
    val machineHits: Int = 0,
    val frameFingerprint: Long = 0L,
    val capturedAt: Long = 0L,
    val displayWidth: Int = 0,
    val displayHeight: Int = 0,
    val secureContentSuspected: Boolean = false
) {
    val isEmpty: Boolean get() = regions.isEmpty()
}

/** Live status of the permanent service. Read by the screen and the home dashboard. */
data class ScreenTranslateStatus(
    val phase: ScreenTranslatePhase = ScreenTranslatePhase.IDLE,
    val overlayPermissionGranted: Boolean = false,
    val captureConsentGranted: Boolean = false,
    val engineOnline: Boolean = false,
    val dictionaryOnly: Boolean = false,
    val runningSince: Long = 0L,
    val framesCaptured: Int = 0,
    val framesSkipped: Int = 0,
    val segmentsTranslated: Int = 0,
    val segmentsSkipped: Int = 0,
    val lastOcrMillis: Long = 0L,
    val lastErrorFa: String? = null,
    val lastErrorEn: String? = null,
    /** Number of frames whose content differed enough to be worth reading again. */
    val frameFingerprints: Int = 0,
    /** True when the last read frame contained nothing translatable. */
    val lastPlanEmpty: Boolean = true,
    /** True when the frame came back almost entirely black — protected content. */
    val secureContentSuspected: Boolean = false,
    val recentSegments: List<TranslatedSegment> = emptyList(),
    val lastPlan: FrameTranslationPlan? = null
) {
    /** True while the service owns the screen-capture session. */
    val isSessionActive: Boolean
        get() = phase == ScreenTranslatePhase.RUNNING ||
            phase == ScreenTranslatePhase.PAUSED ||
            phase == ScreenTranslatePhase.STARTING

    fun message(isPersian: Boolean): String? = if (isPersian) lastErrorFa else lastErrorEn

    fun uptimeMillis(now: Long = System.currentTimeMillis()): Long =
        if (runningSince <= 0L) 0L else (now - runningSince).coerceAtLeast(0L)
}

/**
 * Snapshot of the last processed frame plus running counters, kept in a process-wide
 * observable so both the Compose screen and the overlay renderer read the same truth.
 */
data class ScreenTranslationStats(
    val frames: Int = 0,
    val translatedSegments: Int = 0,
    val dictionarySegments: Int = 0,
    val machineSegments: Int = 0,
    val skippedSegments: Int = 0,
    val cacheHits: Int = 0
) {
    fun plus(plan: FrameTranslationPlan): ScreenTranslationStats = copy(
        frames = frames + 1,
        translatedSegments = translatedSegments + plan.regions.size,
        dictionarySegments = dictionarySegments + plan.dictionaryHits,
        machineSegments = machineSegments + plan.machineHits,
        skippedSegments = skippedSegments + plan.skippedFragments,
        cacheHits = cacheHits + plan.cachedHits
    )
}
