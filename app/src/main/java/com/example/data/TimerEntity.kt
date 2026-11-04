package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "timers")
data class TimerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val label: String,
    val totalSeconds: Long,
    val displayOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    // This timer's own alert. In each of these, null uses the default from settings
    val soundName: String? = null,
    val fadeInSeconds: Int? = null,
    val autoStopMinutes: Int? = null, // 0 means never
    val vibrationName: String? = null // A vibration's name, or VIBRATION_OFF_NAME
) {
    // A one-time timer is never saved, so it has a negative id no saved timer can have
    val isOneTime: Boolean get() = id < 0L

    fun formattedDuration(): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
}
