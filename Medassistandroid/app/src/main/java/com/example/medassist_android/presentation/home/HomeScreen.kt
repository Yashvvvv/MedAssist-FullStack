package com.example.medassist_android.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.example.medassist_android.data.model.Medicine
import com.example.medassist_android.presentation.auth.AuthViewModel
import com.example.medassist_android.presentation.medicine.MedicineViewModel
import com.example.medassist_android.presentation.pharmacy.PharmacyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToMedicineSearch: () -> Unit,
    onNavigateToCamera: () -> Unit,
    onNavigateToPharmacyList: () -> Unit,
    onNavigateToPharmacyMap: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToMedicineDetail: (Long) -> Unit,
    onNavigateToDrugInteractions: () -> Unit,
    onNavigateToReminders: () -> Unit = {},
    onNavigateToMedicineHistory: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val medicineViewModel: MedicineViewModel = hiltViewModel()
    val pharmacyViewModel: PharmacyViewModel = hiltViewModel()

    val authUiState by authViewModel.authUiState.collectAsStateWithLifecycle()
    val favoriteMedicines by medicineViewModel.favoriteMedicines.collectAsStateWithLifecycle()
    val recentlySearched by medicineViewModel.recentlySearched.collectAsStateWithLifecycle()
    val favoritePharmacies by pharmacyViewModel.favoritePharmacies.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Welcome back,",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = authUiState.user?.firstName ?: "User",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        IconButton(
                            onClick = onNavigateToProfile
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "How can we help you today?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Quick Actions
        item {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    icon = Icons.Default.Search,
                    title = "Search Medicine",
                    subtitle = "Find medicine info",
                    onClick = onNavigateToMedicineSearch,
                    modifier = Modifier.weight(1f)
                )

                QuickActionCard(
                    icon = Icons.Default.CameraAlt,
                    title = "Scan Medicine",
                    subtitle = "AI recognition",
                    onClick = onNavigateToCamera,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    icon = Icons.Default.LocalPharmacy,
                    title = "Find Pharmacy",
                    subtitle = "Nearby locations",
                    onClick = onNavigateToPharmacyList,
                    modifier = Modifier.weight(1f)
                )

                QuickActionCard(
                    icon = Icons.Default.Map,
                    title = "Pharmacy Map",
                    subtitle = "View on map",
                    onClick = onNavigateToPharmacyMap,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    icon = Icons.Default.Science,
                    title = "Drug Interactions",
                    subtitle = "Check safety",
                    onClick = onNavigateToDrugInteractions,
                    modifier = Modifier.weight(1f)
                )
                
                QuickActionCard(
                    icon = Icons.Default.Alarm,
                    title = "Reminders",
                    subtitle = "Set medicine alarms",
                    onClick = onNavigateToReminders,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    icon = Icons.Default.History,
                    title = "History",
                    subtitle = "View intake log",
                    onClick = onNavigateToMedicineHistory,
                    modifier = Modifier.weight(1f)
                )
                
                QuickActionCard(
                    icon = Icons.Default.Settings,
                    title = "Settings",
                    subtitle = "App preferences",
                    onClick = onNavigateToSettings,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Recently Searched Medicines
        if (recentlySearched.isNotEmpty()) {
            item {
                Text(
                    text = "Recently Searched",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(recentlySearched.take(5)) { medicine ->
                        MedicineCard(
                            medicine = medicine,
                            onClick = { onNavigateToMedicineDetail(medicine.id) }
                        )
                    }
                }
            }
        }

        // Favorite Medicines
        if (favoriteMedicines.isNotEmpty()) {
            item {
                Text(
                    text = "Favorite Medicines",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(favoriteMedicines.take(5)) { medicine ->
                        MedicineCard(
                            medicine = medicine,
                            onClick = { onNavigateToMedicineDetail(medicine.id) }
                        )
                    }
                }
            }
        }

        // Favorite Pharmacies
        if (favoritePharmacies.isNotEmpty()) {
            item {
                Text(
                    text = "Favorite Pharmacies",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                favoritePharmacies.take(3).forEach { pharmacy ->
                    PharmacyListItem(
                        pharmacy = pharmacy,
                        onClick = { /* Navigate to pharmacy detail */ }
                    )
                }
            }
        }

        // Health Tips or Additional Features
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = "Health Tip",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Health Tip",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Always consult with a healthcare professional before taking any new medication. Keep track of your medications and their expiration dates.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicineCard(
    medicine: Medicine,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.width(200.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = medicine.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = medicine.genericName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = medicine.manufacturer,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
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
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocalPharmacy,
                contentDescription = "Pharmacy",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
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
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            pharmacy.distance?.let { distance ->
                Text(
                    text = "${String.format("%.1f", distance)} km",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
