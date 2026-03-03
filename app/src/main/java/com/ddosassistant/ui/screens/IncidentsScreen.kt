package com.ddosassistant.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ddosassistant.domain.IncidentCategory
import com.ddosassistant.ui.screens.viewmodel.IncidentsViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun IncidentsScreen(
    viewModel: IncidentsViewModel,
    onOpenIncident: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    val incidents by viewModel.incidents.collectAsStateWithLifecycle()
    val createState by viewModel.createState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    var categoryExpanded by remember { mutableStateOf(false) }

    val attachmentPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            viewModel.addAttachment(uri)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onOpenSettings) { Text("Settings") }
        }

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = createState.title,
            onValueChange = { value -> viewModel.updateCreateState { state -> state.copy(title = value) } },
            label = { Text("Incident title") }
        )

        BoxedCategoryPicker(
            selected = createState.category,
            expanded = categoryExpanded,
            onExpand = { categoryExpanded = true },
            onDismiss = { categoryExpanded = false },
            onSelect = {
                viewModel.updateCreateState { state -> state.copy(category = it) }
                categoryExpanded = false
            }
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = createState.affectedService,
            onValueChange = { value -> viewModel.updateCreateState { state -> state.copy(affectedService = value) } },
            label = { Text("Affected service") }
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = createState.description,
            onValueChange = { value -> viewModel.updateCreateState { state -> state.copy(description = value) } },
            label = { Text("Description") }
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = createState.additionalInfo,
            onValueChange = { value -> viewModel.updateCreateState { state -> state.copy(additionalInfo = value) } },
            label = { Text("Additional information") }
        )

        Button(onClick = {
            val cal = Calendar.getInstance().apply { timeInMillis = createState.createdAtEpochMs }
            DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    val afterDate = Calendar.getInstance().apply {
                        timeInMillis = createState.createdAtEpochMs
                        set(year, month, dayOfMonth)
                    }
                    TimePickerDialog(
                        context,
                        { _, hour, minute ->
                            afterDate.set(Calendar.HOUR_OF_DAY, hour)
                            afterDate.set(Calendar.MINUTE, minute)
                            viewModel.updateCreateState { it.copy(createdAtEpochMs = afterDate.timeInMillis) }
                        },
                        afterDate.get(Calendar.HOUR_OF_DAY),
                        afterDate.get(Calendar.MINUTE),
                        true
                    ).show()
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }) {
            Text("Created time: ${dateFormat.format(Date(createState.createdAtEpochMs))}")
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { attachmentPicker.launch(arrayOf("*/*")) }) { Text("Add attachment") }
            Text("${createState.attachments.size} attached", style = MaterialTheme.typography.bodyMedium)
        }

        createState.attachments.forEach { uri ->
            Text(
                text = uri.toString(),
                modifier = Modifier.clickable { viewModel.removeAttachment(uri) },
                style = MaterialTheme.typography.bodySmall
            )
        }

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = createState.emailRecipients,
            onValueChange = { value -> viewModel.updateCreateState { state -> state.copy(emailRecipients = value) } },
            label = { Text("Email recipients (comma separated)") }
        )

        Button(onClick = viewModel::createIncident) { Text("Create incident") }

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
                        Text("${incident.category} • ${incident.severity} • ${incident.status}")
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxedCategoryPicker(
    selected: IncidentCategory,
    expanded: Boolean,
    onExpand: () -> Unit,
    onDismiss: () -> Unit,
    onSelect: (IncidentCategory) -> Unit
) {
    Column {
        OutlinedTextField(
            value = selected.name,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth().clickable(onClick = onExpand),
            label = { Text("Incident type") }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
            IncidentCategory.entries.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name.replace('_', ' ')) },
                    onClick = { onSelect(category) }
                )
            }
        }
    }
}
