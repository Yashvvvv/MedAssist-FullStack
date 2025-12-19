package com.example.medassist_android.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.medassist_android.data.local.converter.ReminderConverters
import com.example.medassist_android.data.local.converter.StringListConverter
import com.example.medassist_android.data.local.dao.MedicineDao
import com.example.medassist_android.data.local.dao.PharmacyDao
import com.example.medassist_android.data.local.dao.ReminderDao
import com.example.medassist_android.data.local.dao.SearchHistoryDao
import com.example.medassist_android.data.local.dao.UserFavoriteDao
import com.example.medassist_android.data.local.entity.MedicationReminderEntity
import com.example.medassist_android.data.local.entity.MedicineEntity
import com.example.medassist_android.data.local.entity.MedicineIntakeLogEntity
import com.example.medassist_android.data.local.entity.PharmacyEntity
import com.example.medassist_android.data.local.entity.ReminderAlarmEntity
import com.example.medassist_android.data.local.entity.SearchHistoryEntity
import com.example.medassist_android.data.local.entity.UserFavoriteEntity

@Database(
    entities = [
        MedicineEntity::class,
        PharmacyEntity::class,
        SearchHistoryEntity::class,
        UserFavoriteEntity::class,
        MedicationReminderEntity::class,
        ReminderAlarmEntity::class,
        MedicineIntakeLogEntity::class
    ],
    version = 3, // Incremented version from 2 to 3 for reminder tables
    exportSchema = false
)
@TypeConverters(StringListConverter::class, ReminderConverters::class)
abstract class MedAssistDatabase : RoomDatabase() {

    abstract fun medicineDao(): MedicineDao
    abstract fun pharmacyDao(): PharmacyDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun userFavoriteDao(): UserFavoriteDao
    abstract fun reminderDao(): ReminderDao

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

        // Migration from version 2 to 3 - Add reminder tables
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create medication_reminders table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS medication_reminders (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        medicineName TEXT NOT NULL,
                        medicineId INTEGER,
                        dosage TEXT NOT NULL,
                        frequency TEXT NOT NULL,
                        times TEXT NOT NULL,
                        daysOfWeek TEXT,
                        startDate INTEGER NOT NULL,
                        endDate INTEGER,
                        instructions TEXT,
                        isActive INTEGER NOT NULL DEFAULT 1,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())

                // Create reminder_alarms table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS reminder_alarms (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        reminderId INTEGER NOT NULL,
                        scheduledTime INTEGER NOT NULL,
                        isCompleted INTEGER NOT NULL DEFAULT 0,
                        completedAt INTEGER,
                        isSkipped INTEGER NOT NULL DEFAULT 0,
                        skippedReason TEXT,
                        FOREIGN KEY(reminderId) REFERENCES medication_reminders(id) ON DELETE CASCADE
                    )
                """.trimIndent())

                // Create medicine_intake_log table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS medicine_intake_log (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        reminderId INTEGER,
                        medicineName TEXT NOT NULL,
                        medicineId INTEGER,
                        dosage TEXT NOT NULL,
                        intakeTime INTEGER NOT NULL,
                        scheduledTime INTEGER,
                        status TEXT NOT NULL,
                        notes TEXT,
                        sideEffectsExperienced TEXT,
                        mood INTEGER,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(reminderId) REFERENCES medication_reminders(id) ON DELETE SET NULL
                    )
                """.trimIndent())

                // Create indexes for faster queries
                database.execSQL("CREATE INDEX IF NOT EXISTS index_reminder_alarms_reminderId ON reminder_alarms(reminderId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_reminder_alarms_scheduledTime ON reminder_alarms(scheduledTime)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_medicine_intake_log_reminderId ON medicine_intake_log(reminderId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_medicine_intake_log_intakeTime ON medicine_intake_log(intakeTime)")
            }
        }
    }
}
