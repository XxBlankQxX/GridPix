package com.blanksstudio.gridpix

import android.app.Application
import com.blanksstudio.gridpix.billing.BillingManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/** Application class: the Hilt component root. Registered in AndroidManifest.xml. */
@HiltAndroidApp
class GridPixApp : Application() {

    @Inject
    lateinit var billingManager: BillingManager

    override fun onCreate() {
        super.onCreate()
        // SPEC section 5/6: owned products are re-verified from Play on every launch.
        billingManager.start()
    }
}
