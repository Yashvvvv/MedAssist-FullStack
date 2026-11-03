package com.medassist.app.ui.pharmacy

import androidx.compose.material3.FloatingActionButtonDefaults
import com.medassist.app.ui.theme.OnStaffHeader
import com.medassist.app.ui.theme.Mono
import com.medassist.app.ui.theme.AppButtonShape
import com.medassist.app.ui.common.appBarColors
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.rounded.LocalPharmacy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import com.medassist.app.ui.common.AppCard
import com.medassist.app.ui.theme.LocalStatusColors
import com.medassist.app.ui.theme.Numeric
import java.math.BigDecimal
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
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.PointOfSale
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
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
import kotlinx.coroutines.launch

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val insights: InsightsRepository,
    private val pharmacy: PharmacyRepository,
    private val context: PharmacyContext,
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
    LaunchedEffect(Unit) { vm.load(pull = true) }

    Scaffold(
        topBar = {
            // Carbon UI shell: product name, then the org in the header's secondary grey.
            TopAppBar(
                colors = appBarColors(),
                title = {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("MedAssist", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.width(8.dp))
                        Text(org?.name ?: "", style = MaterialTheme.typography.bodyMedium, color = OnStaffHeader.copy(alpha = 0.72f),
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
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
            ExtendedFloatingActionButton(
                onClick = onSell,
                shape = AppButtonShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                icon = { Icon(Icons.Rounded.PointOfSale, null) },
                text = { Text("New bill") },
            )
        },
    ) { padding ->
        PullToRefreshBox(isRefreshing = refreshing, onRefresh = { vm.load(pull = true) }, modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val s = state) {
                UiState.Loading -> LoadingBox()
                is UiState.Error -> ErrorBox(s.error, { vm.load() })
                is UiState.Success -> DashboardContent(s.data, org, branches, scope, vm::selectScope, onOpenOrders, onOpenStock, onReports)
            }
        }
    }
}

/** Carbon breadcrumb: "Branch / Overview", with the branch as a dropdown when there's more than one. */
@Composable
private fun Breadcrumb(branches: List<BranchView>, selected: String?, onSelect: (String?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val name = branches.firstOrNull { it.id == selected }?.name ?: if (branches.size == 1) branches.first().name else "All branches"
    Box {
        Row(
            Modifier.then(if (branches.size > 1) Modifier.clickable { open = true } else Modifier).padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(name, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            if (branches.size > 1) Icon(Icons.Rounded.ExpandMore, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            Text("  /  Overview", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(open, { open = false }) {
            DropdownMenuItem(text = { Text("All branches") }, onClick = { open = false; onSelect(null) })
            branches.forEach { b -> DropdownMenuItem(text = { Text(b.name) }, onClick = { open = false; onSelect(b.id) }) }
        }
    }
}

/**
 * Carbon Grid dashboard: a breadcrumb and a light "Today" title, a full-bleed 2×2 tile grid on 1dp gutters,
 * the week's revenue on solid gridlines, then a work-queue table. Every tile and row is a shortcut.
 */
@Composable
private fun DashboardContent(
    d: Dashboard,
    org: OrganizationView?,
    branches: List<BranchView>,
    scope: String?,
    onScope: (String?) -> Unit,
    onOpenOrders: () -> Unit,
    onOpenStock: () -> Unit,
    onReports: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
        item {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 12.dp)) {
                Breadcrumb(branches, scope, onScope)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Today", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                    org?.subscription?.let { sub ->
                        if (sub.status == "TRIALING" && sub.trialEndsAt != null) {
                            StatusPill("PRO trial · ${sub.trialEndsAt.asDateTime().substringBefore(",")}", Tone.Brand)
                        }
                    }
                }
            }
        }
        item {
            // 1dp gutters: the grid's background shows through as hairlines.
            Column(Modifier.fillMaxWidth().background(scheme.outlineVariant).padding(vertical = 1.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                    Tile("Today's sales", d.salesToday.rupeesRounded(), "${d.invoicesToday} bills · MTD ${d.salesMonthToDate.rupeesRounded()}",
                        null, Modifier.weight(1f), onReports)
                    Tile("Gross profit (MTD)", d.grossProfitMonthToDate.rupeesRounded(), "before tax", null, Modifier.weight(1f), onReports)
                }
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                    Tile("Low stock", d.lowStockProducts.toString(), "below reorder level",
                        if (d.lowStockProducts > 0) "Attention" to Tone.Warning else null, Modifier.weight(1f), onOpenStock)
                    Tile("Expiring ≤ 30 days", d.batchesExpiringIn30Days.toString(), "batches",
                        if (d.batchesExpiringIn30Days > 0) "${d.valueExpiringIn30Days.rupeesRounded()} at cost" to Tone.Danger else null,
                        Modifier.weight(1f), onOpenStock)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Last 7 days", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = onReports, shape = AppButtonShape) { Text("Reports") }
            }
            val week = d.last7Days.fold(BigDecimal.ZERO) { acc, p -> acc + p.revenue }
            Text(week.rupeesRounded(), style = MaterialTheme.typography.headlineSmall.merge(Numeric), modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(12.dp))
            BarChart(
                values = d.last7Days.map { it.revenue.toFloat() },
                labels = d.last7Days.map { dayLabel(it.date) },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        item {
            Text("Work queue", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 16.dp, top = 22.dp, bottom = 8.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp).background(scheme.surfaceContainerHighest).padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text("Item", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                Text("Count", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.width(24.dp))
            }
            Column(Modifier.padding(horizontal = 16.dp)) {
                QueueRow("Online orders to handle", if (d.openOrders > 0) "Pack or confirm pickup" else "All caught up", d.openOrders.toString(), onOpenOrders)
                QueueRow("Low on stock", "Reorder before you run out", d.lowStockProducts.toString(), onOpenStock)
                QueueRow("Batches expiring soon", "${d.valueExpiringIn30Days.rupeesRounded()} at cost in 30 days", d.batchesExpiringIn30Days.toString(), onOpenStock)
                QueueRow("Refills due", "Customers due in the next 3 days", d.refillsDueIn3Days.toString(), null)
                QueueRow("Stock value", "All batches, at cost", d.stockValueAtCost.rupeesRounded(), onOpenStock)
            }
        }
    }
}

@Composable
private fun Tile(label: String, value: String, caption: String, tag: Pair<String, Tone>?, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.fillMaxHeight().background(MaterialTheme.colorScheme.surfaceContainerLowest).clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
            overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(8.dp))
        Text(value, style = MaterialTheme.typography.headlineMedium.merge(Numeric), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(caption, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
        if (tag != null) {
            Spacer(Modifier.height(6.dp))
            StatusPill(tag.first, tag.second)
        }
    }
}

@Composable
private fun QueueRow(title: String, subtitle: String, value: String, onClick: (() -> Unit)?) {
    Column {
        Row(
            Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }
            Text(value, style = MaterialTheme.typography.bodyMedium.merge(Mono))
            if (onClick != null) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
            } else {
                Spacer(Modifier.width(24.dp))
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

private fun dayLabel(iso: String): String =
    runCatching { LocalDate.parse(iso).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH) }.getOrDefault("")
