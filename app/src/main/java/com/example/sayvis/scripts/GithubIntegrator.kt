package com.example.sayvis.scripts

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import com.example.sayvis.net.SayvisNet

/**
 * SAYVIS v5.3.0 — GITHUB INTEGRATOR (ادغام‌گر گیت‌هاب)
 *
 * The script section's tool the owner asked for:
 *   1) SCAN  — keyless GitHub search (same working pattern as the v4 engine
 *              tuner: api.github.com with a proper User-Agent).
 *   2) IDENTIFY — pure scoring flags the special items (Kotlin/Compose/
 *              trading-engine relevance, stars, permissive licence).
 *   3) SELECT — the owner ticks the hits worth merging.
 *   4) MERGE — selected items are fetched (top-level file list + README head),
 *              staged into a persisted integration registry, and exported as
 *              a merge dossier. Real compile-into-the-app integration happens
 *              in the following build task — this store is the honest staging
 *              layer between "found" and "in the binary".
 */
class GithubIntegrator(private val client: okhttp3.OkHttpClient = SayvisNet.client(8, 12)) {

    data class RepoHit(
        val repo: String,
        val description: String,
        val url: String,
        val stars: Int,
        val language: String,
        val license: String,
        val score: Int,
        val special: Boolean
    )

    companion object {
        /** Pure scoring (unit-tested). Cap 10; special ≥ 6. */
        fun score(description: String, language: String, license: String, stars: Int, queryWords: List<String>): Int {
            var s = 0
            val d = description.lowercase()
            if (language.equals("Kotlin", ignoreCase = true)) s += 2
            if (listOf("compose", "android").any { d.contains(it) }) s += 1
            if (queryWords.any { d.contains(it.lowercase()) }) s += 2
            if (stars >= 100) s += 1
            if (stars >= 1000) s += 1
            if (license.isNotEmpty() && !license.contains("GPL", ignoreCase = true)) s += 1
            if (listOf("trading", "bot", "engine", "market", "assistant", "voice").any { d.contains(it) }) s += 2
            return s.coerceAtMost(10)
        }

        const val SPECIAL_THRESHOLD = 6
    }

    /** Keyless search; never throws. */
    suspend fun search(query: String): List<RepoHit> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isEmpty()) return@withContext emptyList()
        val url = "https://api.github.com/search/repositories?q=" +
            java.net.URLEncoder.encode(q, "UTF-8") + "&per_page=8&sort=best-match"
        val request = SayvisNet.get(url)
            .header("Accept", "application/vnd.github+json")
            .build()
        runCatching {
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@use emptyList<RepoHit>()
                val root = JSONObject(resp.body?.string().orEmpty())
                val items = root.optJSONArray("items") ?: return@use emptyList<RepoHit>()
                val queryWords = q.split(Regex("\\s+")).filter { it.length >= 3 }
                (0 until items.length()).mapNotNull { i ->
                    val it = items.optJSONObject(i) ?: return@mapNotNull null
                    val license = it.optJSONObject("license")?.optString("spdx_id").orEmpty()
                        .let { l -> if (l == "null" || l.isBlank()) "" else l }
                    val description = it.optString("description").orEmpty()
                    val language = it.optString("language").orEmpty()
                    val stars = it.optInt("stargazers_count", 0)
                    val score = score(description, language, license, stars, queryWords)
                    RepoHit(
                        repo = it.optString("full_name"),
                        description = description,
                        url = it.optString("html_url"),
                        stars = stars,
                        language = language,
                        license = license,
                        score = score,
                        special = score >= SPECIAL_THRESHOLD
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    /** Top-level file names of a repo (contents API, keyless) — merge dossier input. */
    suspend fun topFiles(repo: String): List<String> = withContext(Dispatchers.IO) {
        val request = SayvisNet.get("https://api.github.com/repos/$repo/contents/")
            .header("Accept", "application/vnd.github+json")
            .build()
        runCatching {
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@use emptyList<String>()
                val arr = org.json.JSONArray(resp.body?.string().orEmpty())
                (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.optString("name") }
                    .filter { it.endsWith(".kt") || it.endsWith(".kts") || it.endsWith(".md") || it == "README.md" }
            }
        }.getOrDefault(emptyList())
    }

    /** README head (first ~1200 chars of the rendered readme via raw main branch). */
    suspend fun readmeHead(repo: String): String = withContext(Dispatchers.IO) {
        val request = SayvisNet.get("https://raw.githubusercontent.com/$repo/HEAD/README.md").build()
        runCatching {
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@use ""
                resp.body?.string().orEmpty().take(1200)
            }
        }.getOrDefault("")
    }
}

