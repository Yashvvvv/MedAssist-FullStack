package com.example.medassist_android.data.local.dao

import androidx.room.*
import com.example.medassist_android.data.local.entity.MedicineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicineDao {

    @Query("SELECT * FROM medicines ORDER BY name ASC")
    fun getAllMedicines(): Flow<List<MedicineEntity>>

    @Query("SELECT * FROM medicines WHERE id = :id")
    suspend fun getMedicineById(id: Long): MedicineEntity?

    @Query("SELECT * FROM medicines WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavoriteMedicines(): Flow<List<MedicineEntity>>

    @Query("SELECT * FROM medicines WHERE lastSearched IS NOT NULL ORDER BY lastSearched DESC LIMIT 20")
    fun getRecentlySearchedMedicines(): Flow<List<MedicineEntity>>

    @Query("SELECT * FROM medicines WHERE name LIKE '%' || :query || '%' OR genericName LIKE '%' || :query || '%' OR manufacturer LIKE '%' || :query || '%' ORDER BY name ASC")
    suspend fun searchMedicines(query: String): List<MedicineEntity>

    @Query("SELECT * FROM medicines WHERE category = :category ORDER BY name ASC")
    suspend fun getMedicinesByCategory(category: String): List<MedicineEntity>

    @Query("SELECT * FROM medicines WHERE requiresPrescription = :requiresPrescription ORDER BY name ASC")
    suspend fun getMedicinesByPrescription(requiresPrescription: Boolean): List<MedicineEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicine(medicine: MedicineEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicines(medicines: List<MedicineEntity>)

    @Update
    suspend fun updateMedicine(medicine: MedicineEntity)

    @Query("UPDATE medicines SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Long, isFavorite: Boolean)

    @Query("UPDATE medicines SET lastSearched = :timestamp WHERE id = :id")
    suspend fun updateLastSearched(id: Long, timestamp: Long)

    @Delete
    suspend fun deleteMedicine(medicine: MedicineEntity)

    @Query("DELETE FROM medicines WHERE id = :id")
    suspend fun deleteMedicineById(id: Long)

    @Query("DELETE FROM medicines")
    suspend fun deleteAllMedicines()

    @Query("SELECT DISTINCT category FROM medicines ORDER BY category ASC")
    suspend fun getAllCategories(): List<String>

    @Query("SELECT DISTINCT manufacturer FROM medicines ORDER BY manufacturer ASC")
    suspend fun getAllManufacturers(): List<String>

    @Query("SELECT DISTINCT form FROM medicines ORDER BY form ASC")
    suspend fun getAllForms(): List<String>
}
