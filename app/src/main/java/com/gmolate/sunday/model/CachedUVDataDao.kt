package com.gmolate.sunday.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import java.util.Date

@Dao
interface CachedUVDataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cachedUVData: CachedUVData)

    @Query("SELECT * FROM cached_uv_data WHERE latitude BETWEEN :minLat AND :maxLat AND longitude BETWEEN :minLon AND :maxLon AND date >= :startDate AND date <= :endDate")
    suspend fun getCachedData(minLat: Double, maxLat: Double, minLon: Double, maxLon: Double, startDate: Date, endDate: Date): List<CachedUVData>
}
