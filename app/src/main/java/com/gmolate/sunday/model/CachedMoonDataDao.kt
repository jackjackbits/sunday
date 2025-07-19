package com.gmolate.sunday.model

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface CachedMoonDataDao {
    @Query("SELECT * FROM cached_moon_data WHERE date = :date AND latitude = :lat AND longitude = :lon")
    suspend fun getMoonDataForDateAndLocation(date: Date, lat: Double, lon: Double): CachedMoonData?

    @Query("SELECT * FROM cached_moon_data ORDER BY date DESC LIMIT 1")
    suspend fun getLatestMoonData(): CachedMoonData?

    @Query("SELECT * FROM cached_moon_data WHERE date >= :startDate AND date <= :endDate")
    fun getMoonDataInRange(startDate: Date, endDate: Date): Flow<List<CachedMoonData>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoonData(moonData: CachedMoonData)

    @Delete
    suspend fun deleteMoonData(moonData: CachedMoonData)

    @Query("DELETE FROM cached_moon_data WHERE lastUpdated < :cutoffDate")
    suspend fun deleteOldMoonData(cutoffDate: Date)

    @Query("DELETE FROM cached_moon_data")
    suspend fun deleteAllMoonData()
}
