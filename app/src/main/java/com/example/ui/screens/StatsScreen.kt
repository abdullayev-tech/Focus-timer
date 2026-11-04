package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DayFocus
import com.example.data.FocusStats
import com.example.data.TimerEvent
import com.example.data.focusStats
import com.example.data.formatFocus
import com.example.data.lastSevenDayStarts
import com.example.ui.theme.AppleTimerOrange
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.OLEDBlack
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// A long log is read on its way down, not all at once
private const val ACTIVITY_ROWS = 50

private val CHART_HEIGHT = 96.dp

/** The log, read back. Nothing here asks for anything: no streaks, no goals, no score. */
@Composable
fun StatsScreen(
    events: List<TimerEvent>?, // Null until the log has been read
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val dayStarts = remember(events) { lastSevenDayStarts(System.currentTimeMillis()) }
    val stats = remember(events) { focusStats(events.orEmpty(), dayStarts) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OLEDBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant)
                    .testTag("stats_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = "Stats",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        when {
            events == null -> Unit
            events.isEmpty() -> EmptyStats()
            else -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
            ) {
                TodayHero(stats)
                Spacer(modifier = Modifier.height(24.dp))
                WeekChart(stats.days)
                WeekNumbers(stats)
                TimerNumbers(stats)
                Activity(events, today = dayStarts.last())
            }
        }
    }
}

@Composable
private fun EmptyStats() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "Nothing yet",
                color = TextSecondary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Run a timer and it shows here",
                color = TextMuted,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun TodayHero(stats: FocusStats) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Focused today",
            color = TextSecondary,
            fontSize = 13.sp
        )
        Text(
            text = formatFocus(stats.focusedToday),
            color = TextPrimary,
            fontSize = 56.sp,
            fontWeight = FontWeight.Light,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.testTag("stats_focused_today")
        )
        Text(
            text = if (stats.finishedToday == 1) "1 session finished" else "${stats.finishedToday} sessions finished",
            color = TextSecondary,
            fontSize = 14.sp
        )
    }
}

/** Focused time per day. Today is the bright bar; only today and the best day carry a number. */
@Composable
private fun WeekChart(days: List<DayFocus>) {
    val most = days.maxOf { it.focusedMillis }
    val dayName = remember { SimpleDateFormat("EEE", Locale.getDefault()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("stats_week_chart"),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        days.forEachIndexed { index, day ->
            val isToday = index == days.lastIndex
            val name = dayName.format(Date(day.dayStart))
            val hasLabel = day.focusedMillis > 0L && (isToday || day.focusedMillis == most)
            Column(
                modifier = Modifier
                    .weight(1f)
                    // No hover on a phone, so a screen reader gets every day's number
                    .clearAndSetSemantics {
                        contentDescription = "$name, ${formatFocus(day.focusedMillis)}"
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (hasLabel) formatFocus(day.focusedMillis) else "",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(CHART_HEIGHT),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    if (day.focusedMillis > 0L) {
                        Box(
                            modifier = Modifier
                                .widthIn(max = 28.dp)
                                .fillMaxWidth()
                                // A very short day still shows as more than a line
                                .fillMaxHeight((day.focusedMillis.toFloat() / most).coerceAtLeast(0.04f))
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(if (isToday) AppleTimerOrange else TextMuted)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(DarkBorder)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = name,
                    color = if (isToday) TextPrimary else TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun WeekNumbers(stats: FocusStats) {
    SettingsGroup(title = "Last 7 days", footer = "Never uploaded. Kept on this phone.") {
        StatRow(title = "Focused", value = formatFocus(stats.focusedMillis))
        RowDivider()
        StatRow(title = "Finished", value = "${stats.finished} of ${stats.started}")
        RowDivider()
        StatRow(title = "Ended early", value = stats.endedEarly.toString())
        RowDivider()
        StatRow(title = "Pauses", value = stats.pauses.toString())
        RowDivider()
        StatRow(title = "Left it running", value = stats.leftRunning.toString())
        stats.typicalSilenceMillis?.let {
            RowDivider()
            StatRow(title = "Usually silenced in", value = formatFocus(it.coerceAtLeast(1000L)))
        }
    }
}

@Composable
private fun TimerNumbers(stats: FocusStats) {
    if (stats.timers.isEmpty()) return

    SettingsGroup(title = "Where it went") {
        stats.timers.take(5).forEachIndexed { index, timer ->
            if (index > 0) RowDivider()
            StatRow(title = timer.label, value = formatFocus(timer.focusedMillis))
        }
    }
}

/** Every action, newest first, one group a day. */
@Composable
private fun Activity(events: List<TimerEvent>, today: Long) {
    val dayFormat = remember { SimpleDateFormat("EEE, d MMM", Locale.getDefault()) }
    val timeFormat = remember { DateFormat.getTimeInstance(DateFormat.SHORT) }

    events.take(ACTIVITY_ROWS)
        .groupBy { dayFormat.format(Date(it.at)) }
        .forEach { (dayName, dayEvents) ->
            val isToday = dayEvents.first().at >= today
            SettingsGroup(title = if (isToday) "Today" else dayName) {
                dayEvents.forEachIndexed { index, event ->
                    if (index > 0) RowDivider()
                    ActivityRow(event, time = timeFormat.format(Date(event.at)))
                }
            }
        }
}

@Composable
private fun ActivityRow(event: TimerEvent, time: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = time,
            color = TextSecondary,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 1
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = event.type.title,
            color = TextPrimary,
            fontSize = 14.sp,
            maxLines = 1
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = event.label.orEmpty(),
            color = TextSecondary,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatRow(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            color = TextSecondary,
            fontSize = 14.sp,
            maxLines = 1
        )
    }
}
