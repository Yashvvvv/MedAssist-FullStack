package com.medassist.app.ui.account

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.medassist.app.BuildConfig
import com.medassist.app.core.auth.Session
import com.medassist.app.core.model.InboxItem
import com.medassist.app.core.model.toApiError
import com.medassist.app.core.repository.InsightsRepository
import com.medassist.app.data.ThemeMode
import com.medassist.app.ui.common.DetailScaffold
import com.medassist.app.ui.common.EmptyState
import com.medassist.app.ui.common.StateContent
import com.medassist.app.ui.common.UiState
import com.medassist.app.ui.common.asRelative
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Profile, theme, switching between pharmacies (or to customer mode), and log out. Used by both modes. */
@Composable
fun AccountScreen(sessionViewModel: SessionViewModel, onBack: (() -> Unit)?) {
    val session by sessionViewModel.session.collectAsStateWithLifecycle()
    val theme by sessionViewModel.theme.collectAsStateWithLifecycle()
    val memberships by sessionViewModel.memberships.collectAsStateWithLifecycle()
    val message by sessionViewModel.message.collectAsStateWithLifecycle()
    val tokens = (session as? Session.SignedIn)?.tokens
    var confirmLogout by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { sessionViewModel.loadMemberships() }

    val body: @Composable (PaddingValues) -> Unit = { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            tokens?.let { t ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(52.dp)) {
                            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                Text(t.fullName.take(1).uppercase(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(t.fullName, style = MaterialTheme.typography.titleMedium)
                            Text(t.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            t.orgName?.let { Text("${t.role?.name?.lowercase() ?: ""} at $it", style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
            }

            if (memberships.isNotEmpty()) {
                Text("Switch to", style = MaterialTheme.typography.titleSmall)
                memberships.filter { it.orgId != tokens?.orgId }.forEach { m ->
                    ListItem(
                        headlineContent = { Text(m.orgName) },
                        supportingContent = { Text("as ${m.role.name.lowercase()}") },
                        leadingContent = { Icon(Icons.Outlined.Storefront, null) },
                        modifier = Modifier.clickable { sessionViewModel.switchTo(m.orgId) },
                    )
                }
                if (tokens?.orgId != null) {
                    ListItem(
                        headlineContent = { Text("Customer mode") },
                        supportingContent = { Text("Find pharmacies and order, like a customer") },
                        leadingContent = { Icon(Icons.Outlined.SwapHoriz, null) },
                        modifier = Modifier.clickable { sessionViewModel.switchTo(null) },
                    )
                }
                HorizontalDivider()
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.DarkMode, null)
                Spacer(Modifier.width(12.dp))
                Text("Theme", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(selected = theme == mode, onClick = { sessionViewModel.setTheme(mode) },
                        label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) })
                }
            }
            HorizontalDivider()
            OutlinedButton(onClick = { confirmLogout = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.AutoMirrored.Outlined.Logout, null)
                Spacer(Modifier.width(8.dp))
                Text("Log out")
            }
            Text(
                "MedAssist ${BuildConfig.VERSION_NAME}. Medicine information in this app is for reference; " +
                    "always follow your doctor's and pharmacist's advice.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (onBack != null) DetailScaffold("Account", onBack, content = body) else body(PaddingValues(top = 24.dp))

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("Log out?") },
            confirmButton = { TextButton(onClick = { confirmLogout = false; sessionViewModel.signOut() }) { Text("Log out") } },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("Cancel") } },
        )
    }
    message?.let {
        AlertDialog(onDismissRequest = sessionViewModel::consumeMessage, text = { Text(it) },
            confirmButton = { TextButton(onClick = sessionViewModel::consumeMessage) { Text("OK") } })
    }
}

@HiltViewModel
class NotificationsViewModel @Inject constructor(private val insights: InsightsRepository) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<InboxItem>>>(UiState.Loading)
    val state: StateFlow<UiState<List<InboxItem>>> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            insights.inbox().fold({ _state.value = UiState.Success(it.content) }, { _state.value = UiState.Error(it.toApiError()) })
            insights.markAllRead() // opening the inbox counts as reading it
        }
    }
}

@Composable
fun NotificationsScreen(onBack: () -> Unit, vm: NotificationsViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    DetailScaffold("Notifications", onBack) { padding ->
        Column(Modifier.padding(padding)) {
            StateContent(state, vm::load) { items ->
                if (items.isEmpty()) EmptyState(Icons.Outlined.Notifications, "All caught up", "Order updates and alerts show up here.")
                else LazyColumn {
                    items(items, key = { it.id }) { n ->
                        ListItem(
                            headlineContent = { Text(n.title, style = if (n.read) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.titleSmall) },
                            supportingContent = { Text(n.body) },
                            trailingContent = { Text(n.createdAt.asRelative(), style = MaterialTheme.typography.labelSmall) },
                            leadingContent = {
                                Surface(
                                    shape = CircleShape,
                                    color = if (n.read) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(10.dp),
                                ) {}
                            },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Suppress("unused")
private val spacer = Modifier.height(0.dp)
