package com.gmolate.sunday.service

import android.location.Location
import java.util.*
import kotlin.math.*

class SolarCalculator {
    
    /**
     * Calcula el mediodía solar (momento de máximo UV) para una ubicación específica
     * @param location Ubicación del usuario
     * @param date Fecha para la cual calcular
     * @return Hora del mediodía solar en milisegundos
     */
    fun calculateSolarNoon(location: Location, date: Date = Date()): Date {
        val calendar = Calendar.getInstance().apply {
            time = date
        }
        
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val longitude = location.longitude
        
        // Ecuación del tiempo - corrección para la órbita elíptica de la Tierra
        val equationOfTime = calculateEquationOfTime(dayOfYear)
        
        // Corrección por longitud
        val longitudeCorrection = (longitude - getTimeZoneLongitude(calendar)) * 4 // 4 minutos por grado
        
        // Mediodía solar = 12:00 + ecuación del tiempo + corrección de longitud
        val solarNoonMinutes = 12 * 60 + equationOfTime + longitudeCorrection
        
        // Convertir a Date
        val solarNoonCalendar = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MINUTE, solarNoonMinutes.toInt())
        }
        
        return solarNoonCalendar.time
    }
    
    /**
     * Calcula el momento óptimo para notificar (30 minutos antes del mediodía solar)
     */
    fun calculateOptimalNotificationTime(location: Location, date: Date = Date()): Date {
        val solarNoon = calculateSolarNoon(location, date)
        val calendar = Calendar.getInstance().apply {
            time = solarNoon
            add(Calendar.MINUTE, -30) // 30 minutos antes
        }
        return calendar.time
    }
    
    /**
     * Verifica si ahora es un buen momento para la exposición solar
     * (dentro de 2 horas del mediodía solar)
     */
    fun isOptimalSunExposureTime(location: Location, currentTime: Date = Date()): Boolean {
        val solarNoon = calculateSolarNoon(location, currentTime)
        val diffInMinutes = abs(currentTime.time - solarNoon.time) / (1000 * 60)
        return diffInMinutes <= 120 // Dentro de 2 horas
    }
    
    /**
     * Calcula la ecuación del tiempo en minutos
     */
    private fun calculateEquationOfTime(dayOfYear: Int): Double {
        val b = 2 * PI * (dayOfYear - 81) / 365.0
        return 9.87 * sin(2 * b) - 7.53 * cos(b) - 1.5 * sin(b)
    }
    
    /**
     * Obtiene la longitud central de la zona horaria
     */
    private fun getTimeZoneLongitude(calendar: Calendar): Double {
        val timeZone = calendar.timeZone
        val offsetHours = timeZone.getOffset(calendar.timeInMillis) / (1000 * 60 * 60)
        return offsetHours * 15.0 // 15 grados por hora
    }
    
    /**
     * Calcula la elevación solar en grados
     */
    fun calculateSolarElevation(location: Location, time: Date = Date()): Double {
        val calendar = Calendar.getInstance().apply { time = time }
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val hour = calendar.get(Calendar.HOUR_OF_DAY) + calendar.get(Calendar.MINUTE) / 60.0
        
        val latitude = Math.toRadians(location.latitude)
        val declination = Math.toRadians(23.45 * sin(Math.toRadians(360.0 * (284 + dayOfYear) / 365.0)))
        val hourAngle = Math.toRadians(15.0 * (hour - 12.0))
        
        val elevation = asin(
            sin(latitude) * sin(declination) + 
            cos(latitude) * cos(declination) * cos(hourAngle)
        )
        
        return Math.toDegrees(elevation)
    }
}
