package com.example.sayvis.ai

import com.example.sayvis.model.UicCategory

/**
 * SAYVIS v5.3.0 — COGNITIVE FILE AUTO-UPDATER (پروندهٔ شناختی زنده)
 *
 * Turns the owner's real activity into cognitive-file attributes:
 *   • searches performed through SAYVIS (web/assistant/GitHub),
 *   • quick notes the owner writes in the app (and shares into it),
 *   • alarms/timers the owner commands SAYVIS to set,
 *   • commands and missions executed by the assistant.
 *
 * PURE rules (unit-tested): deterministic keyword/category extraction with
 * conservative confidence (0.4, status OBSERVED) — never fabricated facts.
 * The owner can confirm/revoke every auto attribute on the UIC screen.
 */
object CognitiveIngest {

    enum class Source(val fa: String, val en: String) {
        SEARCH("جست‌وجو", "Search"),
        NOTE("یادداشت", "Note"),
        ALARM("آلارم", "Alarm"),
        COMMAND("فرمان", "Command"),
        MISSION("مأموریت", "Mission")
    }

    data class Extracted(
        val category: UicCategory,
        val key: String,
        val titleFa: String,
        val titleEn: String,
        val value: String,
        val confidence: Float = 0.4f
    )

    private val FA_STOPWORDS = setOf(
        "از", "به", "با", "که", "این", "آن", "را", "برای", "است", "بود", "یک", "های", "می", "و", "در", "هم", "کن", "کنید"
    )
    private val EN_STOPWORDS = setOf(
        "the", "a", "an", "of", "to", "in", "for", "and", "or", "is", "are", "was", "with", "on", "at", "my", "me"
    )

    /** Up to 3 meaningful keywords from free text (fa/en, digits stripped). */
    fun keywords(text: String): List<String> {
        val raw = text.lowercase()
            .replace(Regex("[\\p{Punct}؟،؛!؟]+"), " ")
            .split(Regex("\\s+"))
            .filter { it.length >= 3 && !it.all(Char::isDigit) }
        return raw.filter { it !in FA_STOPWORDS && it !in EN_STOPWORDS }.distinct().take(3)
    }

    /**
     * Extract cognitive attributes from one captured activity.
     * Returns 0..2 items — small, focused, mergeable.
     */
    fun extract(source: Source, text: String): List<Extracted> {
        val clean = text.trim()
        if (clean.isBlank()) return emptyList()
        val out = ArrayList<Extracted>(2)

        when (source) {
            Source.ALARM -> {
                out += Extracted(
                    UicCategory.HABITS, "alarm_pattern",
                    "الگوی یادآورها", "Reminder pattern",
                    clean.take(120), 0.45f
                )
            }
            Source.SEARCH -> {
                val kw = keywords(clean)
                if (kw.isNotEmpty()) {
                    out += Extracted(
                        UicCategory.PREFERENCES, "search_interest",
                        "علایق جست‌وجو", "Search interests",
                        kw.joinToString("، "), 0.4f
                    )
                }
            }
            Source.NOTE -> {
                val lower = clean.lowercase()
                when {
                    listOf("دوست دارم", "علاقه", "ترجیح", "بشم", "i like", "prefer", "love").any { lower.contains(it) } ->
                        out += Extracted(
                            UicCategory.PREFERENCES, "note_preference",
                            "ترجیح ثبت‌شده", "Noted preference", clean.take(140)
                        )
                    listOf("هر روز", "معمولا", "همیشه", "روزی یک", "always", "every day", "usually").any { lower.contains(it) } ->
                        out += Extracted(
                            UicCategory.HABITS, "note_habit",
                            "عادت مشاهده‌شده", "Observed habit", clean.take(140)
                        )
                    listOf("هدف", "می‌خواهم", "میخوام", "برنامه", "قصد", "goal", "want to", "plan to").any { lower.contains(it) } ->
                        out += Extracted(
                            UicCategory.GOALS, "note_goal",
                            "هدف ثبت‌شده", "Noted goal", clean.take(140)
                        )
                    else -> out += Extracted(
                        UicCategory.WORKING_PATTERNS, "note_freeform",
                        "یادداشت آزاد", "Free-form note", clean.take(140)
                    )
                }
            }
            Source.COMMAND -> {
                out += Extracted(
                    UicCategory.INTERACTION_STYLE, "command_pattern",
                    "الگوی فرمان‌ها", "Command pattern",
                    clean.take(120)
                )
            }
            Source.MISSION -> {
                out += Extracted(
                    UicCategory.GOALS, "mission_goal",
                    "هدف مأموریت", "Mission goal",
                    clean.take(140), 0.5f
                )
            }
        }
        return out
    }

    /** Stable merge key so repeated captures update one attribute, not spam. */
    fun mergeKey(source: Source, extractedKey: String, dayBucket: Long): String =
        "auto_${source.name.lowercase()}_${extractedKey}_$dayBucket"

    /** 1 bucket per day — keeps daily patterns fresh without duplicates. */
    fun dayBucket(now: Long = System.currentTimeMillis()): Long = now / 86_400_000L
}
