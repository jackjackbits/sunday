package com.gmolate.sunday

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.gmolate.sunday.model.AppDatabase
import com.gmolate.sunday.service.NetworkMonitor

class SundayApplication : Application() {
    lateinit var networkMonitor: NetworkMonitor
        private set

    // Lazy initialization de la base de datos
    val database by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        networkMonitor = NetworkMonitor(this)

        // Crear canales de notificación
        createNotificationChannels()

        // Inicializar servicios si es necesario
        initializeServices()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = ContextCompat.getSystemService(
                this,
                NotificationManager::class.java
            ) as NotificationManager

            // Canal para notificaciones UV
            val uvChannel = NotificationChannel(
                "uv_alerts",
                "Alertas UV",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones sobre niveles altos de radiación UV"
                enableVibration(true)
                enableLights(true)
            }

            // Canal para mediodía solar
            val solarNoonChannel = NotificationChannel(
                "solar_noon",
                "Mediodía Solar",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones sobre el mediodía solar"
                enableVibration(false)
                enableLights(false)
            }

            // Canal para vitamina D
            val vitaminDChannel = NotificationChannel(
                "vitamin_d",
                "Vitamina D",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Recordatorios y progreso de vitamina D"
                enableVibration(false)
                enableLights(false)
            }

            // Crear los canales
            notificationManager.createNotificationChannels(
                listOf(uvChannel, solarNoonChannel, vitaminDChannel)
            )
        }
    }

    private fun initializeServices() {
        // Aquí se pueden inicializar servicios que necesiten estar disponibles
        // desde el inicio de la aplicación
    }

    companion object {
        private lateinit var instance: SundayApplication

        fun getInstance(): SundayApplication {
            return instance
        }

        const val UV_CHANNEL_ID = "uv_alerts"
        const val SOLAR_NOON_CHANNEL_ID = "solar_noon"
        const val VITAMIN_D_CHANNEL_ID = "vitamin_d"
    }
}
