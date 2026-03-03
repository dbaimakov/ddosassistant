package com.ddosassistant.ui.screens.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ddosassistant.data.db.IncidentEntity
import com.ddosassistant.data.repo.IncidentRepository
import com.ddosassistant.domain.IncidentCategory
import com.ddosassistant.domain.Severity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CreateIncidentUiState(
    val title: String = "",
    val category: IncidentCategory = IncidentCategory.DDOS,
    val affectedService: String = "",
    val description: String = "",
    val additionalInfo: String = "",
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val attachments: List<Uri> = emptyList(),
    val emailRecipients: String = ""
)

class IncidentsViewModel(
    private val repository: IncidentRepository
) : ViewModel() {

    val incidents: StateFlow<List<IncidentEntity>> = repository.observeIncidents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _createState = MutableStateFlow(CreateIncidentUiState())
    val createState = _createState.asStateFlow()

    fun updateCreateState(update: (CreateIncidentUiState) -> CreateIncidentUiState) {
        _createState.value = update(_createState.value)
    }

    fun addAttachment(uri: Uri) {
        updateCreateState { it.copy(attachments = (it.attachments + uri).distinct()) }
    }

    fun removeAttachment(uri: Uri) {
        updateCreateState { it.copy(attachments = it.attachments - uri) }
    }

    fun createIncident() {
        val draft = _createState.value
        if (draft.title.isBlank() || draft.description.isBlank()) return

        viewModelScope.launch {
            val incidentId = repository.createIncident(
                title = draft.title,
                category = draft.category,
                affectedService = draft.affectedService,
                description = draft.description,
                additionalInfo = draft.additionalInfo,
                createdAtEpochMs = draft.createdAtEpochMs,
                severity = Severity.HIGH,
                attachmentUris = draft.attachments
            )

            val recipients = draft.emailRecipients
                .split(',', ';')
                .map { it.trim() }
                .filter { it.isNotBlank() }

            if (recipients.isNotEmpty()) {
                repository.sendIncidentEmail(incidentId, recipients)
            }

            _createState.value = CreateIncidentUiState()
        }
    }
}
