package com.example.sayvis.screentranslate

import android.content.Context
import java.util.Collections

/**
 * Persistent, bounded cache of screen translations.
 *
 * A permanent translator sees the *same* sentences all day: a menu, a notification, a
 * dashboard row. Remembering them means each sentence is resolved once — by the dictionary
 * or, at worst, by one model call — and every later frame is free.
 */
class ScreenTranslationCache(
    private val store: KeyValueStore? = null,
    private val limit: Int = DEFAULT_LIMIT
) {

    private val entries: MutableMap<String, String> = Collections.synchronizedMap(
        object : LinkedHashMap<String, String>(64, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>): Boolean =
                size > limit
        }
    )

    init {
        store?.read()?.forEach { (key, value) ->
            if (key.isNotBlank() && value.isNotBlank()) entries[key] = value
        }
    }

    /** Cached Persian for [source], or null. */
    fun get(source: String): String? = entries[keyOf(source)]

    /** Stores a translation. Non-Persian results are never cached. */
    fun put(source: String, translation: String) {
        val key = keyOf(source)
        val value = sanitise(translation)
        if (key.isEmpty() || value.isEmpty() || !ScreenTextPlanner.isPersianText(value)) return
        entries[key] = value
        persist()
    }

    fun putAll(values: Map<String, String>) {
        var changed = false
        values.forEach { (source, translation) ->
            val key = keyOf(source)
            val value = sanitise(translation)
            if (key.isEmpty() || value.isEmpty() || !ScreenTextPlanner.isPersianText(value)) return@forEach
            entries[key] = value
            changed = true
        }
        if (changed) persist()
    }

    fun contains(source: String): Boolean = entries.containsKey(keyOf(source))

    fun clear() {
        entries.clear()
        store?.write(emptyMap())
    }

    fun size(): Int = entries.size

    /** Snapshot of the cache, for the owner-facing "learned sentences" counter. */
    fun snapshot(): Map<String, String> = entries.toMap()

    private fun persist() {
        val target = store ?: return
        target.write(entries.toMap())
    }

    private fun keyOf(source: String): String = ScreenLexicon.normalize(source).take(MAX_KEY_CHARS)

    private fun sanitise(value: String): String =
        value.replace('\n', ' ').replace('\u0001', ' ').trim()

    companion object {
        const val DEFAULT_LIMIT = 1200
        private const val MAX_KEY_CHARS = 220
    }
}

/** Minimal storage port so the cache can be unit-tested without Android. */
interface KeyValueStore {
    fun read(): Map<String, String>
    fun write(values: Map<String, String>)
}

/** In-memory store used by tests and by the "do not persist anything" mode. */
class MemoryKeyValueStore : KeyValueStore {
    private var values: Map<String, String> = emptyMap()
    override fun read(): Map<String, String> = values
    override fun write(values: Map<String, String>) {
        this.values = values.toMap()
    }
}

/**
 * SharedPreferences-backed store. Entries are packed into a single string
 * (`key` + separator + `value`, one per line) which keeps the whole cache to one atomic
 * write — no JSON runtime, no write amplification on every translated frame.
 */
class PreferencesKeyValueStore(
    context: Context,
    fileName: String = "sayvis_screen_translations"
) : KeyValueStore {

    private val prefs = context.applicationContext.getSharedPreferences(fileName, Context.MODE_PRIVATE)

    override fun read(): Map<String, String> {
        val raw = prefs.getString(KEY, null) ?: return emptyMap()
        val out = LinkedHashMap<String, String>()
        raw.split('\n').forEach { line ->
            val index = line.indexOf(SEPARATOR)
            if (index <= 0) return@forEach
            val key = line.substring(0, index)
            val value = line.substring(index + 1)
            if (key.isNotBlank() && value.isNotBlank()) out[key] = value
        }
        return out
    }

    override fun write(values: Map<String, String>) {
        val packed = values.entries.joinToString("\n") { (key, value) ->
            key.replace('\n', ' ') + SEPARATOR + value.replace('\n', ' ')
        }
        prefs.edit().putString(KEY, packed).apply()
    }

    companion object {
        private const val KEY = "screen_translation_cache_v1"
        private const val SEPARATOR = '\u0001'
    }
}
