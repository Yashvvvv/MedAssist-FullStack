package com.medassist.app.ui.pharmacy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.PointOfSale
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.medassist.app.core.auth.Session
import com.medassist.app.core.auth.SessionManager
import com.medassist.app.core.model.BranchView
import com.medassist.app.core.model.Dashboard
import com.medassist.app.core.model.OrganizationView
import com.medassist.app.core.model.toApiError
import com.medassist.app.core.repository.InsightsRepository
import com.medassist.app.core.repository.PharmacyRepository
import com.medassist.app.data.PharmacyContext
import com.medassist.app.ui.common.BarChart
import com.medassist.app.ui.common.ErrorBox
import com.medassist.app.ui.common.LoadingBox
import com.medassist.app.ui.common.StatCard
import com.medassist.app.ui.common.StatusPill
import com.medassist.app.ui.common.Tone
import com.medassist.app.ui.common.UiState
import com.medassist.app.ui.common.asDateTime
import com.medassist.app.ui.common.rupeesRounded
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val insights: InsightsRepository,
    private val pharmacy: PharmacyRepository,
    private val context: PharmacyContext,
    session: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<Dashboard>>(UiState.Loading)
    val state: StateFlow<UiState<Dashboard>> = _state.asStateFlow()

    private val _org = MutableStateFlow<OrganizationView?>(null)
    val org: StateFlow<OrganizationView?> = _org.asStateFlow()

    private val _unread = MutableStateFlow(0L)
    val unread: StateFlow<Long> = _unread.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    val branches = context.branches
    val branch = context.branch
    val userName: StateFlow<String> = session.session
        .map { (it as? Session.SignedIn)?.tokens?.fullName?.substringBefore(" ").orEmpty() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    /** "Whole pharmacy" (null) or one branch; the dashboard is the one screen that can show all branches. */
    private val _scope = MutableStateFlow<String?>(null)
    val scope: StateFlow<String?> = _scope.asStateFlow()

    init {
        load()
    }

    fun load(pull: Boolean = false) {
        viewModelScope.launch {
            if (pull) _refreshing.value = true else if (_state.value !is UiState.Success) _state.value = UiState.Loading
            insights.dashboard(_scope.value).fold(
                onSuccess = { _state.value = UiState.Success(it) },
                onFailure = { if (_state.value !is UiState.Success) _state.value = UiState.Error(it.toApiError()) },
            )
            pharmacy.organization().onSuccess { _org.value = it }
            insights.unreadCount().onSuccess { _unread.value = it }
            _refreshing.value = false
        }
    }

    fun selectScope(branchId: String?) {
        _scope.value = branchId
        branchId?.let(context::select)
        load()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onOpenOrders: () -> Unit,
    onOpenStock: () -> Unit,
    onSell: () -> Unit,
    onNotifications: () -> Unit,
    onReports: () -> Unit,
    vm: DashboardViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val org by vm.org.collectAsStateWithLifecycle()
    val unread by vm.unread.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    val branches by vm.branches.collectAsStateWithLifecycle()
    val scope by vm.scope.collectAsStateWithLifecycle()
    val name by vm.userName.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.load(pull = true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(org?.name ?: "Your pharmacy", style = MaterialTheme.typography.titleLarge)
                        BranchPicker(branches, scope, vm::selectScope)
                    }
                },
                actions = {
                    IconButton(onClick = onNotifications) {
                        BadgedBox(badge = { if (unread > 0) Badge { Text(if (unread > 99) "99+" else unread.toString()) } }) {
                            Icon(Icons.Outlined.Notifications, "Notifications")
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onSell, icon = { Icon(Icons.Rounded.PointOfSale, null) }, text = { Text("New bill") })
        },
    ) { padding ->
        PullToRefreshBox(isRefreshing = refreshing, onRefresh = { vm.load(pull = true) }, modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val s = state) {
                UiState.Loading -> LoadingBox()
                is UiState.Error -> ErrorBox(s.error, { vm.load() })
                is UiState.Success -> DashboardContent(s.data, org, name, onOpenOrders, onOpenStock, onReports)
            }
        }
    }
}

