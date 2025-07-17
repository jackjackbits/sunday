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

    private suspend fun handleError(e: Exception, location: Location) {
        val cachedData = getCachedDataForLocation(location)
        if (cachedData != null) {
            processUvData(cachedData)
            _isOfflineMode.value = true
            _error.value = "Using cached data: ${e.message}"
        } else {
            _error.value = "Error fetching UV data: ${e.message}"
        }
    }

    private suspend fun getCachedDataForLocation(location: Location): CachedUVData? {
        val tolerance = 0.01 // aproximadamente 1km
        return db.cachedUVDataDao().getCachedData(
            minLat = location.latitude - tolerance,
            maxLat = location.latitude + tolerance,
            minLon = location.longitude - tolerance,
            maxLon = location.longitude + tolerance,
            startDate = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }.time,
            endDate = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
            }.time
        ).firstOrNull()
    }

    private fun isDataStillValid(lastUpdated: Date): Boolean {
        val currentTime = System.currentTimeMillis()
        val timeDifference = currentTime - lastUpdated.time
        return timeDifference < 30 * 60 * 1000 // 30 minutos
    }

    private fun calculateBurnTimes(uvIndex: Double) {
        val burnTimes = mutableMapOf<Int, Int>()
        for (skinType in 1..6) {
            val baseTime = when (skinType) {
                1 -> 150
                2 -> 250
                3 -> 425
                4 -> 600
                5 -> 850
                6 -> 1100
                else -> 425
            }
            val adjustedTime = if (uvIndex > 0) {
                (baseTime / uvIndex).toInt()
            } else {
                Int.MAX_VALUE
            }
            burnTimes[skinType] = adjustedTime
        }
        _burnTimeMinutes.value = burnTimes
    }

    private suspend fun processUvData(response: OpenMeteoResponse, location: Location) {
        val altitudeMultiplier = 1.0 + (location.altitude / 1000.0 * 0.1)

        // Datos de hoy
        response.daily.uv_index_max.firstOrNull()?.let { todayMaxUv ->
            _maxUV.value = todayMaxUv * altitudeMultiplier
        }

        // Datos de mañana
        response.daily.uv_index_max.getOrNull(1)?.let { tomorrowMax ->
            _tomorrowMaxUV.value = tomorrowMax * altitudeMultiplier
        }

        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())

        // Amanecer/atardecer de hoy
        _todaySunrise.value = response.daily.sunrise.firstOrNull()?.let { formatter.parse(it) }
        _todaySunset.value = response.daily.sunset.firstOrNull()?.let { formatter.parse(it) }

        // Amanecer/atardecer de mañana
        _tomorrowSunrise.value = response.daily.sunrise.getOrNull(1)?.let { formatter.parse(it) }
        _tomorrowSunset.value = response.daily.sunset.getOrNull(1)?.let { formatter.parse(it) }

        // UV actual
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val currentHourUV = response.hourly?.uv_index?.get(hour) ?: 0.0
        val nextHourUV = response.hourly?.uv_index?.getOrNull(hour + 1) ?: currentHourUV
        val interpolationFactor = minute / 60.0

        _currentUV.value = (currentHourUV + (nextHourUV - currentHourUV) * interpolationFactor) * altitudeMultiplier

        calculateBurnTimes(_currentUV.value)
        cacheUVData(response, location)
    }

    private suspend fun cacheUVData(response: OpenMeteoResponse, location: Location) {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        response.daily.time.forEachIndexed { index, dateString ->
            val date = formatter.parse(dateString)
            if (date != null) {
                val startHour = index * 24
                val endHour = (index + 1) * 24
                val hourlyUv = response.hourly?.uv_index?.subList(startHour, endHour) ?: emptyList()
                val hourlyCloudCover = response.hourly?.cloud_cover?.subList(startHour, endHour) ?: emptyList()
                val sunrise = response.daily.sunrise.getOrNull(index)?.let { SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault()).parse(it) }
                val sunset = response.daily.sunset.getOrNull(index)?.let { SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault()).parse(it) }

                if (sunrise != null && sunset != null) {
                    val cachedData = CachedUVData(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        date = date,
                        hourlyUV = hourlyUv,
                        hourlyCloudCover = hourlyCloudCover,
                        maxUV = response.daily.uv_index_max.getOrNull(index) ?: 0.0,
                        sunrise = sunrise,
                        sunset = sunset,
                        lastUpdated = Date()
                    )
                    db.cachedUVDataDao().insert(cachedData)
                }
            }
        }
    }

    private suspend fun loadCachedData(location: Location) {
        val calendar = Calendar.getInstance()
        val today = calendar.time
        calendar.add(Calendar.DATE, 1)
        val tomorrow = calendar.time

        val latTolerance = 0.01
        val lonTolerance = 0.01
        val minLat = location.latitude - latTolerance
        val maxLat = location.latitude + latTolerance
        val minLon = location.longitude - lonTolerance
        val maxLon = location.longitude + lonTolerance

        val cachedData = db.cachedUVDataDao().getCachedData(minLat, maxLat, minLon, maxLon, today, tomorrow)
        val todayData = cachedData.find {
            val todayCal = Calendar.getInstance()
            val cachedCal = Calendar.getInstance().apply { time = it.date }
            todayCal.get(Calendar.YEAR) == cachedCal.get(Calendar.YEAR) &&
                    todayCal.get(Calendar.DAY_OF_YEAR) == cachedCal.get(Calendar.DAY_OF_YEAR)
        }
        if (todayData != null) {
            val altitudeMultiplier = 1.0 + (location.altitude / 1000.0 * 0.1)
            _maxUV.value = todayData.maxUV * altitudeMultiplier
            _todaySunrise.value = todayData.sunrise
            _todaySunset.value = todayData.sunset

            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            _currentUV.value = todayData.hourlyUV.getOrNull(hour)?.times(altitudeMultiplier) ?: 0.0
            calculateBurnTimes(_currentUV.value)
        } else {
            _error.value = "No cached data available."
        }
    }

    private fun calculateSafeExposureTimes() {
        val medTimesAtUV1 = mapOf(
            1 to 150.0,
            2 to 250.0,
            3 to 425.0,
            4 to 600.0,
            5 to 850.0,
            6 to 1100.0
        )

        val uvToUse = if (currentUV.value > 0) currentUV.value else 0.1
        val burnTimes = mutableMapOf<Int, Int>()
        for ((skinType, medTime) in medTimesAtUV1) {
            val fullMED = medTime / uvToUse
            burnTimes[skinType] = maxOf(1, fullMED.toInt())
        }
        _burnTimeMinutes.value = burnTimes
    }
}
