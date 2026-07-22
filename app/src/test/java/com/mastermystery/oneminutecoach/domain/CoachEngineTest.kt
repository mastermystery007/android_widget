package com.mastermystery.oneminutecoach.domain

import com.mastermystery.oneminutecoach.data.ActionEntity
import com.mastermystery.oneminutecoach.data.CoachContext
import com.mastermystery.oneminutecoach.data.EnergyLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class CoachEngineTest {

    private val actions = listOf(
        ActionEntity(
            id = 1,
            goalId = 7,
            title = "Open the document",
            durationMinutes = 1,
            energy = EnergyLevel.LOW,
            context = CoachContext.ANY,
        ),
        ActionEntity(
            id = 2,
            goalId = 7,
            title = "Write for five minutes",
            durationMinutes = 5,
            energy = EnergyLevel.MEDIUM,
            context = CoachContext.HOME,
        ),
        ActionEntity(
            id = 3,
            goalId = 7,
            title = "Solve the hard blocker",
            durationMinutes = 20,
            energy = EnergyLevel.HIGH,
            context = CoachContext.WORK,
        ),
    )

    @Test
    fun selectsActionThatFitsTimeEnergyAndContext() {
        val result = CoachEngine.select(
            actions = actions,
            checkIn = CoachCheckIn(
                availableMinutes = 5,
                energy = EnergyLevel.MEDIUM,
                context = CoachContext.HOME,
            ),
            recentActionIds = emptyList(),
            nowEpochMillis = 1_767_268_800_000L,
        )

        assertNotNull(result)
        assertEquals(2L, result?.action?.id)
    }

    @Test
    fun avoidsMostRecentlyShownActionWhenAlternativesExist() {
        val first = CoachEngine.select(
            actions = actions,
            checkIn = CoachCheckIn(5, EnergyLevel.MEDIUM, CoachContext.HOME),
            recentActionIds = emptyList(),
        )
        val second = CoachEngine.select(
            actions = actions,
            checkIn = CoachCheckIn(5, EnergyLevel.MEDIUM, CoachContext.HOME),
            recentActionIds = listOf(first!!.action.id),
        )

        assertNotNull(second)
        assertNotEquals(first.action.id, second?.action?.id)
    }

    @Test
    fun returnsNullWhenEveryActionIsDisabled() {
        val result = CoachEngine.select(
            actions = actions.map { it.copy(isEnabled = false) },
            checkIn = CoachCheckIn(),
            recentActionIds = emptyList(),
        )

        assertEquals(null, result)
    }
}
