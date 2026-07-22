package com.mastermystery.oneminutecoach.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mastermystery.oneminutecoach.data.ActionEntity
import com.mastermystery.oneminutecoach.data.GoalEntity

@Composable
fun GoalsScreen(
    goals: List<GoalEntity>,
    actions: List<ActionEntity>,
    onActivate: (Long) -> Unit,
    onDeleteGoal: (Long) -> Unit,
    onAddAction: () -> Unit,
    onToggleAction: (Long, Boolean) -> Unit,
    onDeleteAction: (Long) -> Unit,
    onCreateGoal: () -> Unit,
) {
    var pendingDeleteGoal by remember { mutableStateOf<GoalEntity?>(null) }
    var pendingDeleteAction by remember { mutableStateOf<ActionEntity?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Goals", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Keep one goal active so the coach can protect your attention.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (goals.isEmpty()) {
            item { EmptyCoachCard(onCreateGoal) }
        } else {
            items(goals, key = { it.id }) { goal ->
                GoalCard(
                    goal = goal,
                    onActivate = { onActivate(goal.id) },
                    onDelete = { pendingDeleteGoal = goal },
                )
            }
        }

        if (goals.any { it.isActive }) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Action library",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Disable anything that does not fit. Recommendations update immediately.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = onAddAction) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Custom")
                    }
                }
            }

            if (actions.isEmpty()) {
                item {
                    OutlinedCard {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("No enabled actions remain.", fontWeight = FontWeight.Bold)
                            Text("Add a custom micro-action to keep coaching active.")
                            TextButton(onClick = onAddAction) { Text("Add action") }
                        }
                    }
                }
            } else {
                items(actions, key = { it.id }) { action ->
                    ActionCard(
                        action = action,
                        onToggle = { onToggleAction(action.id, it) },
                        onDelete = { pendingDeleteAction = action },
                    )
                }
            }
        }
    }

    pendingDeleteGoal?.let { goal ->
        ConfirmDeleteDialog(
            title = "Delete goal?",
            message = "This removes “${goal.title}”, its action library, and its local history.",
            onDismiss = { pendingDeleteGoal = null },
            onConfirm = {
                pendingDeleteGoal = null
                onDeleteGoal(goal.id)
            },
        )
    }

    pendingDeleteAction?.let { action ->
        ConfirmDeleteDialog(
            title = "Delete action?",
            message = "The action “${action.title}” will be removed from this goal.",
            onDismiss = { pendingDeleteAction = null },
            onConfirm = {
                pendingDeleteAction = null
                onDeleteAction(action.id)
            },
        )
    }
}

@Composable
private fun GoalCard(
    goal: GoalEntity,
    onActivate: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        onClick = { if (!goal.isActive) onActivate() },
        colors = CardDefaults.cardColors(
            containerColor = if (goal.isActive) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(goal.title, fontWeight = FontWeight.Bold)
                if (goal.outcome.isNotBlank()) {
                    Text(
                        goal.outcome,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    "${goal.category.displayName()}${if (goal.isActive) " · Active" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (!goal.isActive) {
                TextButton(onClick = onActivate) { Text("Use") }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete goal")
            }
        }
    }
}

@Composable
private fun ActionCard(
    action: ActionEntity,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    OutlinedCard {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 10.dp, bottom = 10.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(action.title, fontWeight = FontWeight.Medium)
                Text(
                    "${action.durationMinutes} min · ${action.energy.displayName()} energy · ${action.context.displayName()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (action.completedCount + action.skippedCount > 0) {
                    Text(
                        "${action.completedCount} completed · ${action.skippedCount} swapped",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            Switch(checked = action.isEnabled, onCheckedChange = onToggle)
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete action")
            }
        }
    }
}

@Composable
private fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Delete") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
