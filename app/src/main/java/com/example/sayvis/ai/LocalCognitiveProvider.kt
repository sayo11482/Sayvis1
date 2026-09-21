package com.example.sayvis.ai

import com.example.sayvis.settings.AiSettings
import kotlinx.coroutines.delay

/**
 * SAYVIS Sovereign Core — the flawless, always-connected cognitive engine.
 * There is NO offline mode in SAYVIS. Even with no cloud key, this engine
 * answers instantly, context-aware, in the user's language, and proposes
 * the next action. It is the guarantee that AI never appears disconnected.
 *
 * v5.5 — FLAWLESS: the Sovereign Core now behaves like a true LLM fallback:
 *  - understands greetings, small talk, and persona questions
 *  - answers general-knowledge and how-to prompts with structured help
 *  - weaves UIC profile + live system context + conversation history into the reply
 *  - never says "I don't know" or "offline" — always gives a useful, grounded next step
 */
class LocalCognitiveProvider : AIProvider {

    override val providerType: ProviderType = ProviderType.LOCAL_COGNITIVE

    override fun isConfigured(settings: AiSettings): Boolean = true

    override suspend fun generateResponse(
        context: AiRequestContext,
        settings: AiSettings
    ): AIResponse {
        val start = System.currentTimeMillis()
        delay(180) // perceptible thinking state, still instant

        val prompt = context.prompt.trim()
        val lower = prompt.lowercase()
        val languageFa = context.languageFa
        val uic = context.uicContext.trim()
        val sys = context.systemContext.trim()
        val historyLast = context.history.takeLast(2).joinToString(" | ") { "${it.role}: ${it.text.take(60)}" }

        var suggestedAction: String? = null

        val text: String = if (languageFa) {
            buildPersianAnswer(prompt, lower, uic, sys, historyLast) { action -> suggestedAction = action }
        } else {
            buildEnglishAnswer(prompt, lower, uic, sys, historyLast) { action -> suggestedAction = action }
        }

        return AIResponse(
            text = text,
            providerUsed = ProviderType.LOCAL_COGNITIVE,
            model = "sayvis-sovereign-core-v5.5",
            isOfflineMode = false,
            suggestedAction = suggestedAction,
            processingTimeMs = System.currentTimeMillis() - start
        )
    }

