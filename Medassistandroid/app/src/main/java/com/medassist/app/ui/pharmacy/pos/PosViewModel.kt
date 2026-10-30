package com.medassist.app.ui.pharmacy.pos

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medassist.app.core.model.ApiError
import com.medassist.app.core.model.CreateSaleRequest
import com.medassist.app.core.model.PaymentMode
import com.medassist.app.core.model.PrescriptionMeta
import com.medassist.app.core.model.ProductView
import com.medassist.app.core.model.SaleLineRequest
import com.medassist.app.core.model.WalkInCustomer
import com.medassist.app.core.model.toApiError
import com.medassist.app.core.repository.SalesRepository
import com.medassist.app.core.repository.StockRepository
import com.medassist.app.core.repository.UploadFile
import com.medassist.app.data.PharmacyContext
import com.medassist.app.data.PhotoReader
import com.medassist.app.ui.common.message
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CartLine(val product: ProductView, val quantity: Int) {
    /** Shelf price × quantity. The server's invoice is the truth (FEFO batch MRP, discounts, rounding). */
    val amount: BigDecimal get() = product.sellingPrice.multiply(BigDecimal(quantity))
}

data class PosState(
    val query: String = "",
    val results: List<ProductView> = emptyList(),
    val searching: Boolean = false,
    val cart: List<CartLine> = emptyList(),
    val customerName: String = "",
    val customerPhone: String = "",
    val paymentMode: PaymentMode = PaymentMode.CASH,
    val remindRefill: Boolean = false,
    val prescription: UploadFile? = null,
    val doctorName: String = "",
    val billing: Boolean = false,
    val error: String? = null,
) {
    val total: BigDecimal get() = cart.fold(BigDecimal.ZERO) { acc, l -> acc + l.amount }
    val itemCount: Int get() = cart.sumOf { it.quantity }
    val needsPrescription: Boolean get() = cart.any { it.product.schedule.requiresPrescription }
    /** Schedule H/H1/X and credit sales need a named customer (Drugs and Cosmetics Rules; khata). */
    val needsCustomer: Boolean get() = needsPrescription || paymentMode == PaymentMode.CREDIT
}

