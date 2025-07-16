package com.gmolate.sunday.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserPreferencesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(userPreferences: UserPreferences)

    @Query("SELECT * FROM user_preferences ORDER BY id DESC LIMIT 1")
    suspend fun getLatest(): UserPreferences?
}
