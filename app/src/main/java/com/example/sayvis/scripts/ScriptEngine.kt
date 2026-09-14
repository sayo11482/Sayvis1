package com.example.sayvis.scripts

/**
 * Interpreter for the SAYVIS automation language.
 *
 * A deliberately small, line-based, side-effect-free rule language:
 *
 * ```
 * # Everything after a hash is a comment
 * SET $risk = 2
 * WHEN battery < 20 THEN notify "Battery is at {battery}% — please charge"
 * WHEN mission.blocked > 0 THEN propose "Re-order the blocked tasks"
 * WHEN quote.XAUUSD.bid > 2600 THEN log "Gold broke 2600"
 * ON MANUAL THEN log "Ran by hand"
 * WHEN account.profit < -50 THEN block "Daily loss cap reached"
 * WHEN network.online == false THEN webhook "https://example.com/hook" "{\"state\":\"offline\"}"
 * ```
 *
 * Design rules that matter for a zero-trust product:
 *  - The engine performs **no I/O**. It returns [ScriptEffect] values and the caller
 *    decides whether to run them through the permission gate.
 *  - There is no loop, no reflection, no eval of host code, and no filesystem access,
 *    so a malicious or buggy script cannot escalate.
 *  - Unknown variables resolve to `null` and comparisons against null fail closed
 *    (the rule does not fire) rather than throwing.
 */
class ScriptEngine {

    // ------------------------------------------------------------------ parsing

    private sealed class Stmt {
        abstract val line: Int
        data class Assign(val name: String, val expression: String, override val line: Int) : Stmt()
        data class Rule(
            val trigger: String,
            val condition: String?,
            val action: String,
            override val line: Int
        ) : Stmt()
    }

