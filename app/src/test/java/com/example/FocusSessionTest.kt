package com.example

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.TimerEntity
import com.example.session.ActiveTimerState
import com.example.session.FocusSession
import com.example.session.SessionNotice
import com.example.session.SessionStore
import com.example.session.isTooLateToAlert
import com.example.session.noticeFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FocusSessionTest {

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val timer = TimerEntity(id = 7, label = "Deep work", totalSeconds = 60)
  private var clock = 1_000_000L

  private fun newSession() = FocusSession(context, now = { clock })

  private fun nextAlarmAt(): Long? {
    val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    return shadowOf(alarms).peekNextScheduledAlarm()?.triggerAtMs
  }

  @Before
  fun clearSaved() {
    SessionStore(context).clear()
  }

  @Test
  fun `starting schedules the alarm for the end, and pausing takes it away`() {
    val session = newSession()
    session.openTimer(timer, autoStart = true)
    assertEquals(clock + 60_000L, nextAlarmAt())

    clock += 10_000L
    session.pauseTimer()
    assertNull(nextAlarmAt())
    assertEquals(50_000L, session.activeState.value.remainingMillis)

    session.togglePlayPause()
    assertEquals(clock + 50_000L, nextAlarmAt())
  }

  @Test
  fun `a running session survives the process, with the clock true`() {
    newSession().openTimer(timer, autoStart = true)
    clock += 20_000L

    val state = newSession().activeState.value
    assertTrue(state.isRunning)
    assertEquals("Deep work", state.timerEntity?.label)
    assertEquals(40_000L, state.remainingMillis)
    assertFalse(state.isCompleted)
  }

  @Test
  fun `a paused session survives the process, at the time it stopped`() {
    val session = newSession()
    session.openTimer(timer, autoStart = true)
    clock += 15_000L
    session.pauseTimer()
    clock += 500_000L

    val state = newSession().activeState.value
    assertFalse(state.isRunning)
    assertEquals(45_000L, state.remainingMillis)
    assertNull(nextAlarmAt())
  }

  @Test
  fun `a one-time timer survives the process too, with its own choices`() {
    val oneTime = TimerEntity(id = -5, label = "Call", totalSeconds = 30, soundName = "BELL", autoStopMinutes = 1)
    newSession().openTimer(oneTime, autoStart = true)

    val restored = newSession().activeState.value.timerEntity
    assertEquals(-5L, restored?.id)
    assertEquals("BELL", restored?.soundName)
    assertEquals(1, restored?.autoStopMinutes)
    assertNull(restored?.fadeInSeconds)
  }

  @Test
  fun `resetting leaves nothing behind`() {
    val session = newSession()
    session.openTimer(timer, autoStart = true)
    session.resetTimer()

    assertNull(nextAlarmAt())
    assertNull(SessionStore(context).load())
    assertFalse(newSession().isFullScreen.value)
  }

  @Test
  fun `opening another timer replaces the alarm`() {
    val session = newSession()
    session.openTimer(timer, autoStart = true)
    session.openTimer(TimerEntity(id = 8, label = "Reading", totalSeconds = 120), autoStart = true)

    assertEquals(clock + 120_000L, nextAlarmAt())
    assertEquals("Reading", session.activeState.value.timerEntity?.label)
  }

  @Test
  fun `an alarm that goes off early is set again and ends nothing`() {
    val session = newSession()
    session.openTimer(timer, autoStart = true)
    session.onTimeUp()

    assertFalse(session.activeState.value.isCompleted)
    assertNotNull(nextAlarmAt())
  }

  @Test
  fun `an alarm for a paused session does nothing`() {
    val session = newSession()
    session.openTimer(timer, autoStart = true)
    session.pauseTimer()
    clock += 120_000L
    session.onTimeUp()

    assertFalse(session.activeState.value.isCompleted)
    assertEquals(60_000L, session.activeState.value.remainingMillis)
  }

  @Test
  fun `the notification says what is going on`() {
    val running = ActiveTimerState(timer, 60, 1000, isRunning = true, endAtMillis = 5000)
    assertEquals(SessionNotice.Running("Deep work", 5000, timeInStatusBar = true), noticeFor(running, isAlerting = false))

    val done = running.copy(isCompleted = true)
    assertEquals(SessionNotice.Done("Deep work", timeInStatusBar = true), noticeFor(done, isAlerting = true))
    assertEquals(SessionNotice.None, noticeFor(done, isAlerting = false))

    assertEquals(SessionNotice.None, noticeFor(running.copy(isRunning = false, endAtMillis = null), false))
    assertEquals(SessionNotice.None, noticeFor(ActiveTimerState(), false))
  }

  @Test
  fun `the status bar shows the time only when asked`() {
    val running = ActiveTimerState(timer, 60, 1000, isRunning = true, endAtMillis = 5000)
    assertEquals(
      SessionNotice.Running("Deep work", 5000, timeInStatusBar = false),
      noticeFor(running, isAlerting = false, timeInStatusBar = false)
    )
    assertEquals(
      SessionNotice.Done("Deep work", timeInStatusBar = false),
      noticeFor(running.copy(isCompleted = true), isAlerting = true, timeInStatusBar = false)
    )
  }

  @Test
  fun `opening the app long after the end doesn't sound the alert`() {
    assertFalse(isTooLateToAlert(-50L))
    assertFalse(isTooLateToAlert(-59_999L))
    assertTrue(isTooLateToAlert(-60_000L))
  }
}
