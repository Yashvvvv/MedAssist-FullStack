package com.example.medassist_android.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.example.medassist_android.data.local.MedAssistDatabase
import com.example.medassist_android.data.local.TokenManager
import com.example.medassist_android.data.local.dao.MedicineDao
import com.example.medassist_android.data.local.dao.PharmacyDao
import com.example.medassist_android.data.local.dao.SearchHistoryDao
import com.example.medassist_android.data.local.dao.UserFavoriteDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "medassist_prefs")

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }

    @Provides
    @Singleton
    fun provideTokenManager(dataStore: DataStore<Preferences>): TokenManager {
        return TokenManager(dataStore)
    }

    @Provides
    @Singleton
    fun provideMedAssistDatabase(@ApplicationContext context: Context): MedAssistDatabase {
        return Room.databaseBuilder(
            context,
            MedAssistDatabase::class.java,
            MedAssistDatabase.DATABASE_NAME
        )
            .addMigrations(MedAssistDatabase.MIGRATION_1_2) // ✅ Added proper migration
            .fallbackToDestructiveMigration() // Keep as fallback for other schema changes
            .build()
    }

    @Provides
    fun provideMedicineDao(database: MedAssistDatabase): MedicineDao {
        return database.medicineDao()
    }

    @Provides
    fun providePharmacyDao(database: MedAssistDatabase): PharmacyDao {
        return database.pharmacyDao()
    }

    @Provides
    fun provideSearchHistoryDao(database: MedAssistDatabase): SearchHistoryDao {
        return database.searchHistoryDao()
    }

    @Provides
    fun provideUserFavoriteDao(database: MedAssistDatabase): UserFavoriteDao {
        return database.userFavoriteDao()
    }
}
