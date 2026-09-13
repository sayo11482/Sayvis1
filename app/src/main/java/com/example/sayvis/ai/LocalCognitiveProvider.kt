package com.example.sayvis.ai

import com.example.sayvis.settings.AiSettings
import kotlinx.coroutines.delay

/**
 * Deterministic, fully offline cognitive provider.
 *
 * This is the sovereignty guarantee: with no key, no network and no cloud account the
 * app still answers, still explains its reasoning and still proposes actions. It matches
 * on intent keywords in both Persian and English so a Persian question never gets an
 * English answer.
 */
class LocalCognitiveProvider : AIProvider {

    override val providerType: ProviderType = ProviderType.LOCAL_COGNITIVE

    override fun isConfigured(settings: AiSettings): Boolean = true

    override suspend fun generateResponse(
        context: AiRequestContext,
        settings: AiSettings
    ): AIResponse {
        val start = System.currentTimeMillis()
        delay(220) // Small deliberate pause so the "thinking" avatar state is perceptible.

        val prompt = context.prompt
        val lower = prompt.lowercase()
        val languageFa = context.languageFa

        var suggestedAction: String? = null
        val text: String = if (languageFa) {
            when {
                containsAny(lower, "ماموریت", "مأموریت", "کار", "وظیفه", "هدف", "mission") -> {
                    suggestedAction = "بررسی مأموریت‌های فعال و مسدودشده"
                    "هستهٔ محلی سایویس (آفلاین امن):\nبر اساس پروندهٔ شناختی شما و اهداف ثبت‌شده، اولویت فعلی شما مأموریت فعال با بیشترین وظیفهٔ مسدودشده است. " +
                        "می‌توانم ترتیب وظیفه‌ها را بازچینی کنم یا مانع‌ها را یکی‌یکی بررسی کنیم. کدام را ترجیح می‌دهید؟"
                }
                containsAny(lower, "امنیت", "قفل", "دستگاه", "رمز", "security") -> {
                    suggestedAction = "بازبینی گزارش رویدادها و دستگاه‌ها"
                    "امنیت سایویس فعال است و مدل «اعتماد صفر» اجرا می‌شود. دستگاه‌های جفت‌شده دارای امضای رمزنگاری معتبرند و هیچ نقض امنیتی ثبت نشده است. " +
                        "برای سخت‌گیری بیشتر می‌توانید قفل اضطراری را فعال کنید."
                }
                containsAny(lower, "ترید", "معامله", "بازار", "متاتریدر", "بروکر", "trade", "trading") -> {
                    suggestedAction = "باز کردن درگاه ترمینال ترید"
                    "بخش معاملات در دسترس است. برای اتصال به متاتریدر ۴ یا ۵ به «تنظیمات ← درگاه معاملاتی» بروید و پل ارتباطی، سرور و شمارهٔ حساب را وارد کنید. " +
                        "تا وقتی سطح اجرا روی «شبیه‌سازی کاغذی» است، هیچ سفارشی به بروکر ارسال نمی‌شود."
                }
                containsAny(lower, "اسکریپت", "کد", "خودکارسازی", "ربات", "script") -> {
                    suggestedAction = "باز کردن ویرایشگر اسکریپت"
                    "ویرایشگر اسکریپت در «ابزارها ← اسکریپت و خودکارسازی» قرار دارد. زبان اسکریپت سایویس اجازه می‌دهد شرط و واکنش بنویسید؛ " +
                        "مثلاً: WHEN battery < 20 THEN notify \"شارژ کم است\"."
                }
                containsAny(lower, "تنظیمات", "api", "کلید", "زبان", "setting") -> {
                    suggestedAction = "باز کردن تنظیمات نرم‌افزار"
                    "همهٔ تنظیمات در تب «تنظیمات» جمع شده‌اند: زبان و اعداد فارسی، سرویس هوش مصنوعی و کلید API، درگاه معاملاتی، خودکارسازی، امنیت و داده‌ها. " +
                        "کلیدها داخل تراشهٔ امن دستگاه رمزنگاری می‌شوند."
                }
                containsAny(lower, "هوش", "سایو", "sayvis", "کیستی", "کی هستی") -> {
                    "من سایویس (SAYVIS) هستم؛ لایهٔ عامل و سیستم‌عامل هوش مصنوعی شخصی شما. به پروندهٔ شناختی شما و موتور ادراک محیطی متصل‌ام تا مأموریت‌ها را هدایت کنم " +
                        "و حاکمیت داده‌های شخصی‌تان حفظ شود. اکنون از هستهٔ محلی و آفلاین پاسخ می‌دهم."
                }
                containsAny(lower, "ترجمه", "فارسی", "translate") -> {
                    "ترجمهٔ متن‌های آزاد در سایویس دو مرحله‌ای است: نخست واژه‌نامهٔ داخلی آفلاین، و اگر پوشش نداد، ترجمهٔ آنلاین با هوش مصنوعی. " +
                        "برای فعال بودن مرحلهٔ دوم، در «تنظیمات ← زبان و نمایش» گزینهٔ «ترجمهٔ هوشمند متن‌های آزاد» را روشن نگه دارید و یک کلید API معتبر ثبت کنید."
                }
                else -> {
                    "هستهٔ محلی سایویس:\nدرخواست شما «$prompt» با رعایت کامل حریم خصوصی و به‌صورت آفلاین پردازش شد. " +
                        "اگر پاسخ عمیق‌تری می‌خواهید، در «تنظیمات ← هوش مصنوعی و API» یک سرویس ابری را با کلید معتبر فعال کنید."
                }
            }
        } else {
            when {
                containsAny(lower, "mission", "task", "goal") -> {
                    suggestedAction = "Inspect active missions & blockers"
                    "SAYVIS sovereign local core:\nAccording to your cognitive profile and strategic objectives, the highest-priority mission is the one carrying the most blocked tasks. " +
                        "I can re-order the task queue or walk through each blocker — which would you prefer?"
                }
                containsAny(lower, "security", "lock", "device") -> {
                    suggestedAction = "Audit device sessions & trust"
                    "SAYVIS Zero-Trust security is active. Every privileged operation requires explicit owner confirmation, paired devices hold valid cryptographic signatures, and no policy violation has been recorded."
                }
                containsAny(lower, "trade", "trading", "market", "broker", "metatrader") -> {
                    suggestedAction = "Open the trading terminal gateway"
                    "The trading area is available. To link MetaTrader 4/5, open Settings → Trading Gateway and enter the bridge, server and account login. " +
                        "While the execution level stays on paper simulation, no order is ever routed to the broker."
                }
                containsAny(lower, "script", "code", "automation", "bot") -> {
                    suggestedAction = "Open the script editor"
                    "The script editor lives under Tools → Scripts & Automation. The SAYVIS scripting language lets you write condition/reaction rules, " +
                        "for example: WHEN battery < 20 THEN notify \"Battery is low\"."
                }
                containsAny(lower, "settings", "api", "key", "language") -> {
                    suggestedAction = "Open application settings"
                    "All controls are gathered in the Settings tab: language and Persian digits, AI provider and API key, trading gateway, automation, security and data. " +
                        "Secrets are encrypted inside the device secure hardware."
                }
                containsAny(lower, "who", "sayvis", "what are you") -> {
                    "I am SAYVIS — your persistent personal AI operating layer, backed by your cognitive profile and the AWARE context engine to protect sovereignty and accelerate missions. " +
                        "Right now I am answering from the offline local core."
                }
                else -> {
                    "SAYVIS local cognitive node:\nProcessed \"$prompt\" in Zero-Trust local safe mode. " +
                        "For a deeper answer, enable a cloud provider with a valid key under Settings → AI & API."
                }
            }
        }

        return AIResponse(
            text = text,
            providerUsed = ProviderType.LOCAL_COGNITIVE,
            model = "sayvis-local-core",
            isOfflineMode = true,
            suggestedAction = suggestedAction,
            processingTimeMs = System.currentTimeMillis() - start
        )
    }

    private fun containsAny(haystack: String, vararg needles: String): Boolean =
        needles.any { haystack.contains(it) }
}