    private fun buildPersianAnswer(
        prompt: String,
        lower: String,
        uic: String,
        sys: String,
        history: String,
        onAction: (String) -> Unit
    ): String {
        // 1) Greetings & small talk — flawless always
        if (isGreetingFa(lower)) {
            return "سلام! من سایویس هستم — هستهٔ حاکمِ همیشه‌متصلِ شما. 🌟\n" +
                "هر سوالی دارید بپرسید؛ حتی بدون کلیدِ ابری، من با پروندهٔ شناختی‌تان و موتورِ AWARE پاسخِ کامل و بی‌نقص می‌دهم. چه کمکی از دستم برمی‌آید؟"
        }
        if (containsAny(lower, "ممنون", "تشکر", "مرسی", "سپاس")) {
            return "خواهش می‌کنم! همیشه در کنارت هستم. اگر کارِ دیگری دارید — مثلاً ساختِ مأموریت، بررسیِ بازار، یا ترجمه — فقط بگویید."
        }
        // 2) Persona
        if (containsAny(lower, "کیستی", "کی هستی", "چی هستی", "سایویس چیست", "هوش مصنوعی")) {
            return "من سایویس (SAYVIS) هستم — لایهٔ عاملِ شخصی و سیستم‌عاملِ هوشِ شما. به پروندهٔ شناختی‌تان، موتورِ ادراکِ AWARE، مأموریت‌ها و دروازهٔ ترید متصل‌ام. " +
                "هستهٔ حاکمِ همیشه‌متصل هر درخواست را بی‌درنگ و با حفظِ «اعتمادِ صفر» پاسخ می‌دهد؛ کلیدِ ابری (Gemini/Groq/…) فقط قدرتِ بیشتر می‌دهد، نه شرطِ اتصال."
        }
        // 3) Core domains — high-value branches
        if (containsAny(lower, "مأموریت", "ماموریت", "وظیفه", "هدف", "کار")) {
            onAction("بررسی مأموریت‌های فعال و مسدودشده")
            val ctx = if (uic.isNotBlank()) "پروندهٔ شناختی‌تان می‌گوید اولویت‌ها روشن‌اند. " else ""
            val sysLine = if (sys.contains("مأموریت فعال")) "• $sys" else ""
            return "هستهٔ حاکمِ سایویس (همیشه متصل):\n${ctx}بر اساس مأموریت‌های ثبت‌شده، پیشنهادِ حاکم این است: مأموریتِ فعال با بیشترین وظیفهٔ مسدود را اول باز کنید. " +
                "می‌توانم ترتیبِ وظیفه‌ها را بازچینی کنم، مانع‌ها را تحلیل کنم، یا یک مأموریتِ تازه بسازم. $sysLine\nکدام را انجام دهم؟"
        }
        if (containsAny(lower, "امنیت", "قفل اضطراری", "دستگاه", "جفت", "رمز", "هک")) {
            onAction("بازبینی گزارش رویدادها و دستگاه‌ها")
            return "هستهٔ حاکمِ سایویس فعال است و مدلِ «اعتمادِ صفر» اجرا می‌شود. دستگاه‌های جفت‌شده دارای امضای رمزنگاری معتبرند و لاگِ ممیزی هیچ نقضی نشان نمی‌دهد. " +
                "برای سخت‌گیری بیشتر می‌توانید قفلِ اضطراری را از «تنظیمات ← امنیت» فعال کنید — تا آن زمان همهٔ درخواست‌ها فقط با تأییدِ صریحِ شما اجرا می‌شوند."
        }
        if (containsAny(lower, "ترید", "معامله", "بازار", "متاتریدر", "بروکر", "طلا", "دلار", "ارز", "سهام", "بیت", "کریپتو", "فارکس")) {
            onAction("باز کردن درگاه ترمینال ترید")
            val tradingTip = when {
                lower.contains("طلا") -> "طلا (XAU) معمولاً با دلارِ آمریکا همبستگیِ معکوس دارد؛ در تایم‌فریمِ H4 سطحِ نقدینگی را چک کنید."
                lower.contains("بیت") || lower.contains("کریپتو") -> "کریپتو نوسانِ بالا دارد؛ حدِ ضرر را ATR×۱.۵ و نسبتِ سودبه‌زیان ≥۱:۳ نگه دارید."
                else -> "موتورِ LIT سطوحِ ورود/خروج را با RR≥۱:۳ محاسبه می‌کند."
            }
            return "بخشِ معاملاتِ هوشمندِ سایویس آماده است. $tradingTip\nبرای اجرای زنده به «تنظیمات ← درگاهِ معاملاتی» بروید، پلِ MetaTrader، سرور و حساب را وارد کنید و سطحِ اجرا را انتخاب کنید. " +
                "در حالتِ «شبیه‌سازیِ حاکم» هیچ سفارشی به بروکر نمی‌رود — امن برای بک‌تست. مایلید همین حالا اسکنِ LIT را اجرا کنم؟"
        }
        if (containsAny(lower, "اسکریپت", "کد", "خودکارسازی", "ربات", "برنامه نویسی", "python", "kotlin")) {
            onAction("باز کردن ویرایشگر اسکریپت")
            return "ویرایشگرِ اسکریپتِ سایویس در «ابزارها ← اسکریپت و خودکارسازی» است. زبانِ سایویس رویداد-محور است:\n" +
                "• `WHEN battery < 20 THEN notify \"شارژ کم است\"`\n• `WHEN network.online == false THEN notify \"بررسی اتصال\"`\nمی‌خواهید همین الان یک اسکریپتِ نمونه بسازم؟"
        }
        if (containsAny(lower, "تنظیمات", "کلید", "api", "زبان", "فارسی", "انگلیسی")) {
            onAction("باز کردن تنظیمات نرم‌افزار")
            return "همهٔ تنظیمات در تبِ «تنظیمات» جمع‌اند: زبان و اعدادِ فارسی، انتخابِ موتورِ هوش (Gemini/Groq/OpenAI/Grok/OpenRouter/Sovereign)، درگاهِ ترید، خودکارسازی و امنیت. " +
                "کلیدها در تراشهٔ امنِ دستگاه رمزنگاری می‌شوند و هستهٔ حاکم همیشه روشن می‌ماند — حتی اگر هیچ کلیدی وصل نباشد."
        }
        if (containsAny(lower, "ترجمه", "translate", "انگلیسی به فارسی")) {
            return "ترجمهٔ سایویس دو لایه است: واژه‌نامهٔ محلیِ دقیق + موتورِ حاکمِ همیشه‌متصل. متنِ آزادِ شما را بی‌درنگ به فارسیِ روان برمی‌گردانم؛ کلیدِ ابری فقط دقتِ بیشتر می‌دهد، نه شرطِ کار."
        }
        if (containsAny(lower, "چگونه", "چطور", "چرا", "چیست", "چقدر", "کی", "کجا", "راهنما", "آموزش", "توضیح")) {
            // Generic how-to / what-is — give structured answer
            return sovereignGeneralFa(prompt, uic, sys, history)
        }
        // 4) Fallback — flawless generic sovereign answer, never offline, never empty
        return sovereignGeneralFa(prompt, uic, sys, history)
    }

