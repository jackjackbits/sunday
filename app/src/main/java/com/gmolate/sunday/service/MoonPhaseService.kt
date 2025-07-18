package com.gmolate.sunday.service

import android.content.Context
import com.gmolate.sunday.model.AppDatabase
import com.gmolate.sunday.model.CachedMoonData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MoonPhaseService(
    private val context: Context,
    private val db: AppDatabase
) {
    private val api: FarmsenseApi = Retrofit.Builder()
        .baseUrl("https://api.farmsense.net/v1/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(FarmsenseApi::class.java)

    private val _currentMoonPhase = MutableStateFlow("")
    val currentMoonPhase: StateFlow<String> = _currentMoonPhase.asStateFlow()

    private val _currentMoonIcon = MutableStateFlow("🌕")
    val currentMoonIcon: StateFlow<String> = _currentMoonIcon.asStateFlow()

    private val _moonAge = MutableStateFlow(0.0)
    val moonAge: StateFlow<Double> = _moonAge.asStateFlow()

    private val _moonFraction = MutableStateFlow(0.0)
    val moonFraction: StateFlow<Double> = _moonFraction.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    suspend fun fetchMoonPhase() {
        _isLoading.value = true
        try {
            val today = dateFormat.format(Date())
            val cachedData = db.cachedMoonDataDao().getMoonDataForDate(today)
            
            if (cachedData != null && isDataStillValid(cachedData.lastUpdated)) {
                updateMoonData(cachedData)
                return
            }

            val response = api.getMoonPhase(today)

            // Guardar en caché
            val moonData = CachedMoonData(
                latitude = 0.0, // Global data
                longitude = 0.0,
                date = Date(),
                moonPhase = response.moonPhase,
                moonrise = parseTime(response.moonrise),
                moonset = parseTime(response.moonset),
                illumination = response.illumination,
                distanceKm = response.distanceKm,
                lastUpdated = Date()
            )
            
            db.cachedMoonDataDao().insertMoonData(moonData)
            updateMoonData(moonData)
            
        } catch (e: HttpException) {
            handleError(e)
        } catch (e: Exception) {
            handleError(e)
        } finally {
            _isLoading.value = false
        }
    }

    private fun updateMoonData(moonData: CachedMoonData) {
        _moonAge.value = moonData.moonPhase * 29.53 // Ciclo lunar promedio
        _moonFraction.value = moonData.illumination / 100.0
        _currentMoonPhase.value = getMoonPhaseName(moonData.moonPhase)
        _currentMoonIcon.value = getMoonIcon(moonData.moonPhase)
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

    private fun isDataStillValid(lastUpdated: Date): Boolean {
        val now = Date()
        val diffInHours = (now.time - lastUpdated.time) / (1000 * 60 * 60)
        return diffInHours < 24 // Datos válidos por 24 horas
    }

    private fun parseTime(timeString: String?): Date? {
        if (timeString == null) return null
        return try {
            val format = SimpleDateFormat("HH:mm", Locale.getDefault())
            format.parse(timeString)
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun handleError(e: Exception) {
        // Intentar cargar datos del caché aunque sean antiguos
        val today = dateFormat.format(Date())
        val cachedData = db.cachedMoonDataDao().getMoonDataForDate(today)
        if (cachedData != null) {
            updateMoonData(cachedData)
        } else {
            // Valores por defecto si no hay caché
            _currentMoonPhase.value = "Fase Desconocida"
            _currentMoonIcon.value = "🌙"
            _moonAge.value = 0.0
            _moonFraction.value = 0.5
        }
    }
}
