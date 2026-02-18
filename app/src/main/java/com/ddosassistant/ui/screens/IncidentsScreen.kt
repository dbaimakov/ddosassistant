package com.ddosassistant.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ddosassistant.ui.screens.viewmodel.IncidentsViewModel

@Composable
fun IncidentsScreen(
    viewModel: IncidentsViewModel,
    onOpenIncident: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    val incidents by viewModel.incidents.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = viewModel::createSampleIncident) { Text("New sample") }
            Button(onClick = onOpenSettings) { Text("Settings") }
        }

        if (incidents.isEmpty()) {
            Text("No incidents yet.", style = MaterialTheme.typography.bodyLarge)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(incidents) { incident ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenIncident(incident.incidentId) }
                            .padding(8.dp)
                    ) {
                        Text(incident.title, style = MaterialTheme.typography.titleMedium)
                        Text("${incident.severity} • ${incident.status}")
                    }
                }
            }
        }
    }
}