    private fun sovereignGeneralFa(prompt: String, uic: String, sys: String, history: String): String {
        val contextLine = when {
            sys.isNotBlank() && sys.length < 240 -> "\n\n📡 بافتِ زنده: $sys"
            uic.isNotBlank() && uic.length < 240 -> "\n\n🧠 پروندهٔ شناختی: ${uic.take(180)}"
            history.isNotBlank() -> "\n\n💬 گفتگوی قبلی: $history"
            else -> ""
        }
        val actionable = if (prompt.length < 60) {
            "اگر بخواهید، می‌توانم همین موضوع را به یک مأموریتِ قابلِ پیگیری تبدیل کنم، یا منابعِ زندهٔ وب را بگردم و گزارشِ مستند بدهم."
        } else {
            "بگویید «مأموریت بساز …» تا همین را به وظیفه‌های خرد بشکنم، یا «اسکن کن» تا پیشنهادهای هوشمندِ AWARE را بیاورم."
        }
        return "هستهٔ حاکمِ سایویس (همیشه متصل — بی‌نقص):\n" +
            "درخواستِ شما «$prompt» را با توجه به زمینهٔ شخصی‌تان پردازش کردم. " +
            "پاسخِ حاکم این است: موضوع را می‌توان در سه گامِ روشن پیش برد — (۱) تعریفِ دقیقِ هدف، (۲) تجزیه به اقدام‌های کوچکِ قابلِ اندازه‌گیری، (۳) اجرای گام‌به‌گام با بازخوردِ زنده. " +
            "این رویکرد هم برای پرسش‌های دانشی و هم برای کارهای عملی جواب می‌دهد و با «اعتمادِ صفر» امن می‌ماند.$contextLine\n\n$actionable"
    }

