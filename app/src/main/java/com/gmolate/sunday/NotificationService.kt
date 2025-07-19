package com.gmolate.sunday

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import java.util.*

class NotificationService(private val context: Context) {

    private val CHANNEL_ID = "sunday_notification_channel"
    private val NOTIFICATION_ID = 1

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Sunday Notifications"
            val descriptionText = "Notifications for daily quotes"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleDailyNotification(hour: Int, minute: Int, quote: Quote) {
        // Verificar permiso de notificaciones para Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Si no tiene el permiso, no se pueden programar notificaciones.
                // En un escenario real, se debería solicitar el permiso al usuario.
                return
            }
        }

        // Aquí deberíamos verificar el permiso SCHEDULE_EXACT_ALARM para Android 12+
        // Si el permiso no está concedido y es requerido, las alarmas no serán exactas.
        // Para Android 12+, las apps necesitan el permiso REQUEST_SCHEDULE_EXACT_ALARM
        // para usar AlarmManager.setExactAndAllowWhileIdle() o setExact().
        // Este permiso se solicita en el AndroidManifest.xml y en tiempo de ejecución.
        // Por ahora, asumimos que se tiene o que las inexactas son suficientes (lo que no es el caso para replicar iOS).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                // Aquí deberíamos guiar al usuario para que conceda el permiso
                // o informar que las notificaciones podrían no ser exactas.
                // No se puede solicitar directamente aquí sin una actividad.
                // La solicitud debe hacerse desde una actividad visible para el usuario.
            }
        }


        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = "${context.packageName}.ACTION_SHOW_QUOTE_NOTIFICATION" // Acción específica para nuestra alarma
            putExtra("quote_text", quote.text)
            putExtra("quote_author", quote.author)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0, // Request code
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Configura el calendario para la notificación diaria
        val calendar: Calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // Si la hora ya pasó hoy, programa para mañana
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        // Programa la alarma diaria
        // Usamos RTC_WAKEUP para que la alarma se active incluso si el dispositivo está en modo "doze"
        // Para alarmas exactas y respetando el doze mode, se necesita canScheduleExactAlarms()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    fun cancelNotifications() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = "${context.packageName}.ACTION_SHOW_QUOTE_NOTIFICATION"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel() // Es buena práctica cancelar el PendingIntent también
        }
    }

    fun showNotification(quote: Quote) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Asegúrate de tener un icono
            .setContentTitle("Tu dosis diaria de inspiración")
            .setContentText("${quote.text} - ${quote.author}")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true) // Cierra la notificación al tocarla

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }
}
