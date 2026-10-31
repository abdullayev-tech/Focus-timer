package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TimerDao {
    @Query("SELECT * FROM timers ORDER BY displayOrder ASC, createdAt ASC")
    fun getAllTimers(): Flow<List<TimerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimer(timer: TimerEntity): Long

    @Update
    suspend fun updateTimer(timer: TimerEntity)

    @Update
    suspend fun updateTimers(timers: List<TimerEntity>)

    @Delete
    suspend fun deleteTimer(timer: TimerEntity)

    @Query("DELETE FROM timers WHERE id = :id")
    suspend fun deleteById(id: Long)
}
