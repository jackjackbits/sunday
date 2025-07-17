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
                _isLoading.value = false
                return
            }

            val timestamp = System.currentTimeMillis() / 1000
            val response = api.getMoonPhase(timestamp)
            
            val moonData = CachedMoonData(
                date = today,
                phaseName = response.Phase,
                phaseIcon = getMoonIconForPhase(response.Phase),
                age = response.Age,
                fraction = response.Fraction,
                lastUpdated = Date()
            )
            
            db.cachedMoonDataDao().insertMoonData(moonData)
            updateMoonData(moonData)
            
            // Limpiar datos antiguos de forma asíncrona para mejor rendimiento
            cleanupOldDataAsync()
            
        } catch (e: HttpException) {
            handleError()
        } catch (e: Exception) {
            handleError()
        } finally {
            _isLoading.value = false
        }
    }

    private suspend fun cleanupOldDataAsync() {
        try {
            val cutoffDate = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -30)
            }
            db.cachedMoonDataDao().deleteOldMoonData(dateFormat.format(cutoffDate.time))
        } catch (e: Exception) {
            // Silenciosamente ignorar errores de limpieza
        }
    }

    private fun updateMoonData(moonData: CachedMoonData) {
        _currentMoonPhase.value = moonData.phaseName
        _currentMoonIcon.value = moonData.phaseIcon
        _moonAge.value = moonData.age
        _moonFraction.value = moonData.fraction
    }

    private suspend fun handleError() {
        // Intentar usar datos cacheados aunque sean antiguos
        val cachedData = db.cachedMoonDataDao().getLatestMoonData()
        if (cachedData != null) {
            updateMoonData(cachedData)
        } else {
            // Usar valores por defecto
            _currentMoonPhase.value = "Waxing Gibbous"
            _currentMoonIcon.value = "🌔"
            _moonAge.value = 10.0
            _moonFraction.value = 0.75
        }
    }

    private fun isDataStillValid(lastUpdated: Date): Boolean {
        val now = Date()
        val diffInHours = (now.time - lastUpdated.time) / (1000 * 60 * 60)
        return diffInHours < 12 // Los datos de luna son válidos por 12 horas
    }

    private fun getMoonIconForPhase(phase: String): String {
        return when (phase.lowercase()) {
            "new moon", "new" -> "🌑"
            "waxing crescent" -> "🌒"
            "first quarter" -> "🌓"
            "waxing gibbous" -> "🌔"
            "full moon", "full" -> "🌕"
            "waning gibbous" -> "🌖"
            "last quarter", "third quarter" -> "🌗"
            "waning crescent" -> "🌘"
            else -> "🌕" // Por defecto luna llena
        }
    }

    fun getMoonIconResourceForWidget(phase: String): String {
        // Para el widget de Android usaremos nombres de recursos
        return when (phase.lowercase()) {
            "new moon", "new" -> "ic_moon_new"
            "waxing crescent" -> "ic_moon_waxing_crescent"
            "first quarter" -> "ic_moon_first_quarter"
            "waxing gibbous" -> "ic_moon_waxing_gibbous"
            "full moon", "full" -> "ic_moon_full"
            "waning gibbous" -> "ic_moon_waning_gibbous"
            "last quarter", "third quarter" -> "ic_moon_last_quarter"
            "waning crescent" -> "ic_moon_waning_crescent"
            else -> "ic_moon_full"
        }
    }
}
