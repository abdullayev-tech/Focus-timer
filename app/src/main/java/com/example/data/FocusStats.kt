package com.example.data

import java.util.Calendar
import java.util.TimeZone

data class DayFocus(val dayStart: Long, val focusedMillis: Long)

data class TimerFocus(val label: String, val focusedMillis: Long)

data class FocusStats(
    val days: List<DayFocus>, // Oldest first; the last one is today
    val finishedToday: Int,
    val focusedMillis: Long,
    val started: Int,
    val finished: Int,
    val endedEarly: Int,
    val pauses: Int,
    val leftRunning: Int,
    val typicalSilenceMillis: Long?, // How long the alert usually sounds before it's silenced
    val timers: List<TimerFocus> // Most focused first
) {
    val focusedToday: Long get() = days.last().focusedMillis
}

/** Where each of the last seven days began, oldest first. Calendar steps over clock changes. */
fun lastSevenDayStarts(now: Long, zone: TimeZone = TimeZone.getDefault()): List<Long> {
    val day = Calendar.getInstance(zone).apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return List(7) {
        day.timeInMillis.also { day.add(Calendar.DAY_OF_MONTH, -1) }
    }.reversed()
}

/** Reads the log. A stretch of running counts on the day it ended; time past zero doesn't count. */
fun focusStats(events: List<TimerEvent>, dayStarts: List<Long>): FocusStats {
    val counted = events.filter { it.at >= dayStarts.first() }
    val ran = counted.filter { (it.ranMillis ?: 0L) > 0L }
    fun count(type: TimerEventType) = counted.count { it.type == type }

    return FocusStats(
        days = dayStarts.map { start ->
            val end = dayStarts.firstOrNull { it > start } ?: Long.MAX_VALUE
            DayFocus(start, ran.filter { it.at in start until end }.sumOf { it.ranMillis ?: 0L })
        },
        finishedToday = counted.count { it.type == TimerEventType.FINISHED && it.at >= dayStarts.last() },
        focusedMillis = ran.sumOf { it.ranMillis ?: 0L },
        started = count(TimerEventType.STARTED),
        finished = count(TimerEventType.FINISHED),
        endedEarly = counted.count { it.type == TimerEventType.CLOSED && it.wasCutShort() },
        pauses = count(TimerEventType.PAUSED),
        leftRunning = count(TimerEventType.LEFT),
        typicalSilenceMillis = counted
            .filter { it.type == TimerEventType.SILENCED }
            .mapNotNull { it.remainingMillis }
            .map { -it }
            .median(),
        timers = ran
            .groupBy { it.label.orEmpty() }
            .map { (label, stretches) -> TimerFocus(label, stretches.sumOf { it.ranMillis ?: 0L }) }
            .sortedByDescending { it.focusedMillis }
    )
}

// Closed with time still on a clock that had moved
private fun TimerEvent.wasCutShort(): Boolean {
    val remaining = remainingMillis ?: return false
    val total = (totalSeconds ?: return false) * 1000L
    return remaining in 1 until total
}

private fun List<Long>.median(): Long? {
    if (isEmpty()) return null
    return sorted()[size / 2]
}

/** "1h 25m", "45m", or "40s" under a minute. */
fun formatFocus(millis: Long): String {
    val seconds = millis / 1000L
    val hours = seconds / 3600L
    val minutes = (seconds % 3600L) / 60L
    return when {
        hours > 0L -> "${hours}h ${minutes}m"
        minutes > 0L -> "${minutes}m"
        seconds > 0L -> "${seconds}s"
        else -> "0m"
    }
}
