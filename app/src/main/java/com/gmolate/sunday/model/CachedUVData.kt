package com.gmolate.sunday.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
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
