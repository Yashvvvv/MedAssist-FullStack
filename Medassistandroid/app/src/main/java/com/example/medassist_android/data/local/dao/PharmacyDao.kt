package com.example.medassist_android.data.local.dao

import androidx.room.*
import com.example.medassist_android.data.local.entity.PharmacyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PharmacyDao {

    @Query("SELECT * FROM pharmacies ORDER BY name ASC")
    fun getAllPharmacies(): Flow<List<PharmacyEntity>>

    @Query("SELECT * FROM pharmacies WHERE id = :id")
    suspend fun getPharmacyById(id: Long): PharmacyEntity?

    @Query("SELECT * FROM pharmacies WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavoritePharmacies(): Flow<List<PharmacyEntity>>

    @Query("SELECT * FROM pharmacies WHERE lastViewed IS NOT NULL ORDER BY lastViewed DESC LIMIT 20")
    fun getRecentlyViewedPharmacies(): Flow<List<PharmacyEntity>>

    @Query("SELECT * FROM pharmacies WHERE name LIKE '%' || :query || '%' OR address LIKE '%' || :query || '%' ORDER BY name ASC")
    suspend fun searchPharmacies(query: String): List<PharmacyEntity>

    @Query("SELECT * FROM pharmacies WHERE distance IS NOT NULL ORDER BY distance ASC")
    suspend fun getNearbyPharmacies(): List<PharmacyEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPharmacy(pharmacy: PharmacyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPharmacies(pharmacies: List<PharmacyEntity>)

    @Update
    suspend fun updatePharmacy(pharmacy: PharmacyEntity)

    @Query("UPDATE pharmacies SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Long, isFavorite: Boolean)

    @Query("UPDATE pharmacies SET lastViewed = :timestamp WHERE id = :id")
    suspend fun updateLastViewed(id: Long, timestamp: Long)

    @Delete
    suspend fun deletePharmacy(pharmacy: PharmacyEntity)

    @Query("DELETE FROM pharmacies WHERE id = :id")
    suspend fun deletePharmacyById(id: Long)

    @Query("DELETE FROM pharmacies")
    suspend fun deleteAllPharmacies()
}

@Dao
interface SearchHistoryDao {

    @Query("SELECT * FROM search_history ORDER BY timestamp DESC")
    fun getAllSearchHistory(): Flow<List<com.example.medassist_android.data.local.entity.SearchHistoryEntity>>

    @Query("SELECT * FROM search_history WHERE searchType = :searchType ORDER BY timestamp DESC LIMIT 20")
    fun getSearchHistoryByType(searchType: String): Flow<List<com.example.medassist_android.data.local.entity.SearchHistoryEntity>>

    @Query("SELECT * FROM search_history WHERE userId = :userId ORDER BY timestamp DESC LIMIT 50")
    fun getSearchHistoryByUser(userId: Long): Flow<List<com.example.medassist_android.data.local.entity.SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchHistory(searchHistory: com.example.medassist_android.data.local.entity.SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE timestamp < :timestamp")
    suspend fun deleteOldSearchHistory(timestamp: Long)

    @Query("DELETE FROM search_history")
    suspend fun deleteAllSearchHistory()
}

@Dao
interface UserFavoriteDao {

    @Query("SELECT * FROM user_favorites WHERE userId = :userId ORDER BY addedAt DESC")
    fun getUserFavorites(userId: Long): Flow<List<com.example.medassist_android.data.local.entity.UserFavoriteEntity>>

    @Query("SELECT * FROM user_favorites WHERE userId = :userId AND itemType = :itemType ORDER BY addedAt DESC")
    fun getUserFavoritesByType(userId: Long, itemType: String): Flow<List<com.example.medassist_android.data.local.entity.UserFavoriteEntity>>

    @Query("SELECT * FROM user_favorites WHERE userId = :userId AND itemId = :itemId AND itemType = :itemType")
    suspend fun getFavorite(userId: Long, itemId: Long, itemType: String): com.example.medassist_android.data.local.entity.UserFavoriteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: com.example.medassist_android.data.local.entity.UserFavoriteEntity)

    @Query("DELETE FROM user_favorites WHERE userId = :userId AND itemId = :itemId AND itemType = :itemType")
    suspend fun deleteFavorite(userId: Long, itemId: Long, itemType: String)

    @Query("DELETE FROM user_favorites WHERE userId = :userId")
    suspend fun deleteAllUserFavorites(userId: Long)
}
