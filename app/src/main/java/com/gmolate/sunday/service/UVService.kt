package com.gmolate.sunday.service

import android.location.Location
import com.gmolate.sunday.model.AppDatabase
import com.gmolate.sunday.model.CachedUVData
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
import java.util.TimeZone

class UVService(
    private val db: AppDatabase,
    private val notificationService: NotificationService
) {
    private val api: OpenMeteoApi = Retrofit.Builder()
        .baseUrl("https://api.open-meteo.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OpenMeteoApi::class.java)

    private val _currentUV = MutableStateFlow(0.0)
    val currentUV: StateFlow<Double> = _currentUV.asStateFlow()

    private val _maxUV = MutableStateFlow(0.0)
    val maxUV: StateFlow<Double> = _maxUV.asStateFlow()

    private val _tomorrowMaxUV = MutableStateFlow(0.0)
    val tomorrowMaxUV: StateFlow<Double> = _tomorrowMaxUV.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _burnTimeMinutes = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val burnTimeMinutes: StateFlow<Map<Int, Int>> = _burnTimeMinutes.asStateFlow()

    private val _todaySunrise = MutableStateFlow<Date?>(null)
    val todaySunrise: StateFlow<Date?> = _todaySunrise.asStateFlow()

    private val _todaySunset = MutableStateFlow<Date?>(null)
    val todaySunset: StateFlow<Date?> = _todaySunset.asStateFlow()

    private val _tomorrowSunrise = MutableStateFlow<Date?>(null)
    val tomorrowSunrise: StateFlow<Date?> = _tomorrowSunrise.asStateFlow()

    private val _tomorrowSunset = MutableStateFlow<Date?>(null)
    val tomorrowSunset: StateFlow<Date?> = _tomorrowSunset.asStateFlow()

    private val _isOfflineMode = MutableStateFlow(false)
    val isOfflineMode: StateFlow<Boolean> = _isOfflineMode.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val HIGH_UV_THRESHOLD = 6.0

    suspend fun fetchUVData(location: Location) {
        _isLoading.value = true
        try {
            val cachedData = getCachedDataForLocation(location)
            if (cachedData != null && isDataStillValid(cachedData.lastUpdated)) {
                processUvData(cachedData)
                _isOfflineMode.value = true
                return
            }

            val response = api.getUvData(
                latitude = location.latitude,
                longitude = location.longitude,
                elevation = location.altitude
            )

            processUvData(response, location)
            _isOfflineMode.value = false
            _error.value = null

            // Notificar si el UV es alto (solo si no estamos en modo offline)
            if (response.daily.uv_index_max.firstOrNull() ?: 0.0 >= HIGH_UV_THRESHOLD) {
                try {
                    notificationService.showUVAlert(response.daily.uv_index_max.first())
                } catch (notificationError: Exception) {
                    // Error en notificación no debe afectar la funcionalidad principal
                }
            }
        } catch (e: HttpException) {
            handleError(e, location)
        } catch (e: Exception) {
            handleError(e, location)
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun updateUVData(location: Location) {
        fetchUVData(location)
    }

    private suspend fun handleError(e: Exception, location: Location) {
        val cachedData = getCachedDataForLocation(location)
        if (cachedData != null) {
            processUvData(cachedData)
            _isOfflineMode.value = true
            _error.value = "Usando datos guardados - Sin conexión"
        } else {
            _isOfflineMode.value = true
            _error.value = "Error al obtener datos UV: ${e.message}"
        }
    }

    private suspend fun processUvData(response: OpenMeteoResponse, location: Location) {
        // Guardar en caché
        val cachedData = CachedUVData(
            latitude = location.latitude,
            longitude = location.longitude,
            date = Date(),
            hourlyUV = response.hourly.uv_index,
            hourlyCloudCover = response.hourly.cloud_cover ?: emptyList(),
            maxUV = response.daily.uv_index_max.firstOrNull() ?: 0.0,
            sunrise = parseTime(response.daily.sunrise.firstOrNull()),
            sunset = parseTime(response.daily.sunset.firstOrNull()),
            lastUpdated = Date()
        )

        db.cachedUVDataDao().insertUvData(cachedData)
        processUvData(cachedData)
    }

    private fun processUvData(cachedData: CachedUVData) {
        _maxUV.value = cachedData.maxUV
        _todaySunrise.value = cachedData.sunrise
        _todaySunset.value = cachedData.sunset

        // Calcular UV actual basado en la hora
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        _currentUV.value = if (currentHour < cachedData.hourlyUV.size) {
            cachedData.hourlyUV[currentHour]
        } else {
            0.0
        }

        // Calcular tiempos de quemadura para diferentes tipos de piel
        calculateBurnTimes()
    }

    private suspend fun getCachedDataForLocation(location: Location): CachedUVData? {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        return db.cachedUVDataDao().getUvDataForDateAndLocation(
            today, location.latitude, location.longitude
        )
    }

    private fun isDataStillValid(lastUpdated: Date): Boolean {
        val now = Date()
        val diffInHours = (now.time - lastUpdated.time) / (1000 * 60 * 60)
        return diffInHours < 6 // Datos válidos por 6 horas
    }

    private fun calculateBurnTimes() {
        val currentUV = _currentUV.value
        if (currentUV <= 0) {
            _burnTimeMinutes.value = emptyMap()
            return
        }

        val burnTimes = mutableMapOf<Int, Int>()

        // Calcular para cada tipo de piel (1-6)
        for (skinType in 1..6) {
            val skinTypeEnum = SkinType.fromInt(skinType)
            val burnTime = skinTypeEnum.getSafeExposureMinutes(currentUV)
            burnTimes[skinType] = burnTime
        }

        _burnTimeMinutes.value = burnTimes
    }

    private fun parseTime(timeString: String?): Date? {
        if (timeString == null) return null
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
            format.timeZone = TimeZone.getTimeZone("UTC")
            format.parse(timeString)
        } catch (e: Exception) {
            null
        }
    }
}

// Data classes para la API de OpenMeteo
data class OpenMeteoResponse(
    val hourly: HourlyData,
    val daily: DailyData
)

data class HourlyData(
    val uv_index: List<Double>,
    val cloud_cover: List<Double>?
)

data class DailyData(
    val uv_index_max: List<Double>,
    val sunrise: List<String>,
    val sunset: List<String>
)
