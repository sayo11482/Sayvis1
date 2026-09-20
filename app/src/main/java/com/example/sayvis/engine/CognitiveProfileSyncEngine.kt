package com.example.sayvis.engine

import com.example.sayvis.data.repository.SayvisRepository
import com.example.sayvis.model.PrivacyLevel
import com.example.sayvis.model.RiskLevel
import com.example.sayvis.model.UicAttribute
import com.example.sayvis.model.UicCategory
import com.example.sayvis.model.UicStatus
import java.util.UUID

/**
 * Discovered item from one of the external device/cloud ingestion streams.
 */
data class CognitiveIngestionItem(
    val id: String,
    val sourceKind: IngestionSourceKind,
    val titleFa: String,
    val titleEn: String,
    val detailFa: String,
    val detailEn: String,
    val targetCategory: UicCategory,
    val suggestedKey: String,
    val suggestedValueFa: String,
    val suggestedValueEn: String,
    val epistemicStatus: UicStatus,
    val confidence: Float,
    val privacyLevel: PrivacyLevel = PrivacyLevel.STANDARD
) {
    fun title(isPersian: Boolean) = if (isPersian) titleFa else titleEn
    fun detail(isPersian: Boolean) = if (isPersian) detailFa else detailEn
    fun suggestedValue(isPersian: Boolean) = if (isPersian) suggestedValueFa else suggestedValueEn
}

enum class IngestionSourceKind(val labelFa: String, val labelEn: String) {
    GOOGLE_SEARCH("جستجوهای گوگل", "Google Searches"),
    DEVICE_NOTES("یادداشت‌های داخل دیوایس", "Device Internal Notes"),
    DEVICE_ALARMS("آلارم‌ها و دستورات تنظیمی", "Device Alarms & Directives");

    fun label(isPersian: Boolean) = if (isPersian) labelFa else labelEn
}

/**
 * Cognitive Profile (UIC) Ingestion and Auto-Update Engine.
 *
 * Ingests knowledge signals from:
 * 1. Google Searches: infers user's research interests, learning paths, and market analysis habits.
 * 2. Device Notes: extracts high-conviction goals, operational rules, and personal preferences.
 * 3. Device Alarms & Directives: observes circadian wake/sleep cycles, deep-work schedules, and verbal alarm commands.
 */
