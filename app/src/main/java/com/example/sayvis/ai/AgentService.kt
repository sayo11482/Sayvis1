package com.example.sayvis.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * SAYVIS research agent: on an explicit owner command it runs a real multi-step
 * browsing loop — plan → search the web (keyless) → open and READ the top
 * source pages → (optionally) hand the digest to the connected AI brain for
 * synthesis → answer with citations. Read-only fetches only; every step is
 * reported back so the owner sees the agent working.
 */
class AgentService(
    private val webSearch: WebSearchService = WebSearchService()
) {

    data class Extract(
        val result: WebSearchService.WebResult,
        val bestParagraphs: List<String>
    )

    data class AgentReport(
        val goal: String,
        val answer: String,
        val sources: List<WebSearchService.WebResult>,
        val steps: List<String>,
        val googleLines: List<String> = emptyList()
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(14, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /**
     * Runs the loop. [synthesizer] (when connected) receives the research block
     * and returns the final prose; [googleFetch] (when signed in) is consulted
     * for the owner's private Google data. Both are optional — the agent is
     * fully usable keyless via the extractive fallback.
     */
    suspend fun run(
        goal: String,
        languageFa: Boolean,
        synthesizer: (suspend (String) -> String)?,
        googleFetch: (suspend (String) -> GoogleServicesService.FetchResult)?,
        onStep: (String) -> Unit
    ): AgentReport {
        val steps = ArrayList<String>()

        // ---- Google private capabilities first (gmail / calendar / drive)
        val googleLines = ArrayList<String>()
        if (googleFetch != null && GoogleServicesService.matchCapability(goal) != null) {
            onStep(if (languageFa) "🔐 خواندن دادهٔ گوگل مالک…" else "🔐 Reading owner's Google data…")
            steps += "google:${goal.take(60)}"
            val fetched = runCatching { googleFetch(goal) }.getOrNull()
            if (fetched != null && fetched.ok) googleLines += fetched.lines
        }

        // ---- Step 1: search
        onStep(if (languageFa) "🔍 ایجنت: جست‌وجوی وب برای «$goal»…" else "🔍 Agent: searching the web for \"$goal\"…")
        steps += "search:$goal"
        val sources = runCatching { webSearch.search(goal, languageFa) }.getOrDefault(emptyList())
        if (sources.isEmpty() && googleLines.isEmpty()) {
            val message = if (languageFa) {
                "ایجنت نتوانست منبعی پیدا کند (شبکه/تحریم). اتصال را بررسی کنید یا سؤال را جور دیگر بپرسید."
            } else {
                "The agent found no sources (network/region block). Check connectivity or rephrase the question."
            }
            return AgentReport(goal, message, emptyList(), steps)
        }

        // ---- Step 2: read the top pages
        val extracts = ArrayList<Extract>()
        for ((index, result) in sources.take(3).withIndex()) {
            onStep(
                if (languageFa) "📄 خواندن منبع ${index + 1}: ${result.title.take(48)}…"
                else "📄 Reading source ${index + 1}: ${result.title.take(48)}…"
            )
            steps += "read:${result.url.take(80)}"
            val text = runCatching { fetchReadable(result.url) }.getOrDefault("")
            if (text.isBlank()) continue
            val paragraphs = pickBestParagraphs(text, goal, max = 2)
            if (paragraphs.isNotEmpty()) extracts += Extract(result, paragraphs)
        }

        // ---- Step 3: synthesise
        onStep(if (languageFa) "🧠 نتیجه‌گیری از ${extracts.size} منبع…" else "🧠 Synthesising ${extracts.size} sources…")
        steps += "synthesize:${extracts.size}"

        val researchBlock = researchBlock(goal, extracts, googleLines, languageFa)
        val synthesized = if (synthesizer != null && (extracts.isNotEmpty() || googleLines.isNotEmpty())) {
            runCatching { synthesizer(researchBlock) }.getOrNull()?.takeIf { it.isNotBlank() }
        } else null

        val answer = synthesized ?: extractiveAnswer(goal, extracts, googleLines, languageFa)
        onStep(if (languageFa) "✅ ایجنت کار را تمام کرد." else "✅ Agent finished.")

        return AgentReport(goal, answer, sources, steps, googleLines)
    }

    /** Fetches a page and reduces it to readable text (no scripts/styles/tags). */
    suspend fun fetchReadable(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 SayvisAgent/1.0")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@use ""
            val html = response.body?.string().orEmpty()
            readableText(html)
        }
    }

    companion object {

        /** Explicit agent triggers; returns the research goal, or null. */
        fun triggerGoal(normalizedText: String): String? {
            val text = normalizedText.trim()
            if (text.length < 6) return null
            val triggers = listOf(
                "ایجنت:", "ایجنت ", "تحقیق کن درباره", "تحقیق کن ", "بررسی کن درباره",
                "agent:", "agent ", "research "
            )
            for (trigger in triggers) {
                val index = text.indexOf(trigger)
                if (index >= 0) {
                    val rest = text.substring(index + trigger.length).trim('،', ',', '؟', '?', ' ', ':')
                    if (rest.length >= 3) return rest
                }
            }
            return null
        }

        /** Strips scripts, styles and tags down to readable page text. */
        fun readableText(html: String): String = html
            .replace(Regex("(?is)<(script|style|noscript|svg|header|footer|nav)[^>]*>.*?</\\1>"), " ")
            .replace(Regex("(?is)<br\\s*/?>|</p>|</div>|</li>|</h[1-6]>"), "\n")
            .replace(Regex("<[^>]*>"), " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#x27;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace(Regex("[ \\t]+"), " ")
            .replace(Regex(" *\\n *"), "\n")
            .replace(Regex("\n{2,}"), "\n")
            .trim()

        private fun keywords(goal: String): List<String> =
            goal.lowercase().split(Regex("[\\s،,؛;:.?؟!()\\[\\]{}\"']+"))
                .filter { it.length >= 3 }
                .distinct()
                .take(12)

        /** Picks the paragraphs that best match the goal's keywords. */
        fun pickBestParagraphs(text: String, goal: String, max: Int = 2): List<String> {
            val keys = keywords(goal)
            if (keys.isEmpty()) return emptyList()
            return text.split('\n')
                .map { it.trim() }
                .filter { it.length in 80..2000 }
                .map { paragraph ->
                    val lower = paragraph.lowercase()
                    paragraph to keys.count { lower.contains(it) }
                }
                .filter { it.second >= 2 }
                .sortedByDescending { it.second }
                .take(max)
                .map { it.first }
        }

        /** The block handed to the AI brain for synthesis. */
        fun researchBlock(
            goal: String,
            extracts: List<Extract>,
            googleLines: List<String>,
            languageFa: Boolean
        ): String = buildString {
            appendLine(if (languageFa) "هدف پژوهش ایجنت: $goal" else "Agent research goal: $goal")
            appendLine(if (languageFa) "از منابع زیر استفاده کن و پاسخ مستند بده:" else "Use the sources below and answer with citations:")
            extracts.forEachIndexed { index, extract ->
                appendLine("[${index + 1}] ${extract.result.title} (${extract.result.url})")
                extract.bestParagraphs.forEach { appendLine(it.take(700)) }
                appendLine()
            }
            if (googleLines.isNotEmpty()) {
                appendLine(if (languageFa) "دادهٔ خصوصی حساب گوگل مالک:" else "Owner's private Google account data:")
                googleLines.forEach { appendLine(it) }
            }
        }

        /** Keyless fallback: keyword-scored sentences with numbered citations. */
        fun extractiveAnswer(
            goal: String,
            extracts: List<Extract>,
            googleLines: List<String>,
            languageFa: Boolean
        ): String {
            val keys = keywords(goal)
            fun bestSentences(paragraph: String, count: Int): List<String> =
                paragraph.split(Regex("(?<=[.!?؟])\\s+"))
                    .map { it.trim() }
                    .filter { it.length in 30..400 }
                    .sortedByDescending { sentence -> keys.count { sentence.lowercase().contains(it) } }
                    .take(count)

            val out = StringBuilder()
            out.append(if (languageFa) "🛰 نتیجهٔ پژوهش ایجنت سایویس دربارهٔ «$goal»:\n"
            else "🛰 SAYVIS agent research on \"$goal\":\n")
            extracts.take(3).forEachIndexed { index, extract ->
                val sentences = extract.bestParagraphs.firstOrNull()
                    ?.let { bestSentences(it, 2) }.orEmpty()
                if (sentences.isEmpty()) return@forEachIndexed
                out.append("\n[").append(index + 1).append("] ")
                    .append(extract.result.title).append(":\n")
                sentences.forEach { out.append(it).append(' ') }
                out.append('\n')
            }
            if (googleLines.isNotEmpty()) {
                out.append('\n')
                    .append(if (languageFa) "🔐 از حساب گوگل شما:\n" else "🔐 From your Google account:\n")
                googleLines.take(5).forEach { out.append(it).append('\n') }
            }
            if (out.endsWith(":\n") || extracts.isEmpty() && googleLines.isEmpty()) {
                return if (languageFa) "ایجنت نتوانست متن قابل استنادی از منابع استخراج کند؛ دوباره تلاش کنید یا سؤال را دقیق‌تر بپرسید."
                else "The agent could not extract citable text from the sources; try again or refine the question."
            }
            out.append("\n🌐 ").append(if (languageFa) "منابع:" else "Sources:")
            extracts.take(3).forEachIndexed { index, extract ->
                out.append("\n").append(index + 1).append(". ").append(extract.result.title)
                    .append(" — ").append(extract.result.url)
            }
            return out.toString().trim()
        }
    }
}
