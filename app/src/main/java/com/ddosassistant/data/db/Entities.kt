package com.ddosassistant.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "incidents")
data class IncidentEntity(
    @PrimaryKey val incidentId: String,
    val title: String,
    val affectedService: String,
    val description: String,
    val severity: String,
    val status: String,
    val startTimeEpochMs: Long,
    val endTimeEpochMs: Long?
)

@Entity(tableName = "attack_waves")
data class AttackWaveEntity(
    @PrimaryKey val waveId: String,
    val incidentId: String,
    val startTimeEpochMs: Long,
    val endTimeEpochMs: Long?,
    val peakRps: Long,
    val topEndpoint: String,
    val notes: String
)

@Entity(tableName = "mitigations")
data class MitigationEntity(
    @PrimaryKey val actionId: String,
    val incidentId: String,
    val waveId: String?,
    val type: String,
    val description: String,
    val status: String,
    val implementedAtEpochMs: Long?,
    val implementedBy: String,
    val rationale: String
)

@Entity(tableName = "evidence")
data class EvidenceEntity(
    @PrimaryKey val artifactId: String,
    val incidentId: String,
    val waveId: String?,
    val type: String,
    val collectedAtEpochMs: Long,
    val collectedBy: String,
    val localUri: String,
    val remoteWebUrl: String?,
    val sha256: String?
)

@Entity(tableName = "change_log")
data class ChangeLogEntity(
    @PrimaryKey val changeId: String,
    val incidentId: String,
    val actionId: String?,
    val timestampEpochMs: Long,
    val who: String,
    val what: String,
    val why: String,
    val beforeRef: String?,
    val afterRef: String?
)

@Entity(tableName = "communications")
data class CommunicationEntity(
    @PrimaryKey val commId: String,
    val incidentId: String,
    val channel: String,
    val sentAtEpochMs: Long,
    val sentBy: String,
    val subject: String?,
    val body: String,
    val remoteLink: String?
)
