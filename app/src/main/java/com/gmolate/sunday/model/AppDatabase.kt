package com.gmolate.sunday.model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        CachedUVData::class,
        CachedMoonData::class,
        UserPreferences::class,
        VitaminDSession::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cachedUVDataDao(): CachedUVDataDao
    abstract fun cachedMoonDataDao(): CachedMoonDataDao
    abstract fun userPreferencesDao(): UserPreferencesDao
    abstract fun vitaminDSessionDao(): VitaminDSessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sunday_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
