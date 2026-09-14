package com.example.sayvis.scripts

/** What makes a script run. */
enum class ScriptTrigger(val labelFa: String, val labelEn: String) {
    MANUAL("اجرای دستی", "Manual run"),
    CONTEXT_CHANGE("تغییر وضعیت سیستم", "System context change"),
    SCHEDULE("زمان‌بندی‌شده", "Scheduled"),
    MARKET("رویداد بازار", "Market event");

    fun label(isPersian: Boolean) = if (isPersian) labelFa else labelEn
}

/** One authored automation. Persisted by [ScriptStore]. */
data class AutomationScript(
    val id: String,
    val name: String,
    val description: String = "",
    val trigger: ScriptTrigger = ScriptTrigger.MANUAL,
    val source: String = "",
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastRunAt: Long? = null,
    val lastResultFa: String = "",
    val lastResultEn: String = "",
    val lastSuccess: Boolean? = null,
    val runCount: Int = 0
) {
    fun lastResult(isPersian: Boolean) = if (isPersian) lastResultFa else lastResultEn
}

/**
 * A side effect the script *wants* to happen.
 *
 * The interpreter never performs I/O itself. It returns effects, and the caller runs
 * them through the Zero-Trust permission engine — exactly like every other SAYVIS
 * action proposal. A script can therefore never touch the network, the wallet or the
 * kill-switch on its own.
 */
sealed class ScriptEffect {
    data class Notify(val message: String) : ScriptEffect()
    data class Log(val message: String) : ScriptEffect()
    data class Propose(val summary: String) : ScriptEffect()
    data class Webhook(val url: String, val payload: String) : ScriptEffect()
    data class SetVariable(val name: String, val value: ScriptValue) : ScriptEffect()
    data class Block(val reason: String) : ScriptEffect()
    data class SetExecutionMode(val modeName: String) : ScriptEffect()
}

/** Runtime value produced by the expression evaluator. */
sealed class ScriptValue {
    data class Num(val value: Double) : ScriptValue()
    data class Str(val value: String) : ScriptValue()
    data class Bool(val value: Boolean) : ScriptValue()
    data object Nothing : ScriptValue()

    fun asDouble(): Double? = when (this) {
        is Num -> value
        is Bool -> if (value) 1.0 else 0.0
        is Str -> value.toDoubleOrNull()
        Nothing -> null
    }

    fun asBoolean(): Boolean = when (this) {
        is Bool -> value
        is Num -> value != 0.0
        is Str -> value.equals("true", true) || value == "1"
        Nothing -> false
    }

    fun render(): String = when (this) {
        is Num -> if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
        is Str -> value
        is Bool -> value.toString()
        Nothing -> "null"
    }
}

/** Everything a script is allowed to read. Built fresh on each evaluation. */
data class ScriptContext(
    val batteryPercent: Int = 100,
    val isCharging: Boolean = false,
    val networkOnline: Boolean = true,
    val emergencyLockActive: Boolean = false,
    val activeMissions: Int = 0,
    val blockedTasks: Int = 0,
    val focusWindowActive: Boolean = false,
    val cognitiveLoad: String = "OPTIMAL",
    val hourOfDay: Int = 12,
    val minuteOfHour: Int = 0,
    val quotes: Map<String, Double> = emptyMap(),
    val accountBalance: Double = 0.0,
    val accountEquity: Double = 0.0,
    val dailyPnl: Double = 0.0,
    val variables: Map<String, ScriptValue> = emptyMap()
) {
    /** Resolves a dotted variable path such as `quote.EURUSD.bid` or `$myVar`. */
    fun resolve(path: String): ScriptValue {
        val clean = path.trim().removePrefix("$")
        if (variables.containsKey(clean)) return variables[clean] ?: ScriptValue.Nothing

        val parts = clean.split('.')
        return when (parts[0].lowercase()) {
            "battery" -> if (parts.size > 1 && parts[1] == "charging") ScriptValue.Bool(isCharging) else ScriptValue.Num(batteryPercent.toDouble())
            "network" -> ScriptValue.Bool(networkOnline)
            "lock" -> ScriptValue.Bool(emergencyLockActive)
            "focus" -> ScriptValue.Bool(focusWindowActive)
            "load" -> ScriptValue.Str(cognitiveLoad)
            "mission" -> when (parts.getOrNull(1)) {
                "blocked" -> ScriptValue.Num(blockedTasks.toDouble())
                "active" -> ScriptValue.Num(activeMissions.toDouble())
                else -> ScriptValue.Num(activeMissions.toDouble())
            }
            "time" -> when (parts.getOrNull(1)) {
                "minute" -> ScriptValue.Num(minuteOfHour.toDouble())
                else -> ScriptValue.Num(hourOfDay.toDouble())
            }
            "account" -> when (parts.getOrNull(1)) {
                "equity" -> ScriptValue.Num(accountEquity)
                "profit", "pnl" -> ScriptValue.Num(dailyPnl)
                else -> ScriptValue.Num(accountBalance)
            }
            "quote" -> {
                val symbol = parts.getOrNull(1)?.uppercase() ?: ""
                val field = parts.getOrNull(2) ?: "bid"
                val base = quotes[symbol]
                if (base == null) {
                    ScriptValue.Nothing
                } else {
                    ScriptValue.Num(
                        when (field.lowercase()) {
                            "ask" -> base * 1.00008
                            "change" -> 0.0
                            else -> base
                        }
                    )
                }
            }
            else -> ScriptValue.Nothing
        }
    }
}

/** Outcome of running one script. */
data class ScriptRunResult(
    val success: Boolean,
    val effects: List<ScriptEffect> = emptyList(),
    val logFa: String = "",
    val logEn: String = "",
    val errorLine: Int? = null,
    val errorFa: String = "",
    val errorEn: String = ""
) {
    fun log(isPersian: Boolean) = if (isPersian) logFa else logEn
    fun error(isPersian: Boolean) = if (isPersian) errorFa else errorEn
}

/** Result of a pure syntax check, used by the editor before anything is executed. */
data class ScriptValidation(
    val valid: Boolean,
    val errorLine: Int? = null,
    val errorFa: String = "",
    val errorEn: String = ""
) {
    fun error(isPersian: Boolean) = if (isPersian) errorFa else errorEn
    fun message(isPersian: Boolean): String = when {
        valid -> if (isPersian) "کد معتبر است" else "Code is valid"
        else -> error(isPersian)
    }
}
