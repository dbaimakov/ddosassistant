package com.ddosassistant.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface IncidentDao {
    @Query("SELECT * FROM incidents ORDER BY startTimeEpochMs DESC")
    fun observeAll(): Flow<List<IncidentEntity>>

    @Query("SELECT * FROM incidents WHERE incidentId = :id LIMIT 1")
    fun observeById(id: String): Flow<IncidentEntity?>

    @Query("SELECT * FROM incidents WHERE incidentId = :id LIMIT 1")
    suspend fun getById(id: String): IncidentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(incident: IncidentEntity)

    @Query("DELETE FROM incidents WHERE incidentId = :id")
    suspend fun delete(id: String)
}

@Dao
interface AttackWaveDao {
    @Query("SELECT * FROM attack_waves WHERE incidentId = :incidentId ORDER BY startTimeEpochMs DESC")
    fun observeByIncident(incidentId: String): Flow<List<AttackWaveEntity>>

    @Query("SELECT * FROM attack_waves WHERE incidentId = :incidentId ORDER BY startTimeEpochMs DESC")
    suspend fun listByIncident(incidentId: String): List<AttackWaveEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(wave: AttackWaveEntity)
}

@Dao
interface MitigationDao {
    @Query("SELECT * FROM mitigations WHERE incidentId = :incidentId ORDER BY implementedAtEpochMs DESC")
    fun observeByIncident(incidentId: String): Flow<List<MitigationEntity>>

    @Query("SELECT * FROM mitigations WHERE incidentId = :incidentId ORDER BY implementedAtEpochMs DESC")
    suspend fun listByIncident(incidentId: String): List<MitigationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(action: MitigationEntity)
}

@Dao
interface EvidenceDao {
    @Query("SELECT * FROM evidence WHERE incidentId = :incidentId ORDER BY collectedAtEpochMs DESC")
    fun observeByIncident(incidentId: String): Flow<List<EvidenceEntity>>

    @Query("SELECT * FROM evidence WHERE incidentId = :incidentId ORDER BY collectedAtEpochMs DESC")
    suspend fun listByIncident(incidentId: String): List<EvidenceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(artifact: EvidenceEntity)
}

@Dao
interface ChangeLogDao {
    @Query("SELECT * FROM change_log WHERE incidentId = :incidentId ORDER BY timestampEpochMs DESC")
    fun observeByIncident(incidentId: String): Flow<List<ChangeLogEntity>>

    @Query("SELECT * FROM change_log WHERE incidentId = :incidentId ORDER BY timestampEpochMs DESC")
    suspend fun listByIncident(incidentId: String): List<ChangeLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: ChangeLogEntity)
}

@Dao
interface CommunicationDao {
    @Query("SELECT * FROM communications WHERE incidentId = :incidentId ORDER BY sentAtEpochMs DESC")
    fun observeByIncident(incidentId: String): Flow<List<CommunicationEntity>>

    @Query("SELECT * FROM communications WHERE incidentId = :incidentId ORDER BY sentAtEpochMs DESC")
    suspend fun listByIncident(incidentId: String): List<CommunicationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(comm: CommunicationEntity)
}
