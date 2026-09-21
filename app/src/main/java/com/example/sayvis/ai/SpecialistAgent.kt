package com.example.sayvis.ai

import com.example.sayvis.settings.AiProviderKind
import com.example.sayvis.settings.AiSettings

/**
 * SAYVIS specialist agents. Each agent is the same reliable pipeline —
 * live web grounding + the best available brain + a specialist prompt — but
 * with a distinct professional contract, exactly what a real agent needs:
 *
 *  TRADE     market/structure analysis with sources (education, not signals)
 *  CONTENT   Instagram content system: hooks, captions, hashtags, calendar
 *  WEBDESIGN minimal high-craft site design + starter HTML/CSS
 *  APPDEV    app spec + Kotlin module scaffold + build plan
 *
 * The brain is chosen AUTOMATICALLY, free-tier first, every single run
 * ("لحظه‌به‌لحظه بهترین AI رایگان و کم‌توکن") — see [pickBrain].
 */
object SpecialistAgent {

    enum class Kind(val labelFa: String, val labelEn: String, val icon: String) {
        RESEARCH("پژوهش", "Research", "research"),
        TRADE("ایجنت ترید", "Trade agent", "trade"),
        CONTENT("محتوا و اینستاگرام", "Content & Instagram", "content"),
        WEBDESIGN("طراحی سایت", "Web design", "web"),
        APPDEV("اپلیکیشن‌سازی", "App builder", "app");

        companion object {
            fun from(icon: String): Kind = entries.firstOrNull { it.icon == icon } ?: RESEARCH
        }
    }

    /** A brain offer: provider + its current model + a human note. */
    data class Brain(val kind: AiProviderKind, val model: String, val noteFa: String, val noteEn: String)

    /**
     * Free-first, low-token-first brain ranking — re-evaluated on EVERY run
     * against the keys actually configured. Gemini 2.5 Flash and Groq Llama
     * have the strongest free tiers, so they lead; paid-only brains follow.
     */
    fun pickBrain(settings: AiSettings): Brain? {
        val candidates = listOf(
            Brain(AiProviderKind.GEMINI, settings.geminiModel, "جمینای فلش — رایگان تا سهمیهٔ روزانه", "Gemini Flash — free daily tier"),
            Brain(AiProviderKind.GROQ, settings.groqModel, "گروک LPU — رایگان و بسیار سریع، کم‌توکن", "Groq LPU — free, fast, token-lean"),
            Brain(AiProviderKind.OPENAI, settings.openAiModel, "چت‌جی‌پی‌تی — پولی، دقیق", "ChatGPT — paid, precise"),
            Brain(AiProviderKind.XAI, settings.xaiModel, "گراک — پولی", "Grok — paid"),
            Brain(AiProviderKind.OPENROUTER, settings.openRouterModel, "اوپن‌روتر — چندمدلی (مدل‌های :free)", "OpenRouter — multi-model (:free options)"),
            Brain(AiProviderKind.CUSTOM, settings.customModel, "سرویس دلخواه مالک", "Owner's custom endpoint")
        )
        return candidates.firstOrNull { brain ->
            when (brain.kind) {
                AiProviderKind.GEMINI -> settings.geminiApiKey.isNotBlank()
                AiProviderKind.GROQ -> settings.groqApiKey.isNotBlank()
                AiProviderKind.OPENAI -> settings.openAiApiKey.isNotBlank()
                AiProviderKind.XAI -> settings.xaiApiKey.isNotBlank()
                AiProviderKind.OPENROUTER -> settings.openRouterApiKey.isNotBlank()
                AiProviderKind.CUSTOM -> settings.customBaseUrl.isNotBlank() && settings.customModel.isNotBlank()
                AiProviderKind.LOCAL -> false
            }
        }
    }

