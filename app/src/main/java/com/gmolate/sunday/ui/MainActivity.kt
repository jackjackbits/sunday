package com.gmolate.sunday.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import com.gmolate.sunday.ui.view.ContentView
import androidx.lifecycle.viewmodel.compose.viewModel
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.gmolate.sunday.ui.viewmodel.MainViewModel
import androidx.activity.viewModels

class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            mainViewModel.fetchLocation()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Solicitar permisos de ubicación al inicio
        requestLocationPermissions()

        setContent {
            val viewModel: MainViewModel = viewModel()

            // Efecto para solicitar permisos de Google Fit
            LaunchedEffect(Unit) {
                if (!viewModel.hasHealthPermissions()) {
                    viewModel.requestHealthPermissions(
                        activity = this@MainActivity,
                        launcher = registerForActivityResult(
                            ActivityResultContracts.StartActivityForResult()
                        ) { /* Handle result if needed */ }
                    )
                }
            }

            ContentView(viewModel)
        }
    }

    private fun requestLocationPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ).forEach { permission ->
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            locationPermissionRequest.launch(permissionsToRequest.toTypedArray())
        } else {
            mainViewModel.fetchLocation()
        }
    }
}
