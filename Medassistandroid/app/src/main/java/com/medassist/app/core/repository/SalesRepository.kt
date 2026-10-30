package com.medassist.app.core.repository

import com.medassist.app.core.model.AppJson
import com.medassist.app.core.model.CreateSaleRequest
import com.medassist.app.core.model.PrescriptionMeta
import com.medassist.app.core.model.PrescriptionView
import com.medassist.app.core.model.CustomerView
import com.medassist.app.core.model.PageResponse
import com.medassist.app.core.model.SaleView
import com.medassist.app.core.network.IDEMPOTENCY_KEY_HEADER
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders

class SalesRepository(private val client: HttpClient) {

    /**
     * Bills a sale. [idempotencyKey] is generated once per checkout by the caller and reused on
     * every retry, so a lost response followed by "Try again" returns the same invoice.
     */
    suspend fun createSale(req: CreateSaleRequest, idempotencyKey: String): Result<SaleView> = apiCall {
        client.post("sales") {
            header(IDEMPOTENCY_KEY_HEADER, idempotencyKey)
            json(req)
        }.body()
    }

    suspend fun sale(id: String): Result<SaleView> = apiCall { client.get("sales/$id").body() }

    suspend fun recentSales(branchId: String?, query: String? = null, page: Int = 0): Result<PageResponse<SaleView>> = apiCall {
        client.get("sales") {
            parameter("branchId", branchId)
            parameter("q", query?.takeIf { it.isNotBlank() })
            parameter("page", page)
            parameter("size", 30)
        }.body()
    }

    suspend fun customers(query: String?): Result<PageResponse<CustomerView>> = apiCall {
        client.get("customers") {
            parameter("q", query?.takeIf { it.isNotBlank() })
            parameter("size", 20)
        }.body()
    }

    /** Counter upload of a paper prescription, needed before billing Schedule H/H1/X medicines. */
    suspend fun uploadPrescription(photo: UploadFile, meta: PrescriptionMeta): Result<PrescriptionView> = apiCall {
        client.submitFormWithBinaryData(
            url = "prescriptions",
            formData = formData {
                append("file", photo.bytes, photo.partHeaders())
                append(
                    "meta",
                    AppJson.encodeToString(PrescriptionMeta.serializer(), meta),
                    Headers.build { append(HttpHeaders.ContentType, "application/json") },
                )
            },
        ).body()
    }

    /** The prescription image itself (served only to staff of the pharmacy it belongs to). */
    suspend fun prescriptionFile(id: String): Result<ByteArray> = apiCall { client.get("prescriptions/$id/file").body() }
}
