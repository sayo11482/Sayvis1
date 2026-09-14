package com.example.sayvis.voice

import android.content.Context
import android.content.SharedPreferences

/**
 * On-device store for the owner's voice-print and listening preferences.
 *
 * The print itself is a handful of 26-dimensional unit vectors — it cannot be
 * replayed into sound and never leaves the device (plain private
 * [SharedPreferences], like the rest of SAYVIS' non-secret state).
 */
class VoicePrintStore private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("sayvis_voice", Context.MODE_PRIVATE)

    // ------------------------------------------------------------ voice print

    fun enrolledTemplates(): List<FloatArray> {
        val raw = prefs.getString(KEY_PRINT, null) ?: return emptyList()
        return runCatching { decodeTemplates(raw) }.getOrElse { emptyList() }
    }

    fun enrolledAt(): Long = prefs.getLong(KEY_PRINT_AT, 0L)

    fun hasPrint(): Boolean = enrolledTemplates().isNotEmpty()

    fun savePrint(templates: List<FloatArray>) {
        prefs.edit()
            .putString(KEY_PRINT, encodeTemplates(templates))
            .putLong(KEY_PRINT_AT, System.currentTimeMillis())
            .apply()
    }

    fun clearPrint() {
        prefs.edit().remove(KEY_PRINT).remove(KEY_PRINT_AT).apply()
    }

    /** Minimum utterance similarity for a "this is the owner" decision (0.55..0.95). */
    fun threshold(): Float = prefs.getFloat(KEY_THRESHOLD, DEFAULT_THRESHOLD)

    fun setThreshold(value: Float) {
        prefs.edit().putFloat(KEY_THRESHOLD, value.coerceIn(0.55f, 0.95f)).apply()
    }

    // -------------------------------------------------------- overlay position

    fun overlayX(default: Int = 24): Int = prefs.getInt(KEY_OVERLAY_X, default)
    fun overlayY(default: Int = 320): Int = prefs.getInt(KEY_OVERLAY_Y, default)

    fun saveOverlayPosition(x: Int, y: Int) {
        prefs.edit().putInt(KEY_OVERLAY_X, x).putInt(KEY_OVERLAY_Y, y).apply()
    }

    // --------------------------------------------------------- event history

    /** Most recent recognitions, newest first, capped at [HISTORY_LIMIT]. */
    fun history(): List<ListenBus.ListenEvent> {
        val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return runCatching {
            raw.split("|").filter { it.isNotBlank() }.map { part ->
                val fields = part.split(":")
                ListenBus.ListenEvent(
                    at = fields.getOrNull(0)?.toLongOrNull() ?: 0L,
                    score = fields.getOrNull(1)?.toFloatOrNull() ?: 0f
                )
            }
        }.getOrElse { emptyList() }
    }

    fun addHistory(event: ListenBus.ListenEvent) {
        val next = (listOf(event) + history()).take(HISTORY_LIMIT)
        val blob = next.joinToString("|") { "${it.at}:${it.score}" }
        prefs.edit().putString(KEY_HISTORY, blob).apply()
    }

    fun clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    companion object {
        const val DEFAULT_THRESHOLD = 0.78f
        const val HISTORY_LIMIT = 12

        private const val KEY_PRINT = "voice_print"
        private const val KEY_PRINT_AT = "voice_print_at"
        private const val KEY_THRESHOLD = "match_threshold"
        private const val KEY_OVERLAY_X = "overlay_x"
        private const val KEY_OVERLAY_Y = "overlay_y"
        private const val KEY_HISTORY = "listen_history"

        @Volatile
        private var instance: VoicePrintStore? = null

        fun get(context: Context): VoicePrintStore =
            instance ?: synchronized(this) {
                instance ?: VoicePrintStore(context).also { instance = it }
            }

        /**
         * Compact lossless serialisation for the template set. Kotlin's
         * [Float.toString]/[String.toFloat] pair round-trips bit-exactly, and the
         * format stays trivially inspectable: `1.0,0.5|1.0,0.5`.
         */
        fun encodeTemplates(templates: List<FloatArray>): String =
            templates.joinToString("|") { template ->
                template.joinToString(",") { value -> value.toString() }
            }

        fun decodeTemplates(blob: String): List<FloatArray> =
            blob.split("|")
                .filter { it.isNotBlank() }
                .map { part ->
                    part.split(",").mapNotNull { it.toFloatOrNull() }.toFloatArray()
                }
                .filter { it.size == VoicePrintMath.FEATURE_DIM }
    }
}
