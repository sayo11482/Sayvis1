package com.example.sayvis.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.sayvis.model.Mission
import com.example.sayvis.model.MissionPriority
import com.example.sayvis.model.MissionStatus
import com.example.sayvis.model.MissionTask

@Entity(tableName = "missions")
data class MissionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val priority: String,
    val status: String,
    val progressPercent: Int,
    val deadline: String,
    val tasksSerialized: String, // format: id||title||isCompleted||isBlocked||blockerReason;;...
    val createdAt: Long
) {
    fun toDomain(): Mission {
        val taskList = if (tasksSerialized.isBlank()) emptyList() else {
            tasksSerialized.split(";;").mapNotNull { taskStr ->
                val parts = taskStr.split("||")
                if (parts.size >= 3) {
                    MissionTask(
                        id = parts[0],
                        title = parts[1],
                        isCompleted = parts[2].toBoolean(),
                        isBlocked = parts.getOrNull(3)?.toBoolean() ?: false,
                        blockerReason = parts.getOrNull(4)?.takeIf { it.isNotBlank() }
                    )
                } else null
            }
        }

        return Mission(
            id = id,
            title = title,
            description = description,
            priority = runCatching { MissionPriority.valueOf(priority) }.getOrDefault(MissionPriority.MEDIUM),
            status = runCatching { MissionStatus.valueOf(status) }.getOrDefault(MissionStatus.ACTIVE),
            progressPercent = progressPercent,
            deadline = deadline,
            tasks = taskList,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromDomain(model: Mission): MissionEntity {
            val serialized = model.tasks.joinToString(";;") { task ->
                "${task.id}||${task.title}||${task.isCompleted}||${task.isBlocked}||${task.blockerReason ?: ""}"
            }
            return MissionEntity(
                id = model.id,
                title = model.title,
                description = model.description,
                priority = model.priority.name,
                status = model.status.name,
                progressPercent = model.progressPercent,
                deadline = model.deadline,
                tasksSerialized = serialized,
                createdAt = model.createdAt
            )
        }
    }
}
