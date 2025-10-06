package com.example.medassist_android.data.local.converter

import androidx.room.TypeConverter
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import timber.log.Timber

class StringListConverter {
    private val moshi = Moshi.Builder().build()
    private val adapter: JsonAdapter<List<String>> = moshi.adapter(
        Types.newParameterizedType(List::class.java, String::class.java)
    )

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return try {
            adapter.toJson(value)
        } catch (e: Exception) {
            Timber.e(e, "Failed to convert string list to JSON")
            "[]"
        }
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return try {
            adapter.fromJson(value) ?: emptyList()
        } catch (e: Exception) {
            Timber.e(e, "Failed to convert JSON to string list")
            emptyList()
        }
    }
}
