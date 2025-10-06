package com.example.medassist_android.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.medassist_android.data.local.converter.StringListConverter
import com.example.medassist_android.data.local.dao.MedicineDao
import com.example.medassist_android.data.local.dao.PharmacyDao
import com.example.medassist_android.data.local.dao.SearchHistoryDao
import com.example.medassist_android.data.local.dao.UserFavoriteDao
import com.example.medassist_android.data.local.entity.MedicineEntity
import com.example.medassist_android.data.local.entity.PharmacyEntity
import com.example.medassist_android.data.local.entity.SearchHistoryEntity
import com.example.medassist_android.data.local.entity.UserFavoriteEntity

@Database(
    entities = [
        MedicineEntity::class,
        PharmacyEntity::class,
        SearchHistoryEntity::class,
        UserFavoriteEntity::class
    ],
    version = 2, // ✅ Incremented version from 1 to 2
    exportSchema = false
)
@TypeConverters(StringListConverter::class)
abstract class MedAssistDatabase : RoomDatabase() {

    abstract fun medicineDao(): MedicineDao
    abstract fun pharmacyDao(): PharmacyDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun userFavoriteDao(): UserFavoriteDao

    companion object {
        const val DATABASE_NAME = "medassist_database"

        // Migration from version 1 to 2 - Fix Medicine schema
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create a new table with the correct schema
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS medicines_new (
                        id INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        genericName TEXT NOT NULL,
                        brandNames TEXT NOT NULL,
                        description TEXT,
                        usageDescription TEXT,
                        dosageInformation TEXT,
                        sideEffects TEXT NOT NULL,
                        manufacturer TEXT NOT NULL,
                        category TEXT NOT NULL,
                        form TEXT NOT NULL,
                        strength TEXT NOT NULL,
                        activeIngredient TEXT NOT NULL,
                        requiresPrescription INTEGER NOT NULL,
                        storageInstructions TEXT,
                        createdAt TEXT NOT NULL,
                        updatedAt TEXT NOT NULL,
                        isFavorite INTEGER NOT NULL DEFAULT 0,
                        lastSearched INTEGER,
                        PRIMARY KEY(id)
                    )
                """.trimIndent())

                // Copy data from old table to new table
                // Convert activeIngredients list to single activeIngredient string
                database.execSQL("""
                    INSERT INTO medicines_new 
                    SELECT 
                        id, name, genericName, brandNames, description, usageDescription,
                        dosageInformation, sideEffects, manufacturer, category, form, strength,
                        CASE 
                            WHEN activeIngredients LIKE '[%]' THEN 
                                REPLACE(REPLACE(REPLACE(activeIngredients, '[', ''), ']', ''), '"', '')
                            ELSE activeIngredients 
                        END as activeIngredient,
                        requiresPrescription,
                        NULL as storageInstructions,
                        createdAt, updatedAt, isFavorite, lastSearched
                    FROM medicines
                """.trimIndent())

                // Drop old table
                database.execSQL("DROP TABLE medicines")

                // Rename new table to original name
                database.execSQL("ALTER TABLE medicines_new RENAME TO medicines")
            }
        }
    }
}