@OptIn(FlowPreview::class)
@HiltViewModel
class PosViewModel @Inject constructor(
    private val stock: StockRepository,
    private val sales: SalesRepository,
    private val pharmacy: PharmacyContext,
    private val photos: PhotoReader,
) : ViewModel() {

    private val _state = MutableStateFlow(PosState())
    val state: StateFlow<PosState> = _state.asStateFlow()

    private val _done = MutableStateFlow<String?>(null)
    /** The id of the sale just billed; the screen navigates to the invoice and clears it. */
    val done: StateFlow<String?> = _done.asStateFlow()

    private val queries = MutableStateFlow("")
    private var searchJob: Job? = null

    /**
     * One key per checkout attempt, kept until the bill succeeds or the cart changes. If the
     * response is lost and the cashier taps Bill again, the server returns the same invoice.
     */
    private var checkoutKey: String = UUID.randomUUID().toString()

    init {
        viewModelScope.launch {
            queries.debounce(250).distinctUntilChanged().collect { search(it) }
        }
    }

    fun onQuery(q: String) {
        _state.update { it.copy(query = q) }
        queries.value = q
    }

    private fun search(q: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _state.update { it.copy(searching = true) }
            stock.products(q.trim().ifBlank { null }, size = 30)
                .onSuccess { page -> _state.update { it.copy(results = page.content, searching = false) } }
                .onFailure { e -> _state.update { it.copy(searching = false, error = e.toApiError().message()) } }
        }
    }

    fun onBarcode(code: String) {
        viewModelScope.launch {
            stock.productByBarcode(code)
                .onSuccess(::add)
                .onFailure { e ->
                    val msg = if (e.toApiError() == ApiError.NotFound) "No product with barcode $code" else e.toApiError().message()
                    _state.update { it.copy(error = msg) }
                }
        }
    }

    fun add(product: ProductView) = changeCart { cart ->
        val existing = cart.indexOfFirst { it.product.id == product.id }
        if (existing >= 0) cart.mapIndexed { i, l -> if (i == existing) l.copy(quantity = l.quantity + 1) else l }
        else cart + CartLine(product, 1)
    }

    fun setQuantity(productId: String, quantity: Int) = changeCart { cart ->
        if (quantity <= 0) cart.filterNot { it.product.id == productId }
        else cart.map { if (it.product.id == productId) it.copy(quantity = quantity.coerceAtMost(999)) else it }
    }

    fun clearCart() = changeCart { emptyList() }

    fun setCustomer(name: String? = null, phone: String? = null) =
        _state.update { it.copy(customerName = name ?: it.customerName, customerPhone = phone ?: it.customerPhone) }

    fun setPayment(mode: PaymentMode) = _state.update { it.copy(paymentMode = mode) }
    fun setRemindRefill(on: Boolean) = _state.update { it.copy(remindRefill = on) }
    fun setDoctor(name: String) = _state.update { it.copy(doctorName = name) }
    fun dismissError() = _state.update { it.copy(error = null) }

    fun attachPrescription(uri: Uri) {
        viewModelScope.launch {
            val file = photos.read(uri)
            _state.update { if (file == null) it.copy(error = "Couldn't read that photo") else it.copy(prescription = file) }
        }
    }

    fun removePrescription() = _state.update { it.copy(prescription = null) }
    fun newCaptureFile() = photos.newCaptureFile()

    fun bill() {
        val s = _state.value
        val branch = pharmacy.branch.value
        when {
            s.billing || s.cart.isEmpty() -> return
            branch == null -> return _state.update { it.copy(error = "No active branch. Check your pharmacy setup.") }
            s.needsCustomer && (s.customerName.isBlank() || s.customerPhone.length < 10) ->
                return _state.update { it.copy(error = "Add the customer's name and mobile number for this bill.") }
            s.needsPrescription && s.prescription == null ->
                return _state.update { it.copy(error = "Attach a photo of the prescription for Rx medicines.") }
        }
        viewModelScope.launch {
            _state.update { it.copy(billing = true, error = null) }
            val customer = if (s.customerName.isNotBlank() && s.customerPhone.isNotBlank()) {
                WalkInCustomer(s.customerName.trim(), s.customerPhone.trim())
            } else null
            // The prescription is uploaded first; a retry after a failed bill re-uploads, which is harmless.
            val prescriptionId = s.prescription?.let { file ->
                sales.uploadPrescription(file, PrescriptionMeta(doctorName = s.doctorName.ifBlank { null })).getOrElse { e ->
                    _state.update { it.copy(billing = false, error = e.toApiError().message()) }
                    return@launch
                }.id
            }
            val req = CreateSaleRequest(
                branchId = branch!!.id,
                paymentMode = s.paymentMode,
                newCustomer = customer,
                prescriptionId = prescriptionId,
                lines = s.cart.map { SaleLineRequest(it.product.id, it.quantity, refillAfterDays = if (s.remindRefill) 30 else null) },
            )
            sales.createSale(req, checkoutKey)
                .onSuccess { sale ->
                    checkoutKey = UUID.randomUUID().toString()
                    _state.value = PosState(results = _state.value.results)
                    _done.value = sale.id
                }
                .onFailure { e -> _state.update { it.copy(billing = false, error = e.toApiError().message()) } }
        }
    }

    fun consumeDone() {
        _done.value = null
    }

    /** Any change to what is being bought is a new checkout, so it gets a new idempotency key. */
    private fun changeCart(transform: (List<CartLine>) -> List<CartLine>) {
        checkoutKey = UUID.randomUUID().toString()
        _state.update { it.copy(cart = transform(it.cart), error = null) }
    }
}
