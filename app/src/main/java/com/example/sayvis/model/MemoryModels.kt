package com.example.sayvis.model

enum class MemoryType(val labelEn: String, val labelFa: String) {
    SHORT_TERM_CONTEXT("Short-Term Context", "زمینه کوتاه‌مدت"),
    LONG_TERM_MEMORY("Long-Term Memory", "حافظه بلندمدت"),
    PREFERENCES("Preferences", "ترجیحات"),
    GOALS("Goals", "اهداف"),
    MISSIONS("Missions", "مأموریت‌ها"),
    INTERACTION_HISTORY("Interaction History", "تاریخچه تعامل"),
    SYSTEM_MEMORY("System Memory", "حافظه سیستم"),
    DEVICE_MEMORY("Device Memory", "حافظه دستگاه")
}

enum class EpistemicStatus(val labelEn: String, val labelFa: String) {
    OBSERVED("Observed", "مشاهده‌شده"),
    CONFIRMED("Confirmed", "تأییدشده"),
    INFERRED("Inferred", "استنتاج‌شده"),
    TEMPORARY("Temporary", "موقتی"),
    HISTORICAL("Historical", "تاریخی")
}

enum class RetentionPolicy(val labelEn: String, val labelFa: String) {
    EPHEMERAL("Ephemeral", "زودگذر"),
    SESSION("Session", "تا پایان نشست"),
    PERSISTENT("Persistent", "دائمی"),
    ENCRYPTED_ARCHIVE("Encrypted Archive", "آرشیو رمزگذاری‌شده")
}

data class MemoryItem(
    val id: String,
    val type: MemoryType,
    val content: String,
    val source: String,
    val confidence: Float, // 0.0 - 1.0
    val provenance: String,
    val importance: Int, // 1 to 10
    val retentionPolicy: RetentionPolicy,
    val epistemicStatus: EpistemicStatus,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
