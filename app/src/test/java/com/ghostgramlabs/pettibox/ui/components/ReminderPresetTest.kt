package com.ghostgramlabs.pettibox.ui.components

import java.util.Calendar
import java.util.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ReminderPresetTest {
    private lateinit var originalTimeZone: TimeZone

    @Before
    fun setUp() {
        originalTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalTimeZone)
    }

    @Test
    fun tonightUsesTodayWhenEveningHasNotPassed() {
        val now = utcMillis(year = 2026, month = Calendar.MAY, day = 20, hour = 12)

        val result = ReminderPreset.TONIGHT.toEpochMillis(now, eveningHour = 21)

        assertEquals(
            utcMillis(year = 2026, month = Calendar.MAY, day = 20, hour = 21),
            result
        )
    }

    @Test
    fun tonightRollsToTomorrowWhenEveningPassed() {
        val now = utcMillis(year = 2026, month = Calendar.MAY, day = 20, hour = 22)

        val result = ReminderPreset.TONIGHT.toEpochMillis(now, eveningHour = 21)

        assertEquals(
            utcMillis(year = 2026, month = Calendar.MAY, day = 21, hour = 21),
            result
        )
    }

    @Test
    fun tomorrowUsesConfiguredMorningTime() {
        val now = utcMillis(year = 2026, month = Calendar.MAY, day = 20, hour = 22)

        val result = ReminderPreset.TOMORROW.toEpochMillis(
            now = now,
            morningHour = 8,
            morningMinute = 30
        )

        assertEquals(
            utcMillis(year = 2026, month = Calendar.MAY, day = 21, hour = 8, minute = 30),
            result
        )
    }

    @Test
    fun customReturnsNullSoCallerCanOpenPicker() {
        assertEquals(null, ReminderPreset.CUSTOM.toEpochMillis())
    }

    private fun utcMillis(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int = 0
    ): Long = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(year, month, day, hour, minute, 0)
    }.timeInMillis
}
