package com.example.medassist_android.domain.usecase.reminder

import com.example.medassist_android.data.local.entity.IntakeStatus
import com.example.medassist_android.data.local.entity.MedicationReminderEntity
import com.example.medassist_android.data.local.entity.MedicineIntakeLogEntity
import com.example.medassist_android.data.local.entity.ReminderAlarmEntity
import com.example.medassist_android.data.repository.ReminderRepository
import com.example.medassist_android.util.Resource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

// ==================== Reminder Use Cases ====================

class GetAllRemindersUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository
) {
    operator fun invoke(): Flow<List<MedicationReminderEntity>> {
        return reminderRepository.getAllReminders()
    }
}

class GetActiveRemindersUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository
) {
    operator fun invoke(): Flow<List<MedicationReminderEntity>> {
        return reminderRepository.getActiveReminders()
    }
}

class GetReminderByIdUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository
) {
    suspend operator fun invoke(id: Long): MedicationReminderEntity? {
        return reminderRepository.getReminderById(id)
    }
}

class CreateReminderUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository
) {
    operator fun invoke(reminder: MedicationReminderEntity): Flow<Resource<Long>> {
        return reminderRepository.createReminder(reminder)
    }
}

class UpdateReminderUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository
) {
    suspend operator fun invoke(reminder: MedicationReminderEntity): Resource<Unit> {
        return reminderRepository.updateReminder(reminder)
    }
}

class DeleteReminderUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository
) {
    suspend operator fun invoke(reminderId: Long): Resource<Unit> {
        return reminderRepository.deleteReminder(reminderId)
    }
}

class ToggleReminderActiveUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository
) {
    suspend operator fun invoke(reminderId: Long, isActive: Boolean): Resource<Unit> {
        return reminderRepository.toggleReminderActive(reminderId, isActive)
    }
}

// ==================== Alarm Use Cases ====================

class GetUpcomingAlarmsUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository
) {
    operator fun invoke(limit: Int = 10): Flow<List<ReminderAlarmEntity>> {
        return reminderRepository.getUpcomingAlarms(limit)
    }
}

class MarkAlarmCompletedUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository
) {
    suspend operator fun invoke(alarmId: Long): Resource<Unit> {
        return reminderRepository.markAlarmCompleted(alarmId)
    }
}

class MarkAlarmSkippedUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository
) {
    suspend operator fun invoke(alarmId: Long, reason: String? = null): Resource<Unit> {
        return reminderRepository.markAlarmSkipped(alarmId, reason)
    }
}

// ==================== Intake Log Use Cases ====================

class GetAllIntakeLogsUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository
) {
    operator fun invoke(): Flow<List<MedicineIntakeLogEntity>> {
        return reminderRepository.getAllIntakeLogs()
    }
}

class GetRecentIntakeLogsUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository
) {
    operator fun invoke(limit: Int = 20): Flow<List<MedicineIntakeLogEntity>> {
        return reminderRepository.getRecentIntakeLogs(limit)
    }
}

class GetTodayIntakeLogsUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository
) {
    operator fun invoke(): Flow<List<MedicineIntakeLogEntity>> {
        return reminderRepository.getTodayIntakeLogs()
    }
}

class GetIntakeLogsBetweenUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository
) {
    operator fun invoke(startTime: Long, endTime: Long): Flow<List<MedicineIntakeLogEntity>> {
        return reminderRepository.getIntakeLogsBetween(startTime, endTime)
    }
}

class LogMedicineIntakeUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository
) {
    operator fun invoke(
        medicineName: String,
        dosage: String,
        status: IntakeStatus,
        reminderId: Long? = null,
        medicineId: Long? = null,
        scheduledTime: Long? = null,
        notes: String? = null,
        sideEffects: String? = null,
        mood: Int? = null
    ): Flow<Resource<Long>> {
        return reminderRepository.logMedicineIntake(
            medicineName = medicineName,
            dosage = dosage,
            status = status,
            reminderId = reminderId,
            medicineId = medicineId,
            scheduledTime = scheduledTime,
            notes = notes,
            sideEffects = sideEffects,
            mood = mood
        )
    }
}

class GetIntakeStatsUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository
) {
    suspend operator fun invoke(startTime: Long, endTime: Long): ReminderRepository.IntakeStats {
        return reminderRepository.getIntakeStats(startTime, endTime)
    }
}
