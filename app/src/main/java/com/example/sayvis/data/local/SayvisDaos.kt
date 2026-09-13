package com.example.sayvis.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UicDao {
    @Query("SELECT * FROM uic_attributes ORDER BY category ASC, updatedAt DESC")
    fun getAllAttributesFlow(): Flow<List<UicAttributeEntity>>

    @Query("SELECT * FROM uic_attributes WHERE id = :id")
    suspend fun getAttributeById(id: String): UicAttributeEntity?

    @Query("SELECT * FROM uic_attributes WHERE status = :status")
    fun getAttributesByStatus(status: String): Flow<List<UicAttributeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttribute(entity: UicAttributeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllAttributes(entities: List<UicAttributeEntity>)

    @Update
    suspend fun updateAttribute(entity: UicAttributeEntity)

    @Query("UPDATE uic_attributes SET status = :status, lastConfirmedAt = :timestamp, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, timestamp: Long)

    @Query("DELETE FROM uic_attributes WHERE id = :id")
    suspend fun deleteAttribute(id: String)
}

@Dao
interface AwareDao {
    @Query("SELECT * FROM aware_opportunities ORDER BY createdAt DESC")
    fun getAllOpportunitiesFlow(): Flow<List<AwareOpportunityEntity>>

    @Query("SELECT * FROM aware_opportunities WHERE status = 'PENDING' ORDER BY createdAt DESC")
    fun getPendingOpportunitiesFlow(): Flow<List<AwareOpportunityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOpportunity(entity: AwareOpportunityEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllOpportunities(entities: List<AwareOpportunityEntity>)

    @Update
    suspend fun updateOpportunity(entity: AwareOpportunityEntity)

    @Query("UPDATE aware_opportunities SET status = :status, executedAt = :executedAt WHERE id = :id")
    suspend fun updateOpportunityStatus(id: String, status: String, executedAt: Long?)
}

@Dao
interface MissionDao {
    @Query("SELECT * FROM missions ORDER BY createdAt DESC")
    fun getAllMissionsFlow(): Flow<List<MissionEntity>>

    @Query("SELECT * FROM missions WHERE id = :id")
    suspend fun getMissionById(id: String): MissionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMission(entity: MissionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllMissions(entities: List<MissionEntity>)

    @Update
    suspend fun updateMission(entity: MissionEntity)

    @Query("DELETE FROM missions WHERE id = :id")
    suspend fun deleteMission(id: String)
}

@Dao
interface AuditEventDao {
    @Query("SELECT * FROM audit_events ORDER BY timestamp DESC")
    fun getAllAuditEventsFlow(): Flow<List<AuditEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditEvent(entity: AuditEventEntity)

    @Query("DELETE FROM audit_events")
    suspend fun clearAudit()
}

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memory_items ORDER BY importance DESC, updatedAt DESC")
    fun getAllMemoriesFlow(): Flow<List<MemoryItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(entity: MemoryItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllMemories(entities: List<MemoryItemEntity>)

    @Query("DELETE FROM memory_items WHERE id = :id")
    suspend fun deleteMemory(id: String)

    @Query("DELETE FROM memory_items")
    suspend fun clearAll()
}

@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices ORDER BY isTrusted DESC, lastActiveAt DESC")
    fun getAllDevicesFlow(): Flow<List<DeviceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(entity: DeviceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllDevices(entities: List<DeviceEntity>)

    @Query("UPDATE devices SET isRevoked = :isRevoked, isTrusted = :isTrusted WHERE id = :id")
    suspend fun updateDeviceStatus(id: String, isRevoked: Boolean, isTrusted: Boolean)
}
