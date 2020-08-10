package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.PeriodicTasks
import javax.inject.Inject

class ExposureNotificationBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var periodicTasks: PeriodicTasks

    override fun onReceive(context: Context, intent: Intent) {
        context.applicationContext.appComponent.inject(this)

        val action = intent.action
        Timber.d("onReceive: action = $action")
        val token = intent.getStringExtra(ExposureNotificationClient.EXTRA_TOKEN) ?: "empty"
        Timber.d("onReceive: token = $token")
        if (ExposureNotificationClient.ACTION_EXPOSURE_STATE_UPDATED == action) {

            periodicTasks.scheduleExposureCircuitBreakerInitial(token)
        }
    }
}