    /** Parses source into statements, or returns a structured syntax error. */
    private fun parse(source: String): ParseResult {
        val statements = mutableListOf<Stmt>()
        source.lines().forEachIndexed { index, rawLine ->
            val lineNumber = index + 1
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) return@forEachIndexed

            val upper = line.uppercase()
            when {
                upper.startsWith("SET ") && !line.contains(THEN_REGEX) -> {
                    val body = line.substring(4).trim()
                    val eq = body.indexOf('=')
                    if (eq <= 0) {
                        return ParseResult(
                            null,
                            lineNumber,
                            "خطای نگارشی: عبارت «$body» باید به شکل SET \$name = مقدار باشد.",
                            "Syntax error: \"$body\" must look like SET \$name = value."
                        )
                    }
                    statements.add(
                        Stmt.Assign(
                            name = body.substring(0, eq).trim().removePrefix("$"),
                            expression = body.substring(eq + 1).trim(),
                            line = lineNumber
                        )
                    )
                }

                THEN_REGEX.containsMatchIn(line) -> {
                    val parts = THEN_REGEX.split(line, limit = 2)
                    val head = parts[0].trim()
                    val action = parts.getOrElse(1) { "" }.trim()
                    if (action.isEmpty()) {
                        return ParseResult(
                            null,
                            lineNumber,
                            "خطای نگارشی: بعد از THEN باید یک عمل بیاید (notify، log، propose، webhook، set یا block).",
                            "Syntax error: an action must follow THEN (notify, log, propose, webhook, set or block)."
                        )
                    }
                    val headUpper = head.uppercase()
                    val (trigger, condition) = when {
                        headUpper.startsWith("WHEN ") -> "CONTEXT" to head.substring(5).trim()
                        headUpper.startsWith("IF ") -> "CONTEXT" to head.substring(3).trim()
                        headUpper.startsWith("ON ") -> {
                            val eventName = head.substring(3).trim().uppercase()
                            if (eventName !in KNOWN_TRIGGERS) {
                                return ParseResult(
                                    null,
                                    lineNumber,
                                    "خطای نگارشی: رویداد «$eventName» نامعتبر است. رویدادهای مجاز: MANUAL، CONTEXT_CHANGE، SCHEDULE، MARKET.",
                                    "Syntax error: \"$eventName\" is not a valid event. Allowed: MANUAL, CONTEXT_CHANGE, SCHEDULE, MARKET."
                                )
                            }
                            eventName to null
                        }
                        else -> return ParseResult(
                            null,
                            lineNumber,
                            "خطای نگارشی: خط باید با WHEN، IF، ON یا SET شروع شود.",
                            "Syntax error: a line must start with WHEN, IF, ON or SET."
                        )
                    }
                    if (!isKnownAction(action)) {
                        return ParseResult(
                            null,
                            lineNumber,
                            "خطای نگارشی: عمل «$action» شناخته نشد. عمل‌های مجاز: notify \"…\"، log \"…\"، propose \"…\"، block \"…\"، webhook \"نشانی\" \"بدنه\"، set \$نام = مقدار، set_mode PAPER|DEMO|LIVE.",
                            "Syntax error: unknown action \"$action\". Allowed: notify \"…\", log \"…\", propose \"…\", block \"…\", webhook \"url\" \"body\", set \$name = value, set_mode PAPER|DEMO|LIVE."
                        )
                    }
                    statements.add(Stmt.Rule(trigger, condition, action, lineNumber))
                }

                else -> return ParseResult(
                    null,
                    lineNumber,
                    "خطای نگارشی: خط باید با WHEN، IF، ON یا SET شروع شود.",
                    "Syntax error: a line must start with WHEN, IF, ON or SET."
                )
            }
        }
        return ParseResult(statements, null, "", "")
    }

    private data class ParseResult(
        val statements: List<Stmt>?,
        val errorLine: Int?,
        val errorFa: String,
        val errorEn: String
    ) {
        val isValid: Boolean get() = statements != null
    }

    /**
     * Public syntax check used by the editor to underline a bad line before the owner
     * runs anything.
     */
    fun validate(source: String): ScriptValidation {
        val result = parse(source)
        return ScriptValidation(
            valid = result.isValid,
            errorLine = result.errorLine,
            errorFa = result.errorFa,
            errorEn = result.errorEn
        )
    }

    // ---------------------------------------------------------------- evaluation

    /**
     * Runs [source] against [context].
     *
     * @param activeTrigger which trigger is firing right now ("MANUAL", "CONTEXT_CHANGE",
     *                      "MARKET" or "SCHEDULE"). Rules with a different `ON` trigger
     *                      are skipped; `WHEN` rules always evaluate their condition.
     */
    fun run(source: String, context: ScriptContext, activeTrigger: String = "MANUAL"): ScriptRunResult {
        val parsed = parse(source)
        if (!parsed.isValid) {
            return ScriptRunResult(
                success = false,
                errorLine = parsed.errorLine,
                errorFa = parsed.errorFa,
                errorEn = parsed.errorEn
            )
        }

        val effects = mutableListOf<ScriptEffect>()
        val logsFa = mutableListOf<String>()
        val logsEn = mutableListOf<String>()
        val variables = context.variables.toMutableMap()
        var matched = 0

        for (stmt in parsed.statements ?: emptyList()) {
            when (stmt) {
                is Stmt.Assign -> {
                    val value = evaluate(stmt.expression, context.copy(variables = variables))
                    if (value == null) {
                        return ScriptRunResult(
                            success = false,
                            effects = effects,
                            errorLine = stmt.line,
                            errorFa = "عبارت «${stmt.expression}» قابل محاسبه نیست.",
                            errorEn = "Cannot evaluate the expression \"${stmt.expression}\"."
                        )
                    }
                    variables[stmt.name] = value
                    effects.add(ScriptEffect.SetVariable(stmt.name, value))
                }

                is Stmt.Rule -> {
                    val triggerMatches = when {
                        stmt.condition != null -> true // WHEN/IF: gated by the condition
                        stmt.trigger == activeTrigger -> true
                        stmt.trigger == "MANUAL" && activeTrigger == "MANUAL" -> true
                        else -> false
                    }
                    if (!triggerMatches) continue

                    if (stmt.condition != null && !evaluateCondition(stmt.condition, context.copy(variables = variables))) {
                        continue
                    }

                    matched++
                    val effect = interpretAction(stmt.action, context.copy(variables = variables))
                    if (effect == null) {
                        return ScriptRunResult(
                            success = false,
                            effects = effects,
                            errorLine = stmt.line,
                            errorFa = "عمل «${stmt.action}» شناخته نشد. عمل‌های مجاز: notify، log، propose، webhook، set، block، set_mode.",
                            errorEn = "Unknown action \"${stmt.action}\". Allowed: notify, log, propose, webhook, set, block, set_mode."
                        )
                    }
                    effects.add(effect)
                    val rendered = effect.describe()
                    logsFa.add("خط ${stmt.line}: $rendered")
                    logsEn.add("Line ${stmt.line}: $rendered")
                }
            }
        }

        if (matched == 0) {
            logsFa.add("هیچ شرطی برقرار نشد؛ اقدامی تولید نشد.")
            logsEn.add("No condition matched; no action produced.")
        }

        return ScriptRunResult(
            success = true,
            effects = effects,
            logFa = logsFa.joinToString("\n"),
            logEn = logsEn.joinToString("\n")
        )
    }

    private fun ScriptEffect.describe(): String = when (this) {
        is ScriptEffect.Notify -> "notify → $message"
        is ScriptEffect.Log -> "log → $message"
        is ScriptEffect.Propose -> "propose → $summary"
        is ScriptEffect.Webhook -> "webhook → $url"
        is ScriptEffect.SetVariable -> "set \$$name = ${value.render()}"
        is ScriptEffect.Block -> "block → $reason"
        is ScriptEffect.SetExecutionMode -> "set_mode → $modeName"
    }

    // ------------------------------------------------------------------- actions

    /**
     * Static shape check for an action, run at parse time so the editor can reject a
     * typo'd action before it ever fires. Kept in lock-step with [interpretAction] by
     * reusing exactly the same patterns.
     */
    private fun isKnownAction(action: String): Boolean {
        val trimmed = action.trim()
        if (trimmed.isEmpty()) return false
        if (NOTIFY_REGEX.matchEntire(trimmed) != null) return true
        if (LOG_REGEX.matchEntire(trimmed) != null) return true
        if (PROPOSE_REGEX.matchEntire(trimmed) != null) return true
        if (BLOCK_REGEX.matchEntire(trimmed) != null) return true
        if (WEBHOOK_REGEX.matchEntire(trimmed) != null) return true
        if (SET_MODE_REGEX.matchEntire(trimmed) != null) {
            val mode = SET_MODE_REGEX.matchEntire(trimmed)?.groupValues?.get(1)?.uppercase()
            return mode == "PAPER" || mode == "DEMO" || mode == "LIVE"
        }
        if (trimmed.lowercase().startsWith("set ")) {
            val body = trimmed.substring(4).trim()
            return body.indexOf('=') > 0
        }
        return false
    }

    private fun interpretAction(action: String, context: ScriptContext): ScriptEffect? {
        val trimmed = action.trim()
        val lower = trimmed.lowercase()

        NOTIFY_REGEX.matchEntire(trimmed)?.let { return ScriptEffect.Notify(interpolate(it.groupValues[1], context)) }
        LOG_REGEX.matchEntire(trimmed)?.let { return ScriptEffect.Log(interpolate(it.groupValues[1], context)) }
        PROPOSE_REGEX.matchEntire(trimmed)?.let { return ScriptEffect.Propose(interpolate(it.groupValues[1], context)) }
        BLOCK_REGEX.matchEntire(trimmed)?.let { return ScriptEffect.Block(interpolate(it.groupValues[1], context)) }

        WEBHOOK_REGEX.matchEntire(trimmed)?.let { match ->
            return ScriptEffect.Webhook(
                url = interpolate(match.groupValues[1], context),
                payload = interpolate(match.groupValues.getOrNull(2) ?: "", context)
            )
        }

        SET_MODE_REGEX.matchEntire(trimmed)?.let {
            return ScriptEffect.SetExecutionMode(it.groupValues[1].uppercase())
        }

        if (lower.startsWith("set ")) {
            val body = trimmed.substring(4).trim()
            val eq = body.indexOf('=')
            if (eq > 0) {
                val name = body.substring(0, eq).trim().removePrefix("$")
                val value = evaluate(body.substring(eq + 1).trim(), context) ?: return null
                return ScriptEffect.SetVariable(name, value)
            }
        }
        return null
    }

    /** Replaces `{path}` placeholders with live context values. */
    fun interpolate(template: String, context: ScriptContext): String =
        template.replace(INTERPOLATION_REGEX) { match ->
            val path = match.groupValues[1]
            val resolved = context.resolve(path)
            if (resolved == ScriptValue.Nothing) path else resolved.render()
        }

    // --------------------------------------------------------------- expressions

    private fun evaluateCondition(condition: String, context: ScriptContext): Boolean {
        val match = COMPARISON_REGEX.matchEntire(condition.trim()) ?: return false
        val left = evaluate(match.groupValues[1].trim(), context) ?: return false
        val operator = match.groupValues[2].trim()
        val right = evaluate(match.groupValues[3].trim(), context) ?: return false

        // Fail closed: an unknown variable never satisfies a comparison.
        if (left == ScriptValue.Nothing || right == ScriptValue.Nothing) return false

        val lNum = left.asDouble()
        val rNum = right.asDouble()

        return when (operator) {
            "==" -> if (lNum != null && rNum != null) kotlin.math.abs(lNum - rNum) < 1e-9 else left.render() == right.render()
            "!=" -> if (lNum != null && rNum != null) kotlin.math.abs(lNum - rNum) >= 1e-9 else left.render() != right.render()
            "<" -> lNum != null && rNum != null && lNum < rNum
            ">" -> lNum != null && rNum != null && lNum > rNum
            "<=" -> lNum != null && rNum != null && lNum <= rNum
            ">=" -> lNum != null && rNum != null && lNum >= rNum
            else -> false
        }
    }

    /** Evaluates an arithmetic / literal / variable expression. */
    fun evaluate(expression: String, context: ScriptContext): ScriptValue? {
        val tokens = tokenize(expression) ?: return null
        val parser = ExpressionParser(tokens, context)
        val value = parser.parseExpression() ?: return null
        return if (parser.atEnd()) value else null
    }

    private fun tokenize(input: String): List<String>? {
        val tokens = mutableListOf<String>()
        var index = 0
        while (index < input.length) {
            val ch = input[index]
            when {
                ch.isWhitespace() -> index++
                ch == '"' -> {
                    val end = input.indexOf('"', index + 1)
                    if (end < 0) return null
                    tokens.add(input.substring(index, end + 1))
                    index = end + 1
                }
                ch in "+-*/()" -> {
                    tokens.add(ch.toString())
                    index++
                }
                ch.isDigit() || ch == '.' -> {
                    var end = index
                    while (end < input.length && (input[end].isDigit() || input[end] == '.')) end++
                    tokens.add(input.substring(index, end))
                    index = end
                }
                ch.isLetter() || ch == '_' || ch == '$' -> {
                    var end = index
                    while (end < input.length && (input[end].isLetterOrDigit() || input[end] == '_' || input[end] == '.' || input[end] == '$')) end++
                    tokens.add(input.substring(index, end))
                    index = end
                }
                else -> return null
            }
        }
        return tokens
    }

    /**
     * Recursive-descent parser.
     *
     * expr   := term (('+'|'-') term)*
     * term   := factor (('*'|'/') factor)*
     * factor := ['-'] ( '(' expr ')' | number | "string" | true | false | variable )
     *
     * A leading '-' is only unary when an operand is expected, so `0 - $cap` parses as
     * subtraction (yielding -50 for cap=50) while `-$cap` parses as negation.
     */
    private class ExpressionParser(private val tokens: List<String>, private val context: ScriptContext) {
        private var position = 0

        /** True when the next token must start an operand, which makes '-' unary. */
        private var operandExpected = true

        fun atEnd(): Boolean = position >= tokens.size

        private fun peek(): String? = tokens.getOrNull(position)
        private fun next(): String? = tokens.getOrNull(position++)

        fun parseExpression(): ScriptValue? {
            var left = parseTerm() ?: return null
            while (true) {
                when (peek()) {
                    "+" -> {
                        next()
                        operandExpected = true
                        val right = parseTerm() ?: return null
                        left = combine(left, right, '+') ?: return null
                        operandExpected = false
                    }
                    "-" -> {
                        next()
                        operandExpected = true
                        val right = parseTerm() ?: return null
                        left = combine(left, right, '-') ?: return null
                        operandExpected = false
                    }
                    else -> return left
                }
            }
        }

        private fun parseTerm(): ScriptValue? {
            var left = parseFactor() ?: return null
            while (true) {
                when (peek()) {
                    "*" -> {
                        next()
                        operandExpected = true
                        val right = parseFactor() ?: return null
                        left = combine(left, right, '*') ?: return null
                        operandExpected = false
                    }
                    "/" -> {
                        next()
                        operandExpected = true
                        val right = parseFactor() ?: return null
                        left = combine(left, right, '/') ?: return null
                        operandExpected = false
                    }
                    else -> return left
                }
            }
        }

        private fun parseFactor(): ScriptValue? {
            val token = next() ?: return null

            if (token == "-" && operandExpected) {
                operandExpected = true
                val inner = parseFactor() ?: return null
                val value = inner.asDouble() ?: return null
                operandExpected = false
                return ScriptValue.Num(-value)
            }

            if (token == "(") {
                operandExpected = true
                val inner = parseExpression() ?: return null
                if (next() != ")") return null
                operandExpected = false
                return inner
            }

            if (token == ")") return null

            operandExpected = false

            if (token.startsWith("\"") && token.endsWith("\"") && token.length >= 2) {
                return ScriptValue.Str(token.substring(1, token.length - 1))
            }
            token.toDoubleOrNull()?.let { return ScriptValue.Num(it) }
            if (token.equals("true", true)) return ScriptValue.Bool(true)
            if (token.equals("false", true)) return ScriptValue.Bool(false)
            return context.resolve(token)
        }

        private fun combine(left: ScriptValue, right: ScriptValue, op: Char): ScriptValue? {
            // String concatenation with + is allowed, which makes notify messages composable.
            if (op == '+' && (left is ScriptValue.Str || right is ScriptValue.Str)) {
                return ScriptValue.Str(left.render() + right.render())
            }
            val a = left.asDouble() ?: return null
            val b = right.asDouble() ?: return null
            return ScriptValue.Num(
                when (op) {
                    '+' -> a + b
                    '-' -> a - b
                    '*' -> a * b
                    '/' -> if (kotlin.math.abs(b) < 1e-12) return null else a / b
                    else -> return null
                }
            )
        }
    }

    companion object {
        private val THEN_REGEX = Regex("(?i)\\s+THEN\\s+")
        private val INTERPOLATION_REGEX = Regex("\\{([^}]+)\\}")
        private val COMPARISON_REGEX = Regex("^(.+?)\\s*(<=|>=|==|!=|<|>)\\s*(.+)$")

        /** Trigger names accepted by `ON … THEN …`. */
        private val KNOWN_TRIGGERS: Set<String> = ScriptTrigger.entries.map { it.name }.toSet()

        private val NOTIFY_REGEX = Regex("(?i)^notify\\s+\"(.*)\"$")
        private val LOG_REGEX = Regex("(?i)^log\\s+\"(.*)\"$")
        private val PROPOSE_REGEX = Regex("(?i)^propose\\s+\"(.*)\"$")
        private val BLOCK_REGEX = Regex("(?i)^block\\s+\"(.*)\"$")
        private val WEBHOOK_REGEX = Regex("(?i)^webhook\\s+\"([^\"]+)\"(?:\\s+\"(.*)\")?$")
        private val SET_MODE_REGEX = Regex("(?i)^set_mode\\s+(\\w+)$")

        /** Starter scripts shown in the editor so the language is learnable by example. */
        fun starterScripts(): List<AutomationScript> {
            val now = System.currentTimeMillis()
            return listOf(
                AutomationScript(
                    id = "script_starter_battery",
                    name = "نگهبان باتری",
                    description = "هشدار و پیشنهاد صرفه‌جویی وقتی شارژ کم می‌شود",
                    trigger = ScriptTrigger.CONTEXT_CHANGE,
                    source = """
                        # When the battery drops, warn the owner and ask to save a checkpoint.
                        WHEN battery < 20 THEN notify "Battery is at {battery}% — consider charging"
                        WHEN battery < 10 THEN propose "Enable power saving and flush the local checkpoint"
                        WHEN battery.charging == true THEN log "Charging started"
                    """.trimIndent(),
                    createdAt = now,
                    updatedAt = now
                ),
                AutomationScript(
                    id = "script_starter_focus",
                    name = "سپر تمرکز شبانه",
                    description = "بعد از ساعت ۲۲ اعلان‌ها را محدود می‌کند",
                    trigger = ScriptTrigger.SCHEDULE,
                    source = """
                        WHEN time.hour >= 22 THEN notify "Wind-down window — non-critical alerts are muted"
                        WHEN focus.active == true THEN propose "Shield the focus window from interruptions"
                        WHEN load == "FATIGUE_RISK" THEN log "Cognitive fatigue detected; schedule a break"
                    """.trimIndent(),
                    createdAt = now,
                    updatedAt = now
                ),
                AutomationScript(
                    id = "script_starter_market",
                    name = "پایش سقف زیان معاملاتی",
                    description = "در رسیدن به سقف زیان، اجرای سفارش را مسدود می‌کند",
                    trigger = ScriptTrigger.MARKET,
                    source = """
                        SET ${'$'}cap = 50
                        WHEN account.profit < 0 - ${'$'}cap THEN block "Daily loss cap reached — new orders are blocked"
                        WHEN account.profit > ${'$'}cap THEN notify "Daily target reached: {account.profit}"
                        WHEN quote.XAUUSD.bid > 2600 THEN log "Gold crossed 2600"
                    """.trimIndent(),
                    createdAt = now,
                    updatedAt = now
                ),
                AutomationScript(
                    id = "script_starter_offline",
                    name = "اعلان قطع اتصال",
                    description = "هنگام آفلاین شدن به سرویس شخصی وب‌هوک می‌زند",
                    trigger = ScriptTrigger.CONTEXT_CHANGE,
                    source = """
                        WHEN network.online == false THEN notify "Offline — SAYVIS switched to the local core"
                        WHEN network.online == false THEN webhook "https://example.com/sayvis/hook" "{\"state\":\"offline\"}"
                        WHEN lock.active == true THEN log "Emergency lock engaged by the owner"
                    """.trimIndent(),
                    createdAt = now,
                    updatedAt = now
                )
            )
        }

        /** In-app language reference, shown under the editor. */
        fun reference(isPersian: Boolean): String = if (isPersian) {
            """
            زبان اسکریپت سایویس — راهنمای سریع

            هر خط یکی از این چهار شکل را دارد:
            •  SET ${'$'}نام = مقدار
            •  WHEN شرط THEN عمل
            •  IF شرط THEN عمل          (هم‌معنی WHEN)
            •  ON رویداد THEN عمل       (رویداد: MANUAL / CONTEXT_CHANGE / SCHEDULE / MARKET)
            خط‌هایی که با # شروع می‌شوند توضیح‌اند.

            متغیرهای در دسترس:
            battery ، battery.charging ، network.online ، lock.active ، focus.active
            load ، mission.active ، mission.blocked ، time.hour ، time.minute
            account.balance ، account.equity ، account.profit
            quote.نماد.bid ، quote.نماد.ask

            عملگرهای شرط: < ، > ، <= ، >= ، == ، !=
            عملگرهای ریاضی: + ، - ، * ، / و پرانتز

            عمل‌ها:
            •  notify "متن"        → نمایش پیام به شما
            •  log "متن"           → نوشتن در کنسول
            •  propose "متن"       → ساخت یک پیشنهاد اجرایی (نیازمند تأیید شما)
            •  block "دلیل"        → مسدودسازی اقدام بعدی
            •  webhook "نشانی" "بدنه" → ارسال درخواست HTTP (نیازمند تأیید شما)
            •  set ${'$'}نام = مقدار
            •  set_mode PAPER|DEMO|LIVE

            درون متن می‌توانید از {متغیر} استفاده کنید؛ مقدار واقعی جایگزین می‌شود.
            نکتهٔ امنیتی: اسکریپت هیچ کاری را مستقیم اجرا نمی‌کند؛ فقط «اثر» تولید می‌کند و
            اجرای هر اثر از دروازهٔ اعتماد صفر و تأیید شما عبور می‌کند.
            """.trimIndent()
        } else {
            """
            SAYVIS scripting language — quick reference

            Every line takes one of four forms:
            •  SET ${'$'}name = value
            •  WHEN condition THEN action
            •  IF condition THEN action        (same as WHEN)
            •  ON event THEN action            (event: MANUAL / CONTEXT_CHANGE / SCHEDULE / MARKET)
            Lines starting with # are comments.

            Available variables:
            battery, battery.charging, network.online, lock.active, focus.active
            load, mission.active, mission.blocked, time.hour, time.minute
            account.balance, account.equity, account.profit
            quote.SYMBOL.bid, quote.SYMBOL.ask

            Comparison operators: < , > , <= , >= , == , !=
            Arithmetic: + , - , * , / and parentheses

            Actions:
            •  notify "text"        → show a message to the owner
            •  log "text"           → write to the console
            •  propose "text"       → create an action proposal (needs your approval)
            •  block "reason"       → stop the next action
            •  webhook "url" "body" → send an HTTP request (needs your approval)
            •  set ${'$'}name = value
            •  set_mode PAPER|DEMO|LIVE

            Inside any text you can use {variable} and the live value is substituted.
            Security note: a script never acts on its own. It only produces effects, and
            every effect passes through the zero-trust gate and your confirmation.
            """.trimIndent()
        }
    }
}
