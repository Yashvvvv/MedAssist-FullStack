package com.medassist.app.ui.pharmacy.stock

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.medassist.app.core.model.MedicineView
import com.medassist.app.core.model.toApiError
import com.medassist.app.core.repository.StockRepository
import com.medassist.app.ui.common.DetailScaffold
import com.medassist.app.ui.common.PrimaryButton
import com.medassist.app.ui.common.RxBadge
import com.medassist.app.ui.common.message
import com.medassist.app.ui.common.rupees
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ImportState(
    val query: String = "",
    val results: List<MedicineView> = emptyList(),
    val selected: Set<String> = emptySet(),
    val busy: Boolean = false,
    val message: String? = null,
)

/** Builds a pharmacy's catalogue from the shared medicine master in a few taps. */
@OptIn(FlowPreview::class)
@HiltViewModel
class ImportMedicinesViewModel @Inject constructor(private val stock: StockRepository) : ViewModel() {
    private val _state = MutableStateFlow(ImportState())
    val state: StateFlow<ImportState> = _state.asStateFlow()
    private val queries = MutableStateFlow("a")

    init {
        viewModelScope.launch {
            queries.debounce(250).collect { q ->
                stock.searchMaster(q.ifBlank { "a" }).onSuccess { p -> _state.update { it.copy(results = p.content) } }
            }
        }
    }

    fun onQuery(q: String) {
        _state.update { it.copy(query = q) }
        queries.value = q
    }

    fun toggle(id: String) = _state.update { it.copy(selected = if (id in it.selected) it.selected - id else it.selected + id) }

    fun import() {
        val ids = _state.value.selected.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            stock.importFromMaster(ids).fold(
                { r ->
                    val skipped = if (r.skippedAlreadyInCatalog > 0) ", ${r.skippedAlreadyInCatalog} were already there" else ""
                    _state.update { it.copy(busy = false, selected = emptySet(), message = "Added ${r.imported} product(s)$skipped. Receive stock to start selling.") }
                },
                { e -> _state.update { it.copy(busy = false, message = e.toApiError().message()) } },
            )
        }
    }
}

@Composable
fun ImportMedicinesScreen(onBack: () -> Unit, vm: ImportMedicinesViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    DetailScaffold(
        "Add from catalogue",
        onBack,
        bottomBar = {
            if (s.selected.isNotEmpty()) {
                Surface(tonalElevation = 3.dp) {
                    PrimaryButton("Add ${s.selected.size} to my catalogue", vm::import,
                        Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), loading = s.busy)
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                s.query, vm::onQuery,
                placeholder = { Text("Search by brand or salt, e.g. paracetamol") },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp),
            )
            s.message?.let { Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 16.dp)) }
            LazyColumn {
                items(s.results, key = { it.id }) { m ->
                    ListItem(
                        modifier = Modifier.clickable { vm.toggle(m.id) },
                        leadingContent = { Checkbox(m.id in s.selected, { vm.toggle(m.id) }) },
                        headlineContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(m.name)
                                if (m.prescriptionRequired) {
                                    Spacer(Modifier.width(8.dp))
                                    RxBadge()
                                }
                            }
                        },
                        supportingContent = { Text(listOfNotNull(m.saltComposition, m.manufacturer, m.packSize).joinToString(" · ")) },
                        trailingContent = { m.mrp?.let { Text(it.rupees()) } },
                    )
                }
            }
        }
    }
}
