package com.medassist.app.ui.pharmacy.stock

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.medassist.app.core.model.ProductView
import com.medassist.app.core.model.ReceiveStockRequest
import com.medassist.app.core.model.toApiError
import com.medassist.app.core.repository.StockRepository
import com.medassist.app.data.PharmacyContext
import com.medassist.app.ui.common.DetailScaffold
import com.medassist.app.ui.common.PrimaryButton
import com.medassist.app.ui.common.asDate
import com.medassist.app.ui.common.message
import com.medassist.app.ui.common.rupees
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReceiveState(
    val query: String = "",
    val results: List<ProductView> = emptyList(),
    val product: ProductView? = null,
    val batchNo: String = "",
    val expiry: String? = null,
    val quantity: String = "",
    val cost: String = "",
    val busy: Boolean = false,
    val error: String? = null,
    val saved: String? = null,
)

@OptIn(FlowPreview::class)
@HiltViewModel
class ReceiveStockViewModel @Inject constructor(
    private val stock: StockRepository,
    private val pharmacy: PharmacyContext,
) : ViewModel() {
    private val _state = MutableStateFlow(ReceiveState())
    val state: StateFlow<ReceiveState> = _state.asStateFlow()
    private val queries = MutableStateFlow("")

    init {
        viewModelScope.launch {
            queries.debounce(250).collect { q ->
                stock.products(q.ifBlank { null }, size = 20).onSuccess { p -> _state.update { it.copy(results = p.content) } }
            }
        }
    }

    fun onQuery(q: String) {
        _state.update { it.copy(query = q) }
        queries.value = q
    }

    fun pick(p: ProductView?) = _state.update { it.copy(product = p, saved = null) }
    fun update(transform: (ReceiveState) -> ReceiveState) = _state.update { transform(it).copy(error = null) }

    fun save() {
        val s = _state.value
        val branch = pharmacy.branch.value ?: return
        val product = s.product ?: return
        val qty = s.quantity.toIntOrNull()
        val cost = s.cost.toBigDecimalOrNull()
        val error = when {
            s.batchNo.isBlank() -> "Enter the batch number printed on the strip"
            s.expiry == null -> "Pick the expiry date"
            LocalDate.parse(s.expiry) <= LocalDate.now() -> "That batch has already expired"
            qty == null || qty <= 0 -> "Enter how many units came in"
            cost == null || cost <= BigDecimal.ZERO -> "Enter the purchase price per unit"
            else -> null
        }
        if (error != null) return _state.update { it.copy(error = error) }
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            stock.receive(ReceiveStockRequest(branch.id, product.id, s.batchNo.trim().uppercase(), s.expiry!!, qty!!, cost!!))
                .onSuccess { _state.value = ReceiveState(saved = "Added $qty × ${product.name} (batch ${it.batchNo})") }
                .onFailure { e -> _state.update { it.copy(busy = false, error = e.toApiError().message()) } }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiveStockScreen(onBack: () -> Unit, vm: ReceiveStockViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    var pickingDate by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { vm.onQuery("") }

    DetailScaffold("Receive stock", onBack) { padding ->
        Column(
            Modifier.padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            s.saved?.let {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Text("✓ $it", Modifier.padding(14.dp))
                }
            }
            val product = s.product
            if (product == null) {
                Text("Which product?", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(s.query, vm::onQuery, placeholder = { Text("Search your catalogue") },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                s.results.forEach { p ->
                    ListItem(
                        headlineContent = { Text(p.name) },
                        supportingContent = { Text("MRP ${p.mrp.rupees()}${p.packSize?.let { " · $it" } ?: ""}") },
                        modifier = Modifier.clickable { vm.pick(p) },
                    )
                }
            } else {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
                    Row(Modifier.padding(start = 16.dp)) {
                        Column(Modifier.weight(1f).padding(vertical = 12.dp)) {
                            Text(product.name, style = MaterialTheme.typography.titleMedium)
                            Text("MRP ${product.mrp.rupees()} · GST ${product.gstRate.stripTrailingZeros().toPlainString()}%",
                                style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { vm.pick(null) }) { Icon(Icons.Rounded.Close, "Change product") }
                    }
                }
                OutlinedTextField(s.batchNo, { v -> vm.update { it.copy(batchNo = v) } }, label = { Text("Batch number") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = s.expiry?.asDate() ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Expiry date") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = { TextButton(onClick = { pickingDate = true }) { Text("Pick") } },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(s.quantity, { v -> vm.update { it.copy(quantity = v.filter(Char::isDigit).take(6)) } },
                        label = { Text("Units") }, singleLine = true, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(s.cost, { v -> vm.update { it.copy(cost = v.filter { c -> c.isDigit() || c == '.' }.take(10)) } },
                        label = { Text("Cost per unit (₹)") }, singleLine = true, modifier = Modifier.weight(1f),
                        supportingText = { Text("Before GST") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                }
                s.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Spacer(Modifier.height(8.dp))
                PrimaryButton("Add to stock", vm::save, Modifier.fillMaxWidth(), loading = s.busy)
            }
        }
    }

    if (pickingDate) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = LocalDate.now().plusYears(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let { ms ->
                        val date = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
                        vm.update { it.copy(expiry = date.toString()) }
                    }
                    pickingDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickingDate = false }) { Text("Cancel") } },
        ) { DatePicker(picker) }
    }
}
