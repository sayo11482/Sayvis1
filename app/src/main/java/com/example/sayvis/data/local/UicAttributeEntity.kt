package com.example.sayvis.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.sayvis.model.PrivacyLevel
import com.example.sayvis.model.UicAttribute
import com.example.sayvis.model.UicCategory
import com.example.sayvis.model.UicStatus

@Entity(tableName = "uic_attributes")
data class UicAttributeEntity(
    @PrimaryKey val id: String,
    val category: String,
    val key: String,
    val title: String,
    val value: String,
    val provenance: String,
    val confidence: Float,
    val status: String,
    val privacyLevel: String,
    val firstObservedAt: Long,
    val lastConfirmedAt: Long?,
    val updatedAt: Long
) {
    fun toDomain(): UicAttribute = UicAttribute(
        id = id,
        category = runCatching { UicCategory.valueOf(category) }.getOrDefault(UicCategory.PREFERENCES),
        key = key,
        title = title,
        value = value,
        provenance = provenance,
        confidence = confidence,
        status = runCatching { UicStatus.valueOf(status) }.getOrDefault(UicStatus.OBSERVED),
        privacyLevel = runCatching { PrivacyLevel.valueOf(privacyLevel) }.getOrDefault(PrivacyLevel.STANDARD),
        firstObservedAt = firstObservedAt,
        lastConfirmedAt = lastConfirmedAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(model: UicAttribute): UicAttributeEntity = UicAttributeEntity(
            id = model.id,
            category = model.category.name,
            key = model.key,
            title = model.title,
            value = model.value,
            provenance = model.provenance,
            confidence = model.confidence,
            status = model.status.name,
            privacyLevel = model.privacyLevel.name,
            firstObservedAt = model.firstObservedAt,
            lastConfirmedAt = model.lastConfirmedAt,
            updatedAt = model.updatedAt
        )
    }
}
