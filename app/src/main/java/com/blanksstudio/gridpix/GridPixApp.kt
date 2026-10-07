package com.blanksstudio.gridpix

import android.app.Application
import com.blanksstudio.gridpix.billing.BillingManager
import com.blanksstudio.gridpix.data.settings.SettingsRepository
import com.blanksstudio.gridpix.notifications.DailyReminder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/** Application class: the Hilt component root. Registered in AndroidManifest.xml. */
@HiltAndroidApp
class GridPixApp : Application() {

    @Inject
    lateinit var billingManager: BillingManager

    @Inject
    lateinit var settings: SettingsRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // SPEC section 5/6: owned products are re-verified from Play on every launch.
        billingManager.start()
        // Keep the daily reminder schedule in line with Settings (and re-align it to the chosen hour).
        appScope.launch {
            val s = settings.settings.first()
            DailyReminder.sync(this@GridPixApp, s.reminderEnabled && DailyReminder.canNotify(this@GridPixApp), s.reminderHour)
        }
    }
}
