package com.gmolate.sunday.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.PI
import kotlin.math.sin

class VitaminDCalculator(private val healthManager: HealthManager) {
    private val _isInSun = MutableStateFlow(false)
    val isInSun: StateFlow<Boolean> = _isInSun.asStateFlow()

    private val _clothingLevel = MutableStateFlow(ClothingLevel.LIGHT)
    val clothingLevel: StateFlow<ClothingLevel> = _clothingLevel.asStateFlow()

    private val _skinType = MutableStateFlow(SkinType.TYPE_3)
    val skinType: StateFlow<SkinType> = _skinType.asStateFlow()

    private val _currentVitaminDRate = MutableStateFlow(0.0)
    val currentVitaminDRate: StateFlow<Double> = _currentVitaminDRate.asStateFlow()

    private val _sessionVitaminD = MutableStateFlow(0.0)
    val sessionVitaminD: StateFlow<Double> = _sessionVitaminD.asStateFlow()

    private var timerJob: Job? = null
    private var lastUV: Double = 0.0

    private val uvHalfMax = 4.0
    private val uvMaxFactor = 3.0
    private val dailyVitaminDGoal = 2000.0 // UI por día

    fun toggleSunExposure(uvIndex: Double, scope: CoroutineScope) {
        _isInSun.value = !_isInSun.value
        if (_isInSun.value) {
            startSession(uvIndex, scope)
        } else {
            stopSession()
            healthManager.saveVitaminDSession(_sessionVitaminD.value)
        }
    }

    private fun startSession(uvIndex: Double, scope: CoroutineScope) {
        _sessionVitaminD.value = 0.0
        lastUV = uvIndex

        timerJob = scope.launch {
            flow {
                while (true) {
                    emit(Unit)
                    delay(1000) // Actualizar cada segundo
                }
            }.collect {
                updateVitaminD(lastUV)
            }
        }

        updateVitaminDRate(uvIndex)
    }

    private fun stopSession() {
        timerJob?.cancel()
        timerJob = null
    }

    fun updateUV(uvIndex: Double) {
        lastUV = uvIndex
        updateVitaminDRate(uvIndex)
    }

    private fun updateVitaminD(uvIndex: Double) {
        if (!_isInSun.value) return

        val rate = calculateVitaminDRate(uvIndex)
        _sessionVitaminD.value += rate / 3600.0 // Convertir tasa por hora a tasa por segundo

        // Si alcanzamos el objetivo diario, notificar
        if (_sessionVitaminD.value >= dailyVitaminDGoal) {
            stopSession()
            healthManager.saveVitaminDSession(_sessionVitaminD.value)
        }
    }

    private fun updateVitaminDRate(uvIndex: Double) {
        _currentVitaminDRate.value = calculateVitaminDRate(uvIndex)
    }

    private fun calculateVitaminDRate(uvIndex: Double): Double {
        if (uvIndex <= 0) return 0.0

        // Factor base por tipo de piel
        val skinFactor = _skinType.value.vitaminDFactor

        // Factor por ropa
        val clothingFactor = _clothingLevel.value.exposureFactor

        // Factor por hora del día (mejor absorción cerca del mediodía)
        val timeQualityFactor = calculateTimeQualityFactor()

        // Factor por adaptación (basado en exposición previa)
        val adaptationFactor = calculateAdaptationFactor()

        // Fórmula mejorada para el cálculo de vitamina D
        val normalizedUV = uvIndex / uvHalfMax
        val baseRate = 1000.0 * (1.0 - exp(-normalizedUV * uvMaxFactor))

        return max(0.0, baseRate * skinFactor * clothingFactor * timeQualityFactor * adaptationFactor)
    }

    private fun calculateTimeQualityFactor(): Double {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val timeDecimal = hour + minute / 60.0

        // Factor de calidad basado en la hora del día (pico a las 13:00)
        val hoursFromNoon = abs(timeDecimal - 13.0)
        return max(0.1, exp(-hoursFromNoon * 0.2))
    }

    private fun calculateAdaptationFactor(): Double {
        // Este factor se podría calcular basándose en el historial de exposición
        // Por ahora retornamos un valor fijo
        return 1.0
    }

    private fun calculateSeasonalFactor(): Double {
        val calendar = Calendar.getInstance()
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)

        // Factor estacional basado en el día del año
        return 0.8 + 0.2 * sin(2.0 * PI * (dayOfYear - 172) / 365.0)
    }
}
