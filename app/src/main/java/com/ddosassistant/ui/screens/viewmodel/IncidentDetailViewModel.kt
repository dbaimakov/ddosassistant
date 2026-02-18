package com.ddosassistant.ui.screens.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ddosassistant.data.db.IncidentEntity
import com.ddosassistant.data.repo.IncidentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

class IncidentDetailViewModel(
    private val repository: IncidentRepository
) : ViewModel() {

    private val incidentId = MutableStateFlow<String?>(null)

    val incident: StateFlow<IncidentEntity?> = incidentId
        .filterNotNull()
        .flatMapLatest { repository.observeIncident(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun bind(id: String) {
        incidentId.value = id
    }
}
