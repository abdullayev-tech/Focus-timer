package com.example.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// Saved by name in the log: rename one and old rows can't be read back
enum class TimerEventType(val title: String) {
    CREATED("Created"),
    OPENED("Opened"),
    STARTED("Started"),
    PAUSED("Paused"),
    RESUMED("Resumed"),
    LEFT("Left it running"),
    RETURNED("Came back"),
    FINISHED("Time up"),
    SILENCED("Silenced"),
    RESET("Reset"),
    CLOSED("Closed"),
    EDITED("Edited"),
    DELETED("Deleted"),
    REORDERED("Reordered timers"),
    ALERT_CHANGED("Changed alert")
}

/** One thing the person did, or the clock did for them. Never leaves the phone. */
@Entity(tableName = "timer_events", indices = [Index("at")])
data class TimerEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: TimerEventType,
    val at: Long = System.currentTimeMillis(),
    // Kept as they were: the timer may be one-time, or deleted later
    val timerId: Long? = null,
    val label: String? = null,
    val totalSeconds: Long? = null,
    // What the clock said at that moment; below zero once time is up
    val remainingMillis: Long? = null,
    // Set when a stretch of running ends: how much of the timer it used
    val ranMillis: Long? = null
)

@Dao
interface TimerEventDao {
    @Insert
    suspend fun insert(event: TimerEvent)

    @Query("SELECT * FROM timer_events WHERE at >= :since ORDER BY at DESC, id DESC")
    fun eventsSince(since: Long): Flow<List<TimerEvent>>
}
