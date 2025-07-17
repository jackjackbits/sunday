package com.gmolate.sunday

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.core.content.ContextCompat
import com.gmolate.sunday.ui.theme.SundayTheme
import com.gmolate.sunday.ui.view.ContentView
import com.gmolate.sunday.ui.viewmodel.MainViewModel

/**
 * MainActivity es la actividad principal de la aplicacion.
 * Se encarga de la configuracion inicial, la solicitud de permisos y la inicializacion de la interfaz de usuario.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Inicializa el ViewModel principal que gestiona la logica de la interfaz de usuario.
            val mainViewModel: MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

            // Lanza la solicitud de permisos de ubicacion y gestiona la respuesta.
            val locationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                if (permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
                    permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)
                ) {
                    // Si se conceden los permisos de ubicacion, obtiene la ubicacion actual.
                    mainViewModel.fetchLocation()
                }
            }

            // Lanza la solicitud de permisos de salud y gestiona la respuesta.
            val healthPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) {
                // Tras la respuesta de la solicitud de permisos de salud, obtiene los datos de salud.
                mainViewModel.fetchHealthData()
            }

            // Efecto lanzado una unica vez al inicio para gestionar los permisos.
            LaunchedEffect(Unit) {
                // Comprueba si los permisos de ubicacion ya estan concedidos.
                if (ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    // Si los permisos estan concedidos, obtiene la ubicacion.
                    mainViewModel.fetchLocation()
                } else {
                    // Si no, solicita los permisos de ubicacion.
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }

                // Comprueba si los permisos de salud ya estan concedidos.
                if (mainViewModel.hasHealthPermissions()) {
                    // Si estan concedidos, obtiene los datos de salud.
                    mainViewModel.fetchHealthData()
                } else {
                    // Si no, solicita los permisos de salud.
                    mainViewModel.requestHealthPermissions(this@MainActivity, healthPermissionLauncher)
                }
            }

            // Observa los cambios en la ubicacion y obtiene los datos de UV cuando la ubicacion esta disponible.
            mainViewModel.location.collectAsState().value?.let {
                mainViewModel.fetchUvData(it)
            }

            // Observa los cambios en el indice UV actual y lo actualiza en el ViewModel.
            mainViewModel.currentUV.collectAsState().value.let {
                mainViewModel.updateUv(it)
            }

            // Aplica el tema de la aplicacion.
            SundayTheme {
                // Contenedor de superficie que utiliza el color de fondo del tema.
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    // Muestra la vista principal de la aplicacion.
                    ContentView(mainViewModel)
                }
            }
        }
    }
}
