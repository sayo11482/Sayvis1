package com.example.sayvis.agent

/**
 * SAYVIS v5.3.1 — AUTO MISSION PLANNER (مغز خالص ایجنت اجراگر)
 *
 * The owner gives ONE natural-language command — «برو ۵ موزیک ملایم پرمخاطب
 * پیدا کن و دانلود کن» — and this object decomposes it into an EXECUTABLE
 * plan. No manual inputs, no manual ticking: every step carries a
 * completion predicate that the executor satisfies with REAL work
 * (searching, parsing, downloading) and the task ticks itself only when
 * the predicate is actually true.
 *
 * Pure JVM code (unit-tested): regex parsers, no android.* imports, no
 * org.json (regex field extraction like MarketDataService — JVM-stable).
 */
object AutoMission {

    enum class Pack { MUSIC, GENERIC }

    enum class StepKind { SEARCH, COLLECT, DOWNLOAD, VERIFY, REPORT }

    data class PlanStep(
        val kind: StepKind,
        val titleFa: String,
        val titleEn: String,
        /** Search query / topic for SEARCH-COLLECT steps. */
        val query: String = "",
        /** Target metric that auto-ticks this step (sources, items, files…). */
        val target: Int = 1,
        /** Index of the matching MissionTask created for this step. */
        val taskId: String = ""
    ) {
        fun title(persian: Boolean): String = if (persian) titleFa else titleEn
    }

    data class Plan(
        val pack: Pack,
        val goal: String,
        val targetCount: Int,
        val wantsDownload: Boolean,
        val steps: List<PlanStep>
    )

    data class MusicHit(
        val identifier: String,
        val title: String,
        val downloads: Long
    )

    // ------------------------------------------------------------ NL parsing

    private val DOWNLOAD_WORDS = listOf("دانلود", "دانلد", "download", "save file", "فایلشو", "فایلش را", "فایل ها رو", "فایلها را")
    private val MUSIC_WORDS = listOf(
        "موزیک", "موسیقی", "اهنگ", "آهنگ", "ملایم", "ارام", "آرام", "بی کلام", "music", "song", "track", "relax", "calm", "lofi", "piano"
    )

    /** First explicit count 1..99 in the goal («۵ موزیک» / «find 7 songs»). */
    fun extractCount(goal: String): Int? {
        val ascii = AssistantDigitBridge.toEnglish(goal)
        val m = Regex("(?<![0-9:])([1-9][0-9]?)\\s*(?:تا|عدد|مورد|نفر|song|track|music|item|file|موزیک|اهنگ|آهنگ|فایل)?").find(ascii)
        val v = m?.groupValues?.get(1)?.toIntOrNull()
        return v?.takeIf { it in 1..99 }
    }

    fun wantsDownload(goal: String): Boolean {
        val n = AssistantDigitBridge.normalize(goal)
        return DOWNLOAD_WORDS.any { n.contains(it) }
    }

    fun detectPack(goal: String): Pack {
        val n = AssistantDigitBridge.normalize(goal)
        return if (MUSIC_WORDS.any { n.contains(it) }) Pack.MUSIC else Pack.GENERIC
    }

    /**
     * The one-call planner. Always: SEARCH → COLLECT → [DOWNLOAD → VERIFY] →
     * REPORT. Each step's [PlanStep.target] is the honest auto-tick bar.
     */
    fun plan(goal: String): Plan {
        val clean = goal.trim()
        val pack = detectPack(clean)
        val count = extractCount(clean) ?: 5
        val download = wantsDownload(clean)
        val steps = ArrayList<PlanStep>(5)

        steps += PlanStep(
            StepKind.SEARCH,
            "جست‌وجوی زندهٔ منابع (بینگ/داک‌داک‌گو/آرشیو آزاد)",
            "Live source search (Bing/DDG/free archive)",
            query = clean.take(80),
            target = 1
        )
        steps += PlanStep(
            StepKind.COLLECT,
            "انتخاب $count مورد برتر بر اساس محبوبیت واقعی",
            "Pick the top $count items by real popularity",
            query = clean.take(80),
            target = count
        )
        if (download) {
            steps += PlanStep(
                StepKind.DOWNLOAD,
                "دانلود $count فایل (بدون نیاز به اکانت، از منبع آزاد)",
                "Download $count files (keyless, from the free source)",
                target = count
            )
            steps += PlanStep(
                StepKind.VERIFY,
                "راستی‌آزمایی: هر فایل سالم و بزرگ‌تر از ۱۰ کیلوبایت",
                "Verify: every file intact and larger than 10 KB",
                target = count
            )
        }
        steps += PlanStep(
            StepKind.REPORT,
            "گزارش نهایی با مسیر فایل‌ها و ثبت در پروندهٔ شناختی",
            "Final report with file paths + cognitive file entry",
            target = 1
        )
        return Plan(pack, clean, count, download, steps)
    }

