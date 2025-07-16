package com.gmolate.sunday.ui.viewmodel

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.location.Location
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gmolate.sunday.model.AppDatabase
import com.gmolate.sunday.service.HealthManager
import com.gmolate.sunday.service.LocationManager
import com.gmolate.sunday.service.UVService
import com.gmolate.sunday.service.VitaminDCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val locationManager = LocationManager(application)
    private val healthManager = HealthManager(application)
    private val uvService = UVService(db)
    private val vitaminDCalculator = VitaminDCalculator(healthManager)

    val location: StateFlow<Location?> = locationManager.location
    val locationName: StateFlow<String> = locationManager.locationName
    val currentUV: StateFlow<Double> = uvService.currentUV
    val maxUV: StateFlow<Double> = uvService.maxUV
    val burnTimeMinutes: StateFlow<Map<Int, Int>> = uvService.burnTimeMinutes
    val todaySunrise = uvService.todaySunrise
    val todaySunset = uvService.todaySunset
    val isOfflineMode = uvService.isOfflineMode

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = combine(locationManager.error, uvService.error) { locationError, uvError ->
        locationError ?: uvError
    }.asStateFlow(null)

    val isInSun = vitaminDCalculator.isInSun
    val clothingLevel = vitaminDCalculator.clothingLevel
    val skinType = vitaminDCalculator.skinType
    val currentVitaminDRate = vitaminDCalculator.currentVitaminDRate
    val sessionVitaminD = vitaminDCalculator.sessionVitaminD

    fun fetchLocation() {
        viewModelScope.launch {
            locationManager.fetchLocation()
        }
    }

    fun fetchUvData(location: Location) {
        viewModelScope.launch {
            uvService.fetchUVData(location)
        }
    }

    fun toggleSunExposure() {
        viewModelScope.launch {
            vitaminDCalculator.toggleSunExposure(currentUV.value, viewModelScope)
        }
    }

    fun updateUv(uv: Double) {
        viewModelScope.launch {
            vitaminDCalculator.updateUV(uv)
        }
    }

    fun hasHealthPermissions(): Boolean {
        return healthManager.hasPermissions()
    }

    fun requestHealthPermissions(activity: Activity, launcher: ActivityResultLauncher<Intent>) {
        healthManager.requestPermissions(activity)
    }

    fun fetchHealthData() {
        viewModelScope.launch {
            vitaminDCalculator.updateUV(currentUV.value)
        }
    }
}
