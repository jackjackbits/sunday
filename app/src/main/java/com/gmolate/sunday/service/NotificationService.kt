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
                enableVibration(false)
                setShowBadge(true)
            }

            val solarNoonChannel = NotificationChannel(
                SOLAR_NOON_CHANNEL_ID,
                "Mediodía Solar",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Recordatorios sobre el mediodía solar"
                enableVibration(false)
                setShowBadge(false)
            }

            val systemNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            systemNotificationManager.createNotificationChannels(
                listOf(uvChannel, vitaminDChannel, goalChannel, solarNoonChannel)
            )
        }
    }

    fun showUVAlert(uvIndex: Double) {
        if (!hasNotificationPermission()) return

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, UV_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("¡Alto índice UV!")
            .setContentText("UV ${String.format("%.1f", uvIndex)} - Usa protección solar")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(UV_NOTIFICATION_ID, notification)
    }

    fun showVitaminDUpdate(currentIU: Double, goalIU: Double) {
        if (!hasNotificationPermission()) return

        val percentage = (currentIU / goalIU * 100).toInt().coerceAtMost(100)

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, VITAMIN_D_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Progreso Vitamina D")
            .setContentText("$percentage% del objetivo diario (${String.format("%.0f", currentIU)} IU)")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setProgress(100, percentage, false)
            .build()

        notificationManager.notify(VITAMIN_D_NOTIFICATION_ID, notification)
    }

    fun showGoalAchieved(goalType: String) {
        if (!hasNotificationPermission()) return

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, GOAL_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.star_on)
            .setContentTitle("¡Objetivo alcanzado!")
            .setContentText("Has completado tu objetivo de $goalType")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(GOAL_NOTIFICATION_ID, notification)
    }

    fun showSolarNoonReminder(minutesUntilNoon: Int) {
        if (!hasNotificationPermission()) return

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, SOLAR_NOON_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Mediodía Solar")
            .setContentText("Mejor momento para vitamina D en $minutesUntilNoon minutos")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(SOLAR_NOON_NOTIFICATION_ID, notification)
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    companion object {
        const val UV_CHANNEL_ID = "uv_alerts"
        const val VITAMIN_D_CHANNEL_ID = "vitamin_d_updates"
        const val GOAL_CHANNEL_ID = "goal_achievements"
        const val SOLAR_NOON_CHANNEL_ID = "solar_noon"

        const val UV_NOTIFICATION_ID = 1001
        const val VITAMIN_D_NOTIFICATION_ID = 1002
        const val GOAL_NOTIFICATION_ID = 1003
        const val SOLAR_NOON_NOTIFICATION_ID = 1004
    }
}
