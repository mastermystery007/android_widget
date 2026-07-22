package com.mastermystery.oneminutecoach.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mastermystery.oneminutecoach.data.CoachContext
import com.mastermystery.oneminutecoach.data.EnergyLevel
import com.mastermystery.oneminutecoach.data.GoalCategory

@Composable
fun FocusScreen(
    focus: FocusState,
    onToggle: () -> Unit,
    onAddMinute: () -> Unit,
    onComplete: () -> Unit,
    onSavePartial: () -> Unit,
    onClose: () -> Unit,
) {
    val minutes = focus.remainingSeconds / 60
    val seconds = focus.remainingSeconds % 60
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onClose) { Text("Close") }
                Spacer(Modifier.weight(1f))
                AssistChip(onClick = {}, label = { Text(focus.suggestion.goalTitle) })
            }
            Spacer(Modifier.weight(0.45f))
            Text(
                focus.suggestion.actionTitle,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.weight(0.15f))
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { focus.progress.coerceIn(0f, 1f) },
                    modifier = Modifier.size(230.dp),
                    strokeWidth = 14.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "%02d:%02d".format(minutes, seconds),
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        if (focus.isRunning) "Focus on only this" else "Paused",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.weight(0.15f))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onAddMinute) { Text("+1 minute") }
                Button(onClick = onToggle) {
                    Icon(
                        if (focus.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (focus.isRunning) "Pause" else "Resume")
                }
            }
            Spacer(Modifier.weight(0.5f))
            Button(onClick = onComplete, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Complete action")
            }
            TextButton(onClick = onSavePartial) {
                Text("Stop and save partial progress")
            }
        }
    }
}

@Composable
fun AddGoalDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, GoalCategory) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var outcome by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(GoalCategory.PERSONAL) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create a goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(100) },
                    label = { Text("What do you want to move forward?") },
                    placeholder = { Text("Publish my Android app") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = outcome,
                    onValueChange = { outcome = it.take(240) },
                    label = { Text("What would progress look like?") },
                    placeholder = { Text("Release a useful first version") },
                    minLines = 2,
                    maxLines = 4,
                )
                Text(
                    "Category",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GoalCategory.entries.forEach { item ->
                        FilterChip(
                            selected = category == item,
                            onClick = { category = item },
                            label = { Text(item.displayName()) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(title.trim(), outcome.trim(), category) },
                enabled = title.isNotBlank(),
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
fun AddActionDialog(
    onDismiss: () -> Unit,
    onCreate: (String, Int, EnergyLevel, CoachContext) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var minutesText by remember { mutableStateOf("3") }
    var energy by remember { mutableStateOf(EnergyLevel.MEDIUM) }
    var context by remember { mutableStateOf(CoachContext.ANY) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom micro-action") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(140) },
                    label = { Text("Action") },
                    placeholder = { Text("Write the first paragraph") },
                    minLines = 2,
                    maxLines = 4,
                )
                OutlinedTextField(
                    value = minutesText,
                    onValueChange = { minutesText = it.filter(Char::isDigit).take(2) },
                    label = { Text("Minutes") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                Text("Energy", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    EnergyLevel.entries.forEach { item ->
                        FilterChip(
                            selected = energy == item,
                            onClick = { energy = item },
                            label = { Text(item.displayName()) },
                        )
                    }
                }
                Text("Context", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CoachContext.entries.forEach { item ->
                        FilterChip(
                            selected = context == item,
                            onClick = { context = item },
                            label = { Text(item.displayName()) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCreate(
                        title.trim(),
                        minutesText.toIntOrNull()?.coerceIn(1, 60) ?: 3,
                        energy,
                        context,
                    )
                },
                enabled = title.isNotBlank(),
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

fun GoalCategory.displayName(): String =
    name.lowercase().replaceFirstChar { it.uppercase() }

fun EnergyLevel.displayName(): String =
    name.lowercase().replaceFirstChar { it.uppercase() }

fun CoachContext.displayName(): String =
    if (this == CoachContext.ANY) {
        "Anywhere"
    } else {
        name.lowercase().replaceFirstChar { it.uppercase() }
    }

fun formatTime(hour: Int, minute: Int): String {
    val suffix = if (hour < 12) "AM" else "PM"
    val displayHour = when (val value = hour % 12) {
        0 -> 12
        else -> value
    }
    return "%d:%02d %s".format(displayHour, minute, suffix)
}
