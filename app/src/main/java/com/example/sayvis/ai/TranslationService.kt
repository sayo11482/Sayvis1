package com.example.sayvis.ai

import android.content.Context
import com.example.sayvis.i18n.PersianFormat
import com.example.sayvis.settings.AiSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/** Where a rendered Persian string came from. Drives the "machine-translated" marker. */
enum class TranslationSource {
    /** Text was already Persian, or the UI language is English. */
    ORIGINAL,

    /** Matched the built-in offline dictionary — a hand-written, reviewed translation. */
    DICTIONARY,

    /** Returned from the on-device cache of an earlier AI translation. */
    CACHE,

    /** Produced live by the AI translator. */
    MACHINE,

    /** Translation was attempted and failed; the source text is shown instead. */
    FAILED
}

data class TranslationResult(val text: String, val source: TranslationSource) {
    val isMachine: Boolean get() = source == TranslationSource.MACHINE || source == TranslationSource.CACHE
}

/**
 * Hybrid Persian translation service.
 *
 * Three tiers, tried in order:
 *  1. **Built-in dictionary** — hand-written translations for every string the app
 *     itself generates (seed data, AWARE engine output, scenarios, signals, capability
 *     tags). Fully offline, deterministic, reviewed.
 *  2. **Numeric / quoted templates** — the same strings with interpolated values, so
 *     "Battery level at 17%" still resolves without a network call.
 *  3. **Online AI translation** — for genuinely free-form text (a model's answer, a
 *     broker note, something the owner typed). Results are cached on device so the same
 *     sentence is never paid for twice, and translation stops entirely when offline
 *     mode is on.
 *
 * This is what makes the Persian UI complete instead of partial: nothing falls through
 * to raw English unless tier 3 is unavailable, and even then the caller is told which
 * tier produced the text.
 */
class TranslationService(context: Context? = null) {

    private val prefs = context?.applicationContext
        ?.getSharedPreferences("sayvis_translations", Context.MODE_PRIVATE)

    private val memoryCache = ConcurrentHashMap<String, String>()
    private val inFlight = ConcurrentHashMap<String, Boolean>()
    private val mutex = Mutex()

    init {
        prefs?.all?.forEach { (key, value) ->
            if (value is String) memoryCache[key] = value
        }
    }

    /** True when [text] contains enough Latin letters to be worth translating. */
    fun needsTranslation(text: String): Boolean {
        if (text.isBlank()) return false
        val trimmed = text.trim()
        if (isAlreadyPersian(trimmed)) return false
        // Machine identifiers and tickers are not prose: never spend a model call on them.
        if (TECHNICAL_TOKEN.matches(trimmed)) return false
        // Short all-caps tokens are tickers or enum codes (EURUSD, XAU, BOS).
        if (trimmed.length <= 12 && trimmed.all { it.isUpperCase() || it.isDigit() }) return false
        var latin = 0
        var total = 0
        for (ch in trimmed) {
            if (ch.isLetterOrDigit()) {
                total++
                if (ch.code in 0x0041..0x024F) latin++ // Basic Latin + Latin Extended
            }
        }
        if (total == 0) return false
        return latin.toDouble() / total.toDouble() > 0.35
    }

    /** True when the text is already (mostly) Persian/Arabic script. */
    fun isAlreadyPersian(text: String): Boolean {
        if (text.isBlank()) return false
        var rtl = 0
        var total = 0
        for (ch in text) {
            if (ch.isLetter()) {
                total++
                if (ch.code in 0x0600..0x06FF || ch.code in 0xFB50..0xFDFF || ch.code in 0xFE70..0xFEFF) rtl++
            }
        }
        return total > 0 && rtl.toDouble() / total.toDouble() > 0.5
    }

    // ------------------------------------------------------------- tier 1 & 2

    /**
     * Offline resolution. Never touches the network.
     *
     * @param persianDigits when true, digits inside a dictionary/template translation are
     *        rendered as Persian digits so a translated sentence reads consistently.
     */
    fun resolveOffline(text: String, persianDigits: Boolean = false): TranslationResult {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || isAlreadyPersian(trimmed)) {
            return TranslationResult(text, TranslationSource.ORIGINAL)
        }

