package com.example.sayvis.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * SAYVIS self-evolution agent: it searches GitHub (keyless API) for assistant/
 * agent projects similar to itself, ranks them by real-world traction and
 * distils an ADOPTION BACKLOG — which proven ideas are worth absorbing into
 * SAYVIS's specialist agents (trade / content / web-design / app-builder).
 *
 * A released app cannot rewrite its own code at runtime (that would be unsafe
 * and unverifiable); what it CAN do honestly is maintain a live, ranked,
 * source-backed improvement backlog — this class delivers exactly that.
 */
class EvolutionService {

    data class RepoHit(
        val fullName: String,
        val url: String,
        val stars: Int,
        val description: String,
        val topics: List<String>
    )

    data class EvolutionReport(
        val repos: List<RepoHit>,
        val ideas: List<String>,
        val messageFa: String,
        val messageEn: String
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    suspend fun search(languageFa: Boolean): EvolutionReport = withContext(Dispatchers.IO) {
        val query = "ai+assistant+agent+android"
        val request = Request.Builder()
            .url("https://api.github.com/search/repositories?q=$query&sort=stars&order=desc&per_page=8")
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "SayvisBot/1.0 (self-evolution)")
            .get()
            .build()
        val repos = runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use emptyList<RepoHit>()
                parseGitHubSearch(response.body?.string().orEmpty())
            }
        }.getOrDefault(emptyList())

        if (repos.isEmpty()) {
            return@withContext EvolutionReport(
                emptyList(), emptyList(),
                "گیت‌هاب پاسخ نداد (شبکه/محدودیت نرخ). بعداً دوباره امتحان کنید.",
                "GitHub did not respond (network/rate limit). Try again later."
            )
        }
        val ideas = adoptionIdeas(repos)
        EvolutionReport(
            repos, ideas,
            "از ${repos.size} پروژهٔ برتر گیت‌هاب، ${ideas.size} ایدهٔ قابل جذب استخراج شد.",
            "Scanned ${repos.size} top GitHub projects; distilled ${ideas.size} adoptable ideas."
        )
    }

    companion object {

        /**
         * Pure, JVM-testable parsing (no org.json — it is a stub on the unit
         * test classpath). Items are split at every "full_name" mark because
         * GitHub's item objects nest other objects (owner{…}) that defeat a
         * flat brace matcher; each window then spans exactly one item.
         */
        fun parseGitHubSearch(json: String): List<RepoHit> {
            val marks = Regex("\u0022full_name\u0022").findAll(json).map { it.range.first }.toList()
            val out = ArrayList<RepoHit>()
            for ((index, start) in marks.withIndex()) {
                val end = marks.getOrNull(index + 1) ?: json.length
                val window = json.substring(start, end)
                val fullName = stringField(window, "full_name") ?: continue
                val url = stringField(window, "html_url") ?: "https://github.com/$fullName"
                val stars = intField(window, "stargazers_count")
                val description = stringField(window, "description").orEmpty()
                val topics = topicsField(window)
                out += RepoHit(fullName, url, stars, description, topics)
                if (out.size >= 8) break
            }
            return out.sortedByDescending { it.stars }
        }

        private fun stringField(block: String, field: String): String? =
            Regex("\u0022$field\u0022\\s*:\\s*\u0022((?:\\\\.|[^\u0022\\])*)\u0022")
                .find(block)?.groupValues?.get(1)?.let { WebSearchService.jsonUnescape(it) }

        private fun intField(block: String, field: String): Int =
            Regex("\u0022$field\u0022\\s*:\\s*(\\d+)").find(block)?.groupValues?.get(1)?.toIntOrNull() ?: 0

        private fun topicsField(block: String): List<String> {
            val array = Regex("\u0022topics\u0022\\s*:\\s*\\[([^]]*)]").find(block)?.groupValues?.get(1)
                ?: return emptyList()
            return Regex("\u0022((?:\\\\.|[^\u0022\\])*)\u0022").findAll(array)
                .mapNotNull { it.groupValues[1].takeIf { s -> s.isNotBlank() } }
                .toList()
        }

        /** Pure mapping from discovered repo signals to SAYVIS capabilities. */
        fun adoptionIdeas(repos: List<RepoHit>): List<String> {
            val ideas = LinkedHashSet<String>()
            for (repo in repos) {
                val hay = (repo.description + " " + repo.topics.joinToString(" ")).lowercase()
                if (containsAny(hay, "voice", "speech", "tts", "stt")) {
                    ideas += "صدا: الگوی گفتار/شنیدار «${repo.fullName}» را بررسی کن"
                }
                if (containsAny(hay, "trading", "trade", "stock", "crypto", "forex")) {
                    ideas += "ترید: منطق تحلیل/ریسک «${repo.fullName}» برای ایجنت ترید"
                }
                if (containsAny(hay, "instagram", "social", "content", "post")) {
                    ideas += "محتوا: اتوماسیون محتوای «${repo.fullName}» برای ایجنت اینستاگرام"
                }
                if (containsAny(hay, "rag", "memory", "vector", "embedding")) {
                    ideas += "حافظه: RAG/بردار «${repo.fullName}» برای یادگیری بلندمدت"
                }
                if (containsAny(hay, "plugin", "tool", "function-call", "functioncalling")) {
                    ideas += "ابزار: معماری پلاگین «${repo.fullName}» برای ایجنت‌ها"
                }
                if (containsAny(hay, "automation", "browser", "webview", "scrap")) {
                    ideas += "وب‌گردی: تکنیک استخراج «${repo.fullName}» برای ایجنت وب"
                }
                if (containsAny(hay, "design", "ui", "ux", "theme")) {
                    ideas += "طراحی: الگوی رابط «${repo.fullName}» برای مینیمال‌سازی"
                }
            }
            return ideas.toList().take(10)
        }

        private fun containsAny(hay: String, vararg needles: String): Boolean =
            needles.any { hay.contains(it) }
    }
}
