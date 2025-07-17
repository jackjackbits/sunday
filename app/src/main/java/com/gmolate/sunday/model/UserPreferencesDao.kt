package com.gmolate.sunday.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import java.util.Date

@Dao
interface UserPreferencesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(userPreferences: UserPreferences)

    @Query("SELECT * FROM user_preferences ORDER BY id DESC LIMIT 1")
    suspend fun getLatest(): UserPreferences?

    @Query("UPDATE user_preferences SET skinType = :skinType, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSkinType(id: Int, skinType: SkinType, updatedAt: Date = Date())

    @Query("UPDATE user_preferences SET clothingLevel = :clothingLevel, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateClothingLevel(id: Int, clothingLevel: ClothingLevel, updatedAt: Date = Date())

    @Query("UPDATE user_preferences SET lastLocation = :location, lastLocationLat = :lat, lastLocationLon = :lon, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateLastLocation(id: Int, location: String?, lat: Double?, lon: Double?, updatedAt: Date = Date())

    @Query("UPDATE user_preferences SET dailyVitaminDGoal = :goal, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateDailyGoal(id: Int, goal: Int, updatedAt: Date = Date())

    @Query("UPDATE user_preferences SET enableNotifications = :enable, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateNotifications(id: Int, enable: Boolean, updatedAt: Date = Date())

    @Query("UPDATE user_preferences SET enableSolarNoonNotifications = :enable, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSolarNoonNotifications(id: Int, enable: Boolean, updatedAt: Date = Date())

    @Query("UPDATE user_preferences SET solarNoonNotificationMinutesBefore = :minutes, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSolarNoonTiming(id: Int, minutes: Int, updatedAt: Date = Date())

    @Query("UPDATE user_preferences SET darkMode = :darkMode, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateDarkMode(id: Int, darkMode: Boolean, updatedAt: Date = Date())

    @Query("DELETE FROM user_preferences")
    suspend fun deleteAll()
}
