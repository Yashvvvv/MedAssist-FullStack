package com.example.medassist_android.data.network

import com.example.medassist_android.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface PharmacyApiService {

    @GET("api/pharmacies")
    suspend fun getAllPharmacies(): Response<List<Pharmacy>>

    @GET("api/pharmacies/{id}")
    suspend fun getPharmacyById(@Path("id") id: Long): Response<Pharmacy>

    @POST("api/pharmacies")
    suspend fun createPharmacy(@Body request: CreatePharmacyRequest): Response<Pharmacy>

    @PUT("api/pharmacies/{id}")
    suspend fun updatePharmacy(@Path("id") id: Long, @Body request: CreatePharmacyRequest): Response<Pharmacy>

    @DELETE("api/pharmacies/{id}")
    suspend fun deletePharmacy(@Path("id") id: Long): Response<Unit>

    @PATCH("api/pharmacies/{id}/deactivate")
    suspend fun deactivatePharmacy(@Path("id") id: Long): Response<Unit>

    @GET("api/pharmacies/search")
    suspend fun searchPharmacies(@Query("q") query: String): Response<List<Pharmacy>>

    // Location-based endpoints
    @POST("api/pharmacies/location/nearby")
    suspend fun getNearbyPharmacies(@Body request: NearbyPharmaciesRequest): Response<List<Pharmacy>>

    @GET("api/pharmacies/location/nearby")
    suspend fun getNearbyPharmaciesGet(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("radiusKm") radiusKm: Double = 10.0,
        @Query("maxResults") maxResults: Int = 20,
        @Query("includeOperatingHours") includeOperatingHours: Boolean = true,
        @Query("includeServices") includeServices: Boolean = true,
        @Query("sortBy") sortBy: String = "DISTANCE"
    ): Response<List<Pharmacy>>
}
