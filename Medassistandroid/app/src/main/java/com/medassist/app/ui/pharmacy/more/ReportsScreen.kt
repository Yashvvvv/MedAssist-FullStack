package com.medassist.app.ui.pharmacy.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.medassist.app.core.model.ApiError
import com.medassist.app.core.model.GstSummary
import com.medassist.app.core.model.SalesSummary
import com.medassist.app.core.model.toApiError
import com.medassist.app.core.repository.InsightsRepository
import com.medassist.app.ui.common.paymentLabel
import com.medassist.app.ui.common.BarChart
import com.medassist.app.ui.common.DetailScaffold
import com.medassist.app.ui.common.EmptyState
import com.medassist.app.ui.common.ErrorBox
import com.medassist.app.ui.common.KeyValueRow
import com.medassist.app.ui.common.LoadingBox
import com.medassist.app.ui.common.SectionHeader
import com.medassist.app.ui.common.UiState
import com.medassist.app.ui.common.asShortDate
import com.medassist.app.ui.common.rupees
import com.medassist.app.ui.common.todayIst
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class Period(val label: String) { Today("Today"), Week("7 days"), Month("This month"), LastMonth("Last month") }

data class Reports(val sales: SalesSummary, val gst: GstSummary)

@HiltViewModel
class ReportsViewModel @Inject constructor(private val insights: InsightsRepository) : ViewModel() {
    private val _period = MutableStateFlow(Period.Month)
    val period: StateFlow<Period> = _period.asStateFlow()
    private val _state = MutableStateFlow<UiState<Reports>>(UiState.Loading)
    val state: StateFlow<UiState<Reports>> = _state.asStateFlow()

    init {
        load()
    }

    fun select(p: Period) {
        _period.value = p
        load()
    }

    fun load() {
        val today = todayIst()
        val (from, to) = when (_period.value) {
            Period.Today -> today to today
            Period.Week -> today.minusDays(6) to today
            Period.Month -> today.withDayOfMonth(1) to today
            Period.LastMonth -> today.minusMonths(1).withDayOfMonth(1).let { it to it.plusMonths(1).minusDays(1) }
        }
        viewModelScope.launch {
            _state.value = UiState.Loading
            val sales = async { insights.sales(from.toString(), to.toString()) }
            val gst = async { insights.gst(from.toString(), to.toString()) }
            val s = sales.await()
            val g = gst.await()
            _state.value = if (s.isSuccess && g.isSuccess) UiState.Success(Reports(s.getOrThrow(), g.getOrThrow()))
            else UiState.Error((s.exceptionOrNull() ?: g.exceptionOrNull())!!.toApiError())
        }
    }
}

@Composable
fun ReportsScreen(onBack: () -> Unit, onUpgrade: () -> Unit, vm: ReportsViewModel = hiltViewModel()) {
    val period by vm.period.collectAsStateWithLifecycle()
    val state by vm.state.collectAsStateWithLifecycle()
    DetailScaffold("Reports", onBack) { padding ->
        Column(Modifier.padding(padding)) {
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Period.entries.forEach { p -> FilterChip(selected = p == period, onClick = { vm.select(p) }, label = { Text(p.label) }) }
            }
            when (val s = state) {
                UiState.Loading -> LoadingBox()
                is UiState.Error -> if (s.error is ApiError.PlanLimit) {
                    EmptyState(Icons.Outlined.Lock, "Reports are on PRO", (s.error as ApiError.PlanLimit).message,
                        actionLabel = "See plans", onAction = onUpgrade)
                } else ErrorBox(s.error, vm::load)
                is UiState.Success -> ReportsBody(s.data)
            }
        }
    }
}

@Composable
private fun ReportsBody(r: Reports) {
    val s = r.sales
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            ReportCard("Sales") {
                KeyValueRow("Bills", s.invoices.toString())
                KeyValueRow("Gross sales", s.grossSales.rupees())
                KeyValueRow("Returns", "− ${s.returns.rupees()}")
                KeyValueRow("Net sales", s.netSales.rupees(), strong = true)
                KeyValueRow("Average bill", s.averageBill.rupees())
                if (s.byDay.size in 2..31) {
                    Spacer(Modifier.height(12.dp))
                    BarChart(
                        s.byDay.map { it.revenue.toFloat() },
                        s.byDay.map { it.date.asShortDate() },
                        axis = if (s.byDay.size > 7) s.byDay.first().date.asShortDate() to s.byDay.last().date.asShortDate() else null,
                    )
                }
            }
        }
        item {
            ReportCard("Profit") {
                KeyValueRow("Revenue before GST", (s.netSales - s.tax).rupees())
                KeyValueRow("Cost of goods sold", s.costOfGoods.rupees())
                KeyValueRow("Gross profit", s.grossProfit.rupees(), strong = true)
                Text("Cost is the purchase price of the exact batch each item came from.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (s.byPaymentMode.isNotEmpty()) item {
            ReportCard("Payments") {
                s.byPaymentMode.forEach { KeyValueRow("${paymentLabel(it.paymentMode)} (${it.invoices})", it.amount.rupees()) }
            }
        }
        if (s.topProducts.isNotEmpty()) item {
            ReportCard("Top products") {
                s.topProducts.forEach { KeyValueRow("${it.name} × ${it.quantity}", it.revenue.rupees()) }
            }
        }
        item {
            ReportCard("GST (outward supplies)") {
                r.gst.gstin?.let { Text("GSTIN $it", style = MaterialTheme.typography.bodySmall) }
                r.gst.byRate.forEach { row ->
                    KeyValueRow("${row.rate.stripTrailingZeros().toPlainString()}% on ${row.netTaxable.rupees()}", row.netTax.rupees())
                }
                KeyValueRow("Taxable value", r.gst.totalNetTaxable.rupees())
                KeyValueRow("Total GST", r.gst.totalNetTax.rupees(), strong = true)
                Text("Net of credit notes. Export the HSN summary as CSV from the web dashboard for GSTR-1.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ReportCard(title: String, content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
        Column(Modifier.padding(16.dp)) {
            SectionHeader(title)
            content()
        }
    }
}
