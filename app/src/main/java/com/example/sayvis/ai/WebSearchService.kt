package com.example.sayvis.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLDecoder
import java.util.concurrent.TimeUnit

/**
 * Keyless, on-device web search used by the assistant to ground answers in fresh
 * sources — deliberately usable even when the cloud AI is unreachable (quota,
 * regional block) and even in forced-offline mode, because fetching public pages
 * is read-only and never sends personal data.
 *
 * Backends:
 *  - DuckDuckGo HTML endpoint (no key, no API) parsed with pure-Kotlin regexes;
 *  - Wikipedia opensearch (fa/en) for encyclopaedic coverage.
 */
class WebSearchService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
) {

    data class WebResult(
        val title: String,
        val url: String,
        val snippet: String,
        val source: String
    )

    /** One combined search across backends; never throws. */
    suspend fun search(query: String, languageFa: Boolean): List<WebResult> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext emptyList()
        coroutineScope {
            val ddg = async { runCatching { searchDuckDuckGo(trimmed) }.getOrDefault(emptyList()) }
            val wiki = async { runCatching { searchWikipedia(trimmed, languageFa) }.getOrDefault(emptyList()) }
            val combined = LinkedHashMap<String, WebResult>()
            (ddg.await() + wiki.await()).forEach { result ->
                combined.putIfAbsent(result.url, result)
            }
            combined.values.take(6)
        }
    }

    // ------------------------------------------------------------- DuckDuckGo

    suspend fun searchDuckDuckGo(query: String): List<WebResult> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://html.duckduckgo.com/html/?q=" + urlencode(query))
            .header("User-Agent", "Mozilla/5.0 (Android 14) AppleWebKit/537.36 SayvisBot/1.0")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@use emptyList<WebResult>()
            val html = response.body?.string().orEmpty()
            parseDuckDuckGoHtml(html)
        }
    }

    // -------------------------------------------------------------- Wikipedia

    suspend fun searchWikipedia(query: String, languageFa: Boolean): List<WebResult> = withContext(Dispatchers.IO) {
        val lang = if (languageFa) "fa" else "en"
        val url = "https://$lang.wikipedia.org/w/api.php?action=query&list=search&format=json&utf8=1&srlimit=3&srsearch=" +
            urlencode(query)
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "SayvisBot/1.0 (personal assistant)")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@use emptyList<WebResult>()
            val body = response.body?.string().orEmpty()
            parseWikipediaJson(body, lang)
        }
    }

    companion object {

        // -------------------------------------------------- pure parsing (tested)

        /** Extracts results from the DDG HTML page: anchor + snippet pairs. */
        fun parseDuckDuckGoHtml(html: String): List<WebResult> {
            val anchorRegex = Regex("""class="result__a"[^>]*href="([^"]+)"[^>]*>([\s\S]*?)</a>""")
            val snippetRegex = Regex("""class="result__snippet"[^>]*>([\s\S]*?)</a>""")
            val anchors = anchorRegex.findAll(html).toList()
            val snippets = snippetRegex.findAll(html).map { stripTags(it.groupValues[1]) }.toList()
            val out = ArrayList<WebResult>()
            anchors.forEachIndexed { index, match ->
                val rawUrl = match.groupValues[1]
                val title = stripTags(match.groupValues[2])
                if (title.isBlank()) return@forEachIndexed
                val realUrl = resolveDuckUrl(rawUrl) ?: return@forEachIndexed
                out.add(
                    WebResult(
                        title = title,
                        url = realUrl,
                        snippet = snippets.getOrNull(index).orEmpty(),
                        source = "DuckDuckGo"
                    )
                )
                if (out.size >= 5) return out
            }
            return out
        }

        /** DDG wraps target URLs in a /l/?uddg=<encoded> redirect — unwrap it. */
        fun resolveDuckUrl(raw: String): String? {
            val decoded = if (raw.startsWith("//")) "https:$raw" else raw
            val match = Regex("[?&]uddg=([^&]+)").find(decoded) ?: return decoded.takeIf { it.startsWith("http") }
            return runCatching { URLDecoder.decode(match.groupValues[1], "UTF-8") }.getOrNull()
        }

        /**
         * Extracts search hits from the Wikipedia search JSON. Parsed with a
         * regex scanner instead of org.json so the logic stays unit-testable
         * on the plain JVM (where org.json is a stub).
         */
        fun parseWikipediaJson(json: String, lang: String): List<WebResult> {
            return runCatching {
                val pairRegex = Regex("\\\"title\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"\\s*,\\s*\\\"snippet\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"")
                val out = ArrayList<WebResult>()
                for (match in pairRegex.findAll(json)) {
                    val title = jsonUnescape(match.groupValues[1])
                    if (title.isBlank()) continue
                    out.add(
                        WebResult(
                            title = title,
                            url = "https://$lang.wikipedia.org/wiki/" + urlencode(title).replace("%20", "_"),
                            snippet = stripTags(jsonUnescape(match.groupValues[2])),
                            source = "Wikipedia"
                        )
                    )
                }
                out
            }.getOrDefault(emptyList())
        }

        // ------------------------------------------- search-intent decision (pure)

        data class SearchDecision(val query: String, val explicit: Boolean)

        private val EXPLICIT_TRIGGERS = listOf(
            "جستجو کن درباره", "جستجو کن ", "جستجوی وب", "سرچ کن ", "گوگل کن ",
            "web search", "search for", "search:", "look up"
        )

        private val QUESTION_WORDS = listOf(
            "چیست", "چیه", "چیه؟", "چرا", "چگونه", "چطور", "کیست", "کجاست", "کی بود", "اخبار", "قیمت", "آب و هوا", "هوا چطوره"
        )

        // Latin cues are matched on word boundaries so e.g. "how" inside
        // "show" does not trigger a web search.
        private val LATIN_QUESTION = Regex(
            """\b(what|who|why|how|when|where)\b|\b(news|price of|weather)\b"""
        )

        /**
         * Decides whether the assistant should ground its answer in a live web
         * search. Explicit triggers win; open questions and news/price/weather
         * style prompts trigger an automatic search.
         */
        fun shouldSearch(normalizedText: String): SearchDecision? {
            val text = normalizedText.trim()
            if (text.length < 4) return null
            for (trigger in EXPLICIT_TRIGGERS) {
                val idx = text.indexOf(trigger)
                if (idx >= 0) {
                    val rest = text.substring(idx + trigger.length).trim('،', ',', '؟', '?', ' ', ':')
                    return SearchDecision(query = rest.ifBlank { text }, explicit = true)
                }
            }
            val questionMark = text.contains('؟') || text.contains('?')
            val questionWord = QUESTION_WORDS.any { text.contains(it) } || LATIN_QUESTION.containsMatchIn(text)
            return if (questionMark || questionWord) SearchDecision(query = text, explicit = false) else null
        }

        // --------------------------------------------------------------- helpers

        fun stripTags(html: String): String = html
            .replace(Regex("<[^>]*>"), " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#x27;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace(Regex("\\s+"), " ")
            .trim()

        /** Minimal JSON string unescape for the regex-extracted fields. */
        fun jsonUnescape(value: String): String {
            if ('\\' !in value) return value
            val out = StringBuilder(value.length)
            var i = 0
            while (i < value.length) {
                val ch = value[i]
                if (ch != '\\' || i + 1 >= value.length) { out.append(ch); i++; continue }
                when (val next = value[i + 1]) {
                    '"' -> out.append('"')
                    '\\' -> out.append('\\')
                    '/' -> out.append('/')
                    'n' -> out.append('\n')
                    'r' -> out.append('\r')
                    't' -> out.append('\t')
                    'b' -> out.append('\b')
                    'u' -> if (i + 5 < value.length) {
                        out.append(value.substring(i + 2, i + 6).toIntOrNull(16)?.toChar() ?: 'u')
                        i += 4
                    } else out.append('u')
                    else -> out.append(next)
                }
                i += 2
            }
            return out.toString()
        }

        fun urlencode(value: String): String =
            java.net.URLEncoder.encode(value, "UTF-8")
    }
}
