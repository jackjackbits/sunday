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
        lastUV = uvIndex
        _sessionVitaminD.value = 0.0

        timerJob = scope.launch {
            while (_isInSun.value) {
                delay(60_000) // 1 minuto
                if (_isInSun.value) {
                    val vitaminDGenerated = calculateVitaminDPerMinute(lastUV)
                    _sessionVitaminD.value += vitaminDGenerated
                    _currentVitaminDRate.value = vitaminDGenerated
                }
            }
        }
    }

    private fun stopSession() {
        timerJob?.cancel()
        timerJob = null
        _currentVitaminDRate.value = 0.0
    }

    suspend fun calculateVitaminDPerMinute(uvIndex: Double): Double {
        if (uvIndex <= 0) return 0.0

        val baseRate = calculateBaseVitaminDRate(uvIndex)
        val skinFactor = _skinType.value.vitaminDFactor
        val clothingFactor = _clothingLevel.value.exposureFactor
        val ageFactor = healthManager.getAgeFactor()
        val adaptationFactor = healthManager.getAdaptationFactor()

        return baseRate * skinFactor * clothingFactor * ageFactor * adaptationFactor
    }

    private fun calculateBaseVitaminDRate(uvIndex: Double): Double {
        // Fórmula basada en estudios científicos
        // UV óptimo alrededor de 4-6, con rendimientos decrecientes después
        val uvFactor = when {
            uvIndex <= 0 -> 0.0
            uvIndex <= 2 -> uvIndex / 2.0 * 0.5
            uvIndex <= 6 -> 0.5 + (uvIndex - 2) / 4.0 * 0.5
            else -> 1.0 - exp(-(uvIndex - 6) / 3.0) * 0.2
        }

        // Factor de tiempo solar (mejor al mediodía)
        val solarTimeFactor = calculateSolarTimeFactor()

        // Tasa base: ~1000 IU en 10-15 minutos de exposición óptima
        val baseIUPerMinute = 80.0

        return baseIUPerMinute * uvFactor * solarTimeFactor
    }

    private fun calculateSolarTimeFactor(): Double {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val timeInHours = hour + minute / 60.0

        // Mediodía solar óptimo (12:00), con reducción gradual
        val distanceFromNoon = abs(timeInHours - 12.0)

        return when {
            distanceFromNoon <= 1 -> 1.0 // 11:00 - 13:00
            distanceFromNoon <= 2 -> 0.8 // 10:00 - 14:00
            distanceFromNoon <= 3 -> 0.6 // 09:00 - 15:00
            distanceFromNoon <= 4 -> 0.4 // 08:00 - 16:00
            else -> 0.2 // Temprano en la mañana o tarde
        }
    }

    fun updateUV(uvIndex: Double) {
        lastUV = uvIndex
        if (_isInSun.value) {
            // Actualizar tasa actual si estamos en sesión
            _currentVitaminDRate.value = calculateVitaminDPerMinute(uvIndex)
        }
    }

    fun setSkinType(skinType: SkinType) {
        _skinType.value = skinType
    }

    fun setClothingLevel(level: ClothingLevel) {
        _clothingLevel.value = level
    }

    fun getEstimatedTimeForGoal(uvIndex: Double, goalIU: Double = dailyVitaminDGoal): Int {
        if (uvIndex <= 0) return Int.MAX_VALUE

        val ratePerMinute = calculateVitaminDPerMinute(uvIndex)
        if (ratePerMinute <= 0) return Int.MAX_VALUE

        return (goalIU / ratePerMinute).toInt()
    }

    fun getSafeExposureTime(uvIndex: Double): Int {
        if (uvIndex <= 0) return 60 // Default 1 hora si no hay UV

        return _skinType.value.getSafeExposureMinutes(uvIndex)
    }

    fun getOptimalExposureTime(uvIndex: Double): Int {
        val safeTime = getSafeExposureTime(uvIndex)
        val goalTime = getEstimatedTimeForGoal(uvIndex, dailyVitaminDGoal / 2) // 50% del objetivo

        return minOf(safeTime, goalTime)
    }

    suspend fun getDailyProgress(): Double {
        val todayVitaminD = healthManager.getTodayVitaminD()
        val sessionVitD = _sessionVitaminD.value
        val total = todayVitaminD + sessionVitD

        return (total / dailyVitaminDGoal * 100.0).coerceAtMost(100.0)
    }

    fun reset() {
        stopSession()
        _sessionVitaminD.value = 0.0
        _isInSun.value = false
    }
}
