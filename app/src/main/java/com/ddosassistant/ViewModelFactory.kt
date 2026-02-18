package com.example.ddosassistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.ddosassistant.ui.screens.viewmodel.IncidentDetailViewModel
import com.example.ddosassistant.ui.screens.viewmodel.IncidentsViewModel
import com.example.ddosassistant.ui.screens.viewmodel.SettingsViewModel

class ViewModelFactory : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repo = AppServices.incidentRepository
        val settings = AppServices.settingsRepository

        return when {
            modelClass.isAssignableFrom(IncidentsViewModel::class.java) ->
                IncidentsViewModel(repo) as T

            modelClass.isAssignableFrom(IncidentDetailViewModel::class.java) ->
                IncidentDetailViewModel(repo, settings) as T

            modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                SettingsViewModel(settings) as T

            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
