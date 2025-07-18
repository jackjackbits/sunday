package com.gmolate.sunday.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import java.util.Date

@Entity(tableName = "cached_moon_data")
@TypeConverters(Converters::class)
data class CachedMoonData(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val latitude: Double,
    val longitude: Double,
    val date: Date,
    val moonPhase: Double, // 0.0 = nueva, 0.5 = llena
    val moonrise: Date?,
    val moonset: Date?,
    val illumination: Double, // Porcentaje de iluminación
    val distanceKm: Double, // Distancia a la Luna en km
    val lastUpdated: Date
)
