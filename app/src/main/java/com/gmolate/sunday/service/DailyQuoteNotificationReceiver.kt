package com.gmolate.sunday.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.gmolate.sunday.Quote

class DailyQuoteNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let {
            if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
                // Reprogramar notificaciones de cita diaria si el dispositivo se reinicia
                val preferencesManager = com.gmolate.sunday.PreferencesManager(it) // Usar el nuevo PM
                if (preferencesManager.areNotificationsEnabled()) {
                    val hour = preferencesManager.getNotificationHour()
                    val minute = preferencesManager.getNotificationMinute()
                    val dummyQuote = Quote("¡Inspírate!", "Goose") // Usar una dummy o cargar la real
                    DailyQuoteNotificationService(it).scheduleDailyQuoteNotification(hour, minute, dummyQuote)
                }
                return
            } else if (intent?.action == "${it.packageName}.ACTION_DAILY_QUOTE_NOTIFICATION") {
                val quoteText = intent.getStringExtra("quote_text") ?: "La inspiración te espera."
                val quoteAuthor = intent.getStringExtra("quote_author") ?: "Anónimo"
                val quote = Quote(quoteText, quoteAuthor)
                DailyQuoteNotificationService(it).showNotification(quote)
            }
        }
    }
}
