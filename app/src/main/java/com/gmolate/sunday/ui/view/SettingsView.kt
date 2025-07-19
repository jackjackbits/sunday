package com.gmolate.sunday.ui.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gmolate.sunday.data.UserPreferencesRepository
import kotlinx.coroutines.launch

@Composable
fun SettingsView() {
    val context = LocalContext.current
    val userPreferencesRepository = UserPreferencesRepository(context)
    val theme = userPreferencesRepository.theme.collectAsState(initial = "system").value
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Theme", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("System")
            RadioButton(
                selected = theme == "system",
                onClick = {
                    coroutineScope.launch {
                        userPreferencesRepository.saveTheme("system")
                    }
                }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Light")
            RadioButton(
                selected = theme == "light",
                onClick = {
                    coroutineScope.launch {
                        userPreferencesRepository.saveTheme("light")
                    }
                }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Dark")
            RadioButton(
                selected = theme == "dark",
                onClick = {
                    coroutineScope.launch {
                        userPreferencesRepository.saveTheme("dark")
                    }
                }
            )
        }
    }
}
