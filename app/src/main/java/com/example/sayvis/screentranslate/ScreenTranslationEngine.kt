package com.example.sayvis.screentranslate

import com.example.sayvis.ai.AIOrchestrator
import com.example.sayvis.ai.AiRequestContext
import com.example.sayvis.ai.TranslationSource
import com.example.sayvis.settings.AiProviderKind
import com.example.sayvis.settings.AiSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The three-tier translator behind the screen overlay:
 *
 *  1. **Persistent cache** — a sentence that was translated once is never paid for twice.
 *  2. **Built-in dictionary** ([ScreenLexicon]) — reviewed, instant, fully offline. Word
 *     composition covers lines the phrase table does not know, as long as most of the line
 *     resolves, so half-Persian gibberish is never painted on the screen.
 *  3. **The owner's AI provider** — for genuine prose. Only *text* is sent, never pixels;
 *     requests are batched, deduplicated within a frame and skipped entirely when the
 *     owner selected offline mode, dictionary-only mode or the emergency lock is engaged.
 *
 * The tier that produced each segment is recorded in [TranslatedSegment.provenance], so the
 * interface can honestly label machine output and the audit trail can report how much of the
 * screen left the device.
 */
class ScreenTranslationEngine(
    private val cache: ScreenTranslationCache = ScreenTranslationCache(),
    private val orchestrator: AIOrchestrator = AIOrchestrator()
) {

    // ------------------------------------------------------------------ tier 1 & 2

    /**
     * Cache + dictionary resolution. Never suspends, never touches the network, and is
     * therefore always safe to run on the capture thread.
     */
    fun resolveOffline(
        segments: List<String>,
        settings: ScreenTranslationSettings
    ): Map<String, TranslatedSegment> {
        val out = LinkedHashMap<String, TranslatedSegment>()
        for (segment in segments) {
            val text = segment.trim()
            if (text.isEmpty() || out.containsKey(text)) continue

            val cached = if (settings.cacheTranslations) cache.get(text) else null
            if (cached != null) {
                out[text] = TranslatedSegment(text, cached, TranslationSource.CACHE)
                continue
            }

            val exact = ScreenLexicon.lookup(text)
            if (exact != null) {
                out[text] = TranslatedSegment(text, exact, TranslationSource.DICTIONARY)
                continue
            }

            val composed = ScreenLexicon.glossLine(text)
            if (composed != null) {
                out[text] = TranslatedSegment(text, composed, TranslationSource.DICTIONARY)
            }
        }
        return out
    }

    // ------------------------------------------------------------------------ tier 3

    /**
     * Full resolution: everything [resolveOffline] knows, plus a model pass for the rest.
     *
     * @param segments every candidate on the current frame (lines and, when the granularity
     *        asks for it, individual words). Duplicates are collapsed before any request.
     * @param forceOffline the owner's global offline switch.
     * @param emergencyLockActive the zero-trust kill switch; blocks all network use.
     */
    suspend fun translate(
        segments: List<String>,
        settings: ScreenTranslationSettings,
        ai: AiSettings,
        forceOffline: Boolean,
        emergencyLockActive: Boolean,
        languageFa: Boolean = true
    ): Map<String, TranslatedSegment> = withContext(Dispatchers.IO) {
        val offline = resolveOffline(segments, settings)
        val missing = segments
            .map { it.trim() }
            .filter { it.isNotEmpty() && !offline.containsKey(it) }
            .distinct()

        if (missing.isEmpty()) return@withContext offline

        val canUseModel = !settings.dictionaryOnly &&
            !forceOffline &&
            !emergencyLockActive &&
            ai.provider != AiProviderKind.LOCAL &&
            ai.isProviderConfigured()
        if (!canUseModel) return@withContext offline

        val provider = orchestrator.providerFor(ai.provider)
        if (provider == null || !provider.isConfigured(ai)) return@withContext offline

        val batchSize = settings.maxSegmentsPerRequest.coerceIn(1, 40)
        val collected = LinkedHashMap(offline)
        missing.chunked(batchSize).forEach { batch ->
            val numbered = batch.mapIndexed { index, value -> "${index + 1}. $value" }.joinToString("\n")
            val context = AiRequestContext(
                prompt = numbered,
                languageFa = languageFa,
                persona = ai.systemPersona,
                temperature = 0.15,
                maxOutputTokens = (batch.size * 90).coerceIn(256, ai.maxOutputTokens.coerceAtLeast(256)),
                taskInstruction = BATCH_INSTRUCTION
            )
            val response = runCatching { provider.generateResponse(context, ai) }.getOrNull()
            if (response == null || !response.isSuccess) continue
            val parsed = parseNumbered(response.text, batch.size)
            parsed.forEach { (index, translation) ->
                val source = batch.getOrNull(index) ?: return@forEach
                if (translation.isBlank() || !ScreenTextPlanner.isPersianText(translation)) return@forEach
                collected[source] = TranslatedSegment(source, translation, TranslationSource.MACHINE)
                if (settings.cacheTranslations) cache.put(source, translation)
            }
        }
        collected
    }

    /** Single-segment convenience used by the in-app dictionary tester. */
    suspend fun translateOne(
        text: String,
        settings: ScreenTranslationSettings,
        ai: AiSettings,
        forceOffline: Boolean,
        emergencyLockActive: Boolean,
        languageFa: Boolean = true
    ): TranslatedSegment? =
        translate(listOf(text), settings, ai, forceOffline, emergencyLockActive, languageFa)[text.trim()]

    /**
     * Reads "1. ترجمه" lines back into a map of zero-based index → translation. Tolerates
     * the numbering styles models actually produce (`1.`, `1)`, `1-`, `1:`), stray blank
     * lines and a model that echoes the source numbering with extra spacing.
     */
    fun parseNumbered(raw: String, expected: Int): Map<Int, String> {
        val out = LinkedHashMap<Int, String>()
        val lines = raw.split('\n')
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            val match = NUMBERED.matchEntire(trimmed) ?: continue
            val index = match.groupValues[1].toIntOrNull() ?: continue
            val value = match.groupValues[2]
                .trim()
                .removeSurrounding("\"")
                .removeSurrounding("«", "»")
                .trim()
            if (index in 1..expected && value.isNotEmpty()) out[index - 1] = value
        }
        if (out.isEmpty() && expected == 1) {
            // A single-segment request sometimes comes back as bare prose.
            val only = raw.trim().removeSurrounding("\"").removeSurrounding("«", "»").trim()
            if (only.isNotEmpty()) out[0] = only
        }
        return out
    }

    fun clearCache() = cache.clear()

    fun cacheSize(): Int = cache.size()

    companion object {
        private val NUMBERED = Regex("^\\s*(\\d+)\\s*[.):\\-]\\s*(.+)$")

        private const val BATCH_INSTRUCTION =
            "You are a professional English→Persian translator embedded in an Android screen translator. " +
                "You receive a numbered list of short strings that were read from the user's screen " +
                "(interface labels, menu items, notifications, sentences). Translate each one into fluent, " +
                "natural Persian (فارسی). Rules: keep numbers, prices, dates, units, e-mail addresses, URLs, " +
                "product names, ticker symbols and code identifiers unchanged; use the standard Persian word " +
                "for interface terms; never merge, split, reorder or omit items; never add commentary. " +
                "Reply with exactly one line per item in the form \"1. ترجمه\"."
    }
}
