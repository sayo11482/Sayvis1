package com.example.sayvis.model

/**
 * Category of User Cognitive Model (UIC) attributes.
 */
enum class UicCategory(val displayNameEn: String, val displayNameFa: String) {
    PREFERENCES("Preferences", "ترجیحات"),
    GOALS("Goals", "اهداف"),
    HABITS("Habits", "عادت‌ها"),
    WORKING_PATTERNS("Working Patterns", "الگوهای کاری"),
    INTERACTION_STYLE("Interaction Style", "سبک تعامل"),
    PRIORITIES("Priorities", "اولویت‌ها"),
    CONSTRAINTS("Constraints", "محدودیت‌ها"),
    LONG_TERM_OBJECTIVES("Long-Term Objectives", "اهداف بلندمدت")
}

/**
 * Epistemic status of a cognitive attribute.
 * In SAYVIS, inferred data is never treated as confirmed fact until the owner validates it.
 */
enum class UicStatus(val labelEn: String, val labelFa: String) {
    OBSERVED("Observed", "مشاهده‌شده"),
    INFERRED("Inferred", "استنتاج‌شده"),
    CONFIRMED("Confirmed", "تأییدشده"),
    EXPIRED("Expired", "منقضی‌شده"),
    REVOKED("Revoked", "لغوشده")
}

/**
 * Privacy classification to safeguard personal attributes.
 */
enum class PrivacyLevel(val labelEn: String, val labelFa: String) {
    STANDARD("Standard", "عادی"),
    PROTECTED("Protected", "محافظت‌شده"),
    CONFIDENTIAL("Confidential", "فوق‌محرمانه")
}

/**
 * Structured User Cognitive Model attribute with provenance, confidence, and status.
 */
data class UicAttribute(
    val id: String,
    val category: UicCategory,
    val key: String,
    val title: String,
    val value: String,
    val provenance: String,
    val confidence: Float, // 0.0 to 1.0
    val status: UicStatus,
    val privacyLevel: PrivacyLevel = PrivacyLevel.STANDARD,
    val firstObservedAt: Long = System.currentTimeMillis(),
    val lastConfirmedAt: Long? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
