package com.ddosassistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ddosassistant.ui.screens.viewmodel.IncidentDetailViewModel
import com.ddosassistant.ui.screens.viewmodel.IncidentsViewModel
import com.ddosassistant.ui.screens.viewmodel.SettingsViewModel

class ViewModelFactory : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repo = AppServices.incidentRepository

        return when {
            modelClass.isAssignableFrom(IncidentsViewModel::class.java) ->
                IncidentsViewModel(repo) as T

            modelClass.isAssignableFrom(IncidentDetailViewModel::class.java) ->
                IncidentDetailViewModel(repo) as T

            modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                SettingsViewModel(AppServices.settingsRepository) as T

            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
