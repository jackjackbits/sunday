package com.gmolate.sunday.ui.view

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gmolate.sunday.ui.viewmodel.MainViewModel

// Importaciones necesarias para el botón de configuración y el Dialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentView(viewModel: MainViewModel) {
    val currentUv by viewModel.currentUV.collectAsState()
    val moonPhase by viewModel.moonPhase.moonPhase.collectAsState() // Asumo que moonPhase es un StateFlow de un objeto con una propiedad moonPhase
    val location by viewModel.location.collectAsState()
    val error by viewModel.error.collectAsState()
    val isOfflineMode by viewModel.isOfflineMode.collectAsState()
    val healthData by viewModel.healthData.collectAsState()
    val vitaminDProgress by viewModel.vitaminDProgress.collectAsState()

    // Estado para controlar la visibilidad de la pantalla de configuración
    var showSettingsView by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sunday") }, // Título de la app
                actions = {
                    // Botón de configuración en la barra superior
                    IconButton(onClick = { showSettingsView = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Configuración")
                    }
                }
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues), // Importante para que el contenido no se solape con el TopAppBar
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Estado offline
                if (isOfflineMode) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text(
                            text = "Modo Sin Conexión",
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                // Mostrar errores
                error?.let { errorMessage ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = errorMessage,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // Título principal
            Text(
                text = if (currentUv > 0) "ÍNDICE UV" else "FASE LUNAR",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Valor principal - UV o fase lunar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedVisibility(
                        visible = currentUv > 0,
                        enter = fadeIn(animationSpec = tween(600)) + slideInVertically(),
                        exit = fadeOut(animationSpec = tween(600)) + slideOutVertically()
                    ) {
                        Text(
                            text = String.format("%.1f", currentUv),
                            fontSize = 72.sp,
                            fontWeight = FontWeight.Bold,
                            color = getUVColor(currentUv),
                            modifier = Modifier.semantics {
                                contentDescription = "Índice UV actual: ${String.format("%.1f", currentUv)}"
                            }
                        )
                    }

                    AnimatedVisibility(
                        visible = currentUv <= 0,
                        enter = fadeIn(animationSpec = tween(600)) + slideInVertically(),
                        exit = fadeOut(animationSpec = tween(600)) + slideInVertically()
                    ) {
                        Text(
                            text = getMoonPhaseDescription(moonPhase),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.semantics {
                                contentDescription = "Fase lunar actual: ${getMoonPhaseDescription(moonPhase)}"
                            }
                        )
                    }
                }
            }

            // Información adicional
            if (currentUv > 0) {
                UVInfoCard(currentUv = currentUv)
            }

            // Información de ubicación
            location?.let { loc ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Ubicación",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${String.format("%.3f", loc.latitude)}, ${String.format("%.3f", loc.longitude)}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Progreso de vitamina D
            if (vitaminDProgress > 0) {
                VitaminDProgressCard(progress = vitaminDProgress)
            }

            // Datos de salud (si están disponibles)
            healthData?.let { health ->
                HealthDataCard(healthData = health)
            }
        }
    }

    // Diálogo para mostrar la pantalla de configuración
    if (showSettingsView) {
        Dialog(onDismissRequest = { showSettingsView = false }) {
            // Asegúrate de que SettingsView esté en el paquete correcto o impórtalo
            // Por ejemplo: com.gmolate.sunday.ui.view.SettingsView
            SettingsView(Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun UVInfoCard(currentUv: Double) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Recomendación",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = getUVRecommendation(currentUv),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun VitaminDProgressCard(progress: Double) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Progreso Vitamina D",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = (progress / 100).toFloat(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${String.format("%.1f", progress)}% del objetivo diario",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun HealthDataCard(healthData: Any) {
    // Placeholder para datos de salud
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Datos de Salud",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Conectado a HealthKit",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

private fun getUVColor(uv: Double): Color {
    return when {
        uv < 3 -> Color.Green
        uv < 6 -> Color.Yellow
        uv < 8 -> Color(0xFFFF8C00) // Orange
        uv < 11 -> Color.Red
        else -> Color(0xFF8B00FF) // Purple
    }
}

private fun getUVRecommendation(uv: Double): String {
    return when {
        uv < 3 -> "Bajo riesgo. Puedes estar al sol sin protección por tiempo limitado."
        uv < 6 -> "Riesgo moderado. Usa protección solar si vas a estar expuesto por mucho tiempo."
        uv < 8 -> "Alto riesgo. Usa protector solar, ropa protectora y busca sombra."
        uv < 11 -> "Muy alto riesgo. Minimiza la exposición al sol entre 10 AM y 4 PM."
        else -> "Extremo. Evita la exposición al sol. Usa toda la protección disponible."
    }
}

private fun getMoonPhaseDescription(phase: Double): String {
    return when {
        phase < 0.125 -> "Luna Nueva"
        phase < 0.25 -> "Cuarto Creciente"
        phase < 0.375 -> "Creciente Gibosa"
        phase < 0.5 -> "Luna Llena"
        phase < 0.625 -> "Menguante Gibosa"
        phase < 0.75 -> "Cuarto Menguante"
        phase < 0.875 -> "Menguante"
        else -> "Luna Nueva"
    }
}
"}}}