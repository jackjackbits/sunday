package com.gmolate.sunday.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "cached_moon_data")
data class CachedMoonData(
    @PrimaryKey
    val date: String, // formato YYYY-MM-DD
    val phaseName: String,
    val phaseIcon: String,
    val age: Double,
    val fraction: Double,
    val lastUpdated: Date = Date()
)
