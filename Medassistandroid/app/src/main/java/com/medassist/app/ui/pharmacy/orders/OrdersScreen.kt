package com.medassist.app.ui.pharmacy.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeliveryDining
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.medassist.app.core.model.Fulfilment
import com.medassist.app.core.model.OrderStatus
import com.medassist.app.core.model.OrderView
import com.medassist.app.core.model.toApiError
import com.medassist.app.core.repository.OrdersRepository
import com.medassist.app.ui.common.EmptyState
import com.medassist.app.ui.common.ErrorBox
import com.medassist.app.ui.common.LoadingBox
import com.medassist.app.ui.common.OrderStatusPill
import com.medassist.app.ui.common.UiState
import com.medassist.app.ui.common.asRelative
import com.medassist.app.ui.common.rupees
import com.medassist.app.ui.theme.Numeric
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class OrderFilter(val label: String, val status: OrderStatus?) {
    New("New", OrderStatus.PLACED),
    Preparing("Preparing", OrderStatus.ACCEPTED),
    Ready("Ready", OrderStatus.READY),
    All("All", null),
}

@HiltViewModel
class OrdersViewModel @Inject constructor(private val orders: OrdersRepository) : ViewModel() {
    private val _filter = MutableStateFlow(OrderFilter.New)
    val filter: StateFlow<OrderFilter> = _filter.asStateFlow()
    private val _state = MutableStateFlow<UiState<List<OrderView>>>(UiState.Loading)
    val state: StateFlow<UiState<List<OrderView>>> = _state.asStateFlow()
    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    fun select(f: OrderFilter) {
        _filter.value = f
        load()
    }

    fun load(pull: Boolean = false) {
        viewModelScope.launch {
            if (pull) _refreshing.value = true else _state.value = UiState.Loading
            orders.queue(_filter.value.status).fold(
                { _state.value = UiState.Success(it.content) },
                { _state.value = UiState.Error(it.toApiError()) },
            )
            _refreshing.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(onOpen: (String) -> Unit, vm: OrdersViewModel = hiltViewModel()) {
    val filter by vm.filter.collectAsStateWithLifecycle()
    val state by vm.state.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.load() } // also refreshes when coming back from an order

    Scaffold(topBar = { TopAppBar(title = { Text("Online orders") }) }) { padding ->
        Column(Modifier.padding(padding)) {
            PrimaryTabRow(selectedTabIndex = filter.ordinal) {
                OrderFilter.entries.forEach { f -> Tab(selected = f == filter, onClick = { vm.select(f) }, text = { Text(f.label) }) }
            }
            PullToRefreshBox(refreshing, { vm.load(pull = true) }, Modifier.fillMaxSize()) {
                when (val s = state) {
                    UiState.Loading -> LoadingBox()
                    is UiState.Error -> ErrorBox(s.error, { vm.load() })
                    is UiState.Success -> if (s.data.isEmpty()) {
                        EmptyState(Icons.Outlined.ShoppingBag, "Nothing here",
                            if (filter == OrderFilter.New) "New orders from customers nearby show up here." else "No orders in this list.")
                    } else {
                        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(s.data, key = { it.id }) { OrderCard(it) { onOpen(it.id) } }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrderCard(order: OrderView, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (order.fulfilment == Fulfilment.DELIVERY) Icons.Outlined.DeliveryDining else Icons.Outlined.Storefront,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.padding(start = 8.dp))
                Text(order.orderNo, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                OrderStatusPill(order)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                order.lines.joinToString { "${it.productName} × ${it.quantity}" },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
            )
            Spacer(Modifier.height(8.dp))
            Row {
                Text(
                    listOfNotNull(order.branchName, order.createdAt.asRelative(), if (order.prescriptionId != null) "Rx attached" else null)
                        .joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(order.estimatedTotal.rupees(), style = MaterialTheme.typography.titleSmall.merge(Numeric))
            }
        }
    }
}
