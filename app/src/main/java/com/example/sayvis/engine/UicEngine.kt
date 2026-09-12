package com.example.sayvis.engine

import com.example.sayvis.data.repository.SayvisRepository
import com.example.sayvis.model.PrivacyLevel
import com.example.sayvis.model.UicAttribute
import com.example.sayvis.model.UicCategory
import com.example.sayvis.model.UicStatus
import java.util.UUID

data class UicMetrics(
    val totalAttributes: Int,
    val confirmedCount: Int,
    val inferredCount: Int,
    val observedCount: Int,
    val averageConfidence: Float
)

class UicEngine(
    private val repository: SayvisRepository
) {
    suspend fun addAttribute(
        category: UicCategory,
        key: String,
        title: String,
        value: String,
        provenance: String,
        confidence: Float,
        status: UicStatus,
        privacyLevel: PrivacyLevel
    ) {
        val attribute = UicAttribute(
            id = "uic_" + UUID.randomUUID().toString().take(8),
            category = category,
            key = key.trim().lowercase().replace(" ", "_"),
            title = title,
            value = value,
            provenance = provenance,
            confidence = confidence.coerceIn(0.0f, 1.0f),
            status = status,
            privacyLevel = privacyLevel,
            firstObservedAt = System.currentTimeMillis(),
            lastConfirmedAt = if (status == UicStatus.CONFIRMED) System.currentTimeMillis() else null,
            updatedAt = System.currentTimeMillis()
        )
        repository.insertUicAttribute(attribute)
    }

    suspend fun confirmInference(attributeId: String) {
        repository.confirmUicAttribute(attributeId)
    }

    suspend fun revokeAttribute(attributeId: String) {
        repository.revokeUicAttribute(attributeId)
    }

    suspend fun deleteAttribute(attributeId: String) {
        repository.deleteUicAttribute(attributeId)
    }

    fun computeMetrics(attributes: List<UicAttribute>): UicMetrics {
        val total = attributes.size
        if (total == 0) return UicMetrics(0, 0, 0, 0, 0f)
        val confirmed = attributes.count { it.status == UicStatus.CONFIRMED }
        val inferred = attributes.count { it.status == UicStatus.INFERRED }
        val observed = attributes.count { it.status == UicStatus.OBSERVED }
        val avgConf = attributes.map { it.confidence }.average().toFloat()
        return UicMetrics(
            totalAttributes = total,
            confirmedCount = confirmed,
            inferredCount = inferred,
            observedCount = observed,
            averageConfidence = avgConf
        )
    }
}
