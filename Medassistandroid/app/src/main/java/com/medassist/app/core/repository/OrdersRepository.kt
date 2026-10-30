package com.medassist.app.core.repository

import com.medassist.app.core.model.AppJson
import com.medassist.app.core.model.CompleteRequest
import com.medassist.app.core.model.OrderStatus
import com.medassist.app.core.model.OrderView
import com.medassist.app.core.model.PageResponse
import com.medassist.app.core.model.PaymentMode
import com.medassist.app.core.model.PlaceOrderRequest
import com.medassist.app.core.model.RejectRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders

/** A photo ready to upload: bytes plus its MIME type (image/jpeg, image/png ...). */
class UploadFile(val bytes: ByteArray, val mimeType: String, val fileName: String = "photo.jpg")

internal fun UploadFile.partHeaders(): Headers = Headers.build {
    append(HttpHeaders.ContentType, mimeType)
    append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
}

/** Both sides of online orders: the patient's and the pharmacy's queue. */
class OrdersRepository(private val client: HttpClient) {

    // ---------------------------------------------------------------- patient

    suspend fun place(req: PlaceOrderRequest, prescription: UploadFile?): Result<OrderView> = apiCall {
        if (prescription == null) {
            client.post("me/orders") { json(req) }.body()
        } else {
            client.submitFormWithBinaryData(
                url = "me/orders",
                formData = formData {
                    append(
                        "order",
                        AppJson.encodeToString(PlaceOrderRequest.serializer(), req),
                        Headers.build { append(HttpHeaders.ContentType, "application/json") },
                    )
                    append("prescription", prescription.bytes, prescription.partHeaders())
                },
            ).body()
        }
    }

    suspend fun myOrders(page: Int = 0): Result<PageResponse<OrderView>> = apiCall {
        client.get("me/orders") { parameter("page", page) }.body()
    }

    suspend fun myOrder(id: String): Result<OrderView> = apiCall { client.get("me/orders/$id").body() }

    suspend fun cancelMine(id: String): Result<OrderView> = apiCall { client.post("me/orders/$id/cancel").body() }

    // ---------------------------------------------------------------- staff

    suspend fun queue(status: OrderStatus?, page: Int = 0): Result<PageResponse<OrderView>> = apiCall {
        client.get("orders") {
            parameter("status", status?.name)
            parameter("page", page)
            parameter("size", 50)
        }.body()
    }

    suspend fun order(id: String): Result<OrderView> = apiCall { client.get("orders/$id").body() }

    suspend fun accept(id: String): Result<OrderView> = apiCall { client.post("orders/$id/accept").body() }

    suspend fun ready(id: String): Result<OrderView> = apiCall { client.post("orders/$id/ready").body() }

    suspend fun reject(id: String, reason: String): Result<OrderView> = apiCall {
        client.post("orders/$id/reject") { json(RejectRequest(reason)) }.body()
    }

    suspend fun complete(id: String, mode: PaymentMode): Result<OrderView> = apiCall {
        client.post("orders/$id/complete") { json(CompleteRequest(mode)) }.body()
    }
}
