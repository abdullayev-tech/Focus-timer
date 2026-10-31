package com.example.data

import kotlinx.coroutines.flow.Flow

class TimerRepository(private val timerDao: TimerDao) {
    val allTimers: Flow<List<TimerEntity>> = timerDao.getAllTimers()

    suspend fun insert(timer: TimerEntity): Long {
        return timerDao.insertTimer(timer)
    }

    suspend fun update(timer: TimerEntity) {
        timerDao.updateTimer(timer)
    }

    suspend fun delete(timer: TimerEntity) {
        timerDao.deleteTimer(timer)
    }

    suspend fun deleteById(id: Long) {
        timerDao.deleteById(id)
    }
}
