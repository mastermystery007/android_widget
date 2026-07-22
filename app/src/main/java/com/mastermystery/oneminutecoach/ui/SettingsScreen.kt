package com.mastermystery.oneminutecoach.ui

import android.Manifest
import android.app.TimePickerDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.mastermystery.oneminutecoach.data.CoachPreferences
import com.mastermystery.oneminutecoach.widget.CoachWidgetReceiver

@Composable
fun SettingsScreen(
    preferences: CoachPreferences,
    onReminder: (Boolean, Int, Int) -> Unit,
    onDynamicColor: (Boolean) -> Unit,
    onHaptics: (Boolean) -> Unit,
    onShowReason: (Boolean) -> Unit,
    onShare: () -> Unit,
    onMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    var pendingReminder by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val time = pendingReminder
        if (granted && time != null) {
            onReminder(true, time.first, time.second)
        } else if (!granted) {
            onMessage("Notification permission is needed for reminders.")
        }
        pendingReminder = null
    }

    fun enableReminderAt(hour: Int, minute: Int) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingReminder = hour to minute
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onReminder(true, hour, minute)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        item {
            SettingsGroup("Daily coaching") {
                SettingsToggleRow(
                    icon = Icons.Default.Notifications,
                    title = "Daily reminder",
                    summary = if (preferences.reminderEnabled) {
                        "At ${formatTime(preferences.reminderHour, preferences.reminderMinute)}"
                    } else {
                        "Off"
                    },
                    checked = preferences.reminderEnabled,
                    onChecked = { enabled ->
                        if (!enabled) {
                            onReminder(false, preferences.reminderHour, preferences.reminderMinute)
                        } else {
                            showTimePicker(
                                context,
                                preferences.reminderHour,
                                preferences.reminderMinute,
                                ::enableReminderAt,
                            )
                        }
                    },
                )
                if (preferences.reminderEnabled) {
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text("Reminder time") },
                        supportingContent = {
                            Text(formatTime(preferences.reminderHour, preferences.reminderMinute))
                        },
                        trailingContent = {
                            TextButton(
                                onClick = {
                                    showTimePicker(
                                        context,
                                        preferences.reminderHour,
                                        preferences.reminderMinute,
                                        ::enableReminderAt,
                                    )
                                },
                            ) { Text("Change") }
                        },
                    )
                }
            }
        }
        item {
            SettingsGroup("Home-screen widget") {
                ListItem(
                    headlineContent = { Text("Add widget") },
                    supportingContent = {
                        Text("Pin the responsive widget with Start, Done, and Swap actions.")
                    },
                    leadingContent = { Icon(Icons.Default.Home, contentDescription = null) },
                    trailingContent = {
                        Button(
                            onClick = {
                                val requested = requestPinWidget(context)
                                onMessage(
                                    if (requested) {
                                        "Choose where to place the widget."
                                    } else {
                                        "Open your launcher’s widget picker to add it."
                                    },
                                )
                            },
                        ) { Text("Add") }
                    },
                )
                HorizontalDivider()
                SettingsToggleRow(
                    icon = Icons.Default.AutoAwesome,
                    title = "Show recommendation reason",
                    summary = "Displayed in the expanded widget",
                    checked = preferences.showReasonInWidget,
                    onChecked = onShowReason,
                )
            }
        }
        item {
            SettingsGroup("Appearance and feel") {
                SettingsToggleRow(
                    icon = Icons.Default.AutoAwesome,
                    title = "Dynamic colours",
                    summary = "Match the device wallpaper on Android 12+",
                    checked = preferences.dynamicColorEnabled,
                    onChecked = onDynamicColor,
                )
                HorizontalDivider()
                SettingsToggleRow(
                    icon = Icons.Default.Check,
                    title = "Completion haptics",
                    summary = "A subtle confirmation when an action is completed",
                    checked = preferences.hapticsEnabled,
                    onChecked = onHaptics,
                )
            }
        }
        item {
            SettingsGroup("Your data") {
                ListItem(
                    headlineContent = { Text("Share progress summary") },
                    supportingContent = {
                        Text("Exports a plain-text overview without private notes.")
                    },
                    leadingContent = { Icon(Icons.Default.Share, contentDescription = null) },
                    trailingContent = {
                        TextButton(onClick = onShare) { Text("Share") }
                    },
                )
                HorizontalDivider()
                ListItem(
                    headlineContent = { Text("Private by design") },
                    supportingContent = {
                        Text(
                            "Goals, actions, and history stay on this device. There is no account, internet permission, analytics SDK, or advertising SDK.",
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        OutlinedCard {
            Column { content() }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    summary: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(summary) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onChecked)
        },
    )
}

private fun showTimePicker(
    context: Context,
    hour: Int,
    minute: Int,
    onSelected: (Int, Int) -> Unit,
) {
    TimePickerDialog(
        context,
        { _, selectedHour, selectedMinute -> onSelected(selectedHour, selectedMinute) },
        hour,
        minute,
        false,
    ).show()
}

private fun requestPinWidget(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
    val manager = AppWidgetManager.getInstance(context)
    if (!manager.isRequestPinAppWidgetSupported) return false
    return manager.requestPinAppWidget(
        ComponentName(context, CoachWidgetReceiver::class.java),
        null,
        null,
    )
}
