package com.gmolate.sunday.service

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import java.util.Locale

class LocationManager(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private val geocoder = Geocoder(context, Locale.getDefault())

    private val _location = MutableStateFlow<Location?>(null)
    val location: StateFlow<Location?> = _location

    private val _locationName = MutableStateFlow("")
    val locationName: StateFlow<String> = _locationName

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    @SuppressLint("MissingPermission")
    suspend fun fetchLocation() {
        try {
            val locationResult = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                CancellationTokenSource().token
            ).await()
            locationResult?.let {
                _location.value = it
                fetchLocationName(it)
                _error.value = null
            } ?: run {
                _error.value = "Could not get location."
            }
        } catch (e: Exception) {
            _error.value = e.message
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun fetchLocationName(location: Location) {
        try {
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val locationName = when {
                    !address.locality.isNullOrEmpty() -> address.locality
                    !address.subAdminArea.isNullOrEmpty() -> address.subAdminArea
                    !address.adminArea.isNullOrEmpty() -> address.adminArea
                    !address.countryName.isNullOrEmpty() -> address.countryName
                    else -> "Ubicación desconocida"
                }
                _locationName.value = locationName
            } else {
                _locationName.value = "Ubicación desconocida"
            }
        } catch (e: Exception) {
            _locationName.value = "Ubicación desconocida"
        }
    }

    fun requestLocation() {
        // Iniciar solicitud de ubicación de forma asíncrona
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            fetchLocation()
        }
    }

    @SuppressLint("MissingPermission")
    fun getLastKnownLocation(): Location? {
        return try {
            fusedLocationClient.lastLocation.result
        } catch (e: Exception) {
            null
        }
    }
}
