package com.example.medassist_android.data.repository

import com.example.medassist_android.data.local.dao.ReminderDao
import com.example.medassist_android.data.local.entity.IntakeStatus
import com.example.medassist_android.data.local.entity.MedicationReminderEntity
import com.example.medassist_android.data.local.entity.MedicineIntakeLogEntity
import com.example.medassist_android.data.local.entity.ReminderAlarmEntity
import com.example.medassist_android.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderRepository @Inject constructor(
    private val reminderDao: ReminderDao
) {

    // ==================== Medication Reminders ====================

    fun getAllReminders(): Flow<List<MedicationReminderEntity>> {
        return reminderDao.getAllReminders()
    }

    fun getActiveReminders(): Flow<List<MedicationReminderEntity>> {
        return reminderDao.getActiveReminders()
    }

    fun getRemindersForMedicine(medicineId: Long): Flow<List<MedicationReminderEntity>> {
        return reminderDao.getRemindersForMedicine(medicineId)
    }

    suspend fun getReminderById(id: Long): MedicationReminderEntity? {
        return reminderDao.getReminderById(id)
    }

    fun createReminder(reminder: MedicationReminderEntity): Flow<Resource<Long>> = flow {
        try {
            emit(Resource.Loading())
            val id = reminderDao.insertReminder(reminder)
            emit(Resource.Success(id))
        } catch (e: Exception) {
            Timber.e(e, "Error creating reminder")
            emit(Resource.Error("Failed to create reminder: ${e.message}"))
        }
    }

    suspend fun updateReminder(reminder: MedicationReminderEntity): Resource<Unit> {
        return try {
            reminderDao.updateReminder(reminder.copy(updatedAt = System.currentTimeMillis()))
            Resource.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error updating reminder")
            Resource.Error("Failed to update reminder: ${e.message}")
        }
    }

    suspend fun deleteReminder(reminderId: Long): Resource<Unit> {
        return try {
            reminderDao.deleteReminderById(reminderId)
            reminderDao.deleteAlarmsForReminder(reminderId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error deleting reminder")
            Resource.Error("Failed to delete reminder: ${e.message}")
        }
    }

    suspend fun toggleReminderActive(reminderId: Long, isActive: Boolean): Resource<Unit> {
        return try {
            reminderDao.updateReminderActiveStatus(reminderId, isActive)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error toggling reminder status")
            Resource.Error("Failed to update reminder status: ${e.message}")
        }
    }

    // ==================== Reminder Alarms ====================

    fun getUpcomingAlarms(limit: Int = 10): Flow<List<ReminderAlarmEntity>> {
        return reminderDao.getUpcomingAlarms(System.currentTimeMillis(), limit)
    }

    fun getAlarmsForReminder(reminderId: Long): Flow<List<ReminderAlarmEntity>> {
        return reminderDao.getAlarmsForReminder(reminderId)
    }

    suspend fun getAlarmById(alarmId: Long): ReminderAlarmEntity? {
        return reminderDao.getAlarmById(alarmId)
    }

    suspend fun createAlarm(alarm: ReminderAlarmEntity): Long {
        return reminderDao.insertAlarm(alarm)
    }

    suspend fun createAlarms(alarms: List<ReminderAlarmEntity>) {
        reminderDao.insertAlarms(alarms)
    }

    suspend fun markAlarmCompleted(alarmId: Long): Resource<Unit> {
        return try {
            reminderDao.markAlarmCompleted(alarmId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error marking alarm completed")
            Resource.Error("Failed to mark alarm completed: ${e.message}")
        }
    }

    suspend fun markAlarmSkipped(alarmId: Long, reason: String? = null): Resource<Unit> {
        return try {
            reminderDao.markAlarmSkipped(alarmId, reason)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error marking alarm skipped")
            Resource.Error("Failed to skip alarm: ${e.message}")
        }
    }

    // ==================== Medicine Intake Log ====================

    fun getAllIntakeLogs(): Flow<List<MedicineIntakeLogEntity>> {
        return reminderDao.getAllIntakeLogs()
    }

    fun getRecentIntakeLogs(limit: Int = 20): Flow<List<MedicineIntakeLogEntity>> {
        return reminderDao.getRecentIntakeLogs(limit)
    }

    fun getIntakeLogsBetween(startTime: Long, endTime: Long): Flow<List<MedicineIntakeLogEntity>> {
        return reminderDao.getIntakeLogsBetween(startTime, endTime)
    }

    fun getIntakeLogsForMedicine(medicineName: String): Flow<List<MedicineIntakeLogEntity>> {
        return reminderDao.getIntakeLogsForMedicine(medicineName)
    }

    fun getIntakeLogsForReminder(reminderId: Long): Flow<List<MedicineIntakeLogEntity>> {
        return reminderDao.getIntakeLogsForReminder(reminderId)
    }

    fun getTodayIntakeLogs(): Flow<List<MedicineIntakeLogEntity>> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis
        
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endOfDay = calendar.timeInMillis
        
        return reminderDao.getTodayIntakeLogs(startOfDay, endOfDay)
    }

    fun logMedicineIntake(
        medicineName: String,
        dosage: String,
        status: IntakeStatus,
        reminderId: Long? = null,
        medicineId: Long? = null,
        scheduledTime: Long? = null,
        notes: String? = null,
        sideEffects: String? = null,
        mood: Int? = null
    ): Flow<Resource<Long>> = flow {
        try {
            emit(Resource.Loading())
            val log = MedicineIntakeLogEntity(
                reminderId = reminderId,
                medicineName = medicineName,
                medicineId = medicineId,
                dosage = dosage,
                intakeTime = System.currentTimeMillis(),
                scheduledTime = scheduledTime,
                status = status,
                notes = notes,
                sideEffectsExperienced = sideEffects,
                mood = mood
            )
            val id = reminderDao.insertIntakeLog(log)
            emit(Resource.Success(id))
        } catch (e: Exception) {
            Timber.e(e, "Error logging medicine intake")
            emit(Resource.Error("Failed to log intake: ${e.message}"))
        }
    }

    suspend fun updateIntakeLog(log: MedicineIntakeLogEntity): Resource<Unit> {
        return try {
            reminderDao.updateIntakeLog(log)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error updating intake log")
            Resource.Error("Failed to update intake log: ${e.message}")
        }
    }

    suspend fun deleteIntakeLog(log: MedicineIntakeLogEntity): Resource<Unit> {
        return try {
            reminderDao.deleteIntakeLog(log)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error deleting intake log")
            Resource.Error("Failed to delete intake log: ${e.message}")
        }
    }

    // ==================== Statistics ====================

    suspend fun getIntakeStats(startTime: Long, endTime: Long): IntakeStats {
        val taken = reminderDao.getTakenCountBetween(startTime, endTime)
        val missed = reminderDao.getMissedCountBetween(startTime, endTime)
        val total = reminderDao.getTotalCountBetween(startTime, endTime)
        
        val adherenceRate = if (total > 0) {
            (taken.toFloat() / total.toFloat()) * 100
        } else {
            0f
        }
        
        return IntakeStats(
            taken = taken,
            missed = missed,
            total = total,
            adherenceRate = adherenceRate
        )
    }

    data class IntakeStats(
        val taken: Int,
        val missed: Int,
        val total: Int,
        val adherenceRate: Float
    )
}
