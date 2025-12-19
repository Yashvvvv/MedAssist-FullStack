package com.example.medassist_android.presentation.reminder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medassist_android.data.local.entity.IntakeStatus
import com.example.medassist_android.data.local.entity.MedicationReminderEntity
import com.example.medassist_android.data.local.entity.MedicineIntakeLogEntity
import com.example.medassist_android.data.local.entity.ReminderAlarmEntity
import com.example.medassist_android.data.local.entity.ReminderFrequency
import com.example.medassist_android.data.repository.ReminderRepository
import com.example.medassist_android.domain.usecase.reminder.*
import com.example.medassist_android.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*
import javax.inject.Inject

data class ReminderListUiState(
    val reminders: List<MedicationReminderEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class ReminderFormUiState(
    val medicineName: String = "",
    val medicineId: Long? = null,
    val dosage: String = "",
    val frequency: ReminderFrequency = ReminderFrequency.DAILY,
    val times: List<String> = listOf("09:00"),
    val daysOfWeek: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7), // All days
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long? = null,
    val instructions: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSaved: Boolean = false
)

data class IntakeLogUiState(
    val logs: List<MedicineIntakeLogEntity> = emptyList(),
    val todayLogs: List<MedicineIntakeLogEntity> = emptyList(),
    val stats: ReminderRepository.IntakeStats? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class UpcomingAlarmsUiState(
    val alarms: List<ReminderAlarmEntity> = emptyList(),
    val reminderMap: Map<Long, MedicationReminderEntity> = emptyMap(),
    val isLoading: Boolean = false
)

@HiltViewModel
class ReminderViewModel @Inject constructor(
    private val getAllRemindersUseCase: GetAllRemindersUseCase,
    private val getActiveRemindersUseCase: GetActiveRemindersUseCase,
    private val getReminderByIdUseCase: GetReminderByIdUseCase,
    private val createReminderUseCase: CreateReminderUseCase,
    private val updateReminderUseCase: UpdateReminderUseCase,
    private val deleteReminderUseCase: DeleteReminderUseCase,
    private val toggleReminderActiveUseCase: ToggleReminderActiveUseCase,
    private val getUpcomingAlarmsUseCase: GetUpcomingAlarmsUseCase,
    private val markAlarmCompletedUseCase: MarkAlarmCompletedUseCase,
    private val markAlarmSkippedUseCase: MarkAlarmSkippedUseCase,
    private val getAllIntakeLogsUseCase: GetAllIntakeLogsUseCase,
    private val getRecentIntakeLogsUseCase: GetRecentIntakeLogsUseCase,
    private val getTodayIntakeLogsUseCase: GetTodayIntakeLogsUseCase,
    private val logMedicineIntakeUseCase: LogMedicineIntakeUseCase,
    private val getIntakeStatsUseCase: GetIntakeStatsUseCase
) : ViewModel() {

    private val _reminderListState = MutableStateFlow(ReminderListUiState())
    val reminderListState: StateFlow<ReminderListUiState> = _reminderListState.asStateFlow()

    private val _reminderFormState = MutableStateFlow(ReminderFormUiState())
    val reminderFormState: StateFlow<ReminderFormUiState> = _reminderFormState.asStateFlow()

    private val _intakeLogState = MutableStateFlow(IntakeLogUiState())
    val intakeLogState: StateFlow<IntakeLogUiState> = _intakeLogState.asStateFlow()

    private val _upcomingAlarmsState = MutableStateFlow(UpcomingAlarmsUiState())
    val upcomingAlarmsState: StateFlow<UpcomingAlarmsUiState> = _upcomingAlarmsState.asStateFlow()

    init {
        loadReminders()
        loadTodayLogs()
        loadUpcomingAlarms()
        loadWeeklyStats()
    }

    // ==================== Reminders ====================

    fun loadReminders() {
        viewModelScope.launch {
            _reminderListState.value = _reminderListState.value.copy(isLoading = true)
            getAllRemindersUseCase().collect { reminders ->
                _reminderListState.value = ReminderListUiState(
                    reminders = reminders,
                    isLoading = false
                )
            }
        }
    }

    fun updateFormField(field: String, value: Any) {
        _reminderFormState.value = when (field) {
            "medicineName" -> _reminderFormState.value.copy(medicineName = value as String)
            "medicineId" -> _reminderFormState.value.copy(medicineId = value as? Long)
            "dosage" -> _reminderFormState.value.copy(dosage = value as String)
            "frequency" -> _reminderFormState.value.copy(frequency = value as ReminderFrequency)
            "times" -> _reminderFormState.value.copy(times = value as List<String>)
            "daysOfWeek" -> _reminderFormState.value.copy(daysOfWeek = value as List<Int>)
            "startDate" -> _reminderFormState.value.copy(startDate = value as Long)
            "endDate" -> _reminderFormState.value.copy(endDate = value as? Long)
            "instructions" -> _reminderFormState.value.copy(instructions = value as String)
            else -> _reminderFormState.value
        }
    }

    fun addTime(time: String) {
        val currentTimes = _reminderFormState.value.times.toMutableList()
        if (!currentTimes.contains(time)) {
            currentTimes.add(time)
            currentTimes.sort()
            _reminderFormState.value = _reminderFormState.value.copy(times = currentTimes)
        }
    }

    fun removeTime(time: String) {
        val currentTimes = _reminderFormState.value.times.toMutableList()
        currentTimes.remove(time)
        _reminderFormState.value = _reminderFormState.value.copy(times = currentTimes)
    }

    fun toggleDayOfWeek(day: Int) {
        val currentDays = _reminderFormState.value.daysOfWeek.toMutableList()
        if (currentDays.contains(day)) {
            currentDays.remove(day)
        } else {
            currentDays.add(day)
            currentDays.sort()
        }
        _reminderFormState.value = _reminderFormState.value.copy(daysOfWeek = currentDays)
    }

    fun saveReminder() {
        val formState = _reminderFormState.value
        
        if (formState.medicineName.isBlank()) {
            _reminderFormState.value = formState.copy(error = "Medicine name is required")
            return
        }
        if (formState.dosage.isBlank()) {
            _reminderFormState.value = formState.copy(error = "Dosage is required")
            return
        }
        if (formState.times.isEmpty()) {
            _reminderFormState.value = formState.copy(error = "At least one time is required")
            return
        }

        viewModelScope.launch {
            val reminder = MedicationReminderEntity(
                medicineName = formState.medicineName,
                medicineId = formState.medicineId,
                dosage = formState.dosage,
                frequency = formState.frequency,
                times = formState.times.joinToString(","),
                daysOfWeek = if (formState.frequency == ReminderFrequency.WEEKLY) {
                    formState.daysOfWeek.joinToString(",")
                } else null,
                startDate = formState.startDate,
                endDate = formState.endDate,
                instructions = formState.instructions.ifBlank { null }
            )

            createReminderUseCase(reminder).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _reminderFormState.value = formState.copy(isLoading = true, error = null)
                    }
                    is Resource.Success -> {
                        _reminderFormState.value = ReminderFormUiState(isSaved = true)
                        loadReminders()
                    }
                    is Resource.Error -> {
                        _reminderFormState.value = formState.copy(
                            isLoading = false,
                            error = resource.message
                        )
                    }
                }
            }
        }
    }

    fun deleteReminder(reminderId: Long) {
        viewModelScope.launch {
            when (val result = deleteReminderUseCase(reminderId)) {
                is Resource.Success -> {
                    loadReminders()
                }
                is Resource.Error -> {
                    _reminderListState.value = _reminderListState.value.copy(
                        error = result.message
                    )
                }
                else -> {}
            }
        }
    }

    fun toggleReminderActive(reminderId: Long, isActive: Boolean) {
        viewModelScope.launch {
            toggleReminderActiveUseCase(reminderId, isActive)
            loadReminders()
        }
    }

    fun resetForm() {
        _reminderFormState.value = ReminderFormUiState()
    }

    fun loadReminderForEdit(reminderId: Long) {
        viewModelScope.launch {
            val reminder = getReminderByIdUseCase(reminderId)
            reminder?.let {
                _reminderFormState.value = ReminderFormUiState(
                    medicineName = it.medicineName,
                    medicineId = it.medicineId,
                    dosage = it.dosage,
                    frequency = it.frequency,
                    times = it.times.split(",").filter { t -> t.isNotBlank() },
                    daysOfWeek = it.daysOfWeek?.split(",")?.mapNotNull { d -> d.toIntOrNull() } ?: listOf(1, 2, 3, 4, 5, 6, 7),
                    startDate = it.startDate,
                    endDate = it.endDate,
                    instructions = it.instructions ?: ""
                )
            }
        }
    }

    // ==================== Upcoming Alarms ====================

    private fun loadUpcomingAlarms() {
        viewModelScope.launch {
            _upcomingAlarmsState.value = _upcomingAlarmsState.value.copy(isLoading = true)
            
            // Combine alarms with their reminders
            combine(
                getUpcomingAlarmsUseCase(10),
                getAllRemindersUseCase()
            ) { alarms, reminders ->
                val reminderMap = reminders.associateBy { it.id }
                UpcomingAlarmsUiState(
                    alarms = alarms,
                    reminderMap = reminderMap,
                    isLoading = false
                )
            }.collect { state ->
                _upcomingAlarmsState.value = state
            }
        }
    }

    fun markAlarmTaken(alarmId: Long, reminderId: Long) {
        viewModelScope.launch {
            val reminder = getReminderByIdUseCase(reminderId)
            if (reminder != null) {
                // Mark alarm completed
                markAlarmCompletedUseCase(alarmId)
                
                // Log the intake
                logMedicineIntakeUseCase(
                    medicineName = reminder.medicineName,
                    dosage = reminder.dosage,
                    status = IntakeStatus.TAKEN,
                    reminderId = reminderId,
                    medicineId = reminder.medicineId
                ).collect { /* Handle result */ }
                
                loadUpcomingAlarms()
                loadTodayLogs()
            }
        }
    }

    fun markAlarmSkipped(alarmId: Long, reminderId: Long, reason: String? = null) {
        viewModelScope.launch {
            val reminder = getReminderByIdUseCase(reminderId)
            if (reminder != null) {
                markAlarmSkippedUseCase(alarmId, reason)
                
                logMedicineIntakeUseCase(
                    medicineName = reminder.medicineName,
                    dosage = reminder.dosage,
                    status = IntakeStatus.SKIPPED,
                    reminderId = reminderId,
                    medicineId = reminder.medicineId,
                    notes = reason
                ).collect { /* Handle result */ }
                
                loadUpcomingAlarms()
                loadTodayLogs()
            }
        }
    }

    // ==================== Intake Logs ====================

    fun loadAllLogs() {
        viewModelScope.launch {
            _intakeLogState.value = _intakeLogState.value.copy(isLoading = true)
            getAllIntakeLogsUseCase().collect { logs ->
                _intakeLogState.value = _intakeLogState.value.copy(
                    logs = logs,
                    isLoading = false
                )
            }
        }
    }

    private fun loadTodayLogs() {
        viewModelScope.launch {
            getTodayIntakeLogsUseCase().collect { logs ->
                _intakeLogState.value = _intakeLogState.value.copy(
                    todayLogs = logs
                )
            }
        }
    }

    private fun loadWeeklyStats() {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfWeek = calendar.timeInMillis
            
            calendar.add(Calendar.WEEK_OF_YEAR, 1)
            val endOfWeek = calendar.timeInMillis
            
            val stats = getIntakeStatsUseCase(startOfWeek, endOfWeek)
            _intakeLogState.value = _intakeLogState.value.copy(stats = stats)
        }
    }

    fun logManualIntake(
        medicineName: String,
        dosage: String,
        notes: String? = null,
        sideEffects: String? = null,
        mood: Int? = null
    ) {
        viewModelScope.launch {
            logMedicineIntakeUseCase(
                medicineName = medicineName,
                dosage = dosage,
                status = IntakeStatus.TAKEN,
                notes = notes,
                sideEffects = sideEffects,
                mood = mood
            ).collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        loadTodayLogs()
                        loadAllLogs()
                        loadWeeklyStats()
                    }
                    is Resource.Error -> {
                        _intakeLogState.value = _intakeLogState.value.copy(
                            error = resource.message
                        )
                    }
                    else -> {}
                }
            }
        }
    }

    fun clearError() {
        _reminderListState.value = _reminderListState.value.copy(error = null)
        _reminderFormState.value = _reminderFormState.value.copy(error = null)
        _intakeLogState.value = _intakeLogState.value.copy(error = null)
    }
}
