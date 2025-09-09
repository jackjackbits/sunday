package com.example.sundayandroidapp.logic

import com.example.sundayandroidapp.data.ClothingLevel
import com.example.sundayandroidapp.data.SkinType
import kotlin.math.exp

object SunExposureCalculator {

    // VALORES Y FÓRMULA PORTADOS 1:1 DESDE LA VERSIÓN iOS
    // Minutos para alcanzar 1 MED (Minimal Erythema Dose) con un UV Index de 1
    private val medTimesAtUV1 = mapOf(
        SkinType.TYPE_1 to 150.0,
        SkinType.TYPE_2 to 250.0,
        SkinType.TYPE_3 to 425.0,
        SkinType.TYPE_4 to 600.0,
        SkinType.TYPE_5 to 850.0,
        SkinType.TYPE_6 to 1100.0
    )

    // Factores de Vitamina D basados en el tipo de piel (Referencia: iOS)
    private val vitaminDFactors = mapOf(
        SkinType.TYPE_1 to 1.25,
        SkinType.TYPE_2 to 1.1,
        SkinType.TYPE_3 to 1.0,
        SkinType.TYPE_4 to 0.7,
        SkinType.TYPE_5 to 0.4,
        SkinType.TYPE_6 to 0.2
    )

    // Factores de exposición según la ropa (Referencia: iOS)
    private val clothingExposureFactors = mapOf(
        ClothingLevel.NUDE to 1.0,
        ClothingLevel.MINIMAL to 0.80,
        ClothingLevel.LIGHT to 0.50,
        ClothingLevel.MODERATE to 0.30,
        ClothingLevel.HEAVY to 0.10
    )

    // Parámetros de la curva de saturación UV (Referencia: iOS)
    private const val UV_HALF_MAX = 4.0
    private const val UV_MAX_FACTOR = 3.0
    private const val BASE_VITAMIN_D_RATE = 21000.0 // IU/hora

    /**
     * Calcula el tiempo límite de quemado en minutos, usando la lógica de iOS.
     */
    fun calculateBurnLimitTime(
        uvIndex: Float,
        skinType: SkinType,
        clothingLevel: ClothingLevel, // No usado en la fórmula de iOS, pero se mantiene por consistencia
        cloudCoverPercentage: Int, // No usado, UV ya lo incluye
        altitudeMeters: Int // No usado, UV ya lo incluye
    ): Int {
        if (uvIndex <= 0) return 0

        val medTimeAtUV1 = medTimesAtUV1[skinType] ?: 425.0 // Default a Tipo 3
        
        // FÓRMULA CORRECTA (PORTADA DE iOS)
        val burnLimitMinutes = medTimeAtUV1 / uvIndex

        return burnLimitMinutes.toInt()
    }

    /**
     * Calcula la tasa de producción de Vitamina D en IU/minuto, usando la lógica de iOS.
     */
    fun calculateVitaminDProductionRate(
        uvIndex: Float,
        skinType: SkinType,
        clothingLevel: ClothingLevel,
        cloudCoverPercentage: Int, // No usado
        altitudeMeters: Int // No usado
    ): Int {
        if (uvIndex <= 0) return 0

        // FÓRMULA DE SATURACIÓN UV (PORTADA DE iOS)
        val uvFactor = (uvIndex * UV_MAX_FACTOR) / (UV_HALF_MAX + uvIndex)

        val exposureFactor = clothingExposureFactors[clothingLevel] ?: 0.5
        val skinFactor = vitaminDFactors[skinType] ?: 1.0

        // Tasa de Vitamina D por HORA
        val hourlyRate = BASE_VITAMIN_D_RATE * uvFactor * exposureFactor * skinFactor

        // Convertir a tasa por MINUTO
        val perMinuteRate = hourlyRate / 60.0

        return perMinuteRate.toInt()
    }
}