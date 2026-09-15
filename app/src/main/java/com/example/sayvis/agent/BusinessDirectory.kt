package com.example.sayvis.agent

/**
 * The manager-agent business directory (v5.0.0).
 *
 * The owner's brief: read the SMS inbox, find people who run a business —
 * especially raw-material suppliers — and classify them (supplier /
 * distribution office / administration / entertainment) with first & last
 * name, company, phone, website and physical address when present. The same
 * classifier also digests Instagram profiles (bio + follower/following
 * context) into the same directory.
 *
 * Everything here is PURE and unit-tested; device IO (SMS inbox query,
 * contact-name lookup) lives in [SmsDirectoryAgent].
 */
object BusinessDirectory {

    const val MAX_ENTRIES: Int = 300

    enum class Category(val labelFa: String, val labelEn: String) {
        SUPPLIER("تأمین‌کننده مواد اولیه", "Raw-material supplier"),
        DISTRIBUTION("دفتر پخش / توزیع", "Distribution office"),
        OFFICE("اداره / سازمان", "Administration"),
        ENTERTAINMENT("تفریح / سرگرمی", "Entertainment"),
        BUSINESS("بیزینس عمومی", "General business"),
        OTHER("سایر", "Other")
    }

    data class Entry(
        val name: String,
        val family: String,
        val phone: String,
        val company: String,
        val category: Category,
        val site: String,
        val address: String,
        val signals: List<String>,
        val provenance: String,
        val score: Int,
        val scannedAt: Long
    )

    // ---------------------------------------------------------- lexicons

    private val SUPPLIER_WORDS = listOf(
        "مواد اولیه", "تأمین", "تامین", "عمده", "کارخانه", "تولید", "واردات", "صادرات",
        "موجودی", "قیمت عمده", "فوب", "پالایش", "خام", "گرانول", "پودر", "بارگیری",
        "supplier", "wholesale", "raw material", "manufacturer", "factory", "bulk"
    )
    private val DISTRIBUTION_WORDS = listOf(
        "پخش", "توزیع", "نمایندگی", "پخش سراسری", "موزیع",
        "distribution", "distributor", "reseller", "dealer"
    )
    private val OFFICE_WORDS = listOf(
        "اداره", "سازمان", "ثبت شرکت", "مالیات", "بیمه", "دولتی", "شهرداری", "دفتر رسمی",
        "فاکتور رسمی", "اداره کار", "office", "government", "agency", "administration"
    )
    private val ENTERTAINMENT_WORDS = listOf(
        "تفریح", "سرگرمی", "سینما", "تئاتر", "کافه", "رستوران", "مسابقه", "قرعه‌کشی", "جشن",
        "گردشگری", "تور", "entertainment", "cinema", "cafe", "restaurant", "tour", "fun"
    )
    private val BUSINESS_WORDS = listOf(
        "شرکت", "موسسه", "مؤسسه", "فروشگاه", "فروش", "سفارش", "فاکتور", "پرداخت", "قیمت",
        "درخواست", "پیش‌فاکتور", "business", "company", "order", "invoice", "sale", "price"
    )

    // ------------------------------------------------------------ scoring

    data class Classification(
        val category: Category,
        val score: Int,
        val signals: List<String>
    )

    /**
     * Classifies one text (an SMS body or an Instagram bio) into a directory
     * category with a 0..100 business score and the matched signal words.
     */
    fun classify(text: String): Classification {
        val t = text.lowercase()
        val signals = ArrayList<String>()
        var score = 0

        fun hits(words: List<String>, weight: Int, labelPrefix: String): Int {
            var h = 0
            for (w in words) {
                if (t.contains(w.lowercase())) {
                    h++
                    if (signals.size < 6) signals.add(labelPrefix + w)
                    score += weight
                }
            }
            return h
        }

        val supplierHits = hits(SUPPLIER_WORDS, 22, "تأمین:")
        val distributionHits = hits(DISTRIBUTION_WORDS, 18, "پخش:")
        val officeHits = hits(OFFICE_WORDS, 12, "اداره:")
        val entertainmentHits = hits(ENTERTAINMENT_WORDS, 12, "تفریح:")
        hits(BUSINESS_WORDS, 8, "بیزینس:")

        if (score > 100) score = 100

        val category = when {
            supplierHits > 0 -> Category.SUPPLIER
            distributionHits > 0 -> Category.DISTRIBUTION
            officeHits > 0 -> Category.OFFICE
            entertainmentHits > 0 && businessish(score) -> Category.ENTERTAINMENT
            entertainmentHits > 0 -> Category.ENTERTAINMENT
            businessish(score) -> Category.BUSINESS
            else -> Category.OTHER
        }
        return Classification(category, score, signals)
    }

