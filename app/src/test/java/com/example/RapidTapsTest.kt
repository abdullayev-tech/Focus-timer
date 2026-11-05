package com.example

import com.example.ui.screens.RapidTaps
import org.junit.Assert.assertEquals
import org.junit.Test

class RapidTapsTest {

  private fun completedAt(vararg times: Long): List<Long> {
    val taps = RapidTaps(count = 3, maxGapMillis = 600)
    return times.filter { taps.tap(it) }
  }

  @Test
  fun `three quick taps complete the run`() {
    assertEquals(listOf(1400L), completedAt(1000, 1200, 1400))
  }

  @Test
  fun `one or two taps do nothing`() {
    assertEquals(emptyList<Long>(), completedAt(1000))
    assertEquals(emptyList<Long>(), completedAt(1000, 1200))
  }

  @Test
  fun `a slow tap starts the run over`() {
    assertEquals(emptyList<Long>(), completedAt(1000, 1200, 2000, 2200))
    assertEquals(listOf(2400L), completedAt(1000, 1200, 2000, 2200, 2400))
  }

  @Test
  fun `a fourth quick tap does not complete another run`() {
    assertEquals(listOf(1400L), completedAt(1000, 1200, 1400, 1600))
  }
}
