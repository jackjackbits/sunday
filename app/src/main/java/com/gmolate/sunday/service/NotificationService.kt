package com.gmolate.sunday.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.gmolate.sunday.R
import com.gmolate.sunday.ui.MainActivity

class NotificationService(private val context: Context) {
    private val notificationManager = NotificationManagerCompat.from(context)

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val uvChannel = NotificationChannel(
                UV_CHANNEL_ID,
                "Alertas de índice UV",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertas sobre niveles altos de índice UV"
                enableVibration(true)
                setShowBadge(true)
            }

            val vitaminDChannel = NotificationChannel(
                VITAMIN_D_CHANNEL_ID,
                "Actualizaciones de Vitamina D",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Actualizaciones sobre acumulación de vitamina D"
                enableVibration(true)
                setShowBadge(true)
            }

            val goalChannel = NotificationChannel(
                GOAL_CHANNEL_ID,
                "Objetivos Alcanzados",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones de objetivos diarios alcanzados"
                enableVibration(true)
                setShowBadge(true)
            }

            val solarNoonChannel = NotificationChannel(
                SOLAR_NOON_CHANNEL_ID,
                "Mediodía Solar",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones del momento óptimo para exposición solar"
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(uvChannel)
            notificationManager.createNotificationChannel(vitaminDChannel)
            notificationManager.createNotificationChannel(goalChannel)
            notificationManager.createNotificationChannel(solarNoonChannel)
        }
    }

    fun showUVAlert(uvIndex: Double) {
        if (!hasNotificationPermission()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, UV_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("¡Índice UV Alto!")
            .setContentText("El índice UV actual es ${String.format("%.1f", uvIndex)}. Toma precauciones.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(UV_NOTIFICATION_ID, notification)
    }

    fun showVitaminDGoalReached(amount: Double) {
        if (!hasNotificationPermission()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 1, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, GOAL_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("¡Objetivo Alcanzado!")
            .setContentText("Has alcanzado tu objetivo diario de vitamina D (${amount.toInt()} UI)")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(GOAL_NOTIFICATION_ID, notification)
    }

    fun showExposureWarning(minutes: Int) {
        if (!hasNotificationPermission()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 2, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, VITAMIN_D_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("¡Tiempo de Exposición!")
            .setContentText("Has estado expuesto al sol durante $minutes minutos")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(EXPOSURE_NOTIFICATION_ID, notification)
    }

    fun showSolarNoonNotification() {
        if (!hasNotificationPermission()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 3, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, SOLAR_NOON_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("☀️ Momento Óptimo para Vitamina D")
            .setContentText("¡Perfecto momento para exposición solar! El UV está en su punto máximo.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(SOLAR_NOON_NOTIFICATION_ID, notification)
    }

    private fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    companion object {
        private const val UV_CHANNEL_ID = "uv_alerts"
        private const val VITAMIN_D_CHANNEL_ID = "vitamin_d_updates"
        private const val GOAL_CHANNEL_ID = "goal_reached"
        private const val SOLAR_NOON_CHANNEL_ID = "solar_noon"
        private const val UV_NOTIFICATION_ID = 1001
        private const val VITAMIN_D_NOTIFICATION_ID = 1002
        private const val GOAL_NOTIFICATION_ID = 1003
        private const val EXPOSURE_NOTIFICATION_ID = 1004
        private const val SOLAR_NOON_NOTIFICATION_ID = 1005
    }
}
