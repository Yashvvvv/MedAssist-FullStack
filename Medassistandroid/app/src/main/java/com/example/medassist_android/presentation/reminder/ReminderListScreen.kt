package com.example.medassist_android.presentation.reminder

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.medassist_android.data.local.entity.MedicationReminderEntity
import com.example.medassist_android.data.local.entity.ReminderFrequency
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddReminder: () -> Unit,
    onNavigateToEditReminder: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: ReminderViewModel = hiltViewModel()
    val uiState by viewModel.reminderListState.collectAsStateWithLifecycle()
    val upcomingState by viewModel.upcomingAlarmsState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("Medication Reminders") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = onNavigateToAddReminder) {
                    Icon(Icons.Default.Add, contentDescription = "Add Reminder")
                }
            }
        )

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.reminders.isEmpty()) {
            EmptyRemindersView(onAddReminder = onNavigateToAddReminder)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Upcoming alarms section
                if (upcomingState.alarms.isNotEmpty()) {
                    item {
                        Text(
                            text = "Upcoming",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    
                    items(upcomingState.alarms.take(3)) { alarm ->
                        val reminder = upcomingState.reminderMap[alarm.reminderId]
                        if (reminder != null) {
                            UpcomingAlarmCard(
                                alarm = alarm,
                                reminder = reminder,
                                onTake = { viewModel.markAlarmTaken(alarm.id, alarm.reminderId) },
                                onSkip = { viewModel.markAlarmSkipped(alarm.id, alarm.reminderId) }
                            )
                        }
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "All Reminders",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }

                items(uiState.reminders) { reminder ->
                    ReminderCard(
                        reminder = reminder,
                        onEdit = { onNavigateToEditReminder(reminder.id) },
                        onDelete = { viewModel.deleteReminder(reminder.id) },
                        onToggleActive = { viewModel.toggleReminderActive(reminder.id, !reminder.isActive) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyRemindersView(
    onAddReminder: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Alarm,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "No Reminders Yet",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Set up medication reminders to never miss a dose",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(onClick = onAddReminder) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Reminder")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpcomingAlarmCard(
    alarm: com.example.medassist_android.data.local.entity.ReminderAlarmEntity,
    reminder: MedicationReminderEntity,
    onTake: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val scheduledTime = timeFormat.format(Date(alarm.scheduledTime))
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = reminder.medicineName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "${reminder.dosage} • $scheduledTime",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onSkip,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Text("Skip")
                    }
                    Button(onClick = onTake) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Take")
                    }
                }
            }
            
            reminder.instructions?.let { instructions ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = instructions,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderCard(
    reminder: MedicationReminderEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleActive: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Reminder") },
            text = { Text("Are you sure you want to delete this reminder for ${reminder.medicineName}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (reminder.isActive) 
                MaterialTheme.colorScheme.surface 
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reminder.medicineName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = reminder.dosage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Switch(
                    checked = reminder.isActive,
                    onCheckedChange = { onToggleActive() }
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Frequency and times
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = formatFrequency(reminder.frequency, reminder.times, reminder.daysOfWeek),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            reminder.instructions?.let { instructions ->
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = instructions,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
                }
                
                TextButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit")
                }
            }
        }
    }
}

private fun formatFrequency(
    frequency: ReminderFrequency,
    times: String,
    daysOfWeek: String?
): String {
    val timesList = times.split(",").filter { it.isNotBlank() }
    val timesStr = timesList.joinToString(", ")
    
    return when (frequency) {
        ReminderFrequency.ONCE -> "Once at $timesStr"
        ReminderFrequency.DAILY -> "Daily at $timesStr"
        ReminderFrequency.WEEKLY -> {
            val days = daysOfWeek?.split(",")?.mapNotNull { it.toIntOrNull() }
                ?.map { dayName(it) }?.joinToString(", ") ?: "Selected days"
            "$days at $timesStr"
        }
        ReminderFrequency.EVERY_X_HOURS -> "Every few hours"
        ReminderFrequency.EVERY_X_DAYS -> "Every few days"
        ReminderFrequency.AS_NEEDED -> "As needed"
    }
}

private fun dayName(day: Int): String {
    return when (day) {
        1 -> "Mon"
        2 -> "Tue"
        3 -> "Wed"
        4 -> "Thu"
        5 -> "Fri"
        6 -> "Sat"
        7 -> "Sun"
        else -> ""
    }
}
