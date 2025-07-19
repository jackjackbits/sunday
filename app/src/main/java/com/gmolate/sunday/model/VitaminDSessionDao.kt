package com.gmolate.sunday.model

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface VitaminDSessionDao {
    @Query("SELECT * FROM vitamin_d_session ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<VitaminDSession>>

    @Query("SELECT * FROM vitamin_d_session WHERE DATE(startTime/1000, 'unixepoch') = DATE(:date/1000, 'unixepoch')")
    suspend fun getSessionsForDate(date: Date): List<VitaminDSession>

    @Query("SELECT * FROM vitamin_d_session WHERE startTime >= :startDate AND startTime <= :endDate")
    fun getSessionsInRange(startDate: Date, endDate: Date): Flow<List<VitaminDSession>>

    @Query("SELECT SUM(estimatedVitaminD) FROM vitamin_d_session WHERE DATE(startTime/1000, 'unixepoch') = DATE(:date/1000, 'unixepoch')")
    suspend fun getTotalVitaminDForDate(date: Date): Double?

    @Query("SELECT SUM(durationMinutes) FROM vitamin_d_session WHERE DATE(startTime/1000, 'unixepoch') = DATE(:date/1000, 'unixepoch')")
    suspend fun getTotalExposureTimeForDate(date: Date): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: VitaminDSession): Long

    @Update
    suspend fun updateSession(session: VitaminDSession)

    @Delete
    suspend fun deleteSession(session: VitaminDSession)

    @Query("DELETE FROM vitamin_d_session WHERE createdAt < :cutoffDate")
    suspend fun deleteOldSessions(cutoffDate: Date)

    @Query("SELECT * FROM vitamin_d_session WHERE isCompleted = 0 ORDER BY startTime DESC LIMIT 1")
    suspend fun getActiveSession(): VitaminDSession?
}
