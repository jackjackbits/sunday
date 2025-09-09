package com.example.sundayandroidapp.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

object LocalizationManager {

    fun setLocale(context: Context, languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        context.resources.updateConfiguration(configuration, context.resources.displayMetrics)
    }
}
