package com.example.sayvis.ai

/**
 * Search-taste engine (v5.0.0) — the Smart-Suggestions sports module.
 *
 * The owner asked for sports suggestions (calisthenics & tennis) that learn
 * from their taste. Google's private search history is not accessible to any
 * app, so the engine works on the signals SAYVIS genuinely has:
 *
 *  1. every web search performed inside SAYVIS (the last 100 are persisted);
 *  2. an optional import: the owner pastes their recent Google activity text
 *     (myactivity.google.com → copy) and it is ingested the same way.
 *
 * Pure and unit-tested: tokenising, interest weighting against a Persian +
 * English lexicon, and the mapping of weighted interests to a concrete
 * weekly training plan.
 */
object SearchTasteEngine {

    /** Hard cap that mirrors the owner's "based on my recent 100 searches". */
    const val MAX_HISTORY: Int = 100

    data class Interest(val key: String, val weight: Int)

    data class Suggestion(
        val title: String,
        val body: String,
        val tag: String,
        val reason: String
    )

    // ------------------------------------------------------------ lexicons

    private val LEXICON: Map<String, List<String>> = mapOf(
        "calisthenics" to listOf(
            "کالیستنیکس", "بارفیکس", "پارالل", "شنا", "پل", "میله", "وزن بدن",
            "calisthenics", "pull up", "pullup", "push up", "dip", "muscle up", "planche", "handstand"
        ),
        "tennis" to listOf(
            "تنیس", "تennis", "راکت", "کورت", "سرویس تنیس", "فورهند", "بکهند",
            "tennis", "racket", "forehand", "backhand", "atp", "wta", "wimbledon", "roland garros", "us open"
        ),
        "gym" to listOf(
            "باشگاه", "هالتر", "دمبل", "بدنسازی", "فیله", "سینه", "زیربغل", "اسکات", "ددلیفت",
            "gym", "weightlifting", "deadlift", "squat", "bench", "bodybuilding"
        ),
        "running" to listOf(
            "دویدن", "دو", "ماراتن", "اسپرینت", "کاردیو", "تردمیل",
            "running", "marathon", "jogging", "cardio", "treadmill", "5k", "10k"
        ),
        "football" to listOf(
            "فوتبال", "استقلال", "پرسپولیس", "لیگ", "تیم ملی",
            "football", "soccer", "champions league", "premier league", "la liga"
        ),
        "health" to listOf(
            "تغذیه", "پروتئین", "کم‌کاری تیروئید", "خواب", "وزن", "لاغری", "مکمل",
            "nutrition", "protein", "diet", "sleep", "weight loss", "supplement", "calorie"
        )
    )

    private val STOPWORDS = setOf(
        "the", "and", "for", "with", "from", "what", "how", "best", "چگونه", "بهترین", "برای", "چیست",
        "است", "به", "از", "که", "در", "را", "با", "این", "می", "های"
    )

    // ------------------------------------------------------------- history

    /** Adds a query to the history list and caps it at [MAX_HISTORY]. */
    fun appendQuery(history: List<String>, query: String): List<String> {
        val q = query.trim()
        if (q.isEmpty()) return history
        val next = history + q
        return if (next.size > MAX_HISTORY) next.takeLast(MAX_HISTORY) else next
    }

    /** Serialises the history for settings storage (one query per line). */
    fun encodeHistory(history: List<String>): String =
        history.joinToString("\n") { it.replace('\n', ' ').trim() }

    fun decodeHistory(raw: String): List<String> =
        raw.split('\n').map { it.trim() }.filter { it.isNotEmpty() }.takeLast(MAX_HISTORY)

    /** Ingests a pasted "recent Google activity" blob as pseudo-queries. */
    fun ingestImport(blob: String): List<String> =
        blob.split('\n', '.', '،', '؛', ',')
            .map { it.trim() }
            .filter { it.length in 3..120 }
            .takeLast(MAX_HISTORY)

    // ------------------------------------------------------------ weighting

    fun interestsOf(history: List<String>): List<Interest> {
        val counts = HashMap<String, Int>()
        for (query in history) {
            val q = query.lowercase()
            for ((key, needles) in LEXICON) {
                for (needle in needles) {
                    if (q.contains(needle.lowercase())) {
                        counts[key] = (counts[key] ?: 0) + 1
                        break
                    }
                }
            }
        }
        return counts.entries
            .map { Interest(it.key, it.value) }
            .sortedByDescending { it.weight }
    }

    // ---------------------------------------------------------- suggestions

