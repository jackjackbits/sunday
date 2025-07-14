package com.gmolate.sunday.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "vitamin_d_session")
data class VitaminDSession(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val startTime: Date,
    var endTime: Date?,
    var totalIU: Double,
    var averageUV: Double,
    var peakUV: Double,
    val clothingLevel: Int,
    val skinType: Int
)
