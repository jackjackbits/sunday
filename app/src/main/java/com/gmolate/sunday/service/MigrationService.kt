package com.gmolate.sunday.service

import android.content.Context
import android.content.SharedPreferences
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gmolate.sunday.model.AppDatabase
import com.gmolate.sunday.model.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MigrationService(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("sunday_prefs", Context.MODE_PRIVATE)
    private val db: AppDatabase = AppDatabase.getDatabase(context)

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Agregar nuevas columnas en user_preferences
                database.execSQL("ALTER TABLE user_preferences ADD COLUMN dailyVitaminDGoal INTEGER NOT NULL DEFAULT 1000")
                database.execSQL("ALTER TABLE user_preferences ADD COLUMN enableNotifications INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE user_preferences ADD COLUMN darkMode INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE user_preferences ADD COLUMN useMetricSystem INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE user_preferences ADD COLUMN lastLocation TEXT")
                database.execSQL("ALTER TABLE user_preferences ADD COLUMN lastLocationLat REAL")
                database.execSQL("ALTER TABLE user_preferences ADD COLUMN lastLocationLon REAL")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
ll                // Agregar tabla de sesiones de vitamina D
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS vitamin_d_session (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        startTime INTEGER NOT NULL,
                        endTime INTEGER,
                        durationMinutes INTEGER NOT NULL,
                        uvIndex REAL NOT NULL,
                        estimatedVitaminD REAL NOT NULL,
                        skinType INTEGER NOT NULL,
                        latitude REAL NOT NULL,
                        longitude REAL NOT NULL,
                        isCompleted INTEGER NOT NULL DEFAULT 0,
                        notes TEXT,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Agregar tabla de datos lunares
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS cached_moon_data (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        latitude REAL NOT NULL,
                        longitude REAL NOT NULL,
                        date INTEGER NOT NULL,
                        moonPhase REAL NOT NULL,
                        moonrise INTEGER,
                        moonset INTEGER,
                        illumination REAL NOT NULL,
                        distanceKm REAL NOT NULL,
                        lastUpdated INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }
    }

    suspend fun migrateDataIfNeeded() {
        withContext(Dispatchers.IO) {
            // Verificar si es la primera vez que se ejecuta la app
            val isFirstRun = prefs.getBoolean("is_first_run", true)

            if (isFirstRun) {
                // Crear preferencias por defecto
                createDefaultUserPreferences()

                // Marcar que ya no es la primera ejecución
                prefs.edit().putBoolean("is_first_run", false).apply()
            }

            // Migrar datos de SharedPreferences a Room si es necesario
            migrateSharedPreferencesToRoom()

            // Limpiar datos antiguos si es necesario
            cleanupOldData()
        }
    }

    private suspend fun createDefaultUserPreferences() {
        val existingPrefs = db.userPreferencesDao().getPreferencesOnce()
        if (existingPrefs == null) {
            val defaultPrefs = UserPreferences(
                id = 1,
                skinType = 2, // Tipo de piel por defecto
                userAge = 30,
                dailyVitaminDGoal = 1000.0,
                allowNotifications = true,
                enableSolarNoonNotifications = true,
                solarNoonNotificationMinutesBefore = 30,
                enableHighUVAlerts = true,
                darkMode = false,
                preferredUnits = "metric",
                safeExposureTime = 15,
                dailySunExposure = 0,
                weeklyGoal = 150,
                hasOnboardingCompleted = false,
                lastLocation = null,
                lastLocationLat = null,
                lastLocationLon = null,
                notificationThresholdUV = 6.0,
                createdAt = java.util.Date(),
                updatedAt = java.util.Date()
            )

            db.userPreferencesDao().insertPreferences(defaultPrefs)
        }
    }

    private suspend fun migrateSharedPreferencesToRoom() {
        // Migrar configuraciones existentes de SharedPreferences a Room
        val skinType = prefs.getInt("skin_type", 2)
        val vitaminDGoal = prefs.getFloat("vitamin_d_goal", 1000f).toDouble()
        val allowNotifications = prefs.getBoolean("allow_notifications", true)
        val darkMode = prefs.getBoolean("dark_mode", false)

        // Solo migrar si hay configuraciones existentes
        if (prefs.contains("skin_type") || prefs.contains("vitamin_d_goal")) {
            val existingPrefs = db.userPreferencesDao().getPreferencesOnce()
            if (existingPrefs != null) {
                val updatedPrefs = existingPrefs.copy(
                    skinType = skinType,
                    dailyVitaminDGoal = vitaminDGoal,
                    allowNotifications = allowNotifications,
                    darkMode = darkMode,
                    updatedAt = java.util.Date()
                )
                db.userPreferencesDao().updatePreferences(updatedPrefs)
            }

            // Limpiar SharedPreferences migradas
            prefs.edit()
                .remove("skin_type")
                .remove("vitamin_d_goal")
                .remove("allow_notifications")
                .remove("dark_mode")
                .apply()
        }
    }

    private suspend fun cleanupOldData() {
        val cutoffDate = java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.DAY_OF_YEAR, -30) // Mantener datos de últimos 30 días
        }.time

        // Limpiar datos UV antiguos
        db.cachedUVDataDao().deleteOldUvData(cutoffDate)

        // Limpiar datos lunares antiguos
        db.cachedMoonDataDao().deleteOldMoonData(cutoffDate)

        // Limpiar sesiones de vitamina D muy antiguas
        val oldCutoffDate = java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.DAY_OF_YEAR, -90) // Mantener sesiones de últimos 90 días
        }.time
        db.vitaminDSessionDao().deleteOldSessions(oldCutoffDate)
    }

    fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    fun isFirstInstall(): Boolean {
        return prefs.getBoolean("is_first_run", true)
    }
}
