package com.example.sundayandroidapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sundayandroidapp.R
import java.io.InputStream
import java.nio.charset.Charset

@Composable
fun MethodologyScreen(currentLanguage: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val resourceId = if (currentLanguage == "es") R.raw.methodology_es else R.raw.methodology_en

    val methodologyText = try {
        val inputStream: InputStream = context.resources.openRawResource(resourceId)
        inputStream.readBytes().toString(Charset.defaultCharset())
    } catch (e: Exception) {
        e.printStackTrace()
        "Error loading methodology."
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Methodology", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = methodologyText,
                modifier = Modifier.verticalScroll(rememberScrollState())
            )
        }
    }
}