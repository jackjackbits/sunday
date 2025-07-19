package com.gmolate.sunday.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class SolarNoonNotificationService(private val context: Context) {
    
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val solarCalculator = SolarCalculator()
    private val notificationService = NotificationService(context)
    
    companion object {
        private const val SOLAR_NOON_REQUEST_CODE = 2001
        const val ACTION_SOLAR_NOON_NOTIFICATION = "com.gmolate.sunday.SOLAR_NOON_NOTIFICATION"
    }
    
    /**
     * Programa la notificación de mediodía solar para mañana
     */
    fun scheduleSolarNoonNotification(location: Location) {
        // Calcular mediodía solar para mañana
        val tomorrow = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
        }.time
        
        val optimalTime = solarCalculator.calculateOptimalNotificationTime(location, tomorrow)
        
        // Solo programar si es en el futuro
        if (optimalTime.time > System.currentTimeMillis()) {
            val intent = Intent(context, SolarNoonNotificationReceiver::class.java).apply {
                action = ACTION_SOLAR_NOON_NOTIFICATION
                putExtra("latitude", location.latitude)
                putExtra("longitude", location.longitude)
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                SOLAR_NOON_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                optimalTime.time,
                pendingIntent
            )
        }
    }
    
    /**
     * Cancela la notificación de mediodía solar programada
     */
    fun cancelSolarNoonNotification() {
        val intent = Intent(context, SolarNoonNotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            SOLAR_NOON_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager.cancel(pendingIntent)
    }
    
    /**
     * Verifica si las notificaciones de mediodía solar están habilitadas
     */
    fun isSolarNoonNotificationEnabled(): Boolean {
        val prefs = context.getSharedPreferences("sunday_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("solar_noon_notifications", true)
    }
    
    /**
     * Habilita o deshabilita las notificaciones de mediodía solar
     */
    fun setSolarNoonNotificationEnabled(enabled: Boolean) {
        val prefs = context.getSharedPreferences("sunday_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("solar_noon_notifications", enabled).apply()
        
        if (!enabled) {
            cancelSolarNoonNotification()
        }
    }
}

/**
 * Receptor de notificaciones de mediodía solar
 */
class SolarNoonNotificationReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == SolarNoonNotificationService.ACTION_SOLAR_NOON_NOTIFICATION) {
            val latitude = intent.getDoubleExtra("latitude", 0.0)
            val longitude = intent.getDoubleExtra("longitude", 0.0)
            
            if (latitude != 0.0 && longitude != 0.0) {
                val location = Location("").apply {
                    this.latitude = latitude
                    this.longitude = longitude
                }
                
                CoroutineScope(Dispatchers.IO).launch {
                    val notificationService = NotificationService(context)
                    val solarCalculator = SolarCalculator()
                    
                    // Verificar si realmente es un buen momento
                    if (solarCalculator.isOptimalSunExposureTime(location)) {
                        val solarNoon = solarCalculator.calculateSolarNoon(location)
                        val minutesUntilNoon = ((solarNoon.time - System.currentTimeMillis()) / (1000 * 60)).toInt()
                        notificationService.showSolarNoonReminder(minutesUntilNoon.coerceAtLeast(0))
                    }
                    
                    // Reprogramar para mañana
                    val solarNoonService = SolarNoonNotificationService(context)
                    if (solarNoonService.isSolarNoonNotificationEnabled()) {
                        solarNoonService.scheduleSolarNoonNotification(location)
                    }
                }
            }
        }
    }
}
