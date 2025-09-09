package com.example.sundayandroidapp.api

import com.google.gson.annotations.SerializedName

data class OpenMeteoResponse(
    val latitude: Double,
    val longitude: Double,
    @SerializedName("generationtime_ms")
    val generationTimeMs: Double,
    @SerializedName("utc_offset_seconds")
    val utcOffsetSeconds: Int,
    val timezone: String,
    @SerializedName("timezone_abbreviation")
    val timezoneAbbreviation: String,
    val elevation: Double,
    @SerializedName("current_units")
    val currentUnits: CurrentUnits,
    val current: CurrentData,
    @SerializedName("daily_units")
    val dailyUnits: DailyUnits,
    val daily: DailyData
)

data class CurrentUnits(
    val time: String,
    val interval: String,
    @SerializedName("uv_index")
    val uvIndex: String,
    @SerializedName("cloud_cover")
    val cloudCover: String
)

data class CurrentData(
    val time: String,
    val interval: Int,
    @SerializedName("uv_index")
    val uvIndex: Double,
    @SerializedName("cloud_cover")
    val cloudCover: Int
)

data class DailyUnits(
    val time: String,
    @SerializedName("uv_index_max")
    val uvIndexMax: String,
    val sunrise: String,
    val sunset: String
)

data class DailyData(
    val time: List<String>,
    @SerializedName("uv_index_max")
    val uvIndexMax: List<Double>,
    val sunrise: List<String>,
    val sunset: List<String>
)
