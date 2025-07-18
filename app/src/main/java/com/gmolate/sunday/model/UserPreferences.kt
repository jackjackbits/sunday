package com.gmolate.sunday.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import java.util.Date

@Entity(tableName = "user_preferences")
@TypeConverters(Converters::class)
data class UserPreferences(
    @PrimaryKey
    val id: Int = 1, // Solo habrá una fila de preferencias
    val skinType: Int = 2, // Tipo de piel (1-6, por defecto tipo 2)
    val userAge: Int = 30,
    val dailyVitaminDGoal: Double = 1000.0, // Meta diaria de vitamina D en IU
    val allowNotifications: Boolean = true,
    val enableSolarNoonNotifications: Boolean = true, // Nueva preferencia
    val solarNoonNotificationMinutesBefore: Int = 30, // Minutos antes del mediodía solar
    val enableHighUVAlerts: Boolean = true,
    val darkMode: Boolean = false,
    val preferredUnits: String = "metric", // "metric" o "imperial"
    val safeExposureTime: Int = 15, // Tiempo seguro de exposición en minutos
    val dailySunExposure: Int = 0, // Tiempo de exposición al sol hoy en minutos
    val weeklyGoal: Int = 150, // Meta semanal en minutos
    val hasOnboardingCompleted: Boolean = false,
    val lastLocation: String? = null,
    val lastLocationLat: Double? = null,
    val lastLocationLon: Double? = null,
    val notificationThresholdUV: Double = 6.0, // Umbral UV para notificaciones
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)
