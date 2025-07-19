package com.gmolate.sunday.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import java.util.Date

@Entity(tableName = "vitamin_d_session")
@TypeConverters(Converters::class)
data class VitaminDSession(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val startTime: Date,
    val endTime: Date?,
    val durationMinutes: Int,
    val uvIndex: Double,
    val estimatedVitaminD: Double, // IU estimadas generadas
    val skinType: Int,
    val latitude: Double,
    val longitude: Double,
    val isCompleted: Boolean = false,
    val notes: String? = null,
    val createdAt: Date = Date()
)
