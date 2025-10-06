package com.example.medassist_android.di

import com.example.medassist_android.data.repository.AuthRepository
import com.example.medassist_android.data.repository.MedicineRepository
import com.example.medassist_android.data.repository.PharmacyRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideAuthRepository(
        authApiService: com.example.medassist_android.data.network.AuthApiService,
        tokenManager: com.example.medassist_android.data.local.TokenManager,
        moshi: com.squareup.moshi.Moshi
    ): AuthRepository {
        return AuthRepository(authApiService, tokenManager, moshi)
    }

    @Provides
    @Singleton
    fun provideMedicineRepository(
        medicineApiService: com.example.medassist_android.data.network.MedicineApiService,
        medicineDao: com.example.medassist_android.data.local.dao.MedicineDao,
        searchHistoryDao: com.example.medassist_android.data.local.dao.SearchHistoryDao
    ): MedicineRepository {
        return MedicineRepository(medicineApiService, medicineDao, searchHistoryDao)
    }

    @Provides
    @Singleton
    fun providePharmacyRepository(
        pharmacyApiService: com.example.medassist_android.data.network.PharmacyApiService,
        pharmacyDao: com.example.medassist_android.data.local.dao.PharmacyDao,
        moshi: com.squareup.moshi.Moshi
    ): PharmacyRepository {
        return PharmacyRepository(pharmacyApiService, pharmacyDao, moshi)
    }
}
