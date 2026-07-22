package com.mastermystery.oneminutecoach.domain

import com.mastermystery.oneminutecoach.data.SessionEntity
import com.mastermystery.oneminutecoach.data.SessionOutcome
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class StatsCalculatorTest {

    private val zone = ZoneId.of("UTC")
    private val today = LocalDate.of(2026, 7, 22)
    private val now = today.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun calculatesCurrentAndBestStreaks() {
        val sessions = listOf(
            session(today),
            session(today.minusDays(1)),
            session(today.minusDays(2)),
            session(today.minusDays(5)),
            session(today.minusDays(6)),
        )

        val stats = StatsCalculator.calculate(sessions, now, zone)

        assertEquals(3, stats.currentStreak)
        assertEquals(3, stats.bestStreak)
        assertEquals(5, stats.totalCompletions)
    }

    @Test
    fun partialSessionsDoNotCountAsCompletions() {
        val sessions = listOf(
            session(today, SessionOutcome.COMPLETED),
            session(today, SessionOutcome.PARTIAL),
        )

        val stats = StatsCalculator.calculate(sessions, now, zone)

        assertEquals(1, stats.completedToday)
        assertEquals(50, stats.completionRate)
    }

    private fun session(
        date: LocalDate,
        outcome: SessionOutcome = SessionOutcome.COMPLETED,
    ): SessionEntity {
        val end = date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        return SessionEntity(
            id = end,
            goalId = 1,
            actionId = 2,
            actionTitle = "Test action",
            plannedMinutes = 3,
            actualSeconds = 180,
            outcome = outcome,
            startedAt = end - 180_000,
            endedAt = end,
        )
    }
}
