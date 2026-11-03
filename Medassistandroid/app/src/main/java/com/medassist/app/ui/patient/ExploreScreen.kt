package com.medassist.app.ui.patient

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import com.medassist.app.ui.common.AppCard
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
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.LocalPharmacy
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.medassist.app.core.model.MedicineView
import com.medassist.app.core.model.PharmacyView
import com.medassist.app.core.model.toApiError
import com.medassist.app.core.repository.DiscoveryRepository
import com.medassist.app.data.GeoPoint
import com.medassist.app.data.LocationProvider
import com.medassist.app.ui.common.EmptyState
import com.medassist.app.ui.common.ErrorBox
import com.medassist.app.ui.common.LoadingBox
import com.medassist.app.ui.common.RxBadge
import com.medassist.app.ui.common.StatusPill
import com.medassist.app.ui.common.Tone
import com.medassist.app.ui.common.UiState
import com.medassist.app.ui.common.km
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val discovery: DiscoveryRepository,
    private val location: LocationProvider,
) : ViewModel() {
    val point: StateFlow<GeoPoint?> = location.point
    private val _pharmacies = MutableStateFlow<UiState<List<PharmacyView>>>(UiState.Loading)
    val pharmacies: StateFlow<UiState<List<PharmacyView>>> = _pharmacies.asStateFlow()
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()
    private val _medicines = MutableStateFlow<List<MedicineView>>(emptyList())
    val medicines: StateFlow<List<MedicineView>> = _medicines.asStateFlow()
    private val _radius = MutableStateFlow(5.0)
    val radius: StateFlow<Double> = _radius.asStateFlow()
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            _query.debounce(250).collect { q ->
                searchJob?.cancel()
                if (q.trim().length < 2) {
                    _medicines.value = emptyList()
                } else {
                    searchJob = launch { discovery.searchMedicines(q.trim()).onSuccess { _medicines.value = it.content } }
                }
            }
        }
    }

    fun locate(granted: Boolean) {
        viewModelScope.launch {
            val p = if (granted) location.current() else null
            location.point.value = p ?: location.point.value ?: LocationProvider.DEMO
            load()
        }
    }

    fun useDemo() {
        location.point.value = LocationProvider.DEMO
        load()
    }

    fun widen() {
        _radius.value = (_radius.value * 3).coerceAtMost(50.0)
        load()
    }

    fun load() {
        val p = location.point.value ?: return
        viewModelScope.launch {
            _pharmacies.value = UiState.Loading
            discovery.nearby(p.lat, p.lng, _radius.value).fold(
                { _pharmacies.value = UiState.Success(it) },
                { _pharmacies.value = UiState.Error(it.toApiError()) },
            )
        }
    }

    fun onQuery(q: String) {
        _query.value = q
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    onPharmacy: (String) -> Unit,
    onMedicine: (String) -> Unit,
    onNotifications: () -> Unit,
    onScan: () -> Unit = {},
    vm: ExploreViewModel = hiltViewModel(),
) {
    val point by vm.point.collectAsStateWithLifecycle()
    val pharmacies by vm.pharmacies.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val medicines by vm.medicines.collectAsStateWithLifecycle()
    val radius by vm.radius.collectAsStateWithLifecycle()
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.locate(it) }
    LaunchedEffect(Unit) {
        if (point == null) permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION) else vm.load()
    }
    val scheme = MaterialTheme.colorScheme

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = scheme.background),
                title = { Text("Find medicines", style = MaterialTheme.typography.headlineSmall) },
                actions = {
                    Surface(
                        onClick = onNotifications,
                        shape = MaterialTheme.shapes.small,
                        color = scheme.surfaceContainerLowest,
                        shadowElevation = 1.dp,
                        modifier = Modifier.padding(end = 12.dp).size(42.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Notifications, "Notifications") }
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Surface(
                    onClick = { permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
                    shape = CircleShape,
                    color = scheme.primaryContainer,
                    contentColor = scheme.primary,
                ) {
                    Row(Modifier.padding(start = 8.dp, end = 12.dp, top = 5.dp, bottom = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.LocationOn, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(point?.label ?: "Locating…", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            item {
                TextField(
                    query, vm::onQuery,
                    placeholder = { Text("Search a medicine or salt, e.g. Dolo 650") },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { vm.onQuery("") }) { Icon(Icons.Rounded.Close, "Clear") } },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = scheme.surfaceContainerLowest,
                        unfocusedContainerColor = scheme.surfaceContainerLowest,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    modifier = Modifier.fillMaxWidth().shadow(1.dp, MaterialTheme.shapes.medium)
                        .border(1.5.dp, if (query.isNotEmpty()) scheme.primary else Color.Transparent, MaterialTheme.shapes.medium),
                )
            }
            if (medicines.isNotEmpty()) {
                item {
                    AppCard {
                        medicines.forEachIndexed { i, m ->
                            if (i > 0) HorizontalDivider(color = scheme.outlineVariant, modifier = Modifier.padding(start = 64.dp))
                            ListItem(
                                modifier = Modifier.clickable { onMedicine(m.id) },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                leadingContent = {
                                    Box(Modifier.size(36.dp).clip(MaterialTheme.shapes.small).background(scheme.primaryContainer), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Outlined.Medication, null, tint = scheme.primary, modifier = Modifier.size(20.dp))
                                    }
                                },
                                headlineContent = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(m.name, style = MaterialTheme.typography.titleSmall)
                                        if (m.prescriptionRequired) { Spacer(Modifier.width(8.dp)); RxBadge() }
                                    }
                                },
                                supportingContent = { Text(listOfNotNull(m.saltComposition, m.packSize).joinToString(" · "), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                trailingContent = { Text("Who has it?", style = MaterialTheme.typography.labelMedium, color = scheme.primary) },
                            )
                        }
                    }
                }
            } else {
                item { ScanPromo(onScan) }
                item {
                    Text("Pharmacies near you", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 6.dp))
                }
                when (val s = pharmacies) {
                    UiState.Loading -> item { LoadingBox(Modifier.height(200.dp)) }
                    is UiState.Error -> item { ErrorBox(s.error, vm::load, Modifier.height(300.dp)) }
                    is UiState.Success -> if (s.data.isEmpty()) {
                        item {
                            Column {
                                EmptyState(
                                    Icons.Outlined.LocalPharmacy, "No MedAssist pharmacies within ${radius.toInt()} km",
                                    "Search a wider area, or try the demo area in Noida.",
                                    Modifier.height(280.dp),
                                )
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                    if (radius < 50) TextButton(onClick = vm::widen) { Text("Search ${(radius * 3).coerceAtMost(50.0).toInt()} km") }
                                    TextButton(onClick = vm::useDemo) { Text("Use demo area") }
                                }
                            }
                        }
                    } else items(s.data, key = { it.branchId }) { PharmacyCard(it) { onPharmacy(it.branchId) } }
                }
            }
        }
    }
}

