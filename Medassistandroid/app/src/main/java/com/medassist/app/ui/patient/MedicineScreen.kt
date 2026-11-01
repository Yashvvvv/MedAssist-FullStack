package com.medassist.app.ui.patient

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.medassist.app.core.model.MedicineView
import com.medassist.app.core.model.WhoHasResult
import com.medassist.app.core.model.toApiError
import com.medassist.app.core.repository.DiscoveryRepository
import com.medassist.app.data.LocationProvider
import com.medassist.app.ui.common.DetailScaffold
import com.medassist.app.ui.common.RxBadge
import com.medassist.app.ui.common.SectionHeader
import com.medassist.app.ui.common.StateContent
import com.medassist.app.ui.common.StatusPill
import com.medassist.app.ui.common.Tone
import com.medassist.app.ui.common.UiState
import com.medassist.app.ui.common.km
import com.medassist.app.ui.common.rupees
import com.medassist.app.ui.theme.Numeric
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MedicineData(val medicine: MedicineView, val nearby: List<WhoHasResult>, val substitutes: List<MedicineView>)

/** One medicine: what it is, cheaper same-salt alternatives, and which pharmacies nearby have it now. */
@HiltViewModel
class MedicineViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val discovery: DiscoveryRepository,
    private val location: LocationProvider,
) : ViewModel() {
    private val id: String = checkNotNull(savedState["id"])
    private val _state = MutableStateFlow<UiState<MedicineData>>(UiState.Loading)
    val state: StateFlow<UiState<MedicineData>> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            val point = location.point.value ?: LocationProvider.DEMO
            val med = discovery.medicine(id)
            val near = discovery.whoHas(id, point.lat, point.lng, 15.0)
            val subs = discovery.substitutes(id).getOrDefault(emptyList())
            _state.value = if (med.isSuccess) UiState.Success(MedicineData(med.getOrThrow(), near.getOrDefault(emptyList()), subs))
            else UiState.Error(med.exceptionOrNull()!!.toApiError())
        }
    }
}

@Composable
fun MedicineScreen(onBack: () -> Unit, onPharmacy: (String) -> Unit, onMedicine: (String) -> Unit, vm: MedicineViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    DetailScaffold((state as? UiState.Success)?.data?.medicine?.name ?: "Medicine", onBack) { padding ->
        Column(Modifier.padding(padding)) {
            StateContent(state, vm::load) { d ->
                val m = d.medicine
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
                            Column(Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(m.name, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                                    if (m.prescriptionRequired) RxBadge()
                                }
                                m.saltComposition?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
                                Text(listOfNotNull(m.manufacturer, m.dosageForm, m.packSize).joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                m.therapeuticClass?.let { Spacer(Modifier.height(8.dp)); StatusPill(it, Tone.Brand) }
                                m.mrp?.let { Text("MRP ${it.rupees()}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp)) }
                                if (m.prescriptionRequired) {
                                    Text("Needs a doctor's prescription. Add a photo when you order.",
                                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 6.dp))
                                }
                            }
                        }
                    }
                    item { SectionHeader("In stock near you") }
                    if (d.nearby.isEmpty()) {
                        item { Text("No pharmacy within 15 km has it in stock right now. Try a substitute below.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    } else items(d.nearby, key = { it.branchId }) { r ->
                        Card(onClick = { onPharmacy(r.branchId) }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
                            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(r.pharmacyName ?: r.name, style = MaterialTheme.typography.titleSmall)
                                    Text("${r.distanceKm.km()} away${if (r.deliveryAvailable) " · delivers" else ""}",
                                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(r.price.rupees(), style = MaterialTheme.typography.titleMedium.merge(Numeric))
                            }
                        }
                    }
                    if (d.substitutes.isNotEmpty()) {
                        item { SectionHeader("Same salt, other brands") }
                        items(d.substitutes, key = { "s" + it.id }) { s ->
                            ListItem(
                                modifier = Modifier.clickable { onMedicine(s.id) },
                                leadingContent = { Icon(Icons.Outlined.Medication, null) },
                                headlineContent = { Text(s.name) },
                                supportingContent = { Text(listOfNotNull(s.manufacturer, s.packSize).joinToString(" · ")) },
                                trailingContent = { s.mrp?.let { Text(it.rupees()) } },
                            )
                        }
                        item {
                            Text("Ask your doctor or pharmacist before switching brands.", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Suppress("unused")
private val w = Modifier.width(0.dp)
