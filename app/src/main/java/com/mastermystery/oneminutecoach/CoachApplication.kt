package com.mastermystery.oneminutecoach

import android.app.Application
import android.content.Context
import com.mastermystery.oneminutecoach.data.CoachRepository
import com.mastermystery.oneminutecoach.worker.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CoachApplication : Application() {
    val repository: CoachRepository by lazy { CoachRepository(this) }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
        applicationScope.launch {
            repository.ensureSuggestion()
        }
    }

    companion object {
        fun from(context: Context): CoachApplication =
            context.applicationContext as CoachApplication
    }
}
