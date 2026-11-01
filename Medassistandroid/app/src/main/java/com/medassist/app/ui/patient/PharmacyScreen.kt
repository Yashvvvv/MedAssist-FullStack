package com.medassist.app.ui.patient

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.medassist.app.core.model.Availability
import com.medassist.app.core.model.Fulfilment
import com.medassist.app.core.model.OrderLineRequest
import com.medassist.app.core.model.PharmacyView
import com.medassist.app.core.model.PlaceOrderRequest
import com.medassist.app.core.model.PublicProductView
import com.medassist.app.core.model.toApiError
import com.medassist.app.core.repository.DiscoveryRepository
import com.medassist.app.core.repository.OrdersRepository
import com.medassist.app.core.repository.UploadFile
import com.medassist.app.data.PhotoReader
import com.medassist.app.ui.common.DetailScaffold
import com.medassist.app.ui.common.KeyValueRow
import com.medassist.app.ui.common.PrimaryButton
import com.medassist.app.ui.common.RxBadge
import com.medassist.app.ui.common.StateContent
import com.medassist.app.ui.common.StatusPill
import com.medassist.app.ui.common.Tone
import com.medassist.app.ui.common.UiState
import com.medassist.app.ui.common.message
import com.medassist.app.ui.common.rupees
import com.medassist.app.ui.pharmacy.pos.Stepper
import com.medassist.app.ui.theme.Numeric
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class Basket(
    val items: Map<String, Pair<PublicProductView, Int>> = emptyMap(),
    val fulfilment: Fulfilment = Fulfilment.PICKUP,
    val address: String = "",
    val phone: String = "",
    val notes: String = "",
    val prescription: UploadFile? = null,
    val placing: Boolean = false,
    val error: String? = null,
) {
    val count: Int get() = items.values.sumOf { it.second }
    val total: BigDecimal get() = items.values.fold(BigDecimal.ZERO) { a, (p, q) -> a + p.price.multiply(BigDecimal(q)) }
    val needsPrescription: Boolean get() = items.values.any { it.first.prescriptionRequired }
}