    /** The specialist system contract for one run. */
    fun prompt(kind: Kind, goal: String, languageFa: Boolean, ownerContext: String): String {
        val extra = if (ownerContext.isBlank()) "" else "\n$ownerContext\n"
        return when (kind) {
            Kind.RESEARCH -> if (languageFa) goal else goal
            Kind.TRADE -> if (languageFa) {
                "ایجنت ترید سایویس هستی. وظیفه: «$goal».$extra" +
                    "با داده‌های زندهٔ منابع، تحلیل حرفه‌ای بده: روند بازار، سطوح کلیدی حمایت/مقاومت، سناریوهای احتمالی با احتمال، مدیریت ریسک (حجم، حد ضرر)، و جمع‌بندی. " +
                    "همیشه ذکر کن این تحلیل آموزشی است نه سیگنال قطعی. ساختار: خلاصهٔ یک‌خطی → جدول سطوح → سناریوها → ریسک → چک‌لیست."
            } else {
                "You are SAYVIS's trade agent. Task: \"$goal\".$extra " +
                    "Using the live sources, deliver professional analysis: trend, key support/resistance levels, probable scenarios with likelihoods, risk management (sizing, stop), and a conclusion. " +
                    "Always state this is educational analysis, not financial advice. Structure: one-line summary → levels → scenarios → risk → checklist."
            }
            Kind.CONTENT -> if (languageFa) {
                "ایجنت محتوا و اینستاگرام سایویس هستی. وظیفه: «$goal».$extra" +
                    "خروجی: ۱) سه هوک نترکننده برای سه پست ۲) کپشن فارسی هرکدام با CTA ۳) ۱۲ هشتگ هدفمند ترکیبی (پرتکرار + نیچ) ۴) تقویم محتوایی ۷ روزه با بهترین ساعت پست ۵) پرامپت تصویر برای هر پست. لحن: حرفه‌ای، انسانی، بدون کلیشه."
            } else {
                "You are SAYVIS's content & Instagram agent. Task: \"$goal\".$extra " +
                    "Deliver: 1) three scroll-stopping hooks 2) a caption per hook with CTA 3) 12 targeted hashtags (broad + niche mix) 4) a 7-day content calendar with best posting times 5) an image prompt per post. Voice: professional, human, no clichés."
            }
            Kind.WEBDESIGN -> if (languageFa) {
                "ایجنت طراحی سایت سایویس هستی. وظیفه: «$goal».$extra" +
                    "خروجی: ۱) مفهوم و پالت رنگ با کدهای HEX ۲) تایپوگرافی ۳) نقشهٔ سکشن‌ها با هدف هر سکشن ۴) اسکلت HTML/CSS مینیمال و ریسپانسیو در یک بلوک کد ۵) سه ایدهٔ میکرواینترکشن. طراحی باید مینیمال، حرفه‌ای و مدرن باشد."
            } else {
                "You are SAYVIS's web-design agent. Task: \"$goal\".$extra " +
                    "Deliver: 1) concept + HEX palette 2) typography 3) section map with purpose 4) a minimal responsive HTML/CSS skeleton in one code block 5) three micro-interaction ideas. Keep it minimal, high-craft, modern."
            }
            Kind.APPDEV -> if (languageFa) {
                "ایجنت اپلیکیشن‌سازی سایویس هستی. وظیفه: «$goal».$extra" +
                    "خروجی: ۱) شرح اپ در سه خط ۲) معماری و ماژول‌ها ۳) مدل داده ۴) اسکلت کاتلین/کامپوز برای هستهٔ اصلی در بلوک کد ۵) نقشهٔ راه ساخت ۵ مرحله‌ای. به‌روزترین الگوها را از منابع زنده بگیر و نام ببر."
            } else {
                "You are SAYVIS's app-builder agent. Task: \"$goal\".$extra " +
                    "Deliver: 1) three-line app definition 2) architecture & modules 3) data model 4) Kotlin/Compose skeleton for the core in one code block 5) a 5-step build roadmap. Pull current best practices from the live sources and name them."
            }
        }
    }
}
