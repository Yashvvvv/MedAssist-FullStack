package com.medassist.app.ui.pharmacy.pos

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.medassist.app.core.model.PaymentMode
import com.medassist.app.core.model.ProductView
import com.medassist.app.ui.common.paymentLabel
import com.medassist.app.ui.common.EmptyState
import com.medassist.app.ui.common.KeyValueRow
import com.medassist.app.ui.common.PrimaryButton
import com.medassist.app.ui.common.RxBadge
import com.medassist.app.ui.common.rupees
import com.medassist.app.ui.theme.Numeric

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(onInvoice: (String) -> Unit, vm: PosViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val done by vm.done.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showCheckout by rememberSaveable { mutableStateOf(false) }
    var showCart by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(done) {
        done?.let {
            showCheckout = false
            showCart = false
            vm.consumeDone()
            onInvoice(it)
        }
    }

    val scan = {
        GmsBarcodeScanning.getClient(context).startScan()
            .addOnSuccessListener { barcode -> barcode.rawValue?.let(vm::onBarcode) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New bill") },
                actions = {
                    if (s.cart.isNotEmpty()) TextButton(onClick = vm::clearCart) { Text("Clear") }
                },
            )
        },
        bottomBar = {
            if (s.cart.isNotEmpty()) {
                BottomAppBar {
                    Column(Modifier.weight(1f).clickable { showCart = true }.padding(start = 8.dp)) {
                        Text("${s.itemCount} item(s) · tap to review", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(s.total.rupees(), style = MaterialTheme.typography.titleLarge.merge(Numeric))
                    }
                    PrimaryButton("Checkout", { showCheckout = true }, Modifier.padding(end = 8.dp))
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = s.query,
                onValueChange = vm::onQuery,
                placeholder = { Text("Search medicine, salt or SKU") },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                trailingIcon = {
                    IconButton(onClick = { scan() }) { Icon(Icons.Outlined.QrCodeScanner, "Scan barcode") }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (s.searching && s.results.isEmpty()) {
                Row(Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() }
            } else if (s.results.isEmpty()) {
                EmptyState(Icons.Outlined.AddShoppingCart, "No products found",
                    "Add medicines to your catalogue from Stock → Add from catalogue.")
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                    items(s.results, key = { it.id }) { p ->
                        val inCart = s.cart.firstOrNull { it.product.id == p.id }?.quantity ?: 0
                        ProductRow(p, inCart, onAdd = { vm.add(p) })
                    }
                }
            }
        }
    }

    s.error?.let { msg ->
        if (!showCheckout) {
            AlertDialog(
                onDismissRequest = vm::dismissError,
                confirmButton = { TextButton(onClick = vm::dismissError) { Text("OK") } },
                text = { Text(msg) },
            )
        }
    }

    if (showCart) {
        ModalBottomSheet(onDismissRequest = { showCart = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            CartSheet(s, vm, onCheckout = { showCart = false; showCheckout = true })
        }
    }
    if (showCheckout) {
        ModalBottomSheet(onDismissRequest = { showCheckout = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            CheckoutSheet(s, vm)
        }
    }
}

@Composable
private fun ProductRow(p: ProductView, inCart: Int, onAdd: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onAdd),
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(p.name, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (p.schedule.requiresPrescription) {
                    Spacer(Modifier.width(8.dp))
                    RxBadge()
                }
            }
        },
        supportingContent = {
            Text(
                listOfNotNull(p.saltComposition, p.packSize, p.rackLocation?.let { "Rack $it" }).joinToString(" · "),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text(p.sellingPrice.rupees(), style = MaterialTheme.typography.titleSmall.merge(Numeric))
                if (inCart > 0) Text("× $inCart in bill", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        },
    )
}

@Composable
private fun CartSheet(s: PosState, vm: PosViewModel, onCheckout: () -> Unit) {
    Column(Modifier.navigationBarsPadding().padding(horizontal = 20.dp)) {
        Text("This bill", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        s.cart.forEach { line ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(line.product.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${line.product.sellingPrice.rupees()} each", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Stepper(line.quantity) { vm.setQuantity(line.product.id, it) }
                Text(line.amount.rupees(), style = MaterialTheme.typography.bodyLarge.merge(Numeric),
                    modifier = Modifier.width(88.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        KeyValueRow("Estimated total", s.total.rupees(), strong = true)
        Text("GST is included in MRP. The invoice shows the exact batch, tax split and rounding.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        PrimaryButton("Checkout", onCheckout, Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun Stepper(value: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        FilledTonalIconButton(onClick = { onChange(value - 1) }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Rounded.Remove, "Less", Modifier.size(18.dp))
        }
        Text(value.toString(), style = MaterialTheme.typography.titleMedium.merge(Numeric), modifier = Modifier.width(36.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        FilledTonalIconButton(onClick = { onChange(value + 1) }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Rounded.Add, "More", Modifier.size(18.dp))
        }
    }
}

@Composable
private fun CheckoutSheet(s: PosState, vm: PosViewModel) {
    val context = LocalContext.current
    var captureUri by remember { mutableStateOf<Uri?>(null) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(vm::attachPrescription) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok) captureUri?.let(vm::attachPrescription) }

    Column(
        Modifier
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Text("Checkout", style = MaterialTheme.typography.titleLarge)
        Text("${s.itemCount} item(s) · ${s.total.rupees()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))

        Text(if (s.needsCustomer) "Customer (required)" else "Customer (optional)", style = MaterialTheme.typography.titleSmall)
        OutlinedTextField(s.customerName, { vm.setCustomer(name = it) }, label = { Text("Name") }, singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
        OutlinedTextField(s.customerPhone, { vm.setCustomer(phone = it.filter { c -> c.isDigit() || c == '+' }.take(13)) },
            label = { Text("Mobile") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp))

        if (s.needsPrescription) {
            Spacer(Modifier.height(16.dp))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RxBadge()
                        Spacer(Modifier.width(8.dp))
                        Text("Prescription needed", style = MaterialTheme.typography.titleSmall)
                    }
                    Text("Schedule H/H1 medicines can only be sold against a doctor's prescription. Keep a photo with the bill.",
                        style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    if (s.prescription != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("Photo attached (${s.prescription.bytes.size / 1024} KB)", Modifier.weight(1f))
                            IconButton(onClick = vm::removePrescription) { Icon(Icons.Rounded.Close, "Remove") }
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                val file = vm.newCaptureFile()
                                val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
                                captureUri = uri
                                camera.launch(uri)
                            }) {
                                Icon(Icons.Outlined.CameraAlt, null)
                                Spacer(Modifier.width(6.dp))
                                Text("Camera")
                            }
                            OutlinedButton(onClick = { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                                Icon(Icons.Outlined.Image, null)
                                Spacer(Modifier.width(6.dp))
                                Text("Gallery")
                            }
                        }
                    }
                    OutlinedTextField(s.doctorName, vm::setDoctor, label = { Text("Doctor's name (optional)") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Payment", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
            PaymentMode.entries.forEach { mode ->
                FilterChip(selected = s.paymentMode == mode, onClick = { vm.setPayment(mode) },
                    label = { Text(paymentLabel(mode.name)) })
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Refill reminder in 30 days", style = MaterialTheme.typography.bodyLarge)
                Text("WhatsApp reminder to the customer (PRO)", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(s.remindRefill, vm::setRemindRefill)
        }
        s.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp))
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton("Bill ${s.total.rupees()}", vm::bill, Modifier.fillMaxWidth(), loading = s.billing)
        Spacer(Modifier.height(20.dp))
    }
}
