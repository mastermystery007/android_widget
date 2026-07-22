package com.mastermystery.oneminutecoach.domain

import com.mastermystery.oneminutecoach.data.ActionEntity
import com.mastermystery.oneminutecoach.data.CoachContext
import com.mastermystery.oneminutecoach.data.EnergyLevel
import com.mastermystery.oneminutecoach.data.GoalCategory
import com.mastermystery.oneminutecoach.data.SessionEntity
import com.mastermystery.oneminutecoach.data.SessionOutcome
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

data class CoachCheckIn(
    val availableMinutes: Int = 3,
    val energy: EnergyLevel = EnergyLevel.MEDIUM,
    val context: CoachContext = CoachContext.ANY,
)

data class CoachPick(
    val action: ActionEntity,
    val reason: String,
    val score: Double,
)

data class CoachStats(
    val completedToday: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val totalCompletions: Int = 0,
    val totalFocusMinutes: Int = 0,
    val completionRate: Int = 0,
    val lastSevenDays: List<Int> = List(7) { 0 },
)

object CoachEngine {

    fun select(
        actions: List<ActionEntity>,
        checkIn: CoachCheckIn,
        recentActionIds: List<Long>,
        nowEpochMillis: Long = System.currentTimeMillis(),
    ): CoachPick? {
        val enabledActions = actions.filter { it.isEnabled }
        if (enabledActions.isEmpty()) return null

        val mostRecent = recentActionIds.firstOrNull()
        val candidates = if (mostRecent != null && enabledActions.size > 1) {
            enabledActions.filterNot { it.id == mostRecent }.ifEmpty { enabledActions }
        } else {
            enabledActions
        }

        val hour = Instant.ofEpochMilli(nowEpochMillis)
            .atZone(ZoneId.systemDefault())
            .hour

        return candidates
            .asSequence()
            .map { action ->
                var score = 100.0

                score -= abs(action.durationMinutes - checkIn.availableMinutes) * 9.0
                if (action.durationMinutes > checkIn.availableMinutes) score -= 20.0
                score -= abs(action.energy.ordinal - checkIn.energy.ordinal) * 14.0

                if (action.context != CoachContext.ANY && action.context != checkIn.context) {
                    score -= 22.0
                }

                val recentIndex = recentActionIds.indexOf(action.id)
                if (recentIndex >= 0) score -= (36 - recentIndex * 4).coerceAtLeast(8)

                score += action.completedCount.coerceAtMost(12) * 1.5
                score -= action.skippedCount.coerceAtMost(10) * 1.2

                if (hour < 11 && action.morningFriendly) score += 8
                if (hour >= 20 && action.eveningFriendly) score += 8

                action to score
            }
            .maxByOrNull { it.second }
            ?.let { (action, score) ->
                CoachPick(
                    action = action,
                    reason = reasonFor(action, checkIn),
                    score = score,
                )
            }
    }

    private fun reasonFor(action: ActionEntity, checkIn: CoachCheckIn): String {
        return when {
            action.durationMinutes <= 1 ->
                "Small enough to start immediately."
            checkIn.energy == EnergyLevel.LOW ->
                "A low-friction step that still moves the goal forward."
            action.durationMinutes == checkIn.availableMinutes ->
                "A precise fit for the ${checkIn.availableMinutes} minutes you have."
            action.context == checkIn.context && checkIn.context != CoachContext.ANY ->
                "Matched to where you are right now."
            else ->
                "The strongest useful next step for this check-in."
        }
    }

