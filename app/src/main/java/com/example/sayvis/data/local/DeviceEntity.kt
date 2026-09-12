package com.example.sayvis.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.sayvis.model.Device
import com.example.sayvis.model.DeviceType

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val publicKeyFingerprint: String,
    val isTrusted: Boolean,
    val isRevoked: Boolean,
    val lastActiveAt: Long,
    val capabilitiesCsv: String
) {
    fun toDomain(): Device = Device(
        id = id,
        name = name,
        type = runCatching { DeviceType.valueOf(type) }.getOrDefault(DeviceType.ANDROID_PHONE),
        publicKeyFingerprint = publicKeyFingerprint,
        isTrusted = isTrusted,
        isRevoked = isRevoked,
        lastActiveAt = lastActiveAt,
        capabilities = if (capabilitiesCsv.isBlank()) emptyList() else capabilitiesCsv.split(",")
    )

    companion object {
        fun fromDomain(model: Device): DeviceEntity = DeviceEntity(
            id = model.id,
            name = model.name,
            type = model.type.name,
            publicKeyFingerprint = model.publicKeyFingerprint,
            isTrusted = model.isTrusted,
            isRevoked = model.isRevoked,
            lastActiveAt = model.lastActiveAt,
            capabilitiesCsv = model.capabilities.joinToString(",")
        )
    }
}
