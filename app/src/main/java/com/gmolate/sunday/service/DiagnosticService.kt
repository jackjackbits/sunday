package com.gmolate.sunday.service

import android.content.Context
 import com.gmolate.sunday.model.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DiagnosticService(private val context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val uvService = UVService(db, NotificationService(context))
    private val moonPhaseService = MoonPhaseService(context, db)
    private val locationManager = LocationManager(context)

    suspend fun runDiagnostics(): String = withContext(Dispatchers.IO) {
        val report = StringBuilder()
        report.append("--- Sunday Diagnostic Report ---\n")
        report.append("Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n\n")

        // 1. Location Check
        report.append("--- Location ---\n")
        try {
            val location = locationManager.getLastKnownLocation()
            if (location != null) {
                report.append("Last known location: ${location.latitude}, ${location.longitude}\n")
            } else {
                report.append("Location not available.\n")
            }
        } catch (e: Exception) {
            report.append("Error getting location: ${e.message}\n")
        }

        // 2. UV Data Check
        report.append("\n--- UV Data ---\n")
        try {
            val latestUv = db.cachedUVDataDao().getLatestUvData()
            if (latestUv != null) {
                report.append("Latest UV data from: ${latestUv.lastUpdated}\n")
                report.append("Max UV: ${latestUv.maxUV}\n")
            } else {
                report.append("No cached UV data found.\n")
            }
        } catch (e: Exception) {
            report.append("Error accessing UV data: ${e.message}\n")
        }

        // 3. Moon Phase Check
        report.append("\n--- Moon Phase ---\n")
        try {
            val latestMoon = db.cachedMoonDataDao().getLatestMoonData()
            if (latestMoon != null) {
                report.append("Latest moon data from: ${latestMoon.lastUpdated}\n")
                report.append("Phase: ${moonPhaseService.currentMoonPhase.value}\n")
            } else {
                report.append("No cached moon data found.\n")
            }
        } catch (e: Exception) {
            report.append("Error accessing moon data: ${e.message}\n")
        }

        // 4. User Preferences
        report.append("\n--- User Preferences ---\n")
        try {
            val prefs = db.userPreferencesDao().getPreferencesOnce()
            if (prefs != null) {
                report.append("Skin Type: ${prefs.skinType}\n")
                report.append("Vitamin D Goal: ${prefs.dailyVitaminDGoal}\n")
                report.append("Onboarding Completed: ${prefs.hasOnboardingCompleted}\n")
            } else {
                report.append("No user preferences found.\n")
            }
        } catch (e: Exception) {
            report.append("Error accessing user preferences: ${e.message}\n")
        }

        report.append("\n--- End of Report ---\n")
        report.toString()
    }
}
