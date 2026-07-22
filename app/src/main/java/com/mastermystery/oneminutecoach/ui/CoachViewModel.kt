package com.mastermystery.oneminutecoach.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mastermystery.oneminutecoach.data.ActionEntity
import com.mastermystery.oneminutecoach.data.CoachContext
import com.mastermystery.oneminutecoach.data.CoachRepository
import com.mastermystery.oneminutecoach.data.DashboardSnapshot
import com.mastermystery.oneminutecoach.data.EnergyLevel
import com.mastermystery.oneminutecoach.data.GoalCategory
import com.mastermystery.oneminutecoach.data.GoalEntity
import com.mastermystery.oneminutecoach.data.SessionEntity
import com.mastermystery.oneminutecoach.data.SuggestionEntity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppSection { COACH, GOALS, HISTORY, SETTINGS }

data class CoachUiState(
    val dashboard: DashboardSnapshot = DashboardSnapshot(),
    val goals: List<GoalEntity> = emptyList(),
    val actions: List<ActionEntity> = emptyList(),
    val sessions: List<SessionEntity> = emptyList(),
    val section: AppSection = AppSection.COACH,
    val focus: FocusState? = null,
    val isBusy: Boolean = false,
)

data class FocusState(
    val suggestion: SuggestionEntity,
    val totalSeconds: Int,
    val remainingSeconds: Int,
    val isRunning: Boolean = true,
) {
    val elapsedSeconds: Int get() = (totalSeconds - remainingSeconds).coerceAtLeast(0)
    val progress: Float get() =
        if (totalSeconds <= 0) 0f else elapsedSeconds.toFloat() / totalSeconds.toFloat()
}

sealed interface CoachEvent {
    data class Message(val text: String) : CoachEvent
    data class Share(val text: String) : CoachEvent
}

class CoachViewModel(
    private val repository: CoachRepository,
) : ViewModel() {

    private val section = MutableStateFlow(AppSection.COACH)
    private val focus = MutableStateFlow<FocusState?>(null)
    private val busy = MutableStateFlow(false)
    private val _events = MutableSharedFlow<CoachEvent>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()

    private var timerJob: Job? = null

    val uiState: StateFlow<CoachUiState> = combine(
        repository.dashboard,
        repository.goals,
        repository.actionsForActiveGoal,
        repository.recentSessions,
        section,
        focus,
        busy,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        CoachUiState(
            dashboard = values[0] as DashboardSnapshot,
            goals = values[1] as List<GoalEntity>,
            actions = values[2] as List<ActionEntity>,
            sessions = values[3] as List<SessionEntity>,
            section = values[4] as AppSection,
            focus = values[5] as FocusState?,
            isBusy = values[6] as Boolean,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        CoachUiState(),
    )

    init {
        viewModelScope.launch { repository.ensureSuggestion() }
    }

    fun navigate(section: AppSection) {
        this.section.value = section
    }

    fun openSettings() {
        section.value = AppSection.SETTINGS
    }

    fun createGoal(title: String, outcome: String, category: GoalCategory) {
        launchBusy {
            repository.createGoal(title, outcome, category)
            section.value = AppSection.COACH
            _events.emit(CoachEvent.Message("Goal created. Your first action is ready."))
        }
    }

    fun activateGoal(goalId: Long) {
        launchBusy {
            repository.setActiveGoal(goalId)
            section.value = AppSection.COACH
        }
    }

    fun deleteGoal(goalId: Long) {
        launchBusy {
            repository.deleteGoal(goalId)
            _events.emit(CoachEvent.Message("Goal deleted."))
        }
    }

    fun addCustomAction(
        title: String,
        minutes: Int,
        energy: EnergyLevel,
        coachContext: CoachContext,
    ) {
        launchBusy {
            repository.addCustomAction(title, minutes, energy, coachContext)
            _events.emit(CoachEvent.Message("Custom action added."))
        }
    }

    fun setActionEnabled(actionId: Long, enabled: Boolean) {
        viewModelScope.launch { repository.setActionEnabled(actionId, enabled) }
    }

    fun deleteAction(actionId: Long) {
        viewModelScope.launch { repository.deleteAction(actionId) }
    }

    fun updateMinutes(minutes: Int) {
        viewModelScope.launch { repository.updateCheckIn(availableMinutes = minutes) }
    }

    fun updateEnergy(energy: EnergyLevel) {
        viewModelScope.launch { repository.updateCheckIn(energy = energy) }
    }

    fun updateContext(context: CoachContext) {
        viewModelScope.launch { repository.updateCheckIn(coachContext = context) }
    }

    fun swap() {
        launchBusy { repository.swapCurrent() }
    }

    fun completeNow() {
        launchBusy {
            repository.completeCurrent()
            _events.emit(CoachEvent.Message("Completed. A fresh next step is ready."))
        }
    }

    fun startFocusFromCurrent() {
        viewModelScope.launch {
            val suggestion = repository.ensureSuggestion() ?: return@launch
            val seconds = suggestion.durationMinutes * 60
            focus.value = FocusState(
                suggestion = suggestion,
                totalSeconds = seconds,
                remainingSeconds = seconds,
            )
            section.value = AppSection.COACH
            startTimerLoop()
        }
    }

    fun toggleTimer() {
        val current = focus.value ?: return
        focus.value = current.copy(isRunning = !current.isRunning)
        if (focus.value?.isRunning == true) startTimerLoop() else timerJob?.cancel()
    }

    fun addMinute() {
        val current = focus.value ?: return
        focus.value = current.copy(
            totalSeconds = current.totalSeconds + 60,
            remainingSeconds = current.remainingSeconds + 60,
        )
    }

    fun finishFocus(completed: Boolean = true) {
        val current = focus.value ?: return
        timerJob?.cancel()
        focus.value = null
        launchBusy {
            repository.completeCurrent(
                actualSeconds = current.elapsedSeconds.coerceAtLeast(1),
                partial = !completed,
            )
            _events.emit(
                CoachEvent.Message(
                    if (completed) "Focus session completed." else "Partial progress saved.",
                ),
            )
        }
    }

    fun dismissFocus() {
        timerJob?.cancel()
        focus.value = null
    }

    fun updateReminder(enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            repository.updateReminder(enabled, hour, minute)
            _events.emit(
                CoachEvent.Message(
                    if (enabled) "Daily reminder scheduled." else "Daily reminder disabled.",
                ),
            )
        }
    }

    fun updateAppearance(
        dynamicColor: Boolean? = null,
        haptics: Boolean? = null,
        showReason: Boolean? = null,
    ) {
        viewModelScope.launch {
            repository.updateAppearance(dynamicColor, haptics, showReason)
        }
    }

    fun shareProgress() {
        viewModelScope.launch {
            _events.emit(CoachEvent.Share(repository.buildShareSummary()))
        }
    }

    private fun startTimerLoop() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1_000)
                val current = focus.value ?: break
                if (!current.isRunning) break
                if (current.remainingSeconds <= 1) {
                    focus.value = current.copy(remainingSeconds = 0, isRunning = false)
                    break
                }
                focus.value = current.copy(remainingSeconds = current.remainingSeconds - 1)
            }
        }
    }

    private fun launchBusy(block: suspend () -> Unit) {
        viewModelScope.launch {
            busy.value = true
            runCatching { block() }
                .onFailure { _events.emit(CoachEvent.Message(it.message ?: "Something went wrong.")) }
            busy.value = false
        }
    }

    companion object {
        fun factory(repository: CoachRepository, context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CoachViewModel(repository) as T
                }
            }
    }
}
