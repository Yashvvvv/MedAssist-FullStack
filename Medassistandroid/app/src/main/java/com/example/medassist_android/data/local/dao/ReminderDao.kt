package com.example.medassist_android.data.local.dao

import androidx.room.*
import com.example.medassist_android.data.local.entity.MedicationReminderEntity
import com.example.medassist_android.data.local.entity.MedicineIntakeLogEntity
import com.example.medassist_android.data.local.entity.ReminderAlarmEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    
    // ==================== Medication Reminders ====================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: MedicationReminderEntity): Long
    
    @Update
    suspend fun updateReminder(reminder: MedicationReminderEntity)
    
    @Delete
    suspend fun deleteReminder(reminder: MedicationReminderEntity)
    
    @Query("DELETE FROM medication_reminders WHERE id = :reminderId")
    suspend fun deleteReminderById(reminderId: Long)
    
    @Query("SELECT * FROM medication_reminders WHERE id = :id")
    suspend fun getReminderById(id: Long): MedicationReminderEntity?
    
    @Query("SELECT * FROM medication_reminders ORDER BY createdAt DESC")
    fun getAllReminders(): Flow<List<MedicationReminderEntity>>
    
    @Query("SELECT * FROM medication_reminders WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getActiveReminders(): Flow<List<MedicationReminderEntity>>
    
    @Query("SELECT * FROM medication_reminders WHERE medicineId = :medicineId")
    fun getRemindersForMedicine(medicineId: Long): Flow<List<MedicationReminderEntity>>
    
    @Query("UPDATE medication_reminders SET isActive = :isActive, updatedAt = :updatedAt WHERE id = :reminderId")
    suspend fun updateReminderActiveStatus(reminderId: Long, isActive: Boolean, updatedAt: Long = System.currentTimeMillis())
    
    // ==================== Reminder Alarms ====================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: ReminderAlarmEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarms(alarms: List<ReminderAlarmEntity>)
    
    @Update
    suspend fun updateAlarm(alarm: ReminderAlarmEntity)
    
    @Query("DELETE FROM reminder_alarms WHERE reminderId = :reminderId")
    suspend fun deleteAlarmsForReminder(reminderId: Long)
    
    @Query("SELECT * FROM reminder_alarms WHERE reminderId = :reminderId ORDER BY scheduledTime ASC")
    fun getAlarmsForReminder(reminderId: Long): Flow<List<ReminderAlarmEntity>>
    
    @Query("SELECT * FROM reminder_alarms WHERE scheduledTime > :fromTime AND isCompleted = 0 AND isSkipped = 0 ORDER BY scheduledTime ASC LIMIT :limit")
    fun getUpcomingAlarms(fromTime: Long = System.currentTimeMillis(), limit: Int = 10): Flow<List<ReminderAlarmEntity>>
    
    @Query("SELECT * FROM reminder_alarms WHERE id = :alarmId")
    suspend fun getAlarmById(alarmId: Long): ReminderAlarmEntity?
    
    @Query("UPDATE reminder_alarms SET isCompleted = 1, completedAt = :completedAt WHERE id = :alarmId")
    suspend fun markAlarmCompleted(alarmId: Long, completedAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE reminder_alarms SET isSkipped = 1, skippedReason = :reason WHERE id = :alarmId")
    suspend fun markAlarmSkipped(alarmId: Long, reason: String?)
    
    // ==================== Medicine Intake Log ====================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntakeLog(log: MedicineIntakeLogEntity): Long
    
    @Update
    suspend fun updateIntakeLog(log: MedicineIntakeLogEntity)
    
    @Delete
    suspend fun deleteIntakeLog(log: MedicineIntakeLogEntity)
    
    @Query("SELECT * FROM medicine_intake_log ORDER BY intakeTime DESC")
    fun getAllIntakeLogs(): Flow<List<MedicineIntakeLogEntity>>
    
    @Query("SELECT * FROM medicine_intake_log WHERE intakeTime BETWEEN :startTime AND :endTime ORDER BY intakeTime DESC")
    fun getIntakeLogsBetween(startTime: Long, endTime: Long): Flow<List<MedicineIntakeLogEntity>>
    
    @Query("SELECT * FROM medicine_intake_log WHERE medicineName LIKE '%' || :medicineName || '%' ORDER BY intakeTime DESC")
    fun getIntakeLogsForMedicine(medicineName: String): Flow<List<MedicineIntakeLogEntity>>
    
    @Query("SELECT * FROM medicine_intake_log WHERE medicineId = :medicineId ORDER BY intakeTime DESC")
    fun getIntakeLogsForMedicineId(medicineId: Long): Flow<List<MedicineIntakeLogEntity>>
    
    @Query("SELECT * FROM medicine_intake_log WHERE reminderId = :reminderId ORDER BY intakeTime DESC")
    fun getIntakeLogsForReminder(reminderId: Long): Flow<List<MedicineIntakeLogEntity>>
    
    @Query("SELECT * FROM medicine_intake_log ORDER BY intakeTime DESC LIMIT :limit")
    fun getRecentIntakeLogs(limit: Int = 20): Flow<List<MedicineIntakeLogEntity>>
    
    @Query("SELECT COUNT(*) FROM medicine_intake_log WHERE intakeTime BETWEEN :startTime AND :endTime AND status = 'TAKEN'")
    suspend fun getTakenCountBetween(startTime: Long, endTime: Long): Int
    
    @Query("SELECT COUNT(*) FROM medicine_intake_log WHERE intakeTime BETWEEN :startTime AND :endTime AND status = 'MISSED'")
    suspend fun getMissedCountBetween(startTime: Long, endTime: Long): Int
    
    @Query("SELECT COUNT(*) FROM medicine_intake_log WHERE intakeTime BETWEEN :startTime AND :endTime")
    suspend fun getTotalCountBetween(startTime: Long, endTime: Long): Int
    
    // Get today's logs
    @Query("""
        SELECT * FROM medicine_intake_log 
        WHERE intakeTime >= :startOfDay AND intakeTime < :endOfDay 
        ORDER BY intakeTime DESC
    """)
    fun getTodayIntakeLogs(startOfDay: Long, endOfDay: Long): Flow<List<MedicineIntakeLogEntity>>
}
