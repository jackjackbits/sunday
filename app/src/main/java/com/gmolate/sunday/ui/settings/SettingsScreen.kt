package com.gmolate.sunday.ui.settings

import android.app.TimePickerDialog
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gmolate.sunday.PreferencesManager
import com.gmolate.sunday.Quote
import com.gmolate.sunday.service.DailyQuoteNotificationService
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    val notificationService = remember { DailyQuoteNotificationService(context) }
    val coroutineScope = rememberCoroutineScope()

    // Estados para las preferencias
    var notificationsEnabled by remember { mutableStateOf(false) }
    var notificationHour by remember { mutableStateOf(9) }
    var notificationMinute by remember { mutableStateOf(0) }
    var darkModePreference by remember { mutableStateOf<Boolean?>(null) }

    // Cargar preferencias al iniciar el Composable
    LaunchedEffect(Unit) {
        notificationsEnabled = preferencesManager.areNotificationsEnabled()
        notificationHour = preferencesManager.getNotificationHour()
        notificationMinute = preferencesManager.getNotificationMinute()
        darkModePreference = preferencesManager.getDarkModePreference()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        // Icono de flecha hacia atrás, puedes cambiarlo si quieres
                        // Iconos de Material Design: Icons.Filled.ArrowBack
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Opción para Notificaciones
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Notificaciones Diarias", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = {
                        notificationsEnabled = it
                        coroutineScope.launch {
                            preferencesManager.setNotificationsEnabled(it)
                            if (it) {
                                // Re-programar notificación si se activa
                                val dummyQuote = Quote("¡Hora de la inspiración!", "Goose") // TODO: Cargar cita real
                                notificationService.scheduleDailyQuoteNotification(notificationHour, notificationMinute, dummyQuote)
                            } else {
                                // Cancelar notificación si se desactiva
                                notificationService.cancelDailyQuoteNotification()
                            }
                            Toast.makeText(context, if (it) "Notificaciones activadas" else "Notificaciones desactivadas", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            // Opción para la hora de la notificación
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable(enabled = notificationsEnabled) {
                        val calendar = Calendar.getInstance()
                        val hour = calendar.get(Calendar.HOUR_OF_DAY)
                        val minute = calendar.get(Calendar.MINUTE)

                        TimePickerDialog(
                            context,
                            { _, selectedHour, selectedMinute ->
                                notificationHour = selectedHour
                                notificationMinute = selectedMinute
                                coroutineScope.launch {
                                    preferencesManager.setNotificationTime(selectedHour, selectedMinute)
                                    // Reprogramar la notificación con la nueva hora
                                    val dummyQuote = Quote("¡Hora de la inspiración!", "Goose") // TODO: Cargar cita real
                                    notificationService.scheduleDailyQuoteNotification(selectedHour, selectedMinute, dummyQuote)
                                    Toast.makeText(context, "Notificación programada para las $selectedHour:${String.format("%02d", selectedMinute)}", Toast.LENGTH_SHORT).show()
                                }
                            }, notificationHour, notificationMinute, true // Usar la hora actual de la preferencia
                        ).show()
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Hora de la Notificación", style = MaterialTheme.typography.bodyLarge)
                Text("${String.format("%02d", notificationHour)}:${String.format("%02d", notificationMinute)}",
                    style = MaterialTheme.typography.bodyMedium)
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Opción para el Modo Oscuro
            Text("Tema de la Aplicación", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // Opción para Tema del Sistema
                FilterChip(
                    selected = darkModePreference == null,
                    onClick = {
                        coroutineScope.launch {
                            darkModePreference = null
                            preferencesManager.setDarkModePreference(null)
                            Toast.makeText(context, "Tema: Sistema", Toast.LENGTH_SHORT).show()
                            // TODO: Recrear la actividad o manejar el tema de forma dinámica en MainActivity
                        }
                    },
                    label = { Text("Sistema") }
                )
                // Opción para Tema Claro
                FilterChip(
                    selected = darkModePreference == false,
                    onClick = {
                        coroutineScope.launch {
                            darkModePreference = false
                            preferencesManager.setDarkModePreference(false)
                            Toast.makeText(context, "Tema: Claro", Toast.LENGTH_SHORT).show()
                            // TODO: Recrear la actividad o manejar el tema de forma dinámica en MainActivity
                        }
                    },
                    label = { Text("Claro") }
                )
                // Opción para Tema Oscuro
                FilterChip(
                    selected = darkModePreference == true,
                    onClick = {
                        coroutineScope.launch {
                            darkModePreference = true
                            preferencesManager.setDarkModePreference(true)
                            Toast.makeText(context, "Tema: Oscuro", Toast.LENGTH_SHORT).show()
                            // TODO: Recrear la actividad o manejar el tema de forma dinámica en MainActivity
                        }
                    },
                    label = { Text("Oscuro") }
                )
            }
        }
    }
}
