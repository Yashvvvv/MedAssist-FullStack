package com.medassist.app.ui.pharmacy.more

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.medassist.app.core.model.OrganizationView
import com.medassist.app.core.model.Plan
import com.medassist.app.core.model.PlanView
import com.medassist.app.core.model.toApiError
import com.medassist.app.core.repository.PharmacyRepository
import com.medassist.app.ui.common.DetailScaffold
import com.medassist.app.ui.common.PrimaryButton
import com.medassist.app.ui.common.StateContent
import com.medassist.app.ui.common.StatusPill
import com.medassist.app.ui.common.Tone
import com.medassist.app.ui.common.UiState
import com.medassist.app.ui.common.asDateTime
import com.medassist.app.ui.common.message
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PlanData(val org: OrganizationView, val plans: List<PlanView>)

@HiltViewModel
class PlanViewModel @Inject constructor(private val pharmacy: PharmacyRepository) : ViewModel() {
    private val _state = MutableStateFlow<UiState<PlanData>>(UiState.Loading)
    val state: StateFlow<UiState<PlanData>> = _state.asStateFlow()
    private val _busy = MutableStateFlow<Plan?>(null)
    val busy: StateFlow<Plan?> = _busy.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            val org = pharmacy.organization()
            val plans = pharmacy.plans()
            _state.value = if (org.isSuccess && plans.isSuccess) UiState.Success(PlanData(org.getOrThrow(), plans.getOrThrow()))
            else UiState.Error((org.exceptionOrNull() ?: plans.exceptionOrNull())!!.toApiError())
        }
    }

    /** Manual billing for now: the switch is immediate. With Razorpay this becomes a checkout. */
    fun choose(plan: Plan) {
        viewModelScope.launch {
            _busy.value = plan
            pharmacy.changePlan(plan).fold({ load() }, { _error.value = it.toApiError().message() })
            _busy.value = null
        }
    }
}

private val FeatureNames = mapOf(
    "ONLINE_ORDERS" to "Online orders",
    "REFILL_REMINDERS" to "WhatsApp refill reminders",
    "ADVANCED_REPORTS" to "Sales, profit and GST reports",
    "STOCK_TRANSFERS" to "Stock transfers between branches",
    "AI_PRESCRIPTION_READER" to "AI prescription reader",
    "AUDIT_LOG_EXPORT" to "Audit log export",
)

@Composable
fun PlanScreen(onBack: () -> Unit, vm: PlanViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    DetailScaffold("Plan & billing", onBack) { padding ->
        Column(Modifier.padding(padding)) {
            StateContent(state, vm::load) { data ->
                val sub = data.org.subscription
                val usage = data.org.usage
                val limits = data.org.limits
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Current plan", style = MaterialTheme.typography.labelLarge)
                                Text(sub?.effectivePlan?.name ?: "-", style = MaterialTheme.typography.headlineMedium)
                                sub?.let {
                                    Text(when (it.status) {
                                        "TRIALING" -> "Free trial until ${it.trialEndsAt?.asDateTime() ?: "-"}"
                                        "PAST_DUE" -> "Payment overdue"
                                        "CANCELED" -> "Cancelled"
                                        else -> "Active"
                                    })
                                }
                                if (usage != null && limits != null) {
                                    Spacer(Modifier.height(12.dp))
                                    UsageBar("Branches", usage.branches, limits.maxBranches)
                                    UsageBar("Staff", usage.staffSeats, limits.maxStaff)
                                    UsageBar("Products", usage.products, limits.maxProducts)
                                    UsageBar("AI calls this month", usage.aiCallsThisMonth, limits.aiCallsPerMonth)
                                }
                            }
                        }
                    }
                    error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
                    items(data.plans, key = { it.plan.name }) { p ->
                        val current = p.plan == sub?.plan
                        Card(
                            border = if (current) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(p.plan.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                                    if (current) StatusPill("Current", Tone.Brand)
                                }
                                Text(if (p.pricePaisePerMonth == 0) "Free" else "₹${p.pricePaisePerMonth / 100} / month", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(8.dp))
                                Text("${p.maxBranches.limit()} branches · ${p.maxStaff.limit()} staff · ${p.maxProducts.limit()} products · ${p.aiCallsPerMonth} AI calls/month",
                                    style = MaterialTheme.typography.bodySmall)
                                p.features.mapNotNull { FeatureNames[it] }.forEach { Text("✓ $it", style = MaterialTheme.typography.bodyMedium) }
                                if (!current) {
                                    Spacer(Modifier.height(12.dp))
                                    if (p.plan == Plan.FREE) OutlinedButton(onClick = { vm.choose(p.plan) }, modifier = Modifier.fillMaxWidth()) { Text("Switch to Free") }
                                    else PrimaryButton("Choose ${p.plan.name}", { vm.choose(p.plan) }, Modifier.fillMaxWidth(), loading = busy == p.plan)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Int.limit(): String = if (this >= 1_000_000) "Unlimited" else toString()

@Composable
private fun UsageBar(label: String, used: Long, max: Int) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Row {
            Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            Text("$used / ${max.limit()}", style = MaterialTheme.typography.bodySmall)
        }
        LinearProgressIndicator(progress = { if (max >= 1_000_000) 0f else (used.toFloat() / max).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
    }
}
