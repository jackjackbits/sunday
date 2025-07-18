package com.gmolate.sunday.model

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface CachedUVDataDao {
    @Query("SELECT * FROM cached_uv_data WHERE date = :date AND latitude = :lat AND longitude = :lon")
    suspend fun getUvDataForDateAndLocation(date: Date, lat: Double, lon: Double): CachedUVData?

    @Query("SELECT * FROM cached_uv_data ORDER BY date DESC LIMIT 1")
    suspend fun getLatestUvData(): CachedUVData?

    @Query("SELECT * FROM cached_uv_data WHERE date >= :startDate AND date <= :endDate")
    fun getUvDataInRange(startDate: Date, endDate: Date): Flow<List<CachedUVData>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUvData(uvData: CachedUVData)

    @Delete
    suspend fun deleteUvData(uvData: CachedUVData)

    @Query("DELETE FROM cached_uv_data WHERE lastUpdated < :cutoffDate")
    suspend fun deleteOldUvData(cutoffDate: Date)

    @Query("DELETE FROM cached_uv_data")
    suspend fun deleteAllUvData()

    @Query("SELECT maxUV FROM cached_uv_data WHERE date = :date ORDER BY lastUpdated DESC LIMIT 1")
    suspend fun getMaxUvForDate(date: Date): Double?
}
