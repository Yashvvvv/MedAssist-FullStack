package com.medassist.app.core

import com.medassist.app.core.model.AppJson
import com.medassist.app.core.model.AuthResponse
import com.medassist.app.core.model.BranchView
import com.medassist.app.core.model.Dashboard
import com.medassist.app.core.model.ExpiringBatchView
import com.medassist.app.core.model.GstSummary
import com.medassist.app.core.model.InboxItem
import com.medassist.app.core.model.MedicineView
import com.medassist.app.core.model.OrderStatus
import com.medassist.app.core.model.OrderView
import com.medassist.app.core.model.OrgSummary
import com.medassist.app.core.model.OrganizationView
import com.medassist.app.core.model.PageResponse
import com.medassist.app.core.model.PharmacyView
import com.medassist.app.core.model.PlanView
import com.medassist.app.core.model.ProductView
import com.medassist.app.core.model.PublicProductView
import com.medassist.app.core.model.Role
import com.medassist.app.core.model.SaleView
import com.medassist.app.core.model.SalesSummary
import com.medassist.app.core.model.StockLevelView
import com.medassist.app.core.model.UnreadCount
import com.medassist.app.core.model.WhoHasResult
import java.math.BigDecimal
import kotlinx.serialization.builtins.ListSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Every model the app uses decodes the backend's real JSON. */
class ContractTest {

    private inline fun <reified T> decode(name: String): T = AppJson.decodeFromString(Fixtures.read(name))

    @Test
    fun `auth responses carry the role that picks the app mode`() {
        val owner = decode<AuthResponse>("auth_owner")
        assertEquals(Role.OWNER, owner.activeOrg?.role)
        val patient = decode<AuthResponse>("auth_patient")
        assertNull(patient.activeOrg)
        decode<AuthResponse>("refresh")
    }

    @Test
    fun `pharmacy side decodes`() {
        val d = decode<Dashboard>("dashboard")
        assertEquals(7, d.last7Days.size)
        decode<OrganizationView>("org")
        decode<OrganizationView>("org_after_plan")
        assertTrue(decode<List<BranchView>>("branches").isNotEmpty())
        assertEquals(3, decode<List<PlanView>>("plans").size)
        decode<OrgSummary>("staff")
        decode<PageResponse<ProductView>>("products")
        decode<PageResponse<StockLevelView>>("levels")
        decode<PageResponse<ExpiringBatchView>>("expiring")
        decode<PageResponse<OrderView>>("orders_queue")
        decode<PageResponse<SaleView>>("sales_list")
        decode<SalesSummary>("report_sales")
        decode<GstSummary>("report_gst")
    }

    @Test
    fun `money stays exact to the paisa`() {
        val sale = decode<SaleView>("sale")
        val lines = sale.lines.fold(BigDecimal.ZERO) { a, l -> a + l.lineTotal }
        assertEquals(0, lines.compareTo(sale.total))
        sale.lines.forEach { l -> assertEquals(0, (l.taxableValue + l.cgst + l.sgst).compareTo(l.lineTotal)) }
    }

    @Test
    fun `patient side decodes`() {
        decode<PageResponse<MedicineView>>("medicines_search")
        decode<MedicineView>("medicine")
        AppJson.decodeFromString(ListSerializer(MedicineView.serializer()), Fixtures.read("substitutes"))
        assertTrue(decode<List<PharmacyView>>("nearby").first().distanceKm != null)
        decode<PharmacyView>("pharmacy")
        decode<PageResponse<PublicProductView>>("catalogue")
        decode<List<WhoHasResult>>("who_has")
        assertEquals(OrderStatus.PLACED, decode<OrderView>("order_placed").status)
        decode<PageResponse<OrderView>>("my_orders")
        val done = decode<OrderView>("order_completed")
        assertEquals(OrderStatus.COMPLETED, done.status)
        assertTrue(done.saleId != null)
        decode<PageResponse<InboxItem>>("inbox")
        decode<UnreadCount>("unread")
    }
}
