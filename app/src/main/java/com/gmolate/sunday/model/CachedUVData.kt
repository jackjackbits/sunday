package com.gmolate.sunday.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

@Entity(tableName = "cached_uv_data")
@TypeConverters(Converters::class)
data class CachedUVData(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val latitude: Double,
    val longitude: Double,
    val date: Date,
    val hourlyUV: List<Double>,
    val hourlyCloudCover: List<Double>,
    val maxUV: Double,
    val sunrise: Date,
    val sunset: Date,
    val lastUpdated: Date
)

class Converters {
    @TypeConverter
    fun fromDoubleList(value: List<Double>): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toDoubleList(value: String): List<Double> {
        val listType = object : TypeToken<List<Double>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromDate(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun toDate(timestamp: Long?): Date? {
        return timestamp?.let { Date(it) }
    }
}
