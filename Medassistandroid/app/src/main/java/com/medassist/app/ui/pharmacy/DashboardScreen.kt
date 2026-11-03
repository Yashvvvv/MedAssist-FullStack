package com.medassist.app.ui.pharmacy

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
            Column {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.LocalPharmacy, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(19.dp))
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(org?.name ?: "Your pharmacy", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                BranchPicker(branches, scope, vm::selectScope)
                            }
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
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onSell,
                shape = MaterialTheme.shapes.medium,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Rounded.PointOfSale, null) },
                text = { Text("New bill") },
            )
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
        Row(
            Modifier.clip(MaterialTheme.shapes.extraSmall).clickable { open = true }.padding(vertical = 1.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                branches.firstOrNull { it.id == selected }?.name ?: "All branches",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Icon(Icons.Rounded.ExpandMore, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(open, { open = false }) {
            DropdownMenuItem(text = { Text("All branches") }, onClick = { open = false; onSelect(null) })
            branches.forEach { b -> DropdownMenuItem(text = { Text(b.name) }, onClick = { open = false; onSelect(b.id) }) }
        }
    }
}

/**
 * Counter Pro dashboard: a 2×2 KPI grid with hairlines, the week's revenue with gridlines, then a
 * "needs attention" queue. Everything a counter needs fits above the fold; each number is a shortcut.
 */
@Composable
private fun DashboardContent(
    d: Dashboard,
    org: OrganizationView?,
    name: String,
    onOpenOrders: () -> Unit,
    onOpenStock: () -> Unit,
    onReports: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val status = LocalStatusColors.current
    LazyColumn(contentPadding = PaddingValues(16.dp, 14.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(greeting(name), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                org?.subscription?.let { sub ->
                    if (sub.status == "TRIALING" && sub.trialEndsAt != null) {
                        StatusPill("PRO trial · ${sub.trialEndsAt.asDateTime().substringBefore(",")}", Tone.Brand)
                    }
                }
            }
        }
        item {
            AppCard {
                Row(Modifier.height(IntrinsicSize.Min)) {
                    Kpi("Today's sales", d.salesToday.rupeesRounded(), "${d.invoicesToday} bills · ${d.salesMonthToDate.rupeesRounded()} this month",
                        scheme.onSurface, Modifier.weight(1f), onReports)
                    VerticalDivider(color = scheme.outlineVariant)
                    Kpi("Profit this month", d.grossProfitMonthToDate.rupeesRounded(), "before tax", status.success, Modifier.weight(1f), onReports)
                }
                HorizontalDivider(color = scheme.outlineVariant)
                Row(Modifier.height(IntrinsicSize.Min)) {
                    Kpi("Low stock", d.lowStockProducts.toString(), "below reorder level",
                        if (d.lowStockProducts > 0) status.warning else scheme.onSurface, Modifier.weight(1f), onOpenStock)
                    VerticalDivider(color = scheme.outlineVariant)
                    Kpi("Expiring in 30 days", d.batchesExpiringIn30Days.toString(), "${d.valueExpiringIn30Days.rupeesRounded()} at cost",
                        if (d.batchesExpiringIn30Days > 0) scheme.error else scheme.onSurface, Modifier.weight(1f), onOpenStock)
                }
            }
        }
        item {
            AppCard {
                Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Last 7 days", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
                            val week = d.last7Days.fold(BigDecimal.ZERO) { acc, p -> acc + p.revenue }
                            Text(week.rupeesRounded(), style = MaterialTheme.typography.headlineSmall.merge(Numeric))
                        }
                        TextButton(onClick = onReports) { Text("Reports") }
                    }
                    Spacer(Modifier.height(10.dp))
                    BarChart(
                        values = d.last7Days.map { it.revenue.toFloat() },
                        labels = d.last7Days.map { dayLabel(it.date) },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
            }
        }
        item {
            Text("Needs attention", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, start = 2.dp))
        }
        item {
            AppCard {
                QueueRow(Icons.Outlined.ShoppingBag, scheme.secondary, "Online orders to handle",
                    if (d.openOrders > 0) "Pack or confirm pickup" else "All caught up", d.openOrders, onOpenOrders)
                HorizontalDivider(color = scheme.outlineVariant)
                QueueRow(Icons.Outlined.Inventory, status.warning, "Low on stock",
                    "Reorder before you run out", d.lowStockProducts, onOpenStock)
                HorizontalDivider(color = scheme.outlineVariant)
                QueueRow(Icons.Outlined.EventBusy, scheme.error, "Batches expiring soon",
                    "${d.valueExpiringIn30Days.rupeesRounded()} at cost in 30 days", d.batchesExpiringIn30Days, onOpenStock)
                HorizontalDivider(color = scheme.outlineVariant)
                QueueRow(Icons.Outlined.Autorenew, status.info, "Refills due",
                    "Customers due in the next 3 days", d.refillsDueIn3Days, null)
                HorizontalDivider(color = scheme.outlineVariant)
                QueueRow(Icons.Outlined.Receipt, scheme.onSurfaceVariant, "Stock value",
                    "All batches, at cost", null, onOpenStock, valueText = d.stockValueAtCost.rupeesRounded())
            }
        }
    }
}

@Composable
private fun Kpi(label: String, value: String, caption: String, valueColor: Color, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.fillMaxHeight().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
            overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.headlineSmall.merge(Numeric), color = valueColor, maxLines = 1,
            overflow = TextOverflow.Ellipsis)
        Text(caption, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
    }
}

@Composable
private fun QueueRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    count: Long?,
    onClick: (() -> Unit)?,
    valueText: String? = null,
) {
    val active = (count ?: 1L) > 0
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(tint.copy(alpha = if (active) 0.14f else 0.06f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = if (active) tint else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp)) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
        }
        Text(
            valueText ?: count.toString(),
            style = MaterialTheme.typography.titleMedium.merge(Numeric),
            color = if (active && valueText == null) tint else MaterialTheme.colorScheme.onSurface,
        )
        if (onClick != null) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
        } else {
            Spacer(Modifier.width(20.dp))
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
