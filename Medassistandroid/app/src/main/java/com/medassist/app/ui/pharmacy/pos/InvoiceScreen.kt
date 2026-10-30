package com.medassist.app.ui.pharmacy.pos

import android.content.Intent
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
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.medassist.app.core.model.SaleView
import com.medassist.app.core.model.toApiError
import com.medassist.app.core.repository.SalesRepository
import com.medassist.app.ui.common.paymentLabel
import com.medassist.app.ui.common.DetailScaffold
import com.medassist.app.ui.common.KeyValueRow
import com.medassist.app.ui.common.RxBadge
import com.medassist.app.ui.common.StateContent
import com.medassist.app.ui.common.StatusPill
import com.medassist.app.ui.common.Tone
import com.medassist.app.ui.common.UiState
import com.medassist.app.ui.common.asDate
import com.medassist.app.ui.common.asDateTime
import com.medassist.app.ui.common.rupees
import com.medassist.app.ui.theme.LocalStatusColors
import com.medassist.app.ui.theme.Numeric
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class InvoiceViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val sales: SalesRepository,
) : ViewModel() {
    private val id: String = checkNotNull(savedState["id"])
    private val _state = MutableStateFlow<UiState<SaleView>>(UiState.Loading)
    val state: StateFlow<UiState<SaleView>> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            sales.sale(id).fold({ _state.value = UiState.Success(it) }, { _state.value = UiState.Error(it.toApiError()) })
        }
    }
}

@Composable
fun InvoiceScreen(onBack: () -> Unit, vm: InvoiceViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    DetailScaffold(
        title = "Invoice",
        onBack = onBack,
        actions = {
            (state as? UiState.Success)?.data?.let { sale ->
                IconButton(onClick = {
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, sale.asText())
                    context.startActivity(Intent.createChooser(send, "Share invoice"))
                }) { Icon(Icons.Outlined.Share, "Share") }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            StateContent(state, vm::load) { sale -> InvoiceBody(sale) }
        }
    }
}

@Composable
private fun InvoiceBody(sale: SaleView) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = LocalStatusColors.current.successContainer)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = LocalStatusColors.current.success)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(sale.invoiceNo, style = MaterialTheme.typography.titleMedium)
                        Text("${sale.createdAt.asDateTime()} · ${paymentLabel(sale.paymentMode)}",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    Text(sale.total.rupees(), style = MaterialTheme.typography.titleLarge.merge(Numeric))
                }
            }
        }
        sale.customerName?.let { item { KeyValueRow("Customer", it) } }
        if (sale.warnings.isNotEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = LocalStatusColors.current.warningContainer)) {
                    Column(Modifier.padding(12.dp)) { sale.warnings.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) } }
                }
            }
        }
        items(sale.lines, key = { it.id }) { line ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(line.productName, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        if (line.schedule.requiresPrescription) RxBadge()
                    }
                    Text(
                        "Batch ${line.batchNo ?: "-"} · exp ${line.expiryDate?.asDate() ?: "-"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    KeyValueRow("${line.quantity} × ${line.unitPrice.rupees()}", line.lineTotal.rupees())
                    KeyValueRow("Taxable / GST ${line.gstRate.stripTrailingZeros().toPlainString()}%",
                        "${line.taxableValue.rupees()} + ${(line.cgst + line.sgst).rupees()}")
                    if (line.returnedQty > 0) StatusPill("${line.returnedQty} returned", Tone.Warning)
                }
            }
        }
        item {
            Column {
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                KeyValueRow("Taxable value", sale.subtotal.rupees())
                if (sale.discountTotal.signum() > 0) KeyValueRow("Discount", "− ${sale.discountTotal.rupees()}")
                KeyValueRow("GST (CGST + SGST)", sale.taxTotal.rupees())
                KeyValueRow("Total", sale.total.rupees(), strong = true)
            }
        }
    }
}

/** Plain-text invoice for WhatsApp/SMS. */
private fun SaleView.asText(): String = buildString {
    appendLine("Invoice $invoiceNo")
    appendLine(createdAt.asDateTime())
    customerName?.let { appendLine("Customer: $it") }
    appendLine()
    lines.forEach { appendLine("${it.productName} × ${it.quantity}  ${it.lineTotal.rupees()}") }
    appendLine()
    appendLine("GST included: ${taxTotal.rupees()}")
    append("Total: ${total.rupees()}")
}
