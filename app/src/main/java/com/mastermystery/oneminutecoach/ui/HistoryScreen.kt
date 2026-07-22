package com.mastermystery.oneminutecoach.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mastermystery.oneminutecoach.data.SessionEntity
import com.mastermystery.oneminutecoach.data.SessionOutcome
import com.mastermystery.oneminutecoach.domain.CoachStats
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HistoryScreen(
    stats: CoachStats,
    sessions: List<SessionEntity>,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                "Progress",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Consistency is measured by returning, not by being perfect.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("${stats.totalCompletions}", "Completed", Modifier.weight(1f))
                StatTile("${stats.bestStreak}", "Best streak", Modifier.weight(1f))
                StatTile("${stats.completionRate}%", "Finish rate", Modifier.weight(1f))
            }
        }
        item { WeeklyMomentumCard(stats) }
        item {
            Text(
                "Recent activity",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        if (sessions.isEmpty()) {
            item {
                OutlinedCard {
                    Text(
                        "Completed and partial focus sessions will appear here.",
                        modifier = Modifier.padding(18.dp),
                    )
                }
            }
        } else {
            items(sessions.take(50), key = { it.id }) { session ->
                SessionCard(session)
            }
        }
    }
}

@Composable
private fun SessionCard(session: SessionEntity) {
    val date = Instant.ofEpochMilli(session.endedAt)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("d MMM · h:mm a"))
    val status = when (session.outcome) {
        SessionOutcome.COMPLETED -> "Completed"
        SessionOutcome.PARTIAL -> "Partial"
        SessionOutcome.SKIPPED -> "Skipped"
    }
    OutlinedCard {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        if (session.outcome == SessionOutcome.COMPLETED) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (session.outcome == SessionOutcome.COMPLETED) {
                        Icons.Default.Check
                    } else {
                        Icons.Default.Timer
                    },
                    contentDescription = null,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(session.actionTitle, fontWeight = FontWeight.Medium)
                Text(
                    "$status · ${(session.actualSeconds / 60).coerceAtLeast(1)} min · $date",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (session.note.isNotBlank()) {
                    Text(
                        session.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
