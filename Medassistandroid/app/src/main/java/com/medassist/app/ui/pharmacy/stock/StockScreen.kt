package com.medassist.app.ui.pharmacy.stock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LibraryAdd
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.medassist.app.core.model.ExpiringBatchView
import com.medassist.app.core.model.StockLevelView
import com.medassist.app.core.model.toApiError
import com.medassist.app.core.repository.StockRepository
import com.medassist.app.data.PharmacyContext
import com.medassist.app.ui.common.EmptyState
import com.medassist.app.ui.common.ErrorBox
import com.medassist.app.ui.common.LoadingBox
import com.medassist.app.ui.common.StatusPill
import com.medassist.app.ui.common.Tone
import com.medassist.app.ui.common.UiState
import com.medassist.app.ui.common.asDate
import com.medassist.app.ui.common.rupees
import com.medassist.app.ui.common.rupeesRounded
import com.medassist.app.ui.theme.Numeric
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class StockTab(val label: String) { All("All stock"), Low("Low stock"), Expiring("Expiring") }

sealed interface StockRows {
    data class Levels(val rows: List<StockLevelView>) : StockRows
    data class Expiring(val rows: List<ExpiringBatchView>) : StockRows
}

@OptIn(FlowPreview::class)
@HiltViewModel
class StockViewModel @Inject constructor(
    private val stock: StockRepository,
    private val pharmacy: PharmacyContext,
) : ViewModel() {
    private val _tab = MutableStateFlow(StockTab.All)
    val tab: StateFlow<StockTab> = _tab.asStateFlow()
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()
    private val _state = MutableStateFlow<UiState<StockRows>>(UiState.Loading)
    val state: StateFlow<UiState<StockRows>> = _state.asStateFlow()
    val branch = pharmacy.branch
    private var job: Job? = null

    init {
        viewModelScope.launch { _query.debounce(300).collect { load() } }
    }

    fun select(t: StockTab) {
        _tab.value = t
        load()
    }

    fun onQuery(q: String) {
        _query.value = q
    }

    fun load() {
        job?.cancel()
        job = viewModelScope.launch {
            if (_state.value !is UiState.Success) _state.value = UiState.Loading
            val branchId = pharmacy.branch.filterNotNull().first().id
            val result: Result<StockRows> = when (_tab.value) {
                StockTab.All -> stock.levels(branchId, _query.value).map { StockRows.Levels(it.content) }
                StockTab.Low -> stock.lowStock(branchId).map { StockRows.Levels(it.content) }
                StockTab.Expiring -> stock.expiring(branchId, 90).map { StockRows.Expiring(it.content) }
            }
            result.fold({ _state.value = UiState.Success(it) }, { _state.value = UiState.Error(it.toApiError()) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockScreen(onReceive: () -> Unit, onImport: () -> Unit, vm: StockViewModel = hiltViewModel()) {
    val tab by vm.tab.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val state by vm.state.collectAsStateWithLifecycle()
    val branch by vm.branch.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Stock")
                        branch?.let { Text(it.name, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                },
                actions = { IconButton(onClick = onImport) { Icon(Icons.Outlined.LibraryAdd, "Add from catalogue") } },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onReceive, icon = { Icon(Icons.Rounded.Add, null) }, text = { Text("Receive stock") })
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            PrimaryTabRow(selectedTabIndex = tab.ordinal) {
                StockTab.entries.forEach { t -> Tab(selected = t == tab, onClick = { vm.select(t) }, text = { Text(t.label) }) }
            }
            if (tab == StockTab.All) {
                OutlinedTextField(
                    query, vm::onQuery,
                    placeholder = { Text("Search stock") },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            when (val s = state) {
                UiState.Loading -> LoadingBox()
                is UiState.Error -> ErrorBox(s.error, vm::load)
                is UiState.Success -> when (val rows = s.data) {
                    is StockRows.Levels -> if (rows.rows.isEmpty()) {
                        if (tab == StockTab.Low) EmptyState(Icons.Outlined.Inventory2, "All stocked up", "Nothing is at or below its reorder level.")
                        else EmptyState(Icons.Outlined.Inventory2, "No stock yet", "Add medicines from the catalogue, then receive stock.", actionLabel = "Add from catalogue", onAction = onImport)
                    } else LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                        items(rows.rows, key = { it.productId }) { LevelRow(it); HorizontalDivider() }
                    }
                    is StockRows.Expiring -> if (rows.rows.isEmpty()) {
                        EmptyState(Icons.Outlined.Inventory2, "Nothing expiring", "No batches expire in the next 90 days.")
                    } else LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                        item {
                            val total = rows.rows.fold(java.math.BigDecimal.ZERO) { a, r -> a + r.valueAtCost }
                            Text("${total.rupeesRounded()} at cost expires in 90 days. Return it to the distributor or sell it first.",
                                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp))
                        }
                        items(rows.rows, key = { it.batch.id }) { ExpiringRow(it); HorizontalDivider() }
                    }
                }
            }
        }
    }
}

@Composable
private fun LevelRow(r: StockLevelView) {
    ListItem(
        headlineContent = { Text(r.productName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(listOfNotNull("Reorder at ${r.reorderLevel}", r.nearestExpiry?.let { "next expiry ${it.asDate()}" }).joinToString(" · "))
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text(r.quantity.toString(), style = MaterialTheme.typography.titleMedium.merge(Numeric))
                if (r.quantity == 0L) StatusPill("Out", Tone.Danger)
                else if (r.belowReorderLevel) StatusPill("Low", Tone.Warning)
            }
        },
    )
}

@Composable
private fun ExpiringRow(r: ExpiringBatchView) {
    val b = r.batch
    ListItem(
        headlineContent = { Text(r.productName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text("Batch ${b.batchNo} · ${b.quantity} units · ${r.valueAtCost.rupees()}") },
        trailingContent = {
            Row(horizontalArrangement = Arrangement.End) {
                StatusPill(
                    if (b.expired) "Expired" else "${b.daysToExpiry} days",
                    when {
                        b.expired || b.daysToExpiry <= 30 -> Tone.Danger
                        b.daysToExpiry <= 60 -> Tone.Warning
                        else -> Tone.Neutral
                    },
                )
            }
        },
    )
}

@Suppress("unused")
private val fill = Modifier.fillMaxSize()
