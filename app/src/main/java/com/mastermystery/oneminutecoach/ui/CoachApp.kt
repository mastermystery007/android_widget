package com.mastermystery.oneminutecoach.ui

import android.content.Intent
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoachApp(viewModel: CoachViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showGoalDialog by remember { mutableStateOf(false) }
    var showActionDialog by remember { mutableStateOf(false) }

    val dark = isSystemInDarkTheme()
    val useDynamic = state.dashboard.preferences.dynamicColorEnabled &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        !LocalInspectionMode.current
    val colors = when {
        useDynamic && dark -> dynamicDarkColorScheme(context)
        useDynamic -> dynamicLightColorScheme(context)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }

    MaterialTheme(colorScheme = colors) {
        LaunchedEffect(viewModel, state.dashboard.preferences.hapticsEnabled) {
            viewModel.events.collect { event ->
                when (event) {
                    is CoachEvent.Message -> {
                        if (
                            state.dashboard.preferences.hapticsEnabled &&
                            event.text.contains("completed", ignoreCase = true)
                        ) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        snackbar.showSnackbar(event.text)
                    }
                    is CoachEvent.Share -> {
                        val share = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, event.text)
                        }
                        context.startActivity(Intent.createChooser(share, "Share progress"))
                    }
                }
            }
        }

        val focus = state.focus
        if (focus != null) {
            FocusScreen(
                focus = focus,
                onToggle = viewModel::toggleTimer,
                onAddMinute = viewModel::addMinute,
                onComplete = { viewModel.finishFocus(completed = true) },
                onSavePartial = { viewModel.finishFocus(completed = false) },
                onClose = viewModel::dismissFocus,
            )
        } else {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("One Minute Coach", fontWeight = FontWeight.Bold)
                                Text(
                                    "One useful step. Right now.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                    )
                },
                snackbarHost = { SnackbarHost(snackbar) },
                bottomBar = {
                    CoachNavigationBar(state.section, viewModel::navigate)
                },
                floatingActionButton = {
                    if (state.section == AppSection.GOALS) {
                        ExtendedFloatingActionButton(
                            onClick = { showGoalDialog = true },
                            icon = { Icon(Icons.Default.Add, contentDescription = null) },
                            text = { Text("New goal") },
                        )
                    }
                },
            ) { padding ->
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    when (state.section) {
                        AppSection.COACH -> CoachScreen(
                            state = state,
                            onCreateGoal = { showGoalDialog = true },
                            onMinutes = viewModel::updateMinutes,
                            onEnergy = viewModel::updateEnergy,
                            onContext = viewModel::updateContext,
                            onStart = viewModel::startFocusFromCurrent,
                            onDone = viewModel::completeNow,
                            onSwap = viewModel::swap,
                        )
                        AppSection.GOALS -> GoalsScreen(
                            goals = state.goals,
                            actions = state.actions,
                            onActivate = viewModel::activateGoal,
                            onDeleteGoal = viewModel::deleteGoal,
                            onAddAction = { showActionDialog = true },
                            onToggleAction = viewModel::setActionEnabled,
                            onDeleteAction = viewModel::deleteAction,
                            onCreateGoal = { showGoalDialog = true },
                        )
                        AppSection.HISTORY -> HistoryScreen(
                            stats = state.dashboard.stats,
                            sessions = state.sessions,
                        )
                        AppSection.SETTINGS -> SettingsScreen(
                            preferences = state.dashboard.preferences,
                            onReminder = viewModel::updateReminder,
                            onDynamicColor = {
                                viewModel.updateAppearance(dynamicColor = it)
                            },
                            onHaptics = {
                                viewModel.updateAppearance(haptics = it)
                            },
                            onShowReason = {
                                viewModel.updateAppearance(showReason = it)
                            },
                            onShare = viewModel::shareProgress,
                            onMessage = { message ->
                                scope.launch { snackbar.showSnackbar(message) }
                            },
                        )
                    }
                }
            }
        }

        if (showGoalDialog) {
            AddGoalDialog(
                onDismiss = { showGoalDialog = false },
                onCreate = { title, outcome, category ->
                    showGoalDialog = false
                    viewModel.createGoal(title, outcome, category)
                },
            )
        }

        if (showActionDialog) {
            AddActionDialog(
                onDismiss = { showActionDialog = false },
                onCreate = { title, minutes, energy, coachContext ->
                    showActionDialog = false
                    viewModel.addCustomAction(title, minutes, energy, coachContext)
                },
            )
        }

        if (state.isBusy) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.18f),
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
private fun CoachNavigationBar(
    current: AppSection,
    onNavigate: (AppSection) -> Unit,
) {
    val destinations = listOf(
        Triple(AppSection.COACH, "Coach", Icons.Default.AutoAwesome),
        Triple(AppSection.GOALS, "Goals", Icons.Default.Flag),
        Triple(AppSection.HISTORY, "Progress", Icons.Default.BarChart),
        Triple(AppSection.SETTINGS, "Settings", Icons.Default.Settings),
    )
    NavigationBar {
        destinations.forEach { (section, label, icon) ->
            NavigationBarItem(
                selected = current == section,
                onClick = { onNavigate(section) },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) },
            )
        }
    }
}
