package com.example.medassist_android.data.network

import com.example.medassist_android.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface MedicineApiService {

    @GET("api/medicines")
    suspend fun getAllMedicines(): Response<List<Medicine>>

    @GET("api/medicines/{id}")
    suspend fun getMedicineById(@Path("id") id: Long): Response<Medicine>

    @POST("api/medicines")
    suspend fun createMedicine(@Body request: CreateMedicineRequest): Response<Medicine>

    @PUT("api/medicines/{id}")
    suspend fun updateMedicine(@Path("id") id: Long, @Body request: CreateMedicineRequest): Response<Medicine>

    @DELETE("api/medicines/{id}")
    suspend fun deleteMedicine(@Path("id") id: Long): Response<Unit>

    @GET("api/medicines/search")
    suspend fun searchMedicines(@Query("q") query: String): Response<List<Medicine>>

    @GET("api/medicines/search/name")
    suspend fun searchByName(@Query("name") name: String): Response<List<Medicine>>

    @GET("api/medicines/search/generic")
    suspend fun searchByGeneric(@Query("genericName") genericName: String): Response<List<Medicine>>

    @GET("api/medicines/search/manufacturer")
    suspend fun searchByManufacturer(@Query("manufacturer") manufacturer: String): Response<List<Medicine>>

    @GET("api/medicines/search/category")
    suspend fun searchByCategory(@Query("category") category: String): Response<List<Medicine>>

    @GET("api/medicines/search/form")
    suspend fun searchByForm(@Query("form") form: String): Response<List<Medicine>>

    @GET("api/medicines/search/brand")
    suspend fun searchByBrand(@Query("brandName") brandName: String): Response<List<Medicine>>

    @GET("api/medicines/search/ingredient")
    suspend fun searchByIngredient(@Query("ingredient") ingredient: String): Response<List<Medicine>>

    @GET("api/medicines/search/prescription")
    suspend fun searchByPrescription(@Query("requiresPrescription") requiresPrescription: Boolean): Response<List<Medicine>>

    @GET("api/medicines/search/strength")
    suspend fun searchByStrength(@Query("strength") strength: String): Response<List<Medicine>>

    // AI Medicine Recognition endpoints
    @POST("api/ai/medicine/analyze/text")
    suspend fun analyzeByText(@Body request: MedicineAnalysisRequest): Response<MedicineAnalysisResponse>

    @Multipart
    @POST("api/ai/medicine/analyze/image")
    suspend fun analyzeByImage(@Part image: MultipartBody.Part): Response<MedicineAnalysisResponse>

    @Multipart
    @POST("api/ai/medicine/analyze/combined")
    suspend fun analyzeCombined(
        @Part image: MultipartBody.Part,
        @Part("query") query: RequestBody
    ): Response<MedicineAnalysisResponse>
}
