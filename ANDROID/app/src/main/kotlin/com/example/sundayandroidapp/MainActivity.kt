package com.example.sundayandroidapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.sundayandroidapp.screens.HomeScreen
import com.example.sundayandroidapp.ui.theme.SundayAndroidAppTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var currentLanguage by remember { mutableStateOf(Locale.getDefault().language) }
            SundayAndroidAppTheme {
                HomeScreen(
                    currentLanguage = currentLanguage,
                    onLanguageChange = { newLanguage ->
                        currentLanguage = newLanguage
                        val configuration = resources.configuration
                        configuration.setLocale(Locale(newLanguage))
                        resources.updateConfiguration(configuration, resources.displayMetrics)
                    }
                )
            }
        }
    }
}

