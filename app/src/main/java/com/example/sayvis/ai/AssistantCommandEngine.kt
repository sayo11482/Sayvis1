package com.example.sayvis.ai

import kotlin.math.abs
import kotlin.math.pow

/**
 * The offline command-analysis layer of the SAYVIS assistant.
 *
 * Every chat turn is parsed here BEFORE any language model is consulted. When the
 * owner asks for something concrete ("یک مأموریت بساز…", "یادت باشه که…", "حساب کن…",
 * "وضعیت رو گزارش بده") the engine returns a structured [AssistantCommand] with the
 * extracted slots, which the app then actually executes — with the audit trail and,
 * for privileged switches, an explicit owner consent step.
 *
 * Pure Kotlin: no Android APIs, fully unit-testable. Bilingual (Persian + English)
 * and tolerant of ZWNJ, Arabic yeh/kaf and Persian digits.
 */

/** One parsed owner command, ready to execute. */
sealed class AssistantCommand {
    /** Create a real mission; [taskCount] >= 1 also pre-creates that many open tasks. */
    data class CreateMission(val title: String, val taskCount: Int = 0, val urgent: Boolean = false) : AssistantCommand()

    /** Store a durable fact in the on-device long-term memory. */
    data class Remember(val fact: String) : AssistantCommand()

    /** Search long-term memory (and the UIC profile) and answer with the hits. */
    data class Recall(val query: String) : AssistantCommand()

    /** Evaluate an arithmetic expression safely and answer with the number. */
    data class Calculate(val expression: String, val value: Double) : AssistantCommand()

    /** Live system + mission report (battery, network, locks, mission counts…). */
    object StatusReport : AssistantCommand()

    /** List the owner's missions with progress. */
    object ShowMissions : AssistantCommand()

    /** Run the AWARE proactive scan now. */
    object RunAwareScan : AssistantCommand()

    /** Open a specific screen; [target] is the enum name of [com.example.sayvis.ui.SayvisScreen]. */
    data class Navigate(val target: String) : AssistantCommand()

    /** Engage / disengage the emergency lock — always goes through owner consent. */
    data class ToggleEmergencyLock(val engage: Boolean) : AssistantCommand()

    /** Turn forced-offline mode on / off — always goes through owner consent. */
    data class ToggleOffline(val enable: Boolean) : AssistantCommand()

    /**
     * Set a real device alarm (AlarmClock intent) — [hour]/[minute] for absolute
     * times («آلارم ۶:۳۰») or [inMinutes] for durations («۲۰ دقیقه دیگر»).
     */
    data class CreateAlarm(
        val label: String,
        val hour: Int? = null,
        val minute: Int? = null,
        val inMinutes: Int? = null
    ) : AssistantCommand()

    /** Current device time. */
    object TimeQuery : AssistantCommand()

    /** Today's date. */
    object DateQuery : AssistantCommand()

    /** Battery level & charging state. */
    object BatteryQuery : AssistantCommand()

    /** Everything the assistant can do. */
    object Help : AssistantCommand()
}

object AssistantCommandEngine {

    // ------------------------------------------------------------- normalise

    /** Normalises Persian/Arabic orthography so matching works for real-world typing. */
    fun normalize(raw: String): String = buildString(raw.length) {
        for (ch in raw.lowercase()) {
            when (ch) {
                'ﺁ', 'آ', 'أ', 'إ' -> append('ا')
                'ي', 'ى' -> append('ی')
                'ك' -> append('ک')
                'ة' -> append('ه')
                '\u200c', '\u200f', '\u200e', '\u064b', '\u064c', '\u064d', '\u064e', '\u064f', '\u0650', '\u0651', '\u0652' -> append(' ')
                '۰' -> append('0'); '۱' -> append('1'); '۲' -> append('2'); '۳' -> append('3'); '۴' -> append('4')
                '۵' -> append('5'); '۶' -> append('6'); '۷' -> append('7'); '۸' -> append('8'); '۹' -> append('9')
                '٠' -> append('0'); '١' -> append('1'); '٢' -> append('2'); '٣' -> append('3'); '٤' -> append('4')
                '٥' -> append('5'); '٦' -> append('6'); '٧' -> append('7'); '٨' -> append('8'); '٩' -> append('9')
                else -> append(ch)
            }
        }
    }.replace(Regex("\\s+"), " ").trim()

