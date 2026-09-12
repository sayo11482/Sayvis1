package com.example.sayvis.model

enum class LifeDomain(val labelEn: String, val labelFa: String) {
    CAREER_WORK("Career & Work", "کار و حرفه"),
    FINANCE("Financial Architecture", "ساختار مالی"),
    PROJECTS("Strategic Projects", "پروژه‌های راهبردی"),
    HEALTH_ENERGY("Health & Energy", "سلامت و انرژی"),
    RELATIONSHIPS("Key Relationships", "روابط کلیدی"),
    COGNITIVE_LEARNING("Cognitive & Learning", "یادگیری و مهارت")
}

data class LifeScenario(
    val id: String,
    val domain: LifeDomain,
    val decisionHypothesis: String,
    val projectedTrajectory: String,
    val confidenceScore: Float,
    val detectedRisks: List<String>,
    val projectedOpportunities: List<String>,
    val isSpeculativeNotice: Boolean = true
)
