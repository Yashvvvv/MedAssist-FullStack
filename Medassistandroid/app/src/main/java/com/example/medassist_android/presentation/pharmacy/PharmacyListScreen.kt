package com.example.medassist_android.presentation.pharmacy

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PharmacyListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pharmacyViewModel: PharmacyViewModel = hiltViewModel()
    val pharmacyUiState by pharmacyViewModel.pharmacyUiState.collectAsStateWithLifecycle()
    val nearbyUiState by pharmacyViewModel.nearbyPharmaciesUiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        pharmacyViewModel.loadPharmacies()
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("Pharmacies") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = onNavigateToMap) {
                    Icon(Icons.Default.Map, contentDescription = "Map View")
                }
                IconButton(onClick = { /* TODO: Search */ }) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
            }
        )

        // Quick Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { /* TODO: Find nearby */ },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = "Location")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Nearby")
            }

            OutlinedButton(
                onClick = { /* TODO: Filter */ },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.FilterList, contentDescription = "Filter")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Filter")
            }
        }

        // Content
        if (pharmacyUiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else pharmacyUiState.error?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        } ?: run {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(pharmacyUiState.pharmacies) { pharmacy ->
                    PharmacyListItem(
                        pharmacy = pharmacy,
                        onClick = { onNavigateToDetail(pharmacy.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PharmacyListItem(
    pharmacy: com.example.medassist_android.data.model.Pharmacy,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = pharmacy.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = pharmacy.address,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Phone",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = pharmacy.phoneNumber ?: "N/A",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    pharmacy.distance?.let { distance ->
                        Text(
                            text = "${String.format("%.1f", distance)} km",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    pharmacy.isCurrentlyOpen?.let { isOpen ->
                        AssistChip(
                            onClick = { },
                            label = {
                                Text(
                                    text = if (isOpen) "Open" else "Closed",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (isOpen)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.errorContainer
                            )
                        )
                    }
                }
            }

            if (!pharmacy.services.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    pharmacy.services?.take(3)?.forEach { service ->
                        AssistChip(
                            onClick = { },
                            label = {
                                Text(
                                    text = service,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    }
                }
            }
        }
    }
}
