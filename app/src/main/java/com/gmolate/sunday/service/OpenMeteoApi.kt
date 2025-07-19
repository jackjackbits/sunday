package com.gmolate.sunday.service

import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoApi {
    @GET("v1/forecast")
    suspend fun getUvData(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("elevation") elevation: Double,
        @Query("daily") daily: String = "uv_index_max,uv_index_clear_sky_max,sunrise,sunset,temperature_2m_max",
        @Query("hourly") hourly: String = "uv_index,cloud_cover,temperature_2m,relative_humidity_2m",
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = 5
    ): OpenMeteoResponse
}

data class OpenMeteoResponse(
    val daily: DailyData,
    val hourly: HourlyData?,
    val elevation: Double?
)

data class DailyData(
    val time: List<String>,
    val uv_index_max: List<Double>,
    val uv_index_clear_sky_max: List<Double>?,
    val sunrise: List<String>,
    val sunset: List<String>,
    val temperature_2m_max: List<Double>?
)

data class HourlyData(
    val time: List<String>,
    val uv_index: List<Double>,
    val cloud_cover: List<Double>?,
    val temperature_2m: List<Double>?,
    val relative_humidity_2m: List<Double>?
)
