package com.gmolate.sunday.service

import android.location.Location
import com.gmolate.sunday.model.AppDatabase
import com.gmolate.sunday.model.CachedUVData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class UVService(private val db: AppDatabase) {

    private val api: OpenMeteoApi = Retrofit.Builder()
        .baseUrl("https://api.open-meteo.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OpenMeteoApi::class.java)

    private val _currentUV = MutableStateFlow(0.0)
    val currentUV: StateFlow<Double> = _currentUV

    private val _maxUV = MutableStateFlow(0.0)
    val maxUV: StateFlow<Double> = _maxUV

    private val _burnTimeMinutes = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val burnTimeMinutes: StateFlow<Map<Int, Int>> = _burnTimeMinutes

    private val _todaySunrise = MutableStateFlow<Date?>(null)
    val todaySunrise: StateFlow<Date?> = _todaySunrise

    private val _todaySunset = MutableStateFlow<Date?>(null)
    val todaySunset: StateFlow<Date?> = _todaySunset

    private val _isOfflineMode = MutableStateFlow(false)
    val isOfflineMode: StateFlow<Boolean> = _isOfflineMode

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    suspend fun fetchUVData(location: Location) {
        try {
            val response = api.getUvData(location.latitude, location.longitude, location.altitude)
            processUvData(response, location)
            _isOfflineMode.value = false
            _error.value = null
        } catch (e: HttpException) {
            loadCachedData(location)
            _error.value = "Network error: ${e.code()}"
            _isOfflineMode.value = true
        } catch (e: Exception) {
            loadCachedData(location)
            _error.value = "Error: ${e.message}"
            _isOfflineMode.value = true
        }
    }

    private suspend fun processUvData(response: OpenMeteoResponse, location: Location) {
        val altitudeMultiplier = 1.0 + (location.altitude / 1000.0 * 0.1)

        val todayMaxUv = response.daily.uv_index_max.firstOrNull() ?: 0.0
        _maxUV.value = todayMaxUv * altitudeMultiplier

        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
        _todaySunrise.value = response.daily.sunrise.firstOrNull()?.let { formatter.parse(it) }
        _todaySunset.value = response.daily.sunset.firstOrNull()?.let { formatter.parse(it) }

        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val currentHourUV = response.hourly?.uv_index?.get(hour) ?: 0.0
        val interpolationFactor = minute / 60.0
        val nextHourUV = response.hourly?.uv_index?.getOrNull(hour + 1) ?: currentHourUV
        val interpolatedUV = currentHourUV + (nextHourUV - currentHourUV) * interpolationFactor
        _currentUV.value = interpolatedUV * altitudeMultiplier

        calculateSafeExposureTimes()
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
            calculateSafeExposureTimes()
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
