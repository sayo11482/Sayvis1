package com.example.sayvis.scripts

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Persists the owner's automation scripts as a JSON array in private app storage.
 *
 * Kept deliberately outside the Room schema: scripts are owner-authored configuration,
 * they change shape as the language evolves, and a plain JSON document lets us add
 * fields without a database migration.
 */
class ScriptStore private constructor(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("sayvis_scripts", Context.MODE_PRIVATE)

    private val _scripts = MutableStateFlow<List<AutomationScript>>(emptyList())
    val scripts: StateFlow<List<AutomationScript>> = _scripts.asStateFlow()

    private val _lastRun = MutableStateFlow<ScriptRunResult?>(null)
    val lastRun: StateFlow<ScriptRunResult?> = _lastRun.asStateFlow()

    init {
        _scripts.value = load()
    }

    fun all(): List<AutomationScript> = _scripts.value

    fun enabled(): List<AutomationScript> = _scripts.value.filter { it.enabled }

    fun byTrigger(trigger: ScriptTrigger): List<AutomationScript> = enabled().filter { it.trigger == trigger }

    fun find(id: String): AutomationScript? = _scripts.value.firstOrNull { it.id == id }

    fun upsert(script: AutomationScript) {
        val stamped = script.copy(updatedAt = System.currentTimeMillis())
        val next = _scripts.value.toMutableList()
        val index = next.indexOfFirst { it.id == stamped.id }
        if (index >= 0) next[index] = stamped else next.add(stamped)
        _scripts.value = next
        persist(next)
    }

    fun create(name: String, description: String, trigger: ScriptTrigger, source: String): AutomationScript {
        val script = AutomationScript(
            id = "script_" + UUID.randomUUID().toString().take(8),
            name = name.trim(),
            description = description.trim(),
            trigger = trigger,
            source = source
        )
        upsert(script)
        return script
    }

    fun delete(id: String) {
        val next = _scripts.value.filterNot { it.id == id }
        _scripts.value = next
        persist(next)
    }

    fun setEnabled(id: String, enabled: Boolean) {
        val next = _scripts.value.map { if (it.id == id) it.copy(enabled = enabled) else it }
        _scripts.value = next
        persist(next)
    }

    /** Records the outcome of a run next to the script so history survives a restart. */
    fun recordRun(id: String, result: ScriptRunResult, isPersian: Boolean) {
        _lastRun.value = result
        val next = _scripts.value.map {
            if (it.id != id) it else {
                it.copy(
                    lastRunAt = System.currentTimeMillis(),
                    lastSuccess = result.success,
                    lastResultFa = result.logFa.ifBlank { result.errorFa },
                    lastResultEn = result.logEn.ifBlank { result.errorEn },
                    runCount = it.runCount + 1
                )
            }
        }
        _scripts.value = next
        persist(next)
    }

    /** Seeds the starter scripts the first time the owner opens the editor. */
    fun ensureStarters() {
        if (_scripts.value.isNotEmpty()) return
        val starters = ScriptEngine.starterScripts()
        _scripts.value = starters
        persist(starters)
    }

    // -------------------------------------------------------------- persistence

    private fun load(): List<AutomationScript> {
        val raw = prefs.getString(KEY_SCRIPTS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                AutomationScript(
                    id = item.optString("id"),
                    name = item.optString("name"),
                    description = item.optString("description"),
                    trigger = runCatching { ScriptTrigger.valueOf(item.optString("trigger")) }
                        .getOrDefault(ScriptTrigger.MANUAL),
                    source = item.optString("source"),
                    enabled = item.optBoolean("enabled", true),
                    createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = item.optLong("updatedAt", System.currentTimeMillis()),
                    lastRunAt = if (item.isNull("lastRunAt")) null else item.optLong("lastRunAt"),
                    lastResultFa = item.optString("lastResultFa"),
                    lastResultEn = item.optString("lastResultEn"),
                    lastSuccess = if (item.isNull("lastSuccess")) null else item.optBoolean("lastSuccess"),
                    runCount = item.optInt("runCount", 0)
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun persist(scripts: List<AutomationScript>) {
        val array = JSONArray()
        scripts.forEach { script ->
            array.put(
                JSONObject().apply {
                    put("id", script.id)
                    put("name", script.name)
                    put("description", script.description)
                    put("trigger", script.trigger.name)
                    put("source", script.source)
                    put("enabled", script.enabled)
                    put("createdAt", script.createdAt)
                    put("updatedAt", script.updatedAt)
                    put("lastRunAt", script.lastRunAt ?: JSONObject.NULL)
                    put("lastResultFa", script.lastResultFa)
                    put("lastResultEn", script.lastResultEn)
                    put("lastSuccess", script.lastSuccess ?: JSONObject.NULL)
                    put("runCount", script.runCount)
                }
            )
        }
        prefs.edit().putString(KEY_SCRIPTS, array.toString()).apply()
    }

    companion object {
        private const val KEY_SCRIPTS = "scripts_json_v1"

        @Volatile
        private var INSTANCE: ScriptStore? = null

        fun get(context: Context): ScriptStore =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: ScriptStore(context).also { INSTANCE = it }
            }
    }
}
