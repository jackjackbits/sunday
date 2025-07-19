package com.gmolate.sunday

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let {
            if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
                // El dispositivo se ha reiniciado, reprogramar las notificaciones
                // Aquí deberías cargar las preferencias del usuario para saber a qué hora programar
                // Por ahora, asumimos una hora por defecto para demostración
                val notificationService = NotificationService(it)
                // TODO: Cargar hora de notificación desde PreferencesManager
                // Para la demostración, programamos una notificación a una hora fija (ej: 9 AM)
                val dummyQuote = Quote("¡La vida es bella!", "Goose") // O cargar una cita real
                notificationService.scheduleDailyNotification(9, 0, dummyQuote)
                return
            } else if (intent?.action == "${it.packageName}.ACTION_SHOW_QUOTE_NOTIFICATION") {
                val quoteText = intent.getStringExtra("quote_text") ?: "La inspiración te espera."
                val quoteAuthor = intent.getStringExtra("quote_author") ?: "Anónimo"
                val quote = Quote(quoteText, quoteAuthor)
                NotificationService(it).showNotification(quote)
            }
        }
    }
}