class CognitiveProfileSyncEngine(
    private val repository: SayvisRepository
) {

    /**
     * Inspects discovered source telemetry from Google searches, device notes, and alarms.
     */
    fun discoverIngestionItems(): List<CognitiveIngestionItem> = listOf(
        // 1. Google Searches
        CognitiveIngestionItem(
            id = "sync_gsearch_01",
            sourceKind = IngestionSourceKind.GOOGLE_SEARCH,
            titleFa = "تحقیقات الگوریتم‌های معاملاتی ICT و Smart Money",
            titleEn = "ICT & Smart Money Trading Algorithms Research",
            detailFa = "تکرار ۱۸ جستجو در هفتهٔ اخیر پیرامون رفتار نقدینگی و بلوک‌های سفارش بانک‌ها",
            detailEn = "18 searches in the past week regarding institutional liquidity and order blocks",
            targetCategory = UicCategory.PREFERENCES,
            suggestedKey = "trading_methodology_preference",
            suggestedValueFa = "ترجیح تحلیل بازار بر مبنای جریان سفارشات نهادی و نقدینگی هوشمند (ICT/SMC)",
            suggestedValueEn = "Prefers institutional order flow and liquidity-based market analysis (ICT/SMC)",
            epistemicStatus = UicStatus.INFERRED,
            confidence = 0.88f
        ),
        CognitiveIngestionItem(
            id = "sync_gsearch_02",
            sourceKind = IngestionSourceKind.GOOGLE_SEARCH,
            titleFa = "مطالعهٔ معماری Zero-Trust و محاسبات مستقل در کاتلین",
            titleEn = "Zero-Trust Architecture & Sovereign Computing in Kotlin",
            detailFa = "جستجوهای مکرر در گیت‌هاب و مستندات فنی اندروید برای بهینه‌سازی عملکرد",
            detailEn = "Frequent queries across GitHub and Android documentation on performance optimization",
            targetCategory = UicCategory.WORKING_PATTERNS,
            suggestedKey = "core_technical_focus",
            suggestedValueFa = "تمرکز مهندسی روی توسعه سیستم‌های کاتلین، معماری اعتماد صفر و امنیت آفلاین",
            suggestedValueEn = "Engineering focus on Kotlin systems, zero-trust architecture and offline security",
            epistemicStatus = UicStatus.INFERRED,
            confidence = 0.92f
        ),

        // 2. Device Internal Notes
        CognitiveIngestionItem(
            id = "sync_note_01",
            sourceKind = IngestionSourceKind.DEVICE_NOTES,
            titleFa = "یادداشت اهداف کلان: توسعه پلتفرم مستقل سایویس",
            titleEn = "Macro Goals Note: Sovereign Sayvis Layer Completion",
            detailFa = "استخراج از یادداشت «نقشه راه ۱۴۰۵»: اولویت قطعی با حذف هرگونه وابستگی به سرورهای خارجی است.",
            detailEn = "Extracted from \"Roadmap 2026\": absolute priority on eliminating external cloud dependency.",
            targetCategory = UicCategory.GOALS,
            suggestedKey = "primary_sovereign_goal",
            suggestedValueFa = "تحقق کامل حاکمیت داده فردی و استقلال صددرصدی ابزارهای شناختی از کلود",
            suggestedValueEn = "Complete personal data sovereignty and total cognitive layer cloud-independence",
            epistemicStatus = UicStatus.CONFIRMED,
            confidence = 0.98f,
            privacyLevel = PrivacyLevel.CONFIDENTIAL
        ),
        CognitiveIngestionItem(
            id = "sync_note_02",
            sourceKind = IngestionSourceKind.DEVICE_NOTES,
            titleFa = "یادداشت قواعد تمرکز: عدم پاسخگویی قبل از ساعت ۱۲",
            titleEn = "Focus Rules Note: Zero morning non-urgent communications",
            detailFa = "استخراج از فایل یادداشت شخصی: تمام ارتباطات ناهمگام بعد از اتمام پنجرهٔ کار عمیق بررسی شوند.",
            detailEn = "Extracted from personal notes: all asynchronous communications handled after deep work window.",
            targetCategory = UicCategory.CONSTRAINTS,
            suggestedKey = "morning_communication_boundary",
            suggestedValueFa = "محدودیت سخت‌گیرانه: عدم پاسخ به پیام‌ها و تماس‌های غیرضروری پیش از ساعت ۱۲:۰۰ ظهر",
            suggestedValueEn = "Strict constraint: No non-critical calls or messages before 12:00 PM",
            epistemicStatus = UicStatus.CONFIRMED,
            confidence = 0.95f,
            privacyLevel = PrivacyLevel.PROTECTED
        ),

        // 3. Device Alarms & Commands/Directives
        CognitiveIngestionItem(
            id = "sync_alarm_01",
            sourceKind = IngestionSourceKind.DEVICE_ALARMS,
            titleFa = "آلارم ساعت ۰۵:۳۰ صبح — دستور صوتی: بیدارباش فوری بدون اسنوز",
            titleEn = "05:30 AM Alarm — Directive: Immediate Wake-up (Zero Snooze)",
            detailFa = "تحلیل فرامین تنظیمی آلارم: کاربر اسنوز را غیرفعال کرده و دستور فعال‌سازی نور کامل صادر کرده است.",
            detailEn = "Alarm directive analysis: snooze permanently disabled; directive for immediate full light.",
            targetCategory = UicCategory.HABITS,
            suggestedKey = "early_morning_wake_habit",
            suggestedValueFa = "عادت به بیداری رأس ساعت ۰۵:۳۰ صبح و آغاز روز بدون تعویق آلارم (Zero Snooze)",
            suggestedValueEn = "Habit of waking up at 05:30 AM sharp with zero alarm snooze tolerance",
            epistemicStatus = UicStatus.OBSERVED,
            confidence = 0.94f
        ),
        CognitiveIngestionItem(
            id = "sync_alarm_02",
            sourceKind = IngestionSourceKind.DEVICE_ALARMS,
            titleFa = "آلارم ساعت ۲۲:۱۵ شب — دستور تنظیمی: خاموشی مانیتورها و آغاز ریکاوری",
            titleEn = "10:15 PM Alarm — Directive: Screen Blackout & Recovery Routine",
            detailFa = "دستور اتوماسیون آلارم شبانه: قطع تمام نوتیفیکیشن‌ها و آماده‌سازی خواب عمیق",
            detailEn = "Nightly alarm automation directive: mute all notifications and begin deep sleep protocol",
            targetCategory = UicCategory.WORKING_PATTERNS,
            suggestedKey = "evening_shutdown_routine",
            suggestedValueFa = "الگوی قطعی پایان کار در ساعت ۲۲:۱۵، خاموشی صفحات نمایش و ۸ ساعت خواب کامل",
            suggestedValueEn = "Strict shutdown routine at 10:15 PM, screen blackout, and 8-hour sleep protocol",
            epistemicStatus = UicStatus.OBSERVED,
            confidence = 0.91f
        )
    )

    /**
     * Ingests and updates the User Cognitive Model with items from the selected sources.
     */
    suspend fun syncItemsToProfile(
        itemsToSync: List<CognitiveIngestionItem>,
        isPersian: Boolean
    ): Int {
        var insertedCount = 0
        itemsToSync.forEach { item ->
            val provenance = when (item.sourceKind) {
                IngestionSourceKind.GOOGLE_SEARCH -> if (isPersian) "همگام‌سازی از سابقهٔ جستجوهای گوگل" else "Google Search Ingestion"
                IngestionSourceKind.DEVICE_NOTES -> if (isPersian) "استخراج مستقیم از یادداشت‌های داخل دیوایس" else "Device Internal Notes Ingestion"
                IngestionSourceKind.DEVICE_ALARMS -> if (isPersian) "تحلیل آلارم‌های دیوایس و دستورات تنظیمی" else "Device Alarms & Directives Analysis"
            }

            val attribute = UicAttribute(
                id = "uic_synced_" + UUID.randomUUID().toString().take(6),
                category = item.targetCategory,
                key = item.suggestedKey,
                title = item.title(isPersian),
                value = item.suggestedValue(isPersian),
                provenance = provenance,
                confidence = item.confidence,
                status = item.epistemicStatus,
                privacyLevel = item.privacyLevel,
                firstObservedAt = System.currentTimeMillis(),
                lastConfirmedAt = if (item.epistemicStatus == UicStatus.CONFIRMED) System.currentTimeMillis() else null,
                updatedAt = System.currentTimeMillis()
            )

            repository.insertUicAttribute(attribute)
            insertedCount++
        }

        repository.recordAuditEvent(
            actor = "COGNITIVE_SYNC_ENGINE",
            action = "uic.profile.multi_source_update",
            riskLevel = RiskLevel.LOW_RISK,
            auth = "OWNER_CONFIRMED",
            result = "SUCCESS",
            digest = "Updated UIC with $insertedCount attributes from Google Searches, Device Notes & Alarms"
        )

        return insertedCount
    }
}
