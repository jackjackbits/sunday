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
                // Crear tabla para datos de luna cachados
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS cached_moon_data (
                        date TEXT PRIMARY KEY NOT NULL,
                        phaseName TEXT NOT NULL,
                        phaseIcon TEXT NOT NULL,
                        age REAL NOT NULL,
                        fraction REAL NOT NULL,
                        lastUpdated INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Agregar nuevas columnas para notificaciones de mediodía solar
                database.execSQL("ALTER TABLE user_preferences ADD COLUMN enableSolarNoonNotifications INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE user_preferences ADD COLUMN solarNoonNotificationMinutesBefore INTEGER NOT NULL DEFAULT 30")
            }
        }
    }

    suspend fun migrateDataIfNeeded() = withContext(Dispatchers.IO) {
        val currentVersion = prefs.getInt("data_version", 0)
        val targetVersion = 4 // Incrementar cuando se agreguen nuevas migraciones

        if (currentVersion < targetVersion) {
            when (currentVersion) {
                0 -> migrateToVersion1()
                1 -> migrateToVersion2()
                2 -> migrateToVersion3()
                3 -> migrateToVersion4()
            }
            prefs.edit().putInt("data_version", targetVersion).apply()
        }
    }

    private suspend fun migrateToVersion1() {
        // Migrar datos antiguos si es necesario
        val userPreferences = db.userPreferencesDao().getLatest()
        if (userPreferences == null) {
            // Crear preferencias por defecto si no existen
            db.userPreferencesDao().insert(UserPreferences())
        }
    }

    private suspend fun migrateToVersion2() {
        // Migrar a la versión 2 que incluye las nuevas preferencias
        val userPreferences = db.userPreferencesDao().getLatest()
        if (userPreferences != null) {
            // Actualizar preferencias existentes con nuevos valores por defecto
            db.userPreferencesDao().insert(userPreferences.copy(
                dailyVitaminDGoal = 1000,
                enableNotifications = true,
                darkMode = false,
                useMetricSystem = true
            ))
        }
    }

    private suspend fun migrateToVersion3() {
        // Migración para la versión 3 que incluye datos de luna
        // No hay datos que migrar ya que es una tabla nueva
    }

    private suspend fun migrateToVersion4() {
        // Migración para la versión 4 que incluye notificaciones de mediodía solar
        // Los valores por defecto ya están establecidos en la migración SQL
    }
}
