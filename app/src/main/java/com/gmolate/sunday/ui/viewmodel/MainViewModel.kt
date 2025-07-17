package com.gmolate.sunday.ui.viewmodel

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.location.Location
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gmolate.sunday.model.AppDatabase
import com.gmolate.sunday.service.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val locationManager = LocationManager(application)
    private val healthManager = HealthManager(application)
    private val networkMonitor = NetworkMonitor(application)
    private val migrationService = MigrationService(application)
    private val notificationService = NotificationService(application)
    private val uvService = UVService(db, notificationService)
    private val vitaminDCalculator = VitaminDCalculator(healthManager)
    private val moonPhaseService = MoonPhaseService(application, db)
    private val solarNoonService = SolarNoonNotificationService(application)

    private val _location = MutableStateFlow<Location?>(null)
    val location: StateFlow<Location?> = _location.asStateFlow()

    private val _locationName = MutableStateFlow("")
    val locationName: StateFlow<String> = _locationName.asStateFlow()

    private val _currentUV = MutableStateFlow(0.0)
    val currentUV: StateFlow<Double> = _currentUV.asStateFlow()

    private val _maxUV = MutableStateFlow(0.0)
    val maxUV: StateFlow<Double> = _maxUV.asStateFlow()

    private val _burnTimeMinutes = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val burnTimeMinutes: StateFlow<Map<Int, Int>> = _burnTimeMinutes.asStateFlow()

    private val _todaySunrise = MutableStateFlow<Long?>(null)
    val todaySunrise: StateFlow<Long?> = _todaySunrise.asStateFlow()

    private val _todaySunset = MutableStateFlow<Long?>(null)
    val todaySunset: StateFlow<Long?> = _todaySunset.asStateFlow()

    private val _isOfflineMode = MutableStateFlow(false)
    val isOfflineMode: StateFlow<Boolean> = _isOfflineMode.asStateFlow()

    // Estados para fases lunares
    val currentMoonPhase: StateFlow<String> = moonPhaseService.currentMoonPhase
    val currentMoonIcon: StateFlow<String> = moonPhaseService.currentMoonIcon
    val moonAge: StateFlow<Double> = moonPhaseService.moonAge
    val moonFraction: StateFlow<Double> = moonPhaseService.moonFraction

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = combine(
        locationManager.error,
        uvService.error
    ) { locationError, uvError ->
        locationError ?: uvError
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val isInSun: StateFlow<Boolean> = vitaminDCalculator.isInSun
    val clothingLevel: StateFlow<ClothingLevel> = vitaminDCalculator.clothingLevel
    val skinType: StateFlow<SkinType> = vitaminDCalculator.skinType
    val currentVitaminDRate: StateFlow<Double> = vitaminDCalculator.currentVitaminDRate
    val sessionVitaminD: StateFlow<Double> = vitaminDCalculator.sessionVitaminD

    init {
        viewModelScope.launch {
            migrationService.migrateDataIfNeeded()
            // Cargar fases lunares al inicio
            moonPhaseService.fetchMoonPhase()
            
            // Usar un solo Job para todas las colecciones para mejor manejo de memoria
            launch {
                networkMonitor.isOnline.collect { isOnline ->
                    if (isOnline) {
                        updateUVData()
                        // Actualizar fases lunares cuando hay conexión
                        moonPhaseService.fetchMoonPhase()
                    }
                }
            }
            
            // Agrupar las colecciones relacionadas para optimizar recursos
            launch { 
                locationManager.location.collect { 
                    _location.value = it
                    // Programar notificaciones cuando se obtiene ubicación nueva
                    it?.let { loc ->
                        if (solarNoonService.isSolarNoonNotificationEnabled()) {
                            solarNoonService.scheduleSolarNoonNotification(loc)
                        }
                    }
                } 
            }
            launch { locationManager.locationName.collect { _locationName.value = it } }
            
            // Colecciones de UV
            launch { uvService.currentUV.collect { _currentUV.value = it } }
            launch { uvService.maxUV.collect { _maxUV.value = it } }
            launch { uvService.burnTimeMinutes.collect { _burnTimeMinutes.value = it } }
            launch {
                uvService.todaySunrise.collect { date ->
                    _todaySunrise.value = date?.time
                }
            }
            launch {
                uvService.todaySunset.collect { date ->
                    _todaySunset.value = date?.time
                }
            }
            launch { uvService.isOfflineMode.collect { _isOfflineMode.value = it } }
        }
    }

    private fun updateUVData() {
        viewModelScope.launch {
            location.value?.let { loc ->
                uvService.fetchUVData(loc)
            }
        }
    }

    fun updateClothingLevel(level: ClothingLevel) {
        vitaminDCalculator.clothingLevel.value = level
    }

    fun updateSkinType(type: SkinType) {
        vitaminDCalculator.skinType.value = type
    }

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

    fun enableSolarNoonNotifications(enabled: Boolean) {
        solarNoonService.setSolarNoonNotificationEnabled(enabled)
        if (enabled) {
            location.value?.let { loc ->
                solarNoonService.scheduleSolarNoonNotification(loc)
            }
        }
    }

    fun isSolarNoonNotificationEnabled(): Boolean {
        return solarNoonService.isSolarNoonNotificationEnabled()
    }
}