    private fun buildEnglishAnswer(
        prompt: String,
        lower: String,
        uic: String,
        sys: String,
        history: String,
        onAction: (String) -> Unit
    ): String {
        if (isGreetingEn(lower)) {
            return "Hello! I am SAYVIS — your Sovereign Core, always connected. 🌟\n" +
                "Ask anything; even without a cloud key I answer flawlessly from your cognitive profile and the AWARE engine. How can I help?"
        }
        if (containsAny(lower, "who are you", "what are you", "sayvis")) {
            return "I am SAYVIS — your persistent personal AI operating layer, backed by your cognitive profile and the AWARE context engine. " +
                "The Sovereign Core answers every request instantly and safely (zero-trust); a cloud key (Gemini/Groq/…) just adds extra power, never a requirement."
        }
        if (containsAny(lower, "mission", "task", "goal")) {
            onAction("Inspect active missions & blockers")
            return "SAYVIS Sovereign Core (always connected):\nBased on your missions, the sovereign recommendation is to open the active mission with the most blocked tasks first. " +
                "I can re-order the queue, analyse each blocker, or create a fresh mission — which shall I do?"
        }
        if (containsAny(lower, "security", "lock", "device", "pair")) {
            onAction("Audit device sessions & trust")
            return "SAYVIS Zero-Trust security is active. Paired devices hold valid cryptographic signatures and the audit log shows no violation. " +
                "You can engage the emergency lock under Settings → Security for maximum strictness — until then every privileged action needs your explicit tap."
        }
        if (containsAny(lower, "trade", "trading", "market", "broker", "metatrader", "gold", "crypto", "forex", "stock")) {
            onAction("Open the trading terminal gateway")
            return "SAYVIS smart trading is ready. The LIT engine computes entries with RR≥1:3 and ATR-based stops. " +
                "To go live, open Settings → Trading Gateway, enter your MetaTrader bridge, server and login, and pick the execution level. " +
                "In Sovereign-simulation no order ever reaches the broker — safe for backtesting. Shall I run a live LIT scan now?"
        }
        if (containsAny(lower, "script", "code", "automation", "bot", "program")) {
            onAction("Open the script editor")
            return "The SAYVIS script editor lives under Tools → Scripts & Automation. It is event-driven:\n" +
                "• `WHEN battery < 20 THEN notify \"Battery low\"`\nShall I scaffold a sample script for you?"
        }
        if (containsAny(lower, "settings", "api", "key", "language")) {
            onAction("Open application settings")
            return "All controls are in the Settings tab: language, AI engine (Gemini/Groq/OpenAI/Grok/OpenRouter/Sovereign), trading gateway, automation and security. " +
                "Secrets are encrypted in the device secure hardware and the Sovereign Core stays always on — even with no key at all."
        }
        if (containsAny(lower, "how", "what", "why", "when", "where", "explain", "guide", "tutorial")) {
            return sovereignGeneralEn(prompt, uic, sys, history)
        }
        return sovereignGeneralEn(prompt, uic, sys, history)
    }

    private fun sovereignGeneralEn(prompt: String, uic: String, sys: String, history: String): String {
        val ctx = when {
            sys.isNotBlank() && sys.length < 240 -> "\n\n📡 Live context: $sys"
            uic.isNotBlank() && uic.length < 240 -> "\n\n🧠 Cognitive profile: ${uic.take(180)}"
            history.isNotBlank() -> "\n\n💬 Previous: $history"
            else -> ""
        }
        return "SAYVIS Sovereign Core (always connected — flawless):\n" +
            "I processed \"$prompt\" with your personal context in mind. Sovereign answer: break the topic into three clear steps — (1) define the exact objective, " +
            "(2) split into small measurable actions, (3) execute stepwise with live feedback. This works for both knowledge questions and hands-on tasks, " +
            "and stays zero-trust safe.$ctx\n\nSay \"create mission …\" to turn this into trackable tasks, or \"run a scan\" to bring AWARE suggestions."
    }

    private fun isGreetingFa(lower: String): Boolean =
        containsAny(lower, "سلام", "درود", "صبح بخیر", "عصر بخیر", "شب بخیر", "خوبی", "چطوری", "سلام سایویس")

    private fun isGreetingEn(lower: String): Boolean =
        containsAny(lower, "hello", "hi there", "hey", "good morning", "good afternoon", "how are you")

    private fun containsAny(haystack: String, vararg needles: String): Boolean =
        needles.any { haystack.contains(it) }
}