    /** The auto-tick predicate: a step is DONE only when metric reaches target. */
    fun autoTick(step: PlanStep, metric: Int): Boolean = metric >= step.target.coerceAtLeast(1)

    // ------------------------------------------------- archive.org (free music)

    /**
     * Keyless archive.org advancedsearch query for the MUSIC pack, sorted by
     * REAL popularity (downloads desc). Free/public collections only — this
     * is the honest, legal way to "download the files".
     */
    fun archiveSearchUrl(subjectTerms: List<String>, count: Int): String {
        val subject = subjectTerms.take(4).joinToString(" OR ") { "subject:\"$it\"" }
        val q = "(collection:(audio_music)) AND mediatype:(audio) AND ($subject)"
        return "https://archive.org/advancedsearch.php?q=" +
            java.net.URLEncoder.encode(q, "UTF-8") +
            "&fl%5B%5D=identifier&fl%5B%5D=title&fl%5B%5D=downloads" +
            "&sort%5B%5D=downloads+desc&rows=" + (count * 4).coerceAtMost(40) +
            "&page=1&output=json"
    }

    /** Default subject terms for soft/relaxing music (fa words mapped to en). */
    fun musicSubjects(goal: String): List<String> {
        val n = AssistantDigitBridge.normalize(goal)
        val terms = linkedSetOf("relaxing", "calm", "soft")
        if (n.contains("پیانو") || n.contains("piano")) terms += "piano"
        if (n.contains("lofi") || n.contains("لو فای")) terms += "lo-fi"
        if (n.contains("خواب") || n.contains("sleep")) terms += "sleep"
        return terms.toList()
    }

    /**
     * Parses the advancedsearch JSON with regexes (JVM-testable), sorts by
     * downloads DESC and keeps [count] hits.
     */
    fun pickMusicItems(json: String, count: Int): List<MusicHit> {
        if (json.isBlank()) return emptyList()
        val docRegex = Regex("\\{[^{}]*\\}")
        val idRegex = Regex("\"identifier\"\\s*:\\s*\"([^\"]+)\"")
        val titleRegex = Regex("\"title\"\\s*:\\s*\"([^\"]+)\"")
        val dlRegex = Regex("\"downloads\"\\s*:\\s*([0-9]+)")
        val hits = ArrayList<MusicHit>()
        docRegex.findAll(json).forEach { m ->
            val blob = m.value
            val id = idRegex.find(blob)?.groupValues?.get(1) ?: return@forEach
            val title = titleRegex.find(blob)?.groupValues?.get(1) ?: id
            val dl = dlRegex.find(blob)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
            hits += MusicHit(id, title.replace("\\u0026", "&"), dl)
        }
        return hits.distinctBy { it.identifier }.sortedByDescending { it.downloads }.take(count)
    }

    /**
     * From the archive.org metadata JSON of one item, picks the first real
     * MP3 and returns its direct download URL (https://archive.org/download/…).
     */
    fun pickAudioFile(metadataJson: String, identifier: String): String? {
        val nameRegex = Regex("\"name\"\\s*:\\s*\"([^\"]+)\"")
        val names = nameRegex.findAll(metadataJson).mapNotNull { m ->
            m.groupValues.getOrNull(1)
        }.toList()
        val mp3 = names.firstOrNull { it.lowercase().endsWith(".mp3") }
            ?: names.firstOrNull { it.lowercase().endsWith(".flac") }
            ?: names.firstOrNull { it.lowercase().endsWith(".ogg") }
            ?: return null
        val encoded = java.net.URLEncoder.encode(mp3, "UTF-8").replace("+", "%20")
        return "https://archive.org/download/$identifier/$encoded"
    }

    /** True for URLs that point at a downloadable file rather than a web page. */
    fun isDirectFileUrl(url: String): Boolean =
        Regex("\\.(mp3|flac|ogg|wav|pdf|zip|epub|txt|png|jpe?g)(\\?|#|$)", RegexOption.IGNORE_CASE)
            .containsMatchIn(url)

    /** Human size (KB/MB) for progress lines. */
    fun humanSize(bytes: Long): String = when {
        bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
        bytes >= 1_024 -> "%.0f KB".format(bytes / 1_024.0)
        else -> "$bytes B"
    }
}

/**
 * Tiny bridge so the pure planner reuses the SAME normalisation/digit rules
 * as the assistant command engine (kept indirection-free for tests).
 */
internal object AssistantDigitBridge {
    fun toEnglish(raw: String): String = com.example.sayvis.ai.AssistantCommandEngine.toEnglishDigits(raw)
    fun normalize(raw: String): String = com.example.sayvis.ai.AssistantCommandEngine.normalize(raw)
}
