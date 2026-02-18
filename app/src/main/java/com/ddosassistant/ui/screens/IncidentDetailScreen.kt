package com.ddosassistant.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ddosassistant.ui.screens.viewmodel.IncidentDetailViewModel

@Composable
fun IncidentDetailScreen(
    viewModel: IncidentDetailViewModel,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val incident by viewModel.incident.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(onClick = onBack) { Text("Back") }
        Button(onClick = onOpenSettings) { Text("Settings") }
        Text(incident?.title ?: "Incident not found")
        Text(incident?.description.orEmpty())
    }
}