    private fun startsWithAny(text: String, prefixes: List<String>): String? =
        prefixes.filter { text.startsWith(it) }.maxByOrNull { it.length }

    private fun containsAny(text: String, needles: List<String>): Boolean =
        needles.any { text.contains(it) }

    // ------------------------------------------------------------ alarm (v5.3.0)

    private val ALARM_TRIGGERS = listOf(
        "آلارم", "الارم", "بیدارم کن", "زنگ ساعت", "یادآوری کن", "یاداوری کن", "یادم بنداز",
        "set alarm", "set an alarm", "wake me", "alarm at", "remind me in", "set timer"
    )

    /** Persian/Arabic → ASCII digits (pure; unit-tested via parseAlarm). */
    fun toEnglishDigits(raw: String): String = buildString(raw.length) {
        for (ch in raw) {
            when (ch) {
                '۰' -> append('0'); '۱' -> append('1'); '۲' -> append('2'); '۳' -> append('3'); '۴' -> append('4')
                '۵' -> append('5'); '۶' -> append('6'); '۷' -> append('7'); '۸' -> append('8'); '۹' -> append('9')
                '٠' -> append('0'); '١' -> append('1'); '٢' -> append('2'); '٣' -> append('3'); '٤' -> append('4')
                '٥' -> append('5'); '٦' -> append('6'); '٧' -> append('7'); '٨' -> append('8'); '٩' -> append('9')
                else -> append(ch)
            }
        }
    }

    /**
     * Parses «آلارم ۶:۳۰ بذار», «بیدارم کن ساعت 6:30», «۲۰ دقیقه دیگر یادآوری کن»,
     * «set alarm at 06:30», «remind me in 20 minutes» → CreateAlarm. Pure.
     */
    fun parseAlarm(rawInput: String): AssistantCommand.CreateAlarm? {
        val text = normalize(rawInput)
        if (!containsAny(text, ALARM_TRIGGERS)) return null
        val ascii = toEnglishDigits(text)

        val clock = Regex("(\\d{1,2})\\s*[:：]\\s*(\\d{2})").find(ascii)
        var hour: Int? = null
        var minute: Int? = null
        if (clock != null) {
            val h = clock.groupValues[1].toIntOrNull()?.takeIf { it in 0..23 }
            val m = clock.groupValues[2].toIntOrNull()?.takeIf { it in 0..59 }
            if (h != null && m != null) {
                hour = h
                minute = m
            }
        }

        var inMinutes: Int? = null
        if (hour == null) {
            val durMin = Regex("(\\d{1,4})\\s*(دقیقه|minute|min)").find(ascii)?.groupValues?.get(1)?.toIntOrNull()
            val durHour = Regex("(\\d{1,2})\\s*(ساعت|hour|hr)").find(ascii)?.groupValues?.get(1)?.toIntOrNull()
            val computed = (durHour ?: 0) * 60 + (durMin ?: 0)
            if (computed > 0) inMinutes = computed.coerceAtMost(60 * 24 * 7)
        }

        if (hour == null && inMinutes == null) {
            // «آلارم ساعت ۶» — hour only, minute 0.
            val hourOnly = Regex("(?:ساعت\\s*|at\\s*)(\\d{1,2})(?![:\\d])").find(ascii)
            val h = hourOnly?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it in 0..23 }
            if (h != null) {
                hour = h
                minute = 0
            }
        }
        if (hour == null && inMinutes == null) return null

