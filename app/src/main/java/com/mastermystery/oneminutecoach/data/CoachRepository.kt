package com.mastermystery.oneminutecoach.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.glance.appwidget.updateAll
import androidx.room.withTransaction
import com.mastermystery.oneminutecoach.domain.CoachCheckIn
import com.mastermystery.oneminutecoach.domain.CoachEngine
import com.mastermystery.oneminutecoach.domain.CoachStats
import com.mastermystery.oneminutecoach.domain.StatsCalculator
import com.mastermystery.oneminutecoach.widget.CoachWidget
import com.mastermystery.oneminutecoach.worker.ReminderScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

private val Context.coachDataStore by preferencesDataStore(name = "coach_preferences")

class CoachRepository(
    private val context: Context,
    private val database: CoachDatabase = CoachDatabase.get(context),
) {
    private val dao = database.coachDao()

    private object Keys {
        val availableMinutes = intPreferencesKey("available_minutes")
        val energy = intPreferencesKey("energy")
        val context = intPreferencesKey("context")
        val reminderEnabled = booleanPreferencesKey("reminder_enabled")
        val reminderHour = intPreferencesKey("reminder_hour")
        val reminderMinute = intPreferencesKey("reminder_minute")
        val hapticsEnabled = booleanPreferencesKey("haptics_enabled")
        val dynamicColorEnabled = booleanPreferencesKey("dynamic_color_enabled")
        val showReasonInWidget = booleanPreferencesKey("show_reason_in_widget")
    }

    val preferences: Flow<CoachPreferences> = context.coachDataStore.data.map { prefs ->
        CoachPreferences(
            availableMinutes = prefs[Keys.availableMinutes] ?: 3,
            energy = EnergyLevel.entries.getOrElse(prefs[Keys.energy] ?: 1) { EnergyLevel.MEDIUM },
            context = CoachContext.entries.getOrElse(prefs[Keys.context] ?: 0) { CoachContext.ANY },
            reminderEnabled = prefs[Keys.reminderEnabled] ?: false,
            reminderHour = prefs[Keys.reminderHour] ?: 9,
            reminderMinute = prefs[Keys.reminderMinute] ?: 0,
            hapticsEnabled = prefs[Keys.hapticsEnabled] ?: true,
            dynamicColorEnabled = prefs[Keys.dynamicColorEnabled] ?: true,
            showReasonInWidget = prefs[Keys.showReasonInWidget] ?: true,
        )
    }

    val goals: Flow<List<GoalEntity>> = dao.observeGoals()
    val activeGoal: Flow<GoalEntity?> = dao.observeActiveGoal()
    val suggestion: Flow<SuggestionEntity?> = dao.observeSuggestion()
    val recentSessions: Flow<List<SessionEntity>> = dao.observeRecentSessions(100)

    val actionsForActiveGoal: Flow<List<ActionEntity>> = activeGoal.flatMapLatest { goal ->
        if (goal == null) flowOf(emptyList()) else dao.observeActions(goal.id)
    }

    val stats: Flow<CoachStats> = recentSessions.map { StatsCalculator.calculate(it) }

    val dashboard: Flow<DashboardSnapshot> = combine(
        activeGoal,
        suggestion,
        stats,
        preferences,
    ) { goal, currentSuggestion, currentStats, currentPreferences ->
        DashboardSnapshot(goal, currentSuggestion, currentStats, currentPreferences)
    }

    suspend fun createGoal(
        title: String,
        outcome: String,
        category: GoalCategory,
    ): Long {
        require(title.isNotBlank()) { "Goal title cannot be blank." }

        val goalId = database.withTransaction {
            dao.deactivateAllGoals()
            val id = dao.insertGoal(
                GoalEntity(
                    title = title.trim(),
                    outcome = outcome.trim(),
                    category = category,
                ),
            )
            dao.insertActions(CoachEngine.createTemplates(category, id))
            dao.clearSuggestion()
            id
        }
        generateSuggestion()
        return goalId
    }

    suspend fun setActiveGoal(goalId: Long) {
        database.withTransaction {
            dao.activateGoal(goalId)
            dao.clearSuggestion()
        }
        generateSuggestion()
    }

    suspend fun deleteGoal(goalId: Long) {
        database.withTransaction {
            dao.deleteActionsForGoal(goalId)
            dao.deleteSessionsForGoal(goalId)
            dao.deleteGoal(goalId)
            dao.clearSuggestion()
            val remaining = dao.getGoals()
            if (remaining.isNotEmpty()) dao.activateGoal(remaining.first().id)
        }
        ensureSuggestion()
    }

    suspend fun addCustomAction(
        title: String,
        durationMinutes: Int,
        energy: EnergyLevel,
        coachContext: CoachContext,
    ) {
        val goal = dao.getActiveGoal() ?: return
        if (title.isBlank()) return
        dao.insertAction(
            ActionEntity(
                goalId = goal.id,
                title = title.trim(),
                durationMinutes = durationMinutes.coerceIn(1, 60),
                energy = energy,
                context = coachContext,
            ),
        )
        generateSuggestion()
    }

    suspend fun setActionEnabled(actionId: Long, enabled: Boolean) {
        dao.setActionEnabled(actionId, enabled)
        generateSuggestion()
    }

    suspend fun deleteAction(actionId: Long) {
        dao.deleteAction(actionId)
        val current = dao.getSuggestion()
        if (current?.actionId == actionId) {
            dao.clearSuggestion()
            generateSuggestion()
        } else {
            refreshWidgets()
        }
    }

    suspend fun updateCheckIn(
        availableMinutes: Int? = null,
        energy: EnergyLevel? = null,
        coachContext: CoachContext? = null,
    ) {
        context.coachDataStore.edit { prefs ->
            availableMinutes?.let { prefs[Keys.availableMinutes] = it.coerceIn(1, 60) }
            energy?.let { prefs[Keys.energy] = it.ordinal }
            coachContext?.let { prefs[Keys.context] = it.ordinal }
        }
        generateSuggestion()
    }

    suspend fun updateAppearance(
        dynamicColorEnabled: Boolean? = null,
        hapticsEnabled: Boolean? = null,
        showReasonInWidget: Boolean? = null,
    ) {
        context.coachDataStore.edit { prefs ->
            dynamicColorEnabled?.let { prefs[Keys.dynamicColorEnabled] = it }
            hapticsEnabled?.let { prefs[Keys.hapticsEnabled] = it }
            showReasonInWidget?.let { prefs[Keys.showReasonInWidget] = it }
        }
        refreshWidgets()
    }

    suspend fun updateReminder(enabled: Boolean, hour: Int, minute: Int) {
        context.coachDataStore.edit { prefs ->
            prefs[Keys.reminderEnabled] = enabled
            prefs[Keys.reminderHour] = hour.coerceIn(0, 23)
            prefs[Keys.reminderMinute] = minute.coerceIn(0, 59)
        }
        if (enabled) {
            ReminderScheduler.schedule(context, hour, minute)
        } else {
            ReminderScheduler.cancel(context)
        }
    }

    suspend fun ensureSuggestion(): SuggestionEntity? {
        return dao.getSuggestion() ?: generateSuggestion()
    }

    suspend fun generateSuggestion(
        extraRecentActionIds: List<Long> = emptyList(),
    ): SuggestionEntity? {
        val goal = dao.getActiveGoal() ?: run {
            dao.clearSuggestion()
            refreshWidgets()
            return null
        }
        val actions = dao.getActionsForGoal(goal.id)
        val prefs = preferences.first()
        val recentIds = extraRecentActionIds + dao.getRecentSessions(12).mapNotNull { it.actionId }
        val pick = CoachEngine.select(
            actions = actions,
            checkIn = CoachCheckIn(
                availableMinutes = prefs.availableMinutes,
                energy = prefs.energy,
                context = prefs.context,
            ),
            recentActionIds = recentIds,
        ) ?: run {
            dao.clearSuggestion()
            refreshWidgets()
            return null
        }

        val entity = SuggestionEntity(
            goalId = goal.id,
            goalTitle = goal.title,
            actionId = pick.action.id,
            actionTitle = pick.action.title,
            durationMinutes = pick.action.durationMinutes,
            reason = pick.reason,
        )
        dao.upsertSuggestion(entity)
        refreshWidgets()
        return entity
    }

    suspend fun completeCurrent(
        actualSeconds: Int? = null,
        note: String = "",
        partial: Boolean = false,
    ) {
        val current = dao.getSuggestion() ?: return
        val endedAt = System.currentTimeMillis()
        val plannedSeconds = current.durationMinutes * 60
        val elapsed = actualSeconds?.coerceAtLeast(1) ?: plannedSeconds
        val outcome = if (partial) SessionOutcome.PARTIAL else SessionOutcome.COMPLETED
        val startedAt = endedAt - elapsed * 1000L

        database.withTransaction {
            dao.insertSession(
                SessionEntity(
                    goalId = current.goalId,
                    actionId = current.actionId,
                    actionTitle = current.actionTitle,
                    plannedMinutes = current.durationMinutes,
                    actualSeconds = elapsed,
                    outcome = outcome,
                    startedAt = startedAt,
                    endedAt = endedAt,
                    note = note.trim(),
                ),
            )
            if (outcome == SessionOutcome.COMPLETED) {
                dao.markActionCompleted(current.actionId, endedAt)
            }
            dao.clearSuggestion()
        }
        generateSuggestion(extraRecentActionIds = listOf(current.actionId))
    }

    suspend fun swapCurrent() {
        val current = dao.getSuggestion() ?: return
        dao.markActionSkipped(current.actionId, System.currentTimeMillis())
        dao.clearSuggestion()
        generateSuggestion(extraRecentActionIds = listOf(current.actionId))
    }

    suspend fun getWidgetSnapshot(): WidgetSnapshot {
        val current = ensureSuggestion()
        val sessionRows = dao.getRecentSessions(365)
        val currentStats = StatsCalculator.calculate(sessionRows)
        val prefs = preferences.first()
        return WidgetSnapshot(
            suggestion = current,
            currentStreak = currentStats.currentStreak,
            completedToday = currentStats.completedToday,
            showReason = prefs.showReasonInWidget,
        )
    }

    suspend fun buildShareSummary(): String {
        val goal = dao.getActiveGoal()
        val currentStats = StatsCalculator.calculate(dao.getRecentSessions(500))
        val goalLine = goal?.let { "Current goal: ${it.title}" } ?: "No active goal"
        return """
            One Minute Coach progress

            $goalLine
            Current streak: ${currentStats.currentStreak} days
            Total completed actions: ${currentStats.totalCompletions}
            Focus time: ${currentStats.totalFocusMinutes} minutes
            Completion rate: ${currentStats.completionRate}%
        """.trimIndent()
    }

    private suspend fun refreshWidgets() {
        CoachWidget().updateAll(context)
    }
}

data class DashboardSnapshot(
    val activeGoal: GoalEntity? = null,
    val suggestion: SuggestionEntity? = null,
    val stats: CoachStats = CoachStats(),
    val preferences: CoachPreferences = CoachPreferences(),
)
