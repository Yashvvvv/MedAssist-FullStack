package com.medassist.app.core.repository

import com.medassist.app.core.model.BatchView
import com.medassist.app.core.model.ExpiringBatchView
import com.medassist.app.core.model.ImportRequest
import com.medassist.app.core.model.ImportResult
import com.medassist.app.core.model.MedicineView
import com.medassist.app.core.model.PageResponse
import com.medassist.app.core.model.ProductView
import com.medassist.app.core.model.ReceiveStockRequest
import com.medassist.app.core.model.StockLevelView
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.http.encodeURLPathPart

/** Catalogue and inventory for staff. */
class StockRepository(private val client: HttpClient) {

    suspend fun products(query: String?, page: Int = 0, size: Int = 30): Result<PageResponse<ProductView>> = apiCall {
        client.get("products") {
            parameter("q", query?.takeIf { it.isNotBlank() })
            parameter("active", true)
            parameter("page", page)
            parameter("size", size)
        }.body()
    }

    suspend fun productByBarcode(barcode: String): Result<ProductView> = apiCall {
        client.get("products/barcode/${barcode.encodeURLPathPart()}").body()
    }

    suspend fun searchMaster(query: String): Result<PageResponse<MedicineView>> = apiCall {
        client.get("public/medicines") {
            parameter("q", query)
            parameter("size", 30)
        }.body()
    }

    suspend fun importFromMaster(medicineIds: List<String>): Result<ImportResult> = apiCall {
        client.post("products/import") { json(ImportRequest(medicineIds, reorderLevel = 10)) }.body()
    }

    suspend fun levels(branchId: String, query: String?, page: Int = 0): Result<PageResponse<StockLevelView>> = apiCall {
        client.get("inventory/levels") {
            parameter("branchId", branchId)
            parameter("q", query?.takeIf { it.isNotBlank() })
            parameter("page", page)
            parameter("size", 50)
        }.body()
    }

    suspend fun lowStock(branchId: String): Result<PageResponse<StockLevelView>> = apiCall {
        client.get("inventory/low-stock") {
            parameter("branchId", branchId)
            parameter("size", 100)
        }.body()
    }

    suspend fun expiring(branchId: String, withinDays: Int = 90): Result<PageResponse<ExpiringBatchView>> = apiCall {
        client.get("inventory/expiring") {
            parameter("branchId", branchId)
            parameter("withinDays", withinDays)
            parameter("size", 100)
        }.body()
    }

    suspend fun batches(productId: String, branchId: String): Result<List<BatchView>> = apiCall {
        client.get("products/$productId/batches") { parameter("branchId", branchId) }.body()
    }

    suspend fun receive(req: ReceiveStockRequest): Result<BatchView> = apiCall {
        client.post("inventory/receive") { json(req) }.body()
    }
}
