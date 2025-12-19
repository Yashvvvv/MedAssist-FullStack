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
import com.example.medassist_android.data.local.entity.IntakeStatus
import com.example.medassist_android.data.local.entity.MedicineIntakeLogEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicineHistoryScreen(
    onNavigateBack: () -> Unit,
    onLogManualIntake: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: ReminderViewModel = hiltViewModel()
    val intakeState by viewModel.intakeLogState.collectAsStateWithLifecycle()
    
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Today", "All History")

    LaunchedEffect(Unit) {
        viewModel.loadAllLogs()
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("Medicine History") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = onLogManualIntake) {
                    Icon(Icons.Default.Add, contentDescription = "Log Intake")
                }
            }
        )

        // Stats Card
        intakeState.stats?.let { stats ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "This Week",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(
                            value = "${stats.adherenceRate.toInt()}%",
                            label = "Adherence",
                            color = MaterialTheme.colorScheme.primary
                        )
                        StatItem(
                            value = "${stats.taken}",
                            label = "Taken",
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        StatItem(
                            value = "${stats.missed}",
                            label = "Missed",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    LinearProgressIndicator(
                        progress = { stats.adherenceRate / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        color = when {
                            stats.adherenceRate >= 80 -> MaterialTheme.colorScheme.primary
                            stats.adherenceRate >= 50 -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.error
                        }
                    )
                }
            }
        }

        // Tabs
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        // Content
        val displayLogs = when (selectedTab) {
            0 -> intakeState.todayLogs
            else -> intakeState.logs
        }

        if (intakeState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (displayLogs.isEmpty()) {
            EmptyHistoryView(
                message = if (selectedTab == 0) "No medicine intake logged today" else "No history yet",
                onLogIntake = onLogManualIntake
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Group logs by date for "All History"
                if (selectedTab == 1) {
                    val groupedLogs = displayLogs.groupBy { log ->
                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            .format(Date(log.intakeTime))
                    }
                    
                    groupedLogs.forEach { (date, logs) ->
                        item {
                            Text(
                                text = formatDateHeader(date),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        
                        items(logs) { log ->
                            IntakeLogCard(log = log)
                        }
                    }
                } else {
                    items(displayLogs) { log ->
                        IntakeLogCard(log = log)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun EmptyHistoryView(
    message: String,
    onLogIntake: () -> Unit,
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
            imageVector = Icons.Default.History,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(onClick = onLogIntake) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Log Intake")
        }
    }
}

@Composable
private fun IntakeLogCard(
    log: MedicineIntakeLogEntity,
    modifier: Modifier = Modifier
) {
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (log.status) {
                IntakeStatus.TAKEN -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                IntakeStatus.SKIPPED -> MaterialTheme.colorScheme.surfaceVariant
                IntakeStatus.MISSED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                IntakeStatus.LATE -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (log.status) {
                        IntakeStatus.TAKEN -> Icons.Default.CheckCircle
                        IntakeStatus.SKIPPED -> Icons.Default.Cancel
                        IntakeStatus.MISSED -> Icons.Default.Error
                        IntakeStatus.LATE -> Icons.Default.Schedule
                    },
                    contentDescription = null,
                    tint = when (log.status) {
                        IntakeStatus.TAKEN -> MaterialTheme.colorScheme.tertiary
                        IntakeStatus.SKIPPED -> MaterialTheme.colorScheme.onSurfaceVariant
                        IntakeStatus.MISSED -> MaterialTheme.colorScheme.error
                        IntakeStatus.LATE -> MaterialTheme.colorScheme.secondary
                    },
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = log.medicineName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = log.dosage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    log.notes?.let { notes ->
                        Text(
                            text = notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = timeFormat.format(Date(log.intakeTime)),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = log.status.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = when (log.status) {
                        IntakeStatus.TAKEN -> MaterialTheme.colorScheme.tertiary
                        IntakeStatus.SKIPPED -> MaterialTheme.colorScheme.onSurfaceVariant
                        IntakeStatus.MISSED -> MaterialTheme.colorScheme.error
                        IntakeStatus.LATE -> MaterialTheme.colorScheme.secondary
                    }
                )
                
                // Mood indicator
                log.mood?.let { mood ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = moodEmoji(mood),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

private fun formatDateHeader(dateStr: String): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val date = sdf.parse(dateStr) ?: return dateStr
    
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val dateCalendar = Calendar.getInstance().apply { time = date }
    
    return when {
        isSameDay(dateCalendar, today) -> "Today"
        isSameDay(dateCalendar, yesterday) -> "Yesterday"
        else -> SimpleDateFormat("EEEE, MMM dd", Locale.getDefault()).format(date)
    }
}

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

private fun moodEmoji(mood: Int): String {
    return when (mood) {
        1 -> "😞"
        2 -> "😕"
        3 -> "😐"
        4 -> "🙂"
        5 -> "😊"
        else -> ""
    }
}
