package com.example.medassist_android.data.api

import com.example.medassist_android.data.model.Medicine
import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface MedicineApiService {
    @Multipart
    @POST("api/v1/medicines/recognize")
    suspend fun recognizeMedicine(@Part image: MultipartBody.Part): Medicine
}

