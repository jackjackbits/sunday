package com.gmolate.sunday.model

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserPreferencesDao {
    @Query("SELECT * FROM user_preferences WHERE id = 1")
    fun getPreferences(): Flow<UserPreferences?>

    @Query("SELECT * FROM user_preferences WHERE id = 1")
    suspend fun getPreferencesOnce(): UserPreferences?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreferences(preferences: UserPreferences)

    @Update
    suspend fun updatePreferences(preferences: UserPreferences)

    @Query("UPDATE user_preferences SET skinType = :skinType WHERE id = 1")
    suspend fun updateSkinType(skinType: Int)

    @Query("UPDATE user_preferences SET dailyVitaminDGoal = :goal WHERE id = 1")
    suspend fun updateVitaminDGoal(goal: Double)

    @Query("UPDATE user_preferences SET allowNotifications = :allow WHERE id = 1")
    suspend fun updateNotificationSetting(allow: Boolean)

    @Query("UPDATE user_preferences SET dailySunExposure = :exposure WHERE id = 1")
    suspend fun updateDailySunExposure(exposure: Int)

    @Query("UPDATE user_preferences SET hasOnboardingCompleted = :completed WHERE id = 1")
    suspend fun updateOnboardingCompleted(completed: Boolean)

    @Query("UPDATE user_preferences SET lastLocation = :location, lastLocationLat = :lat, lastLocationLon = :lon WHERE id = 1")
    suspend fun updateLastLocation(location: String?, lat: Double?, lon: Double?)
}
