package com.gmolate.sunday.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.gmolate.sunday.R
import com.gmolate.sunday.model.AppDatabase
import com.gmolate.sunday.ui.MainActivity
import com.gmolate.sunday.model.CachedMoonData
import com.gmolate.sunday.model.CachedUVData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SundayWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Usar un scope más eficiente para el widget
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val cachedUVData = db.cachedUVDataDao().getLatestUvData()
                val cachedMoonData = db.cachedMoonDataDao().getLatestMoonData()

                appWidgetIds.forEach { appWidgetId ->
                    val uvIndex = getCurrentUVFromData(cachedUVData)
                    updateAppWidget(context, appWidgetManager, appWidgetId, uvIndex, cachedMoonData, cachedUVData)
                }
            } catch (e: Exception) {
                // En caso de error, mostrar datos por defecto
                appWidgetIds.forEach { appWidgetId ->
                    updateAppWidgetWithDefaults(context, appWidgetManager, appWidgetId)
                }
            }
        }
    }

    private fun updateAppWidgetWithDefaults(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val views = RemoteViews(context.packageName, R.layout.widget_sunday).apply {
            setTextViewText(R.id.current_uv, "--")
            setTextViewText(R.id.widget_label, "No Data")
            setTextViewText(R.id.sunrise_time, "🌅 --:--")
            setTextViewText(R.id.sunset_time, "🌇 --:--")
            setOnClickPendingIntent(R.id.widget_container, pendingIntent)
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        uvIndex: Double,
        moonData: CachedMoonData?,
        uvData: CachedUVData?
    ) {
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        val views = RemoteViews(context.packageName, R.layout.widget_sunday).apply {
            if (uvIndex > 0) {
                setTextViewText(R.id.current_uv, String.format("%.1f", uvIndex))
                setTextViewText(R.id.widget_label, "UV INDEX")
            } else {
                moonData?.let { moon ->
                    setTextViewText(R.id.current_uv, getMoonIcon(moon.moonPhase))
                    setTextViewText(R.id.widget_label, getMoonPhaseName(moon.moonPhase))
                } ?: run {
                    setTextViewText(R.id.current_uv, "🌕")
                    setTextViewText(R.id.widget_label, "Full Moon")
                }
            }
            
            // Mostrar sunrise/sunset si están disponibles
            uvData?.let { data ->
                setTextViewText(R.id.sunrise_time, "🌅 ${timeFormat.format(data.sunrise)}")
                setTextViewText(R.id.sunset_time, "🌇 ${timeFormat.format(data.sunset)}")
            } ?: run {
                setTextViewText(R.id.sunrise_time, "🌅 --:--")
                setTextViewText(R.id.sunset_time, "🌇 --:--")
            }
            
            setOnClickPendingIntent(R.id.widget_container, pendingIntent)
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun getMoonPhaseName(phase: Double): String {
        return when {
            phase < 0.125 -> "Luna Nueva"
            phase < 0.25 -> "Cuarto Creciente"
            phase < 0.375 -> "Creciente Gibosa"
            phase < 0.5 -> "Luna Llena"
            phase < 0.625 -> "Menguante Gibosa"
            phase < 0.75 -> "Cuarto Menguante"
            phase < 0.875 -> "Menguante"
            else -> "Luna Nueva"
        }
    }

    private fun getMoonIcon(phase: Double): String {
        return when {
            phase < 0.125 -> "🌑"
            phase < 0.25 -> "🌒"
            phase < 0.375 -> "🌓"
            phase < 0.5 -> "🌔"
            phase < 0.625 -> "🌕"
            phase < 0.75 -> "🌖"
            phase < 0.875 -> "🌗"
            else -> "🌘"
        }
    }

    private fun getCurrentUVFromData(uvData: CachedUVData?): Double {
        if (uvData == null || uvData.hourlyUV.isEmpty()) return 0.0
        
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return if (currentHour < uvData.hourlyUV.size) {
            uvData.hourlyUV[currentHour]
        } else {
            0.0
        }
    }

    override fun onEnabled(context: Context) {
        // Primera vez que se añade un widget
    }

    override fun onDisabled(context: Context) {
        // Último widget eliminado
    }
}