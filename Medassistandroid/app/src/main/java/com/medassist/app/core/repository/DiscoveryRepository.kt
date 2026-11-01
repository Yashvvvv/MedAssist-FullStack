package com.medassist.app.core.repository

import com.medassist.app.core.model.IdentifyResult
import com.medassist.app.core.model.InteractionRequest
import com.medassist.app.core.model.InteractionResult
import com.medassist.app.core.model.MedicineView
import com.medassist.app.core.model.PageResponse
import com.medassist.app.core.model.PharmacyView
import com.medassist.app.core.model.PublicProductView
import com.medassist.app.core.model.WhoHasResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post

/** The patient's side: find pharmacies and medicines, and the AI helpers. */
class DiscoveryRepository(private val client: HttpClient) {

    suspend fun nearby(lat: Double, lng: Double, radiusKm: Double): Result<List<PharmacyView>> = apiCall {
        client.get("public/pharmacies/nearby") {
            parameter("lat", lat)
            parameter("lng", lng)
            parameter("radiusKm", radiusKm)
        }.body()
    }

    suspend fun pharmacy(branchId: String): Result<PharmacyView> = apiCall {
        client.get("public/pharmacies/$branchId").body()
    }

    suspend fun catalogue(branchId: String, query: String?, page: Int = 0): Result<PageResponse<PublicProductView>> = apiCall {
        client.get("public/pharmacies/$branchId/products") {
            parameter("q", query?.takeIf { it.isNotBlank() })
            parameter("page", page)
            parameter("size", 40)
        }.body()
    }

    suspend fun searchMedicines(query: String): Result<PageResponse<MedicineView>> = apiCall {
        client.get("public/medicines") {
            parameter("q", query)
            parameter("size", 20)
        }.body()
    }

    suspend fun medicine(id: String): Result<MedicineView> = apiCall { client.get("public/medicines/$id").body() }

    suspend fun substitutes(id: String): Result<List<MedicineView>> = apiCall {
        client.get("public/medicines/$id/substitutes").body()
    }

    suspend fun whoHas(medicineId: String, lat: Double, lng: Double, radiusKm: Double): Result<List<WhoHasResult>> = apiCall {
        client.get("public/medicines/$medicineId/availability") {
            parameter("lat", lat)
            parameter("lng", lng)
            parameter("radiusKm", radiusKm)
        }.body()
    }

    suspend fun identify(photo: UploadFile): Result<IdentifyResult> = apiCall {
        client.submitFormWithBinaryData(
            url = "ai/identify",
            formData = formData { append("image", photo.bytes, photo.partHeaders()) },
        ).body()
    }

    suspend fun interactions(medicines: List<String>): Result<InteractionResult> = apiCall {
        client.post("ai/interactions") { json(InteractionRequest(medicines)) }.body()
    }
}
