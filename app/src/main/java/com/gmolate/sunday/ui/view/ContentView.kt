package com.gmolate.sunday.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gmolate.sunday.service.ClothingLevel
import com.gmolate.sunday.service.SkinType
import com.gmolate.sunday.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ContentView(mainViewModel: MainViewModel = viewModel()) {
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
            error?.let {
                Text(text = it, color = Color.Red)
            }

            Text(
                text = "UV INDEX",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
            Text(
                text = String.format("%.1f", currentUv),
                fontSize = 72.sp,
                color = Color.White
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                InfoColumn(title = "MAX UVI", value = String.format("%.1f", maxUv))
                InfoColumn(title = "SUNRISE", value = todaySunrise?.let { SimpleDateFormat("HH:mm", Locale.getDefault()).format(it) } ?: "--:--")
                InfoColumn(title = "SUNSET", value = todaySunset?.let { SimpleDateFormat("HH:mm", Locale.getDefault()).format(it) } ?: "--:--")
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(onClick = { mainViewModel.toggleSunExposure() }) {
                Text(text = if (isInSun) "Stop Tracking" else "Start Tracking")
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
                    onSelected = { mainViewModel.clothingLevel.value = it }
                )
                SkinTypeSelector(
                    selected = skinType,
                    onSelected = { mainViewModel.skinType.value = it }
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
