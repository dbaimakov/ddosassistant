package com.ddosassistant.ui.screens.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ddosassistant.data.db.IncidentEntity
import com.ddosassistant.data.repo.IncidentRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class IncidentsViewModel(
    private val repository: IncidentRepository
) : ViewModel() {

    val incidents: StateFlow<List<IncidentEntity>> = repository.observeIncidents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun createSampleIncident() {
        viewModelScope.launch {
            repository.createIncident(
                title = "Potential DDoS wave",
                affectedService = "Public API",
                description = "Auto-generated incident to kickstart triage."
            )
        }
    }
}
