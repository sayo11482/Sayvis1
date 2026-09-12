package com.example.sayvis.model

enum class MissionStatus(val labelEn: String, val labelFa: String) {
    PLANNED("Planned", "برنامه‌ریزی‌شده"),
    ACTIVE("Active", "در حال اجرا"),
    AT_RISK("At Risk", "در معرض خطر"),
    BLOCKED("Blocked", "مسدودشده"),
    COMPLETED("Completed", "تکمیل‌شده"),
    CANCELLED("Cancelled", "لغوشده")
}

enum class MissionPriority(val labelEn: String, val labelFa: String) {
    LOW("Low", "پایین"),
    MEDIUM("Medium", "متوسط"),
    HIGH("High", "بالا"),
    CRITICAL("Critical", "بحرانی")
}

data class MissionTask(
    val id: String,
    val title: String,
    val isCompleted: Boolean = false,
    val isBlocked: Boolean = false,
    val blockerReason: String? = null
)

data class Mission(
    val id: String,
    val title: String,
    val description: String,
    val priority: MissionPriority,
    val status: MissionStatus,
    val progressPercent: Int, // 0 to 100
    val deadline: String,
    val tasks: List<MissionTask> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)
