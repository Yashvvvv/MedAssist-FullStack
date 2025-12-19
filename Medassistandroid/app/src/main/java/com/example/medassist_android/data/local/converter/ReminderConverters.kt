package com.example.medassist_android.data.local.converter

import androidx.room.TypeConverter
import com.example.medassist_android.data.local.entity.IntakeStatus
import com.example.medassist_android.data.local.entity.ReminderFrequency

class ReminderConverters {
    
    @TypeConverter
    fun fromReminderFrequency(frequency: ReminderFrequency): String {
        return frequency.name
    }
    
    @TypeConverter
    fun toReminderFrequency(value: String): ReminderFrequency {
        return ReminderFrequency.valueOf(value)
    }
    
    @TypeConverter
    fun fromIntakeStatus(status: IntakeStatus): String {
        return status.name
    }
    
    @TypeConverter
    fun toIntakeStatus(value: String): IntakeStatus {
        return IntakeStatus.valueOf(value)
    }
}