    private fun businessish(score: Int): Boolean = score >= 16

    // ---------------------------------------------------------- extraction

    /** First http(s) or www link inside the text. */
    fun extractWebsite(text: String): String {
        val words = text.split(Regex("[\\s<>\"']"))
        for (w in words) {
            val lower = w.lowercase()
            if (lower.startsWith("http://") || lower.startsWith("https://")) return w.trim('.', ',')
            if (lower.startsWith("www.") && w.contains('.')) return "https://" + w.trim('.', ',')
        }
        return ""
    }

    /** Heuristic physical address: text after "آدرس"/"نشانی" up to a break. */
    fun extractAddress(text: String): String {
        for (key in listOf("آدرس:", "آدرس :", "نشانی:", "نشانی :", "address:")) {
            val idx = text.lowercase().indexOf(key.lowercase())
            if (idx >= 0) {
                val rest = text.substring(idx + key.length).trim()
                val end = rest.indexOfFirst { it == '\n' || it == '|' || it == '،' }
                val value = (if (end < 0) rest else rest.substring(0, end)).trim()
                if (value.length >= 5) return value.take(160)
            }
        }
        return ""
    }

    /** Company name: the words right after شرکت/موسسه/کارخانه when present. */
    fun extractCompany(text: String): String {
        for (key in listOf("شرکت", "موسسه", "مؤسسه", "کارخانه", "گروه صنعتی")) {
            val idx = text.indexOf(key)
            if (idx >= 0) {
                val rest = text.substring(idx).trim()
                val words = rest.split(Regex("[\\s،,.:|]")).filter { it.isNotBlank() }
                if (words.size >= 2) {
                    return words.take(4).joinToString(" ").take(80)
                }
            }
        }
        return ""
    }

    /** Splits a contact display name into given/family parts. */
    fun splitPersonName(displayName: String): Pair<String, String> {
        val cleaned = displayName.trim().replace(Regex("\\s+"), " ")
        if (cleaned.isEmpty()) return "" to ""
        val parts = cleaned.split(' ')
        return when {
            parts.size == 1 -> parts[0] to ""
            else -> parts.first() to parts.last()
        }
    }

    /** True when the SMS sender looks like an alphanumeric sender ID (a business). */
    fun isSenderCode(address: String): Boolean {
        val a = address.trim()
        if (a.isEmpty()) return false
        return a.any { it.isLetter() }
    }

    // ------------------------------------------------------------- store

    /** Merges new entries, newest first, capped at [MAX_ENTRIES]. */
    fun merge(current: List<Entry>, additions: List<Entry>): List<Entry> {
        val byKey = HashMap<String, Entry>()
        for (e in current) byKey[e.phone.lowercase() + "|" + e.provenance] = e
        for (e in additions) byKey[e.phone.lowercase() + "|" + e.provenance] = e
        return byKey.values
            .sortedByDescending { it.score }
            .takeLast(MAX_ENTRIES)
            .sortedByDescending { it.score }
    }

    // ------------------------------------------------- hand-rolled JSON IO
    // (org.json is a stub on the JVM test classpath — same pattern as Tuning.)

    fun encode(entries: List<Entry>): String {
        val B = 92.toChar()
        val Q = 34.toChar()
        fun esc(v: String): String = v.replace(B.toString(), B + "" + B).replace(Q.toString(), B + "" + Q).replace('\n', ' ')
        return entries.joinToString(",") { e ->
            "{" +
                Q + "n" + Q + ":" + Q + esc(e.name) + Q + "," +
                Q + "f" + Q + ":" + Q + esc(e.family) + Q + "," +
                Q + "p" + Q + ":" + Q + esc(e.phone) + Q + "," +
                Q + "c" + Q + ":" + Q + esc(e.company) + Q + "," +
                Q + "cat" + Q + ":" + Q + e.category.name + Q + "," +
                Q + "s" + Q + ":" + Q + esc(e.site) + Q + "," +
                Q + "a" + Q + ":" + Q + esc(e.address) + Q + "," +
                Q + "sig" + Q + ":[" + e.signals.joinToString(",") { Q + esc(it) + Q } + "]," +
                Q + "prov" + Q + ":" + Q + esc(e.provenance) + Q + "," +
                Q + "sc" + Q + ":" + e.score + "," +
                Q + "t" + Q + ":" + e.scannedAt +
                "}"
        }.let { "[" + it + "]" }
    }

