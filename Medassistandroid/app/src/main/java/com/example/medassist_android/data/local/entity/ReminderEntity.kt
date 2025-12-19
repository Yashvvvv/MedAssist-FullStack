package com.example.medassist_android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a medication reminder
 */
@Entity(tableName = "medication_reminders")
data class MedicationReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicineName: String,
    val medicineId: Long? = null, // Reference to medicine in DB (optional)
    val dosage: String, // e.g., "1 tablet", "10ml"
    val frequency: ReminderFrequency,
    val times: String, // JSON array of times like ["08:00", "20:00"]
    val daysOfWeek: String? = null, // JSON array for weekly reminders like [1,3,5] for Mon,Wed,Fri
    val startDate: Long, // Timestamp
    val endDate: Long? = null, // Null means ongoing
    val instructions: String? = null, // e.g., "Take with food"
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class ReminderFrequency {
    ONCE,           // One-time reminder
    DAILY,          // Every day
    WEEKLY,         // Specific days of week
    EVERY_X_HOURS,  // Every X hours
    EVERY_X_DAYS,   // Every X days
    AS_NEEDED       // Manual tracking only
}

/**
 * Entity representing a single scheduled alarm for a reminder
 */
@Entity(tableName = "reminder_alarms")
data class ReminderAlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reminderId: Long, // Foreign key to MedicationReminderEntity
    val scheduledTime: Long, // Timestamp when alarm should fire
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val isSkipped: Boolean = false,
    val skippedReason: String? = null
)

/**
 * Entity representing medicine intake history/log
 */
@Entity(tableName = "medicine_intake_log")
data class MedicineIntakeLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reminderId: Long? = null, // Optional - can log without reminder
    val medicineName: String,
    val medicineId: Long? = null,
    val dosage: String,
    val intakeTime: Long, // When medicine was taken
    val scheduledTime: Long? = null, // When it was supposed to be taken (if from reminder)
    val status: IntakeStatus,
    val notes: String? = null,
    val sideEffectsExperienced: String? = null, // JSON array
    val mood: Int? = null, // 1-5 scale
    val createdAt: Long = System.currentTimeMillis()
)

enum class IntakeStatus {
    TAKEN,
    SKIPPED,
    MISSED,
    LATE
}