    fun createTemplates(category: GoalCategory, goalId: Long): List<ActionEntity> {
        val rows = when (category) {
            GoalCategory.WORK -> listOf(
                Template("Write the next concrete deliverable", 3, EnergyLevel.MEDIUM, CoachContext.WORK),
                Template("Remove one blocker", 5, EnergyLevel.HIGH, CoachContext.WORK),
                Template("Send the shortest useful follow-up", 2, EnergyLevel.LOW, CoachContext.ANY),
                Template("Clarify the next decision in one sentence", 1, EnergyLevel.LOW, CoachContext.ANY),
                Template("Open the project and improve one small detail", 5, EnergyLevel.MEDIUM, CoachContext.ANY),
                Template("List the next three actions", 3, EnergyLevel.MEDIUM, CoachContext.ANY),
            )
            GoalCategory.STUDY -> listOf(
                Template("Recall one concept without notes", 3, EnergyLevel.MEDIUM, CoachContext.ANY),
                Template("Read one page and write one takeaway", 5, EnergyLevel.LOW, CoachContext.ANY),
                Template("Solve the smallest available practice problem", 10, EnergyLevel.HIGH, CoachContext.HOME),
                Template("Create one flashcard", 2, EnergyLevel.LOW, CoachContext.ANY),
                Template("Explain one idea aloud", 3, EnergyLevel.MEDIUM, CoachContext.ANY),
                Template("Open the material and mark the next section", 1, EnergyLevel.LOW, CoachContext.ANY),
            )
            GoalCategory.HEALTH -> listOf(
                Template("Walk continuously for five minutes", 5, EnergyLevel.MEDIUM, CoachContext.OUTDOORS),
                Template("Drink water and take ten slow breaths", 2, EnergyLevel.LOW, CoachContext.ANY),
                Template("Do one mobility sequence", 5, EnergyLevel.MEDIUM, CoachContext.HOME),
                Template("Prepare the next healthy meal component", 10, EnergyLevel.MEDIUM, CoachContext.HOME),
                Template("Stand up and stretch", 1, EnergyLevel.LOW, CoachContext.ANY),
                Template("Put exercise clothes where you can see them", 2, EnergyLevel.LOW, CoachContext.HOME),
            )
            GoalCategory.CREATIVE -> listOf(
                Template("Make one deliberately rough draft", 5, EnergyLevel.MEDIUM, CoachContext.ANY),
                Template("Capture one idea without judging it", 1, EnergyLevel.LOW, CoachContext.ANY),
                Template("Improve one sentence, frame, or sketch", 3, EnergyLevel.MEDIUM, CoachContext.ANY),
                Template("Collect three references", 5, EnergyLevel.LOW, CoachContext.ANY),
                Template("Create continuously for ten minutes", 10, EnergyLevel.HIGH, CoachContext.HOME),
                Template("Name the emotion or effect you want", 2, EnergyLevel.LOW, CoachContext.ANY),
            )
            GoalCategory.HOME -> listOf(
                Template("Clear one visible surface", 5, EnergyLevel.MEDIUM, CoachContext.HOME),
                Template("Put away five items", 3, EnergyLevel.LOW, CoachContext.HOME),
                Template("Start one load or cleaning cycle", 2, EnergyLevel.LOW, CoachContext.HOME),
                Template("Discard one thing you no longer need", 1, EnergyLevel.LOW, CoachContext.HOME),
                Template("Clean one small zone completely", 10, EnergyLevel.HIGH, CoachContext.HOME),
                Template("Prepare tomorrow's essentials", 5, EnergyLevel.MEDIUM, CoachContext.HOME),
            )
            GoalCategory.PERSONAL -> listOf(
                Template("Write the next step in one sentence", 1, EnergyLevel.LOW, CoachContext.ANY),
                Template("Do the easiest useful part", 3, EnergyLevel.LOW, CoachContext.ANY),
                Template("Make one decision you have been postponing", 5, EnergyLevel.HIGH, CoachContext.ANY),
                Template("Send one message that moves this forward", 2, EnergyLevel.MEDIUM, CoachContext.ANY),
                Template("Prepare the environment for the next session", 5, EnergyLevel.MEDIUM, CoachContext.HOME),
                Template("Work with full attention for ten minutes", 10, EnergyLevel.HIGH, CoachContext.ANY),
            )
        }

        return rows.map {
            ActionEntity(
                goalId = goalId,
                title = it.title,
                durationMinutes = it.minutes,
                energy = it.energy,
                context = it.context,
                morningFriendly = it.minutes <= 5,
                eveningFriendly = it.energy != EnergyLevel.HIGH,
            )
        }
    }

    private data class Template(
        val title: String,
        val minutes: Int,
        val energy: EnergyLevel,
        val context: CoachContext,
    )
}

object StatsCalculator {

    fun calculate(
        sessions: List<SessionEntity>,
        nowEpochMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): CoachStats {
        val completed = sessions.filter { it.outcome == SessionOutcome.COMPLETED }
        val today = Instant.ofEpochMilli(nowEpochMillis).atZone(zoneId).toLocalDate()
        val completionDates = completed
            .map { Instant.ofEpochMilli(it.endedAt).atZone(zoneId).toLocalDate() }

        val dateCounts = completionDates.groupingBy { it }.eachCount()
        val activeDates = dateCounts.keys.sortedDescending()

        var currentStreak = 0
        var cursor = today
        if (cursor !in dateCounts && cursor.minusDays(1) in dateCounts) {
            cursor = cursor.minusDays(1)
        }
        while (cursor in dateCounts) {
            currentStreak++
            cursor = cursor.minusDays(1)
        }

        var bestStreak = 0
        var running = 0
        var previous: LocalDate? = null
        activeDates.sorted().forEach { date ->
            running = if (previous != null && date == previous!!.plusDays(1)) running + 1 else 1
            bestStreak = maxOf(bestStreak, running)
            previous = date
        }

        val attempted = sessions.count {
            it.outcome == SessionOutcome.COMPLETED || it.outcome == SessionOutcome.PARTIAL
        }
        val completionRate = if (attempted == 0) 0 else completed.size * 100 / attempted

        return CoachStats(
            completedToday = dateCounts[today] ?: 0,
            currentStreak = currentStreak,
            bestStreak = bestStreak,
            totalCompletions = completed.size,
            totalFocusMinutes = completed.sumOf { (it.actualSeconds / 60).coerceAtLeast(1) },
            completionRate = completionRate,
            lastSevenDays = (6 downTo 0).map { daysAgo ->
                dateCounts[today.minusDays(daysAgo.toLong())] ?: 0
            },
        )
    }
}
