package com.gmolate.sunday.service

import android.content.Context
import android.util.Log
import com.gmolate.sunday.model.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Servicio de diagnóstico para verificar el estado de la aplicación
 * Solo para desarrollo y debugging
 */
class DiagnosticService(private val context: Context) {
    
    private val db = AppDatabase.getDatabase(context)
    private val tag = "SundayDiagnostic"
    
    fun runDiagnostics() {
        CoroutineScope(Dispatchers.IO).launch {
            Log.d(tag, "=== SUNDAY APP DIAGNOSTICS ===")
            
            // Verificar base de datos
            checkDatabase()
            
            // Verificar datos cacheados
            checkCachedData()
            
            // Verificar configuraciones
            checkUserPreferences()
            
            Log.d(tag, "=== DIAGNOSTICS COMPLETE ===")
        }
    }
    
    private suspend fun checkDatabase() {
        try {
            val userPrefs = db.userPreferencesDao().getLatest()
            Log.d(tag, "Database: ✅ Accessible")
            Log.d(tag, "User Preferences: ${if (userPrefs != null) "✅ Found" else "❌ Not found"}")
        } catch (e: Exception) {
            Log.e(tag, "Database: ❌ Error - ${e.message}")
        }
    }
    
    private suspend fun checkCachedData() {
        try {
            val uvData = db.cachedUVDataDao().getLatestData()
            val moonData = db.cachedMoonDataDao().getLatestMoonData()
            
            Log.d(tag, "UV Data: ${if (uvData != null) "✅ Available (${uvData.maxUV} UV)" else "❌ No data"}")
            Log.d(tag, "Moon Data: ${if (moonData != null) "✅ Available (${moonData.phaseName})" else "❌ No data"}")
        } catch (e: Exception) {
            Log.e(tag, "Cached Data: ❌ Error - ${e.message}")
        }
    }
    
    private suspend fun checkUserPreferences() {
        try {
            val prefs = db.userPreferencesDao().getLatest()
            if (prefs != null) {
                Log.d(tag, "Notifications: ${if (prefs.enableNotifications) "✅ Enabled" else "❌ Disabled"}")
                Log.d(tag, "Solar Noon: ${if (prefs.enableSolarNoonNotifications) "✅ Enabled" else "❌ Disabled"}")
                Log.d(tag, "Skin Type: ${prefs.skinType}")
                Log.d(tag, "Clothing: ${prefs.clothingLevel}")
            }
        } catch (e: Exception) {
            Log.e(tag, "User Preferences: ❌ Error - ${e.message}")
        }
    }
    
    fun getAppStatus(): String {
        return "Sunday App v2.0 - All systems operational"
    }
}