@OptIn(FlowPreview::class)
@HiltViewModel
class PharmacyViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val discovery: DiscoveryRepository,
    private val orders: OrdersRepository,
    private val photos: PhotoReader,
) : ViewModel() {
    val branchId: String = checkNotNull(savedState["id"])
    private val _pharmacy = MutableStateFlow<UiState<PharmacyView>>(UiState.Loading)
    val pharmacy: StateFlow<UiState<PharmacyView>> = _pharmacy.asStateFlow()
    private val _products = MutableStateFlow<List<PublicProductView>>(emptyList())
    val products: StateFlow<List<PublicProductView>> = _products.asStateFlow()
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()
    private val _basket = MutableStateFlow(Basket())
    val basket: StateFlow<Basket> = _basket.asStateFlow()
    private val _placed = MutableStateFlow<String?>(null)
    val placed: StateFlow<String?> = _placed.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            _query.debounce(250).collect { q -> discovery.catalogue(branchId, q).onSuccess { _products.value = it.content } }
        }
    }

    fun load() {
        viewModelScope.launch {
            discovery.pharmacy(branchId).fold({ _pharmacy.value = UiState.Success(it) }, { _pharmacy.value = UiState.Error(it.toApiError()) })
        }
    }

    fun onQuery(q: String) {
        _query.value = q
    }

    fun setQty(p: PublicProductView, qty: Int) = _basket.update { b ->
        val items = b.items.toMutableMap()
        if (qty <= 0) items.remove(p.productId) else items[p.productId] = p to qty.coerceAtMost(20)
        b.copy(items = items, error = null)
    }

    fun update(transform: (Basket) -> Basket) = _basket.update { transform(it).copy(error = null) }
    fun newCaptureFile() = photos.newCaptureFile()

    fun attach(uri: Uri) {
        viewModelScope.launch {
            val file = photos.read(uri)
            _basket.update { if (file == null) it.copy(error = "Couldn't read that photo") else it.copy(prescription = file) }
        }
    }

    fun place() {
        val b = _basket.value
        val error = when {
            b.items.isEmpty() -> "Your basket is empty"
            !Regex("^\\+?[0-9]{10,13}$").matches(b.phone.trim()) -> "Enter a 10-digit mobile number the pharmacy can call"
            b.fulfilment == Fulfilment.DELIVERY && b.address.isBlank() -> "Enter the delivery address"
            b.needsPrescription && b.prescription == null -> "Add a photo of your prescription for Rx medicines"
            else -> null
        }
        if (error != null) return _basket.update { it.copy(error = error) }
        viewModelScope.launch {
            _basket.update { it.copy(placing = true) }
            val req = PlaceOrderRequest(
                branchId = branchId,
                fulfilment = b.fulfilment,
                contactPhone = b.phone.trim(),
                deliveryAddress = b.address.trim().takeIf { b.fulfilment == Fulfilment.DELIVERY },
                notes = b.notes.trim().ifBlank { null },
                lines = b.items.values.map { (p, q) -> OrderLineRequest(p.productId, q) },
            )
            orders.place(req, b.prescription).fold(
                { _placed.value = it.id },
                { e -> _basket.update { it.copy(placing = false, error = e.toApiError().message()) } },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PharmacyScreen(onBack: () -> Unit, onOrdered: (String) -> Unit, vm: PharmacyViewModel = hiltViewModel()) {
    val pharmacy by vm.pharmacy.collectAsStateWithLifecycle()
    val products by vm.products.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val basket by vm.basket.collectAsStateWithLifecycle()
    val placed by vm.placed.collectAsStateWithLifecycle()
    var checkout by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(placed) { placed?.let(onOrdered) }

    DetailScaffold(
        title = (pharmacy as? UiState.Success)?.data?.let { it.pharmacyName ?: it.name } ?: "Pharmacy",
        onBack = onBack,
        bottomBar = {
            if (basket.count > 0) {
                BottomAppBar {
                    Column(Modifier.weight(1f).padding(start = 8.dp)) {
                        Text("${basket.count} item(s)", style = MaterialTheme.typography.labelMedium)
                        Text(basket.total.rupees(), style = MaterialTheme.typography.titleLarge.merge(Numeric))
                    }
                    PrimaryButton("Review order", { checkout = true }, Modifier.padding(end = 8.dp))
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            StateContent(pharmacy, vm::load) { p ->
                LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                    item { PharmacyHeader(p) }
                    item {
                        OutlinedTextField(
                            query, vm::onQuery,
                            placeholder = { Text("Search this pharmacy") },
                            leadingIcon = { Icon(Icons.Outlined.Search, null) },
                            singleLine = true,
                            shape = MaterialTheme.shapes.extraLarge,
                            modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp),
                        )
                    }
                    items(products, key = { it.productId }) { prod ->
                        val qty = basket.items[prod.productId]?.second ?: 0
                        ProductListing(prod, qty) { vm.setQty(prod, it) }
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }

    if (checkout) {
        ModalBottomSheet(onDismissRequest = { checkout = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            CheckoutSheet(basket, (pharmacy as? UiState.Success)?.data?.deliveryAvailable == true, vm)
        }
    }
}

@Composable
private fun PharmacyHeader(p: PharmacyView) {
    val context = LocalContext.current
    Card(Modifier.padding(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(listOfNotNull(p.addressLine, p.city, p.pincode).joinToString(", "), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (p.open24x7) StatusPill("Open 24×7", Tone.Success) else p.openingHours?.let { StatusPill(it, Tone.Neutral) }
                StatusPill(if (p.deliveryAvailable) "Pickup or delivery" else "Pickup only", Tone.Brand)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                p.phone?.let { phone ->
                    FilledTonalButton(onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))) }) {
                        Icon(Icons.Outlined.Call, null); Spacer(Modifier.width(6.dp)); Text("Call")
                    }
                }
                if (p.latitude != null && p.longitude != null) {
                    FilledTonalButton(onClick = { openInMaps(context, p.latitude, p.longitude, p.pharmacyName ?: p.name) }) {
                        Icon(Icons.Outlined.Directions, null); Spacer(Modifier.width(6.dp)); Text("Directions")
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductListing(p: PublicProductView, qty: Int, onQty: (Int) -> Unit) {
    val available = p.availability != Availability.OUT_OF_STOCK
    ListItem(
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(p.name, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (p.prescriptionRequired) { Spacer(Modifier.width(8.dp)); RxBadge() }
            }
        },
        supportingContent = {
            Column {
                Text(listOfNotNull(p.saltComposition, p.packSize).joinToString(" · "), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(p.price.rupees(), style = MaterialTheme.typography.titleSmall.merge(Numeric), color = MaterialTheme.colorScheme.onSurface)
                    if (p.mrp > p.price) {
                        Spacer(Modifier.width(6.dp))
                        Text("MRP ${p.mrp.rupees()}", style = MaterialTheme.typography.labelSmall,
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)
                    }
                    Spacer(Modifier.width(8.dp))
                    when (p.availability) {
                        Availability.IN_STOCK -> StatusPill("In stock", Tone.Success)
                        Availability.LOW_STOCK -> StatusPill("Few left", Tone.Warning)
                        Availability.OUT_OF_STOCK -> StatusPill("Out of stock", Tone.Neutral)
                    }
                }
            }
        },
        trailingContent = {
            if (qty == 0) OutlinedButton(onClick = { onQty(1) }, enabled = available) { Text("Add") }
            else Stepper(qty, onQty)
        },
    )
}

@Composable
private fun CheckoutSheet(b: Basket, deliveryAvailable: Boolean, vm: PharmacyViewModel) {
    val context = LocalContext.current
    var captureUri by remember { mutableStateOf<Uri?>(null) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { it?.let(vm::attach) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok) captureUri?.let(vm::attach) }
    Column(Modifier.navigationBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Text("Your order", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        b.items.values.forEach { (p, q) -> KeyValueRow("${p.name} × $q", p.price.multiply(BigDecimal(q)).rupees()) }
        KeyValueRow("Estimated total", b.total.rupees(), strong = true)
        Text("You pay at the pharmacy. The final bill can differ slightly (batch MRP).",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(b.fulfilment == Fulfilment.PICKUP, { vm.update { it.copy(fulfilment = Fulfilment.PICKUP) } }, label = { Text("Pick up") })
            if (deliveryAvailable) {
                FilterChip(b.fulfilment == Fulfilment.DELIVERY, { vm.update { it.copy(fulfilment = Fulfilment.DELIVERY) } }, label = { Text("Deliver to me") })
            }
        }
        if (b.fulfilment == Fulfilment.DELIVERY) {
            OutlinedTextField(b.address, { v -> vm.update { it.copy(address = v) } }, label = { Text("Delivery address") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp), minLines = 2)
        }
        OutlinedTextField(b.phone, { v -> vm.update { it.copy(phone = v.filter { c -> c.isDigit() || c == '+' }.take(13)) } },
            label = { Text("Mobile number") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        OutlinedTextField(b.notes, { v -> vm.update { it.copy(notes = v.take(500)) } }, label = { Text("Note for the pharmacy (optional)") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        if (b.needsPrescription) {
            Spacer(Modifier.height(12.dp))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Column(Modifier.padding(14.dp)) {
                    Text("Prescription needed", style = MaterialTheme.typography.titleSmall)
                    Text("Some items need a doctor's prescription. The pharmacist checks it before preparing your order.",
                        style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    if (b.prescription != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("Photo added")
                        }
                    } else Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            val uri = FileProvider.getUriForFile(context, context.packageName + ".files", vm.newCaptureFile())
                            captureUri = uri
                            camera.launch(uri)
                        }) { Icon(Icons.Outlined.CameraAlt, null); Spacer(Modifier.width(6.dp)); Text("Camera") }
                        OutlinedButton(onClick = { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                            Icon(Icons.Outlined.Image, null); Spacer(Modifier.width(6.dp)); Text("Gallery")
                        }
                    }
                }
            }
        }
        b.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp)) }
        Spacer(Modifier.height(16.dp))
        PrimaryButton("Place order", vm::place, Modifier.fillMaxWidth(), loading = b.placing)
        Spacer(Modifier.height(20.dp))
    }
}
