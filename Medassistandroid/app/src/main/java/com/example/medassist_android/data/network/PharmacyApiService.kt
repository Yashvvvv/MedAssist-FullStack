package com.example.medassist_android.data.network

import com.example.medassist_android.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface PharmacyApiService {

    @GET("api/v1/pharmacies")
    suspend fun getAllPharmacies(): Response<List<Pharmacy>>

    @GET("api/v1/pharmacies/{id}")
    suspend fun getPharmacyById(@Path("id") id: Long): Response<Pharmacy>

    @POST("api/v1/pharmacies")
    suspend fun createPharmacy(@Body request: CreatePharmacyRequest): Response<Pharmacy>

    @PUT("api/v1/pharmacies/{id}")
    suspend fun updatePharmacy(@Path("id") id: Long, @Body request: CreatePharmacyRequest): Response<Pharmacy>

    @DELETE("api/v1/pharmacies/{id}")
    suspend fun deletePharmacy(@Path("id") id: Long): Response<Unit>

    @PATCH("api/v1/pharmacies/{id}/deactivate")
    suspend fun deactivatePharmacy(@Path("id") id: Long): Response<Unit>

    @GET("api/v1/pharmacies/search")
    suspend fun searchPharmacies(@Query("q") query: String): Response<List<Pharmacy>>

    @GET("api/v1/pharmacies/search/name")
    suspend fun searchByName(@Query("name") name: String): Response<List<Pharmacy>>

    @GET("api/v1/pharmacies/search/location")
    suspend fun searchByLocation(
        @Query("city") city: String,
        @Query("state") state: String? = null
    ): Response<List<Pharmacy>>

    @GET("api/v1/pharmacies/search/zipcode")
    suspend fun searchByZipCode(@Query("zipCode") zipCode: String): Response<List<Pharmacy>>

    @GET("api/v1/pharmacies/search/chain")
    suspend fun searchByChain(@Query("chainName") chainName: String): Response<List<Pharmacy>>

    // Feature-based search endpoints
    @GET("api/v1/pharmacies/24hours")
    suspend fun get24HourPharmacies(): Response<List<Pharmacy>>

    @GET("api/v1/pharmacies/delivery")
    suspend fun getPharmaciesWithDelivery(): Response<List<Pharmacy>>

    @GET("api/v1/pharmacies/drive-through")
    suspend fun getPharmaciesWithDriveThrough(): Response<List<Pharmacy>>

    @GET("api/v1/pharmacies/consultation")
    suspend fun getPharmaciesWithConsultation(): Response<List<Pharmacy>>

    @GET("api/v1/pharmacies/insurance")
    suspend fun getPharmaciesThatAcceptInsurance(): Response<List<Pharmacy>>

    @GET("api/v1/pharmacies/service")
    suspend fun getPharmaciesByService(@Query("service") service: String): Response<List<Pharmacy>>

    // Basic nearby search from PharmacyController
    @GET("api/v1/pharmacies/nearby")
    suspend fun getNearbyPharmaciesBasic(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("radius") radius: Double = 10.0
    ): Response<List<Pharmacy>>

    // Location-based endpoints from PharmacyLocationController
    @POST("api/v1/pharmacies/location/nearby")
    suspend fun getNearbyPharmacies(@Body request: NearbyPharmaciesRequest): Response<List<PharmacyLocationResponse>>

    @GET("api/v1/pharmacies/location/nearby")
    suspend fun getNearbyPharmaciesGet(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("radius") radius: Double = 10.0,
        @Query("maxResults") maxResults: Int = 20,
        @Query("openNow") openNow: Boolean? = null,
        @Query("hasDelivery") hasDelivery: Boolean? = null,
        @Query("hasDriveThrough") hasDriveThrough: Boolean? = null,
        @Query("acceptsInsurance") acceptsInsurance: Boolean? = null,
        @Query("is24Hours") is24Hours: Boolean? = null,
        @Query("chainName") chainName: String? = null,
        @Query("medicineName") medicineName: String? = null,
        @Query("sortBy") sortBy: String = "DISTANCE"
    ): Response<List<PharmacyLocationResponse>>
}
