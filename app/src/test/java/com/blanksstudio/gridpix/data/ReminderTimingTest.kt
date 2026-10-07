package com.blanksstudio.gridpix.data

import com.blanksstudio.gridpix.notifications.DailyReminder
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderTimingTest {

    private val zone = ZoneId.of("Africa/Johannesburg")
    private fun at(h: Int, m: Int) = ZonedDateTime.of(2026, 10, 7, h, m, 0, 0, zone)

    @Test
    fun `reminder later today`() {
        assertEquals(Duration.ofHours(8).plusMinutes(30), DailyReminder.delayUntil(18, at(9, 30)))
    }

    @Test
    fun `reminder hour already passed goes to tomorrow`() {
        assertEquals(Duration.ofHours(23), DailyReminder.delayUntil(18, at(19, 0)))
        assertEquals(Duration.ofHours(24), DailyReminder.delayUntil(18, at(18, 0)))
    }
}
