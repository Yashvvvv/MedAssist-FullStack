package com.medassist.app.ui.pharmacy.more

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.medassist.app.core.model.ApiError
import com.medassist.app.core.model.OrgSummary
import com.medassist.app.core.model.Role
import com.medassist.app.core.model.toApiError
import com.medassist.app.core.repository.PharmacyRepository
import com.medassist.app.ui.common.DetailScaffold
import com.medassist.app.ui.common.SectionHeader
import com.medassist.app.ui.common.StateContent
import com.medassist.app.ui.common.StatusPill
import com.medassist.app.ui.common.Tone
import com.medassist.app.ui.common.UiState
import com.medassist.app.ui.common.message
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class StaffViewModel @Inject constructor(private val pharmacy: PharmacyRepository) : ViewModel() {
    private val _state = MutableStateFlow<UiState<OrgSummary>>(UiState.Loading)
    val state: StateFlow<UiState<OrgSummary>> = _state.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    private val _planLimit = MutableStateFlow(false)
    val planLimit: StateFlow<Boolean> = _planLimit.asStateFlow()
    /** The link to share when email isn't configured on the server. */
    private val _inviteLink = MutableStateFlow<String?>(null)
    val inviteLink: StateFlow<String?> = _inviteLink.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch { pharmacy.staff().fold({ _state.value = UiState.Success(it) }, { _state.value = UiState.Error(it.toApiError()) }) }
    }

    fun invite(email: String, role: Role) {
        viewModelScope.launch {
            pharmacy.invite(email, role).fold(
                { _inviteLink.value = it.acceptUrl; load() },
                { e ->
                    val err = e.toApiError()
                    if (err is ApiError.PlanLimit) _planLimit.value = true
                    _message.value = err.message()
                },
            )
        }
    }

    fun revoke(id: String) = viewModelScope.launch { pharmacy.revokeInvitation(id).onSuccess { load() } }
    fun remove(membershipId: String) = viewModelScope.launch {
        pharmacy.removeMember(membershipId).fold({ load() }, { _message.value = it.toApiError().message() })
    }

    fun dismiss() {
        _message.value = null
        _inviteLink.value = null
    }
}

@Composable
fun StaffScreen(onBack: () -> Unit, onUpgrade: () -> Unit, vm: StaffViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val planLimit by vm.planLimit.collectAsStateWithLifecycle()
    val link by vm.inviteLink.collectAsStateWithLifecycle()
    var inviting by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    DetailScaffold(
        "Staff",
        onBack,
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { inviting = true }, icon = { Icon(Icons.Rounded.PersonAdd, null) }, text = { Text("Invite") })
        },
    ) { padding ->
            Column(Modifier.padding(padding)) {
                StateContent(state, vm::load) { summary ->
                    LazyColumn(Modifier.padding(horizontal = 8.dp)) {
                        item { SectionHeader("Team (${summary.members.size})", Modifier.padding(horizontal = 8.dp)) }
                        items(summary.members, key = { it.membershipId }) { m ->
                            ListItem(
                                headlineContent = { Text(m.fullName) },
                                supportingContent = { Text(m.email) },
                                trailingContent = {
                                    Row {
                                        StatusPill(m.role.name.lowercase().replaceFirstChar { it.uppercase() }, if (m.role == Role.OWNER) Tone.Brand else Tone.Neutral)
                                        if (m.role != Role.OWNER) TextButton(onClick = { vm.remove(m.membershipId) }) { Text("Remove") }
                                    }
                                },
                            )
                        }
                        if (summary.pendingInvitations.isNotEmpty()) {
                            item { SectionHeader("Pending invitations", Modifier.padding(horizontal = 8.dp)) }
                            items(summary.pendingInvitations, key = { it.id }) { inv ->
                                ListItem(
                                    headlineContent = { Text(inv.email) },
                                    supportingContent = { Text("Invited as ${inv.role.name.lowercase()}") },
                                    trailingContent = { TextButton(onClick = { vm.revoke(inv.id) }) { Text("Revoke") } },
                                )
                            }
                        }
                    }
                }
            }
    }

    if (inviting) {
        var email by rememberSaveable { mutableStateOf("") }
        var role by rememberSaveable { mutableStateOf(Role.CASHIER) }
        AlertDialog(
            onDismissRequest = { inviting = false },
            title = { Text("Invite a team member") },
            text = {
                Column {
                    OutlinedTextField(email, { email = it }, label = { Text("Their email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                    Text("Role", style = MaterialTheme.typography.titleSmall)
                    listOf(Role.CASHIER to "Bills and orders", Role.PHARMACIST to "+ stock, catalogue, Rx", Role.ADMIN to "+ staff and reports")
                        .forEach { (r, desc) ->
                            FilterChip(selected = role == r, onClick = { role = r }, modifier = Modifier.fillMaxWidth(),
                                label = { Text("${r.name.lowercase().replaceFirstChar { it.uppercase() }}: $desc") })
                        }
                }
            },
            confirmButton = { TextButton(enabled = email.contains("@"), onClick = { inviting = false; vm.invite(email, role) }) { Text("Send invite") } },
            dismissButton = { TextButton(onClick = { inviting = false }) { Text("Cancel") } },
        )
    }
    link?.let { url ->
        AlertDialog(
            onDismissRequest = vm::dismiss,
            title = { Text("Invitation created") },
            text = { Text("We emailed the invite. You can also send the link yourself, e.g. on WhatsApp. It works for 7 days.") },
            confirmButton = {
                TextButton(onClick = {
                    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain")
                        .putExtra(Intent.EXTRA_TEXT, "Join our pharmacy on MedAssist: $url"), "Share invite"))
                    vm.dismiss()
                }) { Text("Share link") }
            },
            dismissButton = { TextButton(onClick = vm::dismiss) { Text("Done") } },
        )
    }
    message?.let {
        AlertDialog(
            onDismissRequest = vm::dismiss,
            text = { Text(it) },
            confirmButton = {
                if (planLimit) TextButton(onClick = { vm.dismiss(); onUpgrade() }) { Text("See plans") }
                else TextButton(onClick = vm::dismiss) { Text("OK") }
            },
        )
    }
}
