package com.gmolate.sunday.service

import retrofit2.http.GET
import retrofit2.http.Query

interface FarmsenseApi {
    @GET("lunarphase_v2/")
    suspend fun getMoonPhase(
        @Query("d") timestamp: Long
    ): FarmsenseResponse
}

data class FarmsenseResponse(
    val DT: String,
    val UT: String,
    val TZ: String,
    val DateStamp: String,
    val TimeStamp: String,
    val Phase: String,
    val Age: Double,
    val Fraction: Double,
    val NPPhase: String,
    val MPPhase: String,
    val NNPhase: String,
    val PPPhase: String,
    val Segment: Int
) {
    // Computed properties para compatibilidad
    val moonPhase: Double get() = Age / 29.53 // Convertir age a fase normalizada (0-1)
    val illumination: Double get() = Fraction * 100.0
    val distanceKm: Double get() = 384400.0 // Distancia media tierra-luna
    val moonrise: String? get() = null // No disponible en esta API
    val moonset: String? get() = null // No disponible en esta API
}
