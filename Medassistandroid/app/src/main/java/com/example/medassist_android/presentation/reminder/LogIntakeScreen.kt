package com.example.medassist_android.presentation.reminder

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogIntakeScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: ReminderViewModel = hiltViewModel()
    
    var medicineName by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedMood by remember { mutableStateOf<Int?>(null) }
    var sideEffects by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("Log Intake") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel")
                }
            },
            actions = {
                TextButton(
                    onClick = {
                        if (medicineName.isBlank()) {
                            error = "Medicine name is required"
                            return@TextButton
                        }
                        if (dosage.isBlank()) {
                            error = "Dosage is required"
                            return@TextButton
                        }
                        
                        isLoading = true
                        viewModel.logManualIntake(
                            medicineName = medicineName,
                            dosage = dosage,
                            notes = notes.ifBlank { null },
                            sideEffects = sideEffects.ifBlank { null },
                            mood = selectedMood
                        )
                        onNavigateBack()
                    },
                    enabled = !isLoading
                ) {
                    if (isLoading) {
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
            error?.let { errorMsg ->
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
                            text = errorMsg,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Medicine Name
            OutlinedTextField(
                value = medicineName,
                onValueChange = { 
                    medicineName = it
                    error = null
                },
                label = { Text("Medicine Name *") },
                placeholder = { Text("e.g., Aspirin, Vitamin D") },
                leadingIcon = { Icon(Icons.Default.Medication, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Dosage
            OutlinedTextField(
                value = dosage,
                onValueChange = { 
                    dosage = it
                    error = null
                },
                label = { Text("Dosage *") },
                placeholder = { Text("e.g., 1 tablet, 10ml") },
                leadingIcon = { Icon(Icons.Default.Scale, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Mood Tracker
            Card {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "How are you feeling?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf(
                            1 to "😞",
                            2 to "😕",
                            3 to "😐",
                            4 to "🙂",
                            5 to "😊"
                        ).forEach { (mood, emoji) ->
                            FilterChip(
                                selected = selectedMood == mood,
                                onClick = { 
                                    selectedMood = if (selectedMood == mood) null else mood 
                                },
                                label = { Text(emoji, style = MaterialTheme.typography.titleLarge) }
                            )
                        }
                    }
                }
            }

            // Side Effects
            OutlinedTextField(
                value = sideEffects,
                onValueChange = { sideEffects = it },
                label = { Text("Side Effects (Optional)") },
                placeholder = { Text("Any side effects experienced?") },
                leadingIcon = { Icon(Icons.Default.Warning, contentDescription = null) },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (Optional)") },
                placeholder = { Text("Any additional notes") },
                leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            // Quick Actions
            Card {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Quick Log",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "Quickly log common medicines you take",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "Vitamin D" to "1 tablet",
                            "Aspirin" to "1 tablet",
                            "Multivitamin" to "1 tablet"
                        ).forEach { (name, dose) ->
                            SuggestionChip(
                                onClick = {
                                    medicineName = name
                                    dosage = dose
                                },
                                label = { Text(name) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
