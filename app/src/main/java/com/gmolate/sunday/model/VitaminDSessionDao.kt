package com.gmolate.sunday.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface VitaminDSessionDao {
    @Insert
    suspend fun insert(session: VitaminDSession)

    @Query("SELECT * FROM vitamin_d_session WHERE startTime >= :since")
    suspend fun getSessions(since: Long): List<VitaminDSession>
}
