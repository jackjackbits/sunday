package com.gmolate.sunday.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gmolate.sunday.service.ClothingLevel
import com.gmolate.sunday.service.SkinType
import com.gmolate.sunday.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ContentView(mainViewModel: MainViewModel = viewModel()) {
    var showSettings by remember { mutableStateOf(false) }
    
    if (showSettings) {
        SettingsView(
            mainViewModel = mainViewModel,
            onBack = { showSettings = false }
        )
        return
    }
    val currentUv by mainViewModel.currentUV.collectAsState()
    val maxUv by mainViewModel.maxUV.collectAsState()
    val burnTimeMinutes by mainViewModel.burnTimeMinutes.collectAsState()
    val todaySunrise by mainViewModel.todaySunrise.collectAsState()
    val todaySunset by mainViewModel.todaySunset.collectAsState()
    val isInSun by mainViewModel.isInSun.collectAsState()
    val clothingLevel by mainViewModel.clothingLevel.collectAsState()
    val skinType by mainViewModel.skinType.collectAsState()
    val sessionVitaminD by mainViewModel.sessionVitaminD.collectAsState()
    val currentVitaminDRate by mainViewModel.currentVitaminDRate.collectAsState()
    val isOfflineMode by mainViewModel.isOfflineMode.collectAsState()
    val error by mainViewModel.error.collectAsState()

    // Estados de fases lunares
    val currentMoonPhase by mainViewModel.currentMoonPhase.collectAsState()
    val currentMoonIcon by mainViewModel.currentMoonIcon.collectAsState()
    val moonAge by mainViewModel.moonAge.collectAsState()
    val moonFraction by mainViewModel.moonFraction.collectAsState()


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF4A90E2),
                        Color(0xFF7BB7E5)
                    )
                )
            )
            .semantics {
                contentDescription = "Pantalla principal de Sunday"
            }
    ) {
        // Botón de configuraciones en la esquina superior derecha
        IconButton(
            onClick = { showSettings = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Text("⚙️", fontSize = 24.sp)
        }
                    colors = listOf(
                        Color(0xFF4A90E2),
                        Color(0xFF7BB7E5)
                    )
                )
            )
            .semantics {
                contentDescription = "Pantalla principal de Sunday"
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isOfflineMode) {
                Text(text = "Offline Mode", color = Color.White)
            }
            error?.let { errorMessage ->
                Text(text = errorMessage, color = Color.Red)
            }

            Text(
                text = if (currentUv > 0) "UV INDEX" else "MOON PHASE",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
            
            // Animación suave entre UV y fases lunares
            AnimatedVisibility(
                visible = currentUv > 0,
                enter = fadeIn(animationSpec = tween(600)) + slideInVertically(),
                exit = fadeOut(animationSpec = tween(600)) + slideOutVertically()
            ) {
                Text(
                    text = String.format("%.1f", currentUv),
                    fontSize = 72.sp,
                    color = Color.White,
                    modifier = Modifier
                        .semantics {
                            contentDescription = "Índice UV actual: ${String.format("%.1f", currentUv)}"
                        }
                )
            }

            AnimatedVisibility(
                visible = currentUv <= 0,
                enter = fadeIn(animationSpec = tween(600)) + slideInVertically(),
                exit = fadeOut(animationSpec = tween(600)) + slideOutVertically()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = currentMoonIcon,
                        fontSize = 72.sp,
                        modifier = Modifier
                            .semantics {
                                contentDescription = "Fase lunar actual: $currentMoonPhase"
                            }
                    )
                    Text(
                        text = currentMoonPhase,
                        fontSize = 16.sp,
                        color = Color.White,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        text = "Age: ${String.format("%.1f", moonAge)} days",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                InfoColumn(title = "MAX UVI", value = String.format("%.1f", maxUv))
                InfoColumn(title = "SUNRISE", value = todaySunrise?.let { SimpleDateFormat("HH:mm", Locale.getDefault()).format(it) } ?: "--:--")
                InfoColumn(title = "SUNSET", value = todaySunset?.let { SimpleDateFormat("HH:mm", Locale.getDefault()).format(it) } ?: "--:--")
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { mainViewModel.toggleSunExposure() },
                modifier = Modifier
                    .semantics {
                        contentDescription = if (isInSun) "Detener exposición al sol" else "Iniciar exposición al sol"
                    }
            ) {
                Text(if (isInSun) "Detener" else "Iniciar")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "Session Vitamin D: ${sessionVitaminD.toInt()} IU")
            Text(text = "Vitamin D Rate: ${currentVitaminDRate.toInt()} IU/hr")

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                ClothingSelector(
                    selected = clothingLevel,
                    onSelected = { newLevel ->
                        mainViewModel.updateClothingLevel(newLevel)
                    }
                )
                SkinTypeSelector(
                    selected = skinType,
                    onSelected = { newType ->
                        mainViewModel.updateSkinType(newType)
                    }
                )
            }
        }
    }
}

@Composable
fun InfoColumn(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = title, fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
        Text(text = value, fontSize = 20.sp, color = Color.White)
    }
}

@Composable
fun ClothingSelector(selected: ClothingLevel, onSelected: (ClothingLevel) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(text = "Clothing", color = Color.White)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.2f))
                .clickable { expanded = true }
                .padding(8.dp)
        ) {
            Text(text = selected.descriptionText, color = Color.White)
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                ClothingLevel.values().forEach { level ->
                    DropdownMenuItem(
                        text = { Text(level.descriptionText) },
                        onClick = {
                            onSelected(level)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SkinTypeSelector(selected: SkinType, onSelected: (SkinType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(text = "Skin Type", color = Color.White)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.2f))
                .clickable { expanded = true }
                .padding(8.dp)
        ) {
            Text(text = selected.descriptionText, color = Color.White)
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                SkinType.values().forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type.descriptionText) },
                        onClick = {
                            onSelected(type)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
