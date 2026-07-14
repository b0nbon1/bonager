package com.bonvic.bonager.data

import org.junit.Assert.assertEquals
import org.junit.Test

class BonagerRepositoryTest {
    @Test
    fun streakIncludesTodayAndConsecutivePreviousDays() {
        val dates = listOf("2026-07-14", "2026-07-13", "2026-07-12", "2026-07-10")
        assertEquals(3, BonagerRepository.calculateStreak(dates, today = "2026-07-14"))
    }

    @Test
    fun streakCanContinueFromYesterdayWhenTodayIsUnchecked() {
        val dates = listOf("2026-07-13", "2026-07-12")
        assertEquals(2, BonagerRepository.calculateStreak(dates, today = "2026-07-14"))
    }

    @Test
    fun dateInputRoundTripsToLocalDisplay() {
        val stored = Dates.inputToStorage("2026-07-14 09:30")
        assertEquals("2026-07-14 09:30", Dates.toLocalInput(stored))
    }
}
