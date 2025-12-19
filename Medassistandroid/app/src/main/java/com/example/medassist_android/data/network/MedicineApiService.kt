package com.example.medassist_android.data.network

import com.example.medassist_android.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface MedicineApiService {

    @GET("api/v1/medicines")
    suspend fun getAllMedicines(): Response<List<Medicine>>

    @GET("api/v1/medicines/{id}")
    suspend fun getMedicineById(@Path("id") id: Long): Response<Medicine>

    @POST("api/v1/medicines")
    suspend fun createMedicine(@Body request: CreateMedicineRequest): Response<Medicine>

    @PUT("api/v1/medicines/{id}")
    suspend fun updateMedicine(@Path("id") id: Long, @Body request: CreateMedicineRequest): Response<Medicine>

    @DELETE("api/v1/medicines/{id}")
    suspend fun deleteMedicine(@Path("id") id: Long): Response<Unit>

    @GET("api/v1/medicines/search")
    suspend fun searchMedicines(@Query("q") query: String): Response<List<Medicine>>

    @GET("api/v1/medicines/search/name")
    suspend fun searchByName(@Query("name") name: String): Response<List<Medicine>>

    @GET("api/v1/medicines/search/generic")
    suspend fun searchByGeneric(@Query("genericName") genericName: String): Response<List<Medicine>>

    @GET("api/v1/medicines/search/manufacturer")
    suspend fun searchByManufacturer(@Query("manufacturer") manufacturer: String): Response<List<Medicine>>

    @GET("api/v1/medicines/search/category")
    suspend fun searchByCategory(@Query("category") category: String): Response<List<Medicine>>

    @GET("api/v1/medicines/search/form")
    suspend fun searchByForm(@Query("form") form: String): Response<List<Medicine>>

    @GET("api/v1/medicines/search/brand")
    suspend fun searchByBrand(@Query("brandName") brandName: String): Response<List<Medicine>>

    @GET("api/v1/medicines/search/ingredient")
    suspend fun searchByIngredient(@Query("ingredient") ingredient: String): Response<List<Medicine>>

    @GET("api/v1/medicines/search/prescription")
    suspend fun searchByPrescription(@Query("requiresPrescription") requiresPrescription: Boolean): Response<List<Medicine>>

    @GET("api/v1/medicines/search/strength")
    suspend fun searchByStrength(@Query("strength") strength: String): Response<List<Medicine>>

    @GET("api/v1/medicines/count")
    suspend fun getTotalMedicineCount(): Response<Long>

    @GET("api/v1/medicines/exists")
    suspend fun checkMedicineExists(@Query("name") name: String): Response<Boolean>

    // AI Medicine Recognition endpoints - aligned with MedicineAIController
    @POST("api/v1/ai/medicine/analyze/text")
    suspend fun analyzeByText(@Query("query") query: String): Response<MedicineAnalysisResponse>

    @Multipart
    @POST("api/v1/ai/medicine/analyze/image")
    suspend fun analyzeByImage(@Part file: MultipartBody.Part): Response<MedicineAnalysisResponse>

    @Multipart
    @POST("api/v1/ai/medicine/analyze/combined")
    suspend fun analyzeCombined(
        @Part image: MultipartBody.Part,
        @Query("query") query: String
    ): Response<MedicineAnalysisResponse>

    @POST("api/v1/ai/medicine/analyze/interactions")
    suspend fun analyzeDrugInteractions(
        @Query("primaryMedicine") primaryMedicine: String,
        @Query("otherMedicines") otherMedicines: List<String>
    ): Response<MedicineAnalysisResponse>
}
