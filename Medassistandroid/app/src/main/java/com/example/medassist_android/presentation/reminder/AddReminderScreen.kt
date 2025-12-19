package com.example.medassist_android.presentation.reminder

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.medassist_android.data.local.entity.ReminderFrequency
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReminderScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: ReminderViewModel = hiltViewModel()
    val formState by viewModel.reminderFormState.collectAsStateWithLifecycle()
    
    var showTimePicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var datePickerType by remember { mutableStateOf("start") }

    // Handle successful save
    LaunchedEffect(formState.isSaved) {
        if (formState.isSaved) {
            onNavigateBack()
        }
    }

    // Reset form on mount
    LaunchedEffect(Unit) {
        viewModel.resetForm()
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("Add Reminder") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel")
                }
            },
            actions = {
                TextButton(
                    onClick = { viewModel.saveReminder() },
                    enabled = !formState.isLoading
                ) {
                    if (formState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Save")
                    }
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Error message
            formState.error?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Medicine Name
            OutlinedTextField(
                value = formState.medicineName,
                onValueChange = { viewModel.updateFormField("medicineName", it) },
                label = { Text("Medicine Name *") },
                placeholder = { Text("e.g., Aspirin, Vitamin D") },
                leadingIcon = { Icon(Icons.Default.Medication, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Dosage
            OutlinedTextField(
                value = formState.dosage,
                onValueChange = { viewModel.updateFormField("dosage", it) },
                label = { Text("Dosage *") },
                placeholder = { Text("e.g., 1 tablet, 10ml, 2 capsules") },
                leadingIcon = { Icon(Icons.Default.Scale, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Frequency
            Card {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Frequency",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    FrequencySelector(
                        selectedFrequency = formState.frequency,
                        onFrequencySelected = { viewModel.updateFormField("frequency", it) }
                    )
                }
            }

            // Times
            Card {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Reminder Times",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        FilledTonalIconButton(
                            onClick = { showTimePicker = true }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add time")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (formState.times.isEmpty()) {
                        Text(
                            text = "No times added. Tap + to add reminder times.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(formState.times) { time ->
                                InputChip(
                                    selected = false,
                                    onClick = { },
                                    label = { Text(time) },
                                    trailingIcon = {
                                        IconButton(
                                            onClick = { viewModel.removeTime(time) },
                                            modifier = Modifier.size(18.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Remove",
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Days of Week (for weekly frequency)
            if (formState.frequency == ReminderFrequency.WEEKLY) {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Days of Week",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            listOf(
                                1 to "M",
                                2 to "T",
                                3 to "W",
                                4 to "T",
                                5 to "F",
                                6 to "S",
                                7 to "S"
                            ).forEach { (day, label) ->
                                FilterChip(
                                    selected = formState.daysOfWeek.contains(day),
                                    onClick = { viewModel.toggleDayOfWeek(day) },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }
            }

            // Start Date
            Card(
                onClick = {
                    datePickerType = "start"
                    showDatePicker = true
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Start Date",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = formatDate(formState.startDate),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.CalendarToday, contentDescription = null)
                }
            }

            // End Date (optional)
            Card(
                onClick = {
                    datePickerType = "end"
                    showDatePicker = true
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "End Date (Optional)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = formState.endDate?.let { formatDate(it) } ?: "No end date",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.CalendarToday, contentDescription = null)
                }
            }

            // Instructions
            OutlinedTextField(
                value = formState.instructions,
                onValueChange = { viewModel.updateFormField("instructions", it) },
                label = { Text("Instructions (Optional)") },
                placeholder = { Text("e.g., Take with food, Take before bed") },
                leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Time Picker Dialog
    if (showTimePicker) {
        TimePickerDialog(
            onDismiss = { showTimePicker = false },
            onConfirm = { hour, minute ->
                val timeStr = String.format("%02d:%02d", hour, minute)
                viewModel.addTime(timeStr)
                showTimePicker = false
            }
        )
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = if (datePickerType == "start") 
                formState.startDate 
            else 
                formState.endDate ?: System.currentTimeMillis()
        )
        
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { date ->
                            viewModel.updateFormField(
                                if (datePickerType == "start") "startDate" else "endDate",
                                date
                            )
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun FrequencySelector(
    selectedFrequency: ReminderFrequency,
    onFrequencySelected: (ReminderFrequency) -> Unit,
    modifier: Modifier = Modifier
) {
    val frequencies = listOf(
        ReminderFrequency.DAILY to "Daily",
        ReminderFrequency.WEEKLY to "Weekly",
        ReminderFrequency.ONCE to "Once",
        ReminderFrequency.AS_NEEDED to "As Needed"
    )
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        frequencies.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { (frequency, label) ->
                    FilterChip(
                        selected = selectedFrequency == frequency,
                        onClick = { onFrequencySelected(frequency) },
                        label = { Text(label) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // Fill remaining space if odd number
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = 9,
        initialMinute = 0
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Time") },
        text = {
            TimePicker(state = timePickerState)
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(timePickerState.hour, timePickerState.minute) }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
