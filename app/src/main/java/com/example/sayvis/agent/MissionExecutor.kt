package com.example.sayvis.agent

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import com.example.sayvis.net.SayvisNet

/**
 * SAYVIS v5.3.1 — MISSION EXECUTOR (دست‌های ایجنت اجراگر)
 *
 * Takes the plan from [AutoMission.plan] and does the REAL work step by
 * step, reporting live progress and the true completion metric of every
 * step so the ViewModel can tick the mission's tasks AUTOMATICALLY:
 *
 *   SEARCH   → keyless Bing/DDG + (music pack) archive.org advancedsearch
 *   COLLECT  → parse & rank (downloads = real popularity), pick top N
 *   DOWNLOAD → metadata → direct file URL → MediaStore Downloads/SAYVIS
 *              (API 29+) or app music dir (older), streamed, named safely
 *   VERIFY   → file exists && size > 10 KB
 *   REPORT   → final summary with paths; failed steps stay honestly unticked
 *
 * Everything flows through SayvisNet → the owner's online/offline gate
 * applies here too (offline ⇒ the executor fails fast with a clear reason).
 */
class MissionExecutor(
    private val context: Context,
    private val genericSearch: com.example.sayvis.ai.WebSearchService = com.example.sayvis.ai.WebSearchService(),
    private val client: OkHttpClient = SayvisNet.client(15, 60).newBuilder().followRedirects(true).build()
) {

    data class Live(
        val onStepStart: suspend (index: Int, line: String) -> Unit,
        val onStepDone: suspend (index: Int, metric: Int, line: String) -> Unit,
        val onItem: suspend (index: Int, line: String) -> Unit
    )

    data class Result(
        val ok: Boolean,
        val report: String,
        val savedPaths: List<String>,
        val topItems: List<AutoMission.MusicHit>
    )

    private class StepOutcome {
        var metric = 0
        val lines = ArrayList<String>()
        var failed = false
        var failReason = ""
    }

    suspend fun execute(goal: String, plan: AutoMission.Plan, languageFa: Boolean, live: Live): Result =
        withContext(Dispatchers.IO) {
            val outcomes = Array(plan.steps.size) { StepOutcome() }
            var hits: List<AutoMission.MusicHit> = emptyList()
            var webItems: List<com.example.sayvis.ai.WebSearchService.WebResult> = emptyList()
            val saved = ArrayList<String>()

            plan.steps.forEachIndexed { index, step ->
                val outcome = outcomes[index]
                live.onStepStart(index, "▸ " + step.title(languageFa))
                try {
                    when (step.kind) {
                        AutoMission.StepKind.SEARCH -> {
                            if (plan.pack == AutoMission.Pack.MUSIC) {
                                val url = AutoMission.archiveSearchUrl(AutoMission.musicSubjects(goal), plan.targetCount)
                                val body = getBody(url)
                                hits = AutoMission.pickMusicItems(body, plan.targetCount * 2)
                                outcome.metric = hits.size
                                outcome.lines += "packed=" + hits.size
                                live.onItem(
                                    index,
                                    (if (languageFa) "🔍 آرشیو آزاد: " else "🔍 Free archive: ") + hits.size + " candidate"
                                )
                            } else {
                                webItems = genericSearch.search(goal, languageFa)
                                outcome.metric = webItems.size
                                live.onItem(
                                    index,
                                    (if (languageFa) "🔍 وب: " else "🔍 Web: ") + webItems.size +
                                        (if (languageFa) " نتیجهٔ زنده" else " live results")
                                )
                            }
                        }
                        AutoMission.StepKind.COLLECT -> {
                            if (plan.pack == AutoMission.Pack.MUSIC) {
                                hits = hits.sortedByDescending { it.downloads }.take(plan.targetCount)
                                outcome.metric = hits.size
                                hits.forEachIndexed { i, h ->
                                    live.onItem(
                                        index,
                                        (if (languageFa) "🎧 " else "🎧 ") + (i + 1) + "/" + plan.targetCount +
                                            "  " + h.title.take(52) + "  (⬇" + AutoMission.humanSize(h.downloads) + ")"
                                    )
                                }
                            } else {
                                val items = webItems.take(plan.targetCount)
                                outcome.metric = items.size
                                items.forEachIndexed { i, w ->
                                    live.onItem(index, "📄 " + (i + 1) + "/" + plan.targetCount + "  " + w.title.take(56))
                                }
                            }
                        }
                        AutoMission.StepKind.DOWNLOAD -> {
                            if (plan.pack == AutoMission.Pack.MUSIC) {
                                hits.take(plan.targetCount).forEachIndexed { i, h ->
                                    val meta = getBody("https://archive.org/metadata/" + h.identifier)
                                    val fileUrl = AutoMission.pickAudioFile(meta, h.identifier)
                                    if (fileUrl == null) {
                                        live.onItem(index, (if (languageFa) "⚠ فایل صوتی مستقیم نداشت: " else "⚠ no direct audio: ") + h.title.take(40))
                                        return@forEachIndexed
                                    }
                                    val fileName = safeName(h.title) + "." + fileUrl.substringAfterLast('.').take(4)
                                    val savedPath = downloadToStorage(fileUrl, fileName)
                                    saved += savedPath
                                    outcome.metric = saved.size
                                    live.onItem(
                                        index,
                                        "⬇ " + (i + 1) + "/" + plan.targetCount + "  " + fileName +
                                            "  (" + AutoMission.humanSize(lastDownloadBytes) + ")"
                                    )
                                }
                            } else {
                                // Generic pack: download only genuinely direct file URLs.
                                val files = webItems.map { it.url }.filter { AutoMission.isDirectFileUrl(it) }.take(plan.targetCount)
                                files.forEachIndexed { i, url ->
                                    val name = url.substringAfterLast('/').substringBefore('?').ifBlank { "file$i" }
                                    val p = downloadToStorage(url, safeName(name))
                                    saved += p
                                    outcome.metric = saved.size
                                    live.onItem(index, "⬇ " + (i + 1) + "/" + files.size + "  " + name)
                                }
                                if (files.isEmpty()) {
                                    outcome.lines += "no-direct-files"
                                    live.onItem(
                                        index,
                                        if (languageFa) "⚠ در نتایج، لینک مستقیم فایلی پیدا نشد (صفحات معمولی بودند)."
                                        else "⚠ no direct file links in the results (regular pages)."
                                    )
                                }
                            }
                        }
                        AutoMission.StepKind.VERIFY -> {
                            var verified = 0
                            saved.forEach { path ->
                                val size = storageSize(path)
                                if (size > 10_240) verified++ else live.onItem(index, "⚠ small/broken: " + path.takeLast(40))
                            }
                            outcome.metric = verified
                        }
                        AutoMission.StepKind.REPORT -> {
                            outcome.metric = 1
                        }
                    }
                } catch (t: Throwable) {
                    outcome.failed = true
                    outcome.failReason = t.message ?: t::class.simpleName.orEmpty()
                    live.onItem(
                        index,
                        (if (languageFa) "⚠ این گام کامل نشد: " else "⚠ step not completed: ") + outcome.failReason.take(80)
                    )
                }
                live.onStepDone(
                    index,
                    outcome.metric,
                    if (AutoMission.autoTick(step, outcome.metric) && !outcome.failed)
                        "✓ " + step.title(languageFa) + (if (languageFa) " (خودکار تیک خورد)" else " (auto-ticked)")
                    else "✗ " + step.title(languageFa) + " — " + outcome.metric + "/" + step.target
                )
            }

            val okAll = outcomes.withIndex().all { (i, o) ->
                AutoMission.autoTick(plan.steps[i], o.metric) && !o.failed
            }
            val report = buildReport(goal, plan, languageFa, outcomes, hits, saved, okAll)
            Result(okAll, report, saved.toList(), hits.take(plan.targetCount))
        }

    private var lastDownloadBytes: Long = 0

    // ------------------------------------------------------------------ net

    private fun getBody(url: String): String {
        val request = SayvisNet.get(url).header("Accept", "*/*").build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw IllegalStateException("HTTP " + resp.code + " from " + url.take(60))
            return resp.body?.string().orEmpty()
        }
    }

    // ------------------------------------------------------------ storage

    private fun downloadToStorage(url: String, fileName: String): String {
        val request = SayvisNet.get(url).header("Accept", "*/*").build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw IllegalStateException("HTTP " + resp.code + " on file")
            val source = resp.body?.byteStream() ?: throw IllegalStateException("empty body")
            lastDownloadBytes = 0
            val counted = CountingStream(source) { read -> lastDownloadBytes += read }
            return if (Build.VERSION.SDK_INT >= 29) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, mimeFor(fileName))
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/SAYVIS")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: throw IllegalStateException("MediaStore insert failed")
                context.contentResolver.openOutputStream(uri)?.use { out -> counted.copyTo(out) }
                    ?: throw IllegalStateException("stream open failed")
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
                uri.toString()
            } else {
                val dir = java.io.File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "SAYVIS")
                    .apply { mkdirs() }
                val f = java.io.File(dir, fileName)
                f.outputStream().use { out -> counted.copyTo(out) }
                f.absolutePath
            }
        }
    }

    private fun storageSize(path: String): Long = runCatching {
        if (path.startsWith("content:")) {
            context.contentResolver.openInputStream(android.net.Uri.parse(path))?.use { it.available().toLong() } ?: 0
        } else {
            java.io.File(path).length()
        }
    }.getOrDefault(0)

    private fun safeName(raw: String): String =
        raw.replace(Regex("[^A-Za-z0-9._ -]+"), "_").trim().take(60).ifBlank { "sayvis-file" }

    private fun mimeFor(name: String): String {
        val ext = name.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "mp3" -> "audio/mpeg"
            "flac" -> "audio/flac"
            "ogg" -> "audio/ogg"
            "wav" -> "audio/wav"
            "pdf" -> "application/pdf"
            "zip" -> "application/zip"
            "epub" -> "application/epub+zip"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "txt" -> "text/plain"
            else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
        }
    }

    private class CountingStream(
        private val upstream: java.io.InputStream,
        private val onRead: (Int) -> Unit
    ) : java.io.InputStream() {
        override fun read(): Int {
            val r = upstream.read()
            if (r >= 0) onRead(1)
            return r
        }
        override fun read(b: ByteArray, off: Int, len: Int): Int {
            val r = upstream.read(b, off, len)
            if (r > 0) onRead(r)
            return r
        }
        override fun close() = upstream.close()
    }

    // -------------------------------------------------------------- report

    private fun buildReport(
        goal: String,
        plan: AutoMission.Plan,
        languageFa: Boolean,
        outcomes: Array<StepOutcome>,
        hits: List<AutoMission.MusicHit>,
        saved: List<String>,
        okAll: Boolean
    ): String = buildString {
        if (languageFa) {
            appendLine("🤖 ایجنت اجراگر — دستور «${goal.take(60)}»")
            appendLine(if (okAll) "همهٔ گام‌ها انجام و خودکار تیک خوردند ✅" else "برخی گام‌ها کامل نشد — همان‌ها بدون تیک ماندند (صادقانه).")
        } else {
            appendLine("🤖 Executor agent — command \"${goal.take(60)}\"")
            appendLine(if (okAll) "All steps done and auto-ticked ✅" else "Some steps did not complete — they stay unticked (honest).")
        }
        appendLine()
        plan.steps.forEachIndexed { i, step ->
            val o = outcomes[i]
            val mark = if (AutoMission.autoTick(step, o.metric) && !o.failed) "✅" else "⬜"
            appendLine("$mark ${step.title(languageFa)} — ${o.metric}/${step.target}")
        }
        if (hits.isNotEmpty()) {
            appendLine()
            if (languageFa) appendLine("🎧 موارد منتخب (محبوبیت = تعداد دانلود واقعی آرشیو):")
            else appendLine("🎧 Selected items (popularity = real archive downloads):")
            hits.take(plan.targetCount).forEach { appendLine("• ${it.title.take(60)} (⬇${it.downloads})") }
        }
        if (saved.isNotEmpty()) {
            appendLine()
            if (languageFa) appendLine("📁 ذخیره‌شده در Downloads/SAYVIS (یا حافظهٔ اپ در اندروید قدیمی):")
            else appendLine("📁 Saved under Downloads/SAYVIS (or app storage on old Android):")
            saved.take(6).forEach { appendLine("• ${it.takeLast(70)}") }
        }
        if (languageFa) {
            appendLine()
            appendLine("منبع دانلود موزیک: آرشیو آزاد archive.org (کلکسیون‌های آزاد/دامن عمومی — قانونی و بی‌اکانت).")
        } else {
            appendLine()
            appendLine("Music source: the free archive.org (free/public-domain collections — legal, keyless).")
        }
    }
}