/**
 * Persisted staging registry for the owner's selected GitHub items
 * (filesDir/github_integrations.json — same style as ScriptStore).
 */
class IntegrationStore private constructor(context: Context) {

    data class Entry(
        val id: String,
        val repo: String,
        val url: String,
        val description: String,
        val stars: Int,
        val score: Int,
        val special: Boolean,
        val topFiles: List<String>,
        val readmeHead: String,
        val stagedAt: Long = System.currentTimeMillis()
    )

    private val file = java.io.File(context.applicationContext.filesDir, "github_integrations.json")
    private val _entries = MutableStateFlow<List<Entry>>(emptyList())
    val entries: StateFlow<List<Entry>> = _entries.asStateFlow()

    init {
        _entries.value = load()
    }

    fun upsert(entry: Entry) {
        val next = ArrayList<Entry>()
        next.addAll(_entries.value.filterNot { it.repo == entry.repo })
        next.add(0, entry)
        _entries.value = next.take(40)
        persist()
    }

    fun remove(id: String) {
        _entries.value = _entries.value.filterNot { it.id == id }
        persist()
    }

    fun clear() {
        _entries.value = emptyList()
        persist()
    }

    /** Plain-text merge dossier (share/export target). */
    fun dossier(): String = buildString {
        appendLine("// SAYVIS merge dossier — staged GitHub integrations")
        _entries.value.forEach { e ->
            appendLine("--- ${e.repo} ★${e.stars} score=${e.score}${if (e.special) " [SPECIAL]" else ""}")
            appendLine("    ${e.url}")
            if (e.description.isNotBlank()) appendLine("    ${e.description.take(140)}")
            if (e.topFiles.isNotEmpty()) appendLine("    files: ${e.topFiles.joinToString(", ")}")
        }
    }

    private fun load(): List<Entry> = runCatching {
        if (!file.exists()) return emptyList()
        val root = JSONObject(file.readText())
        val arr = root.optJSONArray("entries") ?: return emptyList()
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            Entry(
                id = o.optString("id"),
                repo = o.optString("repo"),
                url = o.optString("url"),
                description = o.optString("description"),
                stars = o.optInt("stars"),
                score = o.optInt("score"),
                special = o.optBoolean("special"),
                topFiles = (0 until (o.optJSONArray("topFiles")?.length() ?: 0))
                    .mapNotNull { j -> o.optJSONArray("topFiles")?.optString(j) },
                readmeHead = o.optString("readmeHead"),
                stagedAt = o.optLong("stagedAt")
            )
        }
    }.getOrDefault(emptyList())

    private fun persist() = runCatching {
        val root = JSONObject()
        val arr = org.json.JSONArray()
        _entries.value.forEach { e ->
            arr.put(
                JSONObject()
                    .put("id", e.id)
                    .put("repo", e.repo)
                    .put("url", e.url)
                    .put("description", e.description)
                    .put("stars", e.stars)
                    .put("score", e.score)
                    .put("special", e.special)
                    .put("topFiles", org.json.JSONArray(e.topFiles))
                    .put("readmeHead", e.readmeHead)
                    .put("stagedAt", e.stagedAt)
            )
        }
        root.put("entries", arr)
        file.writeText(root.toString())
    }

    companion object {
        @Volatile private var instance: IntegrationStore? = null
        fun get(context: Context): IntegrationStore =
            instance ?: synchronized(this) {
                instance ?: IntegrationStore(context).also { instance = it }
            }
    }
}
