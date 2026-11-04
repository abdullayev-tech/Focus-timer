package com.example

import com.example.data.TimerEvent
import com.example.data.TimerEventType
import com.example.data.focusStats
import com.example.data.formatFocus
import com.example.data.lastSevenDayStarts
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FocusStatsTest {
    private val day = 24 * 3600 * 1000L
    private val utc = TimeZone.getTimeZone("UTC")
    private val today = 20_000 * day
    private val days = lastSevenDayStarts(today + 3600_000L, utc)

    private fun event(
        type: TimerEventType,
        at: Long = today + 1000L,
        label: String = "Focus",
        total: Long = 600L,
        remaining: Long? = null,
        ran: Long? = null
    ) = TimerEvent(
        type = type,
        at = at,
        label = label,
        totalSeconds = total,
        remainingMillis = remaining,
        ranMillis = ran
    )

    @Test
    fun sevenDays_endWithToday() {
        assertEquals(7, days.size)
        assertEquals(today, days.last())
        assertEquals(today - 6 * day, days.first())
    }

    @Test
    fun sevenDays_stepOverAClockChange() {
        // Clocks go forward in New York on 8 March 2026: that day has 23 hours
        val newYork = TimeZone.getTimeZone("America/New_York")
        val starts = lastSevenDayStarts(1773000000000L + 12 * 3600_000L, newYork)
        starts.forEach { start ->
            val hour = Calendar.getInstance(newYork).apply { timeInMillis = start }.get(Calendar.HOUR_OF_DAY)
            assertEquals(0, hour)
        }
    }

    @Test
    fun focusedTime_addsStretchesOnTheDayTheyEnded() {
        val stats = focusStats(
            listOf(
                event(TimerEventType.PAUSED, ran = 60_000L),
                event(TimerEventType.FINISHED, ran = 540_000L),
                event(TimerEventType.FINISHED, at = today - day + 5L, ran = 120_000L)
            ),
            days
        )

        assertEquals(600_000L, stats.focusedToday)
        assertEquals(120_000L, stats.days[5].focusedMillis)
        assertEquals(720_000L, stats.focusedMillis)
        assertEquals(1, stats.finishedToday)
        assertEquals(2, stats.finished)
    }

    @Test
    fun oldEvents_areLeftOut() {
        val stats = focusStats(
            listOf(event(TimerEventType.FINISHED, at = today - 7 * day, ran = 60_000L)),
            days
        )

        assertEquals(0L, stats.focusedMillis)
        assertEquals(0, stats.finished)
    }

    @Test
    fun endedEarly_isOnlyAClockThatMovedAndStoppedShort() {
        val stats = focusStats(
            listOf(
                event(TimerEventType.CLOSED, remaining = 300_000L), // Gave up halfway
                event(TimerEventType.CLOSED, remaining = 600_000L), // Opened, never started
                event(TimerEventType.CLOSED, remaining = -4_000L) // Finished, then left
            ),
            days
        )

        assertEquals(1, stats.endedEarly)
    }

    @Test
    fun typicalSilence_isTheMiddleOne() {
        val stats = focusStats(
            listOf(
                event(TimerEventType.SILENCED, remaining = -2_000L),
                event(TimerEventType.SILENCED, remaining = -9_000L),
                event(TimerEventType.SILENCED, remaining = -90_000L)
            ),
            days
        )

        assertEquals(9_000L, stats.typicalSilenceMillis)
        assertNull(focusStats(emptyList(), days).typicalSilenceMillis)
    }

    @Test
    fun timers_addUpByLabel_mostFocusedFirst() {
        val stats = focusStats(
            listOf(
                event(TimerEventType.FINISHED, label = "Read", ran = 60_000L),
                event(TimerEventType.PAUSED, label = "Write", ran = 50_000L),
                event(TimerEventType.FINISHED, label = "Write", ran = 50_000L)
            ),
            days
        )

        assertEquals(listOf("Write", "Read"), stats.timers.map { it.label })
        assertEquals(100_000L, stats.timers.first().focusedMillis)
    }

    @Test
    fun format_readsLikeAPersonWouldSayIt() {
        assertEquals("0m", formatFocus(0L))
        assertEquals("40s", formatFocus(40_000L))
        assertEquals("45m", formatFocus(45 * 60_000L))
        assertEquals("1h 25m", formatFocus(85 * 60_000L))
    }
}
