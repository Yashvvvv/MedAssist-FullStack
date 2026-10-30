package com.medassist.app.ui.pharmacy.orders

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.medassist.app.core.model.Fulfilment
import com.medassist.app.core.model.OrderStatus
import com.medassist.app.core.model.OrderView
import com.medassist.app.core.model.PaymentMode
import com.medassist.app.core.model.toApiError
import com.medassist.app.core.repository.OrdersRepository
import com.medassist.app.ui.common.DetailScaffold
import com.medassist.app.ui.common.KeyValueRow
import com.medassist.app.ui.common.OrderTimeline
import com.medassist.app.ui.common.PrimaryButton
import com.medassist.app.ui.common.StateContent
import com.medassist.app.ui.common.UiState
import com.medassist.app.ui.common.asDateTime
import com.medassist.app.ui.common.message
import com.medassist.app.ui.common.rupees
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val orders: OrdersRepository,
    private val sales: com.medassist.app.core.repository.SalesRepository,
) : ViewModel() {
    private val _rx = MutableStateFlow<android.graphics.Bitmap?>(null)
    /** The customer's prescription photo, loaded when the pharmacist opens it. */
    val rx: StateFlow<android.graphics.Bitmap?> = _rx.asStateFlow()

    fun openPrescription(prescriptionId: String) {
        viewModelScope.launch {
            sales.prescriptionFile(prescriptionId).fold(
                { bytes ->
                    val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bmp == null) _error.value = "This prescription is a PDF; open it on the web dashboard." else _rx.value = bmp
                },
                { _error.value = it.toApiError().message() },
            )
        }
    }

    fun closePrescription() {
        _rx.value = null
    }

    private val id: String = checkNotNull(savedState["id"])
    private val _state = MutableStateFlow<UiState<OrderView>>(UiState.Loading)
    val state: StateFlow<UiState<OrderView>> = _state.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            orders.order(id).fold({ _state.value = UiState.Success(it) }, { _state.value = UiState.Error(it.toApiError()) })
        }
    }

    fun accept() = act { orders.accept(id) }
    fun ready() = act { orders.ready(id) }
    fun reject(reason: String) = act { orders.reject(id, reason) }
    fun complete(mode: PaymentMode) = act { orders.complete(id, mode) }
    fun dismissError() {
        _error.value = null
    }

    private fun act(call: suspend () -> Result<OrderView>) {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            call().fold({ _state.value = UiState.Success(it) }, { _error.value = it.toApiError().message(); load() })
            _busy.value = false
        }
    }
}

@Composable
fun OrderDetailScreen(onBack: () -> Unit, onInvoice: (String) -> Unit, vm: OrderDetailViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val rx by vm.rx.collectAsStateWithLifecycle()
    var rejecting by rememberSaveable { mutableStateOf(false) }
    var completing by rememberSaveable { mutableStateOf(false) }
    val order = (state as? UiState.Success)?.data

    DetailScaffold(
        title = order?.orderNo ?: "Order",
        onBack = onBack,
        bottomBar = {
            if (order != null && order.status.isOpen) {
                Surface(tonalElevation = 3.dp) {
                    Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { rejecting = true }, enabled = !busy, modifier = Modifier.weight(1f).height(52.dp)) { Text("Reject") }
                        when (order.status) {
                            OrderStatus.PLACED -> PrimaryButton("Accept", vm::accept, Modifier.weight(1f), loading = busy)
                            OrderStatus.ACCEPTED -> PrimaryButton(
                                if (order.fulfilment == Fulfilment.PICKUP) "Mark ready" else "Out for delivery",
                                vm::ready, Modifier.weight(1f), loading = busy,
                            )
                            else -> PrimaryButton("Hand over & bill", { completing = true }, Modifier.weight(1f), loading = busy)
                        }
                    }
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            StateContent(state, vm::load) { o -> OrderBody(o, onInvoice, vm::openPrescription) }
        }
    }

    if (rejecting) {
        var reason by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { rejecting = false },
            title = { Text("Reject order?") },
            text = {
                Column {
                    Text("The customer sees this reason.")
                    Spacer(Modifier.height(8.dp))
                    listOf("Out of stock", "Prescription unclear", "Can't deliver to this address").forEach { r ->
                        FilterChip(selected = reason == r, onClick = { reason = r }, label = { Text(r) }, modifier = Modifier.padding(end = 4.dp))
                    }
                    OutlinedTextField(reason, { reason = it }, label = { Text("Reason") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(enabled = reason.isNotBlank(), onClick = { rejecting = false; vm.reject(reason.trim()) }) { Text("Reject") }
            },
            dismissButton = { TextButton(onClick = { rejecting = false }) { Text("Cancel") } },
        )
    }
    if (completing) {
        AlertDialog(
            onDismissRequest = { completing = false },
            title = { Text("How did they pay?") },
            text = {
                Column {
                    Text("This bills the order as a normal sale: stock is taken first-expiry-first and a GST invoice is created.")
                    Spacer(Modifier.height(12.dp))
                    PaymentMode.entries.filter { it != PaymentMode.CREDIT }.forEach { mode ->
                        OutlinedButton(onClick = { completing = false; vm.complete(mode) }, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            Text(mode.name.lowercase().replaceFirstChar { it.uppercase() })
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { completing = false }) { Text("Cancel") } },
        )
    }
    rx?.let { bmp ->
        AlertDialog(
            onDismissRequest = vm::closePrescription,
            confirmButton = { TextButton(onClick = vm::closePrescription) { Text("Close") } },
            text = {
                androidx.compose.foundation.Image(
                    bmp.asImageBitmap(), "Prescription",
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = androidx.compose.ui.layout.ContentScale.FillWidth,
                )
            },
        )
    }
    error?.let {
        AlertDialog(onDismissRequest = vm::dismissError, confirmButton = { TextButton(onClick = vm::dismissError) { Text("OK") } }, text = { Text(it) })
    }
}

@Composable
private fun OrderBody(o: OrderView, onInvoice: (String) -> Unit, onPrescription: (String) -> Unit) {
    val context = LocalContext.current
    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
            Column(Modifier.padding(16.dp)) {
                OrderTimeline(o)
                Spacer(Modifier.height(8.dp))
                Text("Placed ${o.createdAt.asDateTime()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
            Column(Modifier.padding(16.dp)) {
                Text(if (o.fulfilment == Fulfilment.DELIVERY) "Home delivery" else "Pickup at store", style = MaterialTheme.typography.titleSmall)
                o.deliveryAddress?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                o.notes?.let { Text("Note: $it", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp)) }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(o.contactPhone, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${o.contactPhone}"))) }) {
                        Icon(Icons.Outlined.Call, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Call")
                    }
                }
                o.prescriptionId?.let { rxId ->
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { onPrescription(rxId) }, modifier = Modifier.fillMaxWidth()) { Text("View prescription photo") }
                    Text("Check it before handing over Rx medicines.", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
            Column(Modifier.padding(16.dp)) {
                Text("Items", style = MaterialTheme.typography.titleSmall)
                o.lines.forEach { KeyValueRow("${it.productName} × ${it.quantity}", (it.unitPrice * it.quantity.toBigDecimal()).rupees()) }
                KeyValueRow(if (o.saleId != null) "Billed" else "Estimated total", o.estimatedTotal.rupees(), strong = true)
            }
        }
        o.saleId?.let { saleId -> OutlinedButton(onClick = { onInvoice(saleId) }, modifier = Modifier.fillMaxWidth()) { Text("View invoice") } }
    }
}
