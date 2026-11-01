package com.medassist.app.ui.pharmacy.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.medassist.app.core.model.SaleView
import com.medassist.app.core.model.toApiError
import com.medassist.app.core.repository.SalesRepository
import com.medassist.app.ui.common.DetailScaffold
import com.medassist.app.ui.common.EmptyState
import com.medassist.app.ui.common.StateContent
import com.medassist.app.ui.common.UiState
import com.medassist.app.ui.common.asDateTime
import com.medassist.app.ui.common.rupees
import com.medassist.app.ui.theme.Numeric
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
@HiltViewModel
class SalesHistoryViewModel @Inject constructor(private val sales: SalesRepository) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()
    private val _state = MutableStateFlow<UiState<List<SaleView>>>(UiState.Loading)
    val state: StateFlow<UiState<List<SaleView>>> = _state.asStateFlow()

    init {
        viewModelScope.launch { _query.debounce(300).collect { load() } }
    }

    fun onQuery(q: String) {
        _query.value = q
    }

    fun load() {
        viewModelScope.launch {
            sales.recentSales(null, _query.value).fold({ _state.value = UiState.Success(it.content) }, { _state.value = UiState.Error(it.toApiError()) })
        }
    }
}

@Composable
fun SalesHistoryScreen(onBack: () -> Unit, onOpen: (String) -> Unit, vm: SalesHistoryViewModel = hiltViewModel()) {
    val q by vm.query.collectAsStateWithLifecycle()
    val state by vm.state.collectAsStateWithLifecycle()
    DetailScaffold("Sales history", onBack) { padding ->
        Column(Modifier.padding(padding)) {
            OutlinedTextField(q, vm::onQuery, placeholder = { Text("Invoice number or customer") },
                leadingIcon = { Icon(Icons.Outlined.Search, null) }, singleLine = true, shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp))
            StateContent(state, vm::load) { list ->
                if (list.isEmpty()) EmptyState(Icons.AutoMirrored.Outlined.ReceiptLong, "No bills yet", "Bills you create appear here.")
                else LazyColumn {
                    items(list, key = { it.id }) { sale ->
                        ListItem(
                            modifier = Modifier.clickable { onOpen(sale.id) },
                            headlineContent = { Text(sale.invoiceNo) },
                            supportingContent = {
                                Text(listOfNotNull(sale.customerName, sale.createdAt.asDateTime(), "${sale.lines.size} item(s)").joinToString(" · "))
                            },
                            trailingContent = { Text(sale.total.rupees(), style = MaterialTheme.typography.titleSmall.merge(Numeric)) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
