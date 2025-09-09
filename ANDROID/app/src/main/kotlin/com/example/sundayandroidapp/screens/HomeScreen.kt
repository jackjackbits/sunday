package com.example.sundayandroidapp.screens

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.sundayandroidapp.R
import com.example.sundayandroidapp.api.OpenMeteoApiService
import com.example.sundayandroidapp.data.ClothingLevel
import com.example.sundayandroidapp.data.SkinType
import com.example.sundayandroidapp.location.LocationService
import com.example.sundayandroidapp.logic.SunExposureCalculator
import com.example.sundayandroidapp.ui.theme.BlueGradient
import com.example.sundayandroidapp.ui.theme.SundayAndroidAppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Composable
fun HomeScreen(currentLanguage: String, onLanguageChange: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedSkinType by remember { mutableStateOf(SkinType.TYPE_3) }
    var selectedClothingLevel by remember { mutableStateOf(ClothingLevel.LIGHT) }
    var isTracking by remember { mutableStateOf(false) }
    var sessionTotal by remember { mutableFloatStateOf(0.0f) }
    var todayTotal by remember { mutableFloatStateOf(0.0f) }
    var showSkinTypeDialog by remember { mutableStateOf(false) }
    var showClothingLevelDialog by remember { mutableStateOf(false) }
    var showMethodology by remember { mutableStateOf(false) }
    var locationPermissionsGranted by remember { mutableStateOf(false) }
    var currentUvIndex by remember { mutableFloatStateOf(0.0f) }
    var maxUvIndex by remember { mutableFloatStateOf(0.0f) }
    var maxUvIndexTime by remember { mutableStateOf("") }
    var sunriseTime by remember { mutableStateOf("") }
    var sunsetTime by remember { mutableStateOf("") }
    var cloudCoverPercentage by remember { mutableIntStateOf(0) }
    var altitudeMeters by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }

    if (showMethodology) {
        MethodologyScreen(currentLanguage = currentLanguage, onDismiss = { showMethodology = false })
        return
    }

    val openMeteoApiService = remember {
        Retrofit.Builder().baseUrl("https://api.open-meteo.com/").addConverterFactory(GsonConverterFactory.create()).build().create(OpenMeteoApiService::class.java)
    }
    val locationService = remember { LocationService(context) }

    val fetchData: suspend () -> Unit = {
        isLoading = true
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            locationPermissionsGranted = true
            val location = locationService.getLastKnownLocation()
            location?.let { loc ->
                try {
                    val response = openMeteoApiService.getSunData(loc.latitude, loc.longitude)
                    currentUvIndex = response.current.uvIndex.toFloat()
                    cloudCoverPercentage = response.current.cloudCover
                    altitudeMeters = response.elevation.toInt()
                    if (response.daily.uvIndexMax.isNotEmpty()) maxUvIndex = response.daily.uvIndexMax[0].toFloat()
                    if (response.daily.sunrise.isNotEmpty()) sunriseTime = formatTime(response.daily.sunrise[0])
                    if (response.daily.sunset.isNotEmpty()) sunsetTime = formatTime(response.daily.sunset[0])
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            locationPermissionsGranted = false
        }
        isLoading = false
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        locationPermissionsGranted = permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false)
        if (locationPermissionsGranted) {
            scope.launch { fetchData() }
        }
    }

    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            fetchData()
        } else {
            locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    val burnLimit = SunExposureCalculator.calculateBurnLimitTime(currentUvIndex, selectedSkinType, selectedClothingLevel, cloudCoverPercentage, altitudeMeters)
    val vitaminDRateCalculated: Float = SunExposureCalculator.calculateVitaminDProductionRate(currentUvIndex, selectedSkinType, selectedClothingLevel, cloudCoverPercentage, altitudeMeters).toFloat()

    LaunchedEffect(isTracking, vitaminDRateCalculated) {
        if (isTracking) {
            while (isTracking) {
                sessionTotal += (vitaminDRateCalculated / 60.0f)
                todayTotal += (vitaminDRateCalculated / 60.0f)
                delay(1000L)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(onClick = { scope.launch { fetchData() } }) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh Location")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().background(BlueGradient).padding(paddingValues).padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("SUN DAY", fontSize = 40.sp, fontWeight = FontWeight.Black, color = Color.White)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showMethodology = true }) {
                        Icon(Icons.Default.MenuBook, contentDescription = "Methodology", tint = Color.White)
                    }
                    Text("EN", color = if (currentLanguage == "en") Color.Yellow else Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.clickable { onLanguageChange("en") })
                    Text("ES", color = if (currentLanguage == "es") Color.Yellow else Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.clickable { onLanguageChange("es") })
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(stringResource(R.string.uv_index_label), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("%.1f".format(currentUvIndex), fontSize = 80.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text(stringResource(R.string.burn_limit_template, burnLimit), fontSize = 20.sp, color = Color.White)
                }
            }
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.sunrise_template, sunriseTime), color = Color.White)
                        Text(stringResource(R.string.sunset_template, sunsetTime), color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.max_uv_template, maxUvIndex, maxUvIndexTime), color = Color.White)
                        Text(stringResource(R.string.clouds_altitude_template, cloudCoverPercentage, altitudeMeters, 6.0f), color = Color.White)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            SessionTrackingCard(
                rate = vitaminDRateCalculated,
                sessionTotal = sessionTotal,
                todayTotal = todayTotal,
                isTracking = isTracking,
                onToggleTracking = {
                    isTracking = !isTracking
                    if (!isTracking) sessionTotal = 0.0f
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { showClothingLevelDialog = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                    Text(stringResource(R.string.select_clothing_level), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(selectedClothingLevel.level, color = Color.White, fontSize = 20.sp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { showSkinTypeDialog = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                    Text(stringResource(R.string.select_skin_type), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.skin_type_template, selectedSkinType.type), color = Color.White, fontSize = 20.sp)
                    Text(selectedSkinType.description, color = Color.White, fontSize = 14.sp)
                }
            }
        }
    }

    if (showSkinTypeDialog) {
        val skinTypeOptions = remember(currentLanguage) {
            SkinType.entries.map { skinType ->
                context.getString(R.string.skin_type_template, skinType.type) + ": " + when (skinType) {
                    SkinType.TYPE_1 -> context.getString(R.string.skin_type_1_desc)
                    SkinType.TYPE_2 -> context.getString(R.string.skin_type_2_desc)
                    SkinType.TYPE_3 -> context.getString(R.string.skin_type_3_desc)
                    SkinType.TYPE_4 -> context.getString(R.string.skin_type_4_desc)
                    SkinType.TYPE_5 -> context.getString(R.string.skin_type_5_desc)
                    SkinType.TYPE_6 -> context.getString(R.string.skin_type_6_desc)
                }
            }
        }
        OptionsSelectorDialog(
            title = stringResource(R.string.select_skin_type),
            options = SkinType.entries,
            selectedOption = selectedSkinType,
            onOptionSelected = { skinType -> selectedSkinType = skinType },
            onDismissRequest = { showSkinTypeDialog = false },
            optionText = { skinType -> skinTypeOptions[skinType.ordinal] }
        )
    }

    if (showClothingLevelDialog) {
        val clothingLevelOptions = remember(currentLanguage) {
            ClothingLevel.entries.map { clothingLevel ->
                when (clothingLevel) {
                    ClothingLevel.NUDE -> context.getString(R.string.clothing_nude)
                    ClothingLevel.MINIMAL -> context.getString(R.string.clothing_minimal)
                    ClothingLevel.LIGHT -> context.getString(R.string.clothing_light)
                    ClothingLevel.MODERATE -> context.getString(R.string.clothing_moderate)
                    ClothingLevel.HEAVY -> context.getString(R.string.clothing_heavy)
                }
            }
        }
        OptionsSelectorDialog(
            title = stringResource(R.string.select_clothing_level),
            options = ClothingLevel.entries,
            selectedOption = selectedClothingLevel,
            onOptionSelected = { newClothingLevel -> selectedClothingLevel = newClothingLevel },
            onDismissRequest = { showClothingLevelDialog = false },
            optionText = { clothingLevel -> clothingLevelOptions[clothingLevel.ordinal] }
        )
    }
}

private fun formatTime(timeString: String): String {
    val parser = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
    val formatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())
    return try {
        formatter.format(LocalDateTime.parse(timeString, parser))
    } catch (e: Exception) {
        e.printStackTrace()
        "N/A"
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewHomeScreen() {
    SundayAndroidAppTheme {
        HomeScreen(currentLanguage = "en", onLanguageChange = {})
    }
}