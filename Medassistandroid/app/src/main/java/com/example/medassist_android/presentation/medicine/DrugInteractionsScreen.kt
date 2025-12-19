package com.example.medassist_android.presentation.medicine

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrugInteractionsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: MedicineViewModel = hiltViewModel()
    val uiState by viewModel.medicineUiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    var primaryMedicine by remember { mutableStateOf("") }
    var currentMedicine by remember { mutableStateOf("") }
    var otherMedicines by remember { mutableStateOf<List<String>>(emptyList()) }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("Drug Interactions") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Info Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Medication,
                        contentDescription = "Info",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Check Drug Interactions",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Enter a primary medicine and add other medicines to check for potential interactions.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Primary Medicine Input
            OutlinedTextField(
                value = primaryMedicine,
                onValueChange = { primaryMedicine = it },
                label = { Text("Primary Medicine") },
                placeholder = { Text("e.g., Aspirin, Ibuprofen") },
                leadingIcon = {
                    Icon(Icons.Default.LocalPharmacy, contentDescription = null)
                },
                trailingIcon = {
                    if (primaryMedicine.isNotEmpty()) {
                        IconButton(onClick = { primaryMedicine = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Other Medicines Section
            Card {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Other Medicines to Check",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Add medicine input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = currentMedicine,
                            onValueChange = { currentMedicine = it },
                            label = { Text("Add Medicine") },
                            placeholder = { Text("Enter medicine name") },
                            leadingIcon = {
                                Icon(Icons.Default.Add, contentDescription = null)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (currentMedicine.isNotBlank() && 
                                        !otherMedicines.contains(currentMedicine.trim())) {
                                        otherMedicines = otherMedicines + currentMedicine.trim()
                                        currentMedicine = ""
                                        focusManager.clearFocus()
                                    }
                                }
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        FilledIconButton(
                            onClick = {
                                if (currentMedicine.isNotBlank() && 
                                    !otherMedicines.contains(currentMedicine.trim())) {
                                    otherMedicines = otherMedicines + currentMedicine.trim()
                                    currentMedicine = ""
                                    focusManager.clearFocus()
                                }
                            },
                            enabled = currentMedicine.isNotBlank()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Added medicines chips
                    if (otherMedicines.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            otherMedicines.forEach { medicine ->
                                InputChip(
                                    selected = false,
                                    onClick = { },
                                    label = { Text(medicine) },
                                    trailingIcon = {
                                        IconButton(
                                            onClick = {
                                                otherMedicines = otherMedicines - medicine
                                            },
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
                    } else {
                        Text(
                            text = "No medicines added yet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Check Interactions Button
            Button(
                onClick = {
                    focusManager.clearFocus()
                    viewModel.analyzeDrugInteractions(primaryMedicine.trim(), otherMedicines)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isAnalyzing && 
                        primaryMedicine.isNotBlank() && 
                        otherMedicines.isNotEmpty()
            ) {
                if (uiState.isAnalyzing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Analyzing...")
                } else {
                    Icon(Icons.Default.Science, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Check Interactions")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Error Message
            uiState.analysisError?.let { error ->
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
                            imageVector = Icons.Default.Error,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Results Section
            uiState.analysisResponse?.let { response ->
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Drug Interactions
                    item {
                        InteractionResultCard(
                            title = "Drug Interactions",
                            icon = Icons.Default.Warning,
                            items = response.drugInteractions ?: emptyList(),
                            containerColor = if ((response.drugInteractions?.size ?: 0) > 0)
                                MaterialTheme.colorScheme.errorContainer
                            else
                                MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = if ((response.drugInteractions?.size ?: 0) > 0)
                                MaterialTheme.colorScheme.onErrorContainer
                            else
                                MaterialTheme.colorScheme.onTertiaryContainer,
                            emptyMessage = "No known drug interactions found"
                        )
                    }

                    // Contraindications
                    if (!response.contraindications.isNullOrEmpty()) {
                        item {
                            InteractionResultCard(
                                title = "Contraindications",
                                icon = Icons.Default.Block,
                                items = response.contraindications ?: emptyList(),
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }

                    // Warnings
                    if (!response.warnings.isNullOrEmpty()) {
                        item {
                            InteractionResultCard(
                                title = "Warnings",
                                icon = Icons.Default.Warning,
                                items = response.warnings ?: emptyList(),
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    // Side Effects
                    if (!response.sideEffects.isNullOrEmpty()) {
                        item {
                            InteractionResultCard(
                                title = "Side Effects",
                                icon = Icons.Default.Info,
                                items = response.sideEffects ?: emptyList(),
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Disclaimer
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PrivacyTip,
                                    contentDescription = "Disclaimer",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "This information is for educational purposes only. Always consult with a healthcare professional before making any changes to your medication regimen.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        content = { content() }
    )
}

@Composable
private fun InteractionResultCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    items: List<String>,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    emptyMessage: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Spacer(modifier = Modifier.weight(1f))
                Badge(
                    containerColor = contentColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "${items.size}",
                        color = contentColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (items.isEmpty() && emptyMessage != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = emptyMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor
                    )
                }
            } else {
                items.forEachIndexed { index, item ->
                    Row(
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyMedium,
                            color = contentColor
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item,
                            style = MaterialTheme.typography.bodyMedium,
                            color = contentColor
                        )
                    }
                    if (index < items.lastIndex) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}
