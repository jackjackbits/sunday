package com.gmolate.sunday.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import java.util.Date

@Dao
interface CachedMoonDataDao {
    @Query("SELECT * FROM cached_moon_data WHERE date = :date LIMIT 1")
    suspend fun getMoonDataForDate(date: String): CachedMoonData?

    @Query("SELECT * FROM cached_moon_data ORDER BY date DESC LIMIT 1")
    suspend fun getLatestMoonData(): CachedMoonData?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoonData(moonData: CachedMoonData)

    @Query("DELETE FROM cached_moon_data WHERE date < :cutoffDate")
    suspend fun deleteOldMoonData(cutoffDate: String)
}
