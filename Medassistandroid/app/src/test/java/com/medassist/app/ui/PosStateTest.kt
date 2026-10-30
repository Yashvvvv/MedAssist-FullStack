package com.medassist.app.ui

import com.medassist.app.core.model.DrugSchedule
import com.medassist.app.core.model.PaymentMode
import com.medassist.app.core.model.ProductView
import com.medassist.app.ui.pharmacy.pos.CartLine
import com.medassist.app.ui.pharmacy.pos.PosState
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PosStateTest {

    private fun product(id: String, price: String, schedule: DrugSchedule = DrugSchedule.OTC) = ProductView(
        id = id, name = id, schedule = schedule, gstRate = BigDecimal("5"), mrp = BigDecimal(price), sellingPrice = BigDecimal(price),
    )

    @Test
    fun `totals are exact decimals`() {
        // 3 × 0.10 is 0.30000000000000004 in floating point; in BigDecimal it is 0.30.
        val s = PosState(cart = listOf(CartLine(product("a", "0.10"), 3), CartLine(product("b", "33.00"), 15)))
        assertEquals(BigDecimal("495.30"), s.total)
        assertEquals(18, s.itemCount)
    }

    @Test
    fun `prescription medicines and credit sales need a customer`() {
        val otc = PosState(cart = listOf(CartLine(product("a", "10"), 1)))
        assertFalse(otc.needsCustomer)
        assertTrue(otc.copy(paymentMode = PaymentMode.CREDIT).needsCustomer)
        val rx = PosState(cart = listOf(CartLine(product("b", "120", DrugSchedule.H1), 1)))
        assertTrue(rx.needsPrescription)
        assertTrue(rx.needsCustomer)
    }
}