        // 1) Hand-written exact match always wins, even for technical-looking strings.
        EXACT[trimmed]?.let { return TranslationResult(finalise(it, persianDigits), TranslationSource.DICTIONARY) }
        MEMORY_CACHE_SEED[trimmed]?.let { return TranslationResult(finalise(it, persianDigits), TranslationSource.CACHE) }

        // 2) Machine identifiers, tickers and key=value payloads are left untouched rather
        //    than being flagged as untranslated — they are not prose.
        if (TECHNICAL_TOKEN.matches(trimmed)) {
            return TranslationResult(text, TranslationSource.ORIGINAL)
        }

        // 3) Templates for strings carrying interpolated numbers or nested quoted text.
        for ((pattern, template) in PATTERNS) {
            val match = pattern.matchEntire(trimmed) ?: continue
            var rendered = template
            for (index in 1..match.groupValues.size - 1) {
                val captured = match.groupValues[index]
                // Recursively translate captured English fragments (e.g. a task title
                // embedded inside a sentence) so nested text is Persian too.
                val inner = EXACT[captured.trim()] ?: captured
                rendered = rendered.replace("{$index}", inner)
            }
            return TranslationResult(finalise(rendered, persianDigits), TranslationSource.DICTIONARY)
        }
        return TranslationResult(text, TranslationSource.FAILED)
    }

    /**
     * Applies the owner's digit preference to a resolved translation. Uses the
     * prose-safe conversion so sentence punctuation is left intact.
     */
    private fun finalise(value: String, persianDigits: Boolean): String =
        if (persianDigits) PersianFormat.toPersianNumerals(value) else value

    fun cached(text: String): String? = memoryCache[text.trim()]

    // ---------------------------------------------------------------- tier 3

    /**
     * Translates [text] with the configured AI provider and caches the result.
     * Returns null when translation is unavailable (no provider, offline, or failure)
     * so the caller can keep showing the offline resolution instead of blank text.
     */
    suspend fun translateOnline(
        text: String,
        orchestrator: AIOrchestrator,
        settings: AiSettings,
        forceOffline: Boolean
    ): String? {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || !needsTranslation(trimmed)) return null
        if (forceOffline || !settings.isProviderConfigured()) return null
        memoryCache[trimmed]?.let { return it }

        // Never fire two identical requests at once.
        if (inFlight.putIfAbsent(trimmed, true) != null) return null
        try {
            return mutex.withLock {
                val response = orchestrator.querySAYVIS(
                    prompt = trimmed,
                    uicContext = "",
                    systemContext = "",
                    languageFa = true,
                    emergencyLockActive = false,
                    forceOffline = false,
                    settings = settings,
                    taskInstruction = TRANSLATION_INSTRUCTION
                )
                val candidate = response.text
                    .trim()
                    .removeSurrounding("\"")
                    .removeSurrounding("«", "»")
                    .trim()
                if (response.isSuccess && candidate.isNotEmpty() && isAlreadyPersian(candidate)) {
                    cacheResult(trimmed, candidate)
                    candidate
                } else {
                    null
                }
            }
        } finally {
            inFlight.remove(trimmed)
        }
    }

    /** Batch form: one request for many sentences, used when a whole screen loads. */
    suspend fun translateBatch(
        texts: List<String>,
        orchestrator: AIOrchestrator,
        settings: AiSettings,
        forceOffline: Boolean
    ): Map<String, String> = withContext(Dispatchers.IO) {
        val pending = texts.map { it.trim() }
            .filter { it.isNotEmpty() && needsTranslation(it) && memoryCache[it] == null }
            .distinct()
        if (pending.isEmpty() || forceOffline || !settings.isProviderConfigured()) {
            return@withContext emptyMap()
        }
        val numbered = pending.mapIndexed { index, value -> "${index + 1}. $value" }.joinToString("\n")
        val response = orchestrator.querySAYVIS(
            prompt = numbered,
            languageFa = true,
            emergencyLockActive = false,
            forceOffline = false,
            settings = settings,
            taskInstruction = BATCH_TRANSLATION_INSTRUCTION
        )
        if (!response.isSuccess) return@withContext emptyMap()

        val out = mutableMapOf<String, String>()
        response.text.lineSequence().forEach { line ->
            val match = Regex("^\\s*(\\d+)[.\\-)\\s]\\s*(.+)$").matchEntire(line) ?: return@forEach
            val index = match.groupValues[1].toIntOrNull() ?: return@forEach
            val value = match.groupValues[2].trim()
            val source = pending.getOrNull(index - 1) ?: return@forEach
            if (value.isNotEmpty() && isAlreadyPersian(value)) {
                cacheResult(source, value)
                out[source] = value
            }
        }
        out
    }

    private fun cacheResult(source: String, translation: String) {
        memoryCache[source] = translation
        prefs?.edit()?.putString(source, translation)?.apply()
    }

    fun clearCache() {
        memoryCache.clear()
        prefs?.edit()?.clear()?.apply()
    }

    fun cacheSize(): Int = memoryCache.size

    companion object {
        /**
         * Matches machine identifiers rather than prose: snake_case names, dotted action
         * codes, tickers, fingerprints and model ids. These are shown as-is instead of
         * being sent to a translation model or flagged as untranslated.
         */
        private val TECHNICAL_TOKEN = Regex("^[A-Za-z0-9]+(?:[_.=/:-][A-Za-z0-9]+)+\$")

        private const val TRANSLATION_INSTRUCTION =
            "You are a professional English→Persian translator. Translate the owner's message into fluent, " +
                "natural, technically accurate Persian (فارسی). Keep code identifiers, symbol tickers, API names " +
                "and units unchanged. Output ONLY the Persian translation — no preamble, no quotes, no explanation."

        private const val BATCH_TRANSLATION_INSTRUCTION =
            "You are a professional English→Persian translator. The owner sends a numbered list of English lines. " +
                "Translate every line into fluent, technically accurate Persian and return the SAME numbering, one " +
                "line per item, in the form \"1. ترجمه\". Do not merge, drop or reorder lines. Output nothing else."

        /** Exact, hand-written translations for every string SAYVIS generates itself. */
        private val EXACT: Map<String, String> = buildMap {
            // ---- UIC seed: titles ----
            put("Primary Language & Tone", "زبان اصلی و لحن گفتار")
            put("Optimal Deep Work Window", "بهترین پنجرهٔ کار عمیق")
            put("Global Launch of SAYVIS Core", "راه‌اندازی جهانی هستهٔ سایویس")
            put("Nightly Mission Reconciliation", "جمع‌بندی شبانهٔ مأموریت‌ها")
            put("Strict Zero Trust Policy", "سیاست سخت‌گیرانهٔ اعتماد صفر")
            put("Lifelong Cognitive Augmentation", "تقویت شناختی مادام‌العمر")

            // ---- UIC seed: values ----
            put(
                "Bilingual Persian/English - Formal, Precise, High-Composure",
                "دوزبانه فارسی/انگلیسی — رسمی، دقیق و با متانت"
            )
            put(
                "09:00 - 11:30 AM (Peak focus, zero-interruption preference)",
                "۰۹:۰۰ تا ۱۱:۳۰ (اوج تمرکز، بدون پذیرش وقفه)"
            )
            put(
                "Deploy multi-device gateway, native Android node, and secure agent pipeline",
                "استقرار دروازهٔ چنددستگاهی، گرهٔ بومی اندروید و خط لولهٔ امن عامل هوشمند"
            )
            put(
                "Reviews pending tasks and approves next-day agenda between 21:30 and 22:15",
                "بررسی وظایف در انتظار و تأیید برنامهٔ روز بعد بین ساعت ۲۱:۳۰ تا ۲۲:۱۵"
            )
            put(
                "Never allow automated file deletion or financial actions without biometric/passkey approval",
                "حذف خودکار فایل یا اقدام مالی بدون تأیید بیومتریک یا کلید عبور هرگز مجاز نیست"
            )
            put(
                "Empower personal agency, protect sovereign data ownership, and eliminate cognitive drift",
                "توانمندسازی عاملیت شخصی، پاسداری از مالکیت حاکم بر داده و از بین بردن پراکندگی شناختی"
            )

            // ---- UIC seed: provenance ----
            put("Explicit Owner Onboarding Config", "تنظیم صریح مالک در مرحلهٔ راه‌اندازی")
            put("AWARE telemetry analysis over 14 days", "تحلیل داده‌های سنجشی AWARE طی ۱۴ روز")
            put("Owner Strategic Mission Entry", "ثبت راهبردی توسط مالک")
            put("Interaction frequency clustering", "خوشه‌بندی بسامد تعامل‌ها")
            put("Security Governance Rule", "قاعدهٔ حاکمیت امنیتی")
            put("Product Vision Master Directive", "فرمان اصلی چشم‌انداز محصول")
            put("Explicit Owner Entry", "ورود صریح توسط مالک")
            put("USER_EXPLICIT", "ورود صریح کاربر")
            put("SYSTEM_OBSERVED", "مشاهدهٔ سامانه")
            put("AI_INFERRED", "استنتاج هوش مصنوعی")
            put("IMPORTED", "واردشده")
            put("DEVICE_SIGNAL", "سیگنال دستگاه")

            // ---- AWARE seed opportunities ----
            put("Protect Peak Cognitive Window", "حفاظت از پنجرهٔ اوج شناختی")
            put(
                "Detected scheduled meeting conflicting with your 09:30 AM peak deep-work baseline.",
                "جلسهٔ زمان‌بندی‌شده‌ای پیدا شد که با زمان اوج کار عمیق شما در ساعت ۰۹:۳۰ تداخل دارد."
            )
            put(
                "Working Pattern correlation: 88% confidence peak window vs incoming calendar slot.",
                "همبستگی الگوی کاری: ۸۸٪ اطمینان در تداخل پنجرهٔ اوج با زمان ثبت‌شده در تقویم."
            )
            put(
                "Block notifications & reschedule advisory check-in to 14:00",
                "مسدودسازی اعلان‌ها و انتقال جلسهٔ مشورتی به ساعت ۱۴:۰۰"
            )
            put(
                "Deflect non-urgent alerts & request slot postponement via calendar gateway.",
                "انحراف هشدارهای غیرفوری و درخواست تعویق زمان از طریق دروازهٔ تقویم."
            )
            put("Mission Critical Blocker Mitigation", "رفع مانع بحرانی مأموریت")
            put(
                "Task 'Crypto Key Exchange Protocol' in Mission 'SAYVIS Core' has been blocked for 48h.",
                "وظیفهٔ «پروتکل تبادل کلید رمزنگاری» در مأموریت «هستهٔ سایویس» به مدت ۴۸ ساعت مسدود بوده است."
            )
            put(
                "Mission Engine telemetry: Dependency timeout on external cryptographic spec.",
                "سنجش موتور مأموریت: اتمام مهلت وابستگی به مشخصات رمزنگاری بیرونی."
            )
            put(
                "Inject Fallback Ed25519 Spec & reassign priority to high",
                "جایگزینی مشخصات پشتیبان Ed25519 و ارتقای اولویت به «بالا»"
            )
            put(
                "Unblock task #2 and update roadmap milestone dependencies.",
                "رفع انسداد وظیفهٔ شمارهٔ ۲ و به‌روزرسانی وابستگی‌های نقاط عطف نقشهٔ راه."
            )
            put("Unattended Remote Device Session", "نشست بی‌سرپرست دستگاه راه دور")
            put(
                "Windows Workstation (Node-Win01) idle for 18h with active privileged token.",
                "ایستگاه کاری ویندوز (گرهٔ Win01) به مدت ۱۸ ساعت با توکن ویژهٔ فعال، بی‌استفاده مانده است."
            )
            put("Gateway Session Monitor: Inactivity threshold exceeded.", "پایشگر نشست دروازه: آستانهٔ بی‌فعالیتی رد شد.")
            put("Demote session token to read-only safe mode", "تنزل توکن نشست به حالت امنِ فقطخواندنی")
            put(
                "Rotate session key and enforce re-authentication on next interactive packet.",
                "چرخش کلید نشست و الزام احراز هویت دوباره در نخستین بستهٔ تعاملی بعدی."
            )

            // ---- AWARE engine generated ----
            put("Conserve Energy & Save Local Checkpoint", "کاهش مصرف انرژی و ذخیرهٔ نقطهٔ بازگشت محلی")
            put(
                "AWARE Hardware Telemetry: Critical power drop detected while discharging.",
                "سنجش سخت‌افزار AWARE: افت بحرانی توان در حالت تخلیهٔ باتری تشخیص داده شد."
            )
            put(
                "Throttle background polling and flush uncommitted mission states to encrypted storage.",
                "کاهش نرخ پایش پس‌زمینه و انتقال وضعیت‌های ثبت‌نشدهٔ مأموریت به حافظهٔ رمزنگاری‌شده."
            )
            put("Mitigate Thermal Throttling", "کاهش محدودسازی حرارتی")
            put(
                "Sovereign hardware node reports high thermal load. Suggest deferring non-essential background embeddings.",
                "گرهٔ سخت‌افزاری بار حرارتی بالا گزارش می‌کند. پیشنهاد می‌شود پردازش‌های غیرضروری پس‌زمینه به تعویق بیفتند."
            )
            put("AWARE Sensor Telemetry: Thermal load threshold exceeded.", "سنجش حسگر AWARE: آستانهٔ بار حرارتی رد شد.")
            put(
                "Pause background AI fine-tuning and prioritize active interactive tasks.",
                "توقف آموزش پس‌زمینهٔ هوش مصنوعی و اولویت‌دهی به وظایف تعاملی فعال."
            )
            put("Sustained Cognitive Intensity", "شدت شناختی پایدار")
            put("Workstream Blocker Cluster", "تجمع مانع در جریان کاری")
            put("Circadian Rhythm Deviation", "انحراف از ریتم شبانه‌روزی")
            put("Activity detected during nocturnal wind-down phase.", "فعالیت در بازهٔ آرام‌سازی شبانه تشخیص داده شد.")
            put("Recurring Dependency Stalls", "توقف‌های مکرر ناشی از وابستگی")
            put("Cognitive Focus Cycle Adherence", "پایبندی به چرخهٔ تمرکز شناختی")
            put("Schedule Deep Work Shield", "زمان‌بندی سپر کار عمیق")
            put(
                "AWARE Cognitive Monitor: Focus window active with optimal cognitive load.",
                "پایشگر شناختی AWARE: پنجرهٔ تمرکز با بار شناختی بهینه فعال است."
            )
            put(
                "Suppress external notifications and shield active cognitive bandwidth.",
                "سرکوب اعلان‌های بیرونی و حفاظت از ظرفیت شناختی فعال."
            )
            put(
                "AWARE Real-time Monitor: Mission velocity dropped by 45% due to unresolved blocker.",
                "پایشگر بلادرنگ AWARE: سرعت پیشرفت مأموریت به دلیل مانع حل‌نشده ۴۵٪ کاهش یافت."
            )
            put(
                "Re-route dependency through offline cryptographic library",
                "مسیردهی دوبارهٔ وابستگی از طریق کتابخانهٔ رمزنگاری آفلاین"
            )
            put(
                "Re-order task queue and alert owner of verified fallback strategy.",
                "بازچینی صف وظایف و آگاه‌سازی مالک از راهبرد پشتیبانِ اعتبارسنجی‌شده."
            )
            put("External dependency", "وابستگی بیرونی")

            // ---- Missions seed ----
            put("Deploy SAYVIS Multi-Device Gateway", "استقرار دروازهٔ چنددستگاهی سایویس")
            put(
                "Architect and verify Zero-Trust Gateway connecting Android, Windows, and Web nodes.",
                "طراحی و اعتبارسنجی دروازهٔ «اعتماد صفر» برای اتصال گره‌های اندروید، ویندوز و وب."
            )
            put("Tomorrow, 18:00 UTC", "فردا، ساعت ۱۸:۰۰ UTC")
            put("Implement Zero-Trust Action Model", "پیاده‌سازی مدل اقدام «اعتماد صفر»")
            put("Device Cryptographic Identity & Pairing", "هویت رمزنگاری دستگاه و جفت‌سازی")
            put("Integrate AWARE Opportunity Engine", "یکپارچه‌سازی موتور فرصت‌های AWARE")
            put("Enforce Strict Emergency Lockout Mechanism", "اعمال سازوکار سخت‌گیرانهٔ قفل اضطراری")
            put("LIT Trading Intelligence - Risk Engine Gate", "هوش معاملاتی LIT — دروازهٔ موتور ریسک")
            put(
                "Calibrate liquidity inversion analysis and enforce paper-trading kill switch safeguards.",
                "کالیبراسیون تحلیل اینورژن نقدینگی و اعمال حفاظ‌های کلید قطع در معاملات کاغذی."
            )
            put("Sept 18, 2026", "۱۸ سپتامبر ۲۰۲۶")
            put("Order Block & BOS Detection Module", "ماژول تشخیص اردربلاک و شکست ساختار (BOS)")
            put("Drawdown Hard Cap Guardrails", "حفاظ‌های سقف سختِ افت سرمایه")
            put("Broker Adapter Sandbox Audit", "ممیزی محیط آزمایشی رابط بروکر")
            put("Waiting for audit signoff", "در انتظار تأیید ممیزی")

            // ---- Devices ----
            put("SAYVIS Mobile Node (Galaxy S24 / Pixel)", "گرهٔ موبایل سایویس (Galaxy S24 / Pixel)")
            put("SAYVIS Master Rig (Windows 11 Agent)", "رایانهٔ اصلی سایویس (عامل ویندوز ۱۱)")
            put("SAYVIS Cloud Web Console", "کنسول وب ابری سایویس")

            // ---- Device capabilities ----
            put("biometric_auth", "احراز هویت بیومتریک")
            put("voice_vad", "تشخیص فعالیت صوتی")
            put("local_storage", "حافظهٔ محلی")
            put("camera", "دوربین")
            put("push_notifications", "اعلان‌های پوش")
            put("hardware_gpu", "پردازندهٔ گرافیکی")
            put("filesystem", "دسترسی به فایل‌ها")
            put("controlled_powershell", "پاورشل کنترل‌شده")
            put("local_llm", "مدل زبانی محلی")
            put("dashboard_management", "مدیریت داشبورد")
            put("audit_viewer", "نمایشگر گزارش ممیزی")
            put("remote_emergency_lock", "قفل اضطراری از راه دور")

            // ---- Life simulation scenarios ----
            put(
                "Transition to full-time autonomous AI operating system engineering",
                "گذار به مهندسی تمام‌وقتِ سیستم‌عامل هوش مصنوعی خودگردان"
            )
            put(
                "+45% sovereign asset compounding, 2.8x strategic impact, 6-month initial cash buffer required",
                "۴۵٪+ رشد ترکیبی دارایی حاکم، ۲٫۸ برابر اثر راهبردی، نیاز به ذخیرهٔ نقدی شش‌ماهه در ابتدا"
            )
            put("Early runway volatility", "نوسان منابع مالی در ماه‌های نخست")
            put("Multi-platform distribution complexity", "پیچیدگی توزیع در چند سکو")
            put("First-mover advantage in personal AI layers", "مزیت پیشگامی در لایه‌های هوش مصنوعی شخصی")
            put("Complete cognitive sovereignty", "حاکمیت کامل شناختی")
            put(
                "Strict adherence to 09:00 - 11:30 deep work window with 22:00 screen cut-off",
                "پایبندی سخت‌گیرانه به پنجرهٔ کار عمیق ۰۹:۰۰ تا ۱۱:۳۰ همراه با قطع صفحهٔ نمایش در ساعت ۲۲:۰۰"
            )
            put(
                "35% reduction in cognitive fatigue, 18% higher mission completion velocity",
                "۳۵٪ کاهش خستگی شناختی و ۱۸٪ سرعت بیشتر در تکمیل مأموریت‌ها"
            )
            put("Potential pushback from synchronous communicators", "احتمال مقاومت اطرافیانِ ارتباط هم‌زمان")
            put("Sustained high cognitive bandwidth", "ظرفیت شناختی بالای پایدار")
            put("Elimination of evening burnout", "از بین رفتن فرسودگی شامگاهی")

            // ---- LIT trading signals ----
            put(
                "Institutional liquidity grab at low range. Paper-trading simulation only.",
                "جمع‌آوری نقدینگی نهادی در کف محدودهٔ قیمتی. فقط شبیه‌سازی معاملات کاغذی."
            )
            put(
                "Macro inflation hedge inversion with bullish order flow.",
                "اینورژن پوشش ریسک تورم کلان همراه با جریان سفارش صعودی."
            )
            put("XAU/USD (Gold)", "XAU/USD (طلا)")

            // ---- Legacy context prose (kept for stored rows written by older builds) ----
            put("Deep Work Window", "پنجرهٔ کار عمیق")
            put("System Booting", "راه‌اندازی سامانه")
            put("Optimal", "بهینه")
            put("Optimal Flow", "جریان بهینه")
            put("Fatigue Risk", "خطر خستگی")
            put("Fatigue / Blocker Alert", "هشدار خستگی یا مانع")
            put("Online", "متصل")
            put("Offline", "آفلاین")
            put("Deep Work", "کار عمیق")

            // ---- Common audit results & actors ----
            put("SUCCESS", "موفق")
            put("BLOCKED", "مسدود")
            put("REJECTED", "ردشده")
            put("FAILED", "ناموفق")
            put("OWNER", "مالک")
            put("SAYVIS_AGENT", "عامل سایویس")
            put("AWARE_ENGINE", "موتور AWARE")
            put("SCRIPT_ENGINE", "موتور اسکریپت")
            put("SAYVIS_SECURITY_CORE", "هستهٔ امنیت سایویس")
            put("OWNER_CONFIRMED", "تأیید مالک")
            put("SESSION_VALIDATED", "اعتبارسنجی نشست")
            put("BLOCKED_EMERGENCY_LOCK", "مسدود با قفل اضطراری")
            put("OWNER_BIOMETRIC_CONFIRMED", "تأیید بیومتریک مالک")
            put("POLICY_PERMITTED", "مجاز بر اساس سیاست")
            put("POLICY", "سیاست")

            // ---- Audit action codes ----
            put("system.boot.verify_integrity", "راه‌اندازی سامانه — اعتبارسنجی یکپارچگی")
            put("uic.attribute.update_status", "پروندهٔ شناختی — تغییر وضعیت ویژگی")
            put("uic.attribute.create", "پروندهٔ شناختی — افزودن ویژگی")
            put("uic.attribute.delete", "پروندهٔ شناختی — حذف ویژگی")
            put("opportunity.execute.attempt", "تلاش برای اجرای پیشنهاد")
            put("opportunity.execute", "اجرای پیشنهاد")
            put("opportunity.dismiss", "رد کردن پیشنهاد")
            put("mission.task.toggle", "مأموریت — تغییر وضعیت وظیفه")
            put("mission.create", "مأموریت — ایجاد مأموریت تازه")
            put("device.trust.toggle", "دستگاه — تغییر سطح اعتماد")
            put("device.revoke", "دستگاه — لغو دسترسی")
            put("ai.query.respond", "هوش مصنوعی — پاسخ به پرسش")
            put("ai.provider.probe", "هوش مصنوعی — آزمون اتصال سرویس")
            put("security.emergency_lock.engage", "امنیت — فعال‌سازی قفل اضطراری")
            put("security.emergency_lock.disengage", "امنیت — غیرفعال‌سازی قفل اضطراری")
            put("settings.reset_all", "تنظیمات — بازنشانی کامل")
            put("trading.gateway.connect", "معاملات — اتصال درگاه ترمینال")
            put("trading.gateway.disconnect", "معاملات — قطع درگاه ترمینال")
            put("trading.execution_mode.change", "معاملات — تغییر سطح اجازهٔ اجرا")
            put("trading.order.submit", "معاملات — ارسال سفارش")
            put("trading.order.blocked", "معاملات — مسدودسازی سفارش")
            put("trading.position.close", "معاملات — بستن پوزیشن")
            put("automation.script.run", "خودکارسازی — اجرای اسکریپت")
            put("automation.proposal.create", "خودکارسازی — ایجاد پیشنهاد")
            put("automation.block", "خودکارسازی — مسدودسازی")
            put("automation.webhook.send", "خودکارسازی — ارسال وب‌هوک")
            put("automation.webhook.blocked", "خودکارسازی — وب‌هوک مسدود شد")
            put("automation.execution_mode.request", "خودکارسازی — درخواست تغییر سطح اجرا")

            // ---- Opportunity / mission misc ----
            put("Blocked dependency", "وابستگی مسدودکننده")

            // ---- Machine payloads: given a readable Persian gloss so the proposal card
            //      never shows a bare internal identifier to the owner. ----
            put("power_save_mode_enable", "فعال‌سازی حالت ذخیرهٔ نیرو")
            put("defer_background_inference", "به تعویق انداختن پردازش پس‌زمینه")
            put("focus_duration=90", "مدت تمرکز = ۹۰ دقیقه")
        }

        /**
         * Templates for strings carrying interpolated numbers or nested quoted text.
         * Each Persian template uses {1}, {2}, … in capture-group order.
         */
        private val PATTERNS: List<Pair<Regex, String>> = listOf(
            Regex("^Battery level at (\\d+)%\\. Recommend optimizing background services and securing local database checkpoint\\.$") to
                "سطح باتری {1}٪ است. توصیه می‌شود سرویس‌های پس‌زمینه بهینه‌سازی و نقطهٔ بازگشت پایگاه دادهٔ محلی ایمن‌سازی شود.",

            Regex("^Detected (\\d+) consecutive high-effort sessions\\. Rest interval recommended\\.$") to
                "{1} جلسهٔ پرفشار متوالی تشخیص داده شد. یک بازهٔ استراحت توصیه می‌شود.",

            Regex("^(\\d+) blocker encounters recorded\\. Re-planning mission trajectory recommended\\.$") to
                "{1} مورد مواجهه با مانع ثبت شد. بازطراحی مسیر مأموریت توصیه می‌شود.",

            Regex("^Detected (\\d+) mission\\(s\\) stalled on external dependencies or cryptographic specs\\.$") to
                "{1} مأموریت به دلیل وابستگی بیرونی یا مشخصات رمزنگاری متوقف شده است.",

            Regex("^User demonstrates ([\\d.]+)x higher strategic output during mornings \\(([\\d:]+) - ([\\d:]+)\\) when notifications are silenced\\.$") to
                "بازدهی راهبردی کاربر در صبح‌ها ({2} تا {3}) و در حالت بی‌صدای اعلان‌ها، {1} برابر بیشتر است.",

            Regex("^Resolve Blocker in '(.+)'$") to "رفع مانع در «{1}»",

            Regex("^Task '(.+)' is blocked: (.+)\\.$") to "وظیفهٔ «{1}» مسدود است: {2}",

            Regex("^Currently inside the high-yield focus window \\((.+)\\)\\. Silencing non-critical companion alerts\\.$") to
                "اکنون داخل پنجرهٔ تمرکز پربازده ({1}) هستید. اعلان‌های غیربحرانی همراهان بی‌صدا می‌شوند.",

            Regex("^Detected (\\d+) device\\(s\\) with expired trust certificates\\.$") to
                "{1} دستگاه با گواهی اعتماد منقضی‌شده شناسایی شد.",

            Regex("^(\\d+)% complete$") to "{1}٪ تکمیل‌شده"
        )

        /** Translations captured earlier in this process, seeded for instant display. */
        private val MEMORY_CACHE_SEED: Map<String, String> = emptyMap()
    }
}