    /**
     * Builds the personalised sports suggestion list. Unmatched owners still
     * get a sensible starter plan so the module is never empty.
     */
    fun sportsSuggestions(history: List<String>, persian: Boolean): List<Suggestion> {
        val interests = interestsOf(history)
        val top = interests.firstOrNull()
        val out = ArrayList<Suggestion>()

        fun has(key: String) = interests.any { it.key == key }

        fun add(title: String, body: String, tag: String, reason: String) {
            out.add(Suggestion(title, body, tag, reason))
        }

        if (has("calisthenics")) {
            add(
                if (persian) "برنامهٔ کالیستنیکس ۳روزه" else "3-day calisthenics plan",
                if (persian) "شنبه: ۵×(۴ بارفیکس + ۸ شنا) • دوشنبه: ۴×(۱۲ اسکات پرشی + ۳۰ثانیه پلانک) • چهارشنبه: ۵×(۶ پارالل + ۱۰ پا بلند). هر هفته ۱ تکرار اضافه کن."
                else "Sat: 5×(4 pull-ups + 8 push-ups) • Mon: 4×(12 jump squats + 30s plank) • Wed: 5×(6 dips + 10 leg raises). Add one rep weekly.",
                "کالیستنیکس",
                if (persian) "چون «کالیستنیکس/بارفیکس» در جستجوهای اخیر شما پررنگ بود" else "Because calisthenics/pull-ups stood out in your recent searches"
            )
        }
        if (has("tennis")) {
            add(
                if (persian) "تمرین تنیس — دیوار و سایه" else "Tennis — wall & shadow drills",
                if (persian) "۱۵دقیقه فورهند مقابل دیوار (۸۰٪ قدرت) + ۱۰دقیقه بکهند + ۱۰دقیقه سرویس با هدف‌گذاری چهار خانه. هفته‌ای ۲ جلسهٔ سایه‌کاری با کش مقاومتی."
                else "15-min forehands on a wall (80% power) + 10-min backhands + 10-min serve targets. Two shadow-swings sessions with a resistance band weekly.",
                "تنیس",
                if (persian) "بر اساس سلیقهٔ تنیسی شما در ۱۰۰ جستجوی اخیر" else "Based on your tennis taste in the recent searches"
            )
        }
        if (has("gym") && !has("calisthenics")) {
            add(
                if (persian) "تقسیم بدنسازی push/pull/legs" else "Push/pull/legs split",
                if (persian) "پوش: سینه+سرشانه+پشت‌بازو • پول: زیربغل+جلو بازو • پا: اسکات+ددلیفت. ۴ ست × ۸تکرار، استراحت ۹۰ثانیه."
                else "Push: chest+shoulders+triceps • Pull: back+biceps • Legs: squat+deadlift. 4×8, 90s rest.",
                "باشگاه",
                if (persian) "چون جستجوهای باشگاهی شما فعال است" else "Because your gym searches are active"
            )
        }
        if (has("running")) {
            add(
                if (persian) "برنامهٔ دویدن ۵کیلومتری" else "5K running plan",
                if (persian) "۳روز در هفته: روز۱ ۲۰دقیقه آهسته، روز۲ اینتروال ۶×(۱دقیقه تند/۲دقیقه آهسته)، روز۳ ۳۰دقیقه پیوسته."
                else "3 days/week: day1 20-min easy, day2 6×(1-min fast/2-min easy) intervals, day3 30-min steady.",
                "دویدن",
                if (persian) "با توجه به علاقهٔ شما به دویدن/کاردیو" else "Given your running/cardio interest"
            )
        }
        if (has("health")) {
            add(
                if (persian) "تغذیهٔ همراه تمرین" else "Training nutrition",
                if (persian) "پروتئین ≈ ۱٫۶گرم/کیلوگرم وزن، ۲ لیتر آب، خواب ۷–۸ساعت؛ در روزهای تمرین کربوهیدرات را به وعدهٔ بعد از تمرین منتقل کن."
                else "Protein ≈ 1.6g/kg, 2L water, 7–8h sleep; shift carbs to the post-workout meal on training days.",
                "سلامت",
                if (persian) "چون جستجوهای تغذیه/سلامت شما پرتکرار است" else "Because your nutrition/health searches repeat"
            )
        }
        if (has("football")) {
            add(
                if (persian) "آمادگی فوتبالی هفتگی" else "Weekly football conditioning",
                if (persian) "۲ جلسهٔ ۳۰دقیقه‌ای: دویدن تعقیب توپ، لگ‌های کوتاه، پلایومتریک (پرش جعبه ۳×۸)."
                else "Two 30-min sessions: ball-chasing runs, short ladders, plyometrics (3×8 box jumps).",
                "فوتبال",
                if (persian) "به یاد علاقهٔ شما به فوتبال" else "Remembering your football interest"
            )
        }

        // The default starter plan — also used as the "taste" line reason.
        if (out.isEmpty()) {
            add(
                if (persian) "شروع همه‌جانبه (هفتهٔ اول)" else "All-round starter (week 1)",
                if (persian) "سه جلسه: (۱) ۲۰دقیقه پیاده‌روی تند+کشش، (۲) کالیستنیکس مقدماتی ۳×(۸ شنا+۱۲ اسکات+۲۰ثانیه پلانک)، (۳) یک ساعت تنیس تفریحی یا دویدن سبک."
                else "Three sessions: (1) 20-min brisk walk + stretches, (2) beginner calisthenics 3×(8 push-ups + 12 squats + 20s plank), (3) one hour of casual tennis or an easy run.",
                if (persian) "شروع" else "Starter",
                if (persian) "هنوز سیگنال ورزشی کافی در جستجوهای شما نیست — با جست‌وجوی بیشتر شخصی‌سازی می‌شود"
                else "Not enough sports signal yet — it personalises as you search"
            )
        } else if (top != null) {
            add(
                if (persian) "تمرکز این هفته" else "This week's focus",
                if (persian) "بیشترین سیگنال شما «" + top.key + "» با " + top.weight + " جستجو بود؛ ۶۰٪ حجم تمرین را به آن بده و بقیه را متعادل نگه دار."
                else "Your strongest signal is \"" + top.key + "\" with " + top.weight + " searches; give it 60% of the volume and keep the rest balanced.",
                if (persian) "سلیقهٔ شما" else "Your taste",
                if (persian) "محاسبه‌شده از " + history.size + " جستجوی اخیر" else "Computed from your last " + history.size + " searches"
            )
        }
        return out
    }
}
