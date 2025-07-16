package com.gmolate.sunday.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max

class VitaminDCalculator(private val healthManager: HealthManager) {

    val isInSun = MutableStateFlow(false)
    val clothingLevel = MutableStateFlow(ClothingLevel.LIGHT)
    val skinType = MutableStateFlow(SkinType.TYPE3)
    val currentVitaminDRate = MutableStateFlow(0.0)
    val sessionVitaminD = MutableStateFlow(0.0)

    private var timerJob: Job? = null
    private var lastUV: Double = 0.0

    private val uvHalfMax = 4.0
    private val uvMaxFactor = 3.0

    fun startSession(uvIndex: Double, scope: CoroutineScope) {
        if (!isInSun.value) return

        sessionVitaminD.value = 0.0
        lastUV = uvIndex

        timerJob = scope.launch(Dispatchers.Default) {
            flow {
                while (true) {
                    emit(Unit)
                    delay(1000)
                }
            }.collect {
                updateVitaminD(lastUV)
            }
        }

        updateVitaminDRate(uvIndex)
    }

    fun stopSession() {
        timerJob?.cancel()
        timerJob = null
    }

    fun updateUV(uvIndex: Double) {
        lastUV = uvIndex
        updateVitaminDRate(uvIndex)
    }

    private fun updateVitaminDRate(uvIndex: Double) {
        val baseRate = 21000.0
        val uvFactor = (uvIndex * uvMaxFactor) / (uvHalfMax + uvIndex)
        val exposureFactor = clothingLevel.value.exposureFactor
        val skinFactor = skinType.value.vitaminDFactor
        val ageFactor = healthManager.getAgeFactor()
        val qualityFactor = calculateUVQualityFactor()
        val adaptationFactor = healthManager.getAdaptationFactor()

        currentVitaminDRate.value = baseRate * uvFactor * exposureFactor * skinFactor * ageFactor * qualityFactor * adaptationFactor
    }

    private fun updateVitaminD(uvIndex: Double) {
        if (!isInSun.value) return

        updateVitaminDRate(uvIndex)
        sessionVitaminD.value += currentVitaminDRate.value / 3600.0
    }

    fun toggleSunExposure(uvIndex: Double, scope: CoroutineScope) {
        isInSun.value = !isInSun.value

        if (isInSun.value) {
            startSession(uvIndex, scope)
        } else {
            stopSession()
        }
    }

    private fun calculateUVQualityFactor(): Double {
        val calendar = java.util.Calendar.getInstance()
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = calendar.get(java.util.Calendar.MINUTE)
        val timeDecimal = hour + minute / 60.0
        val solarNoon = 13.0
        val hoursFromNoon = abs(timeDecimal - solarNoon)
        val qualityFactor = exp(-hoursFromNoon * 0.2)
        return max(0.1, qualityFactor.coerceAtMost(1.0))
    }
}

enum class ClothingLevel(val descriptionText: String, val exposureFactor: Double) {
    NONE("Nude!", 1.0),
    MINIMAL("Minimal (swimwear)", 0.80),
    LIGHT("Light (shorts & t-shirt)", 0.40),
    MODERATE("Moderate (long sleeves)", 0.15),
    HEAVY("Heavy (fully covered)", 0.05)
}

enum class SkinType(val descriptionText: String, val vitaminDFactor: Double) {
    TYPE1("Very fair", 1.25),
    TYPE2("Fair", 1.1),
    TYPE3("Light", 1.0),
    TYPE4("Medium", 0.7),
    TYPE5("Dark", 0.4),
    TYPE6("Very dark", 0.2)
}
