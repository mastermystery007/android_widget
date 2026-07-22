package com.mastermystery.oneminutecoach.widget

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.mastermystery.oneminutecoach.CoachApplication
import com.mastermystery.oneminutecoach.MainActivity
import com.mastermystery.oneminutecoach.data.WidgetSnapshot

class CoachWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(
            DpSize(120.dp, 80.dp),
            DpSize(250.dp, 110.dp),
            DpSize(250.dp, 190.dp),
        ),
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = CoachApplication.from(context).repository.getWidgetSnapshot()
        provideContent {
            CoachWidgetContent(context, snapshot)
        }
    }
}

class CoachWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CoachWidget()
}

@Composable
private fun CoachWidgetContent(
    context: Context,
    snapshot: WidgetSnapshot,
) {
    val size = LocalSize.current
    val compact = size.width < 200.dp || size.height < 100.dp
    val expanded = size.height >= 170.dp
    val suggestion = snapshot.suggestion
    val dark = context.resources.configuration.uiMode and
        Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(widgetColor(dark, light = 0xFFF7F2FA, dark = 0xFF211F26))
            .padding(if (compact) 10.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (suggestion == null) {
            Text(
                text = "One Minute Coach",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = widgetColor(dark, light = 0xFF1D1B20, dark = 0xFFFFFFFF),
                ),
            )
            Spacer(GlanceModifier.height(6.dp))
            Text(
                text = "Create a goal to get your next action.",
                style = bodyStyle(dark),
                maxLines = 2,
            )
            Spacer(GlanceModifier.height(8.dp))
            Button(
                text = "Set up",
                onClick = actionStartActivity<MainActivity>(),
            )
            return@Column
        }

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = suggestion.goalTitle,
                modifier = GlanceModifier.width(if (compact) 90.dp else 145.dp),
                style = TextStyle(
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = widgetColor(dark, light = 0xFF6750A4, dark = 0xFFD0BCFF),
                ),
                maxLines = 1,
            )
            if (!compact) {
                Spacer(GlanceModifier.width(8.dp))
                Text(
                    text = "${snapshot.currentStreak} day streak",
                    style = smallStyle(dark),
                    maxLines = 1,
                )
            }
        }

        Spacer(GlanceModifier.height(if (compact) 4.dp else 7.dp))

        Text(
            text = suggestion.actionTitle,
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = if (compact) 14.sp else 17.sp,
                color = widgetColor(dark, light = 0xFF1D1B20, dark = 0xFFFFFFFF),
            ),
            maxLines = if (expanded) 3 else 2,
        )

        if (expanded && snapshot.showReason) {
            Spacer(GlanceModifier.height(6.dp))
            Text(
                text = suggestion.reason,
                style = TextStyle(
                    fontStyle = FontStyle.Italic,
                    fontSize = 12.sp,
                    color = widgetColor(dark, light = 0xFF625B71, dark = 0xFFCAC4D0),
                ),
                maxLines = 2,
            )
        }

        Spacer(GlanceModifier.height(if (compact) 6.dp else 10.dp))

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                text = if (compact) "${suggestion.durationMinutes}m" else "Start ${suggestion.durationMinutes}m",
                onClick = actionRunCallback<StartCoachAction>(),
                modifier = GlanceModifier.width(if (compact) 55.dp else 78.dp),
            )
            Spacer(GlanceModifier.width(6.dp))
            Button(
                text = "Done",
                onClick = actionRunCallback<CompleteCoachAction>(),
                modifier = GlanceModifier.width(if (compact) 55.dp else 68.dp),
            )
            if (!compact) {
                Spacer(GlanceModifier.width(6.dp))
                Button(
                    text = "Swap",
                    onClick = actionRunCallback<SwapCoachAction>(),
                    modifier = GlanceModifier.width(68.dp),
                )
            }
        }

        if (expanded) {
            Spacer(GlanceModifier.height(6.dp))
            Text(
                text = "${snapshot.completedToday} completed today · tap the app to adjust time and energy",
                style = smallStyle(dark),
                maxLines = 1,
            )
        }
    }
}

private fun widgetColor(darkMode: Boolean, light: Long, dark: Long): ColorProvider =
    ColorProvider(Color(if (darkMode) dark else light))

private fun bodyStyle(dark: Boolean) = TextStyle(
    fontSize = 13.sp,
    color = widgetColor(dark, light = 0xFF49454F, dark = 0xFFE6E0E9),
)

private fun smallStyle(dark: Boolean) = TextStyle(
    fontSize = 10.sp,
    color = widgetColor(dark, light = 0xFF79747E, dark = 0xFFCAC4D0),
)

class StartCoachAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        context.startActivity(
            Intent(context, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_START_FOCUS, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
        )
    }
}

class CompleteCoachAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        CoachApplication.from(context).repository.completeCurrent()
        CoachWidget().update(context, glanceId)
    }
}

class SwapCoachAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        CoachApplication.from(context).repository.swapCurrent()
        CoachWidget().update(context, glanceId)
    }
}