/** The AI entry point, given a soft teal-to-blue card so it reads as a feature, not a list row. */
@Composable
private fun ScanPromo(onScan: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.background.luminance() < 0.5f
    val brush = if (dark) Brush.linearGradient(listOf(scheme.primaryContainer, scheme.tertiaryContainer))
    else Brush.linearGradient(listOf(Color(0xFFE2F6F1), Color(0xFFD6ECFB)))
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(brush).clickable(onClick = onScan).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(shape = MaterialTheme.shapes.small, color = scheme.surfaceContainerLowest, modifier = Modifier.size(46.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.PhotoCamera, null, tint = scheme.primary) }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Not sure what a medicine is?", style = MaterialTheme.typography.titleSmall)
            Text("Snap the strip: uses, warnings and cheaper same-salt options.", style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = scheme.onSurfaceVariant)
    }
}

@Composable
fun PharmacyCard(p: PharmacyView, onClick: () -> Unit) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    AppCard(onClick = onClick) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(p.pharmacyName ?: p.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold))
                    Text(listOfNotNull(p.name.takeIf { p.pharmacyName != null }, p.addressLine, p.city).joinToString(", "),
                        style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, maxLines = 2)
                }
                p.distanceKm?.let {
                    Spacer(Modifier.width(8.dp))
                    Text(it.km(), style = MaterialTheme.typography.labelMedium, color = scheme.primary,
                        modifier = Modifier.clip(MaterialTheme.shapes.extraSmall).background(scheme.primaryContainer).padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (p.open24x7) StatusPill("Open 24×7", Tone.Success) else p.openingHours?.let { StatusPill(it, Tone.Neutral) }
                if (p.deliveryAvailable) StatusPill("Delivers", Tone.Brand)
            }
            val hasMap = p.latitude != null && p.longitude != null
            if (hasMap || p.phone != null) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    p.phone?.let { phone ->
                        Button(
                            onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))) } },
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Outlined.Call, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Call")
                        }
                    }
                    if (hasMap) {
                        FilledTonalButton(
                            onClick = { openInMaps(context, p.latitude!!, p.longitude!!, p.pharmacyName ?: p.name) },
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Outlined.Directions, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Directions")
                        }
                    }
                }
            }
        }
    }
}

/** Hands navigation to Google Maps (or any maps app): no Maps SDK or API key in this app. */
fun openInMaps(context: android.content.Context, lat: Double, lng: Double, label: String) {
    val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(label)})")
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }.onFailure {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng")))
    }
}
