package com.mastermystery.oneminutecoach

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.mastermystery.oneminutecoach.ui.CoachApp
import com.mastermystery.oneminutecoach.ui.CoachViewModel

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: CoachViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = CoachApplication.from(this).repository
        viewModel = ViewModelProvider(
            this,
            CoachViewModel.factory(repository, applicationContext),
        )[CoachViewModel::class.java]

        handleIntent(intent)

        setContent {
            CoachApp(viewModel = viewModel)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        when {
            intent?.getBooleanExtra(EXTRA_START_FOCUS, false) == true ->
                viewModel.startFocusFromCurrent()
            intent?.getBooleanExtra(EXTRA_OPEN_SETTINGS, false) == true ->
                viewModel.openSettings()
        }
    }

    companion object {
        const val EXTRA_START_FOCUS = "start_focus"
        const val EXTRA_OPEN_SETTINGS = "open_settings"
    }
}
