package com.medassist.app.ui.patient

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.medassist.app.core.model.IdentifyResult
import com.medassist.app.core.model.InteractionResult
import com.medassist.app.core.model.toApiError
import com.medassist.app.core.repository.DiscoveryRepository
import com.medassist.app.data.PhotoReader
import com.medassist.app.ui.common.PrimaryButton
import com.medassist.app.ui.common.SectionHeader
import com.medassist.app.ui.common.StatusPill
import com.medassist.app.ui.common.Tone
import com.medassist.app.ui.common.message
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScanState(
    val identifying: Boolean = false,
    val result: IdentifyResult? = null,
    val medicines: List<String> = emptyList(),
    val checking: Boolean = false,
    val interactions: InteractionResult? = null,
    val error: String? = null,
)

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val discovery: DiscoveryRepository,
    private val photos: PhotoReader,
) : ViewModel() {
    private val _state = MutableStateFlow(ScanState())
    val state: StateFlow<ScanState> = _state.asStateFlow()

    fun newCaptureFile() = photos.newCaptureFile()

    fun identify(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(identifying = true, error = null, result = null) }
            val file = photos.read(uri) ?: return@launch _state.update { it.copy(identifying = false, error = "Couldn't read that photo") }
            discovery.identify(file).fold(
                { r -> _state.update { it.copy(identifying = false, result = r) } },
                { e -> _state.update { it.copy(identifying = false, error = e.toApiError().message()) } },
            )
        }
    }

    fun addMedicine(name: String) {
        val n = name.trim()
        if (n.isEmpty() || _state.value.medicines.any { it.equals(n, ignoreCase = true) } || _state.value.medicines.size >= 10) return
        _state.update { it.copy(medicines = it.medicines + n, interactions = null) }
    }

    fun removeMedicine(name: String) = _state.update { it.copy(medicines = it.medicines - name, interactions = null) }

    fun checkInteractions() {
        val list = _state.value.medicines
        if (list.size < 2) return
        viewModelScope.launch {
            _state.update { it.copy(checking = true, error = null) }
            discovery.interactions(list).fold(
                { r -> _state.update { it.copy(checking = false, interactions = r) } },
                { e -> _state.update { it.copy(checking = false, error = e.toApiError().message()) } },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(onMedicine: (String) -> Unit, vm: ScanViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var captureUri by remember { mutableStateOf<Uri?>(null) }
    var entry by rememberSaveable { mutableStateOf("") }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { it?.let(vm::identify) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok) captureUri?.let(vm::identify) }

    Scaffold(topBar = { TopAppBar(title = { Text("AI medicine help") }) }) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AutoAwesome, null)
                        Spacer(Modifier.width(8.dp))
                        Text("What's this medicine?", style = MaterialTheme.typography.titleMedium)
                    }
                    Text("Photograph the strip or box. We read the label and tell you what it is, what it's for, and cheaper same-salt options.",
                        style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = {
                            val uri = FileProvider.getUriForFile(context, context.packageName + ".files", vm.newCaptureFile())
                            captureUri = uri
                            camera.launch(uri)
                        }, enabled = !s.identifying) { Icon(Icons.Outlined.CameraAlt, null); Spacer(Modifier.width(6.dp)); Text("Take photo") }
                        FilledTonalButton(onClick = { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            enabled = !s.identifying) { Icon(Icons.Outlined.Image, null); Spacer(Modifier.width(6.dp)); Text("Gallery") }
                    }
                }
            }
            if (s.identifying) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Reading the label…")
                }
            }
            s.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            s.result?.let { r -> IdentifyResultCard(r, onMedicine) }

            HorizontalDivider()
            SectionHeader("Check interactions")
            Text("Taking more than one medicine? Add them to check for known interactions.", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(entry, { entry = it }, label = { Text("Medicine name") }, singleLine = true, modifier = Modifier.weight(1f))
                TextButton(onClick = { vm.addMedicine(entry); entry = "" }) { Text("Add") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                s.medicines.forEach { m -> InputChip(selected = false, onClick = { vm.removeMedicine(m) }, label = { Text("$m ✕") }) }
            }
            PrimaryButton("Check ${s.medicines.size} medicines", vm::checkInteractions, Modifier.fillMaxWidth(), loading = s.checking,
                enabled = s.medicines.size >= 2)
            s.interactions?.let { r ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        r.summary?.let { Text(it, style = MaterialTheme.typography.titleSmall) }
                        r.interactions.forEach { i ->
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.WarningAmber, null, tint = MaterialTheme.colorScheme.error)
                                    Spacer(Modifier.width(6.dp))
                                    Text(i.between.joinToString(" + "), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                    i.severity?.let { sev ->
                                        StatusPill(sev.lowercase().replaceFirstChar { it.uppercase() },
                                            if (sev.equals("major", true) || sev.equals("severe", true)) Tone.Danger else Tone.Warning)
                                    }
                                }
                                i.description?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                                i.recommendation?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            }
                        }
                        r.disclaimer?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
        }
    }
}

@Composable
private fun IdentifyResultCard(r: IdentifyResult, onMedicine: (String) -> Unit) {
    val id = r.identification
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(id.name ?: "Not sure what this is", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                id.confidence?.let { c ->
                    StatusPill("${(c * 100).toInt()}% sure", if (c >= 0.8) Tone.Success else Tone.Warning)
                }
            }
            id.saltComposition?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
            Text(listOfNotNull(id.manufacturer, id.dosageForm, id.strength).joinToString(" · "), style = MaterialTheme.typography.bodySmall)
            if (id.prescriptionRequired == true) StatusPill("Prescription medicine", Tone.Danger)
            if (id.uses.isNotEmpty()) Text("Used for: ${id.uses.joinToString()}", style = MaterialTheme.typography.bodyMedium)
            if (id.commonSideEffects.isNotEmpty()) Text("Common side effects: ${id.commonSideEffects.joinToString()}", style = MaterialTheme.typography.bodyMedium)
            id.warnings.forEach { Text("⚠ $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) }
            if (r.catalogMatches.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text("Find it nearby", style = MaterialTheme.typography.titleSmall)
                r.catalogMatches.forEach { m ->
                    ListItem(
                        modifier = Modifier.clickable { onMedicine(m.id) },
                        leadingContent = { Icon(Icons.Outlined.Medication, null) },
                        headlineContent = { Text(m.name) },
                        supportingContent = { m.saltComposition?.let { Text(it) } },
                    )
                }
            }
            r.disclaimer?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}
