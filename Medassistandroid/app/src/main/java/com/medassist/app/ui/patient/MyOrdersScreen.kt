package com.medassist.app.ui.patient

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import com.medassist.app.core.model.toApiError
import com.medassist.app.core.repository.OrdersRepository
import com.medassist.app.ui.common.DetailScaffold
import com.medassist.app.ui.common.EmptyState
import com.medassist.app.ui.common.ErrorBox
import com.medassist.app.ui.common.KeyValueRow
import com.medassist.app.ui.common.LoadingBox
import com.medassist.app.ui.common.OrderTimeline
import com.medassist.app.ui.common.StateContent
import com.medassist.app.ui.common.UiState
import com.medassist.app.ui.common.asDateTime
import com.medassist.app.ui.common.message
import com.medassist.app.ui.common.rupees
import com.medassist.app.ui.pharmacy.orders.OrderCard
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@HiltViewModel
class MyOrdersViewModel @Inject constructor(private val orders: OrdersRepository) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<OrderView>>>(UiState.Loading)
    val state: StateFlow<UiState<List<OrderView>>> = _state.asStateFlow()
    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    fun load(pull: Boolean = false) {
        viewModelScope.launch {
            if (pull) _refreshing.value = true
            orders.myOrders().fold({ _state.value = UiState.Success(it.content) }, { if (_state.value !is UiState.Success) _state.value = UiState.Error(it.toApiError()) })
            _refreshing.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyOrdersScreen(onOpen: (String) -> Unit, onExplore: () -> Unit, vm: MyOrdersViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.load() }
    Scaffold(topBar = { TopAppBar(title = { Text("My orders") }) }) { padding ->
        PullToRefreshBox(refreshing, { vm.load(pull = true) }, Modifier.padding(padding)) {
            when (val s = state) {
                UiState.Loading -> LoadingBox()
                is UiState.Error -> ErrorBox(s.error, { vm.load() })
                is UiState.Success -> if (s.data.isEmpty()) {
                    EmptyState(Icons.Outlined.ShoppingBag, "No orders yet", "Order from a pharmacy near you and track it here.",
                        actionLabel = "Find a pharmacy", onAction = onExplore)
                } else LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(s.data, key = { it.id }) { OrderCard(it) { onOpen(it.id) } }
                }
            }
        }
    }
}

@HiltViewModel
class OrderTrackViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val orders: OrdersRepository,
    private val discovery: com.medassist.app.core.repository.DiscoveryRepository,
) : ViewModel() {
    private val _phone = MutableStateFlow<String?>(null)
    /** The pharmacy's phone, from its public profile (orders don't carry it). */
    val phone: StateFlow<String?> = _phone.asStateFlow()

    private val id: String = checkNotNull(savedState["id"])
    private val _state = MutableStateFlow<UiState<OrderView>>(UiState.Loading)
    val state: StateFlow<UiState<OrderView>> = _state.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        // Light polling while the order is open: no push service yet, and an order changes a few times at most.
        viewModelScope.launch {
            while (isActive) {
                load()
                val open = (_state.value as? UiState.Success)?.data?.status?.isOpen ?: true
                if (!open) break
                delay(20_000)
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            orders.myOrder(id).fold({
                _state.value = UiState.Success(it)
                if (_phone.value == null) discovery.pharmacy(it.branchId).onSuccess { p -> _phone.value = p.phone }
            }, { if (_state.value !is UiState.Success) _state.value = UiState.Error(it.toApiError()) })
        }
    }

    fun cancel() {
        viewModelScope.launch {
            orders.cancelMine(id).fold({ _state.value = UiState.Success(it) }, { _error.value = it.toApiError().message() })
        }
    }

    fun dismiss() {
        _error.value = null
    }
}

@Composable
fun OrderTrackScreen(onBack: () -> Unit, vm: OrderTrackViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val phone by vm.phone.collectAsStateWithLifecycle()
    var confirm by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    DetailScaffold((state as? UiState.Success)?.data?.orderNo ?: "Order", onBack) { padding ->
        Column(Modifier.padding(padding)) {
            StateContent(state, vm::load) { o ->
                Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
                        Column(Modifier.padding(16.dp)) {
                            Text(o.branchName ?: "Pharmacy", style = MaterialTheme.typography.titleMedium)
                            Text("Placed ${o.createdAt.asDateTime()}", style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(12.dp))
                            OrderTimeline(o)
                            if (o.status == OrderStatus.READY && o.fulfilment == Fulfilment.PICKUP) {
                                Text("Show order number ${o.orderNo} at the counter.", style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
                            }
                        }
                    }
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
                        Column(Modifier.padding(16.dp)) {
                            o.lines.forEach { KeyValueRow("${it.productName} × ${it.quantity}", (it.unitPrice * it.quantity.toBigDecimal()).rupees()) }
                            KeyValueRow(if (o.saleId != null) "Paid" else "To pay at pharmacy", o.estimatedTotal.rupees(), strong = true)
                            o.deliveryAddress?.let { Text("Deliver to: $it", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp)) }
                        }
                    }
                    if (o.status == OrderStatus.PLACED) {
                        OutlinedButton(onClick = { confirm = true }, modifier = Modifier.fillMaxWidth()) { Text("Cancel order") }
                    }
                    phone?.let { number ->
                        OutlinedButton(
                            onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Call the pharmacy") }
                    }
                }
            }
        }
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Cancel this order?") },
            confirmButton = { TextButton(onClick = { confirm = false; vm.cancel() }) { Text("Cancel order") } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Keep it") } },
        )
    }
    error?.let { AlertDialog(onDismissRequest = vm::dismiss, text = { Text(it) }, confirmButton = { TextButton(onClick = vm::dismiss) { Text("OK") } }) }
}
