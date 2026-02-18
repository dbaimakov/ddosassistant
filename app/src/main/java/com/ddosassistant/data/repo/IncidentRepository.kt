\
package com.example.ddosassistant.data.repo

import android.content.Context
import android.net.Uri
import com.example.ddosassistant.data.db.*
import com.example.ddosassistant.data.network.ElkConnector
import com.example.ddosassistant.data.network.GraphConnector
import com.example.ddosassistant.data.settings.SettingsRepository
import com.example.ddosassistant.domain.*
import com.example.ddosassistant.util.newId
import com.example.ddosassistant.util.queryDisplayName
import com.example.ddosassistant.util.sha256Hex
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.io.ByteArrayOutputStream
import java.time.Instant

class IncidentRepository(
    private val context: Context,
    private val db: AppDatabase,
    private val settingsRepository: SettingsRepository,
    private val graph: GraphConnector,
    private val elk: ElkConnector,
    private val gson: Gson = Gson()
) {
    private val incidentDao = db.incidentDao()
    private val waveDao = db.attackWaveDao()
    private val mitigationDao = db.mitigationDao()
    private val evidenceDao = db.evidenceDao()
    private val changeLogDao = db.changeLogDao()
    private val commDao = db.communicationDao()

    fun observeIncidents(): Flow<List<IncidentEntity>> = incidentDao.observeAll()
    fun observeIncident(id: String): Flow<IncidentEntity?> = incidentDao.observeById(id)
    fun observeWaves(id: String): Flow<List<AttackWaveEntity>> = waveDao.observeByIncident(id)
    fun observeMitigations(id: String): Flow<List<MitigationEntity>> = mitigationDao.observeByIncident(id)
    fun observeEvidence(id: String): Flow<List<EvidenceEntity>> = evidenceDao.observeByIncident(id)
    fun observeChangeLog(id: String): Flow<List<ChangeLogEntity>> = changeLogDao.observeByIncident(id)
    fun observeComms(id: String): Flow<List<CommunicationEntity>> = commDao.observeByIncident(id)

    suspend fun createIncident(
        title: String,
        affectedService: String,
        description: String,
        severity: Severity = Severity.HIGH
    ): String {
        val id = newId()
        val now = Instant.now().toEpochMilli()
        val incident = IncidentEntity(
            incidentId = id,
            title = title.trim(),
            affectedService = affectedService.trim(),
            description = description.trim(),
            severity = severity.name,
            status = IncidentStatus.DETECTED.name,
            startTimeEpochMs = now,
            endTimeEpochMs = null
        )
        incidentDao.upsert(incident)
        logChange(id, actionId = null, what = "Incident created", why = "Initial detection/triage started")
        return id
    }

    suspend fun updateIncidentStatus(incidentId: String, newStatus: IncidentStatus) {
        val existing = incidentDao.getById(incidentId) ?: return
        val updated = existing.copy(
            status = newStatus.name,
            endTimeEpochMs = if (newStatus == IncidentStatus.CLOSED) Instant.now().toEpochMilli() else existing.endTimeEpochMs
        )
        incidentDao.upsert(updated)
        logChange(
            incidentId = incidentId,
            actionId = null,
            what = "Incident status updated to ${newStatus.name}",
            why = "Operational update"
        )
    }

    suspend fun addWave(
        incidentId: String,
        peakRps: Long,
        topEndpoint: String,
        notes: String
    ) {
        val wave = AttackWaveEntity(
            waveId = newId(),
            incidentId = incidentId,
            startTimeEpochMs = Instant.now().toEpochMilli(),
            endTimeEpochMs = null,
            peakRps = peakRps,
            topEndpoint = topEndpoint.trim(),
            notes = notes.trim()
        )
        waveDao.upsert(wave)
        logChange(incidentId, actionId = null, what = "Attack wave recorded", why = "Track peak and targeting")
    }

    suspend fun addMitigation(
        incidentId: String,
        type: MitigationType,
        description: String,
        status: ActionStatus,
        rationale: String,
        waveId: String? = null,
        implementedBy: String
    ) {
        val actionId = newId()
        val now = Instant.now().toEpochMilli()
        val entity = MitigationEntity(
            actionId = actionId,
            incidentId = incidentId,
            waveId = waveId,
            type = type.name,
            description = description.trim(),
            status = status.name,
            implementedAtEpochMs = if (status == ActionStatus.IMPLEMENTED) now else null,
            implementedBy = implementedBy.trim(),
            rationale = rationale.trim()
        )
        mitigationDao.upsert(entity)
        logChange(incidentId, actionId, what = "Mitigation added: ${type.name}", why = rationale.trim())
    }

    suspend fun addManualEvidenceReference(
        incidentId: String,
        type: EvidenceType,
        localUri: String,
        collectedBy: String,
        remoteWebUrl: String? = null,
        sha256: String? = null,
        waveId: String? = null
    ) {
        val entity = EvidenceEntity(
            artifactId = newId(),
            incidentId = incidentId,
            waveId = waveId,
            type = type.name,
            collectedAtEpochMs = Instant.now().toEpochMilli(),
            collectedBy = collectedBy.trim(),
            localUri = localUri,
            remoteWebUrl = remoteWebUrl,
            sha256 = sha256
        )
        evidenceDao.upsert(entity)
        logChange(incidentId, null, what = "Evidence added: ${type.name}", why = "Preserve artifacts for SSC/vendor")
    }

    suspend fun uploadEvidenceToSharePoint(
        incidentId: String,
        evidenceType: EvidenceType,
        uri: Uri,
        collectedBy: String
    ): EvidenceEntity {
        val settings = settingsRepository.settingsValue()

        require(settings.graphToken.isNotBlank()) { "Graph token not set. Configure it in Settings." }
        require(settings.sharePointDriveId.isNotBlank()) { "SharePoint driveId not set. Configure it in Settings." }

        val displayName = queryDisplayName(context.contentResolver, uri) ?: "evidence.bin"
        val bytes = readAllBytes(uri)
        val hash = sha256Hex(bytes)

        // Ensure the incident folder exists (create once per incident).
        // NOTE: This assumes settings.sharePointBaseFolderPath already exists in the drive.
        graph.createIncidentFolder(
            bearerToken = settings.graphToken,
            driveId = settings.sharePointDriveId,
            baseFolderPath = settings.sharePointBaseFolderPath,
            incidentId = incidentId
        )

        val incidentFolderPath = "${settings.sharePointBaseFolderPath.trim('/')}/Incident-$incidentId"
        val itemPath = "$incidentFolderPath/$displayName"

        val driveItem = graph.uploadBytes(
            bearerToken = settings.graphToken,
            driveId = settings.sharePointDriveId,
            itemPath = itemPath,
            bytes = bytes,
            contentType = guessContentType(displayName)
        )

        val entity = EvidenceEntity(
            artifactId = newId(),
            incidentId = incidentId,
            waveId = null,
            type = evidenceType.name,
            collectedAtEpochMs = Instant.now().toEpochMilli(),
            collectedBy = collectedBy.trim(),
            localUri = uri.toString(),
            remoteWebUrl = driveItem.webUrl,
            sha256 = hash
        )
        evidenceDao.upsert(entity)

        logChange(incidentId, null, what = "Evidence uploaded to SharePoint: ${evidenceType.name}", why = "Share artifacts with SSC/vendor")
        return entity
    }

    suspend fun syncIncidentMetadataToSharePoint(incidentId: String): String {
        val settings = settingsRepository.settingsValue()

        require(settings.graphToken.isNotBlank()) { "Graph token not set. Configure it in Settings." }
        require(settings.sharePointDriveId.isNotBlank()) { "SharePoint driveId not set. Configure it in Settings." }

        val incident = incidentDao.getById(incidentId) ?: error("Incident not found")
        val waves = waveDao.listByIncident(incidentId)
        val mitigations = mitigationDao.listByIncident(incidentId)
        val evidence = evidenceDao.listByIncident(incidentId)
        val changes = changeLogDao.listByIncident(incidentId)
        val comms = commDao.listByIncident(incidentId)

        graph.createIncidentFolder(
            bearerToken = settings.graphToken,
            driveId = settings.sharePointDriveId,
            baseFolderPath = settings.sharePointBaseFolderPath,
            incidentId = incidentId
        )

        val export = IncidentExport(
            incident = incident,
            waves = waves,
            mitigations = mitigations,
            evidence = evidence,
            changeLog = changes,
            communications = comms
        )

        val jsonBytes = gson.toJson(export).toByteArray(Charsets.UTF_8)
        val incidentFolderPath = "${settings.sharePointBaseFolderPath.trim('/')}/Incident-$incidentId"
        val itemPath = "$incidentFolderPath/incident-metadata.json"

        val driveItem = graph.uploadBytes(
            bearerToken = settings.graphToken,
            driveId = settings.sharePointDriveId,
            itemPath = itemPath,
            bytes = jsonBytes,
            contentType = "application/json"
        )

        logChange(incidentId, null, what = "Synced incident metadata to SharePoint", why = "Maintain a single source of truth")
        return driveItem.webUrl ?: "Uploaded (no webUrl returned)"
    }

    suspend fun postTeamsUpdate(incidentId: String, messageHtml: String): String {
        val settings = settingsRepository.settingsValue()
        require(settings.graphToken.isNotBlank()) { "Graph token not set. Configure it in Settings." }
        require(settings.teamsTeamId.isNotBlank()) { "Teams teamId not set. Configure it in Settings." }
        require(settings.teamsChannelId.isNotBlank()) { "Teams channelId not set. Configure it in Settings." }

        val resp = graph.postTeamsMessage(
            bearerToken = settings.graphToken,
            teamId = settings.teamsTeamId,
            channelId = settings.teamsChannelId,
            contentHtml = messageHtml
        )

        val comm = CommunicationEntity(
            commId = newId(),
            incidentId = incidentId,
            channel = CommChannel.TEAMS.name,
            sentAtEpochMs = Instant.now().toEpochMilli(),
            sentBy = settings.defaultActorName,
            subject = null,
            body = messageHtml,
            remoteLink = resp.id
        )
        commDao.upsert(comm)
        logChange(incidentId, null, what = "Teams update posted", why = "Cross-team coordination")
        return resp.id ?: "Posted (no message id returned)"
    }

    suspend fun createKibanaThresholdAlertForIncident(
        incidentId: String,
        ruleName: String,
        index: String,
        queryString: String,
        threshold: Double
    ): String {
        val settings = settingsRepository.settingsValue()
        require(settings.kibanaBaseUrl.isNotBlank()) { "Kibana base URL not set. Configure it in Settings." }
        require(settings.kibanaApiKey.isNotBlank()) { "Kibana API key not set. Configure it in Settings." }

        val respBody = elk.createEsQueryThresholdRule(
            kibanaBaseUrl = settings.kibanaBaseUrl,
            apiKey = settings.kibanaApiKey,
            ruleName = ruleName,
            index = index,
            queryString = queryString,
            threshold = threshold
        )

        logChange(incidentId, null, what = "Created Kibana threshold rule", why = "Automate wave detection / alerts")
        return respBody
    }

    suspend fun logChange(incidentId: String, actionId: String?, what: String, why: String, beforeRef: String? = null, afterRef: String? = null) {
        val settings = settingsRepository.settingsValue()
        val entry = ChangeLogEntity(
            changeId = newId(),
            incidentId = incidentId,
            actionId = actionId,
            timestampEpochMs = Instant.now().toEpochMilli(),
            who = settings.defaultActorName,
            what = what.trim(),
            why = why.trim(),
            beforeRef = beforeRef,
            afterRef = afterRef
        )
        changeLogDao.upsert(entry)
    }

    fun buildDefaultIncidentSummary(incident: IncidentEntity): String {
        val status = incident.status
        val sev = incident.severity
        val service = incident.affectedService
        return """
            <b>Incident Update</b><br/>
            <b>ID:</b> ${incident.incidentId}<br/>
            <b>Service:</b> $service<br/>
            <b>Severity:</b> $sev<br/>
            <b>Status:</b> $status<br/>
            <b>Title:</b> ${incident.title}<br/>
            <br/>
            ${incident.description}
        """.trimIndent()
    }

    private fun readAllBytes(uri: Uri): ByteArray {
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Unable to open URI: $uri" }
            val buffer = ByteArray(64 * 1024)
            val out = ByteArrayOutputStream()
            while (true) {
                val n = input.read(buffer)
                if (n <= 0) break
                out.write(buffer, 0, n)
            }
            return out.toByteArray()
        }
    }

    private fun guessContentType(fileName: String): String {
        val lower = fileName.lowercase()
        return when {
            lower.endsWith(".txt") -> "text/plain"
            lower.endsWith(".csv") -> "text/csv"
            lower.endsWith(".json") -> "application/json"
            lower.endsWith(".zip") -> "application/zip"
            lower.endsWith(".pcap") || lower.endsWith(".pcapng") -> "application/octet-stream"
            else -> "application/octet-stream"
        }
    }
}

data class IncidentExport(
    val incident: IncidentEntity,
    val waves: List<AttackWaveEntity>,
    val mitigations: List<MitigationEntity>,
    val evidence: List<EvidenceEntity>,
    val changeLog: List<ChangeLogEntity>,
    val communications: List<CommunicationEntity>
)

// Convenience extension: get latest settings once (blocking the first emission).
suspend fun SettingsRepository.settingsValue(): com.example.ddosassistant.data.settings.AppSettings {
    return this.settings.first()
}