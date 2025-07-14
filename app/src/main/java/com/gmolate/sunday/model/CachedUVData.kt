package com.gmolate.sunday.model

import androidx.room.Entity
import androidx.room.PrimaryKey
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
    @androidx.room.TypeConverter
    fun fromString(value: String): List<Double> {
        val listType = object : TypeToken<List<Double>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @androidx.room.TypeConverter
    fun fromList(list: List<Double>): String {
        val gson = Gson()
        return gson.toJson(list)
    }

    @androidx.room.TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @androidx.room.TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}