    fun decode(raw: String): List<Entry> {
        if (!raw.trim().startsWith("[")) return emptyList()
        val out = ArrayList<Entry>()
        val objects = splitObjects(raw)
        for (obj in objects) {
            runCatching {
                val cat = miniString(obj, "cat") ?: "OTHER"
                out.add(
                    Entry(
                        name = miniString(obj, "n").orEmpty(),
                        family = miniString(obj, "f").orEmpty(),
                        phone = miniString(obj, "p").orEmpty(),
                        company = miniString(obj, "c").orEmpty(),
                        category = runCatching { Category.valueOf(cat) }.getOrDefault(Category.OTHER),
                        site = miniString(obj, "s").orEmpty(),
                        address = miniString(obj, "a").orEmpty(),
                        signals = miniArray(obj, "sig"),
                        provenance = miniString(obj, "prov").orEmpty(),
                        score = miniInt(obj, "sc"),
                        scannedAt = miniLong(obj, "t")
                    )
                )
            }
        }
        return out
    }

    private fun splitObjects(raw: String): List<String> {
        val out = ArrayList<String>()
        var depth = 0
        var start = -1
        for (i in raw.indices) {
            when (raw[i]) {
                '{' -> {
                    if (depth == 0) start = i
                    depth++
                }
                '}' -> {
                    depth--
                    if (depth == 0 && start >= 0) {
                        out.add(raw.substring(start, i + 1))
                        start = -1
                    }
                }
            }
        }
        return out
    }

    fun miniString(json: String, field: String): String? {
        val B = 92.toChar()
        val Q = 34.toChar()
        val patternText = Q + field + Q + B + "s*:" + B + "s*" + Q + "((?:" + B + B + ".|[^" + Q + B + B + "])*)" + Q
        val hit = Regex(patternText).find(json) ?: return null
        return unescape(hit.groupValues[1])
    }

    private fun miniArray(json: String, field: String): List<String> {
        val B = 92.toChar()
        val Q = 34.toChar()
        val patternText = Q + field + Q + B + "s*:" + B + "s*" + B + "[([^" + B + "]*)" + B + "]"
        val block = Regex(patternText).find(json)?.groupValues?.get(1) ?: return emptyList()
        return Regex(Q + "((?:" + B + B + ".|[^" + Q + B + B + "])*)" + Q).findAll(block)
            .map { unescape(it.groupValues[1]) }
            .filter { it.isNotBlank() }
            .toList()
    }

    private fun miniInt(json: String, field: String): Int =
        Regex(numberPattern(field)).find(json)?.groupValues?.get(1)?.toIntOrNull() ?: 0

    private fun miniLong(json: String, field: String): Long =
        Regex(numberPattern(field)).find(json)?.groupValues?.get(1)?.toLongOrNull() ?: 0L

    /** Pattern "field":<number> built from the shared quoting constants. */
    private fun numberPattern(field: String): String {
        val b = 92.toChar()
        val q = 34.toChar()
        return q + field + q + b + "s*:" + b + "s*(-?[0-9]+)"
    }

    fun unescape(value: String): String {
        if (!value.contains(92.toChar())) return value
        val out = StringBuilder(value.length)
        var i = 0
        while (i < value.length) {
            val ch = value[i]
            if (ch != 92.toChar() || i + 1 >= value.length) {
                out.append(ch); i++; continue
            }
            when (val next = value[i + 1]) {
                34.toChar() -> out.append(34.toChar())
                92.toChar() -> out.append(92.toChar())
                'n' -> out.append(' ')
                else -> out.append(next)
            }
            i += 2
        }
        return out.toString()
    }
}
