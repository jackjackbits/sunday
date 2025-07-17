package com.gmolate.sunday.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gmolate.sunday.service.ClothingLevel
import com.gmolate.sunday.service.SkinType
import java.util.Date

@Entity(tableName = "user_preferences")
data class UserPreferences(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val clothingLevel: ClothingLevel = ClothingLevel.LIGHT,
    val skinType: SkinType = SkinType.TYPE_3,
    val userAge: Int = 30,
    val dailyVitaminDGoal: Int = 2000, // UI por día (valor recomendado típico)
    val enableNotifications: Boolean = true,
    val enableSolarNoonNotifications: Boolean = true, // Nueva preferencia
    val solarNoonNotificationMinutesBefore: Int = 30, // Minutos antes del mediodía solar
    val enableHighUVAlerts: Boolean = true,
    val darkMode: Boolean = false,
    val useMetricSystem: Boolean = true,
    val lastLocation: String? = null,
    val lastLocationLat: Double? = null,
    val lastLocationLon: Double? = null,
    val notificationThresholdUV: Double = 6.0, // Umbral UV para notificaciones
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)
