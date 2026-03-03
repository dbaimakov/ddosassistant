package com.ddosassistant.domain

import com.ddosassistant.data.db.IncidentEntity
import com.ddosassistant.data.network.GraphMessage
import com.ddosassistant.data.network.GraphRecipient
import com.ddosassistant.data.network.GraphRecipientEmailAddress
import com.ddosassistant.data.network.GraphSendMailBodyContent

object IncidentEmailComposer {
    fun compose(
        incident: IncidentEntity,
        toRecipients: List<String>,
        subject: String? = null
    ): GraphMessage {
        val cleanRecipients = toRecipients.map { it.trim() }.filter { it.isNotBlank() }
        require(cleanRecipients.isNotEmpty()) { "At least one valid recipient is required." }

        return GraphMessage(
            subject = subject?.trim().takeUnless { it.isNullOrBlank() }
                ?: "[${incident.severity}] Incident ${incident.incidentId}: ${incident.title}",
            body = GraphSendMailBodyContent(
                contentType = "Text",
                content = buildString {
                    appendLine("Incident ID: ${incident.incidentId}")
                    appendLine("Category: ${incident.category}")
                    appendLine("Service: ${incident.affectedService}")
                    appendLine("Severity: ${incident.severity}")
                    appendLine("Status: ${incident.status}")
                    appendLine("Created: ${incident.startTimeEpochMs}")
                    appendLine()
                    appendLine(incident.description)
                    if (incident.additionalInfo.isNotBlank()) {
                        appendLine()
                        appendLine("Additional information:")
                        appendLine(incident.additionalInfo)
                    }
                }.trim()
            ),
            toRecipients = cleanRecipients.map { email ->
                GraphRecipient(emailAddress = GraphRecipientEmailAddress(address = email))
            }
        )
    }
}
