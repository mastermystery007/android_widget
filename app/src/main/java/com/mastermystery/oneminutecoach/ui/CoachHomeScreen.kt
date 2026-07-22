package com.mastermystery.oneminutecoach.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mastermystery.oneminutecoach.data.CoachContext
import com.mastermystery.oneminutecoach.data.CoachPreferences
import com.mastermystery.oneminutecoach.data.EnergyLevel
import com.mastermystery.oneminutecoach.data.GoalEntity
import com.mastermystery.oneminutecoach.domain.CoachStats
import java.time.LocalDate
import java.time.format.TextStyle

@Composable
fun CoachScreen(
    state: CoachUiState,
    onCreateGoal: () -> Unit,
    onMinutes: (Int) -> Unit,
    onEnergy: (EnergyLevel) -> Unit,
    onContext: (CoachContext) -> Unit,
    onStart: () -> Unit,
    onDone: () -> Unit,
    onSwap: () -> Unit,
) {
    val dashboard = state.dashboard
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (dashboard.activeGoal == null) {
            item { EmptyCoachCard(onCreateGoal) }
            item { HowItWorksCard() }
        } else {
            item {
                CheckInCard(
                    preferences = dashboard.preferences,
                    onMinutes = onMinutes,
                    onEnergy = onEnergy,
                    onContext = onContext,
                )
            }
            item {
                NextActionCard(
                    goal = dashboard.activeGoal,
                    title = dashboard.suggestion?.actionTitle,
                    minutes = dashboard.suggestion?.durationMinutes ?: 1,
                    reason = dashboard.suggestion?.reason,
                    onStart = onStart,
                    onDone = onDone,
                    onSwap = onSwap,
                )
            }
            item { TodayStatsCard(dashboard.stats) }
            item { WeeklyMomentumCard(dashboard.stats) }
        }
    }
}

@Composable
fun EmptyCoachCard(onCreateGoal: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(42.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                "Turn a meaningful goal into one doable action.",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "The coach adapts each next step to your available time, energy, location, and recent behaviour.",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Button(onClick = onCreateGoal) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Create my first goal")
            }
        }
    }
}

@Composable
private fun HowItWorksCard() {
    OutlinedCard {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("How it works", fontWeight = FontWeight.Bold)
            StepRow("1", "Choose one goal that matters.")
            StepRow("2", "Check in with your time, energy, and context.")
            StepRow("3", "Start, finish, or swap one recommended action.")
            StepRow("4", "The coach learns from completion and swap history.")
        }
    }
}

@Composable
private fun StepRow(number: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(number, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Text(text, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun CheckInCard(
    preferences: CoachPreferences,
    onMinutes: (Int) -> Unit,
    onEnergy: (EnergyLevel) -> Unit,
    onContext: (CoachContext) -> Unit,
) {
    OutlinedCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timer, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("What can you give right now?", fontWeight = FontWeight.Bold)
            }
            ChoiceLabel("Time available")
            ScrollableChoices {
                listOf(1, 3, 5, 10, 20).forEach { minute ->
                    FilterChip(
                        selected = preferences.availableMinutes == minute,
                        onClick = { onMinutes(minute) },
                        label = { Text("${minute}m") },
                    )
                }
            }
            ChoiceLabel("Energy")
            ScrollableChoices {
                EnergyLevel.entries.forEach { energy ->
                    FilterChip(
                        selected = preferences.energy == energy,
                        onClick = { onEnergy(energy) },
                        label = { Text(energy.displayName()) },
                    )
                }
            }
            ChoiceLabel("Context")
            ScrollableChoices {
                CoachContext.entries.forEach { context ->
                    FilterChip(
                        selected = preferences.context == context,
                        onClick = { onContext(context) },
                        label = { Text(context.displayName()) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ScrollableChoices(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

@Composable
private fun ChoiceLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun NextActionCard(
    goal: GoalEntity,
    title: String?,
    minutes: Int,
    reason: String?,
    onStart: () -> Unit,
    onDone: () -> Unit,
    onSwap: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    goal.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                AssistChip(onClick = {}, label = { Text("$minutes min") })
            }
            Text(
                title ?: "Preparing your next step…",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            if (!reason.isNullOrBlank()) {
                Text(reason, color = MaterialTheme.colorScheme.onTertiaryContainer)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onStart,
                    enabled = title != null,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(5.dp))
                    Text("Start")
                }
                OutlinedButton(onClick = onDone, enabled = title != null) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.width(5.dp))
                    Text("Done")
                }
                IconButton(onClick = onSwap, enabled = title != null) {
                    Icon(Icons.Default.Refresh, contentDescription = "Show another action")
                }
            }
        }
    }
}

@Composable
fun TodayStatsCard(stats: CoachStats) {
    OutlinedCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Your momentum", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("${stats.completedToday}", "Today", Modifier.weight(1f))
                StatTile("${stats.currentStreak}", "Day streak", Modifier.weight(1f))
                StatTile("${stats.totalFocusMinutes}", "Minutes", Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun WeeklyMomentumCard(stats: CoachStats) {
    val max = stats.lastSevenDays.maxOrNull()?.coerceAtLeast(1) ?: 1
    val today = LocalDate.now()
    val locale = LocalLocale.current.platformLocale
    val labels = (6 downTo 0).map { days ->
        today.minusDays(days.toLong()).dayOfWeek
            .getDisplayName(TextStyle.NARROW, locale)
    }
    OutlinedCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Last 7 days", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                stats.lastSevenDays.forEachIndexed { index, count ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                    ) {
                        Text("$count", style = MaterialTheme.typography.labelSmall)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight((count.toFloat() / max).coerceAtLeast(0.08f))
                                .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                                .background(MaterialTheme.colorScheme.primary),
                        )
                        Text(labels[index], style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
