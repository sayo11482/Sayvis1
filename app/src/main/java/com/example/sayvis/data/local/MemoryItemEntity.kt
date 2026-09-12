package com.example.sayvis.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.sayvis.model.EpistemicStatus
import com.example.sayvis.model.MemoryItem
import com.example.sayvis.model.MemoryType
import com.example.sayvis.model.RetentionPolicy

@Entity(tableName = "memory_items")
data class MemoryItemEntity(
    @PrimaryKey val id: String,
    val type: String,
    val content: String,
    val source: String,
    val confidence: Float,
    val provenance: String,
    val importance: Int,
    val retentionPolicy: String,
    val epistemicStatus: String,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomain(): MemoryItem = MemoryItem(
        id = id,
        type = runCatching { MemoryType.valueOf(type) }.getOrDefault(MemoryType.LONG_TERM_MEMORY),
        content = content,
        source = source,
        confidence = confidence,
        provenance = provenance,
        importance = importance,
        retentionPolicy = runCatching { RetentionPolicy.valueOf(retentionPolicy) }.getOrDefault(RetentionPolicy.PERSISTENT),
        epistemicStatus = runCatching { EpistemicStatus.valueOf(epistemicStatus) }.getOrDefault(EpistemicStatus.CONFIRMED),
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(model: MemoryItem): MemoryItemEntity = MemoryItemEntity(
            id = model.id,
            type = model.type.name,
            content = model.content,
            source = model.source,
            confidence = model.confidence,
            provenance = model.provenance,
            importance = model.importance,
            retentionPolicy = model.retentionPolicy.name,
            epistemicStatus = model.epistemicStatus.name,
            createdAt = model.createdAt,
            updatedAt = model.updatedAt
        )
    }
}