        val label = rawInput.trim().take(60)
        return AssistantCommand.CreateAlarm(label = label, hour = hour, minute = minute, inMinutes = inMinutes)
    }


    private fun stripLeading(text: String, fillers: List<String>): String {
        var t = text.trim()
        var changed = true
        while (changed) {
            changed = false
            val hit = startsWithAny(t, fillers)
            if (hit != null) {
                t = t.removePrefix(hit).trim()
                changed = true
            }
        }
        return t.trim('،', ',', '؛', ';', ':', ' ', '.')
    }

    private val FILLERS = listOf(
        "لطفا", "لطفاً", "سایو", "سایویس", "میخوام", "می خوام", "میخام", "بده", "کن", "کن لطفا",
        "please", "sayo", "sayvis", "hey", "هی", "برام", "برای من", "واسه من", "می تونی", "میتونی",
        "می توانی", "can you", "could you", "i want to", "i want"
    )

    // --------------------------------------------------------------- parser

    /** Parses one chat turn; `null` means "no explicit command — ask the AI provider". */
    fun parse(rawInput: String): AssistantCommand? {
        val text = normalize(rawInput)
        if (text.isBlank()) return null

        // ---- help (before everything, it is unambiguous)
        if (containsAny(text, listOf("چیکار میتونی", "چه کارهایی", "قابلیتات", "تواناییهات", "راهنما", "کمک",
                "what can you do", "help me", "your capabilities", "your abilities", "/help")) &&
            !text.contains("مأموریت") && !text.contains("mission")
        ) return AssistantCommand.Help

        // ---- emergency lock
        if (containsAny(text, listOf("قفل اضطراری", "emergency lock", "kill switch"))) {
            if (containsAny(text, listOf("خاموش", "غیرفعال", "بردار", "باز کن", "قطع کن", "غیر فعال",
                    "disengage", "deactivate", "turn off", "disable", "release", "off"))) {
                return AssistantCommand.ToggleEmergencyLock(engage = false)
            }
            if (containsAny(text, listOf("فعال", "بزن", "روشن", "نگه دار", "engage", "activate", "enable", "turn on", "on"))) {
                return AssistantCommand.ToggleEmergencyLock(engage = true)
            }
            // mention without a switch -> not a command
        }

        // ---- offline mode
        if (containsAny(text, listOf("حالت افلاین", "حالت آفلاین", "offline mode", "force offline"))) {
            if (containsAny(text, listOf("خاموش", "غیرفعال", "قطع", "بردار", "غیر فعال", "disengage", "deactivate", "turn off", "disable", "exit"))) {
                return AssistantCommand.ToggleOffline(enable = false)
            }
            if (containsAny(text, listOf("روشن", "فعال", "بزن", "برو", "activate", "enable", "turn on", "enter", "go"))) {
                return AssistantCommand.ToggleOffline(enable = true)
            }
        }

        // ---- remember / recall (before missions: "یادت باشه" is not a mission)
        if (containsAny(text, listOf("یادت باشه", "یادت باشد", "یادت باش", "به خاطر بسپار", "یادداشت کن",
                "remember that", "remember:", "note that", "keep in mind"))) {
            val fact = extractAfterKeyword(text, listOf(
                "یادت باشه که", "یادت باشه", "یادت باشد که", "یادت باشد", "یادت باش", "به خاطر بسپار که",
                "به خاطر بسپار", "یادداشت کن که", "یادداشت کن", "remember that", "remember:", "note that", "keep in mind"
            ))
            if (fact.isNotBlank()) return AssistantCommand.Remember(fact)
        }
        if (containsAny(text, listOf("یادت هست", "یادت است", "یادته", "یادت اومد", "چه چیزهایی رو یادت",
                "چه چیزهایی را یادت", "do you remember", "what do you remember", "recall"))) {
            val query = extractAfterKeyword(text, listOf(
                "چه چیزهایی رو یادت", "چه چیزهایی را یادت", "یادت هست که", "یادت هست", "یادت است که", "یادت است",
                "یادته که", "یادته", "یادت اومد", "do you remember", "what do you remember about", "what do you remember", "recall"
            ))
            return AssistantCommand.Recall(query.trim())
        }

        // ---- calculate
        parseCalculation(text)?.let { return it }

        // ---- alarm / reminder (real AlarmClock intent; v5.3.0)
        parseAlarm(text)?.let { return it }

        // ---- battery
        if (containsAny(text, listOf("باتری چقدر", "باتری چنده", "شارژ چقدر", "شارژ چنده", "battery level",
                "how much battery", "battery status"))) return AssistantCommand.BatteryQuery

        // ---- time / date
        if (containsAny(text, listOf("ساعت چنده", "ساعت چند", "چه ساعتیه", "چه ساعتی است", "what time", "current time"))) {
            return AssistantCommand.TimeQuery
        }
        if (containsAny(text, listOf("تاریخ امروز", "امروز چندمه", "امروز چندمه", "امروز چه روزیه", "تاریخ چندمه",
                "today's date", "what date", "todays date", "which day"))) {
            return AssistantCommand.DateQuery
        }

        // ---- navigation ("open ...", "برو به ...", "باز کن ...")
        parseNavigate(text)?.let { return it }

        // ---- aware scan
        if (containsAny(text, listOf("اسکن کن", "اسکن", "بررسی هوشمند", "پیشنهاد هوشمند", "پیشنهاد بده",
                "run a scan", "run scan", "smart suggestions", "proactive scan"))) {
            return AssistantCommand.RunAwareScan
        }

        // ---- mission creation (must come BEFORE listing)
        parseCreateMission(text)?.let { return it }

        // ---- mission listing / status report
        if (containsAny(text, listOf("وضعیت", "گزارش بده", "گزارش وضعیت", "summary", "status report", "report status",
                "how are things", "چه خبر"))) {
            return AssistantCommand.StatusReport
        }
        if (containsAny(text, listOf("مأموریت ها", "ماموریت ها", "لیست مأموریت", "نمایش مأموریت", "نشون بده مأموریت",
                "مأموریتها رو نشون", "show missions", "list missions", "my missions", "list my goals"))) {
            return AssistantCommand.ShowMissions
        }

        return null
    }

    // ------------------------------------------------- mission slot filling

    private val CREATE_TRIGGERS = listOf(
        "مأموریت بساز", "مأموریت جدید", "یک مأموریت", "مأموریت ایجاد", "مأموریت بسازیم", "ماموریت بساز",
        "ماموریت جدید", "یک ماموریت", "ماموریت ایجاد", "ایجاد مأموریت", "ایجاد ماموریت", "ثبت مأموریت",
        "create mission", "create a mission", "new mission", "add mission", "add a mission", "make a mission",
        "start a mission", "remind me to"
    )

    private fun parseCreateMission(text: String): AssistantCommand.CreateMission? {
        if (!containsAny(text, CREATE_TRIGGERS)) return null
        var rest = extractAfterKeyword(text, CREATE_TRIGGERS)
        if (rest.isBlank()) return null

        // "… با نام X" / "… titled X"
        for (marker in listOf("با نام", "با عنوان", "به نام", "titled", "named", "called")) {
            val idx = rest.indexOf(marker)
            if (idx >= 0) rest = rest.substring(idx + marker.length).trim()
        }

        // task count: "با ۳ وظیفه" / "با سه وظیفه" / "with 3 tasks"
        var taskCount = 0
        val taskRegex = Regex("(?:با|and|with)\\s+(\\d{1,2})\\s+(?:وظیفه|وظیفه ها|زیرکار|کار|tasks?)")
        taskRegex.find(rest)?.let { m ->
            taskCount = m.groupValues[1].toIntOrNull() ?: 0
            rest = rest.replaceFirst(m.value, " ")
        }
        if (taskCount == 0) {
            WORD_NUMBERS.forEach { (word, n) ->
                val re = Regex("(?:با|and|with)\\s+$word\\s+(?:وظیفه|وظیفه ها|کار|tasks?)\\b")
                if (re.containsMatchIn(rest)) {
                    taskCount = n
                    rest = re.replace(rest, " ")
                    return@forEach
                }
            }
        }

        val urgent = containsAny(text, listOf("فوری", "مهم", "بحرانی", "اضطراری", "urgent", "critical", "asap", "important"))
        val title = stripTitle(rest)
        if (title.isBlank()) return null
        return AssistantCommand.CreateMission(
            title = title,
            taskCount = taskCount.coerceIn(0, 12),
            urgent = urgent
        )
    }

    private val WORD_NUMBERS = mapOf(
        "یک" to 1, "دو" to 2, "سه" to 3, "چهار" to 4, "پنج" to 5, "شش" to 6, "هفت" to 7, "هشت" to 8, "نه" to 9, "ده" to 10,
        "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5, "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10
    )

    private val TITLE_TAIL_FILLERS = listOf(
        "بسازم", "ایجاد کن", "ثبت کن", "درست کن", "بذار", "بزار", "ساخته شود", "بشینه", "میخوام", "بخوام",
        "in the app", "asap", "خیلی مهمه", "مهمه", "فوریه"
    )

    private fun stripTitle(rest: String): String {
        var t = stripLeading(rest, TITLE_TAIL_FILLERS)
        t = t.trim('،', ',', '؛', ';', ' ', '.', '؟', '?', '!')
        // drop a trailing "رو/را"
        t = t.replace(Regex("\\s+(رو|را)$"), "").trim()
        return t.replace(Regex("\\s+"), " ")
    }

    private fun extractAfterKeyword(text: String, keywords: List<String>): String {
        var bestStart = -1
        var bestLen = 0
        for (k in keywords) {
            val idx = text.indexOf(k)
            if (idx >= 0 && k.length > bestLen) {
                bestStart = idx
                bestLen = k.length
            }
        }
        if (bestStart < 0) return ""
        return stripLeading(text.substring(bestStart + bestLen), FILLERS)
    }

    // ----------------------------------------------------- navigation slots

    private fun parseNavigate(text: String): AssistantCommand.Navigate? {
        val wantsOpen = containsAny(text, listOf("باز کن", "بازکردن", "برو به", "برو تو", "نمایش بده", "open ", "go to ", "show me ", "switch to "))
        if (!wantsOpen) return null
        val targets = listOf(
            Pair(listOf("تنظیمات", "settings"), "SETTINGS"),
            Pair(listOf("مأموریت", "ماموریت", "کارها", "mission"), "MISSIONS"),
            Pair(listOf("چت", "گفتگو", "دستیار", "chat", "assistant"), "ASSISTANT"),
            Pair(listOf("ابزار", "tools"), "TOOLS"),
            Pair(listOf("خانه", "home"), "HOME"),
            Pair(listOf("درگاه", "gateway", "متاتریدر", "metatrader"), "GATEWAY"),
            Pair(listOf("اسکریپت", "خودکارسازی", "script", "automation"), "SCRIPTS"),
            Pair(listOf("امنیت", "دستگاه", "security"), "SECURITY"),
            Pair(listOf("پرونده شناختی", "uic"), "UIC"),
            Pair(listOf("اواتار", "آواتار", "شنیدار", "میکروفون", "avatar", "listening", "microphone"), "AVATAR"),
            Pair(listOf("شبیه سازی", "شبیهسازی", "simulation", "simulator"), "SIMULATION"),
            Pair(listOf("معاملات", "بازار", "ترید", "trading", "market"), "TRADING"),
            Pair(listOf("پیشنهاد", "ادراک", "aware"), "AWARE")
        )
        // The earliest (then longest) keyword wins, so "open the avatar settings"
        // lands on the avatar screen, not on settings.
        var bestIdx = Int.MAX_VALUE
        var bestLen = -1
        var bestName: String? = null
        for ((keys, name) in targets) {
            for (key in keys) {
                val idx = text.indexOf(key)
                if (idx >= 0 && (idx < bestIdx || (idx == bestIdx && key.length > bestLen))) {
                    bestIdx = idx
                    bestLen = key.length
                    bestName = name
                }
            }
        }
        return bestName?.let { AssistantCommand.Navigate(it) }
    }

    // ------------------------------------------------------------ calculator

    /**
     * Recognises "calculate 2+3*4", "حساب کن ۱۲ × ۵", "چند میشه 8/2 ?" and a bare
     * arithmetic message like "3.5*(2+1)". Returns null when the text is not maths.
     */
    fun parseCalculation(text: String): AssistantCommand.Calculate? {
        val triggers = listOf("حساب کن", "حساب کن", "چند میشه", "چند می شود", "جمع کن", "calculate", "compute", "what is", "how much is")
        var expr: String? = null
        for (t in triggers) {
            val idx = text.indexOf(t)
            if (idx >= 0) {
                expr = stripLeading(text.substring(idx + t.length), FILLERS)
                break
            }
        }
        if (expr == null && looksLikeBareMath(text)) expr = text
        val candidate = expr ?: return null
        val cleaned = candidate.replace(Regex("[؟?=]+$"), "").trim()
        if (!looksLikeMath(cleaned)) return null
        val value = SafeMath.evaluate(cleaned) ?: return null
        if (abs(value) > 1e15) return null
        return AssistantCommand.Calculate(expression = cleaned, value = value)
    }

    private fun looksLikeBareMath(text: String): Boolean =
        text.matches(Regex("[0-9+\\-*/^%().×÷ ]{3,40}")) && Regex("[+\\-*/^%×÷]").containsMatchIn(text)

    private fun looksLikeMath(text: String): Boolean =
        text.any { it.isDigit() } && text.all { it.isDigit() || it in "+-*/^%().×÷ " }

    /** Safe arithmetic evaluator (shunting-yard), no reflection, no eval. */
    object SafeMath {
        fun evaluate(expr: String): Double? {
            val tokens = tokenize(expr) ?: return null
            if (tokens.isEmpty()) return null
            val rpn = toRpn(tokens) ?: return null
            return evalRpn(rpn)
        }

        private fun tokenize(expr: String): List<String>? {
            val out = ArrayList<String>()
            var i = 0
            val s = expr.replace("×", "*").replace("÷", "/")
            while (i < s.length) {
                val c = s[i]
                when {
                    c == ' ' -> i++
                    c.isDigit() || c == '.' -> {
                        var j = i
                        while (j < s.length && (s[j].isDigit() || s[j] == '.')) j++
                        val num = s.substring(i, j)
                        if (num.count { it == '.' } > 1) return null
                        out.add(num)
                        i = j
                    }
                    c in "+-*/%^()" -> {
                        // unary minus: start, after operator or after '('
                        if (c == '-' && (out.isEmpty() || out.last() in listOf("+", "-", "*", "/", "%", "^", "("))) {
                            out.add("u-")
                        } else {
                            out.add(c.toString())
                        }
                        i++
                    }
                    else -> return null
                }
            }
            return out
        }

        private fun precedence(op: String): Int = when (op) {
            "u-" -> 4
            "^" -> 3
            "*", "/" -> 2
            "+", "-" -> 1
            else -> 0
        }

        private fun rightAssociative(op: String): Boolean = op == "^"

        private fun toRpn(tokens: List<String>): List<String>? {
            val output = ArrayList<String>()
            val ops = ArrayDeque<String>()
            for (t in tokens) {
                when {
                    t.toDoubleOrNull() != null -> output.add(t)
                    t == "(" -> ops.addLast(t)
                    t == ")" -> {
                        while (ops.isNotEmpty() && ops.last() != "(") output.add(ops.removeLast())
                        if (ops.lastOrNull() != "(") return null
                        ops.removeLast()
                    }
                    else -> {
                        while (ops.isNotEmpty() && ops.last() != "(" && ops.last() != ")" &&
                            (precedence(ops.last()) > precedence(t) ||
                                (precedence(ops.last()) == precedence(t) && !rightAssociative(t)))
                        ) {
                            output.add(ops.removeLast())
                        }
                        ops.addLast(t)
                    }
                }
            }
            while (ops.isNotEmpty()) {
                val op = ops.removeLast()
                if (op == "(" || op == ")") return null
                output.add(op)
            }
            return output
        }

        private fun evalRpn(rpn: List<String>): Double? {
            val stack = ArrayDeque<Double>()
            for (t in rpn) {
                val num = t.toDoubleOrNull()
                if (num != null) {
                    stack.addLast(num)
                    continue
                }
                if (t == "u-") {
                    if (stack.isEmpty()) return null
                    stack.addLast(-stack.removeLast())
                    continue
                }
                if (stack.size < 2) return null
                val b = stack.removeLast()
                val a = stack.removeLast()
                stack.addLast(
                    when (t) {
                        "+" -> a + b
                        "-" -> a - b
                        "*" -> a * b
                        "/" -> if (abs(b) < 1e-12) return null else a / b
                        "%" -> if (abs(b) < 1e-12) return null else a % b
                        "^" -> a.pow(b)
                        else -> return null
                    }
                )
            }
            return stack.lastOrNull()
        }
    }
}