@Composable
private fun BranchPicker(branches: List<BranchView>, selected: String?, onSelect: (String?) -> Unit) {
    if (branches.size < 2) return
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }, contentPadding = PaddingValues(0.dp)) {
            Text(branches.firstOrNull { it.id == selected }?.name ?: "All branches", style = MaterialTheme.typography.labelLarge)
            Icon(Icons.Rounded.ExpandMore, null)
        }
        DropdownMenu(open, { open = false }) {
            DropdownMenuItem(text = { Text("All branches") }, onClick = { open = false; onSelect(null) })
            branches.forEach { b -> DropdownMenuItem(text = { Text(b.name) }, onClick = { open = false; onSelect(b.id) }) }
        }
    }
}

@Composable
private fun DashboardContent(
    d: Dashboard,
    org: OrganizationView?,
    name: String,
    onOpenOrders: () -> Unit,
    onOpenStock: () -> Unit,
    onReports: () -> Unit,
) {
    LazyColumn(contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(greeting(name), style = MaterialTheme.typography.headlineSmall)
            org?.subscription?.let { sub ->
                if (sub.status == "TRIALING" && sub.trialEndsAt != null) {
                    Spacer(Modifier.height(6.dp))
                    StatusPill("PRO trial until ${sub.trialEndsAt.asDateTime().substringBefore(",")}", Tone.Brand)
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)) {
                Column(Modifier.padding(20.dp)) {
                    Text("Today's sales", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                    Text(d.salesToday.rupeesRounded(), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onPrimary)
                    Text(
                        "${d.invoicesToday} bills · ${d.salesMonthToDate.rupeesRounded()} this month",
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                    )
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Open orders", d.openOrders.toString(), Icons.Outlined.ShoppingBag, Modifier.weight(1f),
                    tone = if (d.openOrders > 0) Tone.Warning else Tone.Brand, onClick = onOpenOrders)
                StatCard("Low stock", d.lowStockProducts.toString(), Icons.Outlined.Warning, Modifier.weight(1f),
                    tone = if (d.lowStockProducts > 0) Tone.Danger else Tone.Brand, onClick = onOpenStock)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Expiring in 30 days", d.batchesExpiringIn30Days.toString(), Icons.Outlined.EventBusy, Modifier.weight(1f),
                    tone = if (d.batchesExpiringIn30Days > 0) Tone.Warning else Tone.Brand,
                    caption = "${d.valueExpiringIn30Days.rupeesRounded()} at cost", onClick = onOpenStock)
                StatCard("Profit this month", d.grossProfitMonthToDate.rupeesRounded(), Icons.Outlined.TrendingUp, Modifier.weight(1f),
                    tone = Tone.Success, caption = "before tax", onClick = onReports)
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Last 7 days", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        TextButton(onClick = onReports) { Text("Reports") }
                    }
                    Spacer(Modifier.height(8.dp))
                    BarChart(
                        values = d.last7Days.map { it.revenue.toFloat() },
                        labels = d.last7Days.map { dayLabel(it.date) },
                    )
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Stock value", d.stockValueAtCost.rupeesRounded(), Icons.Outlined.Inventory, Modifier.weight(1f), caption = "at cost")
                StatCard("Refills due", d.refillsDueIn3Days.toString(), Icons.Outlined.Receipt, Modifier.weight(1f), caption = "next 3 days")
            }
        }
    }
}

private fun greeting(name: String): String {
    val hour = java.time.LocalTime.now().hour
    val part = when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
    return if (name.isBlank()) part else "$part, $name"
}

private fun dayLabel(iso: String): String =
    runCatching { LocalDate.parse(iso).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH) }.getOrDefault("")

@Suppress("unused")
private val unusedWidth = Modifier.width(0.dp)
